package com.markozivkovic.codegen.utils;

import com.markozivkovic.codegen.enums.SupportedIdTypeEnum;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;

public class UnitTestUtils {
    
    private UnitTestUtils () {}

    /**
     * Computes an invalid ID type for the given ID field. If the ID type is {@link SupportedIdTypeEnum#LONG},
     * {@link SupportedIdTypeEnum#INTEGER}, {@link SupportedIdTypeEnum#SHORT}, {@link SupportedIdTypeEnum#BYTE},
     * {@link SupportedIdTypeEnum#UUID}, {@link SupportedIdTypeEnum#BIG_INTEGER}, {@link SupportedIdTypeEnum#BIG_DECIMAL},
     * or {@link SupportedIdTypeEnum#BYTE_ARRAY}, the returned invalid ID type is {@link SupportedIdTypeEnum#STRING}.
     * If the ID type is {@link SupportedIdTypeEnum#STRING}, the returned invalid ID type is {@link SupportedIdTypeEnum#LONG}.
     * If the ID type is not supported, an {@link IllegalArgumentException} is thrown.
     *
     * @param idField the ID field to compute the invalid ID type for
     * @return the computed invalid ID type
     * @throws IllegalArgumentException if the ID type is not supported
     */
    public static String computeInvalidIdType(final FieldDefinition idField) {

        final SupportedIdTypeEnum supportedIdType = SupportedIdTypeEnum.resolveIdType(idField.getType());

        switch (supportedIdType) {
            case LONG:
            case INTEGER:
            case SHORT:
            case BYTE:
            case UUID:
            case BIG_INTEGER:
            case BIG_DECIMAL:
            case BYTE_ARRAY:
                return SupportedIdTypeEnum.STRING.getKey();
            case STRING:
                return SupportedIdTypeEnum.LONG.getKey();
            default:
                throw new IllegalArgumentException("Unsupported ID type: " + idField.getType());
        }
    }

    /**
     * Returns true if unit tests are enabled in the given configuration, false otherwise.
     * Unit tests are enabled if the configuration is null, or if the tests configuration in the
     * given configuration is null, or if the unit tests flag in the tests configuration is true.
     *
     * @param configuration the configuration to check
     * @return true if unit tests are enabled, false otherwise
     */
    public static boolean isUnitTestsEnabled(final CrudConfiguration configuration) {
        
        return configuration != null && configuration.getTests() != null &&
                Boolean.TRUE.equals(configuration.getTests().getUnit());
    }

    /**
     * Returns true if EasyRandom is enabled in the given configuration, false otherwise.
     * EasyRandom is enabled if the configuration is not null, the tests configuration in the
     * given configuration is not null, the data generator in the tests configuration is not null,
     * and the data generator is set to {@link DataGeneratorEnum#EASYRANDOM}.
     *
     * @param configuration the configuration to check
     * @return true if EasyRandom is enabled, false otherwise
     */
    public static boolean isEasyRandomEnabled(final CrudConfiguration configuration) {
        
        return configuration != null && configuration.getTests() != null &&
                configuration.getTests().getDataGenerator() != null &&
                configuration.getTests().getDataGenerator().equals(DataGeneratorEnum.EASYRANDOM);
    }

}
