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

import java.util.List;
import java.util.Set;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.enums.SpecialTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.utils.ContainerUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;

public class ImportCommon {
    
    private ImportCommon() {}

    /**
     * Adds the given value to the given set if the condition is true.
     *
     * @param condition The condition to check.
     * @param set       The set to add to.
     * @param value     The value to add.
     */
    public static void addIf(final boolean condition, final Set<String> set, final String value) {
        if (condition) {
            set.add(value);
        }
    }

    /**
     * Imports the necessary types for simple collections (e.g. List, Set) found in the given model definition.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param imports            the set of import statements to add to
     */
    public static void importListAndSetForSimpleCollection(final ModelDefinition modelDefinition, final Set<String> imports) {

        final List<FieldDefinition> simpleCollectionFields = FieldUtils.extractSimpleCollectionFields(modelDefinition.getFields());
        if (!ContainerUtils.isEmpty(simpleCollectionFields)) {
            final boolean isAnyFieldList = simpleCollectionFields.stream().anyMatch(field -> SpecialTypeEnum.isListType(field.getType()));
            final boolean isAnyFieldSet = simpleCollectionFields.stream().anyMatch(field -> SpecialTypeEnum.isSetType(field.getType()));
            addIf(isAnyFieldList, imports, ImportConstants.Java.LIST);
            addIf(isAnyFieldSet, imports, ImportConstants.Java.SET);
        }
    }

}
