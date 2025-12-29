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

public class StringUtils {
    
    private StringUtils() {
        
    }

    /**
     * Returns a new string with the first character of the given string capitalized.
     * 
     * @param str The string to capitalize.
     * @return A new string with the first character capitalized.
     */
    public static String capitalize(final String str) {
        if (!isNotBlank(str)) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Returns a new string with the first character of the given string converted to lowercase.
     *
     * @param str The string to uncapitalize.
     * @return A new string with the first character in lowercase.
     */
    public static String uncapitalize(final String str) {
        if (!isNotBlank(str)) return str;
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * Checks if a String is not empty (""), not null and not whitespace only.
     * 
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is not empty and not null and not whitespace only
     */
    public static boolean isNotBlank(final String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        return !str.isBlank();
    }

    /**
     * Checks if a String is empty (""), not null and not whitespace only.
     * 
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is empty and not null and not whitespace only
     */
    public static boolean isBlank(final String str) {
        return !isNotBlank(str);
    }

}
