package com.markozivkovic.codegen.generators;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.ModelDefinition;

public class SpringCrudGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCrudGenerator.class);

    private static final String ENUM = "enum";
    private static final String JPA_MODEL = "jpa-model";
    private static final String JPA_REPOSITORY = "jpa-repository";
    private static final String JPA_SERVICE = "jpa-service";
    private static final String BUSINESS_SERVICE = "business-service";
    private static final String TRANSFER_OBJECT = "transfer-object";
    private static final String MAPPER = "mapper";
    private static final String CONTROLLER = "controller";

    private final Map<String, CodeGenerator> GENERATORS;

    public SpringCrudGenerator(final List<ModelDefinition> entites) {
        this.GENERATORS = Map.of(
                ENUM, new EnumGenerator(),
                JPA_MODEL, new JpaEntityGenerator(),
                JPA_REPOSITORY, new JpaRepositoryGenerator(),
                JPA_SERVICE, new JpaServiceGenerator(),
                BUSINESS_SERVICE, new BusinessServiceGenerator(entites),
                TRANSFER_OBJECT, new TransferObjectGenerator(),
                MAPPER, new MapperGenerator(),
                CONTROLLER, new RestControllerGenerator()
        );
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generator started for model: {}", modelDefinition.getName());
        
        GENERATORS.forEach((key, generator) -> {
            
            generator.generate(modelDefinition, outputDir);
        });
        
        LOGGER.info("Generator finished for model: {}", modelDefinition.getName());
    }
    
}
