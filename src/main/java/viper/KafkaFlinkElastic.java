package viper;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSink;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch2.RequestIndexer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer09;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.net.InetSocketAddress;
import java.util.*;


/**
 * @author Keira Zhou
 * @date 05/10/2016
 */
public class KafkaFlinkElastic {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<String> stream = readFromKafka(env);
        stream.print();
        writeToElastic(stream);
        // execute program
        env.execute("Viper Flink!");
    }

    public static DataStream<String> readFromKafka(StreamExecutionEnvironment env) {
        env.enableCheckpointing(5000);
        // set up the execution environment
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "test");

        DataStream<String> stream = env.addSource(
                new FlinkKafkaConsumer09<>("test", new SimpleStringSchema(), properties));
        return stream;
    }

    public static void writeToElastic(DataStream<String> input) {

        Map<String, String> config = new HashMap<>();

        // This instructs the sink to emit after every element, otherwise they would be buffered
        config.put("bulk.flush.max.actions", "1");
        config.put("cluster.name", "es_keira");

        try {
            // Add elasticsearch hosts on startup
            List<InetSocketAddress> transports = new ArrayList<>();
            transports.add(new InetSocketAddress("127.0.0.1", 9300)); // port is 9300 not 9200 for ES TransportClient

            ElasticsearchSinkFunction<String> indexLog = new ElasticsearchSinkFunction<String>() {
                public IndexRequest createIndexRequest(String element) {
                    String[] logContent = element.trim().split("\t");
                    Map<String, String> esJson = new HashMap<>();
                    esJson.put("IP", logContent[0]);
                    esJson.put("info", logContent[1]);

                    return Requests
                            .indexRequest()
                            .index("viper-test")
                            .type("viper-log")
                            .source(esJson);
                }

                @Override
                public void process(String element, RuntimeContext ctx, RequestIndexer indexer) {
                    indexer.add(createIndexRequest(element));
                }
            };

            ElasticsearchSink esSink = new ElasticsearchSink(config, transports, indexLog);
            input.addSink(esSink);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
