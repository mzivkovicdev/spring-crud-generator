package com.markozivkovic.codegen.generators.tests;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.ArrayList;
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
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.UnitTestUtils;

public class GraphQlUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;

    public GraphQlUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (!UnitTestUtils.isUnitTestsEnabled(configuration) || this.configuration.getGraphQl() == null
                || !this.configuration.getGraphQl()) {
            return;
        }

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            LOGGER.warn("Model {} does not have an ID field. Skipping controller unit test generation.", modelDefinition.getName());
            return;
        }

        LOGGER.info("Generating GraphQL unit test for model: {}", modelDefinition.getName());

        final String testOutputDir = outputDir.replace("main", "test");
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        this.generateQueryUnitTests(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix);
        this.generateMutationUnitTests(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix);
    }

    /**
     * Generates a unit test class for the mutation resolver of the given model definition.
     * The unit test class is written according to the following template: test/unit/resolver/mutation-class-test-template.ftl
     *
     * @param modelDefinition    the model definition containing the class name and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param testOutputDir      the directory where the generated unit test will be written
     * @param packagePath        the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     */
    private void generateMutationUnitTests(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
                final String packagePath, final String modelWithoutSuffix) {

        final StringBuilder sb = new StringBuilder();
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String className = String.format("%sResolverMutationTest", modelWithoutSuffix);
        final String resolverClassName = String.format("%sResolver", modelWithoutSuffix);
        final List<String> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields()).stream()
                .map(FieldUtils::extractJsonFieldName)
                .collect(Collectors.toList());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<String> collectionRelationFields = FieldUtils.extractCollectionRelationNames(modelDefinition);

        final Map<String, Object> context = new HashMap<>();
        final List<Map<String, Object>> relations = new ArrayList<>();
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("resolverClassName", resolverClassName);
        context.put("className", className);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("modelName", modelDefinition.getName());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("createArgs", FieldUtils.extractNonIdFieldNamesForResolver(modelDefinition.getFields()));
        context.put("updateArgs", FieldUtils.extractNonIdNonRelationFieldNamesForResolver(modelDefinition.getFields()));
        context.put("jsonFields", jsonFields);
        context.put("testImports", ImportUtils.computeMutationResolverTestImports());
        context.put("projectImports", ImportUtils.computeProjectImportsForMutationUnitTests(outputDir, modelDefinition));
        
        relationFields.forEach(field -> {
            final ModelDefinition relationModel = this.entities.stream()
                    .filter(entity -> entity.getName().equals(field.getType()))
                    .findFirst()
                    .orElseThrow();
            final FieldDefinition relationIdField = FieldUtils.extractIdField(relationModel.getFields());
            relations.add(Map.of(
                "relationField", field.getName(),
                "relationIdType", relationIdField.getType(),
                "isCollection", collectionRelationFields.contains(field.getName()),
                "invalidRelationIdType", UnitTestUtils.computeInvalidIdType(relationIdField)
            ));
        });
        context.put("relations", relations);

        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.RESOLVERS)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/resolver/mutation-class-test-template.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, GeneratorConstants.DefaultPackageLayout.RESOLVERS, className, sb.toString());
    }

    /**
     * Generates a unit test class for the GraphQL query resolver of the given model definition.
     * The unit test class is written according to the following template: test/unit/resolver/query-mapping-class-test-template.ftl
     *
     * @param modelDefinition    the model definition containing the class name and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param testOutputDir      the directory where the generated unit test will be written
     * @param packagePath        the package path of the directory where the generated code will be written
     * @param modelWithoutSuffix the model name without the suffix
     */
    private void generateQueryUnitTests(final ModelDefinition modelDefinition, final String outputDir, final String testOutputDir,
                final String packagePath, final String modelWithoutSuffix) {
            
        final StringBuilder sb = new StringBuilder();
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final String className = String.format("%sResolverQueryTest", modelWithoutSuffix);
        final String resolverClassName = String.format("%sResolver", modelWithoutSuffix);

        final Map<String, Object> context = new HashMap<>();
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("resolverClassName", resolverClassName);
        context.put("className", className);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("modelName", modelDefinition.getName());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("testImports", ImportUtils.computeQueryResolverTestImports());
        context.put("projectImports", ImportUtils.computeProjectImportsForQueryUnitTests(outputDir, modelDefinition));

        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.RESOLVERS)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/resolver/query-class-test-template.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, GeneratorConstants.DefaultPackageLayout.RESOLVERS, className, sb.toString());
    }
    
}
