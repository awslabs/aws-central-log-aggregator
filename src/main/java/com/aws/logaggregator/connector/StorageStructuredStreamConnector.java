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
 * S3 File Streaming Reader and Writer for logs - Uses Spark Streaming
 *
 * @author Kiran S
 */
package com.aws.logaggregator.connector;

import com.aws.logaggregator.model.LogAggregatorMetadata;
import com.aws.logaggregator.model.LogAppInitializer;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("storagestreaming")
@Scope(value = "prototype")
public class StorageStructuredStreamConnector extends BaseLogConnector {

    private static final Logger logger = Logger.getLogger(StorageStructuredStreamConnector.class);

    @Override
    public String getConnectorType() {
        return "storage";
    }

    @Override
    public Dataset readLogData(SparkSession sparkSession, LogAppInitializer initBean) {
        try {


            LogAggregatorMetadata.Logaggregator.Source source = initBean.getMetdadataBean().getSource();

            Dataset<Row> ds = null;


            Map<String, String> engineOptions =
                    initBean.getMetdadataBean().getSource().getSparkoptions();

            Map<String, String> ancillaryOptions =
                    initBean.getMetdadataBean().getSource().getConfigoptions();

            String dataformat = ancillaryOptions.get("logformat");
            String inputDir = ancillaryOptions.get("location");
            String pattern = ancillaryOptions.get("pattern");
            if (engineOptions != null && engineOptions.size() > 0) {
                if (dataformat.equalsIgnoreCase("json") || dataformat.equalsIgnoreCase("xml")) {
                    ds = sparkSession.readStream().options(engineOptions).format("text").schema(initBean.schemaStruct).load(inputDir);

                } else {
                    ds = sparkSession.readStream().options(engineOptions).format(dataformat).schema(initBean.schemaStruct).load(inputDir);
                }
            } else {

                if (dataformat.equalsIgnoreCase("json") || dataformat.equalsIgnoreCase("xml")) {
                    ds = sparkSession.readStream().format("text").schema(initBean.schemaStruct).load(inputDir);

                } else {
                    ds = sparkSession.readStream().options(engineOptions).format(dataformat).schema(initBean.schemaStruct).load(inputDir);
                }
            }


            return ds;


        } catch (Exception e) {
            logger.error("Error thrown", e);
            throw e;
        }

    }


}

