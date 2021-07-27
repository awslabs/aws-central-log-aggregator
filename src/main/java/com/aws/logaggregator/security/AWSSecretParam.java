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
 * AWSSecretParam class for handling secret params through AWS Simple Systems Management
 *
 * @author iftik
 */
package com.aws.logaggregator.security;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.*;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class AWSSecretParam extends BaseSecretParam {


    private static final Logger logger = Logger.getLogger(AWSSecretParam.class);

    @Value("${master.uri:local}")
    private String masterUri;

    @BeforeClass
    public static void setSetRegion(String region) {
        System.setProperty("aws.region", region);
    }

    private Object getPropertyByName(String propertyName, boolean isSecure) {
        // Let it throw exception this is used in base bean initialization
        Object val = null;
        try {
            AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
            GetParameterRequest parameterRequest = new GetParameterRequest().withName(propertyName).withWithDecryption(isSecure);
            GetParameterResult parameterResult = ssmClient.getParameter(parameterRequest);
            val = parameterResult.getParameter().getValue();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.debug("System Parameters not found for" + propertyName +
                    "-->"
                    + ex.getMessage());
        }
        return val;

    }

    private List<Parameter> getPropertyByPath(String pathName, boolean isSecure) {
        // Let it throw exception this is used in base bean initialization
        List<Parameter> val = null;
        try {
            GetParametersByPathRequest parametersByPathRequest = new GetParametersByPathRequest().withPath(pathName).withWithDecryption(isSecure);
            AWSSimpleSystemsManagement ssmclient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
            GetParametersByPathResult parameterResult = ssmclient.getParametersByPath(parametersByPathRequest);
            val = parameterResult.getParameters();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.debug("System Parameters not found for" + pathName +
                    "-->"
                    + ex.getMessage());
        }
        return val;
    }

    @Override
    public void loadSecretParam(String region, String configArgs) {
        if (!masterUri.equalsIgnoreCase("local")) {
            Map<String, String> secrets = new HashMap<String, String>();
            List<Parameter> propList = getPropertyByPath("/" + "awslogaggregator" + "/", true);
            if (propList != null && propList.size() > 0) {
                for (Parameter param : propList
                ) {
                    String key = param.getName().replace("/awslogaggregator/", "");
                    String value = param.getValue();
                    secrets.put(key, value);
                }

                setSecrets(secrets);
            } else {
                System.out.println("No Paramaters found--->");
            }
        }
        if (configArgs != null && !"".equals(configArgs.trim())) {

            StringTokenizer st = new StringTokenizer(configArgs, "|");
            Map<String, String> configValues = new HashMap<String, String>();
            while (st != null && st.hasMoreTokens()) {

                String data = st.nextToken();
                if (data != null && data.indexOf("#") > 0 && data.length() >= 3) {
                    String key = data.substring(0, data.indexOf("#"));
                    String value = data.substring(data.indexOf("#") + 1);
                    configValues.put(key, value);
                }
            }
            setConfigValues(configValues);
        }
    }
}
