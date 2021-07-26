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
 * Common Log Parser
 *
 * @author Kiran S
 */
package com.aws.logaggregator.logparser;


import com.aws.logaggregator.model.LogAppInitializer;
import com.aws.logaggregator.model.LogSchema;
import com.aws.logaggregator.utils.BaseUtils;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.functions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

@Component("commonparser")
@Scope(value = "prototype")
public class BaseLogParser implements MapFunction<Row, Row>, MapPartitionsFunction {

    private static final Logger logger = Logger.getLogger(BaseLogParser.class);
    @Autowired
    protected BaseUtils baseUtils;
    protected LogAppInitializer initializationBean;

    protected Timestamp create_timestamp;


    @PostConstruct
    public void init() {

    }

    //override this method for custom transformation
    protected Row parse(Row record) {

        return record;
    }

    //override this method for custom transformation
    public Dataset parse(Dataset record) {


        String explode = initializationBean.getMetdadataBean().getSource().getConfigoptions().get("explode");
        if (explode != null && !"".equals(explode) && Arrays.asList(record.columns()).contains(explode)) {

            record = record.select(org.apache.spark.sql.functions.explode(record.col(explode)).as(explode));


        }
        Date utilDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String stringDate = dateFormat.format(utilDate);
        record = record.withColumn("event_timestamp", functions.lit(stringDate));
        return record;


    }

    @Override
    public Row call(Row row) throws Exception {


        if (initializationBean.schema == null || null == initializationBean.schema.getDeriveby()) {
            return parse(row);
        }


        Object[] values = new Object[initializationBean.schema.getAttributes().size() + 2];
        int index = 0;

        for (LogSchema.Attributes attr : initializationBean.schema.getAttributes()) {

            String data = "";

            if ("Name".equalsIgnoreCase(initializationBean.schema.getDeriveby())) {
                Object dataObj = row.getAs(attr.getName());
                if (dataObj != null) {
                    data = String.valueOf(dataObj);
                }


            } else if ("Index".equalsIgnoreCase(initializationBean.schema.getDeriveby())) {
                if (row.get(attr.getIndex()) != null) {
                    data = String.valueOf(row.get(attr.getIndex()));

                }

            }

            index = attr.getIndex();

            if ((data == null || "".equals(data.trim())) && (attr.getDefaultvalue() != null)) {

                data = String.valueOf(attr.getDefaultvalue());
            }

            try {
                switch (attr.getType().toLowerCase()) {

                    case "string":
                        if (attr.isEncryption()) {
                            values[index] = baseUtils.encrypt(data);
                        } else if (attr.isDecryption()) {
                            values[index] = baseUtils.decrypt(data);
                        } else {
                            values[index] = data;
                        }

                        break;
                    case "integer":
                        if (data != null && !"".equals(data.trim())) {

                            values[index] = Integer.valueOf(data);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "int":
                        if (data != null && !"".equals(data.trim())) {
                            values[index] = Integer.valueOf(data);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "decimal":
                        if (data != null && !"".equals(data.trim())) {
                            values[index] = baseUtils.basicDecimaltranformation(data, attr);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "bigdecimal":
                        if (data != null && !"".equals(data.trim())) {
                            values[index] = baseUtils.basicDecimaltranformation(data, attr);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "biginteger":
                        if (data != null && !"".equals(data.trim())) {
                            values[index] = new BigInteger(data);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "bigint":
                        if (data != null && !"".equals(data.trim())) {
                            values[index] = new BigInteger(data);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "double":
                        if (attr.getScale() > 0) {
                            if (data != null && !"".equals(data.trim())) {
                                values[index] = (Double.parseDouble(data) / Math.pow(10, attr.getScale()));
                            } else {
                                values[index] = null;
                            }
                        } else {
                            if (data != null && !"".equals(data.trim())) {
                                values[index] = Double.parseDouble(data);
                            } else {
                                values[index] = null;
                            }
                        }
                        break;
                    case "long":
                        if (data != null && !"".equals(data.trim())) {
                            values[index] = Long.valueOf(data);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "boolean":
                        if (data != null && !"".equals(data.trim())) {
                            values[index] = Boolean.valueOf(data);
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "date":
                        if (data != null && !"".equalsIgnoreCase(data.trim())) {
                            values[index] = new java.sql.Date(new SimpleDateFormat(attr.getFormat()).parse(data).getTime());
                        } else {
                            values[index] = null;
                        }
                        break;
                    case "timestamp":
                        if (data != null && !"".equalsIgnoreCase(data.trim())) {
                            values[index] = new Timestamp(new SimpleDateFormat(attr.getFormat()).parse(data).getTime());
                        } else {
                            values[index] = null;
                        }
                        break;
                    default:
                        values[index] = data;
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                values[index] = data;
            }
        }


        index = index + 1;

        values[index] = create_timestamp;


        return parse(RowFactory.create(values));


    }

    public void setInitializationBeanAndTimeStampData(LogAppInitializer initBean) {
        initializationBean = initBean;

        java.util.Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(date);
        Timestamp ct = baseUtils.getCurrentSQlTimestamp();
        try {
            create_timestamp = ct;


        } catch (Exception e) {
            logger.error("Error Setting timestanp data-->", e);
        }
    }

    @Override
    public Iterator call(Iterator iterator) throws Exception {

        return iterator;
    }
}
