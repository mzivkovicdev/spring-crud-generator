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

package dev.markozivkovic.springcrudgenerator.imports;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

public final class RepositoryImports {

    private RepositoryImports() {}

    /**
     * Computes the necessary imports for the given model definition, including the UUID if any model has a UUID as its ID and the Optional if openInViewEnabled is false.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param openInViewEnabled whether to include the Optional import
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeJpaRepostiroyImports(final ModelDefinition modelDefinition, final Boolean openInViewEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        if (FieldUtils.isIdFieldUUID(idField)) {
            imports.add(String.format(IMPORT, ImportConstants.Java.UUID));
        }

        if (!openInViewEnabled && FieldUtils.hasLazyFetchField(modelDefinition.getFields())) {
            imports.add(String.format(IMPORT, ImportConstants.Java.OPTIONAL));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary import for the given model name, including the package path and the package configuration.
     *
     * @param packagePath          the package path of the project
     * @param packageConfiguration the package configuration of the project
     * @param modelName            the name of the model
     * @return A string containing the necessary import statement for the given model.
     */
    public static String computeProjectImports(final String packagePath, final PackageConfiguration packageConfiguration, final String modelName) {

        return String.format(
            IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelName)
        );
    }
    
}
