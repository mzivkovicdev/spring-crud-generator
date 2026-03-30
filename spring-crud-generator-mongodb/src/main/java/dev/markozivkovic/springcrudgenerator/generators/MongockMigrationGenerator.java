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

package dev.markozivkovic.springcrudgenerator.generators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockCollectionState;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockFieldState;
import dev.markozivkovic.springcrudgenerator.models.mongock.MongockState;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.MongockUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

public class MongockMigrationGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongockMigrationGenerator.class);

    private static final String MIGRATION_SUB_PACKAGE = "migration";
    private static final String CREATE_COLLECTION_TEMPLATE = "migration/mongock/create-collection-changeunit.ftl";
    private static final String ADD_FIELDS_TEMPLATE = "migration/mongock/add-fields-changeunit.ftl";
    private static final String REMOVE_FIELDS_TEMPLATE = "migration/mongock/remove-fields-changeunit.ftl";

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;
    private final List<ModelDefinition> entities;

    public MongockMigrationGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata,
            final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (configuration == null || configuration.isMigrationScripts() == null || !configuration.isMigrationScripts()) {
            return;
        }

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MONGOCK_MIGRATION_SCRIPT)) {
            return;
        }

        LOGGER.info("Generating Mongock migration scripts");

        final String basePackage = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String migrationPackage = PackageUtils.join(basePackage, MIGRATION_SUB_PACKAGE);

        final MongockState state = MongockUtils.loadOrEmpty(this.projectMetadata.getProjectBaseDir());
        int version = state.getLastVersion() + 1;

        final List<ModelDefinition> jsonModels = this.entities.stream()
                .flatMap(entity -> entity.getFields().stream())
                .filter(FieldUtils::isJsonField)
                .map(jsonField -> {
                    final String jsonType = FieldUtils.extractJsonInnerElementType(jsonField);
                    return this.entities.stream()
                            .filter(entity -> entity.getName().equals(jsonType))
                            .findFirst()
                            .orElseThrow();
                })
                .collect(Collectors.toList());

        final List<ModelDefinition> models = this.entities.stream()
                .filter(model -> FieldUtils.isAnyFieldId(model.getFields()))
                .filter(model -> !jsonModels.contains(model))
                .collect(Collectors.toList());

        final List<MongockCollectionState> updatedCollections = new ArrayList<>(state.getCollections());

        for (final ModelDefinition model : models) {

            final Optional<MongockCollectionState> existingCollection = updatedCollections.stream()
                    .filter(c -> c.getCollection().equals(model.getStorageName()))
                    .findFirst();

            if (existingCollection.isEmpty()) {
                version = generateCreateCollectionMigration(model, outputDir, migrationPackage, version);
                updatedCollections.add(buildCollectionState(model));
            } else {
                version = generateIncrementalMigrations(model, existingCollection.get(), outputDir, migrationPackage, version);
                updateCollectionState(updatedCollections, model);
            }
        }

        final MongockState updatedState = new MongockState("1.0", version - 1, updatedCollections);
        MongockUtils.save(this.projectMetadata.getProjectBaseDir(), updatedState);

        LOGGER.info("Mongock migration scripts generated");
        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.MONGOCK_MIGRATION_SCRIPT);
    }

    /**
     * Generates a Mongock migration script that creates a new collection with the given indexes.
     *
     * @param model the model to generate the migration script for
     * @param outputDir the output directory for the migration script
     * @param migrationPackage the package for the migration script
     * @param version the version of the migration script
     * @return the next version number
     */
    private int generateCreateCollectionMigration(final ModelDefinition model, final String outputDir,
            final String migrationPackage, final int version) {

        final String className = buildClassName(version, "Create", model.getStorageName(), "Collection");
        final String order = String.format("%03d", version);

        final List<Map<String, Object>> indexes = buildIndexes(model);

        final Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("packageName", migrationPackage);
        ctx.put("changeUnitId", className);
        ctx.put("order", order);
        ctx.put("className", className);
        ctx.put("collectionName", model.getStorageName());
        ctx.put("indexes", indexes);

        final String content = FreeMarkerTemplateProcessorUtils.processTemplate(CREATE_COLLECTION_TEMPLATE, ctx);
        FileWriterUtils.writeToFile(outputDir, MIGRATION_SUB_PACKAGE, className, content);

        LOGGER.info("Generated Mongock create-collection migration: {}", className);
        return version + 1;
    }

    /**
     * Generates incremental Mongock migration scripts for a given model.
     *
     * This method compares the current state of the model with the previous state and generates
     * migration scripts for any added or removed fields.
     *
     * @param model the model to generate migration scripts for
     * @param previousState the previous state of the model
     * @param outputDir the output directory for the migration scripts
     * @param migrationPackage the package for the migration scripts
     * @param version the version of the migration script
     * @return the updated version of the migration script
     */
    private int generateIncrementalMigrations(final ModelDefinition model, final MongockCollectionState previousState,
            final String outputDir, final String migrationPackage, int version) {

        final List<String> currentFieldNames = getNonIdFieldNames(model);
        final List<String> previousFieldNames = previousState.getFields().stream()
                .map(MongockFieldState::getName)
                .collect(Collectors.toList());

        final List<String> addedFields = currentFieldNames.stream()
                .filter(f -> !previousFieldNames.contains(f))
                .collect(Collectors.toList());

        final List<String> removedFields = previousFieldNames.stream()
                .filter(f -> !currentFieldNames.contains(f))
                .collect(Collectors.toList());

        if (!addedFields.isEmpty()) {
            final String stripped = ModelNameUtils.stripSuffix(model.getName());
            final String className = buildClassName(version, "Add", "FieldsTo", stripped);
            version = writeFieldMigration(ADD_FIELDS_TEMPLATE, className, model.getStorageName(), addedFields,
                    outputDir, migrationPackage, version);
        }

        if (!removedFields.isEmpty()) {
            final String stripped = ModelNameUtils.stripSuffix(model.getName());
            final String className = buildClassName(version, "Remove", "FieldsFrom", stripped);
            version = writeFieldMigration(REMOVE_FIELDS_TEMPLATE, className, model.getStorageName(), removedFields,
                    outputDir, migrationPackage, version);
        }

        return version;
    }

    /**
     * Generates a Mongock migration script that adds or removes fields from a collection.
     *
     * @param template the template to use for the migration script
     * @param className the name of the migration class
     * @param collectionName the name of the collection
     * @param fields the fields to add or remove
     * @param outputDir the output directory for the migration script
     * @param migrationPackage the package for the migration script
     * @param version the version of the migration script
     * @return the version of the migration script
     */
    private int writeFieldMigration(final String template, final String className, final String collectionName,
            final List<String> fields, final String outputDir, final String migrationPackage, final int version) {

        final String order = String.format("%03d", version);

        final Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("packageName", migrationPackage);
        ctx.put("changeUnitId", className);
        ctx.put("order", order);
        ctx.put("className", className);
        ctx.put("collectionName", collectionName);
        ctx.put("fields", fields);

        final String content = FreeMarkerTemplateProcessorUtils.processTemplate(template, ctx);
        FileWriterUtils.writeToFile(outputDir, MIGRATION_SUB_PACKAGE, className, content);

        LOGGER.info("Generated Mongock field migration: {}", className);
        return version + 1;
    }

    /**
     * Builds the list of index descriptors for a model.
     * Generates a unique index for every field with email validation.
     */
    private List<Map<String, Object>> buildIndexes(final ModelDefinition model) {
        final List<Map<String, Object>> indexes = new ArrayList<>();
        for (final FieldDefinition field : model.getFields()) {
            if (field.getId() != null) {
                continue;
            }
            if (field.getRelation() != null) {
                continue;
            }
            if (MongockUtils.isUniqueIndex(field)) {
                final Map<String, Object> idx = new LinkedHashMap<>();
                idx.put("field", field.getName());
                idx.put("unique", true);
                indexes.add(idx);
            }
        }
        return indexes;
    }

    /**
     * Returns the names of all non-id, non-relation fields of the model.
     * Relation fields are tracked as well (they are stored as DBRef values in the document).
     */
    private List<String> getNonIdFieldNames(final ModelDefinition model) {
        return model.getFields().stream()
                .filter(f -> f.getId() == null)
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * Builds a MongockCollectionState object from a model definition.
     *
     * @param model the model definition to build the state from
     * @return a MongockCollectionState object representing the state of the model
     */
    private MongockCollectionState buildCollectionState(final ModelDefinition model) {

        final List<MongockFieldState> fieldStates = model.getFields().stream()
                .filter(f -> f.getId() == null)
                .map(f -> new MongockFieldState(
                        f.getName(),
                        MongockUtils.toBsonType(f.getType()),
                        MongockUtils.isUniqueIndex(f)
                ))
                .collect(Collectors.toList());

        return new MongockCollectionState(model.getName(), model.getStorageName(), fieldStates);
    }

    /**
     * Updates the list of MongockCollectionState objects to reflect the updated state of the given model.
     * Removes any existing state for the given model and adds the new state.
     *
     * @param collections the list of MongockCollectionState objects to update
     * @param model the model definition to update the state for
     */
    private void updateCollectionState(final List<MongockCollectionState> collections, final ModelDefinition model) {
        collections.removeIf(c -> c.getCollection().equals(model.getStorageName()));
        collections.add(buildCollectionState(model));
    }

    /**
     * Builds a valid Java class name for a migration, e.g. {@code V001__Create_Products_Collection}.
     * 
     * @param version the version number of the migration
     * @param parts the parts of the class name
     * @return a valid Java class name
     */
    private String buildClassName(final int version, final String... parts) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("V%03d__", version));
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('_');
            }
            sb.append(StringUtils.capitalize(parts[i]));
        }
        return sb.toString();
    }
}
