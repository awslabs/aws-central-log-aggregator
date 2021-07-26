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

/*
 * Logging App Config Initializer
 * @author Kiran S
 */

package com.aws.logaggregator.model;

import com.aws.logaggregator.config.BaseConfigHolder;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Component
@Scope(value = "prototype")
public class LogAppInitializer implements Serializable {


    public LogAggregatorMetadata logaggregatorMetadata;

    @Autowired
    public BaseConfigHolder baseConfigHolder;


    public LogSchema.Schema schema;

    public StructType schemaStruct;


    @PostConstruct
    public void init() {

    }


    private DataType getDataType(String val, String format) {

        switch (val.toLowerCase()) {
            case "string":
                return DataTypes.StringType;
            case "boolean":
                return DataTypes.BooleanType;
            case "integer":
                return DataTypes.IntegerType;
            case "int":
                return DataTypes.IntegerType;
            case "double":
                return DataTypes.DoubleType;
            case "long":
                return DataTypes.LongType;
            case "decimal": {
                if (format != null && format.indexOf(",") > 0) {
                    int precision = Integer.valueOf(format.substring(0, format.indexOf(",")));
                    int scale = Integer.valueOf(format.substring(format.indexOf(",") + 1));
                    return DataTypes.createDecimalType(precision, scale);
                }
                return DataTypes.createDecimalType();

            }
            case "bigint": {
                return DataTypes.LongType;
            }
            case "biginteger": {
                return DataTypes.LongType;
            }
            case "bigdecimal": {
                if (format != null && format.indexOf(",") > 0) {
                    int precision = Integer.valueOf(format.substring(0, format.indexOf(",")));
                    int scale = Integer.valueOf(format.substring(format.indexOf(",") + 1));
                    return DataTypes.createDecimalType(precision, scale);
                }
                return DataTypes.createDecimalType();

            }

            case "date":
                return DataTypes.DateType;
            case "timestamp":
                return DataTypes.TimestampType;

            default:
                return DataTypes.StringType;
        }
    }


    public void initialize(String path, String key, String region, SparkSession sparkSession) {


        baseConfigHolder.loadLogConfigFiles(path, key, region);
        logaggregatorMetadata = baseConfigHolder.getLogAggregatorMetadata();


        schemaStruct = getSchema();

        if (baseConfigHolder.getLogSchema() != null) {
            schema = baseConfigHolder.getLogSchema().schema;
        }


    }


    public StructType getSchema() {
        if (baseConfigHolder == null || baseConfigHolder.getLogSchema() == null) {
            return null;
        }

        List<StructField> fields = new ArrayList<>();
        int index = 0;
        for (LogSchema.Attributes a : baseConfigHolder.getLogSchema().getSchema().getAttributes()) {
//            if(config.getPartitions() != null)
//            {
//                for(String str:config.getPartitions())
//                {
            if (a.isMandatory()) {
                // partitionIndex.put(a.getName(), index);
            }
//                }
//            }


            StructField field = null;
            if (a.getUpdatedName() != null && !"".equals(a.getUpdatedName())) {
                field = DataTypes.createStructField(a.getUpdatedName(), getDataType(a.getType(), a.getFormat()), !a.isMandatory());
            } else {
                field = DataTypes.createStructField(a.getName(), getDataType(a.getType(), a.getFormat()), !a.isMandatory());
            }

            fields.add(field);
            index = index + 1;
        }

        // StructField create_timestampfield = DataTypes.createStructField("event_timestamp", DataTypes.TimestampType, true);

        //fields.add(create_timestampfield);


        StructType schema = DataTypes.createStructType(fields);

        return schema;
    }


    public LogAggregatorMetadata.Logaggregator getMetdadataBean() {
        return logaggregatorMetadata.logaggregator;
    }

    public LogAggregatorMetadata.Logaggregator.Destination getDestination(String type) {
        for (LogAggregatorMetadata.Logaggregator.Destination destination : logaggregatorMetadata.logaggregator.getDestination()) {
            if (destination.getConfigoptions().get("destinationtype").equals(type)) {
                return destination;
            }
        }
        return null;

    }


}
