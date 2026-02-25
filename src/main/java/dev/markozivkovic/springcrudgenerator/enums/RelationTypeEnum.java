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

package dev.markozivkovic.springcrudgenerator.enums;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RelationTypeEnum {
    
    ONE_TO_ONE("OneToOne"),
    ONE_TO_MANY("OneToMany"),
    MANY_TO_ONE("ManyToOne"),
    MANY_TO_MANY("ManyToMany");

    private final String key;

    RelationTypeEnum(final String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    /**
     * Returns a list of the default lazy relation types, which are {@link ONE_TO_MANY} and {@link MANY_TO_MANY}.
     * These are the relation types that are not eagerly loaded by default.
     * 
     * @return a list containing the default lazy relation types
     */
    public static List<String> getDefaultLazyTypes() {
        return List.of(ONE_TO_MANY.getKey(), MANY_TO_MANY.getKey());
    }

    /**
     * Returns a list of the default eager relation types, which are {@link ONE_TO_ONE} and {@link MANY_TO_ONE}.
     * These are the relation types that are eagerly loaded by default.
     * 
     * @return a list containing the default eager relation types
     */
    public static List<String> getDefaultEagerTypes() {
        return List.of(ONE_TO_ONE.getKey(), MANY_TO_ONE.getKey());
    }

    /**
     * Returns the enum constant with the given value, or throws an exception if no such constant exists.
     * The value is case-insensitive.
     * 
     * @param value the value of the enum constant
     * @return the enum constant with the given value
     * @throws IllegalArgumentException if no enum constant with the given value exists
     */
    public static RelationTypeEnum fromString(final String value) {
        
        return Stream.of(values())
                .filter(enumValue -> enumValue.key.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(
                                "No enum constant with value %s. Possible values are: %s",
                                value, getPossibleValues()
                        )
                ));
    }

    /**
     * Returns a string containing all possible values of the relation types, separated by commas.
     * This string can be used to construct error messages or other text that requires a list of possible relation types.
     * 
     * @return a string containing all possible values of the relation types, separated by commas
     */
    public static String getPossibleValues() {

        return Stream.of(values())
                .map(RelationTypeEnum::getKey)
                .collect(Collectors.joining(", "));
    }


}
