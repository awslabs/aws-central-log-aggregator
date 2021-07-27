/*
*  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*  
*  Licensed under the Apache License, Version 2.0 (the "License").
*  You may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*  
*      http://www.apache.org/licenses/LICENSE-2.0
* 
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

/**
 * Holds Config info in DynamoDB
 *
 * @author Kiran S
 */
package com.aws.logaggregator.config;


import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.aws.logaggregator.model.LogAggregatorMetadata;
import com.aws.logaggregator.model.LogSchema;
import com.aws.logaggregator.security.BaseSecretParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;


public class DynamoDBConfigHolder extends BaseConfigHolder implements Serializable {


    private static final Logger logger = Logger.getLogger(DynamoDBConfigHolder.class);
    private final String predefinedSchemaLocation = null;
    @Autowired
    protected BaseSecretParam secretParam;


    public void loadLogConfigFiles(String tableName, String key, String region) {

        String endpoint = "dynamodb." + region + ".amazonaws.com";

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .build();
        DynamoDB dynamoDB = new DynamoDB(client);

        String config = retrieveItem(dynamoDB, tableName, key);

        JSONObject obj = null;
        if (config != null) {
            obj = new JSONObject(config);
        }
        //this.region = region;
        try {
            String fileConfigData = secretParam.substituteSecrets(obj.get(logconfig).toString());
            String finalConfigData = secretParam.substituteConfigArgs(fileConfigData);
            logAggregatorMetadata = initializeLogMetadata(finalConfigData);

            try {
                if (obj.get(schema) != null && !"".equals(obj.get(schema).toString())) {
                    this.logschema = initializeLogSchema(obj.get(schema).toString());
                }
            } catch (Exception e) {
                logger.debug("Couldn't find schema for the key-->" + key + "-->" + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Please ensure you have provided the right key for retrieving the config files and make sure you have " +
                    "connectivity and access " +
                    "to DynamoDB plus the jsons are saved in proper format", e);
        }
    }


    private String retrieveItem(DynamoDB dynamoDB, String tableName, String pKey) {
        Table table = dynamoDB.getTable(tableName);
        Item item = null;
        try {
            item = table.getItem(hashKeyName, pKey, null, null);
        } catch (Exception e) {
           logger.error("GetItem failed.");

        }
        return item == null ? null : item.toJSONPretty();
    }


    public LogSchema initializeLogSchema(String schemaData) throws Exception {
        return new ObjectMapper().readValue(schemaData, LogSchema.class);
    }


    public LogAggregatorMetadata initializeLogMetadata(String configData) throws Exception {
        return new ObjectMapper().readValue(configData, LogAggregatorMetadata.class);
    }


}
