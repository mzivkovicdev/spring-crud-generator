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

package dev.markozivkovic.codegen.generators;

import static dev.markozivkovic.codegen.constants.ImportConstants.IMPORT;
import static dev.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.constants.ImportConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.imports.BusinessServiceImports;
import dev.markozivkovic.codegen.imports.BusinessServiceImports.BusinessServiceImportScope;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.BusinessServiceTemplateContext;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

public class BusinessServiceGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessServiceGenerator.class);

    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public BusinessServiceGenerator(final List<ModelDefinition> entities, final PackageConfiguration packageConfiguration) {
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generating business service for model: {}", modelDefinition.getName());

        final boolean hasIdField = FieldUtils.isAnyFieldId(modelDefinition.getFields());
        
        if (!hasIdField) {
            LOGGER.warn("Model {} does not have an ID field. Skipping service generation.", modelDefinition.getName());
            return;
        }

        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            LOGGER.info("Model {} does not have any relation fields. Skipping business service generation.", modelDefinition.getName());
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sBusinessService", modelWithoutSuffix);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration)));
        sb.append(BusinessServiceImports.getBaseImport(modelDefinition, FieldUtils.hasCollectionRelation(modelDefinition, entities)));

        if (FieldUtils.isAnyIdFieldUUID(modelDefinition, entities)) {
            sb.append(String.format(IMPORT, ImportConstants.Java.UUID));
        }

        sb.append(String.format(IMPORT, ImportConstants.Logger.LOGGER))
                .append(String.format(IMPORT, ImportConstants.Logger.LOGGER_FACTORY))
                .append(String.format(IMPORT, ImportConstants.SpringStereotype.SERVICE));
        
        if (!GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.RETRYABLE_ANNOTATION)) {
            sb.append(String.format(IMPORT, ImportConstants.SpringTransaction.TRANSACTIONAL));
        }
        
        sb.append(System.lineSeparator())
                .append(BusinessServiceImports.computeModelsEnumsAndServiceImports(modelDefinition, outputDir, BusinessServiceImportScope.BUSINESS_SERVICE, packageConfiguration))
                .append(System.lineSeparator())
                .append(generateBusinessServiceClass(modelDefinition));

        FileWriterUtils.writeToFile(outputDir, PackageUtils.computeBusinessServiceSubPackage(packageConfiguration), className, sb.toString());
    }

    /**
     * Generates the business service class for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the business service class
     */
    private String generateBusinessServiceClass(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = BusinessServiceTemplateContext.computeBusinessServiceContext(modelDefinition);
        context.put("createResource", createResourceMethod(modelDefinition));
        context.put("addRelationMethod", addRelationMethod(modelDefinition));
        context.put("removeRelationMethod", removeRelationMethod(modelDefinition));
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("businessservice/business-service-class-template.ftl", context);
    }

    /**
     * Generates the createResource method as a string for the given model definition.
     * 
     * @param modelDefinition the model definition
     * @return a string representation of the createResource method
     */
    private String createResourceMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = BusinessServiceTemplateContext.computeCreateResourceMethodServiceContext(modelDefinition, entities);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("businessservice/method/create-resource.ftl", context);
    }

    /**
     * Generates the addRelation method as a string for the given model definition.
     * 
     * @param modelDefinition The model definition for which the addRelation method
     *                        is to be generated.
     * @return A string representation of the addRelation method.
     */
    private String addRelationMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = BusinessServiceTemplateContext.computeAddRelationMethodServiceContext(modelDefinition, entities);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("businessservice/method/add-relation.ftl", context);
    }

    /**
     * Generates the removeRelation method as a string for the given model definition.
     * 
     * @param modelDefinition The model definition for which the removeRelation method
     *                        is to be generated.
     * @return A string representation of the removeRelation method.
     */
    private String removeRelationMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = BusinessServiceTemplateContext.computeRemoveRelationMethodServiceContext(modelDefinition, entities);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("businessservice/method/remove-relation.ftl", context);
    }

}
