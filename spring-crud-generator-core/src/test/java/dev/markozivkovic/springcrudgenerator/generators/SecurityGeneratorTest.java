package dev.markozivkovic.springcrudgenerator.generators;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.ApiKeyConfig;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.ApiKeyEntry;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.BasicAuthConfig;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.BasicAuthUser;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.JwtConfig;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.OAuth2Config;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.SecurityConfiguration.SecurityTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;

class SecurityGeneratorTest {

    private static CrudConfiguration mockSecurityEnabled(final SecurityTypeEnum type) {
        final CrudConfiguration config = mock(CrudConfiguration.class);
        final SecurityConfiguration security = mock(SecurityConfiguration.class);
        when(config.getSecurity()).thenReturn(security);
        when(security.getEnabled()).thenReturn(true);
        when(security.getType()).thenReturn(type);
        return config;
    }

    @Test
    void generate_shouldSkipWhenSecurityIsNull() {

        final CrudConfiguration config = mock(CrudConfiguration.class);
        when(config.getSecurity()).thenReturn(null);

        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate("out");

            genCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenSecurityDisabled() {

        final CrudConfiguration config = mock(CrudConfiguration.class);
        final SecurityConfiguration security = mock(SecurityConfiguration.class);
        when(config.getSecurity()).thenReturn(security);
        when(security.getEnabled()).thenReturn(false);

        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate("out");

            genCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenAlreadyGeneratedInContext() {

        final CrudConfiguration config = mockSecurityEnabled(SecurityTypeEnum.BASIC_AUTH);
        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION)).thenReturn(true);

            generator.generate("out");

            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();

            genCtx.verify(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION));
            genCtx.verifyNoMoreInteractions();
        }
    }

    @Test
    void generate_shouldGenerateSecurityConfiguration_forBasicAuth() {

        final CrudConfiguration config = mockSecurityEnabled(SecurityTypeEnum.BASIC_AUTH);
        final SecurityConfiguration security = config.getSecurity();
        final BasicAuthConfig basicAuth = mock(BasicAuthConfig.class);
        final BasicAuthUser user = new BasicAuthUser("admin", "secret", List.of("ADMIN"));
        when(security.getBasicAuth()).thenReturn(basicAuth);
        when(basicAuth.getUsers()).thenReturn(List.of(user));
        when(config.getSpringBootVersion()).thenReturn("3.2.0");

        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.2.0")).thenReturn(true);
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkgUtils.when(() -> PackageUtils.computeConfigurationPackage("com.example", pkg)).thenReturn("com.example.configurations");
            pkgUtils.when(() -> PackageUtils.computeConfigurationSubPackage(pkg)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("security/security-configuration.ftl"), anyMap()))
                    .thenReturn("// SECURITY_CONFIG");

            generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("security/security-configuration.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return map.get("type") == SecurityTypeEnum.BASIC_AUTH
                                && Objects.equals(map.get(TemplateContextConstants.IS_SPRING_BOOT_3), true);
                    })
            ));

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("SecurityConfiguration.java"),
                    argThat(content -> content.contains("// SECURITY_CONFIG"))
            ));

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtTokenProvider.java"), anyString()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("ApiKeyAuthenticationFilter.java"), anyString()), never());

            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldGenerateAllJwtArtifacts() {

        final CrudConfiguration config = mockSecurityEnabled(SecurityTypeEnum.JWT);
        final SecurityConfiguration security = config.getSecurity();
        final JwtConfig jwtConfig = new JwtConfig("mysecret", 86400000L, "myapp");
        when(security.getJwt()).thenReturn(jwtConfig);
        when(config.getSpringBootVersion()).thenReturn("3.3.0");

        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.3.0")).thenReturn(true);
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkgUtils.when(() -> PackageUtils.computeConfigurationPackage("com.example", pkg)).thenReturn("com.example.configurations");
            pkgUtils.when(() -> PackageUtils.computeConfigurationSubPackage(pkg)).thenReturn("configurations");
            pkgUtils.when(() -> PackageUtils.computeControllerPackage("com.example", pkg)).thenReturn("com.example.controllers");
            pkgUtils.when(() -> PackageUtils.computeControllerSubPackage(pkg)).thenReturn("controllers");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("SecurityConfiguration.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtTokenProvider.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtAuthenticationFilter.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("UserDetailsServiceImpl.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("AuthRequest.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("AuthResponse.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("AuthController.java"), anyString()));

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtRoleConverter.java"), anyString()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("ApiKeyAuthenticationFilter.java"), anyString()), never());

            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldGenerateJwtRoleConverter_forOAuth2ResourceServer() {

        final CrudConfiguration config = mockSecurityEnabled(SecurityTypeEnum.OAUTH2_RESOURCE_SERVER);
        final SecurityConfiguration security = config.getSecurity();
        final OAuth2Config oauth2 = new OAuth2Config("https://auth.example.com", null, "roles");
        when(security.getOauth2()).thenReturn(oauth2);
        when(config.getSpringBootVersion()).thenReturn("3.2.0");

        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.2.0")).thenReturn(true);
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkgUtils.when(() -> PackageUtils.computeConfigurationPackage("com.example", pkg)).thenReturn("com.example.configurations");
            pkgUtils.when(() -> PackageUtils.computeConfigurationSubPackage(pkg)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("SecurityConfiguration.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("JwtRoleConverter.java"),
                    argThat(c -> c.contains("// GENERATED"))
            ));

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtTokenProvider.java"), anyString()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("ApiKeyAuthenticationFilter.java"), anyString()), never());

            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldGenerateApiKeyArtifacts() {

        final CrudConfiguration config = mockSecurityEnabled(SecurityTypeEnum.API_KEY);
        final SecurityConfiguration security = config.getSecurity();
        final ApiKeyEntry entry = new ApiKeyEntry("service-a", "key-abc-123", List.of("ADMIN"));
        final ApiKeyConfig apiKey = new ApiKeyConfig("X-API-Key", List.of(entry));
        when(security.getApiKey()).thenReturn(apiKey);
        when(config.getSpringBootVersion()).thenReturn("3.2.0");

        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.2.0")).thenReturn(true);
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkgUtils.when(() -> PackageUtils.computeConfigurationPackage("com.example", pkg)).thenReturn("com.example.configurations");
            pkgUtils.when(() -> PackageUtils.computeConfigurationSubPackage(pkg)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("SecurityConfiguration.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("ApiKeyAuthenticationToken.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("ApiKeyAuthenticationFilter.java"),
                    argThat(c -> c.contains("// GENERATED"))
            ));

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtTokenProvider.java"), anyString()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtRoleConverter.java"), anyString()), never());

            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldUseDefaultBasicAuthType_whenTypeIsNull() {

        final CrudConfiguration config = mock(CrudConfiguration.class);
        final SecurityConfiguration security = mock(SecurityConfiguration.class);
        when(config.getSecurity()).thenReturn(security);
        when(security.getEnabled()).thenReturn(true);
        when(security.getType()).thenReturn(null);  // null → defaults to BASIC_AUTH
        when(security.getBasicAuth()).thenReturn(null);
        when(config.getSpringBootVersion()).thenReturn("3.2.0");

        final PackageConfiguration pkg = mock(PackageConfiguration.class);
        final SecurityGenerator generator = new SecurityGenerator(config, pkg);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SECURITY_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.2.0")).thenReturn(true);
            pkgUtils.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkgUtils.when(() -> PackageUtils.computeConfigurationPackage("com.example", pkg)).thenReturn("com.example.configurations");
            pkgUtils.when(() -> PackageUtils.computeConfigurationSubPackage(pkg)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("SecurityConfiguration.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtTokenProvider.java"), anyString()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("JwtRoleConverter.java"), anyString()), never());
            writer.verify(() -> FileWriterUtils.writeToFile(eq("out"), anyString(), eq("ApiKeyAuthenticationFilter.java"), anyString()), never());

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("security/security-configuration.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return map.get("type") == SecurityTypeEnum.BASIC_AUTH;
                    })
            ));
        }
    }
}
