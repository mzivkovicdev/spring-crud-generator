package com.markozivkovic.codegen.utils;

import java.util.HashMap;
import java.util.Map;

import com.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

public class TemplateContextUtils {
    
    private static final String DATA_GENERATOR = "dataGenerator";
    private static final String DATA_GENERATOR_FIELD_NAME = "generatorFieldName";
    private static final String DATA_GENERATOR_SINGLE_OBJ = "singleObjectMethodName";
    private static final String DATA_GENERATOR_LIST_METHOD = "multipleObjectsMethodName";

    private TemplateContextUtils() {
        
    }

    /**
     * Computes a template context for a unit test data generator based on a TestDataGeneratorConfig object.
     * 
     * @param config the TestDataGeneratorConfig object
     * @return a template context containing the name of the data generator, the name of the random field name,
     *         the name of the single object method, and the name of the list method
     */
    public static Map<String, Object> computeDataGeneratorContext(final TestDataGeneratorConfig config) {
        
        final Map<String, Object> context = new HashMap<>();
        context.put(DATA_GENERATOR, config.generator());
        context.put(DATA_GENERATOR_FIELD_NAME, config.randomFieldName());
        context.put(DATA_GENERATOR_SINGLE_OBJ, config.singleObjectMethodName());
        context.put(DATA_GENERATOR_LIST_METHOD, config.multipleObjectsMethodName());
        
        return context;
    }

}
