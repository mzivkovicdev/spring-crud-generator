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
