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

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.imports.SecurityImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.SecurityTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;

public class SecurityGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGenerator.class);

    private final CrudConfiguration crudConfiguration;
    private final PackageConfiguration packageConfiguration;

    public SecurityGenerator(final CrudConfiguration crudConfiguration,
            final PackageConfiguration packageConfiguration) {
        this.crudConfiguration = crudConfiguration;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final String outputDir) {

        final SecurityConfiguration security = crudConfiguration.getSecurity();
        if (Objects.isNull(security) || !Boolean.TRUE.equals(security.getEnabled())) {
            LOGGER.info("Skipping SecurityGenerator, as security is not enabled.");
            return;
        }

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION)) {
            return;
        }

        final SecurityTypeEnum type = Objects.nonNull(security.getType()) ? security.getType() : SecurityTypeEnum.BASIC_AUTH;
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final boolean isSpringBoot3 = SpringBootVersionUtils.isSpringBoot3(crudConfiguration.getSpringBootVersion());

        LOGGER.info("Generating security configuration for type: {}", type);

        generateSecurityConfiguration(outputDir, packagePath, type, security, isSpringBoot3);

        switch (type) {
            case JWT:
                generateJwtTokenProvider(outputDir, packagePath, security, isSpringBoot3);
                generateJwtAuthenticationFilter(outputDir, packagePath, isSpringBoot3);
                generateUserDetailsService(outputDir, packagePath, isSpringBoot3);
                generateAuthRequest(outputDir, packagePath, isSpringBoot3);
                generateAuthResponse(outputDir, packagePath, isSpringBoot3);
                generateAuthController(outputDir, packagePath, isSpringBoot3);
                break;
            case OAUTH2_RESOURCE_SERVER:
                generateJwtRoleConverter(outputDir, packagePath, security, isSpringBoot3);
                break;
            case API_KEY:
                generateApiKeyAuthenticationToken(outputDir, packagePath, isSpringBoot3);
                generateApiKeyAuthenticationFilter(outputDir, packagePath, security, isSpringBoot3);
                break;
            case BASIC_AUTH:
            default:
                break;
        }

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION);
    }

    /**
     * Generates the main security configuration class for the selected security type.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param type          selected security type
     * @param security      security configuration section from the CRUD specification
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateSecurityConfiguration(final String outputDir, final String packagePath,
            final SecurityTypeEnum type, final SecurityConfiguration security, final boolean isSpringBoot3) {

        final String imports;
        switch (type) {
            case JWT:
                imports = SecurityImports.getJwtSecurityConfigImports();
                break;
            case OAUTH2_RESOURCE_SERVER:
                imports = SecurityImports.getOAuth2SecurityConfigImports();
                break;
            case API_KEY:
                imports = SecurityImports.getApiKeySecurityConfigImports();
                break;
            case BASIC_AUTH:
            default:
                imports = SecurityImports.getBasicAuthSecurityConfigImports();
                break;
        }

        final Map<String, Object> context = new HashMap<>();
        context.put("type", type);
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", imports);

        if (SecurityTypeEnum.BASIC_AUTH.equals(type)) {
            context.put("users", Objects.nonNull(security.getBasicAuth()) ? security.getBasicAuth().getUsers() : null);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/security-configuration.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "SecurityConfiguration.java", sb.toString()
        );
    }

    /**
     * Generates the JWT token provider class.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param security      security configuration section from the CRUD specification
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateJwtTokenProvider(final String outputDir, final String packagePath,
            final SecurityConfiguration security, final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", SecurityImports.getJwtTokenProviderImports());

        if (Objects.nonNull(security.getJwt())) {
            context.put("jwtSecret", security.getJwt().getSecret());
            context.put("jwtExpirationMs", security.getJwt().getExpirationMs());
            context.put("jwtIssuer", security.getJwt().getIssuer());
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/jwt-token-provider.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "JwtTokenProvider.java", sb.toString()
        );
    }

    /**
     * Generates the JWT authentication filter class.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateJwtAuthenticationFilter(final String outputDir, final String packagePath,
            final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", SecurityImports.getJwtAuthenticationFilterImports());

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/jwt-authentication-filter.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "JwtAuthenticationFilter.java", sb.toString()
        );
    }

    /**
     * Generates the user details service implementation used by JWT authentication flow.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateUserDetailsService(final String outputDir, final String packagePath,
            final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", SecurityImports.getUserDetailsServiceImports());

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/user-details-service.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "UserDetailsServiceImpl.java", sb.toString()
        );
    }

    /**
     * Generates the authentication request DTO.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateAuthRequest(final String outputDir, final String packagePath,
            final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/auth-request.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "AuthRequest.java", sb.toString()
        );
    }

    /**
     * Generates the authentication response DTO.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateAuthResponse(final String outputDir, final String packagePath,
            final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/auth-response.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "AuthResponse.java", sb.toString()
        );
    }

    /**
     * Generates the authentication controller used in JWT mode.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateAuthController(final String outputDir, final String packagePath,
            final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", SecurityImports.getAuthControllerImports());

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeControllerPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/auth-controller.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeControllerSubPackage(packageConfiguration), "AuthController.java", sb.toString()
        );
    }

    /**
     * Generates the JWT role converter used for OAuth2 resource server setup.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param security      security configuration section from the CRUD specification
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateJwtRoleConverter(final String outputDir, final String packagePath,
            final SecurityConfiguration security, final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", SecurityImports.getJwtRoleConverterImports());

        if (Objects.nonNull(security.getOauth2()) && Objects.nonNull(security.getOauth2().getRolesClaim())) {
            context.put("rolesClaim", security.getOauth2().getRolesClaim());
        } else {
            context.put("rolesClaim", "realm_access.roles");
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/jwt-role-converter.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "JwtRoleConverter.java", sb.toString()
        );
    }

    /**
     * Generates the API key authentication token class.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateApiKeyAuthenticationToken(final String outputDir, final String packagePath,
            final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", SecurityImports.getApiKeyTokenImports());

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/api-key-authentication-token.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "ApiKeyAuthenticationToken.java", sb.toString()
        );
    }

    /**
     * Generates the API key authentication filter class.
     * 
     * @param outputDir     output directory where generated artifacts are written
     * @param packagePath   base package path resolved from the output directory
     * @param security      security configuration section from the CRUD specification
     * @param isSpringBoot3 whether generated imports should target Spring Boot 3 variants
     */
    private void generateApiKeyAuthenticationFilter(final String outputDir, final String packagePath,
            final SecurityConfiguration security, final boolean isSpringBoot3) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);
        context.put("imports", SecurityImports.getApiKeyFilterImports());

        if (Objects.nonNull(security.getApiKey())) {
            context.put("headerName", Objects.nonNull(security.getApiKey().getHeaderName())
                    ? security.getApiKey().getHeaderName() : "X-API-Key");
            context.put("apiKeys", security.getApiKey().getKeys());
        } else {
            context.put("headerName", "X-API-Key");
            context.put("apiKeys", null);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate("security/api-key-authentication-filter.ftl", context));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "ApiKeyAuthenticationFilter.java", sb.toString()
        );
    }
}