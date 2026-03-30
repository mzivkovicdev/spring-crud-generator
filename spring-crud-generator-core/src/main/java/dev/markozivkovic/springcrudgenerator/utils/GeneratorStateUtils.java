/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.GeneratorState;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.GeneratorState.ModelState;

public final class GeneratorStateUtils {

    private static final String STATE_DIR = ".crud-generator";
    private static final String STATE_FILE = "generator-state.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private GeneratorStateUtils() {}

    /**
     * Loads the generator state from the given base directory. If no generator state is found, a new empty one is created.
     *
     * @param baseDir the base directory in which to look for the generator state
     * @return the loaded generator state if found, otherwise a new empty one
     * @throws RuntimeException if an exception occurs while loading the generator state
     */
    public static GeneratorState loadOrEmpty(final String baseDir) {
        final Path statePath = Paths.get(baseDir, STATE_DIR, STATE_FILE);
        try {
            if (Files.exists(statePath)) {
                return OBJECT_MAPPER.readValue(Files.readAllBytes(statePath), GeneratorState.class);
            }

            return new GeneratorState("1.0", "", new ArrayList<>());
        } catch (final Exception e) {
            throw new RuntimeException(
                String.format("Failed to load generator state from '%s': %s", statePath, e.getMessage()),
                e
            );
        }
    }

    /**
     * Saves the given generator state to the given base directory. If the target directory does not exist, it is created.
     *
     * @param baseDir the base directory to which to save the generator state
     * @param state the generator state to save
     * @throws RuntimeException if an exception occurs while saving the generator state
     */
    public static void save(final String baseDir, final GeneratorState state) {
        try {
            final File dir = new File(baseDir, STATE_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException(
                    String.format("Failed to create directory: %s", dir)
                );
            }
            final File file = new File(dir, STATE_FILE);
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, state);
        } catch (final IOException e) {
            throw new RuntimeException(
                "Failed to save generator-state.json",
                e
            );
        }
    }

    /**
     * Finds the previous fingerprint for a given model in the given generator state.
     * If the given generator state does not contain any models, an empty optional is returned.
     *
     * @param state the generator state to search in
     * @param modelName the name of the model to search for
     * @return an optional containing the previous fingerprint of the given model, or empty if not found
     */
    public static Optional<String> findPreviousFingerprint(final GeneratorState state, final String modelName) {
        if (state.getModels() == null || state.getModels().isEmpty()) {
            return Optional.empty();
        }
        return state.getModels().stream()
                .filter(m -> modelName.equals(m.getName()))
                .map(ModelState::getFingerprint)
                .findFirst();
    }

    /**
     * Updates the fingerprint of the given model in the given generator state.
     * If the model does not exist in the generator state, it is added with the given fingerprint.
     *
     * @param state         the generator state to update
     * @param modelName     the name of the model to update
     * @param fingerprint   the new fingerprint of the model
     * @param configuration the new configuration of the generator
     */
    public static void updateFingerprint(final GeneratorState state, final String modelName, final String fingerprint,
                final String configuration) {
        
        if (state.getModels() == null || state.getModels().isEmpty()) {
            state.setModels(new ArrayList<>());
        }
        state.getModels().stream()
                .filter(m -> modelName.equals(m.getName()))
                .findFirst()
                .ifPresentOrElse(
                    m -> m.setFingerprint(fingerprint),
                    () -> state.getModels().add(new ModelState(modelName, fingerprint))
                );
        state.setConfiguration(configuration);
    }

    /**
     * Computes a fingerprint for a given model definition.
     * The fingerprint is a SHA-256 hash of a canonical map containing the model's name, storage name, description, fields and audit information.
     * If an exception occurs while computing the fingerprint, a RuntimeException is thrown.
     *
     * @param model the model definition for which to compute the fingerprint
     * @return the computed fingerprint
     * @throws RuntimeException if an exception occurs while computing the fingerprint
     */
    public static String computeFingerprint(final ModelDefinition model) {
        
        try {
            final Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("name", model.getName());
            canonical.put("storageName", model.getStorageName());
            canonical.put("description", model.getDescription());
            canonical.put("fields", model.getFields());
            canonical.put("audit", model.getAudit());
            
            final byte[] bytes = OBJECT_MAPPER.writer()
                    .writeValueAsBytes(canonical);
            
            return HashUtils.sha256(bytes);
        } catch (final Exception e) {
            throw new RuntimeException(
                    String.format("Failed to compute fingerprint for model %s", model.getName()),
                    e
            );
        }
    }

    /**
     * Computes a fingerprint for a given crud configuration.
     * The fingerprint is a SHA-256 hash of a canonical map containing the configuration.
     * If an exception occurs while computing the fingerprint, a RuntimeException is thrown.
     *
     * @param configuration the crud configuration for which to compute the fingerprint
     * @return the computed fingerprint
     * @throws RuntimeException if an exception occurs while computing the fingerprint
     */
    public static String computeFingerprint(final CrudConfiguration configuration) {
        
        try {
            final byte[] bytes = OBJECT_MAPPER.writer()
                    .writeValueAsBytes(configuration);
            
            return HashUtils.sha256(bytes);
        } catch (final Exception e) {
            throw new RuntimeException(
                    String.format("Failed to compute fingerprint for configuration"),
                    e
            );
        }
    }

}
