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

package dev.markozivkovic.springcrudgenerator.generators.tests;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.generators.CodeGenerator;
import dev.markozivkovic.springcrudgenerator.imports.MapperImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

public class MapperUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapperUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public MapperUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
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
        
        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }

        LOGGER.info("Generating mapper test for model: {}", modelDefinition.getName());

        final String testOutputDir = outputDir.replace("main", "test");
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final Boolean swagger = Objects.nonNull(this.configuration.getOpenApi())
                && Boolean.TRUE.equals(this.configuration.getOpenApi().getApiSpec())
                && Boolean.TRUE.equals(this.configuration.getOpenApi().getGenerateResources());

        modelDefinition.getFields().stream()
                .filter(FieldUtils::isJsonField)
                .forEach(field -> {

                    final String jsonFieldName = FieldUtils.extractJsonFieldName(field);
                    final ModelDefinition jsonModel = this.entities.stream()
                            .filter(model -> model.getName().equals(jsonFieldName))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                String.format(
                                    "JSON model not found: %s", jsonFieldName
                                )
                            ));
                    
                    this.generateHelperMapperTest(modelDefinition, jsonModel, testOutputDir, packagePath, false, swagger);
                    if (configuration != null && configuration.getGraphql() != null && Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled())) {
                        this.generateHelperMapperTest(modelDefinition, jsonModel, testOutputDir, packagePath, true, false);
                    }
                });

        this.generateMapperTest(modelDefinition, testOutputDir, packagePath, false, swagger);
        if (configuration != null && configuration.getGraphql() != null && Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled())) {
            this.generateMapperTest(modelDefinition, testOutputDir, packagePath, true, false);
        }
    }

    /**
     * Generates a mapper test class for the given model definition.
     *
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path of the directory where the generated class will be written
     * @param isGraphQl indicates if the mapper is for GraphQL or REST
     * @param swagger indicates if the mapper is for Swagger models
     */
    private void generateMapperTest(final ModelDefinition modelDefinition, final String outputDir,
            final String packagePath, final boolean isGraphQl, final boolean swagger) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = isGraphQl ? String.format("%sGraphQlMapperTest", strippedModelName) :
                String.format("%sRestMapperTest", strippedModelName);
        final String transferObjectName = String.format("%sTO", strippedModelName);
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        final Map<String, Object> context = new HashMap<>();
        context.put("modelName", modelDefinition.getName());
        context.put("className", className);
        context.put("strippedModelName", strippedModelName);
        context.put("transferObjectName", transferObjectName);
        context.put("idField", idField.getName());
        context.put("isGraphQL", isGraphQl);
        context.put("fieldNames", FieldUtils.extractNonRelationNonEnumAndNonJsonFieldNames(modelDefinition.getFields()));
        context.put("enumFields", FieldUtils.extractNamesOfEnumFields(modelDefinition.getFields()));
        context.put("swagger", swagger);
        if (swagger) {
            context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.computeOpenApiModelName(modelDefinition.getName()));
        }
        context.put("projectImports", MapperImports.computeTestMapperImports(packagePath, modelDefinition, packageConfiguration, swagger, isGraphQl));

        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/mapper/mapper-test-template.ftl",
                context
        );

        final StringBuilder sb = new StringBuilder();
        final String packagePathResolved = isGraphQl ?
                PackageUtils.computeGraphQlMapperPackage(packagePath, packageConfiguration) :
                PackageUtils.computeRestMapperPackage(packagePath, packageConfiguration);
        final String filePathResolved = isGraphQl ? 
                PackageUtils.computeGraphQlMappersSubPackage(packageConfiguration) :
                PackageUtils.computeRestMappersSubPackage(packageConfiguration);
        sb.append(String.format(PACKAGE, packagePathResolved))
                .append(mapperTemplate);

        FileWriterUtils.writeToFile(outputDir, filePathResolved, className, sb.toString());
    }

    /**
     * Generates a mapper test class for the given model definition.
     *
     * @param parentModel the parent model definition containing the class and field details
     * @param jsonModel the json model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path of the directory where the generated class will be written
     * @param isGraphQl indicates if the mapper is for GraphQL or REST
     * @param swagger indicates if the mapper is for Swagger models
     */
    private void generateHelperMapperTest(final ModelDefinition parentModel, final ModelDefinition jsonModel, final String outputDir,
            final String packagePath, final boolean isGraphQl, final boolean swagger) {
        
        final String strippedModelName = ModelNameUtils.stripSuffix(jsonModel.getName());
        final String className = isGraphQl ? String.format("%sGraphQlMapperTest", strippedModelName) :
                String.format("%sRestMapperTest", strippedModelName);
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final List<String> enumFields = FieldUtils.extractEnumFields(jsonModel.getFields()).stream()
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());

        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());

        final Map<String, Object> context = new HashMap<>();
        context.put("className", className);
        context.put("modelName", jsonModel.getName());
        context.put("strippedModelName", strippedModelName);
        context.put("transferObjectName", transferObjectName);
        context.put("swagger", swagger);
        context.put("isGraphQL", isGraphQl);
        context.put("idField", jsonModel.getFields().stream().findAny().orElseThrow().getName());
        context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.computeOpenApiModelName(strippedModelName));
        context.put("generateAllHelperMethods", swagger);
        context.put("fieldNames", FieldUtils.extractFieldNames(jsonModel.getFields()));
        context.put("enumFields", enumFields);
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

        context.put("projectImports", MapperImports.computeTestHelperMapperImports(packagePath, jsonModel, parentModel, packageConfiguration, swagger, isGraphQl));

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/mapper/mapper-test-template.ftl",
                context
        );

        final StringBuilder sb = new StringBuilder();
        final String packagePathResolved = isGraphQl ?
                PackageUtils.computeHelperGraphQlMapperPackage(packagePath, packageConfiguration) :
                PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration);
        final String filePathResolved = isGraphQl ? 
                PackageUtils.computeHelperGraphQlMappersSubPackage(packageConfiguration) :
                PackageUtils.computeHelperRestMappersSubPackage(packageConfiguration);
        sb.append(String.format(PACKAGE, packagePathResolved))
                .append(mapperTemplate);
        
        FileWriterUtils.writeToFile(outputDir, filePathResolved, className, sb.toString());
    }
    
}
