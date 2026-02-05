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
import dev.markozivkovic.springcrudgenerator.imports.ResolverImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ErrorResponse;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.common.ValidationContextBuilder;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

public class GraphQlTemplateContext {

    private GraphQlTemplateContext() {}
    
    /**
     * Computes a template context for a GraphQL resolver class of a model.
     * 
     * @param modelDefinition the model definition
     * @return a template context for the GraphQL resolver class with stripped model name and class name
     */
    public static Map<String, Object> computeGraphQlResolver(final ModelDefinition modelDefinition) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, strippedModelName);
        context.put(TemplateContextConstants.CLASS_NAME, String.format("%sResolver", strippedModelName));
        context.put(TemplateContextConstants.JSON_FIELDS, jsonFields);
        context.put(TemplateContextConstants.RELATIONS, !FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty());
        
        return context;
    }

    /**
     * Computes a template context for a GraphQL schema class of a model.
     * 
     * The generated context contains the stripped model name, a list of fields with their relation types replaced
     * by their stripped relation types, and a list of model definitions representing entities related to the model.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a template context for the GraphQL schema class
     */
    public static Map<String, Object> computeGraphQlSchemaContext(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        final List<FieldDefinition> fields = modelDefinition.getFields().stream()
            .map(field -> {
                if (Objects.nonNull(field.getRelation())) {
                    return FieldUtils.cloneFieldDefinition(field)
                        .setType(ModelNameUtils.stripSuffix(field.getType()));
                }
                return field;
            }).collect(Collectors.toList());
            
        final List<String> jsonFieldNames = FieldUtils.extractJsonFields(fields).stream()
                .map(jsonField -> FieldUtils.extractJsonFieldName(jsonField))
                .collect(Collectors.toList());
        final List<ModelDefinition> jsonModels = entities.stream()
                .filter(model -> jsonFieldNames.contains(model.getName()))
                .collect(Collectors.toList());

        final Map<String, Object> context = new HashMap<>(Map.of(
                TemplateContextConstants.NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()),
                TemplateContextConstants.FIELDS, fields,
                TemplateContextConstants.JSON_MODELS, jsonModels
        ));

        if (Objects.nonNull(modelDefinition.getAudit()) && Boolean.TRUE.equals(modelDefinition.getAudit().getEnabled())) {
            context.put(TemplateContextConstants.AUDIT_TYPE, AuditUtils.resolveAuditType(modelDefinition.getAudit().getType()));
            context.put(TemplateContextConstants.AUDIT_ENABLED, true);
        }

        return context;
    }

    /**
     * Computes a GraphQL mutation mapping template context for a model definition.
     * 
     * @param modelDefinition the model definition
     * @param entities a list of model definitions representing entities related to the model
     * @return a GraphQL mutation mapping template context with model name, stripped model name, and ID type
     */
    public static Map<String, Object> computeMutationMappingGraphQL(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.INPUT_FIELDS_WITHOUT_RELATIONS, FieldUtils.extractNonIdNonRelationFieldNamesForResolver(modelDefinition.getFields()));
        context.put(TemplateContextConstants.INPUT_FIELDS_WITH_RELATIONS, FieldUtils.extractNonIdFieldNamesForResolver(modelDefinition.getFields()));
        final List<Map<String, Object>> relations = new ArrayList<>();

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        relationFields.forEach(relation -> {

            final ModelDefinition relationEntity = entities.stream()
                    .filter(entity -> entity.getName().equals(relation.getType()))
                    .findFirst()
                    .orElseThrow();
            
            final Map<String, Object> relationContext = new HashMap<>();
            final String strippedFieldName = StringUtils.capitalize(ModelNameUtils.stripSuffix(relation.getName()));
            relationContext.put(TemplateContextConstants.RELATION_FIELD, strippedFieldName);
            relationContext.put(TemplateContextConstants.IS_COLLECTION, manyToManyFields.contains(relation) || oneToManyFields.contains(relation));
            relationContext.put(TemplateContextConstants.RELATION_ID_TYPE, FieldUtils.extractIdField(relationEntity.getFields()).getType());

            relations.add(relationContext);
        });

        context.put(TemplateContextConstants.RELATIONS, relations);

        return context;
    }

    /**
     * Computes a GraphQL query mapping template context for a model definition.
     * 
     * @param modelDefinition the model definition
     * @return a GraphQL query mapping template context with model name, stripped model name, and ID type
     */
    public static Map<String, Object> computeQueryMappingGraphQL(final ModelDefinition modelDefinition) {

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()));
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());

        return context;
    }

    /**
     * Computes a template context for a unit test of a mutation resolver of a model.
     * 
     * @param modelDefinition the model definition
     * @param configuration the CRUD configuration
     * @param packageConfiguration the package configuration
     * @param entities a list of model definitions representing entities related to the model
     * @param outputDir the directory where the generated code will be written
     * @param testOutputDir the directory where the generated unit test will be written
     * @return a template context for the unit test of the mutation resolver
     */
    public static Map<String, Object> computeMutationUnitTestContext(final ModelDefinition modelDefinition,
                final CrudConfiguration configuration, final PackageConfiguration packageConfiguration,
                final List<ModelDefinition> entities, final String outputDir, final String testOutputDir) {

        final Map<String, Object> context = new HashMap<>();
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String resolverClassName = String.format("%sResolver", modelWithoutSuffix);
        final String className = String.format("%sResolverMutationTest", modelWithoutSuffix);
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        final Boolean isGlobalExceptionHandlerEnabled = !(ErrorResponse.NONE.equals(configuration.getErrorResponse()) ||
                        Objects.isNull(configuration.getErrorResponse()));
        final List<String> collectionRelationFields = FieldUtils.extractCollectionRelationNames(modelDefinition);
        final boolean springBoot3 = SpringBootVersionUtils.isSpringBoot3(configuration.getSpringBootVersion());

        final List<Map<String, Object>> relations = new ArrayList<>();
        context.put(TemplateContextConstants.STRIPPED_MODEL_NAME, modelWithoutSuffix);
        context.put("resolverClassName", resolverClassName);
        context.put(TemplateContextConstants.CLASS_NAME, className);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("createArgs", FieldUtils.extractNonIdFieldNamesForResolver(modelDefinition.getFields()));
        context.put("updateArgs", FieldUtils.extractNonIdNonRelationFieldNamesForResolver(modelDefinition.getFields()));
        context.put(TemplateContextConstants.JSON_FIELDS, jsonFields);
        context.put("testImports", ResolverImports.computeMutationResolverTestImports(
                UnitTestUtils.isInstancioEnabled(configuration), !jsonFields.isEmpty(), configuration.getSpringBootVersion()
        ));

        context.put(TemplateContextConstants.FIELD_NAMES, FieldUtils.generateFieldNamesForCreateInputTO(modelDefinition.getFields()));
        context.put("fieldNamesWithoutRelations", FieldUtils.extractNonIdNonRelationFieldNames(modelDefinition.getFields()));
        context.put("projectImports", ResolverImports.computeProjectImportsForMutationUnitTests(outputDir, modelDefinition, packageConfiguration, isGlobalExceptionHandlerEnabled));
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, springBoot3);
        ValidationContextBuilder.contribute(
            modelDefinition, context,
            String.format("%s", context.get(TemplateContextConstants.DATA_GENERATOR_FIELD_NAME)),
            String.format("%s", context.get(TemplateContextConstants.DATA_GENERATOR_SINGLE_OBJ))
        );
        
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        relationFields.forEach(field -> {
            final ModelDefinition relationModel = entities.stream()
                    .filter(entity -> entity.getName().equals(field.getType()))
                    .findFirst()
                    .orElseThrow();
            final FieldDefinition relationIdField = FieldUtils.extractIdField(relationModel.getFields());
            relations.add(Map.of(
                TemplateContextConstants.RELATION_FIELD, field.getName(),
                TemplateContextConstants.RELATION_ID_TYPE, relationIdField.getType(),
                TemplateContextConstants.IS_COLLECTION, collectionRelationFields.contains(field.getName()),
                "invalidRelationIdType", UnitTestUtils.computeInvalidIdType(relationIdField)
            ));
        });
        context.put(TemplateContextConstants.RELATIONS, relations);

        return context;
    }

}
