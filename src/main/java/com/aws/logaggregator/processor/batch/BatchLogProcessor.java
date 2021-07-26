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
 * Base class and has the logic for batch processing of  logs
 *
 * @author Kiran S
 */
package com.aws.logaggregator.processor.batch;


import com.aws.logaggregator.connector.BaseLogConnector;
import com.aws.logaggregator.exception.LogAggregatorException;
import com.aws.logaggregator.model.LogAggregatorMetadata;
import com.aws.logaggregator.processor.BaseLogProcessor;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.storage.StorageLevel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("batch")
@Scope(value = "prototype")
public class BatchLogProcessor extends BaseLogProcessor {

    private static final Logger logger = Logger.getLogger(BatchLogProcessor.class);


    public void process(String type, ApplicationContext ctx) throws LogAggregatorException {
        logger.info("Data Processing Begin------->" + type);
        if (initializer == null || initializer.getMetdadataBean() == null) {
            logger.error("EXITING-->Metadata doesn't exist or the key--> " + type + "<--you have passed is missing in the metadata");
            return;
        }
        String sourceType = initializer.getMetdadataBean().getSource().getConfigoptions().get("sourcetype");
        BaseLogConnector sourceConnector = (BaseLogConnector) facBean.getBean(ctx, sourceType);
        Dataset ds = sourceConnector.readLogData(sparkSession, initializer);

        if (initializer.schemaStruct != null) {

        } else {
            ds = ds
                    // .filter((FilterFunction<Row>) row -> validator.validate(row, accumulator, initBean.partitionIndex))
                    .map(transformerBean, RowEncoder.apply(ds.schema()));
        }
        ds = transformerBean.parse(ds);
        ds.persist(StorageLevel.MEMORY_AND_DISK());


        List<BaseLogConnector> sinkConnectors = new ArrayList<BaseLogConnector>();
        for (LogAggregatorMetadata.Logaggregator.Destination sink : initializer.getMetdadataBean().getDestination()) {
            BaseLogConnector sinkConnector = (BaseLogConnector) facBean.getBean(ctx, sink.getConfigoptions().get("destinationtype"));
            sinkConnectors.add(sinkConnector);
        }
        processSinks(ds, sinkConnectors);
        ds.unpersist();
    }
}
