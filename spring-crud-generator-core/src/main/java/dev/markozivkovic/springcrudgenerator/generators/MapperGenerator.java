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

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.MapperTemplateContexts;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

public class MapperGenerator implements CodeGenerator, ProjectArtifactGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MapperGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public MapperGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
            final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final String outputDir) {

        if (!isOpenApiResourceGenerationEnabled()) {
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        this.generateDateTimeMapper(outputDir, packagePath);
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }
        
        LOGGER.info("Generating mapper for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final boolean swagger = isOpenApiResourceGenerationEnabled();

        modelDefinition.getFields().stream()
                .filter(FieldUtils::isJsonField)
                .forEach(field -> {

                    final String jsonInnerElementType = FieldUtils.extractJsonInnerElementType(field);
                    final ModelDefinition jsonModel = this.entities.stream()
                            .filter(model -> model.getName().equals(jsonInnerElementType))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                String.format(
                                    "JSON model not found: %s", jsonInnerElementType
                                )
                            ));
                    
                    this.generateHelperMapper(modelDefinition, jsonModel, outputDir, packagePath, false, swagger);
                    if (configuration != null && configuration.getGraphql() != null && Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled())) {
                        this.generateHelperMapper(modelDefinition, jsonModel, outputDir, packagePath, true, false);
                    }
                });

        this.generateMapper(modelDefinition, outputDir, packagePath, false, swagger);
        if (configuration != null && configuration.getGraphql() != null && Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled())) {
            this.generateMapper(modelDefinition, outputDir, packagePath, true, false);
        }
    }

    /**
     * Generates the shared mapper used by all REST mappers for date/time conversions.
     * This project-level artifact is generated before the model-specific mappers.
     *
     * @param outputDir  the directory where the mapper is written
     * @param packagePath the base package derived from the output directory
     */
    private void generateDateTimeMapper(final String outputDir, final String packagePath) {

        LOGGER.info("Generating shared DateTimeMapper");

        final String source = String.format(
                PACKAGE, PackageUtils.computeRestMapperPackage(packagePath, packageConfiguration)
        ) + FreeMarkerTemplateProcessorUtils.processTemplate(
                "mapper/date-time-mapper-template.ftl", Map.of()
        );

        FileWriterUtils.writeToFile(
                outputDir,
                PackageUtils.computeRestMappersSubPackage(packageConfiguration),
                "DateTimeMapper",
                source
        );
    }

    /**
     * Determines whether OpenAPI specification and resource generation are enabled.
     *
     * @return true if both OpenAPI specification and resource generation are enabled,
     *         false otherwise
     */
    private boolean isOpenApiResourceGenerationEnabled() {

        return configuration != null
                && configuration.getOpenApi() != null
                && Boolean.TRUE.equals(configuration.getOpenApi().getApiSpec())
                && Boolean.TRUE.equals(configuration.getOpenApi().getGenerateResources());
    }

    /**
     * Generates a mapper class for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @param outputDir       the directory where the generated class will be written
     * @param packagePath     the package path of the directory where the generated class will be written
     */
    private void generateMapper(final ModelDefinition modelDefinition, final String outputDir,
            final String packagePath, final boolean isGraphQl, final boolean swagger) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String mapperName = isGraphQl ? String.format("%sGraphQLMapper", strippedModelName) :
                String.format("%sRestMapper", strippedModelName);
        
        final Map<String, Object> context = MapperTemplateContexts.computeMapperContext(
                modelDefinition, packagePath, swagger, isGraphQl, packageConfiguration
        );
        context.put(
                TemplateContextConstants.OPEN_IN_VIEW_ENABLED,
                AdditionalPropertiesUtils.isOpenInViewEnabled(this.configuration.getAdditionalProperties())
        );
        
        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("mapper/mapper-template.ftl", context);
        
        final String resolvedPackagePath = isGraphQl ?
                PackageUtils.computeGraphQlMapperPackage(packagePath, packageConfiguration) :
                PackageUtils.computeRestMapperPackage(packagePath, packageConfiguration);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, resolvedPackagePath))
                .append(mapperTemplate);

        final String filePath = isGraphQl ? PackageUtils.computeGraphQlMappersSubPackage(packageConfiguration)
                : PackageUtils.computeRestMappersSubPackage(packageConfiguration);
        FileWriterUtils.writeToFile(outputDir, filePath, mapperName, sb.toString());
    }

    /**
     * Generates a helper mapper for the given json model.
     *
     * @param parentModel the parent model definition containing the class and field details
     * @param jsonModel the json model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path of the directory where the generated class will be written
     * @param isGraphQl indicates if the mapper is for GraphQL or REST
     * @param swagger indicates if the mapper is for Swagger models
     */
    private void generateHelperMapper(final ModelDefinition parentModel, final ModelDefinition jsonModel, final String outputDir,
            final String packagePath, final boolean isGraphQl, final boolean swagger) {
        
        final String mapperName = isGraphQl ? String.format("%sGraphQLMapper", ModelNameUtils.stripSuffix(jsonModel.getName())) :
                String.format("%sRestMapper", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final Map<String, Object> context = MapperTemplateContexts.computeHelperMapperContext(
                        parentModel, jsonModel, packagePath, swagger, isGraphQl, packageConfiguration
        );
        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("mapper/mapper-template.ftl", context);

        final String resolvedPackagePath = isGraphQl ? 
                PackageUtils.computeHelperGraphQlMapperPackage(packagePath, packageConfiguration) :
                PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, resolvedPackagePath))
                .append(mapperTemplate);

        final String filePath = isGraphQl ?
                PackageUtils.computeHelperGraphQlMappersSubPackage(packageConfiguration) :
                PackageUtils.computeHelperRestMappersSubPackage(packageConfiguration);
        FileWriterUtils.writeToFile(outputDir, filePath, mapperName, sb.toString());
    }

}
