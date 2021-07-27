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
 * AWSSecretManagerParam class for handling secret params through AWS Secrets Manager
 *
 * @author iftik
 */
package com.aws.logaggregator.security;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AWSSecretManagerParam extends BaseSecretParam {


    private static final Logger logger = Logger.getLogger(AWSSecretManagerParam.class);

    Pattern pattern = Pattern.compile("^[$][{](.*)[}]$");

    @Value("${master.uri:local}")
    private String masterUri;

    @BeforeClass
    public static void setSetRegion(String region) {
        System.setProperty("aws.region", region);
    }

    private String getSecrets(String secretName, String region, String key) {

        Map<String, String> secretsMap = null;
        String secret;

        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                .withRegion(region)
                .build();

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretName);
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = client.getSecretValue(getSecretValueRequest);

            if (getSecretValueResult.getSecretString() != null) {
                secret = getSecretValueResult.getSecretString();
            } else {
                secret = new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
            }
            ObjectMapper mapper = new ObjectMapper();
            secretsMap = mapper.readValue(secret, Map.class);

        } catch (DecryptionFailureException | InternalServiceErrorException | InvalidParameterException | InvalidRequestException | ResourceNotFoundException e) {
            logger.error("exception thron from secret manager", e);
            throw e;
        } catch (JsonParseException | JsonMappingException e) {
            logger.error("exception thron from secret manager", e);

        } catch (IOException e) {
            logger.error("exception thron from secret manager", e);
        } catch (Exception e) {
            logger.error("exception thron from secret manager", e);
        }
        return secretsMap.get(key);

    }

    @Override
    public String substituteSecrets(String input) {
        Map<String, Object> config = null;
        ObjectMapper mapper = new ObjectMapper();
        try {

            config = mapper.readValue(input, Map.class);
            config = fillSecrets(config);
            return mapper.writeValueAsString(config);
        } catch (JsonParseException | JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }

    public Map<String, Object> fillSecrets(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                fillSecrets((Map<String, Object>) entry.getValue());
            } else if (entry.getValue() instanceof List) {
                List<Object> list = (List<Object>) entry.getValue();
                list.forEach((temp) -> {
                    fillSecrets((Map<String, Object>) temp);
                });
            } else {
                Matcher matcher = pattern.matcher("" + entry.getValue());
                if (matcher.matches()) {
                    System.out.println(matcher.group(1));
                    String secretPath = matcher.group(1);
                    String[] pathTokens = secretPath.split("\\|");
                    entry.setValue(getSecrets(pathTokens[0], pathTokens[1], pathTokens.length > 2 ? pathTokens[2] : null));
                }
            }
        }
        return map;
    }


    @Override
    public void loadSecretParam(String region, String configArgs) {
    }
}
