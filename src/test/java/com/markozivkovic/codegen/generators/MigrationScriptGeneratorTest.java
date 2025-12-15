package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.migration.MigrationManifestBuilder;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.DatabaseType;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.models.flyway.MigrationState;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FlywayUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;

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
    void generate_generateCreateTableScriptForNewModel() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final FieldDefinition idField = mock(FieldDefinition.class);
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

            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(
                    anyList(), any(), anyMap()))
                  .thenReturn(Collections.emptyMap());

            fieldUtils.when(() -> FieldUtils.isJsonField(idField)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(List.of(idField))).thenReturn(true);

            final Map<String, Object> createCtx = new HashMap<>();
            createCtx.put("table", "book");

            flyway.when(() -> FlywayUtils.toCreateTableContext(
                    eq(bookModel),
                    eq(DatabaseType.POSTGRESQL),
                    anyMap(),
                    anyList(),
                    anyBoolean()))
                  .thenReturn(createCtx);

            flyway.when(() -> FlywayUtils.toForeignKeysContext(
                    eq(bookModel),
                    anyMap()))
                  .thenReturn(null);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("migration/flyway/create-table.sql.ftl"),
                    eq(createCtx)))
               .thenReturn("CREATE TABLE book (...);");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("migration/flyway/alter-table-combined.sql.ftl"),
                    anyMap()))
               .thenReturn("-- ALTER TABLE ...");

            flyway.when(() -> FlywayUtils.save(eq("/tmp/project"), any(MigrationState.class)))
                  .thenAnswer(inv -> null);

            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(any(FieldDefinition.class)))
                      .thenThrow(new IllegalStateException("Should not be called in this test"));

            generator.generate(bookModel, "out");

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq(expectedScriptsPath),
                    eq("V2__create_book_table.sql"),
                    eq("CREATE TABLE book (...);")
            ));

            ctx.verify(() -> GeneratorContext.markGenerated(
                    GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT));
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
            flyway.when(() -> FlywayUtils.collectReverseOneToManyExtras(
                    anyList(), any(), anyMap()))
                  .thenReturn(Collections.emptyMap());

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                      .thenReturn("AddressEntity");

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList()))
                      .thenReturn(true);

            assertThrows(NoSuchElementException.class,
                    () -> generator.generate(userModel, "out"));

            writer.verifyNoInteractions();
        }
    }
}
