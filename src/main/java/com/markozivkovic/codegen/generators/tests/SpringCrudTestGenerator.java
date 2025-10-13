package com.markozivkovic.codegen.generators.tests;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.generators.CodeGenerator;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;

public class SpringCrudTestGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCrudTestGenerator.class);

    private static final String JPA_SERVICE_TEST = "jpa-service-test";

    private final Map<String, CodeGenerator> GENERATORS;

    public SpringCrudTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entites) {
        this.GENERATORS = Map.ofEntries(
            Map.entry(JPA_SERVICE_TEST, new ServiceUnitTestGenerator(configuration, entites))
        );
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        LOGGER.info("Test generator started for model: {}", modelDefinition.getName());

        this.GENERATORS.values().forEach(generator -> generator.generate(modelDefinition, outputDir));

        LOGGER.info("Test generator finished for model: {}", modelDefinition.getName());
    }
    
}
