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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.generators.CodeGenerator;
import dev.markozivkovic.springcrudgenerator.imports.BusinessServiceImports;
import dev.markozivkovic.springcrudgenerator.imports.BusinessServiceImports.BusinessServiceImportScope;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.BusinessServiceTemplateContext;
import dev.markozivkovic.springcrudgenerator.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

public class BusinessServiceUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessServiceUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public BusinessServiceUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
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

        sb.append(String.format(PACKAGE, PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration)));
        sb.append(this.generateTestBusinessServiceClass(modelDefinition, outputDir));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeBusinessServiceSubPackage(packageConfiguration), className, sb.toString());
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
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());

        final Map<String, Object> context = new HashMap<>();
        context.put("baseImport", BusinessServiceImports.getTestBaseImport(modelDefinition));
        context.put("projectImports", BusinessServiceImports.computeModelsEnumsAndServiceImports(modelDefinition, outputDir, BusinessServiceImportScope.BUSINESS_SERVICE_TEST, packageConfiguration));
        context.put("testImports", BusinessServiceImports.computeTestBusinessServiceImports(UnitTestUtils.isInstancioEnabled(configuration)));
        context.putAll(BusinessServiceTemplateContext.computeBusinessServiceContext(modelDefinition));
        context.put("className", className);
        context.put("modelName", modelDefinition.getName());
        context.put("strippedModelName", modelWithoutSuffix);
        context.put("createResource", createResourceMethod(modelDefinition));
        context.put("addRelationMethod", addRelationMethod(modelDefinition));
        context.put("removeRelationMethod", removeRelationMethod(modelDefinition));
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

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

        final Map<String, Object> context = BusinessServiceTemplateContext.computeRemoveRelationMethodServiceContext(modelDefinition, entities);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
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

        final Map<String, Object> context = BusinessServiceTemplateContext.computeAddRelationMethodServiceContext(modelDefinition, entities);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
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
                BusinessServiceTemplateContext.computeCreateResourceMethodServiceContext(modelDefinition, entities)
        );
        final FieldDefinition id = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> fields = modelDefinition.getFields().stream()
                .filter(field -> !field.equals(id))
                .filter(field -> Objects.isNull(field.getRelation()))
                .collect(Collectors.toList());
        context.put("fields", fields);
        
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

        return FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/businessservice/method/create-resource.ftl", context
        );
    }
    
}
