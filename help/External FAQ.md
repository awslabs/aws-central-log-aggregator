# External FAQ.

## What are the benefits of using this Framework?

This framework enables aggregating logs from applications and services deployed and running in AWS into one centralized location (Elastic Search). This solution when deployed would enable 
> Fast searches over large log volumes
> Helps Investigate production issues faster


## What AWS services are used to achieve this solution?

The solution leverages DynamoDB  (config files realted to log groups are persisted in DynamoDB), EMR (Solution runs on EMR and leverages Spark), Cloudformation & Lambda(Automation)


## What libraries are used in the framework ?

Framework is built using Java , Spark and Spring   libraries.


## Can the Solution be customized ?

Yes, the solution is extensible and can be customized. Please refer to help.md for customizing and extending the solution.


## Is the Solution Scalable ?

The solution leverages EMR/Spark (managed Hadoop) for running the  log ingestion and processing jobs. EMR supports auto scaling and can process TBs/PBs of log data with ease. Each  Job leverages a bunch of executors (JVMs) in a node  to process the data.


## What service is used for Log Storage ?

Proposed solution leverages AWS managed ELK stack for Storing, Analyzing, and correlating application and infrastructure log data to find and fix issues faster and improve application performance. It is a fully managed service that helps monitor, and troubleshoot your applications. The service provides support for open source Elasticsearch APIs, managed Kibana, integration with Logstash and other AWS services, and built-in alerting and SQL querying.

## Is the Solution Secure ?

The solution leverages  AWS EMR/Elasticsearch/DynamoDB/Lambda. These  Services leverage IAM, Security Groups, SSH, Kerberos, Data Encryption/SSL/TLS to achieve the desired Security. Elastic Search supports  Index, doc, and field security too.


## Is there support for Log Analytics/Visualization ?  

Solution leverages AWS Managed ELK. Kibana offers intuitive charts and reports that can be used to interactively navigate through large amounts of log data stored in ES. Using Kibana's pre-built aggregations and filters,  a variety of analytics like histograms, top-N queries, and trends with just a few clicks. You can rapidly create dashboards that pull together charts, maps, and filters to display the full picture of your data. All you need is a browser to view and explore the data.


## Are there any Operartional Overhead ?

Solution is designed/automated to leverage Managed Services provided by AWS such as EMR/ELasticSearch thereby reducing operational overhead.

## Does the solution  automate the process of managing ES indices ?

The solution automates the process of managing indices by leveraging  ultrawarm storage and  indices rollover.

## How do I Search for logs ?

Solution leverages Amazon Elasticsearch Service for storing the log data. There are several common methods for searching documents in Amazon Elasticsearch Service (Amazon ES), including URI searches and request body searches. Amazon ES offers additional functionality that improves the search experience, such as custom packages, SQL support, and asynchronous search.




