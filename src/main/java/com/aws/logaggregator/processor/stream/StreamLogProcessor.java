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
 * Base class and has the logic for processing logs from Streams
 *
 * @author Kiran S
 */
package com.aws.logaggregator.processor.stream;

import com.aws.logaggregator.connector.BaseLogConnector;
import com.aws.logaggregator.exception.LogAggregatorException;
import com.aws.logaggregator.model.LogAggregatorMetadata;
import com.aws.logaggregator.model.LogAppInitializer;
import com.aws.logaggregator.processor.BaseLogProcessor;
import com.aws.logaggregator.utils.PatternBasedParserUtil;
import com.aws.logaggregator.utils.SchemaBasedParserUtil;
import com.databricks.spark.xml.XmlReader;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.VoidFunction2;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.streaming.StreamingQueryListener;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.storage.StorageLevel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for processing stream based sources.
 */

@Component("stream")
@Scope(value = "prototype")
public class StreamLogProcessor extends BaseLogProcessor {

    private static final Logger logger = Logger.getLogger(StreamLogProcessor.class);


    @Override
    public void process(String type, ApplicationContext ctx) throws LogAggregatorException {

        logger.info("Data Processing Begin------->" + type);
        if (initializer == null || initializer.getMetdadataBean() == null) {
            logger.error("EXITING-->Metadata doesn't exist or the key--> " + type + "<--you have passed is missing in the metadata");
            return;
        }
//        if (initializer.schemaStruct == null) {
//            logger.error("SCHEMA IS NOT DEFINED FOR STREAM PROCESSING --> EXITING");
//            return;
//        }
        String sourceType = initializer.getMetdadataBean().getSource().getConfigoptions().get("sourcetype");
        if ("storage".equalsIgnoreCase(sourceType)) {
            sourceType = sourceType + "streaming";
        }
        String dataformat = initializer.getMetdadataBean().getSource().getConfigoptions()
                .get("logformat");
        String pattern = initializer.getMetdadataBean().getSource().getConfigoptions()
                .get("pattern");
        String flatten = initializer.getMetdadataBean().getSource().getConfigoptions()
                .get("flatten");
        String checkpointlocation = initializer.getMetdadataBean().getSource().getConfigoptions()
                .get("checkpointlocation");
        String decompress = initializer.getMetdadataBean().getSource().getConfigoptions()
                .get("decompress");
        BaseLogConnector sourceConnector = (BaseLogConnector) facBean.getBean(ctx, sourceType);
        Dataset ds = sourceConnector.readLogData(sparkSession, initializer);

//       Dataset finaldf  =  ds
//                //.filter((FilterFunction<Row>) row -> validator.validate(row, accumulator, initBean.partitionIndex))
//                .map(transformerBean, RowEncoder.apply(initializer.schemaStruct));
        try {
            writeOutput(ds, initializer, ctx, dataformat, decompress, pattern, sourceType, checkpointlocation);
        } catch (Exception e) {
            logger.error("Exception in Streaming-->", e);
        }


    }


    protected void writeOutput(Dataset ds,
                               LogAppInitializer initializationBean, ApplicationContext ctx,
                               String dataformat, String decompress, String pattern, String flatten, String checkpointlocation) throws Exception {
        sparkSession.streams().addListener(new StreamingQueryListener() {
            @Override
            public void onQueryStarted(QueryStartedEvent queryStarted) {
                System.out.println("Query started: " + queryStarted.id());
            }

            @Override
            public void onQueryTerminated(QueryTerminatedEvent queryTerminated) {
                System.out.println("Query terminated: " + queryTerminated.id());
            }

            @Override
            public void onQueryProgress(QueryProgressEvent queryProgress) {

                if (queryProgress.progress().numInputRows() > 0) {
                    //System.out.println("Query made progress and came here for processing rows: " + queryProgress.progress());
                }

            }
        });

        List<BaseLogConnector> sinkConnectors = new ArrayList<BaseLogConnector>();
        for (LogAggregatorMetadata.Logaggregator.Destination destination : initializer.getMetdadataBean().getDestination()) {
            BaseLogConnector sinkConnector = (BaseLogConnector) facBean.getBean(ctx, destination.getConfigoptions().get("destinationtype"));
            sinkConnectors.add(sinkConnector);
        }


        ds.writeStream().foreachBatch(
                new VoidFunction2<Dataset, Long>() {
                    @Override
                    public void call(Dataset fdsarg, Long v2) {
                        String finalstatus = "";
                        if ("true".equalsIgnoreCase(decompress)) {
                            fdsarg = fdsarg.withColumn("data",
                                    org.apache.spark.sql.functions.callUDF("decompress", fdsarg.col("data")));

                        }
                        accumulator = sparkSession.sparkContext().collectionAccumulator();
                        try {
                            Dataset fds = null;
                            if ("xml".equalsIgnoreCase(dataformat)) {
                                fds = fdsarg.as(Encoders.STRING());
                                Dataset xmldf = new XmlReader().xmlRdd(fds.sqlContext(), fds.rdd());
                                if ("true".equalsIgnoreCase(flatten)) {


                                    fds = flattendf(xmldf);
                                    if (!fds.isEmpty()) {
                                        fds = transformerBean.parse(fds);
                                    }

                                } else {
                                    if (!fds.isEmpty()) {
                                        fds = transformerBean.parse(xmldf);
                                    }
                                }


                            } else if ("json".equalsIgnoreCase(dataformat)) {
                                fds = fdsarg.as(Encoders.STRING());

                                if (fds.count() > 0 && !fds.isEmpty()) {
                                    fds = sparkSession.read().json(fds);
                                    fds = transformerBean.parse(fds);


                                }

                            } else {

                                fds = fdsarg;
                                if (pattern != null) {
                                    fds = fds.flatMap(
                                            new PatternBasedParserUtil(pattern), Encoders.bean(Row.class));
                                } else {
                                    if ("text".equals(dataformat))
                                        fds = fds.map(new SchemaBasedParserUtil(initializationBean.schema), Encoders.bean(Row.class));
                                }
                                if (initializer.schemaStruct != null) {
                                    fds = fds.map(transformerBean, RowEncoder.apply(initializer.schemaStruct));
                                } else {
                                    fds = fds.map(transformerBean, RowEncoder.apply(ds.schema()));
                                }
                                fds = transformerBean.parse(fds);
                            }


                            if (!fds.isEmpty() && fds.columns().length > 0) {
                                fds.persist(StorageLevel.MEMORY_AND_DISK());
                                fds.show();
                                processSinks(fds, sinkConnectors);
                                fds.unpersist();
                            }
                        } catch (Exception e) {
                            finalstatus = "EXCEPTION";
                            logger.error("process exception-->", e);
                            //throw e;

                        } finally {
                            postProcess(finalstatus);

                        }

                    }
                }).option("checkpointLocation", checkpointlocation).start().awaitTermination();


        //  }
        // return query;
    }

    protected Dataset flattendf(Dataset ds) {


        StructField[] fields = ds.schema().fields();

        List<String> fieldsNames = new ArrayList<>();
        for (StructField s : fields) {
            fieldsNames.add(s.name());
        }

        for (int i = 0; i < fields.length; i++) {

            StructField field = fields[i];
            DataType fieldType = field.dataType();
            String fieldName = field.name();

            if (fieldType instanceof ArrayType) {
                List<String> fieldNamesExcludingArray = new ArrayList<String>();
                for (String fieldName_index : fieldsNames) {
                    if (!fieldName.equals(fieldName_index))
                        fieldNamesExcludingArray.add(fieldName_index);
                }

                List<String> fieldNamesAndExplode = new ArrayList<>(fieldNamesExcludingArray);
                String s = String.format("explode_outer(%s) as %s", fieldName, fieldName);
                fieldNamesAndExplode.add(s);

                String[] exFieldsWithArray = new String[fieldNamesAndExplode.size()];
                Dataset exploded_ds = ds.toDF().selectExpr(fieldNamesAndExplode.toArray(exFieldsWithArray));

                return flattendf(exploded_ds);

            } else if (fieldType instanceof StructType) {

                String[] childFieldnames_struct = ((StructType) fieldType).fieldNames();

                List<String> childFieldnames = new ArrayList<>();
                for (String childName : childFieldnames_struct) {
                    childFieldnames.add(fieldName + "." + childName);
                }

                List<String> newfieldNames = new ArrayList<>();
                for (String fieldName_index : fieldsNames) {
                    if (!fieldName.equals(fieldName_index))
                        newfieldNames.add(fieldName_index);
                }

                newfieldNames.addAll(childFieldnames);

                List<Column> renamedStrutctCols = new ArrayList<>();

                for (String newFieldNames_index : newfieldNames) {
                    renamedStrutctCols.add(new Column(newFieldNames_index).as(newFieldNames_index.replace(".", "_")));
                }

                Seq renamedStructCols_seq = JavaConverters.collectionAsScalaIterableConverter(renamedStrutctCols).asScala().toSeq();

                Dataset ds_struct = ds.toDF().select((Column)renamedStructCols_seq);
                return flattendf(ds_struct);
            } else {

            }

        }
        return ds;
    }


}
