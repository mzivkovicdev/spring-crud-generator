package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.AdditionalConfigurationConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class AdditionalPropertyGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalPropertyGenerator.class);
    
    private static final String GRAPHQL = "graphql";
    private static final String ADDITIONAL_CONFIG = "additionalConfig";
    private static final String OPTIMISTIC_LOCKING_RETRY = "optimisticLockingRetry";
    private static final String RETRYABLE_ANNOTATION = "retryableAnnotation";

    private static final String ANNOTATIONS = "annotations";
    private static final String ANNOTATIONS_PACKAGE = "." + ANNOTATIONS;
    private static final String CONFIGURATIONS = "configurations";
    private static final String CONFIGURATIONS_PACKAGE = "." + CONFIGURATIONS;

    private final CrudConfiguration configuration;

    public AdditionalPropertyGenerator(final CrudConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        if (configuration == null || configuration.getAdditionalProperties() == null || configuration.getAdditionalProperties().isEmpty()) {
            return;
        }

        if (GeneratorContext.isGenerated(ADDITIONAL_CONFIG)) {
            return;
        }

        this.generateGraphqlConfiguration(outputDir);
        this.generateOptimisticLockingRetryConfiguration(outputDir);
        this.generateRetryableAnnotationConfiguration(outputDir);

        GeneratorContext.markGenerated(ADDITIONAL_CONFIG);
    }

    /**
     * Generates the configuration for the Retryable annotation.
     * 
     * @param outputDir the base directory where the generated configuration file should be saved
     */
    private void generateRetryableAnnotationConfiguration(final String outputDir) {

        if (GeneratorContext.isGenerated(RETRYABLE_ANNOTATION)) { return; }
        
        final Integer maxAttempts = (Integer) this.configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS);
        final Integer delayMs = (Integer) this.configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS);
        final Integer maxDelayMs = (Integer) this.configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MAX_DELAY_MS);
        final Double multiplier = (Double) this.configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER);

        if (maxAttempts == null || delayMs == null || maxDelayMs == null || multiplier == null) {
            return;
        }

        LOGGER.info("Generating Retryable Annotation configuration");

        final Map<String, Object> context = new HashMap<>();
        context.put("maxAttempts", maxAttempts);
        context.put("delayMs", delayMs);
        context.put("maxDelayMs", maxDelayMs);
        context.put("multiplier", multiplier);

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath + ANNOTATIONS_PACKAGE))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "annotation/retryable-annotation.ftl", context
                ));

        FileWriterUtils.writeToFile(outputDir, ANNOTATIONS, "OptimisticLockingRetry.java", sb.toString());

        GeneratorContext.markGenerated(RETRYABLE_ANNOTATION);
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

        if (GeneratorContext.isGenerated(OPTIMISTIC_LOCKING_RETRY) || !retryConfig) { return; }

        LOGGER.info("Generating Optimistic Locking Retry configuration");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath + CONFIGURATIONS_PACKAGE))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
            "configuration/retry-configuration.ftl", Map.of()
                ));
        
        FileWriterUtils.writeToFile(outputDir, CONFIGURATIONS, "EnableRetryConfiguration.java", sb.toString());

        GeneratorContext.markGenerated(OPTIMISTIC_LOCKING_RETRY);
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

        if (GeneratorContext.isGenerated(GRAPHQL) || !scalarConfig) { return; }

        LOGGER.info("Generating GraphQL configuration");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath + CONFIGURATIONS_PACKAGE))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
            "configuration/scalar-configuration.ftl", Map.of()
                ));
        
        FileWriterUtils.writeToFile(outputDir, CONFIGURATIONS, "GraphQlConfiguration.java", sb.toString());

        GeneratorContext.markGenerated(GRAPHQL);
    }
    
}
