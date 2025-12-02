package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markozivkovic.codegen.constants.AdditionalConfigurationConstants;
import com.markozivkovic.codegen.models.CrudConfiguration;

class AdditionalPropertiesUtilsTest {

    private CrudConfiguration configurationWith(final Map<String, Object> additionalProperties) {
        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setAdditionalProperties(additionalProperties);
        return configuration;
    }

    @Test
    @DisplayName("Return /api when additionalProperties is null")
    void resolveBasePath_shouldReturnDefault_whenAdditionalPropertiesIsNull() {
        
        final CrudConfiguration configuration = this.configurationWith(null);

        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);
        
        assertEquals("/api", basePath);
    }

    @Test
    @DisplayName("Return /api when 'rest.basePath' key is missing")
    void resolveBasePath_shouldReturnDefault_whenKeyMissing() {
        
        final CrudConfiguration configuration = this.configurationWith(Map.of());

        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);
        
        assertEquals("/api", basePath);
    }

    @Test
    @DisplayName("Throw IllegalArgumentException when value is not String")
    void resolveBasePath_shouldThrow_whenValueIsNotString() {

        final CrudConfiguration configuration = this.configurationWith(
                Map.of(AdditionalConfigurationConstants.REST_BASEPATH, 123)
        );

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AdditionalPropertiesUtils.resolveBasePath(configuration)
        );

        assertTrue(ex.getMessage().contains("Invalid type for 'rest.basePath'."));
    }

    @Test
    @DisplayName("Return /api when value is blank")
    void resolveBasePath_shouldReturnDefault_whenValueIsBlank() {
        
        final CrudConfiguration configuration = this.configurationWith(
                Map.of(AdditionalConfigurationConstants.REST_BASEPATH, "")
        );

        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);
        
        assertEquals("/api", basePath);
    }

    @Test
    @DisplayName("Return value when value is String")
    void resolveBasePath_shouldReturn_whenValueIsString() {
        
        final CrudConfiguration configuration = this.configurationWith(
                Map.of(AdditionalConfigurationConstants.REST_BASEPATH, "/test")
        );

        final String basePath = AdditionalPropertiesUtils.resolveBasePath(configuration);
        
        assertEquals("/test", basePath);
    }
    
}
