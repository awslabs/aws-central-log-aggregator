"""
  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
  
  Licensed under the Apache License, Version 2.0 (the "License").
  You may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

  Lambda function to handle spark jobs

  @author iftik
"""

import base64
import json
import logging
import re
import uuid
from typing import Optional

import boto3
import urllib3
from boto3.dynamodb.conditions import Key

secret_field_pattern = '[$][{](\w+)[}]'

s3 = boto3.resource('s3')

# Initialize logger and set log level
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize boto3 and the necessary client(s)
session = boto3.Session()
s3_client = boto3.resource('s3')

es_client = session.client('es')
logs_client = boto3.client('logs')
iam_client = boto3.client('iam')
streams = boto3.client('dynamodbstreams')
session = boto3.session.Session()
sns_client = boto3.client('sns')

logging.basicConfig(format='%(asctime)s %(levelname)s %(funcName)s::%(filename)s(%(lineno)d) :: %(message)s',
                    level=logging.INFO)

SUCCESS = "SUCCESS"
FAILED = "FAILED"

http = urllib3.PoolManager()
logger = logging.getLogger()


def handler(event, context):
    logger.info(f'helper received event: {json.dumps(event)}')

    response_data = dict({
        "Data": "DONE",
        "Status": "SUCCESS",
        "CustomResourcePhysicalID": None
    })

    response_data["CustomResourcePhysicalID"] = \
        context.aws_request_id if context and context.aws_request_id else uuid.uuid4()

    resource_properties = event.get('ResourceProperties')
    resource_type = event.get("ResourceType")

    if resource_type == "Custom::SendSecrets":
        region_name = resource_properties.get("RegionName")
        secret_name = resource_properties.get("ESSecretName")
        topic = resource_properties.get("Topic")
        kibana_url = resource_properties.get("KibanaURL")

        if event["RequestType"] == "Create":
            secrets = get_secret(secret_name, None, region_name)
            if secrets:
                secrets = json.loads(secrets)
            message = f"AWS LogAggregator Kibana Dashboard URL: {kibana_url} and secrets are as below \n" \
                      f" Username: {secrets['username']} \n Password:{secrets['password']}\n "
            sns_client.publish(
                TopicArn=topic,
                Message=message,
                Subject='AWS LogAggregator Kibana Dashboard'
            )
        if event["RequestType"] == "Delete":
            pass

    elif resource_type == "Custom::GenerateConfig":
        region = resource_properties.get("RegionName")
        dynamodb_table = resource_properties.get("ConfigTable")
        cluster_id = resource_properties.get("EMRClusterId")

        if event["RequestType"] == "Create":
            config = prepare_log_config(event)
            dynamodb_client = boto3.resource("dynamodb", region)
            table = dynamodb_client.Table(dynamodb_table)
            table.put_item(Item=config)
        if event["RequestType"] == "Delete":
            response_data["CustomResourcePhysicalID"] = event["PhysicalResourceId"]
            try:
                emr_client = session.client('emr')
                emr_client.set_termination_protection(
                    JobFlowIds=[cluster_id],
                    TerminationProtected=False
                )
            except Exception:
                pass

    else:
        new_step_id = None
        step_id = None
        topic = None
        # Get event name to confirm whether this is an add step workflow and cancel step workflow
        for record in event['Records']:

            event_name = record['eventName']
            event_source = record['eventSource']

            if event_source == 'aws:dynamodb':

                # Add step workflow via DynamoDB insert or modify trigger
                if event_name in ['INSERT', 'MODIFY']:

                    # Extract log source name and type
                    source_name = record['dynamodb']['Keys']['source_name']['S']
                    source_type = record['dynamodb']['NewImage']['source_type']['S']

                    # Get dynamodb name
                    dynamodb_table = record['eventSourceARN'].split('/')[1]

                    # Confirm if config_json has been provided
                    try:
                        config_json = record['dynamodb']['NewImage']['config_json']['M']
                        cluster_id = record['dynamodb']['NewImage']['cluster_id']['S']
                        region = record['dynamodb']['NewImage']['region']['S']
                        topic = record['dynamodb']['NewImage']['topic']['S']
                    except Exception as e:
                        logger.error(e)
                        error_message = f'Failed to validate Config JSON for {source_name}. ' \
                                        f'config_json has to be provided in the DynamoDB item as a map. ' \
                                        f'String will not be accepted.'

                        # Notify admin if failed to prepare log config json
                        sns_client.publish(
                            TopicArn=topic,
                            Message=error_message,
                            Subject='Error: Config JSON',
                        )
                        return None

                    emr_client = session.client('emr')
                    # Prepare inputs for state machine / step functions
                    # Validate if EMR cluster (by cluster ID) is running
                    try:
                        emr_client.describe_cluster(ClusterId=cluster_id)
                    except Exception as e:
                        logger.error(str(e))
                        error_message = f'Not able to find any running EMR cluster {cluster_id}'
                        sns_client.publish(
                            TopicArn=topic,
                            Message=error_message,
                            Subject='Error: EMR Cluster not found.',
                        )
                        return None

                    # INFO: Duplicate step check
                    logger.info('Confirm if an EMR step with name \'' + source_name + '\' has been added before')
                    step_id = get_step_id_by_name(source_name, cluster_id, emr_client)

                    if step_id:
                        error_message = f'Step {source_name} already been added as step ID {step_id}'
                        logger.info(error_message)

                        # Prepare notification message and send over to Admin
                        if topic:
                            sns_client.publish(
                                TopicArn=topic,
                                Message=error_message,
                                Subject='Config JSON error',
                            )
                        return None

                    else:
                        logger.info(f'EMR step with name {source_name} has not been added')

                    # Set termination protection for EMR cluster
                    logger.info('Turn on termination protection for EMR cluster')
                    emr_client.set_termination_protection(
                        JobFlowIds=[cluster_id],
                        TerminationProtected=True
                    )
                    # Get spark submit arguments if available
                    try:
                        spark_submit_args = ('spark-submit ' + record['dynamodb']['NewImage']['spark_submit_args']['S'])
                    except Exception as e:
                        logger.info(
                            'Spark submit arguement was not provided by user. Default spark submit arguement will be applied')

                    # Add job/step to EMR cluster
                    new_step_id = submit_emr_step(dynamodb_table, source_name, source_type, spark_submit_args,
                                                  cluster_id, emr_client, region)
                    logger.info('New step ID ' + new_step_id)

                    message = f'New Job by {new_step_id} is being added to Cluster {cluster_id}'

                    # TODO: iftik, send step ID over to step functions for monitoring

                    # Notify admin of newly added EMR log job.
                    sns_client.publish(
                        TopicArn=topic,
                        Message=message,
                        Subject='Info:  New Job added',
                    )

                    # Terminate this Lambda execution after sending the message to Step Function
                    return None

                # Cancel step workflow. To be defined.
                elif event_name == 'REMOVE':
                    logger.info('REMOVED ' + record['dynamodb']['Keys']['source_name']['S'])

            response_data["CustomResourcePhysicalID"] = new_step_id or step_id

        return None

    send(event, context, response_data["Status"], response_data,
         physical_resource_id=response_data["CustomResourcePhysicalID"])


def prepare_log_config(event):
    resource_properties = event['ResourceProperties']
    log_type = resource_properties.get("LogType")
    if log_type:
        log_type = log_type.lower()

    region = resource_properties.get("RegionName")
    cluster_id = resource_properties.get("EMRClusterId")
    topic = resource_properties.get("Topic")
    source_code_bucket = resource_properties.get("SourceCodeBucket")

    if resource_properties.get('ESSecretName'):
        resource_properties[
            'ESUsername'] = '${' + f'{resource_properties.get("ESSecretName")}|{region}|{"username"}' + '}'
        resource_properties[
            'ESPassword'] = '${' + f'{resource_properties.get("ESSecretName")}|{region}|{"password"}' + '}'

    source_code_prefix = resource_properties.get("SourceCodePrefix")

    log_config_schema = None
    log_config = get_config_template_from_s3(source_code_bucket,
                                             f'{source_code_prefix}/config/{log_type}/{log_type}.json')
    try:
        log_config_schema = get_config_template_from_s3(source_code_bucket,
                                                        f'{source_code_prefix}/config/{log_type}/{log_type}schema.json')
    except:
        logger.info(f"Schema not found for {log_type}")

    log_config["schema"] = log_config_schema
    log_config["region"] = region
    log_config["cluster_id"] = cluster_id
    log_config["topic"] = topic

    log_config = fill_config(log_config, resource_properties)

    logger.info(log_config)

    return log_config


def get_config_template_from_s3(log_config_bucket, log_config_prefix):
    _config = {}
    if log_config_bucket and log_config_prefix:
        data = s3_client.Object(log_config_bucket, log_config_prefix).get()['Body'].read().decode('utf-8')
        _config = json.loads(data)
    return _config


def fill_config(d, prop):
    for k in d:
        if isinstance(d[k], dict) and bool(d[k]):
            fill_config(d[k], prop)
        elif isinstance(d[k], list):
            for i in d[k]:
                if isinstance(i, (list, dict)):
                    fill_config(i, prop)
        elif isinstance(d[k], str):
            while True:
                if re.search(secret_field_pattern, d[k]):
                    secret_path = re.search(secret_field_pattern, d[k]).group(1)
                    if secret_path and prop.get(secret_path):
                        d[k] = d[k].replace('${' + secret_path + '}', prop.get(secret_path))
                else:
                    break
    return d


def get_secret(secret_name: str, sec_key: Optional[str], region_name: str) -> Optional[str]:
    client = session.client(service_name='secretsmanager', region_name=region_name)
    get_secret_value_response = client.get_secret_value(SecretId=secret_name,
                                                        VersionStage="AWSCURRENT")
    if 'SecretString' in get_secret_value_response:
        secret = get_secret_value_response['SecretString']
    else:
        secret = base64.b64decode(get_secret_value_response['SecretBinary'])
    if sec_key:
        secret = json.loads(secret)[sec_key]
    return secret


def send(event, context, response_status, response_data, physical_resource_id=None, no_echo=False, reason=None):
    responseUrl = event['ResponseURL']

    logger.info(responseUrl)

    response_body = {
        'Status': response_status,
        'Reason': reason or "See the details in CloudWatch Log Stream",
        'PhysicalResourceId': physical_resource_id,
        'StackId': event['StackId'],
        'RequestId': event['RequestId'],
        'LogicalResourceId': event['LogicalResourceId'],
        'NoEcho': no_echo,
        'Data': response_data
    }

    json_response_body = json.dumps(response_body)
    logger.info(json_response_body)

    headers = {
        'content-type': '',
        'content-length': str(len(json_response_body))
    }

    try:
        response = http.request('PUT', responseUrl, headers=headers, body=json_response_body)
        logger.info(f"Status code: {response.status}")
    except Exception as e:
        logger.info("send(..) failed executing http.request(..):", e)


def submit_emr_step(dynamodb_table, source_name, source_type, spark_submit_args, cluster_id, emr_client, region):
    emr_step_name = source_name
    logger.info(f'Submit EMR step with step name {emr_step_name}')
    args = spark_submit_args
    val = ' ' + region + ' ' + dynamodb_table + ' ' + source_name + ' ' + source_type
    args = args + val

    response = emr_client.add_job_flow_steps(
        JobFlowId=cluster_id,
        Steps=[
            {
                'Name': source_name,
                'ActionOnFailure': 'CONTINUE',
                'HadoopJarStep': {
                    'Jar': 'command-runner.jar',
                    'Args': args.split()
                }
            }
        ]
    )

    return response['StepIds'][0]


def get_step_id_by_name(emr_step_name, cluster_id, emr_client):
    # Default result to null
    step_id = None

    # Query for all running, pending and cancel pending steps
    response = emr_client.list_steps(
        ClusterId=cluster_id,
        StepStates=['RUNNING', 'PENDING', 'CANCEL_PENDING']
    )

    # Loop through result for comparison
    for step in response['Steps']:
        if emr_step_name == step['Name']:
            if step['Status']['State'] in ['PENDING', 'RUNNING']:
                step_id = step['Id']

    return step_id


def get_config_dynamodb(dynamodb_table: str, source_name: str, source_type: str, region: str):
    dynamodb_client = boto3.resource("dynamodb", region)
    table = dynamodb_client.Table(dynamodb_table)
    response = table.query(
        ProjectionExpression="config_json",
        KeyConditionExpression=Key("source_name").eq(source_name) & Key("source_type").eq(source_type)
    )
    return response["Items"][0]["config_json"]
