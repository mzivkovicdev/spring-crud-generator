package dev.markozivkovic.codegen.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.codegen.constants.ImportConstants;
import dev.markozivkovic.codegen.constants.RelationTypesConstants;
import dev.markozivkovic.codegen.enums.SpecialType;
import dev.markozivkovic.codegen.models.AuditDefinition;
import dev.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.codegen.models.ColumnDefinition;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.IdDefinition;
import dev.markozivkovic.codegen.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.AuditUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;

class ModelImportsTest {

    @Test
    @DisplayName("getBaseImport: no special types, no Objects, no auditing → empty string")
    void getBaseImport_noTypes_noObjects_noAuditing() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());
        model.setAudit(null);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = ModelImports.getBaseImport(model, false, false);

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("getBaseImport: no types, no simple collections, no Objects, no auditing -> empty string")
    void getBaseImport_noTypes_noSimpleCollections_noObjects_noAuditing() {

        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());
        model.setAudit(null);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
            final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldSimpleCollection(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = ModelImports.getBaseImport(model, false, false);

            assertEquals("", result);
            auditUtils.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("getBaseImport: when model has simple collections, should import List/ArrayList and Set/HashSet (no duplicates)")
    void getBaseImport_withSimpleCollections_importsListAndSetAndImpls_noDuplicates() {

        final FieldDefinition listField = mock(FieldDefinition.class);
        when(listField.getType()).thenReturn("List<String>");

        final FieldDefinition setField = mock(FieldDefinition.class);
        when(setField.getType()).thenReturn("Set<UUID>");

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(listField, setField));
        model.setAudit(null);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
            final MockedStatic<SpecialType> specialType = Mockito.mockStatic(SpecialType.class);
            final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldSimpleCollection(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractSimpleCollectionFields(anyList()))
                    .thenReturn(List.of(listField, setField));

            specialType.when(() -> SpecialType.isListType("List<String>")).thenReturn(true);
            specialType.when(() -> SpecialType.isSetType("List<String>")).thenReturn(false);
            specialType.when(() -> SpecialType.isListType("Set<UUID>")).thenReturn(false);
            specialType.when(() -> SpecialType.isSetType("Set<UUID>")).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = ModelImports.getBaseImport(model, false, false);

            assertTrue(result.contains("import " + ImportConstants.Java.LIST + ";"), "Expected List import");
            assertTrue(result.contains("import " + ImportConstants.Java.ARRAY_LIST + ";"), "Expected ArrayList import");
            assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"), "Expected Set import");
            assertTrue(result.contains("import " + ImportConstants.Java.HASH_SET + ";"), "Expected HashSet import");

            final String listNeedle = "import " + ImportConstants.Java.LIST + ";";
            assertEquals(result.indexOf(listNeedle), result.lastIndexOf(listNeedle), "List import should not be duplicated");

            assertTrue(result.endsWith("\n"));

            auditUtils.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("getBaseImport: BigDecimal + UUID + List + Objects + auditing enabled → all relevant imports present")
    void getBaseImport_withTypes_objects_and_auditing() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(model.getAudit()).thenReturn(new AuditDefinition(true, AuditTypeEnum.INSTANT));
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldSimpleCollection(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            auditUtils.when(() -> AuditUtils.resolveAuditingImport(AuditTypeEnum.INSTANT))
                    .thenReturn("java.time.Instant");

            final String result = ModelImports.getBaseImport(
                    model,
                    true,
                    true
            );

            assertTrue(result.contains("import " + ImportConstants.Java.BIG_DECIMAL), "BigDecimal import missing");
            assertTrue(result.contains("import " + ImportConstants.Java.UUID), "UUID import missing");
            assertTrue(result.contains("import " + ImportConstants.Java.OBJECTS), "Objects import missing");
            assertTrue(result.contains("import " + ImportConstants.Java.LIST), "List import missing");
            assertTrue(result.contains("import java.time.Instant;"), "Auditing import missing");
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport: auditing enabled on model, but importAuditing=false → no auditing imports")
    void getBaseImport_auditEnabled_butFlagFalse_noAuditingImport() {
        
        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(model.getFields()).thenReturn(Collections.emptyList());
        Mockito.when(model.getAudit().isEnabled()).thenReturn(true);
        Mockito.when(model.getAudit().getType()).thenReturn(AuditTypeEnum.INSTANT);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<AuditUtils> auditUtils = Mockito.mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT))
                    .thenReturn("java.time.Instant");

            final String result = ModelImports.getBaseImport(
                    model,
                    false,
                    false
            );

            assertFalse(result.contains("java.time.Instant"), "Auditing import should not be present");
        }
    }

    @Test
    @DisplayName("computeJakartaImports: no enums, no relations, no json, no auditing, optimisticLocking=false → only base JPA imports")
    void computeJakartaImports_minimal() {
        final ModelDefinition model = Mockito.mock(ModelDefinition.class);
        final List<FieldDefinition> fields = Collections.emptyList();
        Mockito.when(model.getFields()).thenReturn(fields);
        Mockito.when(model.getAudit()).thenReturn(null);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(new FieldDefinition().setId(new IdDefinition()));
            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFetchTypeDefined(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isCascadeTypeDefined(fields)).thenReturn(false);

            final String result = ModelImports.computeJakartaImports(model, false);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENTITY));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.GENERATED_VALUE));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.GENERATION_TYPE));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ID));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.TABLE));
            assertFalse(result.contains(ImportConstants.Jakarta.ENUM_TYPE));
            assertFalse(result.contains(ImportConstants.Jakarta.ENUMERATED));
            assertFalse(result.contains(ImportConstants.Jakarta.VERSION));
            assertFalse(result.contains(ImportConstants.Jakarta.COLUMN));
            assertFalse(result.contains(ImportConstants.Jakarta.ENTITY_LISTENERS));
            assertFalse(result.contains(ImportConstants.SpringData.AUDITING_ENTITY_LISTENER));
            assertFalse(result.contains(ImportConstants.HibernateAnnotation.JDBC_TYPE_CODE));
            assertFalse(result.contains(ImportConstants.Jakarta.SEQUENCE_GENERATOR));
            assertFalse(result.contains(ImportConstants.Jakarta.TABLE_GENERATOR));
        }
    }

    @Test
    @DisplayName("computeJakartaImports: enums + relations + optimisticLocking + auditing (no JSON) → JPA + SpringData auditing imports")
    void computeJakartaImports_withEnums_relations_auditing_noJson() {
        
        final FieldDefinition field = new FieldDefinition();
        field.setColumn(new ColumnDefinition());

        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        final List<FieldDefinition> fields = List.of(field);
        Mockito.when(model.getFields()).thenReturn(fields);
        Mockito.when(model.getAudit().isEnabled()).thenReturn(true);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                    .thenReturn(List.of(RelationTypesConstants.MANY_TO_ONE));
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(new FieldDefinition().setId(new IdDefinition()));
            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(fields)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToOne(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFetchTypeDefined(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isCascadeTypeDefined(fields)).thenReturn(true);

            final String result = ModelImports.computeJakartaImports(model, true);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENUM_TYPE));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENUMERATED));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.JOIN_COLUMN));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.MANY_TO_ONE));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.FETCH_TYPE));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.CASCADE_TYPE));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.VERSION));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.COLUMN));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENTITY_LISTENERS));
            assertTrue(result.contains("import " + ImportConstants.SpringData.AUDITING_ENTITY_LISTENER));
            assertTrue(result.contains("import " + ImportConstants.SpringData.CREATED_DATE));
            assertTrue(result.contains("import " + ImportConstants.SpringData.LAST_MODIFIED_DATE));
            assertFalse(result.contains(ImportConstants.HibernateAnnotation.JDBC_TYPE_CODE));
            assertFalse(result.contains(ImportConstants.HibernateAnnotation.SQL_TYPES));
            assertFalse(result.contains(ImportConstants.Jakarta.SEQUENCE_GENERATOR));
            assertFalse(result.contains(ImportConstants.Jakarta.TABLE_GENERATOR));
        }
    }

    @Test
    @DisplayName("computeJakartaImports: auditing + JSON fields → JPA + Hibernate JSON imports + SpringData auditing imports")
    void computeJakartaImports_withJsonAndAuditing() {
        
        final FieldDefinition field = new FieldDefinition();
        field.setColumn(new ColumnDefinition());

        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        final List<FieldDefinition> fields = List.of(field);
        Mockito.when(model.getFields()).thenReturn(fields);
        Mockito.when(model.getAudit().isEnabled()).thenReturn(true);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(new FieldDefinition().setId(new IdDefinition()));
            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFetchTypeDefined(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isCascadeTypeDefined(fields)).thenReturn(false);

            final String result = ModelImports.computeJakartaImports(model, false);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENTITY));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.COLUMN));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENTITY_LISTENERS));
            assertTrue(result.contains("import " + ImportConstants.HibernateAnnotation.JDBC_TYPE_CODE));
            assertTrue(result.contains("import " + ImportConstants.HibernateAnnotation.SQL_TYPES));
            assertTrue(result.contains("import " + ImportConstants.SpringData.AUDITING_ENTITY_LISTENER));
            assertTrue(result.contains("import " + ImportConstants.SpringData.CREATED_DATE));
            assertTrue(result.contains("import " + ImportConstants.SpringData.LAST_MODIFIED_DATE));
            assertFalse(result.contains(ImportConstants.Jakarta.SEQUENCE_GENERATOR));
            assertFalse(result.contains(ImportConstants.Jakarta.TABLE_GENERATOR));
        }
    }

    @Test
    @DisplayName("computeJakartaImports: TABLE id strategy → adds @TableGenerator import, no @SequenceGenerator")
    void computeJakartaImports_withTableIdStrategy() {

        final IdDefinition idDef = new IdDefinition();
        idDef.setStrategy(IdStrategyEnum.TABLE);

        final FieldDefinition idField = new FieldDefinition();
        idField.setId(idDef);

        final List<FieldDefinition> fields = List.of(idField);

        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(model.getFields()).thenReturn(fields);
        Mockito.when(model.getAudit()).thenReturn(null);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields)).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFetchTypeDefined(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isCascadeTypeDefined(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldSimpleCollection(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);

            final String result = ModelImports.computeJakartaImports(model, false);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.TABLE_GENERATOR));
            assertFalse(result.contains("import " + ImportConstants.Jakarta.SEQUENCE_GENERATOR));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENTITY));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ID));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.GENERATED_VALUE));
        }
    }

    @Test
    @DisplayName("computeJakartaImports: SEQUENCE id strategy → adds @SequenceGenerator import, no @TableGenerator")
    void computeJakartaImports_withSequenceIdStrategy() {

        final IdDefinition idDef = new IdDefinition();
        idDef.setStrategy(IdStrategyEnum.SEQUENCE);

        final FieldDefinition idField = new FieldDefinition();
        idField.setId(idDef);

        final List<FieldDefinition> fields = List.of(idField);

        final ModelDefinition model = Mockito.mock(ModelDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(model.getFields()).thenReturn(fields);
        Mockito.when(model.getAudit()).thenReturn(null);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(fields)).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToOne(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isFetchTypeDefined(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isCascadeTypeDefined(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldSimpleCollection(fields)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);

            final String result = ModelImports.computeJakartaImports(model, false);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.SEQUENCE_GENERATOR));
            assertFalse(result.contains("import " + ImportConstants.Jakarta.TABLE_GENERATOR));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ENTITY));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.ID));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.GENERATED_VALUE));
        }
    }

}
