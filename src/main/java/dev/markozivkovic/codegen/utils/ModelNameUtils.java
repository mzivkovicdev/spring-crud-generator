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

package dev.markozivkovic.codegen.utils;

import java.util.List;

public class ModelNameUtils {

    private static final List<String> SQL_MODEL_SUFFIXES = List.of(
        "JpaEntity", "Entity", "Model", "Table", "Domain", "DAO", "Data"
    );

    private ModelNameUtils() {

    }

    /**
     * Removes a suffix from a model name, if the model name ends with it.
     * 
     * @param modelName the model name to strip the suffix from
     * @return the model name with the suffix removed, or the original model name if it does not match any suffix
     */
    public static String stripSuffix(final String modelName) {

        return SQL_MODEL_SUFFIXES.stream()
                .filter(suffix -> modelName.toLowerCase().endsWith(suffix.toLowerCase()))
                .findFirst()
                .map(suffix -> modelName.substring(0, modelName.length() - suffix.length()))
                .orElse(modelName);
    }

    /**
     * Computes the name of the OpenAPI model, given a model name.
     * This method strips the suffix from the model name (if it matches any of the known suffixes) and then appends "Payload" to the resulting string.
     * 
     * @param modelName the model name to compute the OpenAPI model name from
     * @return the computed OpenAPI model name
     */
    public static String computeOpenApiModelName(final String modelName) {

        return String.format("%sPayload", stripSuffix(modelName));
    }

    /**
     * Computes the name of the OpenAPI input model, given a model name.
     * This method strips the suffix from the model name (if it matches any of the known suffixes) and then appends "Input" to the resulting string.
     * 
     * @param modelName the model name to compute the OpenAPI input model name from
     * @return the computed OpenAPI input model name
     */
    public static String computeOpenApiInputModelName(final String modelName) {
        return String.format("%sInput", stripSuffix(modelName));
    }

    /**
     * Computes the name of the OpenAPI create model, given a model name.
     * 
     * @param modelName the model name to compute the OpenAPI create model name from
     * @return the computed OpenAPI create model name
     */
    public static String computeOpenApiCreateModelName(final String modelName) {

        return String.format("%sCreatePayload", stripSuffix(modelName));
    }

    /**
     * Computes the name of the OpenAPI update model, given a model name.
     * 
     * @param modelName the model name to compute the OpenAPI update model name from
     * @return the computed OpenAPI update model name
     */
    public static String computeOpenApiUpdateModelName(final String modelName) {

        return String.format("%sUpdatePayload", stripSuffix(modelName));
    }

    /**
     * Computes the name of the OpenAPI create model, given a model name.
     * This method strips the suffix from the model name (if it matches any of the known suffixes) and then appends "CreateTO" to the resulting string.
     * 
     * @param modelName the model name to compute the OpenAPI create model name from
     * @return the computed OpenAPI create model name
     */
    public static String computeCreateTOModelName(final String modelName) {

        return String.format("%sCreateTO", stripSuffix(modelName));
    }

    /**
     * Computes the name of the OpenAPI update model, given a model name.
     * This method strips the suffix from the model name (if it matches any of the known suffixes) and then appends "UpdateTO" to the resulting string.
     * 
     * @param modelName the model name to compute the OpenAPI update model name from
     * @return the computed OpenAPI update model name
     */
    public static String computeUpdateTOModelName(final String modelName) {

        return String.format("%sUpdateTO", stripSuffix(modelName));
    }

    /**
     * Computes the name of the OpenAPI input model, given a model name.
     * This method strips the suffix from the model name (if it matches any of the known suffixes) and then appends "InputTO" to the resulting string.
     * 
     * @param modelName the model name to compute the OpenAPI input model name from
     * @return the computed OpenAPI input model name
     */
    public static String computeInputTOModelName(final String modelName) {
        return String.format("%sInputTO", stripSuffix(modelName));
    }

    /**
     * Converts a camel-case string to a snake-case string.
     * @param s the string to convert
     * @return the converted string
     */
    public static String toSnakeCase(final String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
}
