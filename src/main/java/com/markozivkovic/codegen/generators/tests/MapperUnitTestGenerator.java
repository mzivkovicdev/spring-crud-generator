package com.markozivkovic.codegen.generators.tests;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;
import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.generators.CodeGenerator;
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

public class MapperUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapperUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;

    public MapperUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (this.configuration == null || this.configuration.getUnitTests() == null || !this.configuration.getUnitTests()) {
            return;
        }

        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }

        LOGGER.info("Generating mapper test for model: {}", modelDefinition.getName());

        final String testOutputDir = outputDir.replace("main", "test");
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
                    
                    this.generateHelperMapperTest(modelDefinition, jsonModel, testOutputDir, packagePath, false, swagger);
                    if (configuration != null && configuration.getGraphQl() != null && configuration.getGraphQl()) {
                        this.generateHelperMapperTest(modelDefinition, jsonModel, testOutputDir, packagePath, true, false);
                    }
                });

        this.generateMapperTest(modelDefinition, testOutputDir, packagePath, false, swagger);
        if (configuration != null && configuration.getGraphQl() != null && configuration.getGraphQl()) {
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
        final String modelImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MODELS, modelDefinition.getName()));
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String transferObjectImport;
        
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, transferObjectName));
        } else {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST, transferObjectName));
        }

        final Map<String, Object> context = new HashMap<>();
        context.put("modelImport", modelImport);
        context.put("transferObjectImport", transferObjectImport);
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
            context.put("swaggerModel", ModelNameUtils.stripSuffix(modelDefinition.getName()));
            context.put("generatedModelImport", String.format(
                    IMPORT,
                    PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.GENERATED, StringUtils.uncapitalize(strippedModelName), GeneratorConstants.DefaultPackageLayout.MODEL, strippedModelName)
            ));
        }

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/mapper/mapper-test-template.ftl",
                context
        );

        final StringBuilder sb = new StringBuilder();
        final String packagePathResolved = isGraphQl ?
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL) :
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST);
        final String filePathResolved = isGraphQl ? 
                FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL) :
                FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST);
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
        final String modelImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MODELS, GeneratorConstants.DefaultPackageLayout.HELPERS, jsonModel.getName()));
        final String transferObjectImport;
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS, transferObjectName));
        } else {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS, transferObjectName));
        }
        final List<String> enumFields = FieldUtils.extractEnumFields(jsonModel.getFields()).stream()
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());

        final Map<String, Object> context = new HashMap<>();
        context.put("modelImport", modelImport);
        context.put("transferObjectImport", transferObjectImport);
        context.put("className", className);
        context.put("modelName", jsonModel.getName());
        context.put("strippedModelName", strippedModelName);
        context.put("transferObjectName", transferObjectName);
        context.put("swagger", false);
        context.put("isGraphQL", isGraphQl);
        context.put("idField", jsonModel.getFields().stream().findAny().orElseThrow().getName());
        context.put("swaggerModel", strippedModelName);
        context.put("generateAllHelperMethods", swagger);
        context.put("fieldNames", FieldUtils.extractFieldNames(jsonModel.getFields()));
        context.put("enumFields", enumFields);

        if (swagger) {
            context.put("generatedModelImport", String.format(
                PackageUtils.join(
                    packagePath, GeneratorConstants.DefaultPackageLayout.GENERATED, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(parentModel.getName())),
                    GeneratorConstants.DefaultPackageLayout.MODEL, ModelNameUtils.stripSuffix(jsonModel.getName())
                )
            ));
        }

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/mapper/mapper-test-template.ftl",
                context
        );

        final StringBuilder sb = new StringBuilder();
        final String packagePathResolved = isGraphQl ?
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS) :
                PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS);
        final String filePathResolved = isGraphQl ? 
                FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS) :
                FileUtils.join(GeneratorConstants.DefaultPackageLayout.MAPPERS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS);
        sb.append(String.format(PACKAGE, packagePathResolved))
                .append(mapperTemplate);
        
        FileWriterUtils.writeToFile(outputDir, filePathResolved, className, sb.toString());
    }
    
}
