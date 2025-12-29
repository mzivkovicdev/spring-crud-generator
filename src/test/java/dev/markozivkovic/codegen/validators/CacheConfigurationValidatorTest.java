package dev.markozivkovic.codegen.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;

class CacheConfigurationValidatorTest {

    @Test
    @DisplayName("Should do nothing when configuration is null")
    void validate_nullConfiguration_doesNothing() {
        
        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(null));
    }

    @Test
    @DisplayName("Should throw when cache.enabled=false and params are provided")
    void validate_enabledFalseWithParams_throwsIllegalArgumentException() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(false);
        config.setType(CacheTypeEnum.CAFFEINE);

        assertThrows(IllegalArgumentException.class,
                () -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should do nothing when enabled=null and no params are provided")
    void validate_enabledNullNoParams_doesNothing() {

        final CacheConfiguration config = new CacheConfiguration();

        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should allow params when enabled=null (treated as enabled=true)")
    void validate_enabledNullWithParams_continuesValidation() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(null);
        config.setType(CacheTypeEnum.CAFFEINE);
        config.setMaxSize(10L);
        config.setExpiration(60);

        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should allow enabled=true without params (fallback SIMPLE)")
    void validate_enabledTrueWithoutParams_allowsFallbackSimple() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);

        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should throw when CAFFEINE cache has maxSize <= 0")
    void validate_caffeineWithMaxSizeZero_throwsIllegalArgumentException() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);
        config.setType(CacheTypeEnum.CAFFEINE);
        config.setMaxSize(0L);

        assertThrows(IllegalArgumentException.class,
                () -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should accept valid CAFFEINE configuration")
    void validate_caffeineWithValidMaxSize_doesNotThrow() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);
        config.setType(CacheTypeEnum.CAFFEINE);
        config.setMaxSize(100L);
        config.setExpiration(60);

        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should ignore maxSize for non-CAFFEINE types")
    void validate_nonCaffeineWithMaxSize_ignoredWithoutException() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);
        config.setType(CacheTypeEnum.SIMPLE);
        config.setMaxSize(100L);
        config.setExpiration(60);

        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should ignore maxSize when type=null (treated as SIMPLE)")
    void validate_nullTypeWithMaxSize_ignoredWithoutException() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);
        config.setType(null);
        config.setMaxSize(100L);
        config.setExpiration(60);

        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should throw when expiration=0")
    void validate_expirationZero_throwsIllegalArgumentException() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);
        config.setType(CacheTypeEnum.CAFFEINE);
        config.setMaxSize(100L);
        config.setExpiration(0);

        assertThrows(IllegalArgumentException.class,
                () -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should throw when expiration<0")
    void validate_expirationNegative_throwsIllegalArgumentException() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);
        config.setType(CacheTypeEnum.CAFFEINE);
        config.setMaxSize(100L);
        config.setExpiration(-1);

        assertThrows(IllegalArgumentException.class,
                () -> CacheConfigurationValidator.validate(config));
    }

    @Test
    @DisplayName("Should accept valid SIMPLE configuration")
    void validate_validSimpleConfig_doesNotThrow() {

        final CacheConfiguration config = new CacheConfiguration();
        config.setEnabled(true);
        config.setType(CacheTypeEnum.SIMPLE);
        config.setExpiration(60);

        assertDoesNotThrow(() -> CacheConfigurationValidator.validate(config));
    }
}
