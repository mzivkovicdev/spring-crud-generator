package com.markozivkovic.codegen.generators;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.model.CrudConfiguration;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.model.ProjectMetadata;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FlywayUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;

public class MigrationScriptGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationScriptGenerator.class);

    private static final String MIGRATION_SCRIPT = "migration-script";
    private static final String DB_MIGRATION_PATH = "db/migration";

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
        final String pathToDbScripts = String.format("%s/%s", projectMetadata.getProjectBaseDir(), DB_MIGRATION_PATH);

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

        this.entities.stream()
            .filter(model -> !jsonModels.contains(model))
            .forEach(model -> {
                final Map<String, Object> context = FlywayUtils.toFlywayProperty(model, this.configuration.getDatabase());
                final String dbScript = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "migration/flyway/create-table.sql.ftl", context
                );

                FileWriterUtils.writeToFile(pathToDbScripts, model.getName(), dbScript);
            });

        LOGGER.info("Migration scripts generated");

        GeneratorContext.markGenerated(MIGRATION_SCRIPT);
    }

}
