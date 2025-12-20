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

package com.markozivkovic.codegen.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.StringUtils;

public class PackageConfigurationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageConfigurationValidator.class);

    private PackageConfigurationValidator() {}

     /**
     * Validates the package configuration.
     * If the package configuration is not defined, it uses the default package structure.
     * If any of the package groups are defined, it checks if all required packages are defined.
     * If any of the required packages are missing, it throws an {@link IllegalArgumentException}.
     * If the graphQl configuration is enabled, it checks if the resolvers package is defined.
     * If the openApiCodegen configuration is enabled, it checks if the generated package is defined.
     * 
     * @param packageConfiguration the package configuration
     * @param configuration the crud configuration
     */
    public static void validate(final PackageConfiguration packageConfiguration, final CrudConfiguration configuration) {

        if (Objects.isNull(packageConfiguration)) {
            LOGGER.info("Package configuration is not defined, using default package structure");
            return;
        }

        final List<String> missing = new ArrayList<>();
        final boolean anyGroupDefined = StringUtils.isNotBlank(packageConfiguration.getBusinessservices()) ||
                StringUtils.isNotBlank(packageConfiguration.getControllers()) ||
                StringUtils.isNotBlank(packageConfiguration.getEnums()) ||
                StringUtils.isNotBlank(packageConfiguration.getExceptions()) ||
                StringUtils.isNotBlank(packageConfiguration.getMappers()) ||
                StringUtils.isNotBlank(packageConfiguration.getModels()) ||
                StringUtils.isNotBlank(packageConfiguration.getRepositories()) ||
                StringUtils.isNotBlank(packageConfiguration.getServices()) ||
                StringUtils.isNotBlank(packageConfiguration.getTransferobjects());
    
        if (anyGroupDefined) {
            if (StringUtils.isBlank(packageConfiguration.getBusinessservices())) missing.add("businessservices");
            if (StringUtils.isBlank(packageConfiguration.getControllers())) missing.add("controllers");
            if (StringUtils.isBlank(packageConfiguration.getEnums())) missing.add("enums");
            if (StringUtils.isBlank(packageConfiguration.getExceptions())) missing.add("exceptions");
            if (StringUtils.isBlank(packageConfiguration.getMappers())) missing.add("mappers");
            if (StringUtils.isBlank(packageConfiguration.getModels())) missing.add("models");
            if (StringUtils.isBlank(packageConfiguration.getRepositories())) missing.add("repositories");
            if (StringUtils.isBlank(packageConfiguration.getServices())) missing.add("services");
            if (StringUtils.isBlank(packageConfiguration.getTransferobjects())) missing.add("transferobjects");
        }

        if (Boolean.TRUE.equals(configuration.getGraphQl())) {
            if (StringUtils.isBlank(packageConfiguration.getResolvers())) {
                missing.add("resolvers (required when graphQl is enabled)");
            }
        }

        if (Boolean.TRUE.equals(configuration.getOpenApiCodegen())) {
            if (StringUtils.isBlank(packageConfiguration.getGenerated())) {
                missing.add("generated (required when openApiCodegen is enabled)");
            }
        }

        if (!missing.isEmpty()) {
            final String message = String.format(
                    "Invalid package configuration. Missing required package(s): %s", String.join(", ", missing)
            );
            throw new IllegalArgumentException(message);
        }
    }
    
}
