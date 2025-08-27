package com.markozivkovic.codegen.generators;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.model.CrudConfiguration;
import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.model.ProjectMetadata;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;

public class GraphQlGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlGenerator.class);
    
    private static final String GRAPHQL = "graphql";
    private static final String SRC_MAIN_RESOURCES_GRAPHQL = "src/main/resources/" + GRAPHQL;

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;
    private final List<ModelDefinition> entities;

    public GraphQlGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata,
            final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
     
        if (configuration == null || configuration.getGraphQl() == null || !configuration.getGraphQl()) {
            return;
        }

        if (GeneratorContext.isGenerated(GRAPHQL)) {
            return;
        }

        LOGGER.info("Generating GraphQL code");

        final String pathToGraphQlSchema = String.format("%s/%s", projectMetadata.getProjectBaseDir(), SRC_MAIN_RESOURCES_GRAPHQL);

        entities.stream()
            .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
            .forEach(e -> this.generateGraphQlSchema(e, pathToGraphQlSchema));

        GeneratorContext.markGenerated(GRAPHQL);

        LOGGER.info("Finished generating GraphQL code");
    }

    /**
     * Generates a GraphQL schema file based on the provided model definition.
     * The generated schema has the same name as the model definition, and it
     * contains all the fields of the model definition.
     *
     * @param e the model definition for which to generate the GraphQL schema
     * @param pathToGraphQlSchema the path to the directory where the generated
     *        schema will be written
     */
    private void generateGraphQlSchema(final ModelDefinition e, final String pathToGraphQlSchema) {

        final List<FieldDefinition> fields = e.getFields().stream()
            .map(field -> {
                if (Objects.nonNull(field.getRelation())) {
                    return FieldUtils.cloneFieldDefinition(field)
                        .setType(ModelNameUtils.stripSuffix(field.getType()));
                }
                return field;
            }).collect(Collectors.toList());
        
        final Map<String, Object> context = Map.of(
            "name", ModelNameUtils.stripSuffix(e.getName()),
            "fields", fields
        );

        final String graphQl = FreeMarkerTemplateProcessorUtils.processTemplate(
            "graphql/entity.graphql.ftl", context
        );

        FileWriterUtils.writeToFile(
            pathToGraphQlSchema,
            String.format("%s.graphqls", ModelNameUtils.stripSuffix(e.getName()).toLowerCase()),
            graphQl
        );
    }

    // TODO: GraphQL resolvers
    
}
