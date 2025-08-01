package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JPAConstants.SPRING_DATA_PACKAGE_DOMAIN_PAGE;
import static com.markozivkovic.codegen.constants.JPAConstants.SPRING_DATA_PACKAGE_DOMAIN_PAGE_REQUEST;
import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;
import static com.markozivkovic.codegen.constants.LoggerConstants.SL4J_LOGGER;
import static com.markozivkovic.codegen.constants.LoggerConstants.SL4J_LOGGER_FACTORY;
import static com.markozivkovic.codegen.constants.SpringConstants.SPRING_FRAMEWORK_STEREOTYPE_SERVICE;
import static com.markozivkovic.codegen.constants.TransactionConstants.SPRING_FRAMEWORK_TRANSACTION_ANNOTATION_TRANSACTIONAL;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class JpaServiceGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaServiceGenerator.class);

    private static final String SERVICES = "services";
    private static final String REPOSITORIES_PACKAGE = ".repositories";
    private static final String MODELS_PACKAGE = ".models";
    private static final String SERVICES_PACKAGE = "." + SERVICES;
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generating JPA service for model: {}", modelDefinition.getName());
        
        final boolean hasIdField = FieldUtils.isAnyFieldId(modelDefinition.getFields());
        
        if (!hasIdField) {
            LOGGER.warn("Model {} does not have an ID field. Skipping service generation.", modelDefinition.getName());
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sService", modelWithoutSuffix);
        final String enumsImport = ImportUtils.computeEnumsImport(modelDefinition, outputDir);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + SERVICES_PACKAGE));
        sb.append(ImportUtils.getBaseImport(modelDefinition, false));
        
        sb.append(String.format(IMPORT, SL4J_LOGGER))
                .append(String.format(IMPORT, SL4J_LOGGER_FACTORY))
                .append(String.format(IMPORT, SPRING_DATA_PACKAGE_DOMAIN_PAGE))
                .append(String.format(IMPORT, SPRING_DATA_PACKAGE_DOMAIN_PAGE_REQUEST))
                .append(String.format(IMPORT, SPRING_FRAMEWORK_STEREOTYPE_SERVICE))
                .append(String.format(IMPORT, SPRING_FRAMEWORK_TRANSACTION_ANNOTATION_TRANSACTIONAL))
                .append("\n")
                .append(enumsImport)
                .append(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()))
                .append(String.format(IMPORT, packagePath + REPOSITORIES_PACKAGE + "." + modelWithoutSuffix + "Repository"))
                .append("\n");

        sb.append(generateServiceClass(modelDefinition));

        FileWriterUtils.writeToFile(outputDir, SERVICES, className, sb.toString());
    }

    /**
     * Generates the service class for the given model definition.
     * 
     * The generated class extends the service interface and contains the
     * following methods:
     * <ul>
     *     <li>getById: retrieves a model instance by its ID</li>
     *     <li>getAll: retrieves all model instances from the database</li>
     *     <li>create: creates a new model instance</li>
     *     <li>update: updates an existing model instance</li>
     *     <li>delete: deletes a model instance</li>
     * </ul>
     * 
     * @param modelDefinition the model definition containing the class name and field definitions
     * @return a string representation of the service class
     */
    private String generateServiceClass(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.createServiceClassContext(modelDefinition);
        context.put("getByIdMethod", generateGetByIdMethod(modelDefinition));
        context.put("getAllMethod", generateGetAllMethod(modelDefinition));
        context.put("createMethod", generateCreateMethod(modelDefinition));
        context.put("updateMethod", generateUpdateByIdMethod(modelDefinition));
        context.put("deleteMethod", generateDeleteByIdMethod(modelDefinition));

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/service-class-template.ftl", context);
    }

    /**
     * Generates the getAll method as a string for the given model.
     * 
     * @param modelDefinition The model definition for which the getAll method 
     *                        is to be generated.
     * @return A string representation of the getAll method.
     */
    private String generateGetAllMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeGetAllContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/get-all.ftl", context);
    }

    /**
     * Generates the create method as a string for the given model definition.
     * 
     * @param modelDefinition The model definition for which the create method
     *                        is to be generated.
     * @return A string representation of the create method.
     */
    public String generateCreateMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeCreateContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/create.ftl", context);
    }

    /**
     * Generates the updateById method as a string for the given model definition.
     *
     * @param modelDefinition The model definition for which the updateById method
     *                        is to be generated.
     * @return A string representation of the updateById method.
     */
    public String generateUpdateByIdMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeUpdateByIdContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/update-by-id.ftl", context);
    }

    /**
     * Generates the deleteById method as a string for the given model.
     * 
     * @param modelDefinition The model definition for which the deleteById
     *                        method is to be generated.
     * @return A string representation of the deleteById method.
     */
    private String generateDeleteByIdMethod(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeDeleteByIdContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/delete-by-id.ftl", context);
    }

    /**
     * Generates the getById method as a string for the given model.
     * 
     * @param modelDefinition The model definition for which the getById
     *                        method is to be generated.
     * @return A string representation of the getById method.
     */
    public String generateGetByIdMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.computeGetByIdContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/get-by-id.ftl", context);
    }

}
