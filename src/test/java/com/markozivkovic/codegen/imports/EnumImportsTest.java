package com.markozivkovic.codegen.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

class EnumImportsTest {

    @Test
    @DisplayName("Should return empty set when there are no enum fields")
    void computeEnumImports_noEnumFields_returnsEmptySet() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String packagePath = "com.example";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractEnumFields(anyList()))
                    .thenReturn(Collections.emptyList());

            final Set<String> result = EnumImports.computeEnumImports(model, packagePath, packageConfiguration);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("Should compute import with 'Enum' suffix when field name does not end with 'Enum'")
    void computeEnumImports_addsEnumSuffixWhenMissing() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition statusField = new FieldDefinition();
        statusField.setName("status");
        model.setFields(List.of(statusField));

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String packagePath = "com.example";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractEnumFields(anyList()))
                    .thenReturn(List.of(statusField));

            packageUtils.when(() -> PackageUtils.computeEnumPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.enums");
            packageUtils.when(() -> PackageUtils.join("com.example.enums", "StatusEnum"))
                    .thenReturn("com.example.enums.StatusEnum");

            final Set<String> result = EnumImports.computeEnumImports(model, packagePath, packageConfiguration);

            assertEquals(1, result.size());

            final String importLine = result.iterator().next();
            assertTrue(importLine.contains("com.example.enums.StatusEnum"),
                    "Expected import for com.example.enums.StatusEnum, but was: " + importLine);
        }
    }

    @Test
    @DisplayName("Should not duplicate 'Enum' suffix when field name already ends with 'Enum'")
    void computeEnumImports_usesNameAsIsWhenAlreadyEndsWithEnum() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition statusEnumField = new FieldDefinition();
        statusEnumField.setName("statusEnum");
        model.setFields(List.of(statusEnumField));

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String packagePath = "com.example";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractEnumFields(anyList()))
                    .thenReturn(List.of(statusEnumField));

            packageUtils.when(() -> PackageUtils.computeEnumPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.enums");
            packageUtils.when(() -> PackageUtils.join("com.example.enums", "StatusEnum"))
                    .thenReturn("com.example.enums.StatusEnum");

            final Set<String> result = EnumImports.computeEnumImports(model, packagePath, packageConfiguration);

            assertEquals(1, result.size());
            final String importLine = result.iterator().next();

            assertFalse(importLine.contains("statusEnumEnum"));
            assertTrue(importLine.contains("com.example.enums.StatusEnum"));
        }
    }

    @Test
    @DisplayName("Should compute imports for multiple enum fields and avoid duplicates")
    void computeEnumImports_multipleEnumFields_noDuplicates() {

        final ModelDefinition model = new ModelDefinition();

        final FieldDefinition statusField = new FieldDefinition();
        statusField.setName("status");

        final FieldDefinition statusEnumField = new FieldDefinition();
        statusEnumField.setName("statusEnum");

        final FieldDefinition typeField = new FieldDefinition();
        typeField.setName("type");

        model.setFields(List.of(statusField, statusEnumField, typeField));

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String packagePath = "com.example";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractEnumFields(anyList()))
                    .thenReturn(List.of(statusField, statusEnumField, typeField));

            packageUtils.when(() -> PackageUtils.computeEnumPackage(packagePath, packageConfiguration))
                    .thenReturn("com.example.enums");

            packageUtils.when(() -> PackageUtils.join("com.example.enums", "StatusEnum"))
                    .thenReturn("com.example.enums.StatusEnum");
            packageUtils.when(() -> PackageUtils.join("com.example.enums", "TypeEnum"))
                    .thenReturn("com.example.enums.TypeEnum");

            final Set<String> result = EnumImports.computeEnumImports(model, packagePath, packageConfiguration);

            assertEquals(2, result.size());

            final String resultString = String.join("\n", result);
            assertTrue(resultString.contains("com.example.enums.StatusEnum"));
            assertTrue(resultString.contains("com.example.enums.TypeEnum"));
        }
    }
}
