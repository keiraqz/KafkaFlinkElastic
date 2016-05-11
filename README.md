# Kafka Flink Elastic

* Create a Flink project

```
mvn archetype:generate                               \
      -DarchetypeGroupId=org.apache.flink              \
      -DarchetypeArtifactId=flink-quickstart-java      \
      -DarchetypeVersion=1.1-SNAPSHOT
```

* Build the project

```
mvn clean install -Pbuild-jar
```

* Import in IntelliJ

	* IntelliJ:
		* Select “File” -> “Import Project”
		* Select root folder of your project
		* Select “Import project from external model”, select “Maven”
		* Leave default options and finish the import
		
* If running program on a instance but not in IDE

	* Download:
		
	```
	http://apache.mirrors.pair.com/flink/flink-1.0.2/flink-1.0.2-bin-hadoop27-scala_2.11.tgz
	```
	
	* Unzip and cd into the folder:
	
	```
	# start
	./bin/start-local.sh
	
	# stop
	./bin/stop-local.sh
	```
		
* Flink 1.0.2 | Kafka 0.9 | Elasticsearch 2.3.2

	* [Flink & Kafka Connector](https://ci.apache.org/projects/flink/flink-docs-master/apis/streaming/connectors/kafka.html)

		* Maven dependency
		
		```
		<dependency>
			<groupId>org.apache.flink</groupId>
			<artifactId>flink-connector-kafka-0.9_2.10</artifactId>
			<version>1.0.2</version>
		</dependency>
		```
	
		* Example Java code
		
		```java
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
		```
	
	* [Flink & Elasticsearch Connector](https://ci.apache.org/projects/flink/flink-docs-master/apis/streaming/connectors/elasticsearch2.html)

		* Maven dependency
		
		```
		<dependency>
		  <groupId>org.apache.flink</groupId>
		  <artifactId>flink-connector-elasticsearch2_2.10</artifactId>
		  <version>1.1-SNAPSHOT</version>
		</dependency>
		```
		
		* Example Java code
		
		```java
		public static void writeElastic(DataStream<String> input) {

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
		
		```

* Put it all together

[Java code](https://github.kdc.capitalone.com/keira/Viper/blob/master/flink/src/main/java/viper/KafkaFlinkElastic.java)