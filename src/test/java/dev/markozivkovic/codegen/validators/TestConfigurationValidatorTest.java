package dev.markozivkovic.codegen.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;

class TestConfigurationValidatorTest {

    @Test
    @DisplayName("validate: null configuration -> no exception")
    void validate_nullConfiguration_doesNothing() {
        
        assertDoesNotThrow(() -> TestConfigurationValidator.validate(null));
    }

    @Test
    @DisplayName("validate: unit = false, dataGenerator = null -> no exception")
    void validate_unitDisabled_noDataGenerator_ok() {
        
        final TestConfiguration cfg = Mockito.mock(TestConfiguration.class);
        when(cfg.getUnit()).thenReturn(Boolean.FALSE);
        when(cfg.getDataGenerator()).thenReturn(null);

        assertDoesNotThrow(() -> TestConfigurationValidator.validate(cfg));
    }

    @Test
    @DisplayName("validate: unit = null, dataGenerator = null -> no exception (unit treated as disabled)")
    void validate_unitNull_noDataGenerator_ok() {
        
        final TestConfiguration cfg = Mockito.mock(TestConfiguration.class);
        when(cfg.getUnit()).thenReturn(null);
        when(cfg.getDataGenerator()).thenReturn(null);

        assertDoesNotThrow(() -> TestConfigurationValidator.validate(cfg));
    }

    @Test
    @DisplayName("validate: unit = true, dataGenerator = null -> throws IllegalArgumentException with enum list")
    void validate_unitEnabled_noDataGenerator_throws() {
        
        final TestConfiguration cfg = Mockito.mock(TestConfiguration.class);
        when(cfg.getUnit()).thenReturn(Boolean.TRUE);
        when(cfg.getDataGenerator()).thenReturn(null);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TestConfigurationValidator.validate(cfg)
        );

        final String expectedEnumList = Arrays.stream(DataGeneratorEnum.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        final String message = ex.getMessage();
        
        assertTrue(
                message.contains("unit is enabled, but dataGenerator is not set"),
                "Expected error message to mention missing dataGenerator"
        );
        
        assertTrue(
                message.contains(expectedEnumList),
                "Expected error message to contain enum values: " + expectedEnumList
        );
    }

    @Test
    @DisplayName("validate: unit = true, dataGenerator set -> no exception")
    void validate_unitEnabled_withDataGenerator_ok() {
        
        final TestConfiguration cfg = Mockito.mock(TestConfiguration.class);
        when(cfg.getUnit()).thenReturn(Boolean.TRUE);
        when(cfg.getDataGenerator()).thenReturn(DataGeneratorEnum.INSTANCIO);

        assertDoesNotThrow(() -> TestConfigurationValidator.validate(cfg));
    }
}
