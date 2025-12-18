package com.markozivkovic.codegen.imports.common;

import java.util.List;
import java.util.Set;

import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.enums.SpecialType;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.ContainerUtils;
import com.markozivkovic.codegen.utils.FieldUtils;

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
            final boolean isAnyFieldList = simpleCollectionFields.stream().anyMatch(field -> SpecialType.isListType(field.getType()));
            final boolean isAnyFieldSet = simpleCollectionFields.stream().anyMatch(field -> SpecialType.isSetType(field.getType()));
            addIf(isAnyFieldList, imports, ImportConstants.Java.LIST);
            addIf(isAnyFieldSet, imports, ImportConstants.Java.SET);
        }
    }

}
