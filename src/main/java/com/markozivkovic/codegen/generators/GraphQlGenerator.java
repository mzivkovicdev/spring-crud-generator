package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class GraphQlGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlGenerator.class);
    
    private static final String GRAPHQL = "graphql";

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

        if (!GeneratorContext.isGenerated(GRAPHQL)) {
            final String pathToGraphQlSchema = String.format("%s/%s", projectMetadata.getProjectBaseDir(), GeneratorConstants.SRC_MAIN_RESOURCES_GRAPHQL);
    
            entities.stream()
                .filter(e -> FieldUtils.isAnyFieldId(e.getFields()))
                .forEach(e -> this.generateGraphQlSchema(e, pathToGraphQlSchema));

            final String scalars = FreeMarkerTemplateProcessorUtils.processTemplate("graphql/scalars.graphql.ftl", Map.of());
            FileWriterUtils.writeToFile(pathToGraphQlSchema, "scalars.graphqls", scalars);
        }

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            return;
        }

        LOGGER.info("Generating GraphQL code");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String baseImport = ImportUtils.computeResolverBaseImports(modelDefinition);
        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.RESOLVERS)));
        if (StringUtils.isNotBlank(baseImport)) {
            sb.append(baseImport);
        }
        sb.append(this.generateGraphqlResolver(modelDefinition, outputDir));

        FileWriterUtils.writeToFile(
                outputDir,
                GeneratorConstants.DefaultPackageLayout.RESOLVERS,
                String.format("%sResolver.java", ModelNameUtils.stripSuffix(modelDefinition.getName())),
                sb.toString()
        );
        
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
            
        final List<String> jsonFieldNames = FieldUtils.extractJsonFields(fields).stream()
                .map(jsonField -> FieldUtils.extractJsonFieldName(jsonField))
                .collect(Collectors.toList());
        final List<ModelDefinition> jsonModels = this.entities.stream()
                .filter(model -> jsonFieldNames.contains(model.getName()))
                .collect(Collectors.toList());
        
        final Map<String, Object> context = Map.of(
            "name", ModelNameUtils.stripSuffix(e.getName()),
            "fields", fields,
            "jsonModels", jsonModels
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

    /**
     * Generates a GraphQL resolver class based on the given model definition. 
     * 
     * @param modelDefinition the model definition for which to generate the GraphQL resolver
     * @return the generated GraphQL resolver code
     */
    private String generateGraphqlResolver(final ModelDefinition modelDefinition, final String outputDir) {

        final Map<String, Object> context = TemplateContextUtils.computeGraphQlResolver(modelDefinition);
        context.put("queries", this.generateQueryMappings(modelDefinition));
        context.put("mutations", this.generateMutationMappings(modelDefinition));
        context.put("projectImports", ImportUtils.computeGraphQlResolverImports(modelDefinition, outputDir));

        return FreeMarkerTemplateProcessorUtils.processTemplate(
            "graphql/resolver-template.ftl", context
        );
    }

    /**
     * Generates a GraphQL mutation mapping for the given model definition. 
     * 
     * @param modelDefinition the model definition for which to generate the
     *        GraphQL mutation mapping
     * @return the generated GraphQL mutation mapping
     */
    private String generateMutationMappings(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeMutationMappingGraphQL(modelDefinition, entities);

        return FreeMarkerTemplateProcessorUtils.processTemplate(
            "graphql/mapping/mutations.ftl", context
        );
    }

    /**
     * Generates a GraphQL query mapping for the given model definition. 
     * 
     * @param modelDefinition the model definition for which to generate the
     *        GraphQL query mapping
     * @return the generated GraphQL query mapping
     */
    private String generateQueryMappings(final ModelDefinition modelDefinition) {

        final Map<String, Object> context = TemplateContextUtils.computeQueryMappingGraphQL(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate(
            "graphql/mapping/queries.ftl", context
        );
    }
    
}
