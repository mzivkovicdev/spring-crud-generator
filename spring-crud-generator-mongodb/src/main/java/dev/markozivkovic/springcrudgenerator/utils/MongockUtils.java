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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockState;

/**
 * Utility class for Mongock-related operations: state persistence and BSON type mapping.
 */
public class MongockUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongockUtils.class);

    private static final String CRUD_GENERATOR_DIR = ".crud-generator";
    private static final String STATE_FILE = "mongock-state.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MongockUtils() {

    }

    /**
     * Loads the Mongock migration state from the given base directory.
     * Returns a new empty state if no state file exists yet.
     *
     * @param baseDir the project base directory
     * @return the loaded state, or a fresh empty state
     */
    public static MongockState loadOrEmpty(final String baseDir) {
        final Path statePath = Paths.get(baseDir, CRUD_GENERATOR_DIR, STATE_FILE);
        try {
            if (Files.exists(statePath)) {
                return OBJECT_MAPPER.readValue(Files.readAllBytes(statePath), MongockState.class);
            }
            return new MongockState("1.0", 0, new ArrayList<>());
        } catch (final IOException e) {
            throw new RuntimeException(
                String.format("Failed to load Mongock state from '%s': %s", statePath, e.getMessage()), e
            );
        }
    }

    /**
     * Saves the Mongock migration state to the given base directory.
     *
     * @param baseDir the project base directory
     * @param state   the state to save
     */
    public static void save(final String baseDir, final MongockState state) {
        final Path statePath = Paths.get(baseDir, CRUD_GENERATOR_DIR, STATE_FILE);
        try {
            final Path dir = statePath.getParent();
            if (dir != null && Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            Files.write(
                statePath,
                OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(state),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            LOGGER.info("Mongock state saved to: {}", statePath);
        } catch (final IOException e) {
            throw new RuntimeException(
                String.format("Failed to save Mongock state to '%s': %s", statePath, e.getMessage()), e
            );
        }
    }

    /**
     * Maps a Java/spec field type to its corresponding BSON type string for JSON Schema validation.
     *
     * @param javaType the spec field type (e.g. "String", "Integer", "Boolean")
     * @return the BSON type string
     */
    public static String toBsonType(final String javaType) {
        if (javaType == null) {
            return "string";
        }
        return switch (javaType) {
            case "String", "UUID"                   -> "string";
            case "Integer", "int"                   -> "int";
            case "Long", "long"                     -> "long";
            case "Double", "double",
                 "Float",  "float"                  -> "double";
            case "Boolean", "boolean"               -> "bool";
            case "BigDecimal"                       -> "decimal";
            case "LocalDate", "LocalDateTime",
                 "Instant", "Date"                  -> "date";
            default -> {
                // Collections, relations and custom types
                if (javaType.startsWith("List<") || javaType.startsWith("Set<")) {
                    yield "array";
                }
                yield "object";
            }
        };
    }

    /**
     * Returns {@code true} if the field carries a unique-index constraint:
     * either {@code validation.email = true} or {@code validation.unique = true}.
     *
     * @param field the field definition
     * @return {@code true} when the field should have a unique index
     */
    public static boolean isUniqueIndex(final FieldDefinition field) {
        final ValidationDefinition v = field.getValidation();
        if (v == null) {
            return false;
        }
        return Boolean.TRUE.equals(v.isEmail());
    }
}
