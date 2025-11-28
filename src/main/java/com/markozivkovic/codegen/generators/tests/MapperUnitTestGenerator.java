package com.markozivkovic.codegen.generators.tests;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;
import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.generators.CodeGenerator;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.templates.DataGeneratorTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils.TestDataGeneratorConfig;

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
        final String modelImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName()));
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String transferObjectImport;
        
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        } else {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        }

        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
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
            context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.computeOpenApiModelName(modelDefinition.getName()));
            context.put(TemplateContextConstants.GENERATED_MODEL_IMPORT, String.format(
                    IMPORT,
                    PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(strippedModelName)), ModelNameUtils.computeOpenApiModelName(strippedModelName))
            ));
        }

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
        final String modelImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration), jsonModel.getName()));
        final String transferObjectImport;
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        } else {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        }
        final List<String> enumFields = FieldUtils.extractEnumFields(jsonModel.getFields()).stream()
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());

        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());

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
        context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.computeOpenApiModelName(strippedModelName));
        context.put("generateAllHelperMethods", swagger);
        context.put("fieldNames", FieldUtils.extractFieldNames(jsonModel.getFields()));
        context.put("enumFields", enumFields);
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

        if (swagger) {
            context.put(TemplateContextConstants.GENERATED_MODEL_IMPORT, String.format(
                PackageUtils.join(
                    PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(parentModel.getName()))), ModelNameUtils.stripSuffix(jsonModel.getName())
                )
            ));
        }

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
