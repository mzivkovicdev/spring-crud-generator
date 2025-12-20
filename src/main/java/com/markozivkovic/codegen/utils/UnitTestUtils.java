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
     * Returns true if Instancio is enabled in the given configuration, false otherwise.
     * Instancio is enabled if the configuration is null, or if the tests configuration in the
     * given configuration is null, or if the data generator in the tests configuration is Instancio.
     *
     * @param configuration the configuration to check
     * @return true if Instancio is enabled, false otherwise
     */
    public static boolean isInstancioEnabled(final CrudConfiguration configuration) {
        
        return configuration != null && configuration.getTests() != null &&
                configuration.getTests().getDataGenerator() != null &&
                configuration.getTests().getDataGenerator().equals(DataGeneratorEnum.INSTANCIO);
    }

    /**
     * Returns a TestDataGeneratorConfig object based on the given DataGeneratorEnum.
     * The TestDataGeneratorConfig object contains the name of the data generator, the name of the
     * factory class, the name of the factory method, and the name of the method to
     * manufacture the list of objects.
     *
     * @param dataGenerator the DataGeneratorEnum to resolve
     * @return a TestDataGeneratorConfig object containing the resolved data generator config
     */
    public static TestDataGeneratorConfig resolveGeneratorConfig(final DataGeneratorEnum dataGenerator) {
        
        return switch (dataGenerator) {
            case INSTANCIO -> new TestDataGeneratorConfig(
                    DataGeneratorEnum.INSTANCIO.name().toUpperCase(),
                    "Instancio",
                    "create",
                    "ofList");
            case PODAM -> new TestDataGeneratorConfig(
                    DataGeneratorEnum.PODAM.name().toUpperCase(),
                    "PODAM_FACTORY",
                    "manufacturePojo",
                    "manufacturePojo");
            default -> throw new IllegalArgumentException(
                    String.format(
                        "Unsupported data generator: %s",
                        dataGenerator
                    )
            );
        };
    }

    public static record TestDataGeneratorConfig(
            String generator,
            String randomFieldName,
            String singleObjectMethodName,
            String multipleObjectsMethodName) {
        
    }

}
