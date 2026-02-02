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

package dev.markozivkovic.springcrudgenerator.generators;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.RestControllerImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.RestControllerTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

public class RestControllerGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public RestControllerGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
                final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            LOGGER.warn("Model {} does not have an ID field. Skipping REST controller generation.", modelDefinition.getName());
            return;
        }
        
        LOGGER.info("Generating REST controller for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String className = String.format("%sController", modelWithoutSuffix);
        final Boolean swagger = Objects.nonNull(this.configuration.getOpenApi())
                && Boolean.TRUE.equals(this.configuration.getOpenApi().getApiSpec())
                && Boolean.TRUE.equals(this.configuration.getOpenApi().getGenerateResources());

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)));
        sb.append(RestControllerImports.computeControllerBaseImports(modelDefinition, entities))
            .append(System.lineSeparator())
            .append(generateControllerClass(modelDefinition, outputDir, swagger));

        FileWriterUtils.writeToFile(outputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), className, sb.toString());
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
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @throws IllegalArgumentException If base path is null or not defined to be a {@link String}.
     * @return A string representation of the controller class.
     */
    private String generateControllerClass(final ModelDefinition modelDefinition, final String outputDir, final boolean swagger) {

        final Map<String, Object> context = RestControllerTemplateContext.computeControllerClassContext(modelDefinition);
        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);

        context.put("basePath", basePath);
        context.put("projectImports", RestControllerImports.computeControllerProjectImports(modelDefinition, outputDir, swagger, packageConfiguration));
        context.put("createResource", generateCreateResourceEndpoint(modelDefinition, swagger));
        context.put("getResource", generateGetResourceEndpoint(modelDefinition, swagger));
        context.put("getAllResources", generateGetAllResourcesEndpoint(modelDefinition, swagger));
        context.put("updateResource", generateUpdateResourceEndpoint(modelDefinition, swagger));
        context.put("deleteResource", generateDeleteResourceEndpoint(modelDefinition, swagger));
        context.put("addResourceRelation", generateAddResourceRelationEndpoint(modelDefinition, swagger));
        context.put("removeResourceRelation", generateRemoveResourceRelationEndpoint(modelDefinition, swagger));
        context.put(TemplateContextConstants.SWAGGER, swagger);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/controller-template.ftl", context);
    }

    /**
     * Generates the REST endpoint for creating a new resource.
     * 
     * @param modelDefinition The model definition for which the create resource 
     *                        endpoint is to be generated.
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @return A string representation of the create resource endpoint method.
     */
    private String generateCreateResourceEndpoint(final ModelDefinition modelDefinition, final boolean swagger) {

        final Map<String, Object> context = RestControllerTemplateContext.computeCreateEndpointContext(modelDefinition, entities);
        context.put("swagger", swagger);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/create-resource.ftl", context);
    }

    /**
     * Generates the REST endpoint for retrieving a resource by its ID.
     * 
     * @param modelDefinition The model definition for which the get resource 
     *                        endpoint is to be generated.
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @return A string representation of the get resource endpoint method.
     */
    private String generateGetResourceEndpoint(final ModelDefinition modelDefinition, final boolean swagger) {

        final Map<String, Object> context = RestControllerTemplateContext.computeGetByIdEndpointContext(modelDefinition);
        context.put("swagger", swagger);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/get-resource.ftl", context);
    }

    /**
     * Generates the REST endpoint for retrieving all resources.
     * 
     * @param modelDefinition The model definition for which the get all resources 
     *                        endpoint is to be generated.
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @return A string representation of the get all resources endpoint method.
     */
    private String generateGetAllResourcesEndpoint(final ModelDefinition modelDefinition, final boolean swagger) {

        final Map<String, Object> context = RestControllerTemplateContext.computeGetAllEndpointContext(modelDefinition);
        context.put("swagger", swagger);
        context.put(
            TemplateContextConstants.OPEN_IN_VIEW_ENABLED, AdditionalPropertiesUtils.isOpenInViewEnabled(this.configuration.getAdditionalProperties())
        );

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/get-all-resources.ftl", context);
    }

    /**
     * Generates the REST endpoint for updating a resource.
     * 
     * @param modelDefinition The model definition for which the update resource 
     *                        endpoint is to be generated.
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @return A string representation of the update resource endpoint method.
     */
    private String generateUpdateResourceEndpoint(final ModelDefinition modelDefinition, final boolean swagger) {

        final Map<String, Object> context = RestControllerTemplateContext.computeUpdateEndpointContext(modelDefinition, swagger);
        context.put("swagger", swagger);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/update-resource.ftl", context);
    }

    /**
     * Generates the REST endpoint for deleting a resource by its ID.
     * 
     * @param modelDefinition The model definition for which the delete resource 
     *                        endpoint is to be generated.
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @return A string representation of the delete resource endpoint method.
     */
    private String generateDeleteResourceEndpoint(final ModelDefinition modelDefinition, final boolean swagger) {

        final Map<String, Object> context = RestControllerTemplateContext.computeDeleteEndpointContext(modelDefinition);
        context.put("swagger", swagger);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/delete-resource.ftl", context);
    }

    /**
     * Generates the REST endpoint for adding a relation to a resource.
     * 
     * @param modelDefinition The model definition for which the add resource 
     *                        relation endpoint is to be generated.
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @return A string representation of the add resource relation endpoint method.
     */
    private String generateAddResourceRelationEndpoint(final ModelDefinition modelDefinition, final boolean swagger) {
        
        final Map<String, Object> context = RestControllerTemplateContext.computeAddResourceRelationEndpointContext(modelDefinition, entities);
        
        if (context.isEmpty()) {
            return null;
        }

        context.put("swagger", swagger);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/add-resource-relation.ftl", context);
    }

    /**
     * Generates the REST endpoint for removing a relation from a resource.
     * 
     * @param modelDefinition The model definition for which the remove resource 
     *                        relation endpoint is to be generated.
     * @param swagger Indicates whether Swagger generatror is enabled.
     * @return A string representation of the remove resource relation endpoint method,
     *         or null if the context is empty.
     */
    private String generateRemoveResourceRelationEndpoint(final ModelDefinition modelDefinition, final boolean swagger) {
        
        final Map<String, Object> context = RestControllerTemplateContext.computeRemoveResourceRelationEndpointContext(modelDefinition, entities);
        
        if (context.isEmpty()) {
            return null;
        }

        context.put("swagger", swagger);

        return FreeMarkerTemplateProcessorUtils.processTemplate("controller/endpoint/remove-resource-relation.ftl", context);
    }

}
