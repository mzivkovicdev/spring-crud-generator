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

package dev.markozivkovic.springcrudgenerator.generators.tests;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants.GeneratorContextKeys;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.generators.CodeGenerator;
import dev.markozivkovic.springcrudgenerator.imports.ResolverImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ErrorResponse;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.springcrudgenerator.templates.GraphQlTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

public class GraphQlUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public GraphQlUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
                final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (!UnitTestUtils.isUnitTestsEnabled(configuration) || this.configuration.getGraphql() == null
                || !Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled())) {
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

        if (!GeneratorContext.isGenerated(GeneratorContextKeys.RESOLVER_TEST_CONFIG)) {
            this.generateConfigFile(testOutputDir, packagePath);
        }

        this.generateQueryUnitTests(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix);
        this.generateMutationUnitTests(modelDefinition, outputDir, testOutputDir, packagePath, modelWithoutSuffix);
    }

    /**
     * Generates a test configuration class for the GraphQL unit tests.
     *
     * @param testOutputDir    the directory where the generated unit test will be written
     * @param packagePath      the package path of the directory where the generated code will be written
     */
    private void generateConfigFile(final String testOutputDir, final String packagePath) {
        
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeResolversPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/resolver/test-config-class-template.ftl", Map.of()
                ));

        FileWriterUtils.writeToFile(
                testOutputDir, PackageUtils.computeResolversSubPackage(packageConfiguration),
                "ResolverTestConfiguration.java", sb.toString()
        );

        GeneratorContext.markGenerated(GeneratorContextKeys.RESOLVER_TEST_CONFIG);
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
        final String className = String.format("%sResolverMutationTest", modelWithoutSuffix);

        final Map<String, Object> context = GraphQlTemplateContext.computeMutationUnitTestContext(
                modelDefinition, configuration, packageConfiguration, entities, outputDir, testOutputDir
        );

        sb.append(String.format(PACKAGE, PackageUtils.computeResolversPackage(packagePath, packageConfiguration)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/resolver/mutation-class-test-template.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeResolversSubPackage(packageConfiguration), className, sb.toString());
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
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        final Boolean isGlobalExceptionHandlerEnabled = !(ErrorResponse.NONE.equals(this.configuration.getErrorResponse()) ||
                        Objects.isNull(this.configuration.getErrorResponse()));
        final boolean springBoot3 = SpringBootVersionUtils.isSpringBoot3(configuration.getSpringBootVersion());

        final Map<String, Object> context = new HashMap<>();
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("resolverClassName", resolverClassName);
        context.put("className", className);
        context.put("hasRelations", !FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty());
        context.put("modelName", modelDefinition.getName());
        context.put("idType", idField.getType());
        context.put("idField", idField.getName());
        context.put("invalidIdType", UnitTestUtils.computeInvalidIdType(idField));
        context.put("testImports", ResolverImports.computeQueryResolverTestImports(
                UnitTestUtils.isInstancioEnabled(configuration), configuration.getSpringBootVersion()
        ));
        context.put("projectImports", ResolverImports.computeProjectImportsForQueryUnitTests(outputDir, modelDefinition, packageConfiguration, isGlobalExceptionHandlerEnabled));
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        context.put("isGlobalExceptionHandlerEnabled", isGlobalExceptionHandlerEnabled);
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, springBoot3);

        sb.append(String.format(PACKAGE, PackageUtils.computeResolversPackage(packagePath, packageConfiguration)));
        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/resolver/query-class-test-template.ftl",
                context
        ));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeResolversSubPackage(packageConfiguration), className, sb.toString());
    }
    
}
