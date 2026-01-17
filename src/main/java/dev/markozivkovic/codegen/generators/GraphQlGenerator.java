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

package dev.markozivkovic.codegen.generators;

import static dev.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.imports.ResolverImports;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.models.ProjectMetadata;
import dev.markozivkovic.codegen.templates.GraphQlTemplateContext;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.StringUtils;

public class GraphQlGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlGenerator.class);

    private final CrudConfiguration configuration;
    private final ProjectMetadata projectMetadata;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public GraphQlGenerator(final CrudConfiguration configuration, final ProjectMetadata projectMetadata,
            final List<ModelDefinition> entities, final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.projectMetadata = projectMetadata;
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
     
        if (configuration == null || configuration.getGraphql() == null || !Boolean.TRUE.equals(configuration.getGraphql().getEnabled())) {
            return;
        }

        if (!GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL)) {
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
        final String baseImport = ResolverImports.computeResolverBaseImports(modelDefinition);
        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, PackageUtils.computeResolversPackage(packagePath, packageConfiguration)));
        if (StringUtils.isNotBlank(baseImport)) {
            sb.append(baseImport);
        }
        sb.append(this.generateGraphqlResolver(modelDefinition, outputDir));

        FileWriterUtils.writeToFile(
                outputDir,
                PackageUtils.computeResolversSubPackage(packageConfiguration),
                String.format("%sResolver.java", ModelNameUtils.stripSuffix(modelDefinition.getName())),
                sb.toString()
        );

        final boolean isAduditEnabled = this.entities.stream()
            .anyMatch(e -> Objects.nonNull(e.getAudit()) && Boolean.TRUE.equals(e.getAudit().getEnabled()));

        this.generateGraphqlConfiguration(outputDir, isAduditEnabled);
        this.generateGraphqlDateTimeConfiguration(outputDir, isAduditEnabled);
        
        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL);

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
       
        final Map<String, Object> context = GraphQlTemplateContext.computeGraphQlSchemaContext(e, entities);

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

        final Map<String, Object> context = GraphQlTemplateContext.computeGraphQlResolver(modelDefinition);
        context.put("queries", this.generateQueryMappings(modelDefinition));
        context.put("mutations", this.generateMutationMappings(modelDefinition));
        context.put("projectImports", ResolverImports.computeGraphQlResolverImports(modelDefinition, outputDir, packageConfiguration));

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

        final Map<String, Object> context = GraphQlTemplateContext.computeMutationMappingGraphQL(modelDefinition, entities);

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

        final Map<String, Object> context = GraphQlTemplateContext.computeQueryMappingGraphQL(modelDefinition);

        return FreeMarkerTemplateProcessorUtils.processTemplate(
            "graphql/mapping/queries.ftl", context
        );
    }

    /**
     * Generates the GraphQL DateTime configuration. This method is only called if the GraphQL scalar configuration is enabled and if the audit is enabled.
     * 
     * @param outputDir the directory where the generated configuration will be written
     * @param isAuditEnabled whether the audit is enabled
     */
    private void generateGraphqlDateTimeConfiguration(final String outputDir,final boolean isAuditEnabled) {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL_DATE_TIME_CONFIGURATION) ||
                !Boolean.TRUE.equals(this.configuration.getGraphql().getScalarConfig())) {
            return;
        }

        if (!isAuditEnabled) {
            return;
        }

        LOGGER.info("Generating GraphQL DateTime configuration");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
            "configuration/date-time-scalar-configuration.ftl",
                    Map.of(TemplateContextConstants.AUDIT_ENABLED, isAuditEnabled)
                ));
        
        FileWriterUtils.writeToFile(
                outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "GraphQlDateTimeScalarConfig.java", sb.toString()
        );

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL_DATE_TIME_CONFIGURATION);
    }

    /**
     * Generates the GraphQL configuration. This method is only called if the GraphQL scalar
     * configuration is enabled.
     *
     * @param outputDir the output directory where the configuration file will be generated
     */
    private void generateGraphqlConfiguration(final String outputDir, final Boolean auditEnabled) {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL_CONFIGURATION)
            || Objects.isNull(this.configuration.getGraphql()) 
            || (!Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled()) && !Boolean.TRUE.equals(this.configuration.getGraphql().getScalarConfig()))) { return; }

        LOGGER.info("Generating GraphQL configuration");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
            "configuration/scalar-configuration.ftl",
                    Map.of(TemplateContextConstants.AUDIT_ENABLED, auditEnabled)
                ));
        
        FileWriterUtils.writeToFile(
                outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "GraphQlConfiguration.java", sb.toString()
        );

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL_CONFIGURATION);
    }
    
}
