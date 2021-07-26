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
 * Extend this class to add different config holders (Support now exist for only DynamoDB and flat files)
 *
 * @author Kiran S
 */
package com.aws.logaggregator.config;

import com.aws.logaggregator.model.LogAggregatorMetadata;
import com.aws.logaggregator.model.LogSchema;
import com.aws.logaggregator.security.BaseSecretParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;

public abstract class BaseConfigHolder implements Serializable {

    @Autowired
    protected BaseSecretParam secretParam;
    protected LogSchema logschema;
    protected LogAggregatorMetadata logAggregatorMetadata;
    @Value("${config.fieldMapping.hashKeyName}")
    String hashKeyName;
    @Value("${config.fieldMapping.schema}")
    String schema;
    @Value("${config.fieldMapping.config}")
    String logconfig;

    public abstract void loadLogConfigFiles(String path, String key, String region);

    public abstract LogSchema initializeLogSchema(String schemaData) throws Exception;

    public abstract LogAggregatorMetadata initializeLogMetadata(String configData) throws Exception;

    public LogAggregatorMetadata getLogAggregatorMetadata() {
        return logAggregatorMetadata;
    }

    public LogSchema getLogSchema() {
        return logschema;
    }

}
