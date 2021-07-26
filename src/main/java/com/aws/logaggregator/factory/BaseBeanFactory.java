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
 * Bean Factory class
 *
 * @author Kiran S
 */

package com.aws.logaggregator.factory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Factory class
 *
 * @param <E>
 */
@Component
@Scope(value = "prototype")
public class BaseBeanFactory<E> implements IBeanFactory<E> {
    @Override
    public E getBean(ApplicationContext ctx, String type) {
        try {
            return (E) ctx.getBean(type);
        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }
    }
}
