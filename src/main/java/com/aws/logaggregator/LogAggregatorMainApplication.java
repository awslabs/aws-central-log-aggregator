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
 * Main class of the LogAggregator app
 *
 * @author Kiran S
 */
package com.aws.logaggregator;


import com.aws.logaggregator.factory.BaseBeanFactory;
import com.aws.logaggregator.processor.BaseLogProcessor;
import org.apache.log4j.Logger;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;


@Component
@Configuration
@ComponentScan
@EnableAspectJAutoProxy

public class LogAggregatorMainApplication {


    private static final Logger logger = Logger.getLogger(LogAggregatorMainApplication.class);


    protected SparkSession sparkSession;

    public static void main(String[] args) throws Exception {
        new LogAggregatorMainApplication().startProcessing(args);
    }

    public void startProcessing(String[] args) throws Exception {
        logger.info("Loading Log Aggregator Application Context Begin");

        ApplicationContext ctx =
                new AnnotationConfigApplicationContext(LogAggregatorMainApplication.class);

        logger.info("Loading Log Aggregator Application Context End");
        if (args.length < 4) {
            logger.error("Please pass Region name, table name, Key and the source type (stream or batch)");
            System.exit(0);
        }
        String configParamValues = "";
        if (args.length > 4) {
            configParamValues = args[4];
        }
        try {
            sparkSession = (SparkSession) new BaseBeanFactory().getBean(ctx, "spark-session");
            Set<Callable<String>> callables = new HashSet<>();
            final String region = args[0];
            final String path = args[1];
            final String key = args[2];
            final String sourceType = args[3];
            BaseLogProcessor logProcessorBean = (BaseLogProcessor) new BaseBeanFactory().getBean(ctx, sourceType);

            if (logProcessorBean == null) {
                return;
            }
            try {

                logProcessorBean.initializeEnvironment(path, key, region, configParamValues, sparkSession, ctx);
                logProcessorBean.process(key, ctx);
            } catch (Exception e) {
                logger.error("Exception thrown inside callable", e);
            }
            logger.info("Ending processing of type--> " + args[2]);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception thrown", e);

        } finally {
            logger.info("Closing spark session-->");
            sparkSession.close();
            System.exit(0);
        }
    }
}

