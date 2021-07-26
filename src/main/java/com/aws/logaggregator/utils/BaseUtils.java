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

package com.aws.logaggregator.utils;

import com.aws.logaggregator.model.LogSchema;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class BaseUtils implements Serializable {


    public BigDecimal basicDecimaltranformation(String value, LogSchema.Attributes attr) {


        BigDecimal val = new BigDecimal(value);

        if (attr.getScale() > 0) {
            val.setScale(attr.getScale());
        }

        return val;
    }

    public java.sql.Timestamp getCurrentSQlTimestamp() {
        Date utilDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String stringDate = dateFormat.format(utilDate);
        final java.sql.Timestamp sqlDate = java.sql.Timestamp.valueOf(stringDate);
        return sqlDate;
    }

    public String encrypt(final String data) {
        return data;
    }

    public String decrypt(final String data) {
        return data;
    }
}
