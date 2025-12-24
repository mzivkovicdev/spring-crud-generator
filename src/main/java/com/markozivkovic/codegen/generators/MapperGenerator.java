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

package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.templates.MapperTemplateContexts;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class MapperGenerator implements CodeGenerator {
    
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
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }
        
        LOGGER.info("Generating mapper for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final boolean swagger = configuration.getOpenApi() != null && Boolean.TRUE.equals(this.configuration.getOpenApi().getApiSpec()) &&
                        Boolean.TRUE.equals(this.configuration.getOpenApi().getGenerateResources());

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
                    
                    this.generateHelperMapper(modelDefinition, jsonModel, outputDir, packagePath, false, swagger);
                    if (configuration != null && configuration.getGraphQl() != null && configuration.getGraphQl()) {
                        this.generateHelperMapper(modelDefinition, jsonModel, outputDir, packagePath, true, false);
                    }
                });

        this.generateMapper(modelDefinition, outputDir, packagePath, false, swagger);
        if (configuration != null && configuration.getGraphQl() != null && configuration.getGraphQl()) {
            this.generateMapper(modelDefinition, outputDir, packagePath, true, false);
        }
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
