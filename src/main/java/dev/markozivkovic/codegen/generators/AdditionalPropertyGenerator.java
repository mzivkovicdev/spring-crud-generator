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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.codegen.constants.AdditionalConfigurationConstants;
import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.codegen.utils.ContainerUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

public class AdditionalPropertyGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalPropertyGenerator.class);

    private final CrudConfiguration configuration;
    private final PackageConfiguration packageConfiguration;

    public AdditionalPropertyGenerator(final CrudConfiguration configuration, final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final String outputDir) {
        
        if (ContainerUtils.isEmpty(configuration.getAdditionalProperties())) {
            return;
        }

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ADDITIONAL_CONFIG)) {
            return;
        }

        this.generateGraphqlConfiguration(outputDir);
        this.generateOptimisticLockingRetryConfiguration(outputDir);
        this.generateRetryableAnnotationConfiguration(outputDir);

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.ADDITIONAL_CONFIG);
    }

    /**
     * Generates the configuration for the Retryable annotation.
     * 
     * @param outputDir the base directory where the generated configuration file should be saved
     */
    private void generateRetryableAnnotationConfiguration(final String outputDir) {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.RETRYABLE_ANNOTATION)) return;

        if (!Boolean.TRUE.equals(this.configuration.getOptimisticLocking())) return;

        if (!AdditionalPropertiesUtils.hasAnyRetryableConfigOverride(this.configuration.getAdditionalProperties())) return;
        
        final Integer maxAttempts = AdditionalPropertiesUtils.getInt(
                this.configuration.getAdditionalProperties(), AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS
        );
        final Integer delayMs = AdditionalPropertiesUtils.getInt(
                this.configuration.getAdditionalProperties(), AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS
        );
        final Integer maxDelayMs = AdditionalPropertiesUtils.getInt(
                this.configuration.getAdditionalProperties(), AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MAX_DELAY_MS
        );
        final Double multiplier = AdditionalPropertiesUtils.getDouble(
                this.configuration.getAdditionalProperties(), AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER
        );

        LOGGER.info("Generating Retryable Annotation configuration");

        final Map<String, Object> context = new HashMap<>();
        context.put("maxAttempts", maxAttempts);
        context.put("delayMs", delayMs);
        context.put("maxDelayMs", maxDelayMs);
        context.put("multiplier", multiplier);

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeAnnotationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "annotation/retryable-annotation.ftl", context
                ));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeAnnotationSubPackage(packageConfiguration), "OptimisticLockingRetry.java", sb.toString()
        );

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.RETRYABLE_ANNOTATION);
    }

    /**
     * Generates the Optimistic Locking Retry configuration.
     * This configuration is required for the retryable annotation to work.
     * 
     * @param outputDir the directory where the generated configuration will be written
     */
    private void generateOptimisticLockingRetryConfiguration(final String outputDir) {

        final boolean retryConfig = (Boolean) configuration.getAdditionalProperties()
                .getOrDefault(AdditionalConfigurationConstants.OPT_LOCK_RETRY_CONFIGURATION, false);

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.OPTIMISTIC_LOCKING_RETRY) || !retryConfig
                || !Boolean.TRUE.equals(this.configuration.getOptimisticLocking())) { return; }

        LOGGER.info("Generating Optimistic Locking Retry configuration");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        final StringBuilder sb = new StringBuilder();
        
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
            "configuration/retry-configuration.ftl", Map.of()
                ));
        
        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "EnableRetryConfiguration.java", sb.toString()
        );

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.OPTIMISTIC_LOCKING_RETRY);
    }

    /**
     * Generates the GraphQL configuration. This method is only called if the GraphQL scalar
     * configuration is enabled.
     *
     * @param outputDir the output directory where the configuration file will be generated
     */
    private void generateGraphqlConfiguration(final String outputDir) {
        
        final boolean scalarConfig = (Boolean) configuration.getAdditionalProperties()
                .getOrDefault(AdditionalConfigurationConstants.GRAPHQL_SCALAR_CONFIG, false);

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL_CONFIGURATION) || !scalarConfig
                || !Boolean.TRUE.equals(this.configuration.getGraphQl())) { return; }

        LOGGER.info("Generating GraphQL configuration");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
            "configuration/scalar-configuration.ftl", Map.of()
                ));
        
        FileWriterUtils.writeToFile(
                outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "GraphQlConfiguration.java", sb.toString()
        );

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL_CONFIGURATION);
    }
    
}
