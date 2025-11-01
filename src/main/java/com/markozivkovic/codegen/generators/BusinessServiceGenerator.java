package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;
import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class BusinessServiceGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessServiceGenerator.class);

    private static final String RETRYABLE_ANNOTATION = "retryableAnnotation";

    private static final String BUSINESS_SERVICES = "businessservices";
    private static final String BUSINESS_SERVICES_PACKAGE = "." + BUSINESS_SERVICES;

    private final List<ModelDefinition> entites;

    public BusinessServiceGenerator(final List<ModelDefinition> entites) {
        this.entites = entites;
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

        sb.append(String.format(PACKAGE, packagePath + BUSINESS_SERVICES_PACKAGE));
        sb.append(ImportUtils.getBaseImport(modelDefinition, false, FieldUtils.hasCollectionRelation(modelDefinition, entites), false));

        if (FieldUtils.isAnyIdFieldUUID(modelDefinition, entites)) {
            sb.append(String.format(IMPORT, ImportConstants.Java.UUID));
        }

        sb.append(String.format(IMPORT, ImportConstants.Logger.LOGGER))
                .append(String.format(IMPORT, ImportConstants.Logger.LOGGER_FACTORY))
                .append(String.format(IMPORT, ImportConstants.SpringStereotype.SERVICE));
        
        if (!GeneratorContext.isGenerated(RETRYABLE_ANNOTATION)) {
            sb.append(String.format(IMPORT, ImportConstants.SpringTransaction.TRANSACTIONAL));
        }
        
        sb.append("\n")
                .append(ImportUtils.computeModelsEnumsAndServiceImports(modelDefinition, outputDir))
                .append("\n")
                .append(generateBusinessServiceClass(modelDefinition));

        FileWriterUtils.writeToFile(outputDir, BUSINESS_SERVICES, className, sb.toString());
    }

    /**
     * Generates the business service class for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the business service class
     */
    private String generateBusinessServiceClass(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeBusinessServiceContext(modelDefinition);
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

        final Map<String, Object> context = TemplateContextUtils.computeCreateResourceMethodServiceContext(modelDefinition, entites);
        
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
        
        final Map<String, Object> context = TemplateContextUtils.computeAddRelationMethodServiceContext(modelDefinition, entites);
        
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

        final Map<String, Object> context = TemplateContextUtils.computeRemoveRelationMethodServiceContext(modelDefinition, entites);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("businessservice/method/remove-relation.ftl", context);
    }

}
