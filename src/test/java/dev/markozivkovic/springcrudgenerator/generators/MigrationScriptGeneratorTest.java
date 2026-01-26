package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.migrations.MigrationDiffer;
import dev.markozivkovic.springcrudgenerator.migrations.MigrationManifestBuilder;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition.JoinTableDefinition;
import dev.markozivkovic.springcrudgenerator.models.flyway.DdlArtifactState.DdlArtifactType;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.Result;
import dev.markozivkovic.springcrudgenerator.models.flyway.EntityState;
import dev.markozivkovic.springcrudgenerator.models.flyway.FkState;
import dev.markozivkovic.springcrudgenerator.models.flyway.JoinState;
import dev.markozivkovic.springcrudgenerator.models.flyway.MigrationState;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FlywayUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;

class MigrationScriptGeneratorTest {

    private ModelDefinition newModel(final String name, final String storageName, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getStorageName()).thenReturn(storageName);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @BeforeEach
    void resetVersion() throws Exception {
        final Field f = MigrationScriptGenerator.class.getDeclaredField("version");
        f.setAccessible(true);
        f.set(null, 1);
    }

    @Test
    void generate_shouldSkipWhenMigrationScriptsDisabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> allEntities = List.of();

        when(cfg.isMigrationScripts()).thenReturn(false);

        final MigrationScriptGenerator generator =
                new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final ModelDefinition anyModel = mock(ModelDefinition.class);

        try (final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class)) {

            generator.generate(anyModel, "out");

            flyway.verifyNoInteractions();
            writer.verifyNoInteractions();
            ctx.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenAlreadyGeneratedInContext() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> allEntities = List.of();

        when(cfg.isMigrationScripts()).thenReturn(true);

        final MigrationScriptGenerator generator =
                new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final ModelDefinition anyModel = mock(ModelDefinition.class);

        try (final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT))
               .thenReturn(true);

            generator.generate(anyModel, "out");

            flyway.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateCreateTableScriptForNewModel() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.AUTO);
        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);
        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr = mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                    final MigrationState ms = mock(MigrationState.class);
                    when(ms.getEntities()).thenReturn(null);
                    when(ms.getDdlArtifacts()).thenReturn(null);
                    when(mock.build()).thenReturn(ms);
                    when(mock.hasDdlArtifactFileWithSuffixAndContent(any(), anyString(), anyString(), anyString())).thenReturn(false);
                })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)).thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(1);
            when(initialState.getEntities()).thenReturn(null);
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());
            flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL))).thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

            final Map<String, Object> seqCtx = new HashMap<>();
            seqCtx.put("name", "book_id_seq");
            flyway.when(() -> FlywayUtils.toSequenceGeneratorContext(eq(bookModel))).thenReturn(seqCtx);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-sequence-table-generator.sql.ftl"), eq(seqCtx)))
                    .thenReturn("CREATE SEQUENCE book_id_seq;");

            final Map<String, Object> createCtx = new HashMap<>();
            createCtx.put("tableName", "book");
            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createCtx);
            flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap())).thenReturn(null);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-table.sql.ftl"), eq(createCtx))).thenReturn("CREATE TABLE book (...);");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class))).thenAnswer(inv -> null);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(any(FieldDefinition.class)))
                    .thenThrow(new IllegalStateException("Should not be called in this test"));

            generator.generate(bookModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq(expectedScriptsPath), eq("V2__create_book_sequence.sql"), eq("CREATE SEQUENCE book_id_seq;")));
            writer.verify(() -> FileWriterUtils.writeToFile(eq(expectedScriptsPath), eq("V3__create_book_table.sql"), eq("CREATE TABLE book (...);")));

            verify(initialState).setLastScriptVersion(3);
            flyway.verify(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class)));
            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT));

        }
    }

    @Test
    void generate_shouldThrowWhenJsonModelNotFound() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final FieldDefinition jsonField = mock(FieldDefinition.class);

        final ModelDefinition userModel = newModel("UserEntity", "user", List.of(jsonField));
        final List<ModelDefinition> allEntities = List.of(userModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator =
                new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                     mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                         final MigrationState ms = mock(MigrationState.class);
                         when(ms.getEntities()).thenReturn(null);
                         when(ms.getDdlArtifacts()).thenReturn(null);
                         when(mock.build()).thenReturn(ms);
                         when(mock.hasDdlArtifactFileWithSuffixAndContent(any(DdlArtifactType.class), anyString(), anyString(), anyString())).thenReturn(false);
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)).thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(1);
            when(initialState.getEntities()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("AddressEntity");

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList())).thenReturn(true);

            assertThrows(NoSuchElementException.class, () -> generator.generate(userModel, "out"));

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateElementCollectionScript_whenNewElementCollectionTablesExist() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.AUTO);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator =
                new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        final Map<String, Object> ecTable = new LinkedHashMap<>();
        ecTable.put("tableName", "book_tags");
        ecTable.put("ownerTable", "book");
        ecTable.put("joinColumn", "book_id");
        ecTable.put("ownerPkColumn", "id");
        ecTable.put("ownerPkSqlType", "BIGINT");
        ecTable.put("valueColumn", "value");
        ecTable.put("valueSqlType", "VARCHAR(255)");
        ecTable.put("isList", true);
        ecTable.put("orderColumn", "order_index");
        ecTable.put("needsUnique", false);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
                final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
                final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
                final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
                final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
                final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
                final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                        mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                        final MigrationState ms = mock(MigrationState.class);
                        when(ms.getEntities()).thenReturn(null);
                        when(mock.build()).thenReturn(ms);
                        })) {

                ctx.when(() -> GeneratorContext.isGenerated(
                                GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT))
                        .thenReturn(false);

                final MigrationState initialState = mock(MigrationState.class);
                when(initialState.getLastScriptVersion()).thenReturn(1);
                when(initialState.getEntities()).thenReturn(null);

                flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project"))
                        .thenReturn(initialState);

                flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap()))
                        .thenReturn(Collections.emptyMap());

                fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
                fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

                final Map<String, Object> createCtx = new HashMap<>();
                createCtx.put("tableName", "book");
                createCtx.put("columns", Collections.emptyList());

                flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                        .thenReturn(createCtx);

                flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap()))
                        .thenReturn(null);

                flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL)))
                        .thenReturn(List.of(ecTable));

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                                eq("migration/flyway/create-table.sql.ftl"),
                                eq(createCtx)))
                        .thenReturn("CREATE TABLE book (...);");

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                                eq("migration/flyway/element-collection-create.ftl"),
                                argThat(m -> m != null && m.containsKey("collectionTables"))))
                        .thenReturn("CREATE TABLE book_tags (...);");

                modelNameUtils.when(() -> ModelNameUtils.stripSuffix("BookEntity")).thenReturn("Book");
                modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("Book")).thenReturn("book");
                final Map<String, Object> seqCtx = new HashMap<>();
                seqCtx.put("name", "book_id_seq");

                flyway.when(() -> FlywayUtils.toSequenceGeneratorContext(eq(bookModel))).thenReturn(seqCtx);
                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-sequence-table-generator.sql.ftl"), eq(seqCtx)))
                                .thenReturn("CREATE SEQUENCE book_id_seq;");

                modelNameUtils.when(() -> ModelNameUtils.stripSuffix("BookEntity")).thenReturn("Book");
                modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("Book")).thenReturn("book");

                flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class)))
                        .thenAnswer(inv -> null);

                generator.generate(bookModel, "out");

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath), eq("V2__create_book_sequence.sql"), eq("CREATE SEQUENCE book_id_seq;")
                ));

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath), eq("V3__create_book_table.sql"), eq("CREATE TABLE book (...);")
                ));

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath), eq("V4__create_book_element_collections.sql"), eq("CREATE TABLE book_tags (...);")
                ));

                ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT));

                final MigrationManifestBuilder manifest = manifestConstr.constructed().get(0);
                verify(manifest, atLeastOnce()).applyCreateContext(
                        eq("BookEntity"),
                        eq("book_tags"),
                        argThat(m -> m != null && "book_tags".equals(m.get("tableName")))
                );
        }
    }

    @Test
    void generate_shouldNotGenerateElementCollectionScript_whenElementCollectionTableAlreadyInMigrationState() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.AUTO);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator =
                new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final String expectedScriptsPath =
                "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        final Map<String, Object> ecTable = new LinkedHashMap<>();
        ecTable.put("tableName", "book_tags");
        ecTable.put("ownerTable", "book");
        ecTable.put("joinColumn", "book_id");
        ecTable.put("ownerPkColumn", "id");
        ecTable.put("ownerPkSqlType", "BIGINT");
        ecTable.put("valueColumn", "value");
        ecTable.put("valueSqlType", "VARCHAR(255)");
        ecTable.put("isList", true);
        ecTable.put("orderColumn", "order_index");
        ecTable.put("needsUnique", false);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
                final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
                final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
                final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
                final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
                final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                        mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                        final MigrationState ms = mock(MigrationState.class);

                        final EntityState ecState = mock(EntityState.class);
                        when(ecState.getTable()).thenReturn("book_tags");

                        when(ms.getEntities()).thenReturn(List.of(ecState));
                        when(mock.build()).thenReturn(ms);
                        when(mock.hasDdlArtifactFileWithSuffixAndContent(any(), anyString(), anyString(), anyString())).thenReturn(false);
                        })) {

                ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT))
                        .thenReturn(false);

                final MigrationState initialState = mock(MigrationState.class);
                when(initialState.getLastScriptVersion()).thenReturn(1);
                when(initialState.getEntities()).thenReturn(null);
                when(initialState.getDdlArtifacts()).thenReturn(null);

                flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project"))
                        .thenReturn(initialState);

                flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap()))
                        .thenReturn(Collections.emptyMap());

                fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
                fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

                final Map<String, Object> createCtx = new HashMap<>();
                createCtx.put("tableName", "book");
                createCtx.put("columns", Collections.emptyList());

                flyway.when(() -> FlywayUtils.toCreateTableContext(
                                eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean())
                        )
                        .thenReturn(createCtx);

                flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap()))
                        .thenReturn(null);

                flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL)))
                        .thenReturn(List.of(ecTable));

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                                eq("migration/flyway/create-table.sql.ftl"),
                                eq(createCtx)))
                        .thenReturn("CREATE TABLE book (...);");

                flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class)))
                        .thenAnswer(inv -> null);

                final Map<String, Object> seqCtx = new HashMap<>();
                seqCtx.put("name", "book_id_seq");
                flyway.when(() -> FlywayUtils.toSequenceGeneratorContext(eq(bookModel))).thenReturn(seqCtx);

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                        eq("migration/flyway/create-sequence-table-generator.sql.ftl"), eq(seqCtx)))
                .thenReturn("CREATE SEQUENCE book_id_seq;");

                generator.generate(bookModel, "out");

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath), eq("V2__create_book_sequence.sql"), eq("CREATE SEQUENCE book_id_seq;")
                ));

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath), eq("V3__create_book_table.sql"), eq("CREATE TABLE book (...);")
                ));

                tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                        eq("migration/flyway/element-collection-create.ftl"),
                        anyMap()
                ), never());

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath),
                        contains("_element_collections.sql"),
                        anyString()
                ), never());
        }
    }

    @Test
    void generate_shouldGenerateSequenceScript_whenModelHasSequenceId_andNotInManifest() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.SEQUENCE);

        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator =
                new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final String expectedScriptsPath =
                "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
                final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
                final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
                final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
                final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
                final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
                final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                        mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                        final MigrationState ms = mock(MigrationState.class);
                        when(ms.getEntities()).thenReturn(null);
                        when(mock.build()).thenReturn(ms);
                        when(mock.hasDdlArtifactFileWithSuffixAndContent(any(DdlArtifactType.class), anyString(), anyString(), anyString())).thenReturn(false);
                        })) {

                ctx.when(() -> GeneratorContext.isGenerated(
                                GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT))
                        .thenReturn(false);

                final MigrationState initialState = mock(MigrationState.class);
                when(initialState.getLastScriptVersion()).thenReturn(0);
                when(initialState.getEntities()).thenReturn(null);
                when(initialState.getDdlArtifacts()).thenReturn(null);

                flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
                flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());
                fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
                fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

                final Map<String, Object> seqCtx = new HashMap<>();
                seqCtx.put("name", "book_gen");
                seqCtx.put("initialValue", 1);
                seqCtx.put("allocationSize", 50);
                seqCtx.put("sequence", true);

                flyway.when(() -> FlywayUtils.toSequenceGeneratorContext(eq(bookModel))).thenReturn(seqCtx);

                final Map<String, Object> createCtx = new HashMap<>();
                createCtx.put("tableName", "book");
                createCtx.put("columns", Collections.emptyList());

                flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                        .thenReturn(createCtx);

                flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap()))
                        .thenReturn(null);

                flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL)))
                        .thenReturn(Collections.emptyList());

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                                eq("migration/flyway/create-sequence-table-generator.sql.ftl"),
                                eq(seqCtx)))
                        .thenReturn("CREATE SEQUENCE book_gen START WITH 1 INCREMENT BY 50;");

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                                eq("migration/flyway/create-table.sql.ftl"),
                                eq(createCtx)))
                        .thenReturn("CREATE TABLE book (...);");

                flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class)))
                        .thenAnswer(inv -> null);

                modelNameUtils.when(() -> ModelNameUtils.stripSuffix("BookEntity")).thenReturn("Book");
                modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("Book")).thenReturn("book");

                generator.generate(bookModel, "out");

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath),
                        eq("V1__create_book_sequence.sql"),
                        eq("CREATE SEQUENCE book_gen START WITH 1 INCREMENT BY 50;")
                ));

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath),
                        eq("V2__create_book_table.sql"),
                        eq("CREATE TABLE book (...);")
                ));
        }
    }

    @Test
    void generate_shouldNotGenerateSequenceScript_whenDdlArtifactAlreadyInManifestWithSameContent() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.SEQUENCE);

        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final String expectedScriptsPath =
                "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
            final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
            final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
            final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
            final MockedConstruction<MigrationManifestBuilder> manifestConstr = mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                        final MigrationState ms = mock(MigrationState.class);
                        when(ms.getEntities()).thenReturn(null);
                        when(ms.getDdlArtifacts()).thenReturn(null);
                        when(mock.build()).thenReturn(ms);
                        when(mock.hasDdlArtifactFileWithSuffixAndContent(any(DdlArtifactType.class), anyString(), anyString(), anyString())).thenReturn(true);
                    })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT))
                    .thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(0);
            when(initialState.getEntities()).thenReturn(null);
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());
            fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

            final Map<String, Object> seqCtx = new HashMap<>();
            seqCtx.put("name", "book_gen");
            seqCtx.put("initialValue", 1);
            seqCtx.put("allocationSize", 50);
            seqCtx.put("sequence", true);

            flyway.when(() -> FlywayUtils.toSequenceGeneratorContext(eq(bookModel))).thenReturn(seqCtx);

            final Map<String, Object> createCtx = new HashMap<>();
            createCtx.put("tableName", "book");
            createCtx.put("columns", Collections.emptyList());

            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createCtx);

            flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap())).thenReturn(null);
            flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL))).thenReturn(Collections.emptyList());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                            eq("migration/flyway/create-sequence-table-generator.sql.ftl"),
                            eq(seqCtx)))
                    .thenReturn("CREATE SEQUENCE book_gen START WITH 1 INCREMENT BY 50;");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-table.sql.ftl"), eq(createCtx)))
                    .thenReturn("CREATE TABLE book (...);");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class)))
                    .thenAnswer(inv -> null);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("BookEntity")).thenReturn("Book");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("Book")).thenReturn("book");

            generator.generate(bookModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq(expectedScriptsPath), contains("create_book_sequence.sql"), anyString()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq(expectedScriptsPath), eq("V1__create_book_table.sql"), eq("CREATE TABLE book (...);")));
        }
    }

    @Test
    void generate_shouldGenerateTableGeneratorScript_whenModelHasTableId_andNotInManifest() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.TABLE);

        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator =
                new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final String expectedScriptsPath =
                "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
                final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
                final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
                final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
                final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
                final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
                final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                        mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                        final MigrationState ms = mock(MigrationState.class);
                        when(ms.getEntities()).thenReturn(null);
                        when(mock.build()).thenReturn(ms);
                        when(mock.hasDdlArtifactFileWithSuffixAndContent(any(DdlArtifactType.class), anyString(), anyString(), anyString()))
                                .thenReturn(false);
                        })) {

                ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT))
                        .thenReturn(false);

                final MigrationState initialState = mock(MigrationState.class);
                when(initialState.getLastScriptVersion()).thenReturn(0);
                when(initialState.getEntities()).thenReturn(null);

                flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project"))
                        .thenReturn(initialState);

                flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap()))
                        .thenReturn(Collections.emptyMap());

                fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
                fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
                fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

                final Map<String, Object> tableGenCtx = new HashMap<>();
                tableGenCtx.put("name", "book_gen");
                tableGenCtx.put("pkColumnName", "gen_name");
                tableGenCtx.put("valueColumnName", "gen_value");
                tableGenCtx.put("table", true);
                tableGenCtx.put("initialValue", 1);
                tableGenCtx.put("pkColumnValue", "book");

                flyway.when(() -> FlywayUtils.toTableGeneratorContext(eq(bookModel))).thenReturn(tableGenCtx);

                final Map<String, Object> createCtx = new HashMap<>();
                createCtx.put("tableName", "book");
                createCtx.put("columns", Collections.emptyList());

                flyway.when(() -> FlywayUtils.toCreateTableContext(
                        eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean())
                ).thenReturn(createCtx);

                flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap()))
                        .thenReturn(null);

                flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL)))
                        .thenReturn(Collections.emptyList());

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                                eq("migration/flyway/create-sequence-table-generator.sql.ftl"), eq(tableGenCtx))
                ).thenReturn("CREATE TABLE book_id_gen (...);");

                tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                        eq("migration/flyway/create-table.sql.ftl"), eq(createCtx))
                ).thenReturn("CREATE TABLE book (...);");

                flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class))).thenAnswer(inv -> null);

                modelNameUtils.when(() -> ModelNameUtils.stripSuffix("BookEntity")).thenReturn("Book");
                modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("Book")).thenReturn("book");

                generator.generate(bookModel, "out");

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath),
                        eq("V1__create_book_table_generator.sql"),
                        eq("CREATE TABLE book_id_gen (...);")
                ));

                writer.verify(() -> FileWriterUtils.writeToFile(
                        eq(expectedScriptsPath),
                        eq("V2__create_book_table.sql"),
                        eq("CREATE TABLE book (...);")
                ));
        }
    }

    @Test
    void generate_shouldNotGenerateTableGeneratorScript_whenDdlArtifactAlreadyInManifestWithSameContent() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.TABLE);

        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);
        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr = mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {

                        final MigrationState ms = mock(MigrationState.class);
                        when(ms.getEntities()).thenReturn(null);
                        when(ms.getDdlArtifacts()).thenReturn(null);

                        when(mock.build()).thenReturn(ms);

                        when(mock.hasDdlArtifactFileWithSuffixAndContent(any(DdlArtifactType.class), anyString(), anyString(), anyString())).thenReturn(true);
                    })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)).thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(0);
            when(initialState.getEntities()).thenReturn(null);
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());

            fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

            final Map<String, Object> tableGenCtx = new HashMap<>();
            tableGenCtx.put("name", "book_gen");
            tableGenCtx.put("pkColumnName", "gen_name");
            tableGenCtx.put("valueColumnName", "gen_value");
            tableGenCtx.put("table", true);
            tableGenCtx.put("initialValue", 1);
            tableGenCtx.put("pkColumnValue", "book");

            flyway.when(() -> FlywayUtils.toTableGeneratorContext(eq(bookModel))).thenReturn(tableGenCtx);

            final Map<String, Object> createCtx = new HashMap<>();
            createCtx.put("tableName", "book");
            createCtx.put("columns", Collections.emptyList());

            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createCtx);

            flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap())).thenReturn(null);

            flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL)))
                    .thenReturn(Collections.emptyList());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                            eq("migration/flyway/create-sequence-table-generator.sql.ftl"),
                            eq(tableGenCtx)))
                    .thenReturn("CREATE TABLE book_id_gen (...);");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                            eq("migration/flyway/create-table.sql.ftl"),
                            eq(createCtx)))
                    .thenReturn("CREATE TABLE book (...);");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class)))
                    .thenAnswer(inv -> null);

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("BookEntity")).thenReturn("Book");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("Book")).thenReturn("book");

            generator.generate(bookModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), contains("create_book_table_generator.sql"), anyString()
            ), never());

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), eq("V1__create_book_table.sql"), eq("CREATE TABLE book (...);")
            ));
        }
    }

    @Test
    void generate_shouldNotGenerateAlterTableCombined_forNewModel() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.AUTO);

        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);
        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                     mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                         final MigrationState ms = mock(MigrationState.class);
                         when(ms.getEntities()).thenReturn(null);
                         when(ms.getDdlArtifacts()).thenReturn(null);
                         when(mock.build()).thenReturn(ms);
                         when(mock.hasDdlArtifactFileWithSuffixAndContent(any(), anyString(), anyString(), anyString()))
                                 .thenReturn(false);
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)).thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(0);
            when(initialState.getEntities()).thenReturn(null);
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());

            fieldUtils.when(() -> FieldUtils.isJsonField(any(FieldDefinition.class))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);

            final Map<String, Object> seqCtx = new HashMap<>();
            seqCtx.put("name", "book_id_seq");
            flyway.when(() -> FlywayUtils.toSequenceGeneratorContext(eq(bookModel))).thenReturn(seqCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-sequence-table-generator.sql.ftl"), eq(seqCtx)))
                    .thenReturn("CREATE SEQUENCE book_id_seq;");

            final Map<String, Object> createCtx = new HashMap<>();
            createCtx.put("tableName", "book");
            createCtx.put("columns", Collections.emptyList());

            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createCtx);

            flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap())).thenReturn(null);
            flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL)))
                    .thenReturn(Collections.emptyList());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-table.sql.ftl"), eq(createCtx)))
                    .thenReturn("CREATE TABLE book (...);");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/alter-table-combined.sql.ftl"), anyMap()))
                    .thenReturn("-- ALTER COMBINED --");

            modelNameUtils.when(() -> ModelNameUtils.stripSuffix("BookEntity")).thenReturn("Book");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("Book")).thenReturn("book");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class))).thenAnswer(inv -> null);

            generator.generate(bookModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), contains("create_book_table.sql"), eq("CREATE TABLE book (...);")
            ));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("migration/flyway/alter-table-combined.sql.ftl"), anyMap()
            ), never());

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath),
                    contains("alter_table_book.sql"),
                    anyString()
            ), never());
        }
    }

    @Test
    void generate_shouldGenerateJoinTableScript_onlyOnce_whenTwoModelsShareSameJoinTable() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition id1 = mock(FieldDefinition.class);
        final IdDefinition idDef1 = mock(IdDefinition.class);
        when(id1.getId()).thenReturn(idDef1);
        when(idDef1.getStrategy()).thenReturn(IdStrategyEnum.IDENTITY);

        final FieldDefinition mtm1 = mock(FieldDefinition.class);
        final RelationDefinition rel1 = mock(RelationDefinition.class);
        final JoinTableDefinition jt1 = mock(JoinTableDefinition.class);
        when(mtm1.getRelation()).thenReturn(rel1);
        when(rel1.getType()).thenReturn("ManyToMany");
        when(rel1.getJoinTable()).thenReturn(jt1);
        when(jt1.getName()).thenReturn("user_roles");

        final ModelDefinition userModel = newModel("UserEntity", "users", List.of(id1, mtm1));
        final FieldDefinition id2 = mock(FieldDefinition.class);
        final IdDefinition idDef2 = mock(IdDefinition.class);
        when(id2.getId()).thenReturn(idDef2);
        when(idDef2.getStrategy()).thenReturn(IdStrategyEnum.IDENTITY);

        final FieldDefinition mtm2 = mock(FieldDefinition.class);
        final RelationDefinition rel2 = mock(RelationDefinition.class);
        final JoinTableDefinition jt2 = mock(JoinTableDefinition.class);
        when(mtm2.getRelation()).thenReturn(rel2);
        when(rel2.getType()).thenReturn("ManyToMany");
        when(rel2.getJoinTable()).thenReturn(jt2);
        when(jt2.getName()).thenReturn("user_roles");

        final ModelDefinition roleModel = newModel("RoleEntity", "roles", List.of(id2, mtm2));

        final List<ModelDefinition> allEntities = List.of(userModel, roleModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);
        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                     mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                         final MigrationState ms = mock(MigrationState.class);
                         when(ms.getEntities()).thenReturn(null);
                         when(ms.getDdlArtifacts()).thenReturn(null);
                         when(mock.build()).thenReturn(ms);
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)).thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(0);
            when(initialState.getEntities()).thenReturn(null);
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());

            fieldUtils.when(() -> FieldUtils.isJsonField(any(FieldDefinition.class))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    final List<FieldDefinition> fs = (List<FieldDefinition>) inv.getArgument(0);
                    return fs.stream().filter(f -> f.getId() != null).findFirst().orElse(fs.get(0));
            });

            final Map<String, Object> createUsers = new HashMap<>();
            createUsers.put("tableName", "users");
            createUsers.put("columns", Collections.emptyList());

            final Map<String, Object> createRoles = new HashMap<>();
            createRoles.put("tableName", "roles");
            createRoles.put("columns", Collections.emptyList());

            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(userModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createUsers);
            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(roleModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createRoles);

            flyway.when(() -> FlywayUtils.toForeignKeysContext(any(ModelDefinition.class), anyMap(), anyMap())).thenReturn(null);
            flyway.when(() -> FlywayUtils.collectElementCollectionTables(any(ModelDefinition.class), eq(DatabaseType.POSTGRESQL)))
                    .thenReturn(Collections.emptyList());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-table.sql.ftl"), anyMap()))
                    .thenReturn("CREATE TABLE ...;");

            final Map<String, Object> joinCtx = new HashMap<>();
            joinCtx.put("tableName", "user_roles");

            flyway.when(() -> FlywayUtils.toJoinTableContext(any(ModelDefinition.class), any(FieldDefinition.class), eq(DatabaseType.POSTGRESQL), anyMap()))
                  .thenReturn(joinCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-join-table.sql.ftl"), eq(joinCtx)))
                    .thenReturn("CREATE TABLE user_roles (...);");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class))).thenAnswer(inv -> null);

            generator.generate(userModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), contains("create_user_roles.sql"), eq("CREATE TABLE user_roles (...);")
            ), times(1));
        }
    }

    @Test
    void generate_shouldGenerateAddForeignKeysScript_whenNewModelHasForeignKeysContext() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.AUTO);

        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                     mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                         final MigrationState ms = mock(MigrationState.class);
                         when(ms.getEntities()).thenReturn(null);
                         when(ms.getDdlArtifacts()).thenReturn(null);
                         when(mock.build()).thenReturn(ms);
                         when(mock.hasDdlArtifactFileWithSuffixAndContent(any(), anyString(), anyString(), anyString()))
                                 .thenReturn(false);
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT))
               .thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(1);
            when(initialState.getEntities()).thenReturn(null);
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap()))
                  .thenReturn(Collections.emptyMap());
            flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL)))
                  .thenReturn(Collections.emptyList());

            fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(List.of(idField))).thenReturn(idField);

            final Map<String, Object> seqCtx = new HashMap<>();
            seqCtx.put("name", "book_id_seq");
            flyway.when(() -> FlywayUtils.toSequenceGeneratorContext(eq(bookModel))).thenReturn(seqCtx);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-sequence-table-generator.sql.ftl"), eq(seqCtx)))
                    .thenReturn("CREATE SEQUENCE book_id_seq;");

            final Map<String, Object> createCtx = new HashMap<>();
            createCtx.put("tableName", "book");
            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createCtx);
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-table.sql.ftl"), eq(createCtx)))
                   .thenReturn("CREATE TABLE book (...);");

            final Map<String, Object> fkCtx = new HashMap<>();
            fkCtx.put("table", "book");
            fkCtx.put("fks", List.of(
                    new HashMap<>(Map.of("column", "author_id", "refTable", "author", "refColumn", "id"))
            ));
            flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap()))
                    .thenReturn(fkCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("migration/flyway/add-foreign-keys.sql.ftl"),
                    argThat(m -> m != null && m.containsKey("fks") && ((List<?>) m.get("fks")).size() == 1)))
                .thenReturn("ALTER TABLE book ADD CONSTRAINT ...;");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class))).thenAnswer(inv -> null);

            generator.generate(bookModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), eq("V2__create_book_sequence.sql"), eq("CREATE SEQUENCE book_id_seq;")
            ));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), eq("V3__create_book_table.sql"), eq("CREATE TABLE book (...);")
            ));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), eq("V4__alter_table_book.sql"), eq("ALTER TABLE book ADD CONSTRAINT ...;")
            ));

            verify(initialState).setLastScriptVersion(4);
        }
    }

    @Test
    void generate_shouldGenerateAddForeignKeysScript_onlyForNewFks_whenSomeAlreadyExistInState() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idField.getId()).thenReturn(idDef);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.IDENTITY);

        final ModelDefinition bookModel = newModel("BookEntity", "book", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(bookModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;
        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        final FkState existingFk = mock(FkState.class);
        when(existingFk.getColumn()).thenReturn("author_id");
        when(existingFk.getRefTable()).thenReturn("author");
        when(existingFk.getRefColumn()).thenReturn("id");

        final EntityState oldBookState = mock(EntityState.class);
        when(oldBookState.getTable()).thenReturn("book");
        when(oldBookState.getFks()).thenReturn(List.of(existingFk));

        final MigrationState msInManifest = mock(MigrationState.class);
        when(msInManifest.getEntities()).thenReturn(List.of(oldBookState));
        when(msInManifest.getDdlArtifacts()).thenReturn(null);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<MigrationDiffer> differ = mockStatic(MigrationDiffer.class, Mockito.CALLS_REAL_METHODS);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                     mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                         when(mock.build()).thenReturn(msInManifest);
                         when(mock.hasDdlArtifactFileWithSuffixAndContent(any(), anyString(), anyString(), anyString()))
                                 .thenReturn(false);
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)).thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(0);
            when(initialState.getEntities()).thenReturn(List.of(oldBookState));
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());
            flyway.when(() -> FlywayUtils.collectElementCollectionTables(eq(bookModel), eq(DatabaseType.POSTGRESQL))).thenReturn(Collections.emptyList());

            fieldUtils.when(() -> FieldUtils.isJsonField(any(FieldDefinition.class))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);

            final Map<String, Object> createCtx = new HashMap<>();
            createCtx.put("tableName", "book");
            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(bookModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createCtx);

            final Result emptyDiff = mock(Result.class);
            when(emptyDiff.isEmpty()).thenReturn(true);
            differ.when(() -> MigrationDiffer.diff(eq(oldBookState), anyMap())).thenReturn(emptyDiff);

            final Map<String, Object> fkCtx = new HashMap<>();
            fkCtx.put("table", "book");
            fkCtx.put("fks", List.of(
                    new HashMap<>(Map.of("column", "author_id", "refTable", "author", "refColumn", "id")),
                    new HashMap<>(Map.of("column", "publisher_id", "refTable", "publisher", "refColumn", "id"))
            ));
            flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(bookModel), anyMap(), anyMap())).thenReturn(fkCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("migration/flyway/add-foreign-keys.sql.ftl"),
                    argThat(m -> {
                        if (m == null) return false;
                        final Object fksObj = m.get("fks");
                        if (!(fksObj instanceof List<?> fks)) return false;
                        if (fks.size() != 1) return false;
                        final Object fk0 = fks.get(0);
                        if (!(fk0 instanceof Map<?, ?> mm)) return false;
                        return "publisher_id".equals(String.valueOf(mm.get("column")));
                    })
            )).thenReturn("ALTER TABLE book ADD FK publisher_id;");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class))).thenAnswer(inv -> null);

            generator.generate(bookModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath), eq("V1__alter_table_book.sql"), eq("ALTER TABLE book ADD FK publisher_id;")
            ));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/alter-table-combined.sql.ftl"), anyMap()), never());
        }
    }

    @Test
    void generate_shouldNotGenerateJoinTableScript_whenJoinTableAlreadyExistsInMigrationState() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        final FieldDefinition id1 = mock(FieldDefinition.class);
        final IdDefinition idDef1 = mock(IdDefinition.class);
        when(id1.getId()).thenReturn(idDef1);
        when(idDef1.getStrategy()).thenReturn(IdStrategyEnum.IDENTITY);

        final FieldDefinition mtm1 = mock(FieldDefinition.class);
        final RelationDefinition rel1 = mock(RelationDefinition.class);
        final JoinTableDefinition jt1 = mock(JoinTableDefinition.class);
        when(mtm1.getRelation()).thenReturn(rel1);
        when(rel1.getType()).thenReturn("ManyToMany");
        when(rel1.getJoinTable()).thenReturn(jt1);
        when(jt1.getName()).thenReturn("user_roles");

        final ModelDefinition userModel = newModel("UserEntity", "users", List.of(id1, mtm1));
        final List<ModelDefinition> allEntities = List.of(userModel);

        when(cfg.isMigrationScripts()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.POSTGRESQL);
        when(cfg.getOptimisticLocking()).thenReturn(false);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final String expectedScriptsPath = "/tmp/project/" + GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION;

        final JoinState joinState = mock(JoinState.class);
        when(joinState.getTable()).thenReturn("user_roles");

        final EntityState entityState = mock(EntityState.class);
        when(entityState.getTable()).thenReturn("users");
        when(entityState.getJoins()).thenReturn(List.of(joinState));

        final MigrationState msInManifest = mock(MigrationState.class);
        when(msInManifest.getEntities()).thenReturn(List.of(entityState));
        when(msInManifest.getDdlArtifacts()).thenReturn(null);
        final MigrationScriptGenerator generator = new MigrationScriptGenerator(cfg, projectMetadata, allEntities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FlywayUtils> flyway = mockStatic(FlywayUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedConstruction<MigrationManifestBuilder> manifestConstr =
                     mockConstruction(MigrationManifestBuilder.class, (mock, constructionContext) -> {
                         when(mock.build()).thenReturn(msInManifest);
                         when(mock.hasDdlArtifactFileWithSuffixAndContent(any(), anyString(), anyString(), anyString()))
                                 .thenReturn(false);
                     })) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)).thenReturn(false);

            final MigrationState initialState = mock(MigrationState.class);
            when(initialState.getLastScriptVersion()).thenReturn(0);
            when(initialState.getEntities()).thenReturn(List.of(entityState));
            when(initialState.getDdlArtifacts()).thenReturn(null);

            flyway.when(() -> FlywayUtils.loadOrEmpty("/tmp/project")).thenReturn(initialState);
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(anyList(), any(), anyMap())).thenReturn(Collections.emptyMap());
            flyway.when(() -> FlywayUtils.collectElementCollectionTables(any(ModelDefinition.class), eq(DatabaseType.POSTGRESQL)))
                    .thenReturn(Collections.emptyList());

            fieldUtils.when(() -> FieldUtils.isJsonField(any(FieldDefinition.class))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(id1);

            final Map<String, Object> createUsers = new HashMap<>();
            createUsers.put("tableName", "users");
            createUsers.put("columns", Collections.emptyList());

            flyway.when(() -> FlywayUtils.toCreateTableContext(eq(userModel), eq(DatabaseType.POSTGRESQL), anyMap(), anyList(), anyBoolean()))
                    .thenReturn(createUsers);

            flyway.when(() -> FlywayUtils.toForeignKeysContext(eq(userModel), anyMap(), anyMap())).thenReturn(null);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-table.sql.ftl"), anyMap()))
                    .thenReturn("CREATE TABLE users (...);");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-join-table.sql.ftl"), anyMap()))
                    .thenReturn("CREATE TABLE user_roles (...);");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class))).thenAnswer(inv -> null);

            generator.generate(userModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq(expectedScriptsPath), contains("create_user_roles.sql"), anyString()), never());
            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("migration/flyway/create-join-table.sql.ftl"), anyMap()), never());
        }
    }

}
