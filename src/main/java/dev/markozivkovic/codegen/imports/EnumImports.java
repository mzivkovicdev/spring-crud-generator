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

package dev.markozivkovic.codegen.imports;

import static dev.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.StringUtils;

public class EnumImports {

    private EnumImports() {}

    /**
     * Computes the necessary imports for the given model definition, including the enums if any exist.
     *
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param packagePath          the package path where the generated code will be written
     * @param packageConfiguration the package configuration for the project
     * @return A set of strings containing the necessary import statements for the given model.
     */
    public static Set<String> computeEnumImports(final ModelDefinition modelDefinition, final String packagePath,
                final PackageConfiguration packageConfiguration) {
        
        final List<FieldDefinition> enumFields = FieldUtils.extractEnumFields(modelDefinition.getFields());
        final Set<String> imports = new LinkedHashSet<>();

        enumFields.forEach(enumField -> {
            
            final String enumName;
            if (!enumField.getName().endsWith("Enum")) {
                enumName = String.format("%sEnum", StringUtils.capitalize(enumField.getName()));
            } else {
                enumName = StringUtils.capitalize(enumField.getName());
            }

            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEnumPackage(packagePath, packageConfiguration), enumName)));
        });

        return imports;
    }
}
