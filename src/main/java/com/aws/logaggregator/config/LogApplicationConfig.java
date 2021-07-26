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
 * Initializer
 *
 * @author Kiran S
 */
package com.aws.logaggregator.config;


import com.aws.logaggregator.security.BaseSecretParam;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import java.io.Serializable;
import java.lang.reflect.Constructor;


@Configuration
public class LogApplicationConfig implements Serializable {


    @Value("${configholder.class.name}")
    public String configholderclassname;
    @Value("${secret.class.name}")
    public String secretclassname;
    @Autowired
    private Environment env;

    @Value("${app.name}")
    private String appName;


    @Value("${master.uri:local}")
    private String masterUri;

    @Value("${spark.serializer}")
    private String sparkSerializer;

    @Value("${spark.hadoop.mapreduce.fileoutputcommitter.algorithm.version}")
    private String fileOutputCommiter;


    @Value("${spark.sql.avro.compression.codec}")
    private String avroCompression;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SparkConf sparkConf() {
        SparkConf sparkConf = new SparkConf()
                .setAppName(appName);

        sparkConf.set("spark.serializer", sparkSerializer);
        sparkConf.set("spark.hadoop.mapreduce.fileoutputcommitter.algorithm.version", fileOutputCommiter);
        sparkConf.set("hive.metastore.client.factory.class", "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory");
        sparkConf.set("hive.exec.dynamic.partition", "true");
        sparkConf.set("hive.exec.dynamic.partition.mode", "nonstrict");
        sparkConf.set("hive.enforce.bucketing", "true");


        sparkConf.set("spark.sql.avro.compression.codec", avroCompression);
        if (masterUri.equals("local")) {
            sparkConf.set("spark.driver.host", "localhost");

        }
        return sparkConf;
    }

    @Bean(name = "spark-session")
    //@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SparkSession sparkSession() {

        if (masterUri.equals("local")) {
            return SparkSession.builder().config(sparkConf()).master("local[*]").getOrCreate();
        }
        return SparkSession.builder().config(sparkConf()).enableHiveSupport().getOrCreate();
    }

    @Bean
    public BaseConfigHolder baseConfigHolder() throws Exception {

        Class<BaseConfigHolder> baseschemaclass = (Class<BaseConfigHolder>) Class.forName(configholderclassname);
        Constructor<BaseConfigHolder> cons = baseschemaclass.getConstructor();

        return cons.newInstance();


    }

    @Bean
    public BaseSecretParam secretParam() throws Exception {
        Class<BaseSecretParam> baseschemaclass = (Class<BaseSecretParam>) Class.forName(secretclassname);
        Constructor<BaseSecretParam> cons = baseschemaclass.getConstructor();
        return cons.newInstance();
    }

    @Configuration
    @Profile("default")
    @PropertySource("classpath:cluster-application.properties")
    static class Defaults {

    }

    @Configuration
    @Profile("dev")
    @PropertySource("classpath:application.properties")
    static class Overrides {
    }


}
