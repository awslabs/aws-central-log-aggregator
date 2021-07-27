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
 * Kafka Reader and Writer for logs - Uses Spark Streaming
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
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

@Component("kafka")
@Scope(value = "prototype")
public class KafkaConnector extends BaseLogConnector {

    private static final Logger logger = Logger.getLogger(KafkaConnector.class);

    @Override
    public String getConnectorType() {
        return "kafka";
    }

    @Override
    public Dataset readLogData(SparkSession sparkSession, LogAppInitializer initBean) {
        try {


            LogAggregatorMetadata.Logaggregator.Source source = initBean.getMetdadataBean().getSource();

            Dataset<Row> ds = null;
            if ("true".equalsIgnoreCase(source.getConfigoptions().get("decompress"))) {
                UDF1<byte[], String> decompress = new UDF1<byte[], String>() {
                    @Override
                    public String call(byte[] compressed) throws Exception {
                        final StringBuilder outStr = new StringBuilder();
                        final ByteArrayInputStream ba = new ByteArrayInputStream(compressed);
                        final GZIPInputStream gis = new GZIPInputStream(ba);
                        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));

                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            outStr.append(line);
                        }
                        ba.close();
                        gis.close();
                        bufferedReader.close();
                        return outStr.toString();
                    }
                };

                sparkSession.udf().register("decompress", decompress, DataTypes.StringType);
            }

            if ("json".equalsIgnoreCase(source.getConfigoptions().get("logformat"))) {
                ds = sparkSession
                        .readStream()
                        .format("kafka")
                        .options(source.getSparkoptions())
                        .load()
                        //.select(org.apache.spark.sql.functions.from_json(org.apache.spark.sql.functions.col("data"), initBean.schemaStruct).as("data"))
                        .select("data");

            } else if ("text".equalsIgnoreCase(source.getConfigoptions().get("logformat"))) {

                ds = sparkSession
                        .readStream()
                        .format("kafka")
                        .options(source.getSparkoptions())
                        .load().selectExpr("CAST(data AS STRING) as data")
                        //.select(org.apache.spark.sql.functions.from_json(org.apache.spark.sql.functions.col("data"), initBean.schemaStruct).as("data"))
                        .select("data");


            } else if ("xml".equalsIgnoreCase(source.getConfigoptions().get("logformat"))) {

                ds = sparkSession
                        .readStream()
                        .format("kafka")
                        .options(source.getSparkoptions())
                        .load().selectExpr("CAST(data AS STRING) as data")
                        //.select(org.apache.spark.sql.functions.from_json(org.apache.spark.sql.functions.col("data"), initBean.schemaStruct).as("data"))
                        .select("data");


            }
            return ds;

        } catch (Exception e) {
            //TODO need to decide on what actions to be taken
            logger.error("Error thrown", e);
            //throw e;
        }
        return null;
    }


}
