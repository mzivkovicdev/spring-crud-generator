package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockCollectionState;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockFieldState;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockState;

class MongockUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void loadOrEmpty_shouldReturnEmptyStateWhenNoFileExists() {
        final MongockState state = MongockUtils.loadOrEmpty(tempDir.toString());

        assertNotNull(state);
        assertEquals("1.0", state.getGeneratorVersion());
        assertEquals(0, state.getLastVersion());
        assertNotNull(state.getCollections());
        assertTrue(state.getCollections().isEmpty());
    }

    @Test
    void loadOrEmpty_shouldLoadExistingState() throws Exception {
        final Path dir = tempDir.resolve(".crud-generator");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("mongock-state.json"),
                """
                {
                  "generatorVersion": "1.0",
                  "lastVersion": 3,
                  "collections": [
                    {
                      "entityName": "ProductModel",
                      "collection": "products",
                      "fields": [
                        { "name": "name", "bsonType": "string", "unique": false }
                      ]
                    }
                  ]
                }
                """);

        final MongockState state = MongockUtils.loadOrEmpty(tempDir.toString());

        assertEquals(3, state.getLastVersion());
        assertEquals(1, state.getCollections().size());
        assertEquals("products", state.getCollections().get(0).getCollection());
        assertEquals(1, state.getCollections().get(0).getFields().size());
        assertEquals("name", state.getCollections().get(0).getFields().get(0).getName());
    }

    @Test
    void loadOrEmpty_shouldThrowWhenFileIsCorrupted() throws Exception {
        final Path dir = tempDir.resolve(".crud-generator");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("mongock-state.json"), "{ not valid json }}}");

        assertThrows(RuntimeException.class, () -> MongockUtils.loadOrEmpty(tempDir.toString()));
    }

    @Test
    void save_shouldCreateDirectoryAndWriteFile() {
        final MongockState state = new MongockState("1.0", 2,
                List.of(new MongockCollectionState("UserEntity", "users",
                        List.of(new MongockFieldState("email", "string", true)))));

        MongockUtils.save(tempDir.toString(), state);

        final Path file = tempDir.resolve(".crud-generator/mongock-state.json");
        assertTrue(Files.exists(file));
    }

    @Test
    void save_shouldPersistLastVersion() {
        final MongockState state = new MongockState("1.0", 5, List.of());

        MongockUtils.save(tempDir.toString(), state);

        final MongockState loaded = MongockUtils.loadOrEmpty(tempDir.toString());
        assertEquals(5, loaded.getLastVersion());
    }

    @Test
    void save_shouldOverwriteExistingFile() {
        MongockUtils.save(tempDir.toString(), new MongockState("1.0", 1, List.of()));
        MongockUtils.save(tempDir.toString(), new MongockState("1.0", 7, List.of()));

        final MongockState loaded = MongockUtils.loadOrEmpty(tempDir.toString());
        assertEquals(7, loaded.getLastVersion());
    }

    @Test
    void save_shouldPersistCollections() {
        final MongockCollectionState col = new MongockCollectionState("ProductModel", "products",
                List.of(new MongockFieldState("name", "string", false)));
        MongockUtils.save(tempDir.toString(), new MongockState("1.0", 1, List.of(col)));

        final MongockState loaded = MongockUtils.loadOrEmpty(tempDir.toString());
        assertEquals(1, loaded.getCollections().size());
        assertEquals("products", loaded.getCollections().get(0).getCollection());
    }

    @Test
    void toBsonType_shouldReturnStringForNull() {
        assertEquals("string", MongockUtils.toBsonType(null));
    }

    @Test
    void toBsonType_shouldMapStringTypes() {
        assertEquals("string", MongockUtils.toBsonType("String"));
        assertEquals("string", MongockUtils.toBsonType("UUID"));
    }

    @Test
    void toBsonType_shouldMapIntegerTypes() {
        assertEquals("int", MongockUtils.toBsonType("Integer"));
        assertEquals("int", MongockUtils.toBsonType("int"));
    }

    @Test
    void toBsonType_shouldMapLongTypes() {
        assertEquals("long", MongockUtils.toBsonType("Long"));
        assertEquals("long", MongockUtils.toBsonType("long"));
    }

    @Test
    void toBsonType_shouldMapDoubleTypes() {
        assertEquals("double", MongockUtils.toBsonType("Double"));
        assertEquals("double", MongockUtils.toBsonType("double"));
        assertEquals("double", MongockUtils.toBsonType("Float"));
        assertEquals("double", MongockUtils.toBsonType("float"));
    }

    @Test
    void toBsonType_shouldMapBooleanTypes() {
        assertEquals("bool", MongockUtils.toBsonType("Boolean"));
        assertEquals("bool", MongockUtils.toBsonType("boolean"));
    }

    @Test
    void toBsonType_shouldMapBigDecimalToDecimal() {
        assertEquals("decimal", MongockUtils.toBsonType("BigDecimal"));
    }

    @Test
    void toBsonType_shouldMapDateTypesToDate() {
        assertEquals("date", MongockUtils.toBsonType("LocalDate"));
        assertEquals("date", MongockUtils.toBsonType("LocalDateTime"));
        assertEquals("date", MongockUtils.toBsonType("Instant"));
        assertEquals("date", MongockUtils.toBsonType("Date"));
    }

    @Test
    void toBsonType_shouldMapCollectionTypesToArray() {
        assertEquals("array", MongockUtils.toBsonType("List<String>"));
        assertEquals("array", MongockUtils.toBsonType("Set<Integer>"));
    }

    @Test
    void toBsonType_shouldMapUnknownTypeToObject() {
        assertEquals("object", MongockUtils.toBsonType("SomeCustomType"));
        assertEquals("object", MongockUtils.toBsonType("MyEntity"));
    }

    @Test
    void isUniqueIndex_shouldReturnTrueWhenEmailValidationIsTrue() {
        final FieldDefinition field = new FieldDefinition()
                .setValidation(new ValidationDefinition().setEmail(Boolean.TRUE));

        assertTrue(MongockUtils.isUniqueIndex(field));
    }

    @Test
    void isUniqueIndex_shouldReturnFalseWhenEmailValidationIsFalse() {
        final FieldDefinition field = new FieldDefinition()
                .setValidation(new ValidationDefinition().setEmail(Boolean.FALSE));

        assertFalse(MongockUtils.isUniqueIndex(field));
    }

    @Test
    void isUniqueIndex_shouldReturnFalseWhenNoValidation() {
        final FieldDefinition field = new FieldDefinition();

        assertFalse(MongockUtils.isUniqueIndex(field));
    }

    @Test
    void isUniqueIndex_shouldReturnFalseWhenValidationHasNoEmailFlag() {
        final FieldDefinition field = new FieldDefinition()
                .setValidation(new ValidationDefinition().setRequired(Boolean.TRUE));

        assertFalse(MongockUtils.isUniqueIndex(field));
    }
}
