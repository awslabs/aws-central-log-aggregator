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
 * S3 Reader and Writer - Uses Spark Streaming
 *
 * @author Kiran S
 */
package com.aws.logaggregator.connector;

import com.aws.logaggregator.model.LogAppInitializer;
import com.aws.logaggregator.utils.PatternBasedParserUtil;
import com.aws.logaggregator.utils.SchemaBasedParserUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

@Component("storage")
@Scope(value = "prototype")
public class StorageConnector extends BaseLogConnector implements Serializable {
    @Override
    public String getConnectorType() {
        return "Storage";
    }

    @Override
    public Dataset readLogData(SparkSession sparkSession, LogAppInitializer initBean) {

        Map<String, String> sparkoptions =
                initBean.getMetdadataBean().getSource().getSparkoptions();

        Map<String, String> configoptions =
                initBean.getMetdadataBean().getSource().getConfigoptions();


        String inputDir = configoptions.get("location");
        String format = configoptions.get("logformat");


        Dataset ds = null;
        if (sparkoptions != null && sparkoptions.size() > 0) {
            ds = sparkSession.sqlContext().read().options(sparkoptions).format(format).load(inputDir);
        } else {
            ds = sparkSession.sqlContext().read().format(format).load(inputDir);
        }

        if ("text".equalsIgnoreCase(format)) {
            String pattern = configoptions.get("pattern");
            if (pattern != null) {
                ds = ds.flatMap(
                        new PatternBasedParserUtil(pattern), Encoders.bean(Row.class));
            } else {
                ds = ds.map(new SchemaBasedParserUtil(initBean.schema), Encoders.bean(Row.class));
            }
        }

        return ds;


    }

    @Override
    public void writeLogData(Dataset dataset, SparkSession session, LogAppInitializer initBean) {

        Map<String, String> ancillaryOptions = initBean.getDestination("storage").getConfigoptions();
        Map<String, String> engineoptions = initBean.getDestination("storage").getSparkoptions();
        String outputformat = ancillaryOptions.get("outputformat");
        String savemode = ancillaryOptions.get("savemode");
        String partitioncolumns = ancillaryOptions.get("partitioncolumns");

        String outputDirectory = ancillaryOptions.get("outputlocation");
        String partitionStrategy = ancillaryOptions.get("partitionstrategy");

        int numberofOutputPartitons = 1;

        if (ancillaryOptions.get("numberofoutputpartitions") != null) {
            numberofOutputPartitons = Integer.valueOf(ancillaryOptions.get("numberofoutputpartitions"));
        }

        if (partitioncolumns != null && !"".equalsIgnoreCase(partitioncolumns.trim())) {

            String[] pcolumns = partitioncolumns.split(",");
            if (engineoptions != null && engineoptions.size() > 0) {
                if (partitionStrategy != null && partitionStrategy.equalsIgnoreCase("repartition")) {
                    dataset.repartition(numberofOutputPartitons).write().options(engineoptions).format(outputformat).mode(savemode).
                            partitionBy(pcolumns).save(outputDirectory);
                } else {
                    dataset.coalesce(numberofOutputPartitons).write().options(engineoptions).format(outputformat).mode(savemode).
                            partitionBy(pcolumns).save(outputDirectory);
                }

            } else {
                if (partitionStrategy != null && partitionStrategy.equalsIgnoreCase("repartition")) {
                    dataset.repartition(numberofOutputPartitons).write().format(outputformat).mode(savemode).
                            partitionBy(pcolumns).save(outputDirectory);
                } else {
                    dataset.coalesce(numberofOutputPartitons).write().format(outputformat).mode(savemode).
                            partitionBy(pcolumns).save(outputDirectory);
                }

            }


        } else {
            if (engineoptions != null && engineoptions.size() > 0) {
                if (partitionStrategy != null && partitionStrategy.equalsIgnoreCase("repartition")) {
                    dataset.repartition(numberofOutputPartitons).write().options(engineoptions).format(outputformat).mode(savemode).save(outputDirectory);

                } else {
                    dataset.coalesce(numberofOutputPartitons).write().options(engineoptions).format(outputformat).mode(savemode).save(outputDirectory);

                }

            } else {
                if (partitionStrategy != null && partitionStrategy.equalsIgnoreCase("repartition")) {
                    dataset.repartition(numberofOutputPartitons).write().format(outputformat).mode(savemode).save(outputDirectory);
                } else {
                    dataset.coalesce(numberofOutputPartitons).write().format(outputformat).mode(savemode).save(outputDirectory);
                }

            }

        }

    }


}
