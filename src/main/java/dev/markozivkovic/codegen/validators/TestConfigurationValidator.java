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
