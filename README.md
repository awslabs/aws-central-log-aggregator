# AWS Log Aggregator

A Framework that supports aggregating logs from applications and services deployed and running in AWS into one centralized location. Focus is to centralize logs from multiple AWS accounts/regions into one immutable location and provide a unified view of the entire system.

A cloud-scale logging solution on the AWS platform in order to support proactive and reactive logging and monitoring use cases for all customer assets residing on AWS.

* Fast searches over large log volumes
* Scalable cloud-based log management
* Helps Investigate production issues faster
* Easy integration with various AWS service to provide easy monitoring of entire stack.

## Design

![Architecture](/doc/AWS-log-agg.png)

- Log Ingestion and Processing:
Proposed/Implemented architecture/Framework leverages EMR/Spark/Spark Streaming to create a unified ETL approach for extracting log data from various log sources both in batch and streaming mode, transform and persist the data into sink Elastic Search. 

- Log Processing:
 Since it leverages Spark and the framework being built is config driven, the number or type of  log data sources can easily be expanded in future to include any streaming/batch log data source (For e.g Kafka,HDFS, DB etc) and  log data sink (Search, NoSQl, Sql) with the change of few config parameters.

- Scalability:
 The solution leverages EMR (managed Hadoop) for running the spark log ingestion and processing jobs. EMR supports auto scaling and can process TBs/PBS of log data with ease. Each spark Job leverages a bunch of executors (JVMs) in a node  to process the data.

- Flexibility/Future Proof:
 By leveraging open source Spark APIs, you get access to  all spark Connectors for various sources and sinks thereby proving the flexibility to expand the solution in future by modifying the metadata to connect to log sources such as Kafka, MQ etc and persist the data  in sinks such as Elastic Search,NoSQl, DW etc.

- Pricing: 
 EMR supports auto scaling and can help save on cost. 

- Error handling:
 The solution sticks to one unified approach for processing and ingesting all kind of log data sources/sinks and has less moving parts thereby reducing the failures. Error handling is built in the framework to ensure data is not lost. This is achieved via checkpointing and custom  error handling code.

- Orchestration:
 Orchestration of the jobs is implemented using StepFunctions/Lambda.

- Metadata Storage
 Log sources Metadata are persisted in DynamoDB.

- Log Storage,Analytics and Monitoring
Proposed solution leverages AWS managed ELK stack for Storing, analyzing, and correlating application and infrastructure log data to find and fix issues faster and improve application performance. It is a fully managed service that helps monitor, and troubleshoot your applications The service provides support for open source Elasticsearch APIs, managed Kibana, integration with Logstash and other AWS services, and built-in alerting and SQL querying.

- Flexibility- Supports any unstructured data  and it is schema on write.

- Security- Provides support for  Index, doc, and field security

- Visualization- Kibana offers intuitive charts and reports that you can use to interactively navigate through large amounts of log data stored in ES. 
Using Kibana’s pre-built aggregations and filters, you can run a variety of analytics like histograms, top-N queries, and trends with just a few clicks.
You can rapidly create dashboards that pull together charts, maps, and filters to display the full picture of your data. All you need is a browser to view and explore the data.

- Machine Leaning- Easy to detect anomalies hiding in Elasticsearch data and explore the properties that significantly influence them with unsupervised machine learning features. Helps you achieve actionable insights.

- Operations- Amazon Elasticsearch Service, you get the ELK stack you need, without the operational overhead.

- Pricing- Amazon Elasticsearch Service lets you pay only for what you use – there are no upfront costs or usage requirements

- Alerting- Easy to build alerts that trigger custom actions



## Table of content

- [Installation](#installing-pre-packaged-solution-template)
- [Customization](#customization)
  - [Setup and Build](#setup-and-build)
  - [Changes](#changes)
  - [Unit Test](#unit-test)
  - [Deploy](#deploy)
- [File Structure](#file-structure)
- [License](#license)

## Installing pre-packaged solution template

- Primary Template: [log-aggregator-primary.yaml](https://gitlab.aws.dev/wwco-proserve-gcci/offerings/aws-log-aggregator/-/blob/master/automation/template/log-aggregator-primary.yaml)

- Demo Template: [log-aggregator-secondary.yaml](https://gitlab.aws.dev/wwco-proserve-gcci/offerings/aws-log-aggregator/-/blob/master/automation/template/log-aggregator-secondary.yaml)

## Customization

### Prerequisite: 
- Java 8 and above
- maven
- S3 bucket to store jar Lambda zip, Config, dependencies, dependent CFN template.

### Setup and Build

Clone the repository and run the following commands to compile source code, zip lambda function and copy them all bucket location.

After running below cmd it will ask for S3 bucket where compile code jar and other dependency will be copied. Script will add for individually for each action, enter "y" for all script actions.

```
./build.sh 
```

Once above script ran successfully it will create below directory structure in your S3 bucket location.

<pre>
|-v0.0.1/
  |app                      [ spark app ]
  |bootstrap/               [ Bootstrap script for EMR instances]
  |config/                  [ Log type configuration ]
  |dependencies/            [ EMR spark app dependency jars]
  |lambda/                  [ Lambda code for Spark job management and ES ultra-warm support]
  |template/                [ Cloudformation Templates ]
</pre>
### Changes

You can customize the code and add any Log extension to the solution.

### Deploy

In order to deploy this solution, you need to run cloudformation
Primary Template: [log-aggregator-primary.yaml](https://gitlab.aws.dev/wwco-proserve-gcci/offerings/aws-log-aggregator/-/blob/master/automation/template/log-aggregator-primary.yaml) 
from your S3 bucket that you passed in above build script.



## File structure

AWS Log Aggregator solution consists of:

- Cloudformation templates to generate needed resources
- Lambda to enable Ultrawarm feature in Elastic resource

<pre>

|-- automation
|   |-- bootstrap                                                 [Bootstrap script for EMR cluster instances]
|   |   `-- bootstrap.sh              
|   |-- dependencies                                              [Spark app jar dependencies]
|   |   |-- amazon-kinesis-client-1.7.3.jar
|   |   |-- spark-sql-kinesis_2.11-1.2.0_spark-2.4.jar
|   |   `-- spark-streaming-kinesis-asl_2.12-2.4.4.jar
|   |-- lambda                                                    [Lambda for spark job management and ElasticSearch Ultrawarm feature ]
|   |   `-- handlers
|   |       |-- index.py
|   |       `-- ultrawarm.py
|   `-- template                                                  [CFN Templates]      
|       |-- log-aggregator-nginx.yaml                             [CFN Template for nginx server- Access ES deployed on Private subnet] 
|       |-- log-aggregator-primary.yaml                           [Primary AWS Log aggregator template]
|       |-- log-aggregator-secondary.yaml                         [Secondary/Demo AWS Log aggregator template]
|       `-- log-aggregator-vpc.yaml                               
|
|-- build.sh                                                      [Build script to compile, compress lambda and upload to a s3 bucket location]
|-- config                                                        [Config for different logs types]
|   |-- apigateway
|   |   |-- apigateway.json
|   |   `-- apigatewayschema.json
|   |-- cloudfront
|   |   |-- cloudfront.json
|   |   `-- cloudfrontschema.json
|   |-- cloudtrail
|   |   |-- cloudtrail.json
|   |   `-- cloudtrailschema.json
|   |-- emr
|   |   |-- emr.json
|   |   `-- emrschema.json
|   |-- lambda
|   |   |-- lambda.json
|   |   `-- lambdaschema.json
|   `-- vpcflow
|       |-- vpcflow.json
|       `-- vpcflowschema.json
|-- pom.xml
`-- src                                                           [Source code written in java]

</pre>

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.


## License

See license [here](./LICENSE)
