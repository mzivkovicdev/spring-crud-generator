package dev.markozivkovic.codegen.validators;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;

public class TestConfigurationValidator {
    
    private TestConfigurationValidator() {}

    /**
     * Validates the given test configuration.
     * 
     * If the unit test is enabled, then the data generator must be set.
     * 
     * @param testConfiguration the test configuration to validate
     * @throws IllegalArgumentException if the unit test is enabled but the data generator is not set
     */
    public static void validate(final TestConfiguration testConfiguration) {

        if (Objects.isNull(testConfiguration)) return;

        if (!Boolean.TRUE.equals(testConfiguration.getUnit())) return;

        if (Objects.isNull(testConfiguration.getDataGenerator())) {
            
            final String expected = Arrays.asList(DataGeneratorEnum.values()).stream()
                    .map(DataGeneratorEnum::name)
                    .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                String.format(
                    "Invalid test configuration: unit is enabled, but dataGenerator is not set. Please set dataGenerator to one of the following values: %s",
                    expected
                )
            );
        }
    }

}
