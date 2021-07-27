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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope(value = "prototype")
public class S3Utils extends BaseUtils {


    private static final Logger logger = Logger.getLogger(S3Utils.class);


    public boolean movefiles(String clientRegion, String sourceBucketName, String destinationBucketName) {

        try {
            logger.info("Moving S3 files from -->" + sourceBucketName + "-->to-->" + destinationBucketName);

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new DefaultAWSCredentialsProviderChain())
                    .withRegion(clientRegion)
                    .build();


            List<String> files = getListofFiles(sourceBucketName);

            for (String key : files) {
                // Copy the object into a new object in the same bucket.
                CopyObjectRequest copyObjRequest = new CopyObjectRequest(sourceBucketName, key, destinationBucketName, key);
                DeleteObjectRequest delRequest = new DeleteObjectRequest(sourceBucketName, key);
                s3Client.copyObject(copyObjRequest);
                s3Client.deleteObject(delRequest);
            }

            logger.info("Moving S3 files Success -->" + sourceBucketName + "-->to-->" + destinationBucketName);
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            logger.error(e.getMessage(), e);
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            logger.error(e.getMessage(), e);
        }
        return true;
    }


    public List<String> getListofFiles(String bucketName) {
        if (bucketName == null) {
            return null;
        }
        ArrayList<String> files = new ArrayList<String>();

        StringBuffer fileNameBuffer = new StringBuffer();
        try {
            AmazonS3 s3client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
            String rootbucketName = bucketName.substring(0, bucketName.indexOf("/"));
            String prefix = bucketName.substring(bucketName.indexOf("/") + 1);
            Iterable<S3ObjectSummary> objectSummaries = S3Objects.withPrefix(s3client, rootbucketName, prefix);
            for (S3ObjectSummary objectSummary : objectSummaries) {


                if (objectSummary.getKey() != null) {
                    String key = objectSummary.getKey();
                    int lastIndex = key.lastIndexOf("/");
                    if (lastIndex > 0 && lastIndex + 1 != key.length()) {
                        String fileName = key.substring(key.lastIndexOf("/") + 1);
                        files.add(fileName);
                        ///fileNameBuffer.append( fileName );
                        //fileNameBuffer.append( "," );
                    }


                }


            }


        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return files;
    }
}
