package com.markozivkovic.codegen.generators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.models.ProjectMetadata;

public class SpringCrudGenerator implements CodeGenerator, ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCrudGenerator.class);
    
    private static final String ENUM = "enum";
    private static final String JPA_MODEL = "jpa-model";
    private static final String JPA_REPOSITORY = "jpa-repository";
    private static final String EXCEPTION = "exception";
    private static final String EXCEPTION_HANDLER = "exception-handler";
    private static final String ADDITIONAL_PROPERTY = "additional-property";
    private static final String JPA_SERVICE = "jpa-service";
    private static final String BUSINESS_SERVICE = "business-service";
    private static final String TRANSFER_OBJECT = "transfer-object";
    private static final String MAPPER = "mapper";
    private static final String CONTROLLER = "controller";
    private static final String DOCKER = "docker";
    private static final String SWAGGER = "swagger";
    private static final String OPENAPI_CODEGEN = "openapi-codegen";
    private static final String GRAPHQL = "graphql";
    private static final String MIGRATION_SCRIPT = "migration-script";
    
    private final Map<String, ProjectArtifactGenerator> ARTIFACT_GENERATORS;
    private final Map<String, CodeGenerator> GENERATORS;

    public SpringCrudGenerator(final CrudConfiguration crudConfiguration, final List<ModelDefinition> entites,
            final ProjectMetadata projectMetadata, final PackageConfiguration packageConfiguration) {

        this.ARTIFACT_GENERATORS = Stream.of(
            Map.entry(ADDITIONAL_PROPERTY, new AdditionalPropertyGenerator(crudConfiguration, packageConfiguration)),
            Map.entry(DOCKER, new DockerGenerator(crudConfiguration, projectMetadata)),
            Map.entry(EXCEPTION, new ExceptionGenerator(packageConfiguration)),
            Map.entry(EXCEPTION_HANDLER, new GlobalExceptionHandlerGenerator(crudConfiguration, entites, packageConfiguration)),
            Map.entry(SWAGGER, new SwaggerDocumentationGenerator(crudConfiguration, projectMetadata, entites)),
            Map.entry(OPENAPI_CODEGEN, new OpenApiCodeGenerator(crudConfiguration, projectMetadata, entites, packageConfiguration))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        this.GENERATORS = Stream.of(
            Map.entry(ENUM, new EnumGenerator(packageConfiguration)),
            Map.entry(JPA_MODEL, new JpaEntityGenerator(crudConfiguration, entites, packageConfiguration)),
            Map.entry(JPA_REPOSITORY, new JpaRepositoryGenerator(packageConfiguration)),
            Map.entry(JPA_SERVICE, new JpaServiceGenerator(crudConfiguration, entites, packageConfiguration)),
            Map.entry(BUSINESS_SERVICE, new BusinessServiceGenerator(entites, packageConfiguration)),
            Map.entry(TRANSFER_OBJECT, new TransferObjectGenerator(crudConfiguration, entites, packageConfiguration)),
            Map.entry(MAPPER, new MapperGenerator(crudConfiguration, entites, packageConfiguration)),
            Map.entry(CONTROLLER, new RestControllerGenerator(crudConfiguration, entites, packageConfiguration)),
            Map.entry(GRAPHQL, new GraphQlGenerator(crudConfiguration, projectMetadata, entites, packageConfiguration)),
            Map.entry(MIGRATION_SCRIPT, new MigrationScriptGenerator(crudConfiguration, projectMetadata, entites))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public void generate(final String outputDir) {
        
        ARTIFACT_GENERATORS.forEach((key, generator) -> generator.generate(outputDir));
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
