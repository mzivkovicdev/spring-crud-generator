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

package com.markozivkovic.codegen.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.models.PackageConfiguration;

public class PackageUtils {

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
        
        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getConfigurations, GeneratorConstants.DefaultPackageLayout.CONFIGURATIONS
        );
    }

    /**
     * Computes the controller package by joining the base package with either the user-defined controller package or the default controller package path.
     * If the user-defined controller package is not null or empty, it is used, otherwise the default controller package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed controller package path
     */
    public static String computeControllerPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeControllerSubPackage(packageConfiguration));
    }

    /**
     * Computes the controller sub package by joining the base package with either the user-defined controller package or the default controller sub package path.
     * If the user-defined controller package is not null or empty, it is used, otherwise the default controller sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed controller sub package path
     */
    public static String computeControllerSubPackage(final PackageConfiguration packageConfiguration) {

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getControllers, GeneratorConstants.DefaultPackageLayout.CONTROLLERS
        );
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
        
        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getExceptions, GeneratorConstants.DefaultPackageLayout.EXCEPTIONS
        );
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

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getEnums, GeneratorConstants.DefaultPackageLayout.ENUMS
        );
    }

    /**
     * Computes the generated model package by joining the base package with the generated model sub package.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @param strippedModel        the stripped model name
     * @return the computed generated model package path
     */
    public static String computeGeneratedModelPackage(final String basePackage, final PackageConfiguration packageConfiguration,
                final String strippedModel) {
        return join(
                basePackage, computeGeneratedModelSubPackage(packageConfiguration, strippedModel)
        );
    }

    /**
     * computes the generated model sub package by joining the generated sub package with the stripped model name and the default model sub package path.
     * 
     * @param packageConfiguration the package configuration object
     * @param strippedModel        the stripped model name
     * @return the computed generated model sub package path
     */
    private static String computeGeneratedModelSubPackage(final PackageConfiguration packageConfiguration, final String strippedModel) {

        final String generated = resolveSubPackage(packageConfiguration, PackageConfiguration::getGenerated, GeneratorConstants.DefaultPackageLayout.GENERATED);

        return join(generated, strippedModel, GeneratorConstants.DefaultPackageLayout.MODEL);
    }

    /**
     * computes the generated api package by joining the base package with the generated api sub package.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @param strippedModel        the stripped model name
     * @return the computed generated api package path
     */
    public static String computeGeneratedApiPackage(final String basePackage, final PackageConfiguration packageConfiguration,
                final String strippedModel) {
        return join(
                basePackage, computeGeneratedApiSubPackage(packageConfiguration, strippedModel)
        );
    }

    /**
     * computes the generated api sub package by joining the generated sub package with the stripped model name and the default api sub package path.
     * 
     * @param packageConfiguration the package configuration object
     * @param strippedModel         the stripped model name
     * @return the computed generated api sub package path
     */
    private static String computeGeneratedApiSubPackage(final PackageConfiguration packageConfiguration, final String strippedModel) {

        final String generated = resolveSubPackage(packageConfiguration, PackageConfiguration::getGenerated, GeneratorConstants.DefaultPackageLayout.GENERATED);

        return join(generated, strippedModel, GeneratorConstants.DefaultPackageLayout.API);
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

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getAnnotations, GeneratorConstants.DefaultPackageLayout.ANNOTATIONS
        );
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

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getBusinessservices, GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES
        );
    }

    /**
     * Computes the resolvers package by joining the base package with either the user-defined resolvers package or the default resolvers package path.
     * If the user-defined resolvers package is not null or empty, it is used, otherwise the default resolvers package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed resolvers package path
     */
    public static String computeResolversPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeResolversSubPackage(packageConfiguration));
    }

    /**
     * Computes the resolvers sub package by joining the base package with either the user-defined resolvers package or the default resolvers sub package path.
     * If the user-defined resolvers package is not null or empty, it is used, otherwise the default resolvers sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed resolvers sub package path
     */
    public static String computeResolversSubPackage(final PackageConfiguration packageConfiguration) {

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getResolvers, GeneratorConstants.DefaultPackageLayout.RESOLVERS
        );
    }

    /**
     * Computes the repository package by joining the base package with either the user-defined repository package or the default repository package path.
     * If the user-defined repository package is not null or empty, it is used, otherwise the default repository package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed repository package path
     */
    public static String computeRepositoryPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeRepositorySubPackage(packageConfiguration));
    }

    /**
     * Computes the repository package by joining the base package with either the user-defined repository package or the default repository package path.
     * If the user-defined repository package is not null or empty, it is used, otherwise the default repository package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed repository package path
     */
    public static String computeRepositorySubPackage(final PackageConfiguration packageConfiguration) {

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getRepositories, GeneratorConstants.DefaultPackageLayout.REPOSITORIES
        );
    }

    /**
     * Computes the entity package by joining the base package with either the user-defined entity package or the default entity package path.
     * If the user-defined entity package is not null or empty, it is used, otherwise the default entity package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed entity package path
     */
    public static String computeEntityPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeEntitySubPackage(packageConfiguration));
    }

    /**
     * Computes the entity sub package by joining the base package with either the user-defined entity package or the default entity
     * sub package path.
     * If the user-defined entity package is not null or empty, it is used, otherwise the default entity sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed entity sub package path
     */
    public static String computeEntitySubPackage(final PackageConfiguration packageConfiguration) {

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getModels, GeneratorConstants.DefaultPackageLayout.MODELS
        );
    }

    /**
     * Computes the helper entity package by joining the base package with either the user-defined helper entity package or
     * the default helper entity package path.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed helper entity package path
     */
    public static String computeHelperEntityPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(computeEntityPackage(basePackage, packageConfiguration), GeneratorConstants.DefaultPackageLayout.HELPERS);
    }

    /**
     * Computes the transfer object package by joining the base package with either the user-defined transfer object package or the default transfer object package path.
     * If the user-defined transfer object package is not null or empty, it is used, otherwise the default transfer object package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed transfer object package path
     */
    public static String computeTransferObjectPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeTransferObjectSubPackage(packageConfiguration));
    }

    /**
     * Computes the transfer object sub package by joining the base package with either the user-defined transfer object package or the default transfer object sub package path.
     * If the user-defined transfer object package is not null or empty, it is used, otherwise the default transfer object sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed transfer object sub package path
     */
    public static String computeTransferObjectSubPackage(final PackageConfiguration packageConfiguration) {

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getTransferobjects, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
        );
    }

    /**
     * Computes the rest transfer object package by joining the base package with either the user-defined rest transfer object package or the default rest transfer object package path.
     * If the user-defined rest transfer object package is not null or empty, it is used, otherwise the default rest transfer object package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed rest transfer object package path
     */
    public static String computeRestTransferObjectPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        return join(basePackage, computeRestTransferObjectSubPackage(packageConfiguration));
    }

    /**
     * Computes the rest transfer object sub package by joining the base package with either the user-defined rest transfer object package or the default rest transfer object sub package path.
     * If the user-defined rest transfer object package is not null or empty, it is used, otherwise the default rest transfer object sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed rest transfer object sub package path
     */
    public static String computeRestTransferObjectSubPackage(final PackageConfiguration packageConfiguration) {
        return join(
                resolveSubPackage(packageConfiguration, PackageConfiguration::getTransferobjects, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS),
                GeneratorConstants.DefaultPackageLayout.REST
        );
    }

    /**
     * Computes the helper rest transfer object package by joining the base package with either the user-defined helper rest transfer object package or the default helper rest transfer object package path.
     * If the user-defined helper rest transfer object package is not null or empty, it is used, otherwise the default helper rest transfer object package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed helper rest transfer object package path
     */
    public static String computeHelperRestTransferObjectPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        return join(basePackage, computeHelperRestTransferObjectSubPackage(packageConfiguration));
    }

    /**
     * Computes the helper rest transfer object sub package by joining the rest transfer object sub package with the default helper sub package path.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed helper rest transfer object sub package path
     */
    public static String computeHelperRestTransferObjectSubPackage(final PackageConfiguration packageConfiguration) {
        return join(
                computeRestTransferObjectSubPackage(packageConfiguration),
                GeneratorConstants.DefaultPackageLayout.HELPERS
        );
    }

    /**
     * Computes the graphql transfer object package by joining the base package with either the user-defined graphql transfer object package or the default graphql transfer object package path.
     * If the user-defined graphql transfer object package is not null or empty, it is used, otherwise the default graphql transfer object package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed graphql transfer object package path
     */
    public static String computeGraphqlTransferObjectPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeGraphqlTransferObjectSubPackage(packageConfiguration));
    }

    /**
     * Computes the graphql transfer object sub package by joining the graphql sub package with either the user-defined transfer object package or the default transfer object sub package path.
     * If the user-defined transfer object package is not null or empty, it is used, otherwise the default transfer object sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed graphql transfer object sub package path
     */
    public static String computeGraphqlTransferObjectSubPackage(final PackageConfiguration packageConfiguration) {

        return join(
                resolveSubPackage(packageConfiguration, PackageConfiguration::getTransferobjects, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS),
                GeneratorConstants.DefaultPackageLayout.GRAPHQL
        );
    }

    /**
     * Computes the helper graphql transfer object sub package by joining the graphql transfer object sub package with the default helper
     * sub package path.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed helper graphql transfer object sub package path
     */
    public static String computeHelperGraphqlTransferObjectSubPackage(final PackageConfiguration packageConfiguration) {
        return join(
                computeGraphqlTransferObjectSubPackage(packageConfiguration),
                GeneratorConstants.DefaultPackageLayout.HELPERS
        );
    }

    /**
     * Computes the graphql transfer object helper package by joining the base package with the helper graphql transfer object sub package.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed graphql transfer object helper package path
     */
    public static String computeHelperGraphqlTransferObjectPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        return join(basePackage, computeHelperGraphqlTransferObjectSubPackage(packageConfiguration));
    }

    /**
     * Computes the helper entity sub package by joining the entity sub package with the default helper sub package path.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed helper entity sub package path
     */
    public static String computeHelperEntitySubPackage(final PackageConfiguration packageConfiguration) {

        return join(computeEntitySubPackage(packageConfiguration), GeneratorConstants.DefaultPackageLayout.HELPERS);
    }

    /**
     * Computes the service package by joining the base package with either the user-defined service package or the default service package path.
     * If the user-defined service package is not null or empty, it is used, otherwise the default service package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed service package path
     */
    public static String computeServicePackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        
        return join(basePackage, computeServiceSubPackage(packageConfiguration));
    }

    /**
     * Computes the service sub package by joining the base package with either the user-defined service package or the default service package path.
     * If the user-defined service package is not null or empty, it is used, otherwise the default service package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed service sub package path
     */
    public static String computeServiceSubPackage(final PackageConfiguration packageConfiguration) {

        return resolveSubPackage(
                packageConfiguration, PackageConfiguration::getServices, GeneratorConstants.DefaultPackageLayout.SERVICES
        );
    }

    /**
     * Computes the rest mapper package by joining the base package with either the user-defined rest mappers package or the default rest mappers package path.
     * If the user-defined rest mappers package is not null or empty, it is used, otherwise the default rest mappers package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed rest mapper package path
     */
    public static String computeRestMapperPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        return join(basePackage, computeRestMappersSubPackage(packageConfiguration));
    }

    /**
     * Computes the helper rest mapper package by joining the base package with either the user-defined helper rest mappers package or the default helper rest mappers package path.
     * If the user-defined helper rest mappers package is not null or empty, it is used, otherwise the default helper rest mappers package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed helper rest mapper package path
     */
    public static String computeHelperRestMapperPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        return join(basePackage, computeHelperRestMappersSubPackage(packageConfiguration));
    }

    /**
     * Computes the graphql mapper package by joining the base package with either the user-defined graphql mappers package or the default graphql mappers package path.
     * If the user-defined graphql mappers package is not null or empty, it is used, otherwise the default graphql mappers package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed graphql mapper package path
     */
    public static String computeGraphQlMapperPackage(final String basePackage, final PackageConfiguration packageConfiguration) {

        return join(basePackage, computeGraphQlMappersSubPackage(packageConfiguration));
    }

    /**
     * Computes the helper graphql mapper package by joining the base package with either the user-defined helper graphql mappers package or the default helper graphql mappers package path.
     * If the user-defined helper graphql mappers package is not null or empty, it is used, otherwise the default helper graphql mappers package path is used.
     * 
     * @param basePackage          the base package path
     * @param packageConfiguration the package configuration object
     * @return the computed helper graphql mapper package path
     */
    public static String computeHelperGraphQlMapperPackage(final String basePackage, final PackageConfiguration packageConfiguration) {
        return join(basePackage, computeHelperGraphQlMappersSubPackage(packageConfiguration));
    }

    /**
     * Computes the rest mappers sub package by joining the base package with either the user-defined mappers package or the default mappers sub package path,
     * and then joining the result with the default rest sub package path.
     * If the user-defined mappers package is not null or empty, it is used, otherwise the default mappers sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed rest mappers sub package path
     */
    public static String computeRestMappersSubPackage(final PackageConfiguration packageConfiguration) {

        return join(
                resolveSubPackage(packageConfiguration, PackageConfiguration::getMappers, GeneratorConstants.DefaultPackageLayout.MAPPERS),
                GeneratorConstants.DefaultPackageLayout.REST
        );
    }

    /**
     * Computes the graphql mappers sub package by joining the base package with either the user-defined mappers package or the default mappers sub package path,
     * and then joining the result with the default graphql sub package path.
     * If the user-defined mappers package is not null or empty, it is used, otherwise the default mappers sub package path is used.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed graphql mappers sub package path
     */
    public static String computeGraphQlMappersSubPackage(final PackageConfiguration packageConfiguration) {

        return join(
                resolveSubPackage(packageConfiguration, PackageConfiguration::getMappers, GeneratorConstants.DefaultPackageLayout.MAPPERS),
                GeneratorConstants.DefaultPackageLayout.GRAPHQL
        );
    }

    /**
     * Computes the helper rest mappers sub package by joining the rest mappers sub package with the default helper sub package path.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed helper rest mappers sub package path
     */
    public static String computeHelperRestMappersSubPackage(final PackageConfiguration packageConfiguration) {
        return join(
                computeRestMappersSubPackage(packageConfiguration),
                GeneratorConstants.DefaultPackageLayout.HELPERS
        );
    }

    /**
     * Computes the helper graphql mappers sub package by joining the graphql mappers sub package with the default helper sub package path.
     * 
     * @param packageConfiguration the package configuration object
     * @return the computed helper graphql mappers sub package path
     */
    public static String computeHelperGraphQlMappersSubPackage(final PackageConfiguration packageConfiguration) {
        return join(
                computeGraphQlMappersSubPackage(packageConfiguration),
                GeneratorConstants.DefaultPackageLayout.HELPERS
        );
    }

    /**
     * Resolves the sub package by checking if the given package configuration object is not null, and if so, applies
     * the given getter function to retrieve the sub package.
     * If the retrieved value is not null or empty, it is returned, otherwise the default sub package is returned.
     * 
     * @param pkg               the package configuration object
     * @param getter            the function to retrieve the sub package from the package configuration object
     * @param defaultSubPackage the default sub package to return if the retrieved value is null or empty
     * @return the resolved sub package path
     */
    private static String resolveSubPackage(final PackageConfiguration pkg, final Function<PackageConfiguration, String> getter,
            final String defaultSubPackage) {

        if (Objects.isNull(pkg)) {
            return defaultSubPackage;
        }

        final String value = getter.apply(pkg);

        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        return defaultSubPackage;
    }

}
