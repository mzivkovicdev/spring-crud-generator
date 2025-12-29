/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.Map;

import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

public class DataGeneratorTemplateContext {

    private DataGeneratorTemplateContext() {}
    
    /**
     * Computes a template context for a unit test data generator based on a TestDataGeneratorConfig object.
     * 
     * @param config the TestDataGeneratorConfig object
     * @return a template context containing the name of the data generator, the name of the random field name,
     *         the name of the single object method, and the name of the list method
     */
    public static Map<String, Object> computeDataGeneratorContext(final TestDataGeneratorConfig config) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.DATA_GENERATOR, config.generator());
        context.put(TemplateContextConstants.DATA_GENERATOR_FIELD_NAME, config.randomFieldName());
        context.put(TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ, config.singleObjectMethodName());
        context.put(TemplateContextConstants.DATA_GENERATOR_LIST_METHOD, config.multipleObjectsMethodName());
        
        return context;
    }

}
