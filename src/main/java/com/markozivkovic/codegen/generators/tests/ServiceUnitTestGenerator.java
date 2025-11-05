package com.markozivkovic.codegen.generators.tests;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class ServiceUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;

    public ServiceUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entites) {
        this.configuration = configuration;
        this.entities = entites;
    }
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (this.configuration == null || this.configuration.getUnitTests() == null || !this.configuration.getUnitTests()) {
            return;
        }
        
        final boolean hasIdField = FieldUtils.isAnyFieldId(modelDefinition.getFields());

        if (!hasIdField) {
            LOGGER.warn("Skipping service unit test generation for model: {} as it does not have an ID field", modelDefinition.getName());
            return;
        }
        
        LOGGER.info("Generating service unit test for model: {}", modelDefinition.getName());

        final String testOutputDir = outputDir.replace("main", "test");
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sServiceTest", modelWithoutSuffix);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.SERVICES)));
        sb.append(this.generateTestServiceClass(modelDefinition, outputDir));

        FileWriterUtils.writeToFile(testOutputDir, GeneratorConstants.DefaultPackageLayout.SERVICES, className, sb.toString());
    }

    /**
     * Generates the test service class for the given model definition.
     * 
     * The generated class contains unit tests for the service methods.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return a string representation of the test service class
     */
    private String generateTestServiceClass(final ModelDefinition modelDefinition, final String outputDir) {

        final Map<String, Object> context = new HashMap<>();
        final String baseImports = ImportUtils.getTestBaseImport(modelDefinition);
        final String projectImports = ImportUtils.computeModelsEnumsAndRepositoryImports(modelDefinition, outputDir);
        final String testImports = ImportUtils.computeTestServiceImports(modelDefinition, entities);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        context.put("baseImport", baseImports);
        context.put("projectImports", projectImports);
        context.put("testImports", testImports);
        context.put("className", String.format("%sServiceTest", modelWithoutSuffix));
        context.put("modelName", modelDefinition.getName());
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("fieldNames", FieldUtils.extractFieldNamesWithoutRelations(modelDefinition.getFields()));
        context.put("collectionFields", FieldUtils.extractCollectionRelationNames(modelDefinition));
        context.put("idField", idField);
        context.put("idType", idField.getType());
        context.put("getByIdMethod", this.generateGetByIdMethod(modelDefinition));
        context.put("getAllMethod", this.generateGetAllMethod(modelDefinition));
        context.put("createMethod", this.generateCreateMethod(modelDefinition));
        context.put("updateMethod", this.generateUpdateMethod(modelDefinition));
        context.put("deleteMethod", this.generateDeleteByIdMethod(modelDefinition));
        context.put("addRelationMethod", this.addRelationMethod(modelDefinition));
        context.put("removeRelationMethod", this.removeRelationMethod(modelDefinition));
        context.put("getAllByIds", this.getAllByIdsMethod(modelDefinition));
        context.put("getReferenceById", this.getReferenceByIdMethod(modelDefinition));

        return FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/service/service-test-class-template.ftl",
                context
        );
    }

    /**
     * Generates the getReferenceById method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the getReferenceById method, or null if the model has no relations
     */
    private String getReferenceByIdMethod(final ModelDefinition modelDefinition) {
        
        if (!FieldUtils.hasRelation(modelDefinition, entities)) {
            return null;
        }

        final Map<String, Object> context = TemplateContextUtils.createGetReferenceByIdMethodContext(modelDefinition);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/get-reference-by-id.ftl", context);
    }

    /**
     * Generates the getAllByIds method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the getAllByIds method
     */
    private String getAllByIdsMethod(final ModelDefinition modelDefinition) {

        if (!FieldUtils.hasCollectionRelation(modelDefinition, entities)) {
            return null;
        }
        final Map<String, Object> context = TemplateContextUtils.createGetAllByIdsMethodContext(modelDefinition);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/get-all-by-ids.ftl", context);
    }

    /**
     * Generates the removeRelation method as a string for the given model definition.
     * 
     * @param modelDefinition The model definition for which the removeRelation method
     *                        is to be generated.
     * @return A string representation of the removeRelation method, or null if the context
     *         is empty.
     */
    private String removeRelationMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.createRemoveRelationMethodContext(modelDefinition);
        if (context.isEmpty()) {
            return null;
        }

        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/remove-relation.ftl", context);
    }

    /**
     * Generates the addRelation method as a string for the given model definition.
     * 
     * The generated method adds a relation to a model entity and throws an exception if the
     * addition is not possible.
     * 
     * @param modelDefinition The model definition for which the addRelation method
     *                        is to be generated.
     * @return A string representation of the addRelation method, or null if the context
     *         is empty.
     */
    private Object addRelationMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.createAddRelationMethodContext(modelDefinition);
        if (context.isEmpty()) {
            return null;
        }
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/add-relation.ftl", context);
    }

    /**
     * Generates the deleteById method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the deleteById method
     */
    private String generateDeleteByIdMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeDeleteByIdContext(modelDefinition);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/delete-by-id.ftl", context);
    }

    /**
     * Generates the update method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the update method
     */
    private String generateUpdateMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeUpdateByIdContext(modelDefinition);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/update-by-id.ftl", context);
    }

    /**
     * Generates the create method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the create method
     */
    private String generateCreateMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeCreateContext(modelDefinition);
        context.put("fieldNamesList", FieldUtils.extractNonIdFieldNames(modelDefinition.getFields()));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/create.ftl", context);
    }

    /**
     * Generates the get all method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the get all method
     */
    private String generateGetAllMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeGetAllContext(modelDefinition);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/get-all.ftl", context);
    }

    /**
     * Generates the get by id method name for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the get by id method name
     */
    private String generateGetByIdMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeGetByIdContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/get-by-id.ftl", context);
    }

}
