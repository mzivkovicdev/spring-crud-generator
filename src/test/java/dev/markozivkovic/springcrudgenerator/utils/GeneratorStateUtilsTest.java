package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.GeneratorState;
import dev.markozivkovic.springcrudgenerator.models.GeneratorState.ModelState;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;

class GeneratorStateUtilsTest {

    @TempDir
    Path tempDir;

    private ModelState modelState(final String name, final String fingerprint) {
        return new ModelState(name, fingerprint);
    }

    private GeneratorState generatorState(final String version,
                                          final String configuration,
                                          final List<ModelState> models) {

        final GeneratorState state = new GeneratorState();
        state.setGeneratorVersion(version);
        state.setConfiguration(configuration);
        state.setModels(models);
        return state;
    }

    private ModelDefinition modelDefinition(final String name,
                                            final String storageName,
                                            final String description) {
        final ModelDefinition model = new ModelDefinition();
        model.setName(name);
        model.setStorageName(storageName);
        model.setDescription(description);
        model.setFields(new ArrayList<>());
        return model;
    }

    private CrudConfiguration crudConfigurationWithAdditionalProperties(final Map<String, Object> additional) {
        final CrudConfiguration cfg = new CrudConfiguration();
        cfg.setAdditionalProperties(additional);
        return cfg;
    }

    @Test
    @DisplayName("loadOrEmpty returns default empty GeneratorState when file does not exist")
    void loadOrEmpty_shouldReturnDefault_whenStateFileDoesNotExist() {

        final GeneratorState state = GeneratorStateUtils.loadOrEmpty(tempDir.toString());

        assertNotNull(state);
        assertEquals("1.0", state.getGeneratorVersion());
        assertEquals("", state.getConfiguration());
        assertNotNull(state.getModels());
        assertTrue(state.getModels().isEmpty());
    }

    @Test
    @DisplayName("save writes generator-state.json and loadOrEmpty reads the same state back")
    void saveAndLoad_shouldPersistAndReadGeneratorState() {

        final GeneratorState original = generatorState(
                "2.0",
                "config-123",
                new ArrayList<>(List.of(
                        modelState("User", "fp-user"),
                        modelState("Order", "fp-order")
                ))
        );

        GeneratorStateUtils.save(tempDir.toString(), original);

        final Path statePath = tempDir
                .resolve(".crud-generator")
                .resolve("generator-state.json");

        assertTrue(Files.exists(statePath), "Expected generator-state.json to be created");

        final GeneratorState loaded = GeneratorStateUtils.loadOrEmpty(tempDir.toString());
        assertNotNull(loaded);
        assertEquals(original, loaded);
    }

    @Test
    @DisplayName("save creates .crud-generator directory when it does not exist")
    void save_shouldCreateStateDirectory_whenMissing() throws IOException {

        final Path base = tempDir.resolve("subdir");
        Files.createDirectories(base);

        final GeneratorState state = generatorState(
                "3.0",
                "cfg",
                List.of(modelState("User", "fp"))
        );

        GeneratorStateUtils.save(base.toString(), state);

        final Path dir  = base.resolve(".crud-generator");
        final Path file = dir.resolve("generator-state.json");

        assertTrue(Files.exists(dir),  "Expected .crud-generator directory to be created");
        assertTrue(Files.exists(file), "Expected generator-state.json to be created");

        final GeneratorState loaded = GeneratorStateUtils.loadOrEmpty(base.toString());
        assertEquals(state, loaded);
    }

    @Test
    @DisplayName("findPreviousFingerprint returns empty Optional when models list is null or empty")
    void findPreviousFingerprint_shouldReturnEmpty_whenNoModels() {

        final GeneratorState stateWithNullModels = generatorState("1.0", "cfg", null);
        final GeneratorState stateWithEmptyModels = generatorState("1.0", "cfg", new ArrayList<>());

        assertTrue(GeneratorStateUtils.findPreviousFingerprint(stateWithNullModels, "User").isEmpty());
        assertTrue(GeneratorStateUtils.findPreviousFingerprint(stateWithEmptyModels, "User").isEmpty());
    }

    @Test
    @DisplayName("findPreviousFingerprint returns empty Optional when model is not found")
    void findPreviousFingerprint_shouldReturnEmpty_whenModelNotFound() {

        final GeneratorState state = generatorState(
                "1.0",
                "cfg",
                List.of(
                        modelState("User", "fp-user"),
                        modelState("Order", "fp-order")
                )
        );

        final Optional<String> fp = GeneratorStateUtils.findPreviousFingerprint(state, "Product");

        assertTrue(fp.isEmpty());
    }

    @Test
    @DisplayName("findPreviousFingerprint returns fingerprint when model is found")
    void findPreviousFingerprint_shouldReturnFingerprint_whenModelFound() {

        final GeneratorState state = generatorState(
                "1.0",
                "cfg",
                List.of(
                        modelState("User", "fp-user"),
                        modelState("Order", "fp-order")
                )
        );

        final Optional<String> fp = GeneratorStateUtils.findPreviousFingerprint(state, "Order");

        assertTrue(fp.isPresent());
        assertEquals("fp-order", fp.get());
    }

    @Test
    @DisplayName("updateFingerprint initializes models list when null or empty and adds new model")
    void updateFingerprint_shouldInitializeModelsAndAddModel_whenMissing() {

        final GeneratorState state = generatorState("1.0", "old-config", null);

        GeneratorStateUtils.updateFingerprint(state, "User", "fp-user-new", "new-config");

        assertNotNull(state.getModels());
        assertEquals(1, state.getModels().size());

        final ModelState ms = state.getModels().get(0);
        assertEquals("User", ms.getName());
        assertEquals("fp-user-new", ms.getFingerprint());
        assertEquals("new-config", state.getConfiguration());
    }

    @Test
    @DisplayName("updateFingerprint updates existing model fingerprint and configuration")
    void updateFingerprint_shouldUpdateExistingModel() {

        final ModelState user = modelState("User", "old-fp");
        final GeneratorState state = generatorState(
                "1.0",
                "old-config",
                new ArrayList<>(List.of(user))
        );

        GeneratorStateUtils.updateFingerprint(state, "User", "new-fp", "new-config");

        assertEquals(1, state.getModels().size());
        final ModelState updated = state.getModels().get(0);
        assertEquals("User", updated.getName());
        assertEquals("new-fp", updated.getFingerprint());
        assertEquals("new-config", state.getConfiguration());
    }

    @Test
    @DisplayName("updateFingerprint adds new model when it does not exist yet")
    void updateFingerprint_shouldAddModel_whenNotExisting() {

        final GeneratorState state = generatorState(
                "1.0",
                "old-config",
                new ArrayList<>(List.of(modelState("User", "fp-user")))
        );

        GeneratorStateUtils.updateFingerprint(state, "Order", "fp-order", "new-config");

        assertEquals(2, state.getModels().size());
        assertEquals("new-config", state.getConfiguration());

        final Optional<ModelState> order = state.getModels().stream()
                .filter(m -> "Order".equals(m.getName()))
                .findFirst();
        assertTrue(order.isPresent());
        assertEquals("fp-order", order.get().getFingerprint());
    }

    @Test
    @DisplayName("computeFingerprint for ModelDefinition is deterministic for same model")
    void computeFingerprintModel_shouldBeDeterministic_forSameModel() {

        final ModelDefinition model1 = modelDefinition("User", "user_table", "User entity");
        final ModelDefinition model2 = modelDefinition("User", "user_table", "User entity");

        final String fp1 = GeneratorStateUtils.computeFingerprint(model1);
        final String fp2 = GeneratorStateUtils.computeFingerprint(model2);

        assertNotNull(fp1);
        assertNotNull(fp2);
        assertEquals(fp1, fp2);
    }

    @Test
    @DisplayName("computeFingerprint for ModelDefinition changes when model changes")
    void computeFingerprintModel_shouldChange_whenModelChanges() {

        final ModelDefinition model = modelDefinition("User", "user_table", "User entity");

        final String fp1 = GeneratorStateUtils.computeFingerprint(model);

        model.setDescription("Changed description");
        final String fp2 = GeneratorStateUtils.computeFingerprint(model);

        assertNotNull(fp1);
        assertNotNull(fp2);
        assertNotEquals(fp1, fp2);
    }

    @Test
    @DisplayName("computeFingerprint for CrudConfiguration is deterministic for same configuration")
    void computeFingerprintConfig_shouldBeDeterministic_forSameConfiguration() {

        final CrudConfiguration cfg1 = crudConfigurationWithAdditionalProperties(Map.of("a", "b"));
        final CrudConfiguration cfg2 = crudConfigurationWithAdditionalProperties(Map.of("a", "b"));

        final String fp1 = GeneratorStateUtils.computeFingerprint(cfg1);
        final String fp2 = GeneratorStateUtils.computeFingerprint(cfg2);

        assertNotNull(fp1);
        assertNotNull(fp2);
        assertEquals(fp1, fp2);
    }

    @Test
    @DisplayName("computeFingerprint for CrudConfiguration changes when configuration changes")
    void computeFingerprintConfig_shouldChange_whenConfigurationChanges() {

        final CrudConfiguration cfg1 = crudConfigurationWithAdditionalProperties(Map.of("a", "b"));
        final CrudConfiguration cfg2 = crudConfigurationWithAdditionalProperties(Map.of("a", "c"));

        final String fp1 = GeneratorStateUtils.computeFingerprint(cfg1);
        final String fp2 = GeneratorStateUtils.computeFingerprint(cfg2);

        assertNotNull(fp1);
        assertNotNull(fp2);
        assertNotEquals(fp1, fp2);
    }
}
