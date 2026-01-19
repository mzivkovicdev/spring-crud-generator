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

package dev.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.imports.MapperImports;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.AuditUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;

public class MapperTemplateContexts {

    private MapperTemplateContexts() {}

    /**
     * Computes a template context for a mapper class of a model definition.
     * 
     * @param modelDefinition      the model definition containing the class and field details
     * @param packagePath          the package path of the directory where the generated class will be written
     * @param swagger              indicates if the mapper is for Swagger models
     * @param isGraphQl            indicates if the mapper is for GraphQL or REST
     * @param packageConfiguration the package configuration for the project
     * @return a template context for the mapper class
     */
    public static Map<String, Object> computeMapperContext(final ModelDefinition modelDefinition, final String packagePath,
            final boolean swagger, final boolean isGraphQl, final PackageConfiguration packageConfiguration) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String mapperName = isGraphQl ? String.format("%sGraphQLMapper", strippedModelName) : String.format("%sRestMapper", strippedModelName);
        final String transferObjectName = String.format("%sTO", strippedModelName);

        final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final Map<String, Object> context = new HashMap<>();
        
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.MAPPER_NAME, mapperName);
        context.put(TemplateContextConstants.TRANSFER_OBJECT_NAME, transferObjectName);
        context.put(TemplateContextConstants.SWAGGER, swagger);
        if (swagger) {
            context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.computeOpenApiModelName(modelDefinition.getName()));
        }
        
        if (!relationFields.isEmpty() || !jsonFields.isEmpty()) {
            final String mapperParameters = Stream.concat(relationFields.stream(), jsonFields.stream())
                    .map(field -> FieldUtils.isJsonField(field) ? FieldUtils.extractJsonFieldName(field) : ModelNameUtils.stripSuffix(field.getType()))
                    .map(field -> isGraphQl ? String.format("%sGraphQLMapper.class", field) : String.format("%sRestMapper.class", field))
                    .distinct()
                    .collect(Collectors.joining(", "));
            context.put(TemplateContextConstants.PARAMETERS, mapperParameters);
        }

        if (swagger && !isGraphQl && Objects.nonNull(modelDefinition.getAudit()) && Boolean.TRUE.equals(modelDefinition.getAudit().getEnabled())) {
            context.put(TemplateContextConstants.AUDIT_ENABLED, true);
            context.put(TemplateContextConstants.AUDIT_TYPE, AuditUtils.resolveAuditType(modelDefinition.getAudit().getType()));
        }

        context.put("projectImports", MapperImports.computeMapperImports(packagePath, modelDefinition, packageConfiguration, swagger, isGraphQl));

        return context;
    }

    /**
     * Computes a template context for a mapper class of a model definition.
     * 
     * @param parentModel          the parent model definition containing the class and field details
     * @param jsonModel            the json model definition containing the class and field details
     * @param packagePath          the package path of the directory where the generated class will be written
     * @param swagger              indicates if the mapper is for Swagger models
     * @param isGraphQl            indicates if the mapper is for GraphQL or REST
     * @param packageConfiguration the package configuration for the project
     * @return a template context for the mapper class 
     */
    public static Map<String, Object> computeHelperMapperContext(final ModelDefinition parentModel, final ModelDefinition jsonModel,
            final String packagePath, final boolean swagger, final boolean isGraphQl, final PackageConfiguration packageConfiguration) {

        final String mapperName = isGraphQl ? String.format("%sGraphQLMapper", ModelNameUtils.stripSuffix(jsonModel.getName())) :
                String.format("%sRestMapper", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, jsonModel.getName());
        context.put(TemplateContextConstants.MAPPER_NAME, mapperName);
        context.put(TemplateContextConstants.TRANSFER_OBJECT_NAME, transferObjectName);

        context.put(TemplateContextConstants.SWAGGER, swagger);
        context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.computeOpenApiModelName(jsonModel.getName()));
        context.put(TemplateContextConstants.GENERATE_ALL_HELPER_METHODS, swagger);
        context.put("projectImports", MapperImports.computeHelperMapperImports(packagePath, jsonModel, parentModel, packageConfiguration, swagger, isGraphQl));

        return context;
    }
    
}
