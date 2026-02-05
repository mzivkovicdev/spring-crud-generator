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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.generators.CodeGenerator;
import dev.markozivkovic.springcrudgenerator.imports.ServiceImports;
import dev.markozivkovic.springcrudgenerator.imports.ServiceImports.ServiceImportScope;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.DataGeneratorTemplateContext;
import dev.markozivkovic.springcrudgenerator.templates.ServiceTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils;
import dev.markozivkovic.springcrudgenerator.utils.UnitTestUtils.TestDataGeneratorConfig;

public class ServiceUnitTestGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUnitTestGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public ServiceUnitTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
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
            LOGGER.warn("Skipping service unit test generation for model: {} as it does not have an ID field", modelDefinition.getName());
            return;
        }
        
        LOGGER.info("Generating service unit test for model: {}", modelDefinition.getName());

        final String testOutputDir = outputDir.replace("main", "test");
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sServiceTest", modelWithoutSuffix);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, PackageUtils.computeServicePackage(packagePath, packageConfiguration)));
        sb.append(this.generateTestServiceClass(modelDefinition, outputDir));

        FileWriterUtils.writeToFile(testOutputDir, PackageUtils.computeServiceSubPackage(packageConfiguration), className, sb.toString());
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

        final String baseImports = ServiceImports.getTestBaseImport(modelDefinition);
        final String projectImports = ServiceImports.computeModelsEnumsAndRepositoryImports(modelDefinition, outputDir, ServiceImportScope.SERVICE_TEST, packageConfiguration);
        final boolean isSpringBoot3 = SpringBootVersionUtils.isSpringBoot3(this.configuration.getSpringBootVersion());
        final String testImports = ServiceImports.computeTestServiceImports(
            modelDefinition, entities, UnitTestUtils.isInstancioEnabled(configuration), isSpringBoot3
        );
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        
        final Map<String, Object> context = new HashMap<>();
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
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);

        return FreeMarkerTemplateProcessorUtils.processTemplate(
                "test/unit/service/service-test-class-template.ftl",
                context
        );
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
        final Map<String, Object> context = ServiceTemplateContext.createGetAllByIdsMethodContext(modelDefinition);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
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
        
        final Map<String, Object> context = ServiceTemplateContext.createRemoveRelationMethodContext(modelDefinition, entities);
        if (context.isEmpty()) {
            return null;
        }
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

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

        final Map<String, Object> context = ServiceTemplateContext.createAddRelationMethodContext(modelDefinition);
        if (context.isEmpty()) {
            return null;
        }
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/add-relation.ftl", context);
    }

    /**
     * Generates the deleteById method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the deleteById method
     */
    private String generateDeleteByIdMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = ServiceTemplateContext.computeDeleteByIdContext(modelDefinition);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/delete-by-id.ftl", context);
    }

    /**
     * Generates the update method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the update method
     */
    private String generateUpdateMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = ServiceTemplateContext.computeUpdateByIdContext(modelDefinition);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/update-by-id.ftl", context);
    }

    /**
     * Generates the create method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the create method
     */
    private String generateCreateMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = ServiceTemplateContext.computeCreateContext(modelDefinition);
        context.put("fieldNamesList", FieldUtils.extractNonIdFieldNames(modelDefinition.getFields()));
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/create.ftl", context);
    }

    /**
     * Generates the get all method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the get all method
     */
    private String generateGetAllMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = ServiceTemplateContext.computeGetAllContext(modelDefinition);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/get-all.ftl", context);
    }

    /**
     * Generates the get by id method name for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the get by id method name
     */
    private String generateGetByIdMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = ServiceTemplateContext.computeGetByIdContext(modelDefinition);
        final TestDataGeneratorConfig generatorConfig = UnitTestUtils.resolveGeneratorConfig(configuration.getTests().getDataGenerator());
        context.putAll(DataGeneratorTemplateContext.computeDataGeneratorContext(generatorConfig));

        return FreeMarkerTemplateProcessorUtils.processTemplate("test/unit/service/method/get-by-id.ftl", context);
    }

}
