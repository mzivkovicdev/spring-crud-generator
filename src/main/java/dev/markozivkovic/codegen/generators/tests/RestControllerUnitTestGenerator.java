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

package dev.markozivkovic.codegen.generators.tests;

import static dev.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.codegen.generators.CodeGenerator;
import dev.markozivkovic.codegen.imports.RestControllerImports;
import dev.markozivkovic.codegen.imports.RestControllerImports.RestEndpointOperation;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.ErrorResponse;
import dev.markozivkovic.codegen.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.codegen.templates.RestControllerTemplateContext;
import dev.markozivkovic.codegen.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.StringUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils;
import dev.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

public class RestControllerUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public RestControllerUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
                final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (!UnitTestUtils.isUnitTestsEnabled(configuration)) {
            return;
        }

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            LOGGER.warn("Model {} does not have an ID field. Skipping controller unit test generation.", modelDefinition.getName());
            return;
        }

        LOGGER.info("Generating controller unit test for model: {}", modelDefinition.getName());

        final String testOutputDir = outputDir.replace("main", "test");
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final Boolean swagger = Objects.nonNull(this.configuration.getOpenApi())
                && Boolean.TRUE.equals(this.configuration.getOpenApi().getApiSpec())
                && Boolean.TRUE.equals(this.configuration.getOpenApi().getGenerateResources());
        final Boolean isGlobalExceptionHandlerEnabled = !(ErrorResponse.NONE.equals(this.configuration.getErrorResponse()) ||
                        Objects.isNull(this.configuration.getErrorResponse()));

        this.generateGetEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger, isGlobalExceptionHandlerEnabled);
        this.generateDeleteByIdEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, isGlobalExceptionHandlerEnabled);
        this.generateUpdateByIdEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger, isGlobalExceptionHandlerEnabled);
        this.generateCreateEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger, isGlobalExceptionHandlerEnabled);
        this.generateAddRelationEdnpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger, isGlobalExceptionHandlerEnabled);
        this.generateRemoveRelationEdnpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger, isGlobalExceptionHandlerEnabled);
    }

    /**
     * Generates a unit test for the add relation endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition                 the model definition containing the class name and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param testOutputDir                   the directory where the generated unit test will be written
     * @param packagePath                     the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix              the model name without the suffix
     * @param swagger                         indicates if the swagger and open API generator is enabled
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     */
    private void generateAddRelationEdnpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
                final String packagePath, final String modelWithoutSuffix, final Boolean swagger, final Boolean isGlobalExceptionHandlerEnabled) {

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        if (relationFields.isEmpty()) {
                return; 
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());

        relationFields.forEach(relationField -> {

                final StringBuilder sb = new StringBuilder();
                final String strippedRelationField = ModelNameUtils.stripSuffix(relationField.getType());
                final String className = String.format("%sAdd%sMockMvcTest", modelWithoutSuffix, strippedRelationField);
                final String controllerClassName = String.format("%sController", modelWithoutSuffix);

                final Map<String, Object> context = new HashMap<>();
                final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);

                context.put("basePath", basePath);
                context.put("controllerClassName", controllerClassName);
                context.put("className", className);
                context.put("modelName", modelDefinition.getName());
                context.put(
                        "methodName",
                        String.format("%ssId%ssPost", StringUtils.uncapitalize(modelWithoutSuffix), strippedRelationField)
                );
                context.put("strippedModelName", modelWithoutSuffix);
                context.put("idType", idField.getType());
                context.put("idField", idField.getName());
                context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
                context.put("strippedRelationClassName", strippedRelationField);
                context.put("relationFieldModel", StringUtils.capitalize(relationField.getName()));
                context.put("baseImports", RestControllerImports.computeAddRelationEndpointBaseImports(modelDefinition));
                context.put("projectImports", RestControllerImports.computeControllerTestProjectImports(
                        modelDefinition, outputDir, swagger, RestEndpointOperation.ADD_RELATION, relationField, packageConfiguration, isGlobalExceptionHandlerEnabled
                ));
                context.put("testImports", RestControllerImports.computeAddRelationEndpointTestImports(
                        UnitTestUtils.isInstancioEnabled(configuration), configuration.getSpringBootVersion()
                ));
                context.put("swagger", swagger);
                context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
                context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);

                sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)));
                sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                        "test/unit/controller/endpoint/add-resource-relation.ftl",
                        context
                ));

                FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), className, sb.toString());
        });
    }

    /**
     * Generates a unit test class for the remove relation endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition                 the model definition containing the class name and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param testOutputDir                   the directory where the generated unit test will be written
     * @param packagePath                     the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix              the model name without the suffix
     * @param swagger                         indicates if the swagger and open API generator is enabled
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     */
    private void generateRemoveRelationEdnpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
                final String packagePath, final String modelWithoutSuffix, final Boolean swagger, final Boolean isGlobalExceptionHandlerEnabled) {

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        if (relationFields.isEmpty()) {
                return; 
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<String> collectionRelationFields = FieldUtils.extractCollectionRelationNames(modelDefinition);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());

        relationFields.forEach(relationField -> {

                final StringBuilder sb = new StringBuilder();
                final String strippedRelationField = ModelNameUtils.stripSuffix(relationField.getType());
                final String className = String.format("%sRemove%sMockMvcTest", modelWithoutSuffix, strippedRelationField);
                final String controllerClassName = String.format("%sController", modelWithoutSuffix);
                final ModelDefinition relatedModelDefinition = this.entities.stream()
                        .filter(e -> e.getName().equals(relationField.getType()))
                        .findFirst()
                        .orElseThrow();
                final FieldDefinition relatedIdField = FieldUtils.extractIdField(relatedModelDefinition.getFields());

                final Map<String, Object> context = new HashMap<>();
                final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);

                context.put("basePath", basePath);
                context.put("controllerClassName", controllerClassName);
                context.put("className", className);
                context.put(
                        "methodName",
                        String.format("%ssId%ssDelete", StringUtils.uncapitalize(modelWithoutSuffix), strippedRelationField)
                );
                context.put("strippedModelName", modelWithoutSuffix);
                context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
                context.put("isCollection", collectionRelationFields.contains(relationField.getName()));
                context.put("idType", idField.getType());
                context.put("idField", idField.getName());
                context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
                context.put("strippedRelationClassName", strippedRelationField);
                context.put("relationFieldModel", StringUtils.capitalize(relationField.getName()));
                context.put("relIdType", relatedIdField.getType());
                context.put("relIdField", relatedIdField.getName());
                context.put("invalidRelIdType", UnitTestUtils.computeInvalidIdType(relatedIdField));
                context.put("baseImports", RestControllerImports.computeRemoveRelationEndpointBaseImports(modelDefinition, entities));
                context.put("projectImports", RestControllerImports.computeControllerTestProjectImports(
                        modelDefinition, outputDir, false, RestEndpointOperation.REMOVE_RELATION, packageConfiguration, isGlobalExceptionHandlerEnabled
                ));
                context.put("testImports", RestControllerImports.computeDeleteEndpointTestImports(
                        UnitTestUtils.isInstancioEnabled(configuration), configuration.getSpringBootVersion()
                ));
                context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
                context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);

                sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)));
                sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                        "test/unit/controller/endpoint/remove-resource-relation.ftl",
                        context
                ));

                FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), className, sb.toString());
        });
    }

    /**
     * Generates a unit test for the create endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition                 the model definition containing the class name and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param testOutputDir                   the directory where the generated unit test will be written
     * @param packagePath                     the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix              the model name without the suffix
     * @param swagger                         indicates if the swagger and open API generator is enabled
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     */
    private void generateCreateEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix, final Boolean swagger, final Boolean isGlobalExceptionHandlerEnabled) {
        
        final StringBuilder sb = new StringBuilder();
        final String className = String.format("%sCreateMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());

        final Map<String, Object> context = RestControllerTemplateContext.computeCreateTestEndpointContext(modelDefinition, entities);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);

        context.put("basePath", basePath);
        context.put("controllerClassName", controllerClassName);
        context.put("className", className);
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("hasCollectionRelations", FieldUtils.isAnyRelationManyToMany(modelDefinition.getFields()) 
                || FieldUtils.isAnyRelationOneToMany(modelDefinition.getFields()));
        context.put("swagger", swagger);
        context.put("testImports", RestControllerImports.computeUpdateEndpointTestImports(UnitTestUtils.isInstancioEnabled(configuration), configuration.getSpringBootVersion()));
        context.put("projectImports", RestControllerImports.computeCreateEndpointTestProjectImports( 
                modelDefinition, outputDir, swagger, packageConfiguration, isGlobalExceptionHandlerEnabled
        ));
        context.put("jsonFields", jsonFields);
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);

        sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/create-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), className, sb.toString());
    }

    /**
     * Generates a unit test for the update by ID endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition                 the model definition containing the class name and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param testOutputDir                   the directory where the generated unit test will be written
     * @param packagePath                     the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix              the model name without the suffix
     * @param swagger                         indicates if the swagger and open API generator is enabled
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     */
    private void generateUpdateByIdEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix, final Boolean swagger, final Boolean isGlobalExceptionHandlerEnabled) {
        
        final StringBuilder sb = new StringBuilder();
        final String className = String.format("%sUpdateByIdMockMvcTest", modelWithoutSuffix);

        final Map<String, Object> context = RestControllerTemplateContext.computeUpdateByIdTestEndpointContext(
                modelDefinition, configuration, packageConfiguration, swagger, isGlobalExceptionHandlerEnabled, outputDir, testOutputDir, packagePath
        );

        sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/update-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), className, sb.toString());
    }

    /**
     * Generates a unit test for the delete by ID endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition                 the model definition containing the class name and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param testOutputDir                   the directory where the generated unit test will be written
     * @param packagePath                     the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix              the model name without the suffix
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     */
    private void generateDeleteByIdEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix, final Boolean isGlobalExceptionHandlerEnabled) {

        final StringBuilder sb = new StringBuilder();
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String className = String.format("%sDeleteByIdMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);

        final Map<String, Object> context = new HashMap<>();
        context.put("isIdUuid", FieldUtils.isIdFieldUUID(idField));
        context.put("basePath", basePath);
        context.put("controllerClassName", controllerClassName);
        context.put("className", className);
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("projectImports", RestControllerImports.computeControllerTestProjectImports(
                modelDefinition, outputDir, false, RestEndpointOperation.DELETE, packageConfiguration, isGlobalExceptionHandlerEnabled
        ));
        context.put("testImports", RestControllerImports.computeDeleteEndpointTestImports(
                UnitTestUtils.isInstancioEnabled(configuration), configuration.getSpringBootVersion()
        ));
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);

        sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/delete-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), className, sb.toString());
    }

    /**
     * Generates a unit test class for the get endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition                 the model definition containing the class name and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param testOutputDir                   the directory where the generated unit test will be written
     * @param packagePath                     the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix              the model name without the suffix
     * @param swagger                         indicates if the swagger and open API generator is enabled
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     */
    private void generateGetEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix, final Boolean swagger, final Boolean isGlobalExceptionHandlerEnabled) {
        
        final StringBuilder sb = new StringBuilder();
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String className = String.format("%sGetMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);

        final Map<String, Object> context = new HashMap<>();
        context.put("isIdUuid", FieldUtils.isIdFieldUUID(idField));
        context.put("basePath", basePath);
        context.put("controllerClassName", controllerClassName);
        context.put("className", className);
        context.put("swagger", swagger);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("modelName", modelDefinition.getName());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("testImports", RestControllerImports.computeGetEndpointTestImports(
                UnitTestUtils.isInstancioEnabled(configuration), configuration.getSpringBootVersion()
        ));
        context.put("projectImports", RestControllerImports.computeControllerTestProjectImports(
                modelDefinition, outputDir, swagger, RestEndpointOperation.GET, packageConfiguration, isGlobalExceptionHandlerEnabled
        ));
        context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

        sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/get-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), className, sb.toString());
    }

}
