package com.markozivkovic.codegen.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.AdditionalConfigurationConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.PackageConfiguration;

public class PackageUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageUtils.class);
    private static final String SOURCE_JAVA = "src/main/java/";
    
    private PackageUtils() {
        
    }

    /**
     * Given an output directory, returns the package path of the generated Java source files.
     * The output directory must be an absolute path and must contain the source Java directory.
     * 
     * @param outputDir the output directory
     * @return the package path of the generated Java source files
     * @throws IllegalArgumentException if the output directory is null or empty, is not an absolute path, or does 
     *                                  not contain the source Java directory
     */
    public static String getPackagePathFromOutputDir(final String outputDir) {
        
        if (!StringUtils.isNotBlank(outputDir)) {
            throw new IllegalArgumentException("Output directory cannot be null or empty");
        }
        
        final Path absoluteOutputDir = Paths.get(outputDir);
        
        if (!absoluteOutputDir.isAbsolute()) {
            throw new IllegalArgumentException("Output directory must be an absolute path");
        }

        final String outputPathStr = absoluteOutputDir.toString();

        if (!outputPathStr.contains(SOURCE_JAVA)) {
            throw new IllegalArgumentException(
                String.format(
                    "Output directory '%s' does not contain the source Java directory '%s'",
                    outputPathStr, SOURCE_JAVA
                )
            );
        }

        final String relativePackagePath = outputPathStr.substring(outputPathStr.indexOf(SOURCE_JAVA) + SOURCE_JAVA.length());
        
        return relativePackagePath.replace(File.separator, ".");
    }

    /**
     * Joins the given string parts into a single string, separated by dots.
     * Each part is trimmed and any leading or trailing dots are removed.
     * If any part is null or empty, it is ignored.
     * The resulting string will not have any trailing dots.
     * 
     * @param parts the string parts to join
     * @return the joined string
     */
    public static String join(final String ...parts) {

        final StringBuilder sb = new StringBuilder();
        
        Arrays.asList(parts).forEach(part -> {
            if (StringUtils.isNotBlank(part)) {
                final String parsed = part.trim();
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(parsed);
            }
        });

        return sb.toString();
    }

    /**
     * Computes the configuration package by joining the base package with either the user-defined configuration package or the default
     * configuration package path.
     * If the user-defined configuration package is not null or empty, it is used, otherwise the default configuration package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed package path
     */
    public static String computeConfigurationPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeConfigurationSubPackage(packageConfiguration));
    }

    /**
     * Computes the configuration sub package by joining the base package with either the user-defined configuration package or the default
     * configuration sub package path.
     * If the user-defined configuration package is not null or empty, it is used, otherwise the default configuration sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed configuration sub package path
     */
    public static String computeConfigurationSubPackage(final PackageConfiguration packageConfiguration) {
        
        final String configuration = Objects.nonNull(packageConfiguration) ? packageConfiguration.getConfigurations() : null;

        if (StringUtils.isNotBlank(configuration)) {
            return configuration;
        }

        return GeneratorConstants.DefaultPackageLayout.CONFIGURATIONS;
    }

    /**
     * Computes the exception package by joining the base package with either the user-defined exception package or the default exception package path.
     * If the user-defined exception package is not null or empty, it is used, otherwise the default exception package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed exception package path
     */
    public static String computeExceptionPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeExceptionSubPackage(packageConfiguration));
    }

    /**
     * Computes the exception sub package by joining the base package with either the user-defined exception package or the default
     * exception sub package path.
     * If the user-defined exception package is not null or empty, it is used, otherwise the default exception sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed exception sub package path
     */
    public static String computeExceptionSubPackage(final PackageConfiguration packageConfiguration) {
        
        final String exceptions = Objects.nonNull(packageConfiguration) ? packageConfiguration.getExceptions() : null;

        if (StringUtils.isNotBlank(exceptions)) {
            return exceptions;
        }

        return GeneratorConstants.DefaultPackageLayout.EXCEPTIONS;
    }

    /**
     * Computes the exception response package by joining the base package with either the user-defined exception response package or the default exception response package path.
     * If the user-defined exception response package is not null or empty, it is used, otherwise the default exception response package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed exception response package path
     */
    public static String computeExceptionResponsePackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        
        return join(basePackage, computeExceptionResponseSubPackage(packageConfiguration));
    }

    /**
     * Computes the exception response sub package by joining the base package with either the user-defined exception response package or the default exception response sub package path.
     * If the user-defined exception response package is not null or empty, it is used, otherwise the default exception response sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed exception response sub package path
     */
    public static String computeExceptionResponseSubPackage(final PackageConfiguration packageConfiguration) {
        
        final String exceptionResponses = Objects.nonNull(packageConfiguration) ? packageConfiguration.getExceptions() : null;

        if (StringUtils.isNotBlank(exceptionResponses)) {
            return PackageUtils.join(exceptionResponses, GeneratorConstants.DefaultPackageLayout.RESPONSES);
        }

        return join(GeneratorConstants.DefaultPackageLayout.EXCEPTIONS, GeneratorConstants.DefaultPackageLayout.RESPONSES);
    }

    /**
     * Computes the exception handler package by joining the base package with either the user-defined exception handler package or the default exception handler package path.
     * If the user-defined exception handler package is not null or empty, it is used, otherwise the default exception handler package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed exception handler package path
     */
    public static String computeExceptionHandlerPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        
        return join(basePackage, computeExceptionHandlerSubPackage(packageConfiguration));
    }

    /**
     * Computes the exception handler sub package by joining the base package with either the user-defined exception handler package or the default exception handler sub package path.
     * If the user-defined exception handler package is not null or empty, it is used, otherwise the default exception handler sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed exception handler sub package path
     */
    public static String computeExceptionHandlerSubPackage(final PackageConfiguration packageConfiguration) {
        
        final String exceptionResponses = Objects.nonNull(packageConfiguration) ? packageConfiguration.getExceptions() : null;

        if (StringUtils.isNotBlank(exceptionResponses)) {
            return PackageUtils.join(exceptionResponses, GeneratorConstants.DefaultPackageLayout.HANDLERS);
        }

        return join(GeneratorConstants.DefaultPackageLayout.EXCEPTIONS, GeneratorConstants.DefaultPackageLayout.HANDLERS);
    }

    /**
     * Computes the enum package by joining the base package with either the user-defined enum package or the default enum package path.
     * If the user-defined enum package is not null or empty, it is used, otherwise the default enum package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed enum package path
     */
    public static String computeEnumPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        
        return join(basePackage, computeEnumSubPackage(packageConfiguration));
    }

    /**
     * Computes the enum sub package by joining the base package with either the user-defined enum package or the default enum sub package path.
     * If the user-defined enum package is not null or empty, it is used, otherwise the default enum sub package path is used.
     *
     * @param packageConfiguration the package configuration object
     * @return the computed enum sub package path
     */
    public static String computeEnumSubPackage(final PackageConfiguration packageConfiguration) {

        final String enums = Objects.nonNull(packageConfiguration) ? packageConfiguration.getEnums() : null;

        if (StringUtils.isNotBlank(enums)) {
            return enums;
        }

        return GeneratorConstants.DefaultPackageLayout.ENUMS;
    }

    /**
     * Computes the package path by joining the base package with either the user-defined annotation package or the default annotation
     * package path.
     * If the user-defined annotation package is not null or empty, it is used, otherwise the default annotation package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed package path
     */
    public static String computeAnnotationPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeAnnotationSubPackage(packageConfiguration));
    }

    /**
     * Computes the annotation sub package by joining the base package with either the user-defined annotation package or the default annotation
     * package path.
     * If the user-defined annotation package is not null or empty, it is used, otherwise the default annotation package path is used.
     *
     * @param packageConfiguration the package configuration object
     * @return the computed annotation sub package path
     */
    public static String computeAnnotationSubPackage(final PackageConfiguration packageConfiguration) {

        final String annotation = Objects.nonNull(packageConfiguration) ? packageConfiguration.getAnnotations() : null;

        if (StringUtils.isNotBlank(annotation)) {
            return annotation;
        }

        return GeneratorConstants.DefaultPackageLayout.ANNOTATIONS;
    }

    /**
     * Computes the business service package by joining the base package with either the user-defined business service package or the default business service package path.
     * If the user-defined business service package is not null or empty, it is used, otherwise the default business service package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed business service package path
     */
    public static String computeBusinessServicePackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeBusinessServiceSubPackage(packageConfiguration));
    }

    /**
     * Computes the business service sub package by joining the base package with either the user-defined business service package or the default business service package path.
     * If the user-defined business service package is not null or empty, it is used, otherwise the default business service package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed business service sub package path
     */
    public static String computeBusinessServiceSubPackage(final PackageConfiguration packageConfiguration) {

        final String businessservices = Objects.nonNull(packageConfiguration) ? packageConfiguration.getBusinessservices() : null;

        if (StringUtils.isNotBlank(businessservices)) {
            return businessservices;
        }

        return GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES;
    }

    /**
     * Validates the package configuration.
     * If the package configuration is not defined, it uses the default package structure.
     * If any of the package groups are defined, it checks if all required packages are defined.
     * If any of the required packages are missing, it throws an {@link IllegalArgumentException}.
     * If the graphQl configuration is enabled, it checks if the resolvers package is defined.
     * If the openApiCodegen configuration is enabled, it checks if the generated package is defined.
     * 
     * @param packageConfiguration the package configuration
     * @param configuration the crud configuration
     */
    public static void validate(final PackageConfiguration packageConfiguration, final CrudConfiguration configuration) {

        if (Objects.isNull(packageConfiguration)) {
            LOGGER.info("Package configuration is not defined, using default package structure");
            return;
        }

        final List<String> missing = new ArrayList<>();
        validateAdditionalPropertiesPackages(packageConfiguration, configuration, missing);

        final boolean anyGroupDefined = StringUtils.isNotBlank(packageConfiguration.getBusinessservices()) ||
                StringUtils.isNotBlank(packageConfiguration.getControllers()) ||
                StringUtils.isNotBlank(packageConfiguration.getEnums()) ||
                StringUtils.isNotBlank(packageConfiguration.getExceptions()) ||
                StringUtils.isNotBlank(packageConfiguration.getMappers()) ||
                StringUtils.isNotBlank(packageConfiguration.getModels()) ||
                StringUtils.isNotBlank(packageConfiguration.getRepositories()) ||
                StringUtils.isNotBlank(packageConfiguration.getServices()) ||
                StringUtils.isNotBlank(packageConfiguration.getTransferobjects());
    
        if (anyGroupDefined) {
            if (StringUtils.isBlank(packageConfiguration.getBusinessservices())) missing.add("bussinessservices");
            if (StringUtils.isBlank(packageConfiguration.getControllers())) missing.add("controllers");
            if (StringUtils.isBlank(packageConfiguration.getEnums())) missing.add("enums");
            if (StringUtils.isBlank(packageConfiguration.getExceptions())) missing.add("exceptions");
            if (StringUtils.isBlank(packageConfiguration.getMappers())) missing.add("mappers");
            if (StringUtils.isBlank(packageConfiguration.getModels())) missing.add("models");
            if (StringUtils.isBlank(packageConfiguration.getRepositories())) missing.add("repositories");
            if (StringUtils.isBlank(packageConfiguration.getServices())) missing.add("services");
            if (StringUtils.isBlank(packageConfiguration.getTransferobjects())) missing.add("transferobjects");
        }

        if (Boolean.TRUE.equals(configuration.getGraphQl()) && anyGroupDefined) {
            if (StringUtils.isBlank(packageConfiguration.getResolvers())) {
                missing.add("resolvers (required when graphQl is enabled)");
            }
        }

        if (Boolean.TRUE.equals(configuration.getOpenApiCodegen()) && anyGroupDefined) {
            if (StringUtils.isBlank(packageConfiguration.getGenerated())) {
                missing.add("generated (required when openApiCodegen is enabled)");
            }
        }

        if (!missing.isEmpty()) {
            final String message = String.format(
                    "Invalid package configuration. Missing required package(s): %s", String.join(", ", missing)
            );
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates the additional properties configuration.
     * If optimistic locking is enabled, all retry and backoff parameters must be defined.
     * If any of the parameters are missing, an IllegalArgumentException is thrown.
     * 
     * @param packageConfiguration the package configuration
     * @param configuration the crud configuration
     * @param missing the list of missing configurations
     */
    private static void validateAdditionalPropertiesPackages(final PackageConfiguration packageConfiguration,
            final CrudConfiguration configuration, final List<String> missing) {

        if (configuration.getOptimisticLocking() == null || !configuration.getOptimisticLocking()) {
            return;
        }
     
        final Integer maxAttempts = (Integer) configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS);
        final Integer delayMs = (Integer) configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS);
        final Integer maxDelayMs = (Integer) configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MAX_DELAY_MS);
        final Double multiplier = (Double) configuration.getAdditionalProperties()
                .get(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER);

        final List<Object> parameters = Arrays.asList(maxAttempts, delayMs, maxDelayMs, multiplier);

        if (parameters.stream().allMatch(Objects::isNull)) {
            return;
        }

        if (parameters.stream().anyMatch(Objects::isNull)) {
            missing.add("optimisticLocking.retry and optimisticLocking.backoff (all retry/backoff parameters must be defined)");
        }
    }

}
