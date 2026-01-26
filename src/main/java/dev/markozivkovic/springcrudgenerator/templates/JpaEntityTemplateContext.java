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

package dev.markozivkovic.springcrudgenerator.templates;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;

public class JpaEntityTemplateContext {
    
    private JpaEntityTemplateContext() {}

    /**
     * Creates a template context for the JPA model of a given model definition.
     * 
     * @param modelDefinition the model definition containing class and field details
     * @return a map representing the context for the JPA model
     */
    public static Map<String, Object> computeJpaModelContext(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.FIELDS, modelDefinition.getFields());
        context.put(TemplateContextConstants.FIELD_NAMES, FieldUtils.extractFieldNames(modelDefinition.getFields()));
        context.put(TemplateContextConstants.CLASS_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        try {
            context.put(TemplateContextConstants.INPUT_ARGS, FieldUtils.generateInputArgsExcludingId(modelDefinition.getFields()));
            context.put(TemplateContextConstants.NON_ID_FIELD_NAMES, FieldUtils.extractNonIdFieldNames(modelDefinition.getFields()));
        } catch (final IllegalArgumentException e) {
            context.put(TemplateContextConstants.INPUT_ARGS, FieldUtils.generateInputArgsWithoutRelations(modelDefinition.getFields()));
            context.put(TemplateContextConstants.NON_ID_FIELD_NAMES, FieldUtils.extractFieldNames(modelDefinition.getFields()));
        }
        if (Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled()) {
            context.put(TemplateContextConstants.AUDIT_ENABLED, modelDefinition.getAudit().isEnabled());
            context.put(TemplateContextConstants.AUDIT_TYPE, AuditUtils.resolveAuditType(modelDefinition.getAudit().getType()));
        }

        if (FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
            context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        }

        context.put(TemplateContextConstants.IS_BASE_ENTITY, Objects.nonNull(modelDefinition.getStorageName()));
        context.put(TemplateContextConstants.STORAGE_NAME, modelDefinition.getStorageName());

        return context;  
    }
    
}
