package com.markozivkovic.codegen.generators.tests;

import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.generators.CodeGenerator;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils;

public class RestControllerUnitTestGenerator implements CodeGenerator {

    private static final String CONTROLLERS = "controllers";
    private static final String CONTROLLERS_PACKAGE = "." + CONTROLLERS;

    private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;

    public RestControllerUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.entities = entities;
    }
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (this.configuration == null || this.configuration.getUnitTests() == null || !this.configuration.getUnitTests()) {
            return;
        }

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            LOGGER.warn("Model {} does not have an ID field. Skipping controller unit test generation.", modelDefinition.getName());
            return;
        }

        LOGGER.info("Generating controller unit test for model: {}", modelDefinition.getName());

        final String testOutputDir = outputDir.replace("main", "test");
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final Boolean swagger = Objects.nonNull(this.configuration) && Objects.nonNull(this.configuration.getSwagger())
                && this.configuration.isSwagger();

        this.generateGetEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger);
        this.generateDeleteByIdEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix);
        this.generateUpdateByIdEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger);
        this.generateCreateEndpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger);
        this.generateAddRelationEdnpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger);
        this.generateRemoveRelationEdnpointTest(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix, swagger);
    }

    /**
     * Generates a unit test for the add relation endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition    the model definition containing the class name and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param testOutputDir      the directory where the generated unit test will be written
     * @param packagePath        the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     * @param swagger            indicates if the swagger and open API generator is enabled
     */
    private void generateAddRelationEdnpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
                final String packagePath, final String modelWithoutSuffix, final Boolean swagger) {

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        if (relationFields.isEmpty()) {
                return; 
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        relationFields.forEach(relationField -> {

                final StringBuilder sb = new StringBuilder();
                final String strippedRelationField = ModelNameUtils.stripSuffix(relationField.getType());
                final String className = String.format("%sAdd%sMockMvcTest", modelWithoutSuffix, strippedRelationField);
                final String controllerClassName = String.format("%sController", modelWithoutSuffix);

                final Map<String, Object> context = new HashMap<>();
                context.put("controllerClassName", controllerClassName);
                context.put("className", className);
                context.put("modelName", modelDefinition.getName());
                context.put(
                        "methodName",
                        String.format("%ssId%ssPost", StringUtils.uncapitalize(modelWithoutSuffix), strippedRelationField)
                );
                context.put("strippedModelName", modelWithoutSuffix);
                context.put("idType", idField.getType());
                context.put("idField", idField.getName());
                context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
                context.put("strippedRelationClassName", strippedRelationField);
                context.put("relationFieldModel", StringUtils.capitalize(relationField.getName()));
                context.put("baseImports", ImportUtils.computeAddRelationEndpointBaseImports(modelDefinition));
                context.put("projectImports", ImportUtils.computeControllerTestProjectImports(
                        modelDefinition, outputDir, swagger, true, true
                ));
                context.put("testImports", ImportUtils.computeAddRelationEndpointTestImports());
                context.put("swagger", swagger);

                sb.append(String.format(PACKAGE, packagePath + CONTROLLERS_PACKAGE));
                sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                        "test/unit/controller/endpoint/add-resource-relation.ftl",
                        context
                ));

                FileWriterUtils.writeToFile(testOutputDir, CONTROLLERS, className, sb.toString());
        });
    }

    /**
     * Generates a unit test class for the remove relation endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition    the model definition containing the class name and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param testOutputDir      the directory where the generated unit test will be written
     * @param packagePath        the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     * @param swagger            indicates if the swagger and open API generator is enabled
     */
    private void generateRemoveRelationEdnpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
                final String packagePath, final String modelWithoutSuffix, final Boolean swagger) {

        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        if (relationFields.isEmpty()) {
                return; 
        }

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<String> collectionRelationFields = FieldUtils.extractCollectionRelationNames(modelDefinition);

        relationFields.forEach(relationField -> {

                final StringBuilder sb = new StringBuilder();
                final String strippedRelationField = ModelNameUtils.stripSuffix(relationField.getType());
                final String className = String.format("%sRemove%sMockMvcTest", modelWithoutSuffix, strippedRelationField);
                final String controllerClassName = String.format("%sController", modelWithoutSuffix);
                final ModelDefinition relatedModelDefinition = this.entities.stream()
                        .filter(e -> e.getName().equals(relationField.getType()))
                        .findFirst()
                        .orElseThrow();
                final FieldDefinition relatedIdField = FieldUtils.extractIdField(relatedModelDefinition.getFields());

                final Map<String, Object> context = new HashMap<>();
                context.put("controllerClassName", controllerClassName);
                context.put("className", className);
                context.put(
                        "methodName",
                        String.format("%ssId%ssDelete", StringUtils.uncapitalize(modelWithoutSuffix), strippedRelationField)
                );
                context.put("strippedModelName", modelWithoutSuffix);
                context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
                context.put("isCollection", collectionRelationFields.contains(relationField.getName()));
                context.put("idType", idField.getType());
                context.put("idField", idField.getName());
                context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
                context.put("strippedRelationClassName", strippedRelationField);
                context.put("relationFieldModel", StringUtils.capitalize(relationField.getName()));
                context.put("relIdType", relatedIdField.getType());
                context.put("relIdField", relatedIdField.getName());
                context.put("invalidRelIdType", UnitTestUtils.computeInvalidIdType(relatedIdField));
                context.put("baseImports", ImportUtils.computeRemoveRelationEndpointBaseImports(modelDefinition, entities));
                context.put("projectImports", ImportUtils.computeControllerTestProjectImports(
                        modelDefinition, outputDir, false, false, false
                ));
                context.put("testImports", ImportUtils.computeDeleteEndpointTestImports());

                sb.append(String.format(PACKAGE, packagePath + CONTROLLERS_PACKAGE));
                sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                        "test/unit/controller/endpoint/remove-resource-relation.ftl",
                        context
                ));

                FileWriterUtils.writeToFile(testOutputDir, CONTROLLERS, className, sb.toString());
        });
    }

    /**
     * Generates a unit test for the create endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition    the model definition containing the class name and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param testOutputDir      the directory where the generated unit test will be written
     * @param packagePath        the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     * @param swagger            indicates if the swagger and open API generator is enabled
     */
    private void generateCreateEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix, final Boolean swagger) {
        
        final StringBuilder sb = new StringBuilder();
        final String className = String.format("%sCreateMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());

        final Map<String, Object> context = new HashMap<>(
                TemplateContextUtils.computeCreateEndpointContext(modelDefinition, entities)
        );
        context.put("controllerClassName", controllerClassName);
        context.put("className", className);
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("hasCollectionRelations", FieldUtils.isAnyRelationManyToMany(modelDefinition.getFields()) 
                || FieldUtils.isAnyRelationOneToMany(modelDefinition.getFields()));
        context.put("swagger", swagger);
        context.put("testImports", ImportUtils.computeUpdateEndpointTestImports());
        context.put("projectImports", ImportUtils.computeCreateEndpointTestProjectImports( 
                modelDefinition, outputDir, swagger
        ));
        context.put("jsonFields", jsonFields);

        sb.append(String.format(PACKAGE, packagePath + CONTROLLERS_PACKAGE));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/create-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, CONTROLLERS, className, sb.toString());
    }

    /**
     * Generates a unit test for the update by ID endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition    the model definition containing the class name and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param testOutputDir      the directory where the generated unit test will be written
     * @param packagePath        the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     * @param swagger            indicates if the swagger and open API generator is enabled
     */
    private void generateUpdateByIdEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix, final Boolean swagger) {
        
        final StringBuilder sb = new StringBuilder();
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String className = String.format("%sUpdateByIdMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());

        final Map<String, Object> context = new HashMap<>();
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
        context.put("testImports", ImportUtils.computeUpdateEndpointTestImports());
        context.put("projectImports", ImportUtils.computeUpdateEndpointTestProjectImports( 
                modelDefinition, outputDir, swagger
        ));
        context.put("jsonFields", jsonFields);

        sb.append(String.format(PACKAGE, packagePath + CONTROLLERS_PACKAGE));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/update-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, CONTROLLERS, className, sb.toString());
    }

    /**
     * Generates a unit test for the delete by ID endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition the model definition containing the class name and field definitions
     * @param outputDir the directory where the generated code will be written
     * @param testOutputDir the directory where the generated unit test will be written
     * @param packagePath the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     */
    private void generateDeleteByIdEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix) {

        final StringBuilder sb = new StringBuilder();
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String className = String.format("%sDeleteByIdMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);

        final Map<String, Object> context = new HashMap<>();
        context.put("controllerClassName", controllerClassName);
        context.put("className", className);
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("projectImports", ImportUtils.computeControllerTestProjectImports(
                modelDefinition, outputDir, false, false, false
        ));
        context.put("testImports", ImportUtils.computeDeleteEndpointTestImports());

        sb.append(String.format(PACKAGE, packagePath + CONTROLLERS_PACKAGE));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/delete-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, CONTROLLERS, className, sb.toString());
    }

    /**
     * Generates a unit test class for the get endpoint of the REST controller
     * for the given model definition.
     *
     * @param modelDefinition    the model definition containing the class name and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param testOutputDir      the directory where the generated unit test will be written
     * @param packagePath        the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     * @param swagger            indicates if the swagger and open API generator is enabled
     */
    private void generateGetEndpointTest(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
            final String packagePath, final String modelWithoutSuffix, final Boolean swagger) {
        
        final StringBuilder sb = new StringBuilder();
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String className = String.format("%sGetMockMvcTest", modelWithoutSuffix);
        final String controllerClassName = String.format("%sController", modelWithoutSuffix);

        final Map<String, Object> context = new HashMap<>();
        context.put("controllerClassName", controllerClassName);
        context.put("className", className);
        context.put("swagger", swagger);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("modelName", modelDefinition.getName());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("testImports", ImportUtils.computeGetEndpointTestImports());
        context.put("projectImports", ImportUtils.computeControllerTestProjectImports(
                modelDefinition, outputDir, swagger, false, true
        ));

        sb.append(String.format(PACKAGE, packagePath + CONTROLLERS_PACKAGE));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/controller/endpoint/get-resource.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, CONTROLLERS, className, sb.toString());
    }

}
