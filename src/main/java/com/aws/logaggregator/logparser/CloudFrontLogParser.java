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
 * CloudFront Log Parser
 *
 * @author Kiran S
 */
package com.aws.logaggregator.logparser;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.functions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.concat;


@Component("cloudfront")
@Scope(value = "prototype")
public class CloudFrontLogParser extends BaseLogParser {

    public Dataset parse(Dataset record) {

        record = super.parse(record);
        record = record.withColumn("timestamp", concat(col("date"), functions.lit("T"), col("time"), functions.lit("Z")));
        return record;
    }
}
