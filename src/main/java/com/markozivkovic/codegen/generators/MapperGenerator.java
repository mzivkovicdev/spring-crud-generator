package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class MapperGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MapperGenerator.class);

    private static final String MAPPERS_REST = "mappers/rest";
    private static final String MAPPERS_REST_PACKAGE = ".mappers.rest";
    private static final String MAPPERS_REST_HELPERS = "mappers/rest/helpers";
    private static final String MAPPERS_HELPERS_PACKAGE = MAPPERS_REST_PACKAGE + ".helpers";
    private static final String MODELS_PACKAGE = ".models";
    private static final String MODELS_HELPERS_PACKAGE = MODELS_PACKAGE + ".helpers";
    private static final String TRANSFER_OBJECTS = "transferobjects";
    private static final String TRANSFER_OBJECTS_REST_PACKAGE = "." + TRANSFER_OBJECTS + ".rest";
    private static final String TRANSFER_OBJECTS_HELPERS_PACKAGE = TRANSFER_OBJECTS_REST_PACKAGE + ".helpers";
    
    private static final String MAPPERS_GRAPHQL = "mappers/graphql";
    private static final String MAPPERS_GRAPHQL_HELPERS = "mappers/graphql/helpers";
    private static final String MAPPERS_GRAPHQL_PACKAGE = ".mappers.graphql";
    private static final String MAPPERS_GRAPHQL_HELPERS_PACKAGE = MAPPERS_GRAPHQL_PACKAGE + ".helpers";
    private static final String TRANSFER_OBJECTS_GRAPHQL_PACKAGE = "." + TRANSFER_OBJECTS + ".graphql";
    private static final String TRANSFER_OBJECTS_GRAPH_QL_HELPERS_PACKAGE = TRANSFER_OBJECTS_GRAPHQL_PACKAGE + ".helpers";
    
    private static final String GENERATED_RESOURCE_MODEL_RESOURCE = ".generated.%s.model.%s";

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;

    public MapperGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }
        
        LOGGER.info("Generating mapper for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final boolean swagger = configuration != null && configuration.getSwagger() != null && configuration.getSwagger();

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
        final String mapperName = String.format("%sMapper", strippedModelName);
        final String transferObjectName = String.format("%sTO", strippedModelName);
        final String modelImport = String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName());
        final String transferObjectImport;
        
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + transferObjectName);
        } else {
            transferObjectImport = String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + transferObjectName);
        }

        final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final String helperMapperImports = jsonFields.stream()
                .map(FieldUtils::extractJsonFieldName)
                .map(field -> String.format(
                    IMPORT, packagePath + (isGraphQl ? MAPPERS_GRAPHQL_HELPERS_PACKAGE : MAPPERS_HELPERS_PACKAGE) + "." + field + "Mapper"
                ))
                .collect(Collectors.joining(", "));

        final Map<String, Object> context = new HashMap<>();
        context.put("modelImport", modelImport);
        context.put("transferObjectImport", transferObjectImport);
        
        if (StringUtils.isNotBlank(helperMapperImports)) {
            context.put("helperMapperImports", helperMapperImports);
        }
        
        context.put("modelName", modelDefinition.getName());
        context.put("mapperName", mapperName);
        context.put("transferObjectName", transferObjectName);
        context.put("swagger", swagger);
        if (swagger) {
            context.put("swaggerModel", ModelNameUtils.stripSuffix(modelDefinition.getName()));
            context.put("generatedModelImport", String.format(
                    IMPORT,
                    String.format(
                        packagePath + GENERATED_RESOURCE_MODEL_RESOURCE,
                        StringUtils.uncapitalize(strippedModelName), strippedModelName
                    )
            ));
        }
        
        if (!relationFields.isEmpty() || !jsonFields.isEmpty()) {
            final String mapperParameters = Stream.concat(relationFields.stream(), jsonFields.stream())
                    .map(field -> {
                        
                        if (FieldUtils.isJsonField(field)) {
                            return FieldUtils.extractJsonFieldName(field);
                        } else {
                            return ModelNameUtils.stripSuffix(field.getType());
                        }
                    })
                    .map(field -> String.format("%sMapper.class", field))
                    .distinct()
                    .collect(Collectors.joining(", "));
            context.put("parameters", mapperParameters);
            LOGGER.info("Mapper parameters: {}", mapperParameters);
        }

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("mapper/mapper-template.ftl", context);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, isGraphQl ? (packagePath + MAPPERS_GRAPHQL_PACKAGE) : (packagePath + MAPPERS_REST_PACKAGE)))
                .append(mapperTemplate);

        FileWriterUtils.writeToFile(outputDir, isGraphQl ? MAPPERS_GRAPHQL : MAPPERS_REST, mapperName, sb.toString());
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
        
        final String mapperName = String.format("%sMapper", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));

        final String modelImport = String.format(IMPORT, packagePath + MODELS_HELPERS_PACKAGE + "." + jsonModel.getName());
        final String transferObjectImport;
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPH_QL_HELPERS_PACKAGE + "." + transferObjectName);
        } else {
            transferObjectImport = String.format(IMPORT, packagePath + TRANSFER_OBJECTS_HELPERS_PACKAGE + "." + transferObjectName);
        }

        final Map<String, Object> context = new HashMap<>();
        context.put("modelImport", modelImport);
        context.put("transferObjectImport", transferObjectImport);
        context.put("modelName", jsonModel.getName());
        context.put("mapperName", mapperName);
        context.put("transferObjectName", transferObjectName);

        if (swagger) {
            context.put("generatedModelImport", String.format(
                packagePath + GENERATED_RESOURCE_MODEL_RESOURCE,
                StringUtils.uncapitalize(ModelNameUtils.stripSuffix(parentModel.getName())), ModelNameUtils.stripSuffix(jsonModel.getName())    
            ));
        }

        context.put("swagger", false);
        context.put("swaggerModel", ModelNameUtils.stripSuffix(jsonModel.getName()));
        context.put("generateAllHelperMethods", swagger);

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("mapper/mapper-template.ftl", context);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, isGraphQl ? (packagePath + MAPPERS_GRAPHQL_HELPERS_PACKAGE) : (packagePath + MAPPERS_HELPERS_PACKAGE)))
                .append(mapperTemplate);

        FileWriterUtils.writeToFile(outputDir, isGraphQl ? MAPPERS_GRAPHQL_HELPERS : MAPPERS_REST_HELPERS, mapperName, sb.toString());
    }

}
