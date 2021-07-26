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
 * Elastic search Reader and Writer - Uses Spark elastic search connector
 *
 * @author Kiran S
 */
package com.aws.logaggregator.connector;


import com.aws.logaggregator.model.LogAggregatorMetadata;
import com.aws.logaggregator.model.LogAppInitializer;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("elasticsearch")
@Scope(value = "prototype")
public class ElasticSearchConnector extends BaseLogConnector {

    private static final Logger logger = Logger.getLogger(ElasticSearchConnector.class);

    @Override
    public String getConnectorType() {
        return "elasticsearch";
    }


    @Override
    public void writeLogData(Dataset ds, SparkSession session, LogAppInitializer initBean) {
        LogAggregatorMetadata.Logaggregator.Destination destination = initBean.getDestination("elasticsearch");


        ds.write()
                .format("org.elasticsearch.spark.sql").options(destination.getSparkoptions()).
                mode(destination.getConfigoptions().get("mode"))
                .save(destination.getConfigoptions().get("resource"));


    }
}
