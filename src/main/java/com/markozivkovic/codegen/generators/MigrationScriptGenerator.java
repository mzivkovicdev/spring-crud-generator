package com.markozivkovic.codegen.generators;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.model.CrudConfiguration;
import com.markozivkovic.codegen.model.CrudConfiguration.DatabaseType;
import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.model.ProjectMetadata;
import com.markozivkovic.codegen.model.flyway.MigrationState;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FlywayUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.MigrationManifestBuilder;

public class MigrationScriptGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationScriptGenerator.class);

    private static final String MIGRATION_SCRIPT = "migration-script";
    private static final String SRC_MAIN_RESOURCES = "/src/main/resources";
    private static final String DB_MIGRATION_PATH = "db/migration";
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
        
        if (GeneratorContext.isGenerated(MIGRATION_SCRIPT)) {
            return;
        }

        LOGGER.info("Generating migration scripts");
        final String pathToDbScripts = String.format("%s/%s/%s", this.projectMetadata.getProjectBaseDir(), SRC_MAIN_RESOURCES, DB_MIGRATION_PATH);
        final MigrationState migrationState = FlywayUtils.loadOrEmpty(this.projectMetadata.getProjectBaseDir());
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
                .filter(model -> !jsonModels.contains(model))
                .collect(Collectors.toList());

        final Map<String, ModelDefinition> modelsByName = models.stream()
                .collect(Collectors.toMap(ModelDefinition::getName, m -> m, (a,b)-> a, LinkedHashMap::new));

        this.generateCreateTableScripts(pathToDbScripts, models, modelsByName, manifest);

        this.generateAlterTableScripts(pathToDbScripts, models, modelsByName, manifest);

        this.generateJoinTables(pathToDbScripts, models, modelsByName, manifest);

        migrationState.setLastScriptVersion(version - 1);
        FlywayUtils.save(this.projectMetadata.getProjectBaseDir(), manifest.build());

        LOGGER.info("Migration scripts generated");

        GeneratorContext.markGenerated(MIGRATION_SCRIPT);
    }

    /**
     * Generates create table scripts for all many-to-many join tables.
     *
     * @param pathToDbScripts the path to which to write the generated scripts
     * @param models the list of models for which to generate the join tables
     * @param modelsByName the mapping of model names to ModelDefinition objects
     */
    private void generateJoinTables(final String pathToDbScripts, final List<ModelDefinition> models,
            final Map<String, ModelDefinition> modelsByName, final MigrationManifestBuilder manifest) {
        
        final DatabaseType db = this.configuration.getDatabase();
        final Set<String> emittedJoinTables = new HashSet<>();

        for (final ModelDefinition owner : models) {
            if (owner.getFields() == null || owner.getFields().isEmpty())
                continue;

            for (final FieldDefinition f : owner.getFields()) {
                if (f.getRelation() == null || !"ManyToMany".equals(f.getRelation().getType())) continue;

                final String joinTable = f.getRelation().getJoinTable().getName();
                if (!emittedJoinTables.add(joinTable))
                    continue;

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
     */
    private void generateAlterTableScripts(final String pathToDbScripts, final List<ModelDefinition> models,
            final Map<String, ModelDefinition> modelsByName, final MigrationManifestBuilder manifest) {
        
        models.forEach(model -> {
                final Map<String, Object> context = FlywayUtils.toForeignKeysContext(model, modelsByName);
                
                if (context != null && !context.isEmpty()) {
                    final String dbScript = FreeMarkerTemplateProcessorUtils.processTemplate(
                        "migration/flyway/add-foreign-keys.sql.ftl", context
                    );
                    final String tableName = model.getStorageName();
                    final String dbSciptName = String.format(
                        "V%d__alter_table_%s.sql", version, tableName
                    );
                    
                    FileWriterUtils.writeToFile(pathToDbScripts, dbSciptName, dbScript);
                    version++;
                    
                    manifest.addForeignKeys(tableName, context);
                    manifest.addEntityFile(tableName, dbSciptName, dbScript);
                }
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
     */
    private void generateCreateTableScripts(final String pathToDbScripts, final List<ModelDefinition> models,
            final Map<String, ModelDefinition> modelsByName, final MigrationManifestBuilder manifest) {

        final Map<String, List<Map<String,Object>>> extrasByChildTable =
            FlywayUtils.collectReverseOneToManyExtras(models, this.configuration.getDatabase(), modelsByName);

        models.forEach(model -> {
                
                final List<Map<String,Object>> extraCols = extrasByChildTable.getOrDefault(model.getStorageName(), Collections.emptyList());
                final Map<String, Object> context = FlywayUtils.toCreateTableContext(model, this.configuration.getDatabase(), modelsByName, extraCols);
                final String dbScript = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "migration/flyway/create-table.sql.ftl", context
                );
                final String tableName = model.getStorageName();
                final String dbSciptName = String.format(
                    "V%d__create_%s_table.sql", version, tableName
                );
                
                FileWriterUtils.writeToFile(pathToDbScripts, dbSciptName, dbScript);
                version++;

                manifest.applyCreateContext(model.getName(), tableName, context);
                manifest.addEntityFile(tableName, dbSciptName, dbScript);
            });
    }

}
