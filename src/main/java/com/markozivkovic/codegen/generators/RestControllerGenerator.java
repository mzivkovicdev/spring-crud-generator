package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class RestControllerGenerator implements CodeGenerator {
    
    private static final String CONTROLLERS = "controllers";
    private static final String CONTROLLERS_PACKAGE = "." + CONTROLLERS;

    private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerGenerator.class);

    private final List<ModelDefinition> entites;

    public RestControllerGenerator(final List<ModelDefinition> entites) {
        this.entites = entites;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generating REST controller for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sController", modelWithoutSuffix);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + CONTROLLERS_PACKAGE));
        sb.append(ImportUtils.computeControllerBaseImports(modelDefinition, entites))
            .append("\n")
            .append(generateControllerClass(modelDefinition, outputDir));

        FileWriterUtils.writeToFile(outputDir, CONTROLLERS, className, sb.toString());
    }

    /**
     * Generates the controller class for the given model definition.
     * 
     * The generated class contains the following methods:
     * <ul>
     *     <li>createResource: creates a new resource</li>
     *     <li>getResource: retrieves a resource by its ID</li>
     *     <li>getAllResources: retrieves all resources</li>
     *     <li>updateResource: updates an existing resource</li>
     *     <li>deleteResource: deletes a resource</li>
     * </ul>
     * 
     * @param modelDefinition The model definition for which the controller class is to be generated.
     * @param outputDir The output directory where the generated class is to be written.
     * @return A string representation of the controller class.
     */
    private String generateControllerClass(final ModelDefinition modelDefinition, final String outputDir) {

        final Map<String, Object> context = TemplateContextUtils.computeControllerClassContext(modelDefinition);
        context.put("projectImports", ImportUtils.computeControllerProjectImports(modelDefinition, outputDir));

        context.put("createResource", generateCreateResourceEndpoint(modelDefinition));
        context.put("getResource", generateGetResourceEndpoint(modelDefinition));
        context.put("getAllResources", generateGetAllResourcesEndpoint(modelDefinition));
        context.put("updateResource", generateUpdateResourceEndpoint(modelDefinition));
        context.put("deleteResource", generateDeleteResourceEndpoint(modelDefinition));

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/controller-template.ftl", context);
    }

    /**
     * Generates the REST endpoint for creating a new resource.
     * 
     * @param modelDefinition The model definition for which the create resource 
     *                        endpoint is to be generated.
     * @return A string representation of the create resource endpoint method.
     */
    private String generateCreateResourceEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeCreateEndpointContext(modelDefinition, entites);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/create-resource.ftl", context);
    }

    /**
     * Generates the REST endpoint for retrieving a resource by its ID.
     * 
     * @param modelDefinition The model definition for which the get resource 
     *                        endpoint is to be generated.
     * @return A string representation of the get resource endpoint method.
     */
    private String generateGetResourceEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeGetByIdEndpointContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/get-resource.ftl", context);
    }

    private String generateGetAllResourcesEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeGetAllEndpointContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/get-all-resources.ftl", context);
    }

    /**
     * Generates the REST endpoint for updating a resource.
     * 
     * @param modelDefinition The model definition for which the update resource 
     *                        endpoint is to be generated.
     * @return A string representation of the update resource endpoint method.
     */
    private String generateUpdateResourceEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeUpdateEndpointContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/update-resource.ftl", context);
    }

    /**
     * Generates the REST endpoint for deleting a resource by its ID.
     * 
     * @param modelDefinition The model definition for which the delete resource 
     *                        endpoint is to be generated.
     * @return A string representation of the delete resource endpoint method.
     */
    private String generateDeleteResourceEndpoint(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeDeleteEndpointContext(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/delete-resource.ftl", context);
    }

}
