package dev.markozivkovic.springcrudgenerator.generators;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitResponseConfig;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitingConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitingConfiguration.KeyStrategyEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitingConfiguration.RateLimitTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;

class RateLimitingGeneratorTest {

    private static class CrudAndRateLimit {
        CrudConfiguration crudConfig;
        RateLimitingConfiguration rateLimitingConfig;
    }

    private CrudAndRateLimit prepareCrudWithRateLimiting() {
        final CrudAndRateLimit cr = new CrudAndRateLimit();
        cr.crudConfig = mock(CrudConfiguration.class);
        cr.rateLimitingConfig = mock(RateLimitingConfiguration.class);
        when(cr.crudConfig.getRateLimiting()).thenReturn(cr.rateLimitingConfig);
        return cr;
    }

    @Test
    void generate_shouldSkipWhenRateLimitingIsNull() {

        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        when(crudConfig.getRateLimiting()).thenReturn(null);

        final PackageConfiguration packageConfig = mock(PackageConfiguration.class);
        final RateLimitingGenerator generator = new RateLimitingGenerator(crudConfig, packageConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate("out");

            genCtx.verifyNoInteractions();
            pkg.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenRateLimitingDisabled() {

        final CrudAndRateLimit cr = prepareCrudWithRateLimiting();
        when(cr.rateLimitingConfig.getEnabled()).thenReturn(false);

        final PackageConfiguration packageConfig = mock(PackageConfiguration.class);
        final RateLimitingGenerator generator = new RateLimitingGenerator(cr.crudConfig, packageConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate("out");

            genCtx.verifyNoInteractions();
            pkg.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenAlreadyGeneratedInContext() {

        final CrudAndRateLimit cr = prepareCrudWithRateLimiting();
        when(cr.rateLimitingConfig.getEnabled()).thenReturn(true);

        final PackageConfiguration packageConfig = mock(PackageConfiguration.class);
        final RateLimitingGenerator generator = new RateLimitingGenerator(cr.crudConfig, packageConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION)).thenReturn(true);

            generator.generate("out");

            pkg.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();

            genCtx.verify(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION));
            genCtx.verifyNoMoreInteractions();
        }
    }

    @Test
    void generate_shouldGenerateServiceAndFilter_forInMemoryType_withDefaultValues() {

        final CrudAndRateLimit cr = prepareCrudWithRateLimiting();
        when(cr.rateLimitingConfig.getEnabled()).thenReturn(true);
        when(cr.rateLimitingConfig.getType()).thenReturn(RateLimitTypeEnum.IN_MEMORY);
        when(cr.rateLimitingConfig.getKeyStrategy()).thenReturn(KeyStrategyEnum.IP);
        when(cr.rateLimitingConfig.getKeyHeader()).thenReturn(null);
        when(cr.rateLimitingConfig.getGlobal()).thenReturn(null);
        when(cr.rateLimitingConfig.getResponse()).thenReturn(null);
        when(cr.crudConfig.getSpringBootVersion()).thenReturn("3.3.0");

        final PackageConfiguration packageConfig = mock(PackageConfiguration.class);
        final RateLimitingGenerator generator = new RateLimitingGenerator(cr.crudConfig, packageConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.3.0")).thenReturn(true);
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example", packageConfig)).thenReturn("com.example.configurations");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(packageConfig)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/rate-limiter-service.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return map.get("type") == RateLimitTypeEnum.IN_MEMORY
                                && map.get("keyStrategy") == KeyStrategyEnum.IP
                                && Objects.equals(map.get("capacity"), 100L)
                                && Objects.equals(map.get("refillTokens"), 100L)
                                && Objects.equals(map.get("refillDuration"), 60L)
                                && Objects.equals(map.get("hasOverdraft"), false)
                                && Objects.equals(map.get("statusCode"), 429)
                                && Objects.equals(map.get("includeHeaders"), true)
                                && Objects.equals(map.get(TemplateContextConstants.IS_SPRING_BOOT_3), true);
                    })
            ));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/rate-limiting-filter.ftl"), anyMap()));

            // No Redis config for IN_MEMORY
            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/redis-rate-limiter-configuration.ftl"), anyMap()), never());

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("RateLimiterService.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("RateLimitingFilter.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("RedisRateLimiterConfiguration.java"), anyString()), never());

            genCtx.verify(() -> GeneratorContext.markGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldGenerateServiceFilterAndRedisConfig_forRedisType_withExplicitValues() {

        final RateLimitDefinition globalDef = mock(RateLimitDefinition.class);
        when(globalDef.getCapacity()).thenReturn(200L);
        when(globalDef.getRefillTokens()).thenReturn(200L);
        when(globalDef.getRefillDuration()).thenReturn(30L);
        when(globalDef.getOverdraft()).thenReturn(null);

        final RateLimitResponseConfig responseConfig = mock(RateLimitResponseConfig.class);
        when(responseConfig.getStatusCode()).thenReturn(429);
        when(responseConfig.getIncludeHeaders()).thenReturn(true);
        when(responseConfig.getMessage()).thenReturn("Too many requests.");

        final CrudAndRateLimit cr = prepareCrudWithRateLimiting();
        when(cr.rateLimitingConfig.getEnabled()).thenReturn(true);
        when(cr.rateLimitingConfig.getType()).thenReturn(RateLimitTypeEnum.REDIS);
        when(cr.rateLimitingConfig.getKeyStrategy()).thenReturn(KeyStrategyEnum.API_KEY);
        when(cr.rateLimitingConfig.getKeyHeader()).thenReturn(null);
        when(cr.rateLimitingConfig.getGlobal()).thenReturn(globalDef);
        when(cr.rateLimitingConfig.getResponse()).thenReturn(responseConfig);
        when(cr.crudConfig.getSpringBootVersion()).thenReturn("4.0.0");

        final PackageConfiguration packageConfig = mock(PackageConfiguration.class);
        final RateLimitingGenerator generator = new RateLimitingGenerator(cr.crudConfig, packageConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("4.0.0")).thenReturn(false);
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example", packageConfig)).thenReturn("com.example.configurations");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(packageConfig)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/rate-limiter-service.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return map.get("type") == RateLimitTypeEnum.REDIS
                                && map.get("keyStrategy") == KeyStrategyEnum.API_KEY
                                && Objects.equals(map.get("capacity"), 200L)
                                && Objects.equals(map.get("refillTokens"), 200L)
                                && Objects.equals(map.get("refillDuration"), 30L)
                                && Objects.equals(map.get("hasOverdraft"), false)
                                && Objects.equals(map.get(TemplateContextConstants.IS_SPRING_BOOT_3), false);
                    })
            ));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/rate-limiting-filter.ftl"), anyMap()));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/redis-rate-limiter-configuration.ftl"), anyMap()));

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("RateLimiterService.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("RateLimitingFilter.java"), anyString()));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"), eq("configurations"), eq("RedisRateLimiterConfiguration.java"), anyString()));

            genCtx.verify(() -> GeneratorContext.markGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldIncludeOverdraftValues_whenOverdraftIsConfigured() {

        final RateLimitDefinition overdraft = mock(RateLimitDefinition.class);
        when(overdraft.getCapacity()).thenReturn(50L);
        when(overdraft.getRefillTokens()).thenReturn(50L);
        when(overdraft.getRefillDuration()).thenReturn(10L);

        final RateLimitDefinition globalDef = mock(RateLimitDefinition.class);
        when(globalDef.getCapacity()).thenReturn(1000L);
        when(globalDef.getRefillTokens()).thenReturn(1000L);
        when(globalDef.getRefillDuration()).thenReturn(60L);
        when(globalDef.getOverdraft()).thenReturn(overdraft);

        final CrudAndRateLimit cr = prepareCrudWithRateLimiting();
        when(cr.rateLimitingConfig.getEnabled()).thenReturn(true);
        when(cr.rateLimitingConfig.getType()).thenReturn(RateLimitTypeEnum.IN_MEMORY);
        when(cr.rateLimitingConfig.getKeyStrategy()).thenReturn(KeyStrategyEnum.IP);
        when(cr.rateLimitingConfig.getKeyHeader()).thenReturn(null);
        when(cr.rateLimitingConfig.getGlobal()).thenReturn(globalDef);
        when(cr.rateLimitingConfig.getResponse()).thenReturn(null);
        when(cr.crudConfig.getSpringBootVersion()).thenReturn("3.3.0");

        final PackageConfiguration packageConfig = mock(PackageConfiguration.class);
        final RateLimitingGenerator generator = new RateLimitingGenerator(cr.crudConfig, packageConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.3.0")).thenReturn(true);
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example", packageConfig)).thenReturn("com.example.configurations");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(packageConfig)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/rate-limiter-service.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return Objects.equals(map.get("hasOverdraft"), true)
                                && Objects.equals(map.get("overdraftCapacity"), 50L)
                                && Objects.equals(map.get("overdraftRefillTokens"), 50L)
                                && Objects.equals(map.get("overdraftRefillDuration"), 10L);
                    })
            ));

            genCtx.verify(() -> GeneratorContext.markGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldUseDefaultValues_whenGlobalConfigHasNullFields() {

        final RateLimitDefinition globalDef = mock(RateLimitDefinition.class);
        when(globalDef.getCapacity()).thenReturn(null);
        when(globalDef.getRefillTokens()).thenReturn(null);
        when(globalDef.getRefillDuration()).thenReturn(null);
        when(globalDef.getOverdraft()).thenReturn(null);

        final CrudAndRateLimit cr = prepareCrudWithRateLimiting();
        when(cr.rateLimitingConfig.getEnabled()).thenReturn(true);
        when(cr.rateLimitingConfig.getType()).thenReturn(null);
        when(cr.rateLimitingConfig.getKeyStrategy()).thenReturn(null);
        when(cr.rateLimitingConfig.getKeyHeader()).thenReturn(null);
        when(cr.rateLimitingConfig.getGlobal()).thenReturn(globalDef);
        when(cr.rateLimitingConfig.getResponse()).thenReturn(null);
        when(cr.crudConfig.getSpringBootVersion()).thenReturn("3.3.0");

        final PackageConfiguration packageConfig = mock(PackageConfiguration.class);
        final RateLimitingGenerator generator = new RateLimitingGenerator(cr.crudConfig, packageConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<SpringBootVersionUtils> sbv = mockStatic(SpringBootVersionUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION)).thenReturn(false);
            sbv.when(() -> SpringBootVersionUtils.isSpringBoot3("3.3.0")).thenReturn(true);
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example", packageConfig)).thenReturn("com.example.configurations");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(packageConfig)).thenReturn("configurations");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("// GENERATED");

            generator.generate("out");

            // type defaults to IN_MEMORY, keyStrategy to IP, values to defaults
            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/rate-limiter-service.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return map.get("type") == RateLimitTypeEnum.IN_MEMORY
                                && map.get("keyStrategy") == KeyStrategyEnum.IP
                                && Objects.equals(map.get("capacity"), 100L)
                                && Objects.equals(map.get("refillTokens"), 100L)
                                && Objects.equals(map.get("refillDuration"), 60L)
                                && Objects.equals(map.get("statusCode"), 429)
                                && Objects.equals(map.get("includeHeaders"), true)
                                && Objects.equals(map.get("keyHeader"), "X-Client-Id");
                    })
            ));

            // No Redis config for IN_MEMORY (default)
            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("ratelimiting/redis-rate-limiter-configuration.ftl"), anyMap()), never());
        }
    }
}
