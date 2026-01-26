package dev.markozivkovic.springcrudgenerator.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.generators.TransferObjectGenerator.TransferObjectTarget;
import dev.markozivkovic.springcrudgenerator.generators.TransferObjectGenerator.TransferObjectType;
import dev.markozivkovic.springcrudgenerator.imports.common.ImportCommon;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class TransferObjectImportsTest {

    @Test
    @DisplayName("getBaseImport(model): no special types and no list relations → empty string")
    void getBaseImport_simple_noTypesNoLists() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = TransferObjectImports.getBaseImport(model);

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("getBaseImport(model): BigDecimal + UUID + list relations → BigDecimal, UUID i List importi")
    void getBaseImport_simple_withTypesAndLists() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = TransferObjectImports.getBaseImport(model);

            assertTrue(result.contains("import " + ImportConstants.Java.BIG_DECIMAL), "Expected BigDecimal import");
            assertTrue(result.contains("import " + ImportConstants.Java.UUID), "Expected UUID import");
            assertTrue(result.contains("import " + ImportConstants.Java.LIST), "Expected List import");
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport(model): should include imports added by ImportCommon.importListAndSetForSimpleCollection")
    void getBaseImport_simple_shouldIncludeSimpleCollectionImports() {

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {

                fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

                importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                        .thenAnswer(inv -> {
                                final boolean cond = inv.getArgument(0);
                                final Set<String> set = inv.getArgument(1);
                                final String value = inv.getArgument(2);
                                if (cond) set.add(value);
                                return null;
                        });

                importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                        .thenAnswer(inv -> {
                                final Set<String> set = inv.getArgument(1);
                                set.add(ImportConstants.Java.SET);
                                return null;
                        });

                final String result = TransferObjectImports.getBaseImport(model);

                assertTrue(result.contains("import " + ImportConstants.Java.SET + ";"),
                        "Expected SET import from ImportCommon.importListAndSetForSimpleCollection");
                assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport(model, entities, BASE): samo liste kada postoje relacije 1-N/N-M")
    void getBaseImport_withEntities_baseType_listsOnly() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = TransferObjectImports.getBaseImport(
                    model,
                    Collections.emptyList(),
                    TransferObjectType.BASE
            );

            assertTrue(result.contains("import " + ImportConstants.Java.LIST), "Expected List import");
            assertFalse(result.contains(ImportConstants.Java.UUID), "UUID import should not be present");
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport(model, entities, CREATE): UUID dolazi iz related entiteta sa UUID id poljem")
    void getBaseImport_createType_uuidFromRelatedEntityId() {
        
        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("User");
        relationField.setRelation(new RelationDefinition());

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(relationField));

        final ModelDefinition userEntity = new ModelDefinition();
        userEntity.setName("User");
        final FieldDefinition userIdField = new FieldDefinition();
        userEntity.setFields(List.of(userIdField));

        final List<ModelDefinition> entities = List.of(userEntity);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(userEntity.getFields()))
                    .thenReturn(userIdField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(userIdField))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = TransferObjectImports.getBaseImport(
                    model,
                    entities,
                    TransferObjectType.CREATE
            );

            assertTrue(result.contains("import " + ImportConstants.Java.UUID),
                    "UUID import expected from related entity id field");
            assertFalse(result.contains("import " + ImportConstants.Java.LIST));
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("getBaseImport(model, entities, INPUT): baca IllegalArgumentException kada related entity ne postoji")
    void getBaseImport_inputType_missingRelatedEntity_throws() {

        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("MissingEntity");
        relationField.setRelation(new RelationDefinition());

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(relationField));

        final List<ModelDefinition> entities = Collections.emptyList();

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> TransferObjectImports.getBaseImport(
                            model,
                            entities,
                            TransferObjectType.INPUT
                    )
            );

            assertTrue(ex.getMessage().contains("Related entity not found: MissingEntity"));
        }
    }

    @Test
    @DisplayName("getBaseImport(model, entities, INPUT): related entity postoji, id nije UUID → nema UUID importa ni LIST importa")
    void getBaseImport_inputType_relatedEntityNonUuidId_noUuidNoList() {
        
        final FieldDefinition relationField = new FieldDefinition();
        relationField.setType("User");
        relationField.setRelation(new RelationDefinition());

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(relationField));

        final ModelDefinition userEntity = new ModelDefinition();
        userEntity.setName("User");
        final FieldDefinition userIdField = new FieldDefinition();
        userEntity.setFields(List.of(userIdField));

        final List<ModelDefinition> entities = List.of(userEntity);

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.extractIdField(userEntity.getFields()))
                    .thenReturn(userIdField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(userIdField))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

            final String result = TransferObjectImports.getBaseImport(
                    model,
                    entities,
                    TransferObjectType.INPUT
            );

            assertFalse(result.contains("import " + ImportConstants.Java.UUID),
                    "UUID import should not be present");
            assertFalse(result.contains("import " + ImportConstants.Java.LIST),
                    "List import should not be present for INPUT even if relations exist");
        }
    }

    @Test
    @DisplayName("getBaseImport(model, entities, type): should include imports added by ImportCommon.importListAndSetForSimpleCollection")
    void getBaseImport_withEntities_shouldIncludeSimpleCollectionImports() {

        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        final List<ModelDefinition> entities = Collections.emptyList();
        final TransferObjectType type = TransferObjectType.INPUT;

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
                final MockedStatic<ImportCommon> importCommon = Mockito.mockStatic(ImportCommon.class)) {

                fieldUtils.when(() -> FieldUtils.isAnyFieldBigDecimal(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldBigInteger(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDate(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldLocalDateTime(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldUUID(anyList())).thenReturn(false);

                fieldUtils.when(() -> FieldUtils.isAnyRelationOneToMany(anyList())).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyRelationManyToMany(anyList())).thenReturn(false);

                importCommon.when(() -> ImportCommon.addIf(anyBoolean(), anySet(), anyString()))
                        .thenAnswer(inv -> {
                                final boolean cond = inv.getArgument(0);
                                final Set<String> set = inv.getArgument(1);
                                final String value = inv.getArgument(2);
                                if (cond) set.add(value);
                                return null;
                        });

                importCommon.when(() -> ImportCommon.importListAndSetForSimpleCollection(eq(model), anySet()))
                        .thenAnswer(inv -> {
                                final Set<String> set = inv.getArgument(1);
                                set.add(ImportConstants.Java.SET);
                                return null;
                        });

                final String result = TransferObjectImports.getBaseImport(model, entities, type);

                assertTrue(
                        result.contains("import " + ImportConstants.Java.SET + ";"),
                        "Expected SET import from ImportCommon.importListAndSetForSimpleCollection"
                );
                assertTrue(result.endsWith("\n"));
        }
        }

    @Test
    @DisplayName("computeValidationImport: no non-null and no size validations → empty string")
    void computeValidationImport_noConstraints_returnsEmpty() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldNonNullable(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasAnyFieldLengthValidation(anyList())).thenReturn(false);

            final String result = TransferObjectImports.computeValidationImport(model);

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("computeValidationImport: only @NotNull → imports jakarta.validation.constraints.NotNull")
    void computeValidationImport_onlyNotNull() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldNonNullable(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.hasAnyFieldLengthValidation(anyList())).thenReturn(false);

            final String result = TransferObjectImports.computeValidationImport(model);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.NOT_NULL),
                    "Expected @NotNull import");
            assertFalse(result.contains(ImportConstants.Jakarta.SIZE),
                    "SIZE import should not be present");
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("computeValidationImport: only @Size → imports jakarta.validation.constraints.Size")
    void computeValidationImport_onlySize() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldNonNullable(anyList())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasAnyFieldLengthValidation(anyList())).thenReturn(true);

            final String result = TransferObjectImports.computeValidationImport(model);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.SIZE),
                    "Expected @Size import");
            assertFalse(result.contains(ImportConstants.Jakarta.NOT_NULL),
                    "NotNull import should not be present");
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("computeValidationImport: both @NotNull and @Size → imports both")
    void computeValidationImport_notNullAndSize() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class)) {
            fieldUtils.when(() -> FieldUtils.isAnyFieldNonNullable(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.hasAnyFieldLengthValidation(anyList())).thenReturn(true);

            final String result = TransferObjectImports.computeValidationImport(model);

            assertTrue(result.contains("import " + ImportConstants.Jakarta.NOT_NULL));
            assertTrue(result.contains("import " + ImportConstants.Jakarta.SIZE));
            assertTrue(result.endsWith("\n"));
        }
    }

    @Test
    @DisplayName("computeEnumsAndHelperEntitiesImport: no enums and no JSON fields → empty string")
    void computeEnumsAndHelperEntitiesImport_noEnumsNoJson_returnsEmpty() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(Collections.emptyList());

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String outputDir = "/some/output/dir";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(model.getFields())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            final String result = TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                    model,
                    outputDir,
                    true,
                    TransferObjectTarget.REST,
                    packageConfiguration
            );

            assertEquals("", result);
        }
    }

    @Test
    @DisplayName("computeEnumsAndHelperEntitiesImport: only enums, no JSON helper imports")
    void computeEnumsAndHelperEntitiesImport_enumsOnly() {
        
        final ModelDefinition model = new ModelDefinition();
        model.setFields(List.of(new FieldDefinition()));

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String outputDir = "/some/output/dir";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            packageUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example");

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(false);

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.example", packageConfiguration))
                    .thenReturn(Set.of("import com.example.enums.StatusEnum;"));

            final String result = TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                    model,
                    outputDir,
                    false,
                    TransferObjectTarget.REST,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.example.enums.StatusEnum;"),
                    "Enum import expected");
            assertFalse(result.contains("TO;"), "No helper TO imports expected");
        }
    }

    @Test
    @DisplayName("computeEnumsAndHelperEntitiesImport: enums + JSON fields (GRAPHQL target) → enum imports + helper TO imports")
    void computeEnumsAndHelperEntitiesImport_enumsAndJson_graphqlTarget() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition jsonField = new FieldDefinition();
        model.setFields(List.of(jsonField));

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String outputDir = "/output/dir";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            packageUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example");

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(true);

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.example", packageConfiguration))
                    .thenReturn(Set.of("import com.example.enums.StatusEnum;"));
            fieldUtils.when(() -> FieldUtils.extractJsonFields(model.getFields()))
                    .thenReturn(List.of(jsonField));

            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("Address");
            packageUtils.when(() -> PackageUtils.computeHelperGraphqlTransferObjectPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.graphql.helper");
            packageUtils.when(() -> PackageUtils.join("com.example.graphql.helper", "AddressTO"))
                    .thenReturn("com.example.graphql.helper.AddressTO");

            final String result = TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                    model,
                    outputDir,
                    true,
                    TransferObjectTarget.GRAPHQL,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.example.enums.StatusEnum;"), "Enum import missing");
            assertTrue(result.contains("import com.example.graphql.helper.AddressTO;"),
                    "GraphQL helper TO import missing");
        }
    }

    @Test
    @DisplayName("computeEnumsAndHelperEntitiesImport: enums + JSON fields (REST target) → enum imports + REST TO imports")
    void computeEnumsAndHelperEntitiesImport_enumsAndJson_restTarget() {
        
        final ModelDefinition model = new ModelDefinition();
        final FieldDefinition jsonField = new FieldDefinition();
        model.setFields(List.of(jsonField));

        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final String outputDir = "/output/rest";

        try (final MockedStatic<FieldUtils> fieldUtils = Mockito.mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> packageUtils = Mockito.mockStatic(PackageUtils.class);
             final MockedStatic<EnumImports> enumImports = Mockito.mockStatic(EnumImports.class)) {

            packageUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example.rest");

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(model.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldJson(model.getFields())).thenReturn(true);

            enumImports.when(() -> EnumImports.computeEnumImports(model, "com.example.rest", packageConfiguration))
                    .thenReturn(Set.of("import com.example.rest.enums.StatusEnum;"));

            fieldUtils.when(() -> FieldUtils.extractJsonFields(model.getFields()))
                    .thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("MetaData");

            packageUtils.when(() -> PackageUtils.computeHelperRestTransferObjectPackage("com.example.rest", packageConfiguration))
                    .thenReturn("com.example.rest.helper");
            packageUtils.when(() -> PackageUtils.join("com.example.rest.helper", "MetaDataTO"))
                    .thenReturn("com.example.rest.helper.MetaDataTO");

            final String result = TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                    model,
                    outputDir,
                    true,
                    TransferObjectTarget.REST,
                    packageConfiguration
            );

            assertTrue(result.contains("import com.example.rest.enums.StatusEnum;"));
            assertTrue(result.contains("import com.example.rest.helper.MetaDataTO;"));
        }
    }

}
