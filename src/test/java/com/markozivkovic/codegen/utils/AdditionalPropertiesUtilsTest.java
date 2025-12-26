package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    @DisplayName("hasAnyRetryableConfigOverride: should return false when map is null")
    void hasAnyRetryableConfigOverride_nullMap_returnsFalse() {
        assertFalse(AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(null));
    }

    @Test
    @DisplayName("hasAnyRetryableConfigOverride: should return false when map is empty")
    void hasAnyRetryableConfigOverride_emptyMap_returnsFalse() {
        assertFalse(AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(Map.of()));
    }

    @Test
    @DisplayName("hasAnyRetryableConfigOverride: should return false when map does not contain retry keys")
    void hasAnyRetryableConfigOverride_noRetryKeys_returnsFalse() {
        
        final Map<String, Object> props = Map.of(
                "some.other.property", 123,
                "graphql.scalarConfig", true
        );

        assertFalse(AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(props));
    }

    @Test
    @DisplayName("hasAnyRetryableConfigOverride: should return true when maxAttempts is present")
    void hasAnyRetryableConfigOverride_maxAttemptsPresent_returnsTrue() {
        
        final Map<String, Object> props = Map.of(
                AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS, 5
        );

        assertTrue(AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(props));
    }

    @Test
    @DisplayName("hasAnyRetryableConfigOverride: should return true when any backoff property is present")
    void hasAnyRetryableConfigOverride_anyBackoffKeyPresent_returnsTrue() {
        
        final Map<String, Object> props = Map.of(
                AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS, 200
        );

        assertTrue(AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(props));
    }

    @Test
    @DisplayName("hasAnyRetryableConfigOverride: should return true when multiple retry properties are present")
    void hasAnyRetryableConfigOverride_multipleRetryKeys_returnsTrue() {
        
        final Map<String, Object> props = Map.of(
                AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS, 5,
                AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS, 100
        );

        assertTrue(AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(props));
    }

    @Test
    @DisplayName("getInt: should return default value when key is missing but has default mapping")
    void getInt_returnsDefaultWhenKeyMissing() {
        
        final Map<String, Object> props = Map.of();

        final Integer result = AdditionalPropertiesUtils.getInt(props, AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS);

        assertEquals(3, result);
    }

    @Test
    @DisplayName("getInt: should convert numeric value to int when present")
    void getInt_returnsNumericValueWhenPresent() {
        
        final Map<String, Object> props = Map.of(
                AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS, 10
        );

        final Integer result = AdditionalPropertiesUtils.getInt(props, AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS);

        assertEquals(10, result);
    }

    @Test
    @DisplayName("getInt: should throw IllegalArgumentException when value is not a number")
    void getInt_throwsWhenValueNotNumber() {
        
        final Map<String, Object> props = Map.of(
                AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS, "not-a-number"
        );

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AdditionalPropertiesUtils.getInt(props, AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS)
        );

        assertTrue(ex.getMessage().contains("Property " + AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS));
        assertTrue(ex.getMessage().contains("must be a number"));
    }

    @Test
    @DisplayName("getDouble: should return default value when key is missing but has default mapping")
    void getDouble_returnsDefaultWhenKeyMissing() {
        
        final Map<String, Object> props = Map.of();

        final Double result = AdditionalPropertiesUtils.getDouble(props, AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER);

        assertEquals(0.0d, result);
    }

    @Test
    @DisplayName("getDouble: should convert numeric value to double when present")
    void getDouble_returnsNumericValueWhenPresent() {
        
        final Map<String, Object> props = Map.of(
                AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER, 2 
        );

        final Double result = AdditionalPropertiesUtils.getDouble(props, AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER);

        assertEquals(2.0d, result);
    }

    @Test
    @DisplayName("getDouble: should throw IllegalArgumentException when value is not a number")
    void getDouble_throwsWhenValueNotNumber() {
        
        final Map<String, Object> props = Map.of(
                AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER, "NaN"
        );

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AdditionalPropertiesUtils.getDouble(props, AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER)
        );

        assertTrue(ex.getMessage().contains("Property " + AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER));
        assertTrue(ex.getMessage().contains("must be a number"));
    }
    
}
