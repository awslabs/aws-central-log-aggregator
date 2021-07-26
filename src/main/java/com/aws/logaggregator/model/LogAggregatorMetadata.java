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
 * LogAggrgatorMetadata
 *
 * @author Kiran S
 */
package com.aws.logaggregator.model;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Component
@Scope(value = "prototype")
public class LogAggregatorMetadata implements Serializable {
    /**
     * Metadata
     */


    Logaggregator logaggregator;

    public Logaggregator getLogaggregator() {
        return logaggregator;
    }

    public void setLogaggregator(Logaggregator logaggregator) {
        this.logaggregator = logaggregator;
    }

    public static class Logaggregator implements Serializable {

        Map<String, String> engineconfiguration;
        Map<String, String> errorhandler;
        Map<String, String> postprocessing;
        List<Destination> destination;
        Source source;

        public Map<String, String> getErrorhandler() {
            return errorhandler;
        }

        public void setErrorhandler(Map<String, String> errorhandler) {
            this.errorhandler = errorhandler;
        }

        public Map<String, String> getPostprocessing() {
            return postprocessing;
        }

        public void setPostprocessing(Map<String, String> postprocessing) {
            this.postprocessing = postprocessing;
        }

        public Map<String, String> getEngineconfiguration() {
            return engineconfiguration;
        }

        public void setEngineconfiguration(Map<String, String> engineconfiguration) {
            this.engineconfiguration = engineconfiguration;
        }

        public Source getSource() {
            return source;
        }

        public void setSource(Source source) {
            this.source = source;
        }

        public List<Destination> getDestination() {
            return destination;
        }

        public void setDestination(List<Destination> destination) {
            this.destination = destination;
        }

        public static class Source implements Serializable {


            Map<String, String> configoptions;

            Map<String, String> sparkoptions;

            public Map<String, String> getConfigoptions() {
                return configoptions;
            }

            public void setConfigoptions(Map<String, String> configoptions) {
                this.configoptions = configoptions;
            }

            public Map<String, String> getSparkoptions() {
                return sparkoptions;
            }

            public void setSparkoptions(Map<String, String> sparkoptions) {
                this.sparkoptions = sparkoptions;
            }
        }

        public static class Destination implements Serializable {

            Map<String, String> configoptions;

            Map<String, String> sparkoptions;

            public Map<String, String> getConfigoptions() {
                return configoptions;
            }

            public void setConfigoptions(Map<String, String> configoptions) {
                this.configoptions = configoptions;
            }

            public Map<String, String> getSparkoptions() {
                return sparkoptions;
            }

            public void setSparkoptions(Map<String, String> sparkoptions) {
                this.sparkoptions = sparkoptions;
            }
        }
    }

}
