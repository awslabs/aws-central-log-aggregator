# Introduction 
A Framework that supports aggregating logs from applications and services deployed and running in AWS into one centralized location. 
The framework leverages Spark/Spring libraries and is extensible.



## Design Details

### Main Class- LogAggregatorMainApplication is the entry point for the  application.

### Config Package - com.aws.logaggregator.config  

The above package has the classes responsible for reading the metadat related to the log groups. Currently the framework reads  the config files/metadata from DynamoDB

Support for other config data stores can be added by extending the BaseConfigHolder class.

Sample Config Files - Refer to the config folder for various configuration files with regards to log sources.

{
  "config_json": {
  "logaggregator": {
    "source": {
      "configoptions": {
        "sourcetype": "kinesis", // Define the source type here
        "mode": "stream", // Mode (stream or batch)
        "loggroup": "apigateway", (Service/group name associated with the logs)
        "explode": "logEvents", // This attribute lets you  define the json attibute  to be used for exploding the data into multple rows 
        "pattern" : // This attribute lets you define a regex pattern for the logs, please refer to the emr folder under config  for examples.
        "decompress":"true", // true if the data is in .gz format 
        "logformat": "json", // format of the logs (supports json,xml, txt)
        "checkpointlocation": "/tmp/apigw/checkpoint"  //Spark CheckPoint location
      },
      "sparkoptions": { 

      // Define spark related options here with regards to source Kinesis, please refer to spark doc for in depth information. The placeholders are replaced by reading the parameter values from CF templates. This is automated through CF/Lambda scripts.
        "streamName": "${LogSource}",
        "endpointUrl": "${EndpointUrl}",
        "startingposition": "TRIM_HORIZON",    
        "shardsPerTask": "5",
        "shardFetchInterval": "10s"
      }
    },
    "destination": [
      {
        "configoptions": {
          "destinationtype": "elasticsearch",
          "resource": "apigw/_doc",
          "mode": "append"
        },
        "sparkoptions": {
        	// Define spark related options here with regards to elastic search, please refer to spark doc for in depth information 
          "es.nodes": "${LogESDestination}",
          "es.net.http.auth.pass": "${ESPassword}",
          "es.net.http.auth.user": "${ESUsername}",
          "es.port": 443,
          "es.net.ssl": "true",
          "es.write.rest.error.handlers": "es",
          "es.nodes.wan.only": "true",
          "es.write.rest.error.handler.es.client.resource": "apigw-failed/_doc"
        }
      }
    ],

    // Placeholder for future enahcements with regards to errror handling and postprocessing, please leave it blank for now
    "engineconfiguration": {},
    "errorhandler": {},
    "postprocessing": {}
  }
},
  "source_name": "apigw_logs",
  "source_type": "stream",
  //Spark submit args, update the executors, memory and cores values before using this framework
  "spark_submit_args": "--deploy-mode client --executor-memory 1g --executor-cores 1 --num-executors 1 --driver-memory 1g --conf spark.driver.memoryOverhead=1024 --conf spark.executor.memoryOverhead=1024 --jars s3://${SourceCodeBucket}/${SourceCodePrefix}/dependencies/*.jar --class com.aws.logaggregator.LogAggregatorMainApplication s3://${SourceCodeBucket}/${SourceCodePrefix}/app/aws-logaggregator-v1.jar"
}


The framework lets you define a schema for the logs


A sample schema is shown below 

{
  "version": 1,
  "schema": {
    "loggroup": "example",  
    "deriveby": "index", // Accepted values Index or Name, use "Name" if csv file has header in it.
    "attributes": [
      {
        "name": "version", //name of the attribute
        "type": "integer", .// type of the atribute
        "index": 0
      },
      {
        "name": "account_id",
        "type": "String",
        "index": 1
      },
      {
        "name": "datefield",
        "type": "date",
        "format":"Mm/dd/yyyy" // lets you define a date format
        "index": 2
      }
      
    ]
  }
}

### Connector Package :-  com.aws.logaggregator.connector;

Provides implementation for  various connectors for reading the logs from (Kafka, Kinesis, S3) and for persisting the logs to Elastic Search.  More connectors can be added by extending the BaseLogConnector class.

### Log Parser package :-  com.aws.logaggregator.logparser

Provides implementation for parsing the logs. By default BaseLogParser is used, should  custom logic be added, extend the BaseLogParser and override the parse method, ensure the Component annotation of the class has the same value as the logGroup name in the config files. Refer to the code for details.


## Supported Log formats 
The solution  supports reading logs of format
1)Json
2)XML
3)CSV
4)text 


## Supported Log Sources - 

It can read logs from the following sources

1)Kinesis
2)Kafka
3)CloudStorage (S3)

## Supported Log Destination- 

Supports Persisting  the logs in the following destination

1) Elastic Search





