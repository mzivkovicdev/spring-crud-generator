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

package com.markozivkovic.codegen.utils;

import java.util.Objects;
import java.util.Optional;

import com.markozivkovic.codegen.constants.AdditionalConfigurationConstants;
import com.markozivkovic.codegen.models.CrudConfiguration;

public class AdditionalPropertiesUtils {

    /**
     * Resolve the base path from the given configuration.
     * If the configuration does not contain the 'rest.basePath' additional property,
     * or if the value of that property is not a String, an {@link IllegalArgumentException} is thrown.
     * If the value is empty, the default value '/api' is returned.
     *
     * @param configuration the configuration to resolve the base path from
     * @return the resolved base path
     */
    public static String resolveBasePath(final CrudConfiguration configuration) {

        return Optional.ofNullable(configuration.getAdditionalProperties())
                .map(properties -> properties.get(AdditionalConfigurationConstants.REST_BASEPATH))
                .map(value -> {
                    if (value instanceof String) {
                        return (String) value;
                    }
                    throw new IllegalArgumentException(
                        String.format(
                            "Invalid type for 'rest.basePath'. Expected String, but got: %s",
                            Objects.isNull(value) ? "null" : value.getClass().getSimpleName()
                        )
                    );
                })
                .filter(StringUtils::isNotBlank)
                .orElse("/api");
    }
    
}
