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

package com.markozivkovic.codegen.enums;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SpecialType {
    
    ENUM("Enum"),
    JSON("JSON"),
    LIST("List"),
    SET("Set");

    private final String key;

    SpecialType(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns true if the given type is equal to "ENUM", false otherwise.
     * 
     * @param type the type to check
     * @return true if the type is equal to "ENUM", false otherwise
     */
    public static boolean isEnumType(final String type) {

        return Objects.nonNull(type) && type.equalsIgnoreCase(ENUM.getKey());
    }

    /**
     * Returns true if the given type is a JSON type, false otherwise.
     * A JSON type is a type that starts with "JSON<" and ends with ">". For example, "JSON<SomeType>" is a JSON type.
     * 
     * @param type the type to check
     * @return true if the type is a JSON type, false otherwise
     */
    public static boolean isJsonType(final String type) {

        return Objects.nonNull(type) &&
                type.toUpperCase(Locale.ROOT).startsWith(JSON.getKey() + "<") &&
                type.endsWith(">");
    }

    /**
     * Returns true if the given type is a collection type, false otherwise.
     * A collection type is a type that starts with "List<" or "Set<" and ends with ">". For example, "List<SomeType>" is a collection type.
     * 
     * @param type the type to check
     * @return true if the type is a collection type, false otherwise
     */
    public static boolean isCollectionType(final String type) {

        return Objects.nonNull(type) &&
                (type.toUpperCase(Locale.ROOT).startsWith(SpecialType.LIST.getKey().toUpperCase(Locale.ROOT) + "<") ||
                type.toUpperCase(Locale.ROOT).startsWith(SpecialType.SET.getKey().toUpperCase(Locale.ROOT) + "<")) &&
                type.endsWith(">");
    }

    /**
     * Returns true if the given type is a List type, false otherwise.
     * A List type is a type that starts with "List<" and ends with ">". For example, "List<SomeType>" is a List type.
     * 
     * @param type the type to check
     * @return true if the type is a List type, false otherwise
     */
    public static boolean isListType(final String type) {
        
        return Objects.nonNull(type) &&
                type.toUpperCase(Locale.ROOT).startsWith(SpecialType.LIST.getKey().toUpperCase(Locale.ROOT) + "<") &&
                type.endsWith(">");
    }

    /**
     * Returns true if the given type is a Set type, false otherwise.
     * A Set type is a type that starts with "Set<" and ends with ">". For example, "Set<SomeType>" is a Set type.
     * 
     * @param type the type to check
     * @return true if the type is a Set type, false otherwise
     */
    public static boolean isSetType(final String type) {

        return Objects.nonNull(type) &&
                type.toUpperCase(Locale.ROOT).startsWith(SpecialType.SET.getKey().toUpperCase(Locale.ROOT) + "<") &&
                type.endsWith(">");
    }

    /**
     * Returns a string containing all supported special types, separated by commas.
     * 
     * @return a string containing all supported special types, separated by commas
     */
    public static String getSupportedValues() {

        return Stream.of(values())
                .map(SpecialType::getKey)
                .collect(Collectors.joining(", "));
    }
}
