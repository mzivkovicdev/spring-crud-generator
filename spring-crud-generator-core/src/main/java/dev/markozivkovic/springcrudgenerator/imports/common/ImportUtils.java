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

package dev.markozivkovic.springcrudgenerator.imports.common;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for formatting and joining import statements.
 */
public final class ImportUtils {

    private ImportUtils() {}

    /**
     * Formats fully-qualified class names into {@code import ...;} statements, sorts them lexicographically and joins
     * them into a single string.
     *
     * @param imports set of fully-qualified class names
     * @return formatted and sorted import statements
     */
    public static String sortAndFormatImports(final Set<String> imports) {

        return imports.stream()
                .sorted()
                .map(imp -> String.format(IMPORT, imp))
                .collect(Collectors.joining());
    }

    /**
     * Sorts and joins already formatted import statements (for example values already created with
     * {@code String.format(IMPORT, ...)}).
     *
     * @param formattedImports set of already formatted import statements
     * @return sorted and joined import statements
     */
    public static String sortAndJoinFormattedImports(final Set<String> formattedImports) {

        return formattedImports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Joins non-empty import groups with one line separator between groups.
     * <p>
     * Since each import statement already ends with a line separator, this effectively produces one blank line between groups.
     *
     * @param groups import groups (some may be null or blank)
     * @return joined import groups
     */
    public static String joinImportGroups(final String... groups) {

        final List<String> nonEmptyGroups = Arrays.stream(groups)
                .filter(group -> group != null && !group.isBlank())
                .collect(Collectors.toList());

        return String.join(System.lineSeparator(), nonEmptyGroups);
    }
}
