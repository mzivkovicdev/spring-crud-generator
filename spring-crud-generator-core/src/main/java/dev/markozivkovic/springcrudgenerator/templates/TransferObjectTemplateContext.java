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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;

public class TransferObjectTemplateContext {

    private TransferObjectTemplateContext() {}
    
    /**
     * Creates a template context for a transfer object of a model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @return a template context for the transfer object
     */
    public static Map<String, Object> computeUpdateTransferObjectContext(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Update");
        context.put(TemplateContextConstants.INPUT_ARGS, FieldUtils.generateInputArgsWithoutFinalUpdateInputTO(modelDefinition.getFields()));
    
        return context;
    }

    /**
     * Creates a template context for a transfer object of a model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param modelDefinitions the list of model definitions
     * @return a template context for the transfer object
     */
    public static Map<String, Object> computeCreateTransferObjectContext(final ModelDefinition modelDefinition, final List<ModelDefinition> modelDefinitions) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Create");
        context.put(TemplateContextConstants.INPUT_ARGS, FieldUtils.generateInputArgsWithoutFinalCreateInputTO(modelDefinition.getFields(), modelDefinitions));
    
        return context;
    }

    /**
     * Computes a template context for the create transfer object of a model.
     * 
     * The generated context contains the class name and input arguments for the create transfer object.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @return a template context for the create transfer object
     */
    public static Map<String, Object> computeCreateTransferObjectContext(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Create");
        context.put(TemplateContextConstants.INPUT_ARGS, FieldUtils.generateInputArgsWithoutFinalCreateInputTO(modelDefinition.getFields()));
    
        return context;
    }

     /**
     * Creates a template context for the input transfer object of a model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @return a template context for the input transfer object
     */
    public static Map<String, Object> computeInputTransferObjectContext(final ModelDefinition modelDefinition) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        
        return context;
    }

    /**
     * Creates a template context for a transfer object of a model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @return a template context for the transfer object
     */
    public static Map<String, Object> computeTransferObjectContext(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(TemplateContextConstants.INPUT_ARGS, FieldUtils.generateInputArgsWithoutFinal(modelDefinition.getFields()));
        context.put(TemplateContextConstants.AUDIT_ENABLED, Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled());
        if (Objects.nonNull(modelDefinition.getAudit())) {
            context.put(TemplateContextConstants.AUDIT_TYPE, AuditUtils.resolveAuditType(modelDefinition.getAudit().getType()));
        }

        return context;
    }
}
