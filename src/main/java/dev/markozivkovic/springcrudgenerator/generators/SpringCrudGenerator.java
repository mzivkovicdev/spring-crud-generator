/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.generators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;

public class SpringCrudGenerator implements CodeGenerator, ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCrudGenerator.class);
    
    private static final String ENUM = "enum";
    private static final String JPA_MODEL = "jpa-model";
    private static final String MONGO_MODEL = "mongo-model";
    private static final String JPA_REPOSITORY = "jpa-repository";
    private static final String MONGO_REPOSITORY = "mongo-repository";
    private static final String EXCEPTION = "exception";
    private static final String EXCEPTION_HANDLER = "exception-handler";
    private static final String ADDITIONAL_PROPERTY = "additional-property";
    private static final String CACHE = "cache";
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
    private static final String MONGOCK_MIGRATION_SCRIPT = "mongock-migration-script";
    
    private final Map<String, ProjectArtifactGenerator> ARTIFACT_GENERATORS;
    private final Map<String, CodeGenerator> GENERATORS;

    public SpringCrudGenerator(final CrudConfiguration crudConfiguration, final List<ModelDefinition> entities,
            final ProjectMetadata projectMetadata, final PackageConfiguration packageConfiguration) {

        this.ARTIFACT_GENERATORS = new LinkedHashMap<>();
        this.ARTIFACT_GENERATORS.put(ADDITIONAL_PROPERTY, new AdditionalPropertyGenerator(crudConfiguration, packageConfiguration, projectMetadata));
        this.ARTIFACT_GENERATORS.put(CACHE, new CacheGenerator(crudConfiguration, packageConfiguration, entities));
        this.ARTIFACT_GENERATORS.put(DOCKER, new DockerGenerator(crudConfiguration, projectMetadata));
        this.ARTIFACT_GENERATORS.put(EXCEPTION, new ExceptionGenerator(packageConfiguration));
        this.ARTIFACT_GENERATORS.put(EXCEPTION_HANDLER, new GlobalExceptionHandlerGenerator(crudConfiguration, entities, packageConfiguration));
        this.ARTIFACT_GENERATORS.put(SWAGGER, new SwaggerDocumentationGenerator(crudConfiguration, projectMetadata, entities));
        this.ARTIFACT_GENERATORS.put(OPENAPI_CODEGEN, new OpenApiCodeGenerator(crudConfiguration, projectMetadata, entities, packageConfiguration));

        this.GENERATORS = new LinkedHashMap<>();
        this.GENERATORS.put(ENUM, new EnumGenerator(packageConfiguration));
        this.registerDatabaseGenerators(crudConfiguration, entities, projectMetadata, packageConfiguration);
        this.GENERATORS.put(JPA_SERVICE, new JpaServiceGenerator(crudConfiguration, entities, packageConfiguration));
        this.GENERATORS.put(BUSINESS_SERVICE, new BusinessServiceGenerator(entities, packageConfiguration));
        this.GENERATORS.put(TRANSFER_OBJECT, new TransferObjectGenerator(crudConfiguration, entities, packageConfiguration));
        this.GENERATORS.put(MAPPER, new MapperGenerator(crudConfiguration, entities, packageConfiguration));
        this.GENERATORS.put(CONTROLLER, new RestControllerGenerator(crudConfiguration, entities, packageConfiguration));
        this.GENERATORS.put(GRAPHQL, new GraphQlGenerator(crudConfiguration, projectMetadata, entities, packageConfiguration));
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

    /**
     * Registers database specific generators based on the provided configuration.
     * 
     * @param crudConfiguration The configuration containing the database type.
     * @param entities The list of model definitions.
     * @param projectMetadata The project metadata containing the project name and version.
     * @param packageConfiguration The package configuration containing the base package name.
     */
    private void registerDatabaseGenerators(final CrudConfiguration crudConfiguration, final List<ModelDefinition> entities,
            final ProjectMetadata projectMetadata, final PackageConfiguration packageConfiguration) {

        final DatabaseType database = crudConfiguration.getDatabase();

        if (database.isSql()) {
            registerSqlDatabaseGenerators(this.GENERATORS, crudConfiguration, entities, projectMetadata, packageConfiguration);
            return;
        }

        switch (database) {
            case MONGODB -> registerMongoDatabaseGenerators(this.GENERATORS, crudConfiguration, entities, projectMetadata, packageConfiguration);
            default -> throw new IllegalStateException(String.format("Unsupported NoSQL database for generation: %s", database));
        }
    }

    /**
     * Registers database specific generators based on the provided configuration.
     * 
     * @param generators The map of generators to register.
     * @param crudConfiguration The configuration containing the database type.
     * @param entities The list of model definitions.
     * @param projectMetadata The project metadata containing the project name and version.
     * @param packageConfiguration The package configuration containing the base package name.
     */
    private static void registerSqlDatabaseGenerators(final Map<String, CodeGenerator> generators, final CrudConfiguration crudConfiguration,
            final List<ModelDefinition> entities, final ProjectMetadata projectMetadata, final PackageConfiguration packageConfiguration) {

        generators.put(JPA_MODEL, new JpaEntityGenerator(crudConfiguration, entities, packageConfiguration));
        generators.put(JPA_REPOSITORY, new JpaRepositoryGenerator(crudConfiguration, packageConfiguration));
        generators.put(MIGRATION_SCRIPT, new MigrationScriptGenerator(crudConfiguration, projectMetadata, entities));
    }

    /**
     * Registers MongoDB specific generators.
     * 
     * @param generators The map of generators to register.
     * @param crudConfiguration The configuration containing the database type.
     * @param entities The list of model definitions.
     * @param projectMetadata The project metadata containing the project name and version.
     * @param packageConfiguration The package configuration containing the base package name.
     */
    private static void registerMongoDatabaseGenerators(final Map<String, CodeGenerator> generators, final CrudConfiguration crudConfiguration,
            final List<ModelDefinition> entities, final ProjectMetadata projectMetadata, final PackageConfiguration packageConfiguration) {

        generators.put(MONGO_MODEL, new MongoEntityGenerator(crudConfiguration, entities, packageConfiguration));
        generators.put(MONGO_REPOSITORY, new MongoRepositoryGenerator(packageConfiguration));
        generators.put(MONGOCK_MIGRATION_SCRIPT, new MongockMigrationGenerator(crudConfiguration, projectMetadata, entities));
    }
}
