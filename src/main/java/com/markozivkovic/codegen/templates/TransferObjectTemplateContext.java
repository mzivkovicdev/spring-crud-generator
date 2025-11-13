package com.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.AuditUtils;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;

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
