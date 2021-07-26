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
 * Util class and has the logic for pattern based processsing
 *
 * @author Kiran S
 */
package com.aws.logaggregator.utils;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternBasedParserUtil implements FlatMapFunction<Row, Row> {


    Pattern PATTERN = null;

    public PatternBasedParserUtil(String argPattern) {


        PATTERN = Pattern.compile(argPattern);

    }

    @Override

    public Iterator<Row> call(Row o) throws Exception {

        String logline = o.getString(0);

        List<Row> list = new ArrayList<>();
        Matcher m = PATTERN.matcher(logline);


        while (m.find()) {

            String[] attributes = new String[m.groupCount() + 1];

            for (int i = 0; i < m.groupCount(); i++) {

                attributes[i] = m.group(i + 1);
            }
            list.add(RowFactory.create(attributes));


        }

        return list.iterator();
    }
}
