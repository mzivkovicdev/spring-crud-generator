package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
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
    private static final String SERVICES_PACKAGE = "." + SERVICES;

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entites;

    public JpaServiceGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entites) {
        this.configuration = configuration;
        this.entites = entites;
    }
    
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

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + SERVICES_PACKAGE));
        sb.append(ImportUtils.getBaseImport(
                modelDefinition, false, FieldUtils.hasCollectionRelation(modelDefinition, entites), false)
        );
        
        sb.append(ImportUtils.computeJpaServiceBaseImport(
                    Objects.nonNull(configuration) && Objects.nonNull(configuration.isCache()) && configuration.isCache())
                )
                .append("\n")
                .append(ImportUtils.computeModelsEnumsAndRepositoryImports(modelDefinition, outputDir))
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
     *     <li>addRelation: adds a relation to a model instance</li>
     *     <li>removeRelation: removes a relation from a model instance</li>
     *     <li>getAllByIds: retrieves all model instances by their IDs</li>
     *     <li>getReferenceById: retrieves a reference to a model instance by its ID</li>
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
        context.put("addRelationMethod", addRelationMethod(modelDefinition));
        context.put("removeRelationMethod", removeRelationMethod(modelDefinition));
        context.put("getAllByIds", getAllByIdsMethod(modelDefinition));
        context.put("getReferenceById", getReferenceByIdMethod(modelDefinition));

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/service-class-template.ftl", context);
    }

    /**
     * Generates the getAllByIds method as a string for the given model definition.
     * 
     * @param modelDefinition The model definition for which the getAllByIds method
     *                        is to be generated.
     * @return A string representation of the getAllByIds method, or null if the context
     *         is empty.
     */
    private String getAllByIdsMethod(final ModelDefinition modelDefinition) {
        
        if (!FieldUtils.hasCollectionRelation(modelDefinition, entites)) {
            return null;
        }
        final Map<String, Object> context = TemplateContextUtils.createGetAllByIdsMethodContext(modelDefinition);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/get-all-by-ids.ftl", context);
    }

    /**
     * Generates the getReferenceById method as a string for the given model definition.
     * 
     * @param modelDefinition The model definition for which the getReferenceById method
     *                        is to be generated.
     * @return A string representation of the getReferenceById method, or null if the context
     *         is empty.
     */
    private String getReferenceByIdMethod(ModelDefinition modelDefinition) {

        if (!FieldUtils.hasRelation(modelDefinition, entites)) {
            return null;
        }

        final Map<String, Object> context = TemplateContextUtils.createGetReferenceByIdMethodContext(modelDefinition);
        
        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/get-reference-by-id.ftl", context);
    }

    /**
     * Generates the removeRelation method as a string for the given model definition.
     * This method is responsible for removing a relation from a model entity and 
     * throws an exception if the removal is not possible.
     * 
     * @param modelDefinition The model definition for which the removeRelation method
     *                        is to be generated.
     * @return A string representation of the removeRelation method, or null 
     *         if the context is empty.
     */
    private String removeRelationMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.createRemoveRelationMethodContext(modelDefinition);
        if (context.isEmpty()) {
            return null;
        }

        this.putCacheFlagToContext(context);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/remove-relation.ftl", context);
    }

    /**
     * Generates the addRelation method as a string for the given model definition.
     * This method is responsible for adding a new relation to a model entity and 
     * throws an exception if the addition is not possible.
     * 
     * @param modelDefinition The model definition for which the addRelation method
     *                        is to be generated.
     * @return A string representation of the addRelation method, or an empty string 
     *         if the context is empty.
     */
    private String addRelationMethod(final ModelDefinition modelDefinition) {
        
        final Map<String, Object> context = TemplateContextUtils.createAddRelationMethodContext(modelDefinition);
        if (context.isEmpty()) {
            return null;
        }

        this.putCacheFlagToContext(context);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/add-relation.ftl", context);
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
        this.putCacheFlagToContext(context);

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
        this.putCacheFlagToContext(context);

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
        this.putCacheFlagToContext(context);

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
        this.putCacheFlagToContext(context);

        return FreeMarkerTemplateProcessorUtils.processTemplate("service/method/get-by-id.ftl", context);
    }

    /**
     * Adds a flag to the context indicating whether the cache is enabled or not.
     * 
     * @param context the context to which the cache flag is to be added
     */
    private void putCacheFlagToContext(final Map<String, Object> context) {
        if (Objects.nonNull(configuration) && Objects.nonNull(configuration.isCache())) {
            context.put("cache", configuration.isCache());
        }
    }

}
