package com.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.Map;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

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
