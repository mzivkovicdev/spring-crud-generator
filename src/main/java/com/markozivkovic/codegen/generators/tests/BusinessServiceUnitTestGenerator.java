package com.markozivkovic.codegen.generators.tests;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
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
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class BusinessServiceUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessServiceUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entites;

    public BusinessServiceUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entites) {
        this.configuration = configuration;
        this.entites = entites;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (this.configuration == null || this.configuration.getUnitTests() == null || !this.configuration.getUnitTests()) {
            return;
        }
        
        final boolean hasIdField = FieldUtils.isAnyFieldId(modelDefinition.getFields());

        if (!hasIdField) {
            LOGGER.warn("Skipping business service unit test generation for model: {} as it does not have an ID field", modelDefinition.getName());
            return;
        }

        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            LOGGER.info("Model {} does not have any relation fields. Skipping business service test generation.", modelDefinition.getName());
            return;
        }

        LOGGER.info("Generating business service unit test for model: {}", modelDefinition.getName());
        
        final String testOutputDir = outputDir.replace("main", "test");
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sBusinessServiceTest", modelWithoutSuffix);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES)));
        sb.append(this.generateTestBusinessServiceClass(modelDefinition, outputDir));

        FileWriterUtils.writeToFile(testOutputDir, GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES, className, sb.toString());
    }

    /**
     * Generates the test business service class for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return a string representation of the test business service class
     */
    private String generateTestBusinessServiceClass(final ModelDefinition modelDefinition, final String outputDir) {
        
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sBusinessServiceTest", modelWithoutSuffix);

        final Map<String, Object> context = new HashMap<>();
        context.put("baseImport", ImportUtils.getBaseImport(modelDefinition, false, FieldUtils.hasCollectionRelation(modelDefinition, entites), false));
        context.put("projectImports", ImportUtils.computeModelsEnumsAndServiceImports(modelDefinition, outputDir));
        context.put("testImports", ImportUtils.computeTestBusinessServiceImports());
        context.putAll(TemplateContextUtils.computeBusinessServiceContext(modelDefinition));
        context.put("className", className);
        context.put("modelName", modelDefinition.getName());
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("createResource", createResourceMethod(modelDefinition));
        context.put("addRelationMethod", addRelationMethod(modelDefinition));
        context.put("removeRelationMethod", removeRelationMethod(modelDefinition));

        return FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/businessservice/businessservice-test-class-template.ftl", context
        );
    }

    /**
     * Generates the removeRelationMethod method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the removeRelationMethod unit test
     */
    private String removeRelationMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeRemoveRelationMethodServiceContext(modelDefinition, entites);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/businessservice/method/remove-relation.ftl", context
        );
    }

    /**
     * Generates the addRelationMethod method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the addRelationMethod unit test
     */
    private String addRelationMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeAddRelationMethodServiceContext(modelDefinition, entites);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/businessservice/method/add-relation.ftl", context
        );
    }

    /**
     * Generates the createResource method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the createResource method unit test
     */
    private String createResourceMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = new HashMap<>(
                TemplateContextUtils.computeCreateResourceMethodServiceContext(modelDefinition, entites)
        );
        final FieldDefinition id = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> fields = modelDefinition.getFields().stream()
                .filter(field -> !field.equals(id))
                .filter(field -> Objects.isNull(field.getRelation()))
                .collect(Collectors.toList());
        context.put("fields", fields);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/businessservice/method/create-resource.ftl", context
        );
    }
    
}
