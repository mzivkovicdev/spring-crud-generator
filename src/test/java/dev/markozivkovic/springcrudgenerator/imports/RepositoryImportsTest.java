package dev.markozivkovic.springcrudgenerator.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class RepositoryImportsTest {

    @Test
    @DisplayName("computeJpaRepostiroyImports: UUID id -> includes java.util.UUID import")
    void computeJpaRepositoryImports_uuidId_addsUuidImport() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(new FieldDefinition());
        when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(true);

            final String result = RepositoryImports.computeJpaRepostiroyImports(model, true);

            assertTrue(result.contains("import " + ImportConstants.Java.UUID));
            assertFalse(result.contains("import " + ImportConstants.Java.OPTIONAL));
        }
    }

    @Test
    @DisplayName("computeJpaRepostiroyImports: OSIV=false and hasLazyFetchField=true -> includes java.util.Optional import")
    void computeJpaRepositoryImports_osivFalse_lazyTrue_addsOptional() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(new FieldDefinition());
        when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(true);

            final String result = RepositoryImports.computeJpaRepostiroyImports(model, false);

            assertTrue(result.contains("import " + ImportConstants.Java.OPTIONAL));
            assertFalse(result.contains("import " + ImportConstants.Java.UUID));
        }
    }

    @Test
    @DisplayName("computeJpaRepostiroyImports: OSIV=true even if hasLazyFetchField=true -> does NOT include Optional")
    void computeJpaRepositoryImports_osivTrue_lazyTrue_noOptional() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(new FieldDefinition());
        when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(true);

            final String result = RepositoryImports.computeJpaRepostiroyImports(model, true);

            assertFalse(result.contains("import " + ImportConstants.Java.OPTIONAL));
            assertFalse(result.contains("import " + ImportConstants.Java.UUID));
        }
    }

    @Test
    @DisplayName("computeJpaRepostiroyImports: OSIV=false and hasLazyFetchField=false -> does NOT include Optional")
    void computeJpaRepositoryImports_osivFalse_lazyFalse_noOptional() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(new FieldDefinition());
        when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(false);

            final String result = RepositoryImports.computeJpaRepostiroyImports(model, false);

            assertFalse(result.contains("import " + ImportConstants.Java.OPTIONAL));
            assertFalse(result.contains("import " + ImportConstants.Java.UUID));
        }
    }

    @Test
    @DisplayName("computeJpaRepostiroyImports: UUID id + OSIV=false + lazy=true -> includes both UUID and Optional imports")
    void computeJpaRepositoryImports_uuidAndOptional_bothAdded() {

        final ModelDefinition model = mock(ModelDefinition.class);
        final List<FieldDefinition> fields = List.of(new FieldDefinition());
        when(model.getFields()).thenReturn(fields);

        final FieldDefinition idField = new FieldDefinition();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(true);

            final String result = RepositoryImports.computeJpaRepostiroyImports(model, false);

            assertTrue(result.contains("import " + ImportConstants.Java.UUID));
            assertTrue(result.contains("import " + ImportConstants.Java.OPTIONAL));
        }
    }

    @Test
    @DisplayName("computeProjectImports: uses computed entity package and joins with model name")
    void computeProjectImports_usesPackageUtils() {

        final String packagePath = "com.example.app";
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final String modelName = "UserEntity";

        try (final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class)) {

            pkg.when(() -> PackageUtils.computeEntityPackage(packagePath, pkgCfg)).thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.join("com.example.app.entity", modelName)).thenReturn("com.example.app.entity.UserEntity");

            final String result = RepositoryImports.computeProjectImports(packagePath, pkgCfg, modelName);

            assertTrue(result.contains("import com.example.app.entity.UserEntity"));
            pkg.verify(() -> PackageUtils.computeEntityPackage(packagePath, pkgCfg));
            pkg.verify(() -> PackageUtils.join("com.example.app.entity", modelName));
        }
    }
}
