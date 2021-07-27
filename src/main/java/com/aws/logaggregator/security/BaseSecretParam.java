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
 * Base class for handling secret params
 *
 * @author Kiran S
 */
package com.aws.logaggregator.security;


import org.apache.commons.text.StringSubstitutor;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

@Component("secret-common")
public abstract class BaseSecretParam implements Serializable {

    private final transient Logger logger = Logger.getLogger(this.getClass());
    protected Map<String, String> secrets;
    protected Map<String, String> configValues;

    public abstract void loadSecretParam(String region, String configArgs);

    public String getSecretValue(String SecretName) {
        return secrets.get(SecretName);
    }

    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets;
    }

    public Map<String, String> getConfigValues() {
        return configValues;
    }

    public void setConfigValues(Map<String, String> configValues) {
        this.configValues = configValues;
    }

    public String substituteSecrets(String input) {
        return StringSubstitutor.replace(input, secrets, "${", "}");
    }

    public String substituteConfigArgs(String input) {
        return StringSubstitutor.replace(input, configValues, "${", "}");
    }

}

