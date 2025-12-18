package com.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.AuditUtils;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;

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

        context.put(TemplateContextConstants.IS_BASE_ENTITY, Objects.nonNull(modelDefinition.getStorageName()));
        context.put(TemplateContextConstants.STORAGE_NAME, modelDefinition.getStorageName());

        return context;  
    }
    
}
