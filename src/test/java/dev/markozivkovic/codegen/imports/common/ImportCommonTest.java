package dev.markozivkovic.codegen.imports.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.codegen.constants.ImportConstants;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.FieldUtils;

class ImportCommonTest {

    @Test
    @DisplayName("Should add value to set when condition is true")
    void addIf_conditionTrue_addsValue() {
        
        final Set<String> imports = new HashSet<>();

        ImportCommon.addIf(true, imports, "java.util.List");

        assertTrue(imports.contains("java.util.List"));
        assertEquals(1, imports.size());
    }

    @Test
    @DisplayName("Should NOT add value to set when condition is false")
    void addIf_conditionFalse_doesNotAddValue() {
        
        final Set<String> imports = new HashSet<>();

        ImportCommon.addIf(false, imports, "java.util.List");

        assertFalse(imports.contains("java.util.List"));
        assertTrue(imports.isEmpty());
    }

    @Test
    @DisplayName("Should ignore null value when condition is true")
    void addIf_nullValueStillAcceptedBySet() {
        
        final Set<String> imports = new HashSet<>();

        ImportCommon.addIf(true, imports, null);

        assertTrue(imports.contains(null));
        assertEquals(1, imports.size());
    }

    @Test
    @DisplayName("importListAndSetForSimpleCollection: when no simple collection fields -> does not add LIST/SET")
    void importListAndSetForSimpleCollection_noSimpleCollections_addsNothing() {

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getFields()).thenReturn(Collections.emptyList());

        final Set<String> imports = new LinkedHashSet<>();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class, Mockito.CALLS_REAL_METHODS)) {

            fieldUtils.when(() -> FieldUtils.extractSimpleCollectionFields(anyList()))
                    .thenReturn(Collections.emptyList());

            importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                    .thenAnswer(inv -> {
                        final boolean cond = inv.getArgument(0);
                        final Set<String> set = inv.getArgument(1);
                        final String value = inv.getArgument(2);
                        if (cond) set.add(value);
                        return null;
                    });

            ImportCommon.importListAndSetForSimpleCollection(model, imports);

            assertFalse(imports.contains(ImportConstants.Java.LIST));
            assertFalse(imports.contains(ImportConstants.Java.SET));
            assertTrue(imports.isEmpty(), "No imports should be added");
        }
    }

    @Test
    @DisplayName("importListAndSetForSimpleCollection: if any simple collection is List -> adds java.util.List")
    void importListAndSetForSimpleCollection_hasList_addsList() {

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getFields()).thenReturn(Collections.emptyList());

        final FieldDefinition listField = mock(FieldDefinition.class);
        when(listField.getType()).thenReturn("List<String>");

        final Set<String> imports = new LinkedHashSet<>();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class, Mockito.CALLS_REAL_METHODS)) {

            fieldUtils.when(() -> FieldUtils.extractSimpleCollectionFields(anyList()))
                    .thenReturn(List.of(listField));

            importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                    .thenAnswer(inv -> {
                        final boolean cond = inv.getArgument(0);
                        final Set<String> set = inv.getArgument(1);
                        final String value = inv.getArgument(2);
                        if (cond) set.add(value);
                        return null;
                    });

            ImportCommon.importListAndSetForSimpleCollection(model, imports);

            assertTrue(imports.contains(ImportConstants.Java.LIST), "Expected LIST import");
            assertFalse(imports.contains(ImportConstants.Java.SET), "Did not expect SET import");
        }
    }

    @Test
    @DisplayName("importListAndSetForSimpleCollection: if any simple collection is Set -> adds java.util.Set")
    void importListAndSetForSimpleCollection_hasSet_addsSet() {

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getFields()).thenReturn(Collections.emptyList());

        final FieldDefinition setField = mock(FieldDefinition.class);
        when(setField.getType()).thenReturn("Set<String>");

        final Set<String> imports = new LinkedHashSet<>();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class, Mockito.CALLS_REAL_METHODS)) {

            fieldUtils.when(() -> FieldUtils.extractSimpleCollectionFields(anyList()))
                    .thenReturn(List.of(setField));

            importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                    .thenAnswer(inv -> {
                        final boolean cond = inv.getArgument(0);
                        final Set<String> set = inv.getArgument(1);
                        final String value = inv.getArgument(2);
                        if (cond) set.add(value);
                        return null;
                    });

            ImportCommon.importListAndSetForSimpleCollection(model, imports);

            assertFalse(imports.contains(ImportConstants.Java.LIST), "Did not expect LIST import");
            assertTrue(imports.contains(ImportConstants.Java.SET), "Expected SET import");
        }
    }

    @Test
    @DisplayName("importListAndSetForSimpleCollection: if both List and Set exist -> adds both imports")
    void importListAndSetForSimpleCollection_hasListAndSet_addsBoth() {

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getFields()).thenReturn(Collections.emptyList());

        final FieldDefinition listField = mock(FieldDefinition.class);
        when(listField.getType()).thenReturn("List<String>");

        final FieldDefinition setField = mock(FieldDefinition.class);
        when(setField.getType()).thenReturn("Set<String>");

        final Set<String> imports = new LinkedHashSet<>();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class, Mockito.CALLS_REAL_METHODS)) {

            fieldUtils.when(() -> FieldUtils.extractSimpleCollectionFields(anyList()))
                    .thenReturn(List.of(listField, setField));

            importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                    .thenAnswer(inv -> {
                        final boolean cond = inv.getArgument(0);
                        final Set<String> set = inv.getArgument(1);
                        final String value = inv.getArgument(2);
                        if (cond) set.add(value);
                        return null;
                    });

            ImportCommon.importListAndSetForSimpleCollection(model, imports);

            assertTrue(imports.contains(ImportConstants.Java.LIST), "Expected LIST import");
            assertTrue(imports.contains(ImportConstants.Java.SET), "Expected SET import");
        }
    }

    @Test
    @DisplayName("importListAndSetForSimpleCollection: should not duplicate imports when called multiple times")
    void importListAndSetForSimpleCollection_idempotent() {

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getFields()).thenReturn(Collections.emptyList());

        final FieldDefinition listField = mock(FieldDefinition.class);
        when(listField.getType()).thenReturn("List<String>");

        final Set<String> imports = new LinkedHashSet<>();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class, Mockito.CALLS_REAL_METHODS)) {

            fieldUtils.when(() -> FieldUtils.extractSimpleCollectionFields(anyList()))
                    .thenReturn(List.of(listField));

            importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                    .thenAnswer(inv -> {
                        final boolean cond = inv.getArgument(0);
                        final Set<String> set = inv.getArgument(1);
                        final String value = inv.getArgument(2);
                        if (cond) set.add(value);
                        return null;
                    });

            ImportCommon.importListAndSetForSimpleCollection(model, imports);
            ImportCommon.importListAndSetForSimpleCollection(model, imports);

            assertTrue(imports.contains(ImportConstants.Java.LIST));
            assertEquals(1, imports.stream().filter(ImportConstants.Java.LIST::equals).count(),
                    "LIST import should appear only once");
        }
    }
}
