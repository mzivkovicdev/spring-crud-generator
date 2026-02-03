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

package dev.markozivkovic.springcrudgenerator.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.RestControllerImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.common.ValidationContextBuilder;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

public class RestControllerTemplateContext {

    private RestControllerTemplateContext() {}

    /**
     * Creates a template context for a controller class of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a template context for the controller class
     */
    public static Map<String, Object> computeControllerClassContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Controller");
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.RELATIONS, !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put(TemplateContextConstants.JSON_FIELDS, jsonFields);

        return context;
    }

    /**
     * Computes a template context for a create endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a template context for the create endpoint
     */
    public static Map<String, Object> computeCreateEndpointContext(final ModelDefinition modelDefinition,
            final List<ModelDefinition> entities) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final Map<String, Object> context = new HashMap<>();
        
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<Map<String, Object>> inputFields = new ArrayList<>();
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        modelDefinition.getFields().stream()
            .filter(field -> !field.equals(idField))
            .forEach(field -> {

                final boolean isRelation = Objects.nonNull(field.getRelation());
                final boolean isCollection = isRelation && (manyToManyFields.contains(field) || oneToManyFields.contains(field));
                
                final Map<String, Object> fieldContext = new HashMap<>(Map.of(
                    TemplateContextConstants.FIELD, field.getName(),
                    TemplateContextConstants.FIELD_TYPE, field.getResolvedType(),
                    TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(field.getType()),
                    TemplateContextConstants.IS_COLLECTION, isCollection,
                    TemplateContextConstants.IS_RELATION, isRelation,
                    TemplateContextConstants.IS_JSON_FIELD, FieldUtils.isJsonField(field),
                    TemplateContextConstants.IS_ENUM, FieldUtils.isFieldEnum(field)
                ));

                if (isRelation) {
                    final FieldDefinition relationId = entities.stream()
                            .filter(entity -> entity.getName().equals(field.getType()))
                            .findFirst()
                            .map(entity -> FieldUtils.extractIdField(entity.getFields()))
                            .orElseThrow();
                            
                    fieldContext.put(TemplateContextConstants.RELATION_ID_TYPE, relationId.getType());
                    fieldContext.put(TemplateContextConstants.RELATION_ID_FIELD, relationId.getName());
                }
                
                inputFields.add(fieldContext);
        });

        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.INPUT_FIELDS, inputFields);
        context.put(TemplateContextConstants.RELATIONS, !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());

        return context;
    }

    /**
     * Computes a template context for a create test endpoint of a model.
     * The generated context contains the model name, stripped model name, input fields, and fields with length.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a template context for the create test endpoint
     */
    public static Map<String, Object> computeCreateTestEndpointContext(final ModelDefinition modelDefinition,
            final List<ModelDefinition> entities) {

        final Map<String, Object> context = computeCreateEndpointContext(modelDefinition, entities);
        
        return context;
    }

    /**
     * Computes a template context for an update by ID test endpoint of a model.
     * The generated context contains the model name, stripped model name, input fields, fields with length, and test imports.
     * 
     * @param modelDefinition                 the model definition
     * @param configuration                   the CRUD configuration
     * @param packageConfiguration            the package configuration
     * @param swagger                         indicates if the swagger and open API generator is enabled
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     * @param outputDir                       the directory where the generated code will be written
     * @param testOutputDir                   the directory where the generated unit test will be written
     * @param packagePath                     the package path of the directory where the generated code will be written
     * @return a template context for the update by ID test endpoint
     */
    public static Map<String, Object> computeUpdateByIdTestEndpointContext(final ModelDefinition modelDefinition,
                final CrudConfiguration configuration, final PackageConfiguration packageConfiguration,
                final Boolean swagger, final Boolean isGlobalExceptionHandlerEnabled,
                final String outputDir, final String testOutputDir, final String packagePath) {
        
        final Map<String, Object> context = new HashMap<>();
        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final String className = String.format("%sUpdateByIdMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());

        context.put("isIdUuid", FieldUtils.isIdFieldUUID(idField));
        context.put("basePath", basePath);
        context.put("controllerClassName", controllerClassName);
        context.put("className", className);
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("modelName", modelDefinition.getName());
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("swagger", swagger);
        context.put("inputFields", FieldUtils.extractNonIdNonRelationFieldNamesForController(modelDefinition.getFields(), swagger));
        context.put("testImports", RestControllerImports.computeUpdateEndpointTestImports(
                UnitTestUtils.isInstancioEnabled(configuration), configuration.getSpringBootVersion()
        ));
        context.put("projectImports", RestControllerImports.computeUpdateEndpointTestProjectImports( 
                modelDefinition, outputDir, swagger, packageConfiguration, isGlobalExceptionHandlerEnabled
        ));
        context.put("jsonFields", jsonFields);
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);
        context.put("fieldNames", FieldUtils.extractNonIdNonRelationFieldNames(modelDefinition.getFields()));

        ValidationContextBuilder.contribute(
            modelDefinition, context,
            String.format("%s", context.get(TemplateContextConstants.DATA_GENERATOR_FIELD_NAME)),
            String.format("%s", context.get(TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ).toString())
        );

        return context;
    }

    /**
     * Computes a template context for a get by ID endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the get by ID endpoint
     */
    public static Map<String, Object> computeGetByIdEndpointContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }

    /**
     * Computes a template context for a get all endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the get all endpoint
     */
    public static Map<String, Object> computeGetAllEndpointContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);

        return context;
    }

    /**
     * Computes a template context for an update endpoint of a model.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @param swagger whether Swagger is enabled
     * @return a template context for the update endpoint
     */
    public static Map<String, Object> computeUpdateEndpointContext(final ModelDefinition modelDefinition, final boolean swagger) {
        
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();

        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.INPUT_FIELDS, FieldUtils.extractNonIdNonRelationFieldNamesForController(modelDefinition.getFields(), swagger));
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }

    /**
     * Computes a template context for a delete endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the delete endpoint
     */
    public static Map<String, Object> computeDeleteEndpointContext(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();

        context.put(TemplateContextConstants.MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }

    /**
     * Computes a template context for an add resource relation endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @param entities        a list of model definitions
     * @return a template context for the add resource relation endpoint
     */
    public static Map<String, Object> computeAddResourceRelationEndpointContext(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {
        
        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final Map<String, Object> modelContext = new HashMap<>();
        modelContext.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        modelContext.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        modelContext.put(TemplateContextConstants.ID_TYPE, idField.getType());

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<Map<String, Object>> relations = new ArrayList<>();
        
        relationFields.forEach(relation -> {

            final Map<String, Object> relationContext = new HashMap<>();
            final String strippedRelationClassName = ModelNameUtils.stripSuffix(relation.getType());

            final ModelDefinition relationEntity = entities.stream()
                    .filter(entity -> entity.getName().equals(relation.getType()))
                    .findFirst()
                    .orElseThrow();

            final FieldDefinition entityIdField = FieldUtils.extractIdField(relationEntity.getFields());

            relationContext.put(TemplateContextConstants.RELATION_ID_FIELD, entityIdField.getName());
            relationContext.put(TemplateContextConstants.RELATION_FIELD_MODEL, relation.getName());
            relationContext.put(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME, strippedRelationClassName);
            relationContext.put(TemplateContextConstants.METHOD_NAME, String.format("%ssId%ssPost", StringUtils.uncapitalize(strippedModelName), strippedRelationClassName));
            relations.add(relationContext);
        });
        
        return new HashMap<>(Map.of(
            TemplateContextConstants.MODEL, modelContext,
            TemplateContextConstants.RELATIONS, relations
        ));
    }

    /**
     * Computes a template context for a remove resource relation endpoint of a model.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a template context for the remove resource relation endpoint
     */
    public static Map<String, Object> computeRemoveResourceRelationEndpointContext(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {
        
        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return Map.of();
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final Map<String, Object> modelContext = new HashMap<>();
        modelContext.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        modelContext.put(TemplateContextConstants.ID_TYPE, idField.getType());

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());
        final List<Map<String, Object>> relations = new ArrayList<>();
        
        relationFields.forEach(relation -> {

            final ModelDefinition relationEntity = entities.stream()
                    .filter(entity -> entity.getName().equals(relation.getType()))
                    .findFirst()
                    .orElseThrow();

            final FieldDefinition entityIdField = FieldUtils.extractIdField(relationEntity.getFields());

            final Map<String, Object> relationContext = new HashMap<>();
            final String strippedRelationClassName = ModelNameUtils.stripSuffix(relation.getType());
            relationContext.put(TemplateContextConstants.RELATION_FIELD_MODEL, relation.getName());
            relationContext.put(TemplateContextConstants.STRIPPED_RELATION_CLASS_NAME, strippedRelationClassName);
            relationContext.put(TemplateContextConstants.METHOD_NAME, String.format("%ssId%ssDelete", StringUtils.uncapitalize(strippedModelName), strippedRelationClassName));
            relationContext.put(TemplateContextConstants.RELATION_ID_TYPE, entityIdField.getType());
            relationContext.put(TemplateContextConstants.RELATION_FIELD, String.format("%sId", StringUtils.uncapitalize(strippedRelationClassName)));
            relationContext.put(TemplateContextConstants.IS_COLLECTION, manyToManyFields.contains(relation) || oneToManyFields.contains(relation));

            relations.add(relationContext);
        });

        return new HashMap<>(Map.of(
                TemplateContextConstants.MODEL, modelContext,
                TemplateContextConstants.RELATIONS, relations
        ));
    }
    
}
