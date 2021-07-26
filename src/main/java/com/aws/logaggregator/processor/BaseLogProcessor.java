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
 * Base class and has the logic for processing logs
 *
 * @author Kiran S
 */

package com.aws.logaggregator.processor;


import com.aws.logaggregator.connector.BaseLogConnector;
import com.aws.logaggregator.error.BaseErrorHandler;
import com.aws.logaggregator.exception.LogAggregatorException;
import com.aws.logaggregator.factory.IBeanFactory;
import com.aws.logaggregator.logparser.BaseLogParser;
import com.aws.logaggregator.model.LogAppInitializer;
import com.aws.logaggregator.security.BaseSecretParam;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.CollectionAccumulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Scope(value = "prototype")
public abstract class BaseLogProcessor implements Serializable {




    private static final Logger logger = Logger.getLogger(BaseLogProcessor.class);

    @Autowired
    public BaseSecretParam secretParams;
    protected SparkSession sparkSession;
    @Autowired
    protected LogAppInitializer initializer;
    @Autowired
    protected IBeanFactory facBean;
    //@Autowired

    // protected S3Utils s3Utils;
    protected String processingStatus;
    protected String processingHookStatus;
    protected CollectionAccumulator<Row> accumulator;

    protected BaseLogParser transformerBean;
    protected BaseErrorHandler errorHandler;
    protected long snapshotId;

    public SparkSession getSparkSession() {
        return sparkSession;
    }

    public void setSparkSession(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    public LogAppInitializer getInitBean() {
        return initializer;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public IBeanFactory getFacBean() {
        return facBean;
    }

    public CollectionAccumulator<Row> getAccumulator() {
        return accumulator;
    }

    public void process(String type, ApplicationContext ctx) throws LogAggregatorException {
        logger.info("Data Processing Begin------->" + type);
        if (initializer == null || initializer.logaggregatorMetadata == null) {
            logger.error("EXITING-->Either the metadata doesn't exist or the key--> " + type + "<--you have passed is missing in the metadat");
            return;
        }
    }

    public void initializeEnvironment(String path, String type, String region, String configParamArgs,
                                      SparkSession argSparkSession, ApplicationContext ctx) {

        // loadSchema(schemaInfo, region);
        setSparkSession(argSparkSession);
        if (sparkSession.sparkContext().getConf() != null) {
            SparkConf sparkConf = sparkSession.sparkContext().getConf();
//            if (initBean.getSparkOptions("conf") != null && initBean.getSparkOptions("conf").size() > 0)
//            {
//                for (Map.Entry<String,String> entry: initBean.getSparkOptions("conf").entrySet())
//                {
//                    sparkConf.set(entry.getKey() , entry.getValue());
//                }
//            }
        }

        initializer.initialize(path, type, region, sparkSession);
        loadSecretParams(region, configParamArgs);

        //initializer.setAppContext(ctx);
        String parser = initializer.getMetdadataBean().getSource().getConfigoptions().get("loggroup");
        if (parser != null) {
            transformerBean = (BaseLogParser) facBean.getBean(ctx, parser);
            if (transformerBean == null) {
                transformerBean = (BaseLogParser) facBean.getBean(ctx, "commonparser");
            }
        } else {
            transformerBean = (BaseLogParser) facBean.getBean(ctx, "commonparser");
        }

        transformerBean.setInitializationBeanAndTimeStampData(initializer);
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getProcessingHookStatus() {
        return processingHookStatus;
    }

    public void setProcessingHookStatus(String processingHookStatus) {
        this.processingHookStatus = processingHookStatus;
    }

    protected void processSinks(Dataset newDataset, List<BaseLogConnector> connectors) {
        StringBuffer sinkprocessingStatus = new StringBuffer();
        if (connectors.size() == 0) {
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(connectors.size());
        try {

            Set<Callable<String>> callables = new HashSet<>();

            for (BaseLogConnector hook : connectors) {
                callables.add(new Callable<String>() {
                    public String call() {
                        boolean sinkstatus = true;
                        try {
                            hook.writeLogData(newDataset, sparkSession, initializer);
                        } catch (Exception exception) {
                            logger.error("Exception thrown from SInk-->" + exception, exception);
                            sinkstatus = false;
                        }

                        return hook.getConnectorType() + ":" + hook.getClass().getName() + ":" + sinkstatus;
                    }
                });
            }

            List<Future<String>> futures = executorService.invokeAll(callables);
            for (Future<String> future : futures) {
                String status = future.get();
                sinkprocessingStatus.append(status);
                sinkprocessingStatus.append(",");
                logger.info("Processing Sink completed-->" + status);
            }
            setProcessingHookStatus(sinkprocessingStatus.toString());
        } catch (Exception e) {

            logger.error("Error processing hooks", e);
        } finally {
            executorService.shutdown();
        }

    }

    protected void postProcess(String status) {

    }



    public void loadSecretParams(String region, String configArgs) {
        secretParams.loadSecretParam(region, configArgs);
    }
}
