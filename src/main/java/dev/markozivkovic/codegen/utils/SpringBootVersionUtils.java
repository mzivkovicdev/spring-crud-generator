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

package dev.markozivkovic.codegen.utils;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudSpecification;

public class SpringBootVersionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootVersionUtils.class);
    private static final Integer VERSION_3 = 3;
    private static final Integer VERSION_4 = 4;
    
    private SpringBootVersionUtils() {}

    /**
     * Attempts to parse the major version from the given version string.
     * 
     * @param version the version string to parse
     * @return the parsed major version, or null if the version string is blank or cannot be parsed
     */
    private static Integer tryParseMajorVersion(final String version) {
        
        if (StringUtils.isBlank(version)) return null;

        final String trimmedVersion = version.trim();
        final int dot = trimmedVersion.indexOf('.');
        final String majorVersion = dot >= 0 ? trimmedVersion.substring(0, dot) : trimmedVersion;

        try {
            return Integer.parseInt(majorVersion);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if the given major version is supported by the code generator.
     * 
     * @param major the major version to check
     * @return true if the major version is supported, false otherwise
     */
    private static boolean isSupportedSpringBootMajor(final Integer major) {
        return Objects.equals(major, VERSION_3) || Objects.equals(major, VERSION_4);
    }

    /**
     * Resolves the Spring Boot major version from the given spec and parent version, and sets it in the spec configuration.
     * 
     * If the spec configuration provides a valid Spring Boot major version, it is used.
     * If not, and the parent version is a valid Spring Boot major version, it is used.
     * If neither spec nor parent version is valid, the default Spring Boot major version is used (4).
     * 
     * @param spec the spec containing the configuration to set the Spring Boot major version
     * @param parentVersion the parent version to check for a valid Spring Boot major version
     * @return the resolved Spring Boot major version
     */
    public static Integer resolveAndSetSpringBootMajor(final CrudSpecification spec, final String parentVersion) {
        
        final CrudConfiguration configuration = spec.getConfiguration();
        final Integer defaultMajor = VERSION_4;

        final String specVersion = configuration.getSpringBootVersion();
        final Integer specMajorVersion = tryParseMajorVersion(specVersion);
        if (isSupportedSpringBootMajor(specMajorVersion)) {
            configuration.setSpringBootVersion(String.valueOf(specMajorVersion));
            LOGGER.info("Using Spring Boot {} (source: spec.springBootVersion='{}')", specMajorVersion, specVersion);
            return specMajorVersion;
        } else if (StringUtils.isNotBlank(specVersion)) {
            LOGGER.warn("Ignoring invalid/unsupported spec.springBootVersion='{}'. Supported majors: 3, 4.", specVersion);
        }

        final Integer parentMajor = tryParseMajorVersion(parentVersion);
        if (isSupportedSpringBootMajor(parentMajor)) {
            configuration.setSpringBootVersion(String.valueOf(parentMajor));
            LOGGER.info("Using Spring Boot {} (source: project.parent.version='{}')", parentMajor, parentVersion);
            return parentMajor;
        } else if (StringUtils.isNotBlank(parentVersion)) {
            LOGGER.warn("project.parent.version='{}' does not look like Spring Boot 3/4. Ignoring it.", parentVersion);
        }

        configuration.setSpringBootVersion(String.valueOf(defaultMajor));
        LOGGER.warn("Spring Boot version not provided or not recognized. Defaulting to Spring Boot {}. " +
                    "To override, set configuration.springBootVersion in the spec (e.g. '3.0.2' or '4.0.1').",
                    defaultMajor);

        return defaultMajor;
    }

    /**
     * Checks if the given configuration is for Spring Boot 3.
     *
     * @param springBootVersion the Spring Boot version to check
     * @return true if the configuration is for Spring Boot 3, false otherwise
     */
    public static boolean isSpringBoot3(final String springBootVersion) {
        return Objects.nonNull(springBootVersion) && Objects.equals("3", springBootVersion);
    }

    /**
     * Checks if the given configuration is for Spring Boot 4.
     *
     * @param springBootVersion the Spring Boot version to check
     * @return true if the configuration is for Spring Boot 4, false otherwise
     */
    public static boolean isSpringBoot4(final String springBootVersion) {
        return Objects.nonNull(springBootVersion) && Objects.equals("4", springBootVersion);
    }

}
