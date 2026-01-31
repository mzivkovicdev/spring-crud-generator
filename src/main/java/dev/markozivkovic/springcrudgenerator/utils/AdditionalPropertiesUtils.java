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
import java.util.Optional;

import dev.markozivkovic.springcrudgenerator.constants.AdditionalConfigurationConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;

public class AdditionalPropertiesUtils {

    private static final Integer DEFAULT_MAX_ATTEMPTS = 3;
    private static final Integer DEFAULT_DELAY_MS = 1000;
    private static final Integer DEFAULT_MAX_DELAY_MS = 0;
    private static final Double DEFAULT_MULTIPLIER = 0.0d;

    private static final Map<String, Object> DEFAULT_RETRYABLE_CONFIG = Map.of(
            AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS,
            AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS, DEFAULT_DELAY_MS,
            AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MAX_DELAY_MS, DEFAULT_MAX_DELAY_MS,
            AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER, DEFAULT_MULTIPLIER
    );

    private AdditionalPropertiesUtils() {}

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

    /**
     * Returns true if the given map contains any retryable configuration overrides.
     * These overrides are the following additional properties:
     * <ul>
     *     <li>{@link AdditionalConfigurationConstants#OPT_LOCK_MAX_ATTEMPTS}</li>
     *     <li>{@link AdditionalConfigurationConstants#OPT_LOCK_BACKOFF_DELAY_MS}</li>
     *     <li>{@link AdditionalConfigurationConstants#OPT_LOCK_BACKOFF_MAX_DELAY_MS}</li>
     *     <li>{@link AdditionalConfigurationConstants#OPT_LOCK_BACKOFF_MULTIPLIER}</li>
     * </ul>
     * If the map is empty, or none of the above properties are present, false is returned.
     * 
     * @param additionalProperties the map to check for retryable configuration overrides
     * @return true if the map contains any retryable configuration overrides, false otherwise
     */
    public static boolean hasAnyRetryableConfigOverride(final Map<String, Object>  additionalProperties) {

        if (ContainerUtils.isEmpty(additionalProperties)) {
            return false;
        }

        return additionalProperties.containsKey(AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS)
                || additionalProperties.containsKey(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS)
                || additionalProperties.containsKey(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MAX_DELAY_MS)
                || additionalProperties.containsKey(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER);
    }

    /**
     * Retrieves an integer value from the given map of additional properties.
     * If the given key is not present in the map, and the default retryable configuration
     * contains a mapping for the given key, the default value is returned.
     * If the value associated with the given key is not a number, an
     * {@link IllegalArgumentException} is thrown.
     * 
     * @param additionalProperties the map of additional properties to retrieve the value from
     * @param key the key to retrieve the value for
     * @return the integer value associated with the given key
     * @throws IllegalArgumentException if the value associated with the given key is not a number
     */
    public static Integer getInt(final Map<String, Object> additionalProperties, final String key) {
        
        final Object value = additionalProperties.get(key);

        if (Objects.isNull(value) && DEFAULT_RETRYABLE_CONFIG.containsKey(key)) {
            return (Integer) DEFAULT_RETRYABLE_CONFIG.get(key);
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        throw new IllegalArgumentException(String.format(
                "Property %s must be a number, but was: %s", key, value
        ));
    }

    /**
     * Retrieves a double value from the given map of additional properties.
     * If the given key is not present in the map, and the default retryable configuration
     * contains a mapping for the given key, the default value is returned.
     * If the value associated with the given key is not a number, an
     * {@link IllegalArgumentException} is thrown.
     * 
     * @param additionalProperties the map of additional properties to retrieve the value from
     * @param key the key to retrieve the value for
     * @return the double value associated with the given key
     * @throws IllegalArgumentException if the value associated with the given key is not a number
     */
    public static Double getDouble(final Map<String, Object> additionalProperties, final String key) {
        
        final Object value = additionalProperties.get(key);

        if (Objects.isNull(value) && DEFAULT_RETRYABLE_CONFIG.containsKey(key)) {
            return (Double) DEFAULT_RETRYABLE_CONFIG.get(key);
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        throw new IllegalArgumentException(String.format(
                "Property %s must be a number, but was: %s", key, value
        ));
    }

    /**
     * Checks if the given map of additional properties contains the 'spring.jpa.open-in-view' key and its value is true.
     * 
     * @param additionalProperties the map of additional properties to check
     * @return true if the 'spring.jpa.open-in-view' key is present and its value is true, false otherwise
     */
    public static boolean isOpenInViewEnabled(final Map<String, Object> additionalProperties) {
        
        return Boolean.TRUE.equals(
                Optional.ofNullable(additionalProperties)
                        .map(properties -> properties.get(AdditionalConfigurationConstants.JPA_OPEN_IN_VIEW))
                        .orElse(false)
        );
    }
    
}
