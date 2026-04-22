package dev.markozivkovic.springcrudgenerator.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants.GeneratorContextKeys;
import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.imports.common.ImportCommon;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class BusinessServiceImportsTest {

    private ModelDefinition emptyModel() {
        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());
        Mockito.when(model.getAudit()).thenReturn(null);
        return model;
    }

    private ModelDefinition modelWithJsonCollectionFields() {
        final FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        idField.setType("Long");

        final FieldDefinition jsonListField = new FieldDefinition();
        jsonListField.setName("tags");
        jsonListField.setType("JSON<List<String>>");

        final FieldDefinition jsonSetField = new FieldDefinition();
        jsonSetField.setName("numbers");
        jsonSetField.setType("JSON<Set<Long>>");

        final ModelDefinition model = new ModelDefinition();
        model.setName("ExampleModel");
        model.setFields(List.of(idField, jsonListField, jsonSetField));
        model.setAudit(null);

        return model;
    }

    @Test
    @DisplayName("getBaseImport: should return empty string when there are no matching types and no lists")
    void getBaseImport_noTypesNoLists_returnsEmptyString() {
        
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getBaseImport(model, false);

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("getBaseImport: should add BigDecimal, UUID and List imports based on FieldUtils")
    void getBaseImport_addsImportsBasedOnFieldUtils() {
        
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getBaseImport(model, false);

            assertTrue(result.contains("import " + ImportConstants.Java.BIG_DECIMAL), "Expected BigDecimal import");
            assertTrue(result.contains("import " + ImportConstants.Java.UUID), "Expected UUID import");
            assertTrue(result.contains("import " + ImportConstants.Java.LIST), "Expected List import");

            assertTrue(result.endsWith(System.lineSeparator()), "Expected ending newline");
        }
    }

    @Test
    @DisplayName("getBaseImport: unique relation collections should import Set and HashSet")
    void getBaseImport_uniqueRelationCollections_importsSetAndHashSet() {

        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(true);

            final String result = BusinessServiceImports.getBaseImport(model, false);

            assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import");
            assertTrue(result.contains("import " + ImportConstants.Java.HASH_SET + ";"), "Expected HashSet import");
            assertFalse(result.contains("import " + ImportConstants.Java.LIST + ";"),
                    "List import should not be present when only unique relation collections exist");
        }
    }

    @Test
    @DisplayName("getBaseImport: list and unique relation collections should import List, Set and HashSet")
    void getBaseImport_listAndUniqueRelationCollections_importsListSetAndHashSet() {

        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(true);

            final String result = BusinessServiceImports.getBaseImport(model, false);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import");
            assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import");
            assertTrue(result.contains("import " + ImportConstants.Java.HASH_SET + ";"), "Expected HashSet import");
        }
    }

    @Test
    @DisplayName("getBaseImport: bulk create with collection relations should import Collectors")
    void getBaseImport_bulkCreateWithCollectionRelations_importsCollectors() {

        final ModelDefinition model = emptyModel();
        Mockito.when(model.isBulkCreateEnabled()).thenReturn(true);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(true);

            final String result = BusinessServiceImports.getBaseImport(model, true);

            assertTrue(result.contains("import " + ImportConstants.Java.COLLECTORS + ";"), "Expected Collectors import");
        }
    }

    @Test
    @DisplayName("getBaseImport: bulk create without collection relations should not import Collectors")
    void getBaseImport_bulkCreateWithoutCollectionRelations_doesNotImportCollectors() {

        final ModelDefinition model = emptyModel();
        Mockito.when(model.isBulkCreateEnabled()).thenReturn(true);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getBaseImport(model, true);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import");
            assertFalse(result.contains("import " + ImportConstants.Java.COLLECTORS + ";"),
                    "Collectors import should not be present without collection relations");
        }
    }

    @Test
    @DisplayName("getBaseImport: should add List import when importList=true even if there are no relations")
    void getBaseImport_importListFlagForcesListImport() {
        
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getBaseImport(model, true);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST));
        }
    }

    @Test
    @DisplayName("getBaseImport: should add audit import when audit is enabled")
    void getBaseImport_addsAuditImportWhenAuditEnabled() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        final List<FieldDefinition> fields = Collections.emptyList();
        Mockito.when(model.getFields()).thenReturn(fields);
        Mockito.when(model.getAudit().isEnabled()).thenReturn(true);
        Mockito.when(model.getAudit().getType()).thenReturn(AuditTypeEnum.INSTANT);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            auditUtils.when(() -> AuditUtils.resolveAuditingImport(AuditTypeEnum.INSTANT))
                    .thenReturn("java.time.Instant");

            final String result = BusinessServiceImports.getBaseImport(model, false);

            assertTrue(result.contains("import java.time.Instant;"));
        }
    }

    @Test
    @DisplayName("getBaseImport: should import List/Set when model has simple element collections (List<String>, Set<UUID>)")
    void getBaseImport_shouldImportListAndSet_forSimpleElementCollections() {
        final ModelDefinition model = emptyModel();
        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {
                
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

                importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                        .thenAnswer(inv -> {
                                @SuppressWarnings("unchecked")
                                final Set<String> imports = (Set<String>) inv.getArgument(1);
                                imports.add(ImportConstants.Java.LIST);
                                imports.add(ImportConstants.Java.SET);
                                return null;
                        });
                final String result = BusinessServiceImports.getBaseImport(model, false);
                assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import for element collections");
                assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import for element collections");
                assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport: should import List and Set for JSON collection fields")
    void getBaseImport_shouldImportListAndSet_forJsonCollectionFields() {

        final ModelDefinition model = modelWithJsonCollectionFields();

        final String result = BusinessServiceImports.getBaseImport(model, false);

        assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import");
        assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import");
        assertFalse(result.contains("import " + ImportConstants.Java.ARRAY_LIST + ";"),
                "ArrayList should not be imported in INTERFACES_ONLY mode");
        assertFalse(result.contains("import " + ImportConstants.Java.HASH_SET + ";"),
                "HashSet should not be imported for JSON collections in INTERFACES_ONLY mode");
    }

    @Test
    @DisplayName("getTestBaseImport: should return empty string when no matching field types and no list relations")
    void getTestBaseImport_noTypesNoLists_returnsEmptyString() {
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getTestBaseImport(model);

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("getTestBaseImport: should add BigDecimal, LocalDateTime, UUID and List imports")
    void getTestBaseImport_addsImportsBasedOnFieldUtils() {
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getTestBaseImport(model);

            assertTrue(result.contains("import " + ImportConstants.Java.BIG_DECIMAL));
            assertTrue(result.contains("import " + ImportConstants.Java.LOCAL_DATE_TIME));
            assertTrue(result.contains("import " + ImportConstants.Java.UUID));
            assertTrue(result.contains("import " + ImportConstants.Java.LIST));
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: collection relations should import Collectors")
    void getTestBaseImport_collectionRelations_importCollectors() {
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getTestBaseImport(model);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"));
            assertTrue(result.contains("import " + ImportConstants.Java.COLLECTORS + ";"));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: bulk create should import List even without list relations")
    void getTestBaseImport_bulkCreateEnabled_importsListWithoutListRelations() {
        final ModelDefinition model = emptyModel();
        Mockito.when(model.isBulkCreateEnabled()).thenReturn(true);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getTestBaseImport(model);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"));
            assertFalse(result.contains("import " + ImportConstants.Java.COLLECTORS + ";"));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: should not add List import when there are no list relations")
    void getTestBaseImport_noListRelations_noListImport() {
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);

            final String result = BusinessServiceImports.getTestBaseImport(model);

            assertFalse(result.contains("import " + ImportConstants.Java.LIST));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: unique relation collections should import Set but not HashSet")
    void getTestBaseImport_uniqueRelationCollections_importsSetOnly() {

        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(true);

            final String result = BusinessServiceImports.getTestBaseImport(model);

            assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import");
            assertTrue(result.contains("import " + ImportConstants.Java.COLLECTORS + ";"), "Expected Collectors import");
            assertFalse(result.contains("import " + ImportConstants.Java.HASH_SET + ";"),
                    "HashSet should not be imported in test base imports");
            assertFalse(result.contains("import " + ImportConstants.Java.LIST + ";"),
                    "List should not be imported when only unique relation collections exist");
        }
    }

    @Test
    @DisplayName("getTestBaseImport: should import List/Set when model has simple element collections (List<String>, Set<UUID>)")
    void getTestBaseImport_shouldImportListAndSet_forSimpleElementCollections() {

        final ModelDefinition model = emptyModel();
        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);
                importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                        .thenAnswer(inv -> {
                                @SuppressWarnings("unchecked")
                                final Set<String> imports = (Set<String>) inv.getArgument(1);
                                imports.add(ImportConstants.Java.LIST);
                                imports.add(ImportConstants.Java.SET);
                                return null;
                        });
                final String result = BusinessServiceImports.getTestBaseImport(model);
                assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import for element collections");
                assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import for element collections");
                assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getTestBaseImport: should import List and Set for JSON collection fields")
    void getTestBaseImport_shouldImportListAndSet_forJsonCollectionFields() {

        final ModelDefinition model = modelWithJsonCollectionFields();

        final String result = BusinessServiceImports.getTestBaseImport(model);

        assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import");
        assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import");
        assertFalse(result.contains("import " + ImportConstants.Java.ARRAY_LIST + ";"),
                "ArrayList should not be imported in INTERFACES_ONLY mode");
        assertFalse(result.contains("import " + ImportConstants.Java.HASH_SET + ";"),
                "HashSet should not be imported in getTestBaseImport");
    }

    @Test
    @DisplayName("getBaseImport: should not add List twice when element collections already added LIST and hasLists/importList also true")
    void getBaseImport_shouldNotDuplicateListImport_whenAlreadyAddedByImportCommon() {
        final ModelDefinition model = emptyModel();
        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class);
             final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {
                
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);
            
            importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                    .thenAnswer(inv -> {
                            @SuppressWarnings("unchecked")
                            final Set<String> imports = (Set<String>) inv.getArgument(1);
                            imports.add(ImportConstants.Java.LIST);
                            return null;
                    });

            final String result = BusinessServiceImports.getBaseImport(model, false);
            final String needle = "import " + ImportConstants.Java.LIST + ";";
            final int first = result.indexOf(needle);
            final int last = result.lastIndexOf(needle);

            assertTrue(first >= 0, "Expected List import");
            assertEquals(first, last, "List import should not be duplicated");
        }
    }

    @Test
    @DisplayName("getTestBaseImport: should not add List twice when element collections already added LIST and hasLists true")
    void getTestBaseImport_shouldNotDuplicateListImport_whenAlreadyAddedByImportCommon() {
        final ModelDefinition model = emptyModel();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {
                
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionList(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationCollectionSet(anyList())).thenReturn(false);
            importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                    .thenAnswer(inv -> {
                            @SuppressWarnings("unchecked")
                            final Set<String> imports = (Set<String>) inv.getArgument(1);
                            imports.add(ImportConstants.Java.LIST);
                            return null;
                    });
            final String result = BusinessServiceImports.getTestBaseImport(model);
            final String needle = "import " + ImportConstants.Java.LIST + ";";
            final int first = result.indexOf(needle);
            final int last = result.lastIndexOf(needle);
            assertTrue(first >= 0, "Expected List import");
            assertEquals(first, last, "List import should not be duplicated");
        }
    }

    @Test
    @DisplayName("computeModelsEnumsAndServiceImports: basic imports for model and service without relations and retryable annotation")
    void computeModelsEnumsAndServiceImports_basicWithoutRelations() {

        final ModelDefinition model = new ModelDefinition();
        model.setName("User");
        model.setFields(Collections.emptyList());

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String outputDir = "/some/output/dir";

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fields = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelImports> modelImports = Mockito.mockStatic(ModelImports.class);
             final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.example");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.example", packageConfiguration)).thenReturn("com.example.model");
            pkg.when(() -> PackageUtils.computeServicePackage("com.example", packageConfiguration))
                    .thenReturn("com.example.service");
            pkg.when(() -> PackageUtils.computeUtilsPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.utils");

            pkg.when(() -> PackageUtils.join("com.example.model", "User"))
                    .thenReturn("com.example.model.User");
            pkg.when(() -> PackageUtils.join("com.example.service", "UserService"))
                    .thenReturn("com.example.service.UserService");
            pkg.when(() -> PackageUtils.join("com.example.utils", "ArgumentVerifier"))
                    .thenReturn("com.example.utils.ArgumentVerifier");

            names.when(() -> ModelNameUtils.stripSuffix("User"))
                    .thenReturn("User");

            fields.when(() -> FieldUtils.extractRelationFields(anyList()))
                    .thenReturn(Collections.emptyList());

            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, packageConfiguration))
                    .thenReturn("import com.example.common.UserEnums;\n");

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(false);

            final String result = BusinessServiceImports.computeModelsEnumsAndServiceImports(
                    model,
                    outputDir,
                    BusinessServiceImports.BusinessServiceImportScope.BUSINESS_SERVICE_TEST,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.example.common.UserEnums;"), "Enums import");
            assertTrue(result.contains("import com.example.model.User;"), "Entity import");
            assertTrue(result.contains("import com.example.service.UserService;"), "Service import");
            assertTrue(result.contains("import com.example.utils.ArgumentVerifier;"), "ArgumentVerifier import");
            assertFalse(result.contains(GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY),
                    "Retry annotation should not be imported");
        }
    }

    @Test
    @DisplayName("computeModelsEnumsAndServiceImports: imports for relation models/services + retryable annotation when generated and scope=BUSINESS_SERVICE")
    void computeModelsEnumsAndServiceImports_withRelationsAndRetryable() {

        final ModelDefinition model = new ModelDefinition();
        model.setName("Order");

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("Customer");

        model.setFields(List.of(relationField));

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String outputDir = "/another/output/dir";

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> names = Mockito.mockStatic(ModelNameUtils.class);
             final MockedStatic<FieldUtils> fields = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<ModelImports> modelImports = Mockito.mockStatic(ModelImports.class);
             final MockedStatic<GeneratorContext> genContext = Mockito.mockStatic(GeneratorContext.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.shop");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.entity");
            pkg.when(() -> PackageUtils.computeServicePackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.service");
            pkg.when(() -> PackageUtils.computeUtilsPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.utils");
            pkg.when(() -> PackageUtils.computeAnnotationPackage("com.shop", packageConfiguration))
                    .thenReturn("com.shop.annotation");

            pkg.when(() -> PackageUtils.join("com.shop.entity", "Order"))
                    .thenReturn("com.shop.entity.Order");
            pkg.when(() -> PackageUtils.join("com.shop.service", "OrderService"))
                    .thenReturn("com.shop.service.OrderService");

            pkg.when(() -> PackageUtils.join("com.shop.entity", "Customer"))
                    .thenReturn("com.shop.entity.Customer");
            pkg.when(() -> PackageUtils.join("com.shop.service", "CustomerService"))
                    .thenReturn("com.shop.service.CustomerService");
            pkg.when(() -> PackageUtils.join("com.shop.utils", "ArgumentVerifier"))
                    .thenReturn("com.shop.utils.ArgumentVerifier");

            pkg.when(() -> PackageUtils.join("com.shop.annotation", GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY))
                    .thenReturn("com.shop.annotation." + GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY);

            names.when(() -> ModelNameUtils.stripSuffix("Order"))
                    .thenReturn("Order");
            names.when(() -> ModelNameUtils.stripSuffix("Customer"))
                    .thenReturn("Customer");

            fields.when(() -> FieldUtils.extractRelationFields(anyList()))
                    .thenReturn(List.of(relationField));

            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, packageConfiguration))
                    .thenReturn("import com.shop.common.OrderEnums;\n");

            genContext.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(true);

            final String result = BusinessServiceImports.computeModelsEnumsAndServiceImports(
                    model,
                    outputDir,
                    BusinessServiceImports.BusinessServiceImportScope.BUSINESS_SERVICE,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.shop.common.OrderEnums;"), "Enums import");
            assertTrue(result.contains("import com.shop.entity.Order;"), "Order entity import");
            assertTrue(result.contains("import com.shop.service.OrderService;"), "OrderService import");
            assertTrue(result.contains("import com.shop.entity.Customer;"), "Customer entity import");
            assertTrue(result.contains("import com.shop.service.CustomerService;"), "CustomerService import");
            assertTrue(result.contains("import com.shop.utils.ArgumentVerifier;"), "ArgumentVerifier import");
            assertTrue(result.contains("import com.shop.annotation." + GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY + ";"),
                    "Retryable annotation import");
        }
    }

    @Test
    @DisplayName("computeTestBusinessServiceImports: Instancio enabled + Spring Boot 4 → includes Instancio and @MockitoBean")
    void computeTestBusinessServiceImports_withInstancio_springBoot4() {

        final String result = BusinessServiceImports.computeTestBusinessServiceImports(true, false);

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO), "Instancio import expected");
        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.BEFORE_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST));
        assertTrue(result.contains("import " + ImportConstants.JUnit.EXTEND_WITH));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN));
        assertFalse(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.SPRING_EXTENSION));
    }

    @Test
    @DisplayName("computeTestBusinessServiceImports: Instancio disabled + Spring Boot 4 → no Instancio, uses @MockitoBean")
    void computeTestBusinessServiceImports_withoutInstancio_springBoot4() {

        final String result = BusinessServiceImports.computeTestBusinessServiceImports(false, false);

        assertFalse(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO),
                "Instancio import should not be present");
        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.BEFORE_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST));
        assertTrue(result.contains("import " + ImportConstants.JUnit.EXTEND_WITH));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN));
        assertFalse(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.SPRING_EXTENSION));
    }

    @Test
    @DisplayName("computeTestBusinessServiceImports: Instancio enabled + Spring Boot 3 → includes Instancio and @MockBean")
    void computeTestBusinessServiceImports_withInstancio_springBoot3() {

        final String result = BusinessServiceImports.computeTestBusinessServiceImports(true, true);

        assertTrue(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO), "Instancio import expected");
        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.BEFORE_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST));
        assertTrue(result.contains("import " + ImportConstants.JUnit.EXTEND_WITH));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN));
        assertFalse(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.SPRING_EXTENSION));
    }

    @Test
    @DisplayName("computeTestBusinessServiceImports: Instancio disabled + Spring Boot 3 → no Instancio, uses @MockBean")
    void computeTestBusinessServiceImports_withoutInstancio_springBoot3() {

        final String result = BusinessServiceImports.computeTestBusinessServiceImports(false, true);

        assertFalse(result.contains("import " + ImportConstants.INSTANCIO.INSTANCIO),
                "Instancio import should not be present");
        assertTrue(result.contains("import " + ImportConstants.JUnit.AFTER_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.BEFORE_EACH));
        assertTrue(result.contains("import " + ImportConstants.JUnit.TEST));
        assertTrue(result.contains("import " + ImportConstants.JUnit.EXTEND_WITH));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.MOCK_BEAN));
        assertFalse(result.contains("import " + ImportConstants.SpringTest.MOCKITO_BEAN));
        assertTrue(result.contains("import " + ImportConstants.SpringTest.SPRING_EXTENSION));
    }
    
}
