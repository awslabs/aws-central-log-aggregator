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
 * Util class and has the logic for schema based processing
 *
 * @author Kiran S
 */
package com.aws.logaggregator.utils;

import com.aws.logaggregator.model.LogSchema;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import java.util.ArrayList;

public class SchemaBasedParserUtil implements MapFunction<Row, Row> {

    ArrayList<LogSchema.Attributes> attributesList;
    int recordLength;


    public SchemaBasedParserUtil(LogSchema.Schema schema) {
        attributesList = schema.getAttributes();
        recordLength = schema.getRecordLength();


    }

    @Override
    public Row call(Row o) throws Exception {
        String[] attributes = new String[attributesList.size()];
        String data = o.getString(0);


        if (data.length() < recordLength) {

            data = data + String.format("%" + (recordLength - data.length()) + "s", "")
                    .replace(" ", String.valueOf(' '));

        }
        int index = 0;

        for (LogSchema.Attributes attr : attributesList) {
            if (attr.getRange() == null) {
                attributes[index] = data;
                index = index + 1;
            } else {
                int startIndex = Integer.valueOf(attr.getRange().substring(0, attr.getRange().indexOf("-")));
                int endIndex = Integer.valueOf(attr.getRange().substring(attr.getRange().indexOf("-") + 1));
                attributes[index] = data.substring(startIndex, endIndex);
                index = index + 1;
            }


        }

        return RowFactory.create(attributes);


    }
}