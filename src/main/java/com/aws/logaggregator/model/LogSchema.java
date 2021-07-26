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
 * Logging App Schema Object
 * @author Kiran S
 */
package com.aws.logaggregator.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Schema object corresponding to schema.json files
 */
public class LogSchema implements Serializable {


    Schema schema;
    int length;
    int version;


    public LogSchema() {

    }


    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public static class Schema implements Serializable {
        String loggroup;


        String deriveby;
        ArrayList<Attributes> attributes;
        int recordLength;
        boolean enrichWithTimestampData;

        public Schema() {

        }


        public boolean isEnrichWithTimestampData() {
            return enrichWithTimestampData;
        }

        public void setEnrichWithTimestampData(boolean enrichWithTimestampData) {
            this.enrichWithTimestampData = enrichWithTimestampData;
        }

        public String getDeriveby() {
            return deriveby;
        }

        public void setDeriveby(String deriveby) {
            this.deriveby = deriveby;
        }


        public String getLoggroup() {
            return loggroup;
        }

        public void setLoggroup(String loggroup) {
            this.loggroup = loggroup;
        }

        public ArrayList<Attributes> getAttributes() {
            return attributes;
        }

        public void setAttributes(ArrayList<Attributes> attributes) {
            this.attributes = attributes;
        }

        public int getRecordLength() {
            return recordLength;
        }

        public void setRecordLength(int recordLength) {
            this.recordLength = recordLength;
        }
    }

    public static class Attributes implements Serializable {
        String name;
        boolean mandatory;
        String range;
        int length;
        String type;
        String updatedName;
        boolean encryption;
        boolean decryption;

        String format;
        int index;
        String defaultvalue;
        int scale = 0;
        String influxtype;

        public Attributes() {

        }

        public boolean isDecryption() {
            return decryption;
        }

        public void setDecryption(boolean decryption) {
            this.decryption = decryption;
        }

        public boolean isEncryption() {
            return encryption;
        }

        public void setEncryption(boolean encryption) {
            this.encryption = encryption;
        }

        public int getScale() {
            return scale;
        }

        public void setScale(int scale) {
            this.scale = scale;
        }

        public String getInfluxtype() {
            return influxtype;
        }

        public void setInfluxtype(String influxtype) {
            this.influxtype = influxtype;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getDefaultvalue() {
            return defaultvalue;
        }

        public void setDefaultvalue(String defaultvalue) {
            this.defaultvalue = defaultvalue;
        }

        public boolean isMandatory() {
            return mandatory;
        }

        public void setMandatory(boolean mandatory) {
            this.mandatory = mandatory;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }


        public String getRange() {
            return range;
        }

        public void setRange(String range) {
            this.range = range;
        }


        public String getUpdatedName() {
            return updatedName;
        }

        public void setUpdatedName(String updatedName) {
            this.updatedName = updatedName;
        }
    }
}
