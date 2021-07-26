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
 * VPCFlowLog Log Parser
 *
 * @author Kiran S
 */
package com.aws.logaggregator.logparser;

import org.apache.spark.sql.Dataset;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("vpcflow")
@Scope(value = "prototype")
public class VpcFlowLogParser extends BaseLogParser {

    public Dataset parse(Dataset record) {
        if (record.col("start") != null)
            record = record.withColumn("start", org.apache.spark.sql.functions.from_unixtime(record.col("start").divide(1000), "yyyy-MM-dd'T'HH:mm:ssZ"));
        if (record.col("end") != null)
            record = record.withColumn("end", org.apache.spark.sql.functions.from_unixtime(record.col("end").divide(1000), "yyyy-MM-dd'T'HH:mm:ssZ"));

        return record;
    }


}

