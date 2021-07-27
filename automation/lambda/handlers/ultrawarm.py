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
"""

"""
Lambda function to enables ultrawarm on ElasticSearch

@author iftik
"""

import base64
import json
import logging
import os
from datetime import datetime
from typing import Optional

import boto3
from elasticsearch import Elasticsearch

logger = logging.getLogger()
logger.setLevel(logging.INFO)

session = boto3.Session()
ssm_client = boto3.client('secretsmanager', region_name=os.environ['REGION'])


def lambda_handler(event, context):
    es = get_elasticsearch_client()
    my_indices = get_all_log_indices(es)
    for index in my_indices:
        migrating_index_to_warm(es, index, int(os.environ['HOT_INDEX_AGE_LIMIT']))

    return


def migrating_index_to_warm(es, index, hot_index_age_limit):
    logger.info("Evaluating index for ultrawarm migration: " + index)
    result = es.indices.get(index=index)

    flat_json = flatten_json(result)

    is_write_index = json_search(flat_json, 'is_write_index')
    creation_date = int(int(json_search(flat_json, 'creation_date')) / 1000)
    box_type = json_search(flat_json, 'box_type')

    if is_write_index != None and not is_write_index:
        if box_type == None or box_type != 'warm':
            index_age = datetime.now() - datetime.fromtimestamp(creation_date)
            if index_age.days >= hot_index_age_limit:
                logger.info("Migrating following index to warm: " + index)
                es.indices.migrate_to_ultrawarm(index=index)


def flatten_json(nested_json):
    out = {}

    def flatten(x, name=''):
        if type(x) is dict:
            for a in x:
                flatten(x[a], name + a + '_')
        elif type(x) is list:
            i = 0
            for a in x:
                flatten(a, name + str(i) + '_')
                i += 1
        else:
            out[name[:-1]] = x

    flatten(nested_json)
    return out


def json_search(json_object, key):
    for attribute, value in json_object.items():
        if key in attribute:
            return value


def get_all_log_indices(es):
    result = []

    print('Retrieving all indices...')
    indices = es.cat.indices(params={
        'format': 'json',
        'h': 'index'
    })
    for index in indices:
        index_name = index['index']
        if index_name.startswith('my'):
            result += [index_name]

    return result


def get_elasticsearch_client():
    # TODO: Prepare environment variable
    es_endpoint = 'https://' + os.environ['ELASTICSEARCH_ENDPOINT']

    # TODO: Prepare environment variable
    es_credential_user = get_secret(os.environ['SSM_PATH'], "username")
    es_credential_pass = get_secret(os.environ['SSM_PATH'], "password")

    logger.info('Preparing ES client...')
    es = Elasticsearch([es_endpoint], http_auth=(es_credential_user, es_credential_pass))
    return es


def get_secret(secret_name: str, sec_key: str) -> Optional[str]:
    get_secret_value_response = ssm_client.get_secret_value(SecretId=secret_name,
                                                            VersionStage="AWSCURRENT")
    if 'SecretString' in get_secret_value_response:
        secret = get_secret_value_response['SecretString']
    else:
        secret = base64.b64decode(get_secret_value_response['SecretBinary'])
    if sec_key:
        secret = json.loads(secret)[sec_key]
    return secret
