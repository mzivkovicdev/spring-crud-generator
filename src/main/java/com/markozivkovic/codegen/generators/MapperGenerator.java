package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;
import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class MapperGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MapperGenerator.class);
    
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
        final String mapperName = isGraphQl ? String.format("%sGraphQLMapper", strippedModelName) :
                String.format("%sRestMapper", strippedModelName);
        final String transferObjectName = String.format("%sTO", strippedModelName);
        final String modelImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MODELS, modelDefinition.getName()));
        final String transferObjectImport;
        
        if (isGraphQl) {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, transferObjectName)
            );
        } else {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST, transferObjectName)
            );
        }

        final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final String helperMapperImports = jsonFields.stream()
                .map(FieldUtils::extractJsonFieldName)
                .map(field -> {
                    final String resolvedPackage = isGraphQl ?
                            PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS, String.format("%sGraphQLMapper", field)) :
                            PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS, String.format("%sRestMapper", field));
                    return String.format(IMPORT, resolvedPackage);
                })
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
                    .map(field -> isGraphQl ? String.format("%sGraphQLMapper.class", field) : String.format("%sRestMapper.class", field))
                    .distinct()
                    .collect(Collectors.joining(", "));
            context.put("parameters", mapperParameters);
            LOGGER.info("Mapper parameters: {}", mapperParameters);
        }

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("mapper/mapper-template.ftl", context);
        final String resolvedPackagePath = isGraphQl ?
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL) : 
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, resolvedPackagePath))
                .append(mapperTemplate);

        final String filePath = isGraphQl ? FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL)
                : FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST);
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
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final String modelImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MODELS, GeneratorConstants.DefaultPackageLayout.HELPERS, jsonModel.getName()));
        final String transferObjectImport;
        if (isGraphQl) {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS, transferObjectName)
            );
        } else {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS, transferObjectName)
            );
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

        final String resolvedPackagePath = isGraphQl ? 
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS) :
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, resolvedPackagePath))
                .append(mapperTemplate);

        final String filePath = isGraphQl ?
                FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS) : 
                FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS);
        FileWriterUtils.writeToFile(outputDir, filePath, mapperName, sb.toString());
    }

}
