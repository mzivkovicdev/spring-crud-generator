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

package dev.markozivkovic.springcrudgenerator.utils;

import java.util.Map;
import java.util.Objects;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public final class CrudMojoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrudMojoUtils.class);

    private CrudMojoUtils() {}

    /**
     * Prints a banner to the console with information about the plugin version, input CRUD spec file and output directory.
     *
     * @param logger the logger to print banner with
     * @param pluginDescriptor maven plugin descriptor
     * @param inputSpecFile the spec file location
     * @param outputDir output directory for generate goal (or informational value for other goals)
     */
    public static void printBanner(final PluginDescriptor pluginDescriptor, final String inputSpecFile, final String outputDir) {

        final String version = Objects.nonNull(pluginDescriptor) ? pluginDescriptor.getVersion() : "dev";
        
        final Map<String, Object> context = Map.of(
                "version", version,
                "specPath", inputSpecFile,
                "outputDir", outputDir
        );
        
        LOGGER.info(FreeMarkerTemplateProcessorUtils.processTemplate("banner.ftl", context));
    }

    /**
     * Creates an {@link ObjectMapper} based on the file extension of the input spec file.
     * Supported file formats are: .yaml, .yml, .json
     *
     * @param inputSpecFile the input spec file
     * @return an {@link ObjectMapper} based on the file extension of the input spec file
     * @throws IllegalArgumentException if the file format is not supported
     */
    public static ObjectMapper createSpecMapper(final String inputSpecFile) {

        final String specFile = inputSpecFile.toLowerCase().trim();

        if (specFile.endsWith(".yaml") || specFile.endsWith(".yml")) {
            return YAMLMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                    .build();
        } else if (specFile.endsWith(".json")) {
            return JsonMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                    .build();
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported file format: %s. Supported file formats are: .yaml, .yml, .json",
                    specFile
            ));
        }
    }
}
