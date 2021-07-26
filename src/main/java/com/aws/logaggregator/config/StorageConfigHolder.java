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
 * Holds Config info in local store, for testing purpoese
 *
 * @author Kiran S
 */
package com.aws.logaggregator.config;

import com.aws.logaggregator.model.LogAggregatorMetadata;
import com.aws.logaggregator.model.LogSchema;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StorageConfigHolder extends BaseConfigHolder implements Serializable {

    public void loadLogConfigFiles(String path, String key, String region) {
        String schemafileargs = path + "/" + key + "Schema.json";
        String configfileargs = path + "/" + key + "Config.json";

        try {

            this.logschema = initializeLogSchema(schemafileargs);
            this.logAggregatorMetadata = initializeLogMetadata(configfileargs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public LogSchema initializeLogSchema(String schemaData) throws Exception {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(schemaData));

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            //convert json string to object
            if (jsonData != null) {
                return objectMapper.readValue(jsonData, LogSchema.class);
            }
        } catch (Exception e) {

        }

        return null;

    }

    public LogAggregatorMetadata initializeLogMetadata(String configData) throws Exception {
        byte[] jsonData = Files.readAllBytes(Paths.get(configData));
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonData, LogAggregatorMetadata.class);

    }

}
