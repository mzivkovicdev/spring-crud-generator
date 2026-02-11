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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.enums.RelationTypeEnum;
import dev.markozivkovic.springcrudgenerator.migrations.MigrationDiffer;
import dev.markozivkovic.springcrudgenerator.migrations.MigrationManifestBuilder;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.models.flyway.DdlArtifactState.DdlArtifactType;
import dev.markozivkovic.springcrudgenerator.models.flyway.EntityState;
import dev.markozivkovic.springcrudgenerator.models.flyway.MigrationState;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.Result;
import dev.markozivkovic.springcrudgenerator.utils.ContainerUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FlywayUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;

public class MigrationScriptGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationScriptGenerator.class);

    private static Integer version = 1;

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;
    private final List<ModelDefinition> entities;

    public MigrationScriptGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata,
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
        
        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT)) {
            return;
        }

        LOGGER.info("Generating migration scripts");
        final String pathToDbScripts = String.format("%s/%s", this.projectMetadata.getProjectBaseDir(), GeneratorConstants.SRC_MAIN_RESOURCES_DB_MIGRATION);
        final MigrationState migrationState = FlywayUtils.loadOrEmpty(this.projectMetadata.getProjectBaseDir());
        version = migrationState.getLastScriptVersion() + 1;
        final MigrationManifestBuilder manifest = new MigrationManifestBuilder(migrationState);

        final List<ModelDefinition> jsonModels = this.entities.stream()
            .flatMap(entity -> entity.getFields().stream())
            .filter(field -> FieldUtils.isJsonField(field))
            .map(jsonField -> {
                final String jsonType = FieldUtils.extractJsonFieldName(jsonField);
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

        final Map<String, ModelDefinition> modelsByName = models.stream()
                .collect(Collectors.toMap(ModelDefinition::getName, m -> m, (a,b)-> a, LinkedHashMap::new));

        this.generateSequenceAndTableGenerators(pathToDbScripts, models, manifest);

        this.generateCreateTableScripts(pathToDbScripts, models, modelsByName, manifest);

        this.generateAlterTableScripts(pathToDbScripts, models, modelsByName, manifest);

        this.generateJoinTables(pathToDbScripts, models, modelsByName, manifest);

        migrationState.setLastScriptVersion(version - 1);
        FlywayUtils.save(this.projectMetadata.getProjectBaseDir(), manifest.build());

        LOGGER.info("Migration scripts generated");

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.MIGRATION_SCRIPT);
    }

    /**
     * Generates create table scripts for all many-to-many join tables.
     *
     * @param pathToDbScripts the path to which to write the generated scripts
     * @param models the list of models for which to generate the join tables
     * @param modelsByName the mapping of model names to ModelDefinition objects
     * @param manifest the migration manifest
     */
    private void generateJoinTables(final String pathToDbScripts, final List<ModelDefinition> models,
            final Map<String, ModelDefinition> modelsByName, final MigrationManifestBuilder manifest) {
        
        final DatabaseType db = this.configuration.getDatabase();
        final Set<String> emittedJoinTables = new HashSet<>();
        final MigrationState ms = manifest.build();
        final Set<String> existingJoinTables = new HashSet<>();

        if (Objects.nonNull(ms.getEntities())) {
            ms.getEntities().forEach(e -> {
                if (e.getJoins() != null) {
                    e.getJoins().forEach(j -> existingJoinTables.add(j.getTable()));
                }
            });
        }

        for (final ModelDefinition owner : models) {
            if (Objects.isNull(owner.getFields()) || owner.getFields().isEmpty())
                continue;

            for (final FieldDefinition f : owner.getFields()) {
                if (Objects.isNull(f.getRelation()) || !RelationTypeEnum.MANY_TO_MANY.getKey().equals(f.getRelation().getType())) continue;

                final String joinTable = f.getRelation().getJoinTable().getName();
                if (existingJoinTables.contains(joinTable)) {
                    continue;
                }

                if (!emittedJoinTables.add(joinTable)) {
                    continue;
                }

                final Map<String, Object> ctx = FlywayUtils.toJoinTableContext(owner, f, db, modelsByName);

                final String sql = FreeMarkerTemplateProcessorUtils.processTemplate(
                        "migration/flyway/create-join-table.sql.ftl", ctx
                );

                final String fileName = String.format("V%d__create_%s.sql", version, joinTable);
                FileWriterUtils.writeToFile(pathToDbScripts, fileName, sql);
                version++;

                manifest.addJoin(owner.getStorageName(), ctx);
                manifest.addJoinFile(owner.getStorageName(), joinTable, fileName, sql);
            }
        }
    }

    /**
     * Generates alter table scripts for all models except for the ones that are
     * marked as JSON models. The generated scripts are written to the given
     * path.
     * 
     * @param pathToDbScripts the path to which to write the generated scripts
     * @param models the list of models that are not JSON models
     * @param modelsByName the mapping of model names to ModelDefinition objects
     * @param manifest the migration manifest
     */
    @SuppressWarnings("unchecked")
    private void generateAlterTableScripts(final String pathToDbScripts, final List<ModelDefinition> models,
            final Map<String, ModelDefinition> modelsByName, final MigrationManifestBuilder manifest) {

        final MigrationState migrationState = manifest.build();
        final Map<String, EntityState> previousByTable = Objects.isNull(migrationState.getEntities())
                ? Collections.emptyMap()
                : migrationState.getEntities().stream()
                        .collect(Collectors.toMap(EntityState::getTable, e -> e));
        final Map<String, List<Map<String,Object>>> extrasByChildTable = FlywayUtils.collectReverseOneToManyExtras(
                models, this.configuration.getDatabase(), modelsByName
        );

        models.forEach(model -> {

            final Map<String, Object> context = FlywayUtils.toForeignKeysContext(model, modelsByName, extrasByChildTable);
            if (Objects.isNull(context) || context.isEmpty()) {
                return;
            }
            final String tableName = model.getStorageName();
            final EntityState oldState = previousByTable.get(tableName);
            final List<Map<String, Object>> fks = (List<Map<String, Object>>) context.get("fks");

            if (Objects.isNull(fks) || fks.isEmpty()) return;

            final Set<String> newFkKeys = fks.stream()
                    .map(m -> {
                        final String col = String.valueOf(m.get("column"));
                        final String rt  = String.valueOf(m.get("refTable"));
                        final String rc  = String.valueOf(m.get("refColumn"));
                        return MigrationDiffer.fkKey(col, rt, rc);
                    }).collect(Collectors.toSet());

            final Set<String> oldFkKeys = new HashSet<>();
            if (Objects.nonNull(oldState) && Objects.nonNull(oldState.getFks())) {
                oldState.getFks().forEach(existing ->
                        oldFkKeys.add(MigrationDiffer.fkKey(existing.getColumn(), existing.getRefTable(), existing.getRefColumn()))
                );
            }

            final Set<String> reallyNewFkKeys = new HashSet<>(newFkKeys);
            reallyNewFkKeys.removeAll(oldFkKeys);

            if (reallyNewFkKeys.isEmpty()) {
                return;
            }

            final List<Map<String, Object>> filteredFks = fks.stream()
                    .filter(m -> {
                        final String col = String.valueOf(m.get("column"));
                        final String rt  = String.valueOf(m.get("refTable"));
                        final String rc  = String.valueOf(m.get("refColumn"));
                        final String key = MigrationDiffer.fkKey(col, rt, rc);
                        return reallyNewFkKeys.contains(key);
                    })
                    .collect(Collectors.toList());

            context.put("fks", filteredFks);

            final String dbScript = FreeMarkerTemplateProcessorUtils.processTemplate(
                "migration/flyway/add-foreign-keys.sql.ftl", context
            );
            final String dbSciptName = String.format(
                "V%d__alter_table_%s.sql", version, tableName
            );
            
            FileWriterUtils.writeToFile(pathToDbScripts, dbSciptName, dbScript);
            version++;
            
            manifest.addForeignKeys(tableName, context);
            manifest.addEntityFile(tableName, dbSciptName, dbScript);
        });
    }

    /**
     * Generates create table scripts for all models except for the ones that are
     * marked as JSON models. The generated scripts are written to the given
     * path.
     * 
     * @param pathToDbScripts the path to which to write the generated scripts
     * @param models the list of models that are not JSON models
     * @param modelsByName the mapping of model names to ModelDefinition objects
     * @param manifest the migration manifest
     */
    private void generateCreateTableScripts(final String pathToDbScripts, final List<ModelDefinition> models,
            final Map<String, ModelDefinition> modelsByName, final MigrationManifestBuilder manifest) {

        final Map<String, List<Map<String,Object>>> extrasByChildTable =
            FlywayUtils.collectReverseOneToManyExtras(models, this.configuration.getDatabase(), modelsByName);
        final MigrationState ms = manifest.build();
        final Map<String, EntityState> previousByTable = ms.getEntities() == null ?
                Collections.emptyMap() :
                ms.getEntities().stream().collect(Collectors.toMap(
                    EntityState::getTable, e -> e, (a,b)->a, LinkedHashMap::new
                ));

        models.forEach(model -> {
                
            final List<Map<String,Object>> extraCols = extrasByChildTable.getOrDefault(model.getStorageName(), Collections.emptyList());
            final Map<String, Object> context = FlywayUtils.toCreateTableContext(
                    model, this.configuration.getDatabase(), modelsByName, extraCols, this.configuration.getOptimisticLocking()
            );
            final String tableName = model.getStorageName();
            final EntityState oldState = previousByTable.get(tableName);
            final List<Map<String, Object>> elementCollectionTables = FlywayUtils.collectElementCollectionTables(
                    model, this.configuration.getDatabase()
            );

            if (Objects.isNull(oldState)) {
                final String dbScript = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "migration/flyway/create-table.sql.ftl", context
                );
                final String dbSciptName = String.format(
                    "V%d__create_%s_table.sql", version, tableName
                );
                
                FileWriterUtils.writeToFile(pathToDbScripts, dbSciptName, dbScript);
                version++;

                manifest.applyCreateContext(model.getName(), tableName, context);
                manifest.addEntityFile(tableName, dbSciptName, dbScript);
            } else {
                final Map<String,Object> fkCtx = FlywayUtils.toForeignKeysContext(model, modelsByName, extrasByChildTable);
                if (Objects.nonNull(fkCtx) && !fkCtx.isEmpty()) {
                    context.put("fksCtx", fkCtx);
                }
                final Result diff = MigrationDiffer.diff(oldState, context);
                if (!diff.isEmpty()) {
                    final Map<String,Object> alterCtx = new LinkedHashMap<>();
                    alterCtx.put("table", tableName);
                    alterCtx.put("addedColumns", diff.getAddedColumns());
                    alterCtx.put("removedColumns", diff.getRemovedColumns());
                    alterCtx.put("modifiedColumns", diff.getModifiedColumns());
                    alterCtx.put("pkChanged", diff.getPkChanged());
                    alterCtx.put("newPk", diff.getNewPk());
                    alterCtx.put("addedFks", diff.getAddedFks());
                    alterCtx.put("removedFks", diff.getRemovedFks());
                    alterCtx.put("db", this.configuration.getDatabase());
                    alterCtx.put("auditAdded", diff.isAuditAdded());
                    alterCtx.put("auditRemoved", diff.isAuditRemoved());
                    alterCtx.put("auditTypeChanged", diff.isAuditTypeChanged());
                    alterCtx.put("auditCreatedType", context.get("auditCreatedType"));
                    alterCtx.put("auditUpdatedType", context.get("auditUpdatedType"));
                    alterCtx.put("auditNowExpr", context.get("auditNowExpr"));
                    
                    final String dbScript = FreeMarkerTemplateProcessorUtils.processTemplate(
                        "migration/flyway/alter-table-combined.sql.ftl", alterCtx
                    );
                    final String dbSciptName = String.format("V%d__alter_table_%s.sql", version, tableName);
                    FileWriterUtils.writeToFile(pathToDbScripts, dbSciptName, dbScript);
                    version++;
    
                    manifest.applyCreateContext(model.getName(), tableName, context);
                    manifest.removeForeignKeys(tableName, diff.getRemovedFks());
                    manifest.addEntityFile(tableName, dbSciptName, dbScript);
                }
            }

            if (!ContainerUtils.isEmpty(elementCollectionTables)) {
                final List<Map<String, Object>> newElementCollections = elementCollectionTables.stream()
                        .filter(ct -> !previousByTable.containsKey(String.valueOf(ct.get("tableName"))))
                        .toList();

                if (!ContainerUtils.isEmpty(newElementCollections)) {
                    generateElementCollectionTableScripts(pathToDbScripts, newElementCollections, ms, manifest, model);
                }
            }

        });
    }

    /**
     * Generates the create table scripts for all element collection tables.
     *
     * @param pathToDbScripts the path to which to write the generated scripts
     * @param elementCollectionTables the list of element collection tables
     * @param ms the migration state
     * @param manifest the migration manifest
     * @param model the model definition
     */
    private void generateElementCollectionTableScripts(final String pathToDbScripts, final List<Map<String, Object>> elementCollectionTables,
            final MigrationState ms, final MigrationManifestBuilder manifest, final ModelDefinition model
    ) {
        
        final Map<String, Object> ecCtx = new LinkedHashMap<>();
        ecCtx.put("collectionTables", elementCollectionTables);
        ecCtx.put("db", this.configuration.getDatabase().name().toUpperCase(Locale.ROOT));

        final String dbScript = FreeMarkerTemplateProcessorUtils.processTemplate(
            "migration/flyway/element-collection-create.ftl", ecCtx
        );

        final String dbScriptName = String.format("V%d__create_%s_element_collections.sql", version, ModelNameUtils.toSnakeCase(ModelNameUtils.stripSuffix(model.getName())));
        FileWriterUtils.writeToFile(pathToDbScripts, dbScriptName, dbScript);
        version++;

        elementCollectionTables.forEach(ct -> {
            final String ecTable = String.valueOf(ct.get("tableName"));
            final List<Map<String, Object>> cols = new ArrayList<>();
            cols.add(Map.of(
                    "name", String.valueOf(ct.get("joinColumn")),
                    "sqlType", String.valueOf(ct.get("ownerPkSqlType")),
                    "nullable", false,
                    "unique", false,
                    "isPk", false
            ));
            cols.add(Map.of(
                    "name", String.valueOf(ct.get("valueColumn")),
                    "sqlType", String.valueOf(ct.get("valueSqlType")),
                    "nullable", false,
                    "unique", false,
                    "isPk", false
            ));
            final boolean isList = Boolean.TRUE.equals(ct.get("isList"));
            if (isList) {
                cols.add(Map.of(
                        "name", String.valueOf(ct.get("orderColumn")),
                        "sqlType", "INTEGER",
                        "nullable", false,
                        "unique", false,
                        "isPk", false
                ));
            }

            final Map<String, Object> ecCreateCtxForState = new LinkedHashMap<>();
            ecCreateCtxForState.put("tableName", ecTable);
            ecCreateCtxForState.put("columns", cols);
            manifest.applyCreateContext(model.getName(), ecTable, ecCreateCtxForState);
            manifest.addEntityFile(ecTable, dbScriptName, dbScript);
        });
    }

    /**
     * Generates create sequence and table generator scripts for all models that use either sequence or table generator as their id strategy.
     * 
     * @param pathToDbScripts the path to which to write the generated scripts
     * @param models the list of models that are not JSON models
     * @param manifest the migration manifest
     */
    private void generateSequenceAndTableGenerators(final String pathToDbScripts, final List<ModelDefinition> models,
                final MigrationManifestBuilder manifest) {

        final List<ModelDefinition> modelsWithSequences = models.stream()
                .filter(model -> {
                    final FieldDefinition idField = FieldUtils.extractIdField(model.getFields());
                    final boolean importSequenceIfAutoStrategy = (DatabaseType.POSTGRESQL.equals(this.configuration.getDatabase())
                            || DatabaseType.MSSQL.equals(this.configuration.getDatabase())
                            || DatabaseType.MYSQL.equals(this.configuration.getDatabase()))
                             && IdStrategyEnum.AUTO.equals(idField.getId().getStrategy());
                    return IdStrategyEnum.SEQUENCE.equals(idField.getId().getStrategy()) || importSequenceIfAutoStrategy;
                }).collect(Collectors.toList());

        final List<ModelDefinition> modelsWithTableGenerators = models.stream()
                .filter(model -> {
                    final FieldDefinition idField = FieldUtils.extractIdField(model.getFields());
                    return IdStrategyEnum.TABLE.equals(idField.getId().getStrategy());
                }).collect(Collectors.toList());

        modelsWithSequences.forEach(model -> {
            
            final String fileSuffix = String.format(
                    "create_%s_sequence.sql", ModelNameUtils.toSnakeCase(ModelNameUtils.stripSuffix(model.getName()))
            );
            final Map<String, Object> context = FlywayUtils.toSequenceGeneratorContext(model);
            context.put("db", this.configuration.getDatabase().name().toUpperCase(Locale.ROOT));
            final String sql = FreeMarkerTemplateProcessorUtils.processTemplate(
                "migration/flyway/create-sequence-table-generator.sql.ftl", context
            );
            final String seqName = String.valueOf(context.get("name"));

            if (manifest.hasDdlArtifactFileWithSuffixAndContent(DdlArtifactType.SEQUENCE, seqName, fileSuffix, sql)) {
                return;
            }

            final String dbScriptName = String.format("V%d__%s", version, fileSuffix);
            FileWriterUtils.writeToFile(pathToDbScripts, dbScriptName, sql);
            final String tableName = model.getStorageName();
            manifest.addDdlArtifactFile(DdlArtifactType.SEQUENCE, seqName, tableName, dbScriptName, sql);
            version++;
        });

        modelsWithTableGenerators.forEach(model -> {

            final String fileSuffix = String.format(
                    "create_%s_table_generator.sql", ModelNameUtils.toSnakeCase(ModelNameUtils.stripSuffix(model.getName()))
            );
            final Map<String, Object> context = FlywayUtils.toTableGeneratorContext(model);
            context.put("db", this.configuration.getDatabase().name().toUpperCase(Locale.ROOT));
            final String sql = FreeMarkerTemplateProcessorUtils.processTemplate(
                "migration/flyway/create-sequence-table-generator.sql.ftl", context
            );
            final String genTableName = String.valueOf(context.get("name"));

            if (manifest.hasDdlArtifactFileWithSuffixAndContent(DdlArtifactType.TABLE_GENERATOR, genTableName, fileSuffix, sql)) {
                return;
            }

            final String dbScriptName = String.format("V%d__%s", version, fileSuffix);
            FileWriterUtils.writeToFile(pathToDbScripts, dbScriptName, sql);
            final String tableName = model.getStorageName();
            
            manifest.addDdlArtifactFile(DdlArtifactType.TABLE_GENERATOR, genTableName, tableName, dbScriptName, sql);
            version++;
        });
    }

}
