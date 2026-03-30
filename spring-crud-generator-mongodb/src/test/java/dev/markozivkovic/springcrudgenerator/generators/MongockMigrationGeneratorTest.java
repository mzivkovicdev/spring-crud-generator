package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockState;
import dev.markozivkovic.springcrudgenerator.utils.MongockUtils;

class MongockMigrationGeneratorTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetGeneratorContext() throws Exception {
        final Field f = GeneratorContext.class.getDeclaredField("GENERATED_PARTS");
        f.setAccessible(true);
        ((Set<String>) f.get(null)).clear();
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Path outputDir() throws Exception {
        final Path dir = tempDir.resolve("src/main/java/com/example/app");
        Files.createDirectories(dir);
        return dir;
    }

    private CrudConfiguration enabledConfig() {
        return new CrudConfiguration()
                .setDatabase(DatabaseType.MONGODB)
                .setMigrationScripts(Boolean.TRUE);
    }

    private ModelDefinition userEntity() {
        return new ModelDefinition()
                .setName("UserEntity")
                .setStorageName("users")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String")
                                .setValidation(new ValidationDefinition().setEmail(Boolean.TRUE))
                ));
    }

    private ModelDefinition productModel() {
        return new ModelDefinition()
                .setName("ProductModel")
                .setStorageName("products")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("name").setType("String"),
                        new FieldDefinition().setName("users").setType("UserEntity")
                                .setRelation(new RelationDefinition().setType("OneToMany"))
                ));
    }

    // -------------------------------------------------------------------------
    // skip conditions
    // -------------------------------------------------------------------------

    @Test
    void generate_shouldSkipWhenMigrationScriptsDisabled() throws Exception {
        final Path out = outputDir();
        final CrudConfiguration cfg = new CrudConfiguration()
                .setDatabase(DatabaseType.MONGODB)
                .setMigrationScripts(Boolean.FALSE);
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                cfg, new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(userEntity()));

        gen.generate(userEntity(), out.toAbsolutePath().toString());

        assertFalse(Files.exists(out.resolve("migration")));
    }

    @Test
    void generate_shouldSkipWhenMigrationScriptsNull() throws Exception {
        final Path out = outputDir();
        final CrudConfiguration cfg = new CrudConfiguration().setDatabase(DatabaseType.MONGODB);
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                cfg, new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(userEntity()));

        gen.generate(userEntity(), out.toAbsolutePath().toString());

        assertFalse(Files.exists(out.resolve("migration")));
    }

    @Test
    void generate_shouldSkipWhenConfigurationIsNull() throws Exception {
        final Path out = outputDir();
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                null, new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(userEntity()));

        gen.generate(userEntity(), out.toAbsolutePath().toString());

        assertFalse(Files.exists(out.resolve("migration")));
    }

    // -------------------------------------------------------------------------
    // first-run: create collection migrations
    // -------------------------------------------------------------------------

    @Test
    void generate_shouldCreateV001MigrationForSingleEntity() throws Exception {
        final Path out = outputDir();
        final ModelDefinition user = userEntity();
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(user));

        gen.generate(user, out.toAbsolutePath().toString());

        final Path migration = out.resolve("migration/V001__Create_Users_Collection.java");
        assertTrue(Files.exists(migration), "V001 create-users migration must be generated");

        final String content = Files.readString(migration);
        assertTrue(content.contains("@ChangeUnit"));
        assertTrue(content.contains("package com.example.app.migration;"));
        assertTrue(content.contains("mongoTemplate.createCollection(\"users\")"));
        assertTrue(content.contains("mongoTemplate.collectionExists(\"users\")"));
    }

    @Test
    void generate_shouldCreateMigrationsForAllEntities() throws Exception {
        final Path out = outputDir();
        final List<ModelDefinition> entities = List.of(productModel(), userEntity());
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), entities);

        gen.generate(productModel(), out.toAbsolutePath().toString());

        assertTrue(Files.exists(out.resolve("migration/V001__Create_Products_Collection.java")));
        assertTrue(Files.exists(out.resolve("migration/V002__Create_Users_Collection.java")));
    }

    @Test
    void generate_shouldGenerateUniqueIndexForEmailField() throws Exception {
        final Path out = outputDir();
        final ModelDefinition user = userEntity();
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(user));

        gen.generate(user, out.toAbsolutePath().toString());

        final String content = Files.readString(out.resolve("migration/V001__Create_Users_Collection.java"));
        assertTrue(content.contains("ensureIndex"));
        assertTrue(content.contains(".unique()"));
        assertTrue(content.contains("\"email\""));
    }

    @Test
    void generate_shouldNotGenerateIndexWhenNoUniqueFields() throws Exception {
        final Path out = outputDir();
        final ModelDefinition product = productModel();
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(product));

        gen.generate(product, out.toAbsolutePath().toString());

        final String content = Files.readString(out.resolve("migration/V001__Create_Products_Collection.java"));
        assertFalse(content.contains("ensureIndex"));
    }

    @Test
    void generate_shouldNotGenerateIndexForRelationField() throws Exception {
        final Path out = outputDir();
        final ModelDefinition product = productModel();
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(product));

        gen.generate(product, out.toAbsolutePath().toString());

        final String content = Files.readString(out.resolve("migration/V001__Create_Products_Collection.java"));
        assertFalse(content.contains("\"users\""));
    }

    // -------------------------------------------------------------------------
    // run-once guard
    // -------------------------------------------------------------------------

    @Test
    void generate_shouldRunOnlyOnce() throws Exception {
        final Path out = outputDir();
        final ModelDefinition user = userEntity();
        final MongockMigrationGenerator gen = new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(user));

        gen.generate(user, out.toAbsolutePath().toString());
        // Simulate second call (e.g. next entity in the loop)
        gen.generate(new ModelDefinition().setName("OtherModel").setStorageName("other")
                .setFields(List.of(new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()))),
                out.toAbsolutePath().toString());

        // Only one migration should exist — the second call was skipped
        assertTrue(Files.exists(out.resolve("migration/V001__Create_Users_Collection.java")));
        assertFalse(Files.exists(out.resolve("migration/V002__Create_Other_Collection.java")));
    }

    // -------------------------------------------------------------------------
    // incremental generation
    // -------------------------------------------------------------------------

    @Test
    void generate_shouldGenerateAddFieldsMigrationOnSecondRun() throws Exception {
        final Path out = outputDir();

        // First run: user with email only
        final ModelDefinition userV1 = new ModelDefinition()
                .setName("UserEntity").setStorageName("users")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String")
                ));
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(userV1))
                .generate(userV1, out.toAbsolutePath().toString());

        // Reset context so second run goes through
        resetGeneratorContext();

        // Second run: user with email + new status field
        final ModelDefinition userV2 = new ModelDefinition()
                .setName("UserEntity").setStorageName("users")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String"),
                        new FieldDefinition().setName("status").setType("String")
                ));
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(userV2))
                .generate(userV2, out.toAbsolutePath().toString());

        final Path addMigration = out.resolve("migration/V002__Add_FieldsTo_User.java");
        assertTrue(Files.exists(addMigration), "Add-fields migration must be generated");

        final String content = Files.readString(addMigration);
        assertTrue(content.contains("@ChangeUnit"));
        assertTrue(content.contains("\"status\""));
        assertTrue(content.contains("$set"));
        assertTrue(content.contains("$exists"));
    }

    @Test
    void generate_shouldGenerateRemoveFieldsMigrationOnSecondRun() throws Exception {
        final Path out = outputDir();

        // First run: user with email + status
        final ModelDefinition userV1 = new ModelDefinition()
                .setName("UserEntity").setStorageName("users")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String"),
                        new FieldDefinition().setName("status").setType("String")
                ));
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(userV1))
                .generate(userV1, out.toAbsolutePath().toString());

        resetGeneratorContext();

        // Second run: status field removed
        final ModelDefinition userV2 = new ModelDefinition()
                .setName("UserEntity").setStorageName("users")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String")
                ));
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(userV2))
                .generate(userV2, out.toAbsolutePath().toString());

        final Path removeMigration = out.resolve("migration/V002__Remove_FieldsFrom_User.java");
        assertTrue(Files.exists(removeMigration), "Remove-fields migration must be generated");

        final String content = Files.readString(removeMigration);
        assertTrue(content.contains("@ChangeUnit"));
        assertTrue(content.contains("\"status\""));
        assertTrue(content.contains("$unset"));
    }

    @Test
    void generate_shouldNotGenerateDropMigrationWhenEntityRemovedFromSpec() throws Exception {
        final Path out = outputDir();

        // First run: two entities
        final ModelDefinition user = userEntity();
        final ModelDefinition product = productModel();
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(user, product))
                .generate(user, out.toAbsolutePath().toString());

        resetGeneratorContext();

        // Second run: only user (product removed from spec)
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(user))
                .generate(user, out.toAbsolutePath().toString());

        // No drop-collection migration should exist
        try (final var paths = Files.list(out.resolve("migration"))) {
            final boolean hasDropMigration = paths
                    .map(p -> p.getFileName().toString())
                    .anyMatch(name -> name.toLowerCase().contains("drop"));
            assertFalse(hasDropMigration, "No drop-collection migration must be generated");
        }
    }

    // -------------------------------------------------------------------------
    // state persistence
    // -------------------------------------------------------------------------

    @Test
    void generate_shouldSaveMongockStateAfterGeneration() throws Exception {
        final Path out = outputDir();
        final ModelDefinition user = userEntity();
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(user))
                .generate(user, out.toAbsolutePath().toString());

        final MongockState state = MongockUtils.loadOrEmpty(tempDir.toString());
        assertEquals(1, state.getLastVersion());
        assertEquals(1, state.getCollections().size());
        assertEquals("users", state.getCollections().get(0).getCollection());
    }

    @Test
    void generate_shouldIncrementVersionCorrectlyAcrossRuns() throws Exception {
        final Path out = outputDir();
        final List<ModelDefinition> entities = List.of(productModel(), userEntity());
        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), entities)
                .generate(productModel(), out.toAbsolutePath().toString());

        final MongockState state = MongockUtils.loadOrEmpty(tempDir.toString());
        assertEquals(2, state.getLastVersion());
    }

    @Test
    void generate_shouldSkipEntityWithNoIdField() throws Exception {
        final Path out = outputDir();
        final ModelDefinition noId = new ModelDefinition()
                .setName("NoIdModel").setStorageName("no_id")
                .setFields(List.of(new FieldDefinition().setName("name").setType("String")));

        new MongockMigrationGenerator(
                enabledConfig(), new ProjectMetadata("app", "1.0", tempDir.toString()), List.of(noId))
                .generate(noId, out.toAbsolutePath().toString());

        assertFalse(Files.exists(out.resolve("migration")));
    }
}
