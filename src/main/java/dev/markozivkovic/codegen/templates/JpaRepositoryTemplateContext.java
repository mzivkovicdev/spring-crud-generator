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
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;

public class JpaRepositoryTemplateContext {

    private JpaRepositoryTemplateContext() {}
    
    /**
     * Creates a template context for the JPA interface of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the JPA interface
     */
    public static Map<String, Object> computeJpaInterfaceContext(final ModelDefinition modelDefinition) {
    
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Repository");
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        
        return context;
    }
}
