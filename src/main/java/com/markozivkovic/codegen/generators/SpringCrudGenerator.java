package com.markozivkovic.codegen.generators;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.CrudConfiguration;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.model.ProjectMetadata;

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
    private static final String DOCKER_FILE = "dockerfile";

    private final Map<String, CodeGenerator> GENERATORS;

    public SpringCrudGenerator(final CrudConfiguration crudConfiguration, final List<ModelDefinition> entites,
            final ProjectMetadata projectMetadata) {
        this.GENERATORS = Map.of(
            ENUM, new EnumGenerator(),
            JPA_MODEL, new JpaEntityGenerator(crudConfiguration),
            JPA_REPOSITORY, new JpaRepositoryGenerator(),
            JPA_SERVICE, new JpaServiceGenerator(entites),
            BUSINESS_SERVICE, new BusinessServiceGenerator(entites),
            TRANSFER_OBJECT, new TransferObjectGenerator(),
            MAPPER, new MapperGenerator(),
            CONTROLLER, new RestControllerGenerator(entites),
            DOCKER_FILE, new DockerfileGenerator(crudConfiguration, projectMetadata)
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
