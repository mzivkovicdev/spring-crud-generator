package com.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.markozivkovic.codegen.constants.TemplateContextConstants;

public class EnumTemplateContext {

    private EnumTemplateContext() {}

    /**
     * Creates a template context for the enum class of a model.
     * 
     * @param enumName the name of the enum
     * @param enumValues the values of the enum
     * @return a template context for the enum class
     */
    public static Map<String, Object> createEnumContext(final String enumName, final List<String> enumValues) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.ENUM_NAME, enumName);
        context.put(TemplateContextConstants.VALUES, enumValues);
        return context;
    }
    
}
