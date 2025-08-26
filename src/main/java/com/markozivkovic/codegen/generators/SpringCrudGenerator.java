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
    private static final String EXCEPTION = "exception";
    private static final String JPA_SERVICE = "jpa-service";
    private static final String BUSINESS_SERVICE = "business-service";
    private static final String TRANSFER_OBJECT = "transfer-object";
    private static final String MAPPER = "mapper";
    private static final String CONTROLLER = "controller";
    private static final String DOCKER_FILE = "dockerfile";
    private static final String SWAGGER = "swagger";
    private static final String OPENAPI_CODEGEN = "openapi-codegen";
    private static final String GRAPHQL = "graphql";

    private final Map<String, CodeGenerator> GENERATORS;

    public SpringCrudGenerator(final CrudConfiguration crudConfiguration, final List<ModelDefinition> entites,
            final ProjectMetadata projectMetadata) {
        this.GENERATORS = Map.ofEntries(
            Map.entry(ENUM, new EnumGenerator()),
            Map.entry(JPA_MODEL, new JpaEntityGenerator(crudConfiguration, entites)),
            Map.entry(JPA_REPOSITORY, new JpaRepositoryGenerator()),
            Map.entry(EXCEPTION, new ExceptionGenerator()),
            Map.entry(JPA_SERVICE, new JpaServiceGenerator(crudConfiguration, entites)),
            Map.entry(BUSINESS_SERVICE, new BusinessServiceGenerator(entites)),
            Map.entry(TRANSFER_OBJECT, new TransferObjectGenerator(crudConfiguration, entites)),
            Map.entry(MAPPER, new MapperGenerator(entites)),
            Map.entry(CONTROLLER, new RestControllerGenerator(entites)),
            Map.entry(DOCKER_FILE, new DockerfileGenerator(crudConfiguration, projectMetadata)),
            Map.entry(SWAGGER, new SwaggerDocumentationGenerator(crudConfiguration, projectMetadata, entites)),
            Map.entry(OPENAPI_CODEGEN, new OpenApiCodeGenerator(crudConfiguration, projectMetadata, entites)),
            Map.entry(GRAPHQL, new GraphQlGenerator(crudConfiguration, projectMetadata, entites))
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
