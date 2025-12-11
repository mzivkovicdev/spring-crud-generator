package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import com.markozivkovic.codegen.constants.AdditionalConfigurationConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.ContainerUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

class AdditionalPropertyGeneratorTest {

    private static class Env {
        CrudConfiguration config;
        PackageConfiguration pkgConfig;
        Map<String, Object> additionalProps;
        AdditionalPropertyGenerator generator;
    }

    private Env prepareEnv() {
        
        final Env env = new Env();
        env.config = mock(CrudConfiguration.class);
        env.pkgConfig = mock(PackageConfiguration.class);
        env.additionalProps = new HashMap<>();

        when(env.config.getAdditionalProperties()).thenReturn(env.additionalProps);
        when(env.config.getOptimisticLocking()).thenReturn(false);
        when(env.config.getGraphQl()).thenReturn(false);

        env.generator = new AdditionalPropertyGenerator(env.config, env.pkgConfig);
        return env;
    }

    @Test
    void generate_shouldReturnWhenAdditionalPropertiesEmpty() {
        
        final Env env = prepareEnv();

        try (final MockedStatic<ContainerUtils> cont = mockStatic(ContainerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            cont.when(() -> ContainerUtils.isEmpty(env.additionalProps))
                    .thenReturn(true);

            env.generator.generate("out");

            cont.verify(() -> ContainerUtils.isEmpty(env.additionalProps));
            genCtx.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldReturnWhenAdditionalConfigAlreadyGenerated() {
        
        final Env env = prepareEnv();
        env.additionalProps.put("something", true);

        try (final MockedStatic<ContainerUtils> cont = mockStatic(ContainerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            cont.when(() -> ContainerUtils.isEmpty(env.additionalProps))
                    .thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ADDITIONAL_CONFIG))
                    .thenReturn(true);

            env.generator.generate("out");

            genCtx.verify(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ADDITIONAL_CONFIG));
            genCtx.verifyNoMoreInteractions();
        }
    }

    @Test
    void generate_shouldGenerateGraphqlRetryConfigAndRetryableAnnotation() {
        
        final Env env = prepareEnv();

        env.additionalProps.put(AdditionalConfigurationConstants.GRAPHQL_SCALAR_CONFIG, true);
        env.additionalProps.put(AdditionalConfigurationConstants.OPT_LOCK_RETRY_CONFIGURATION, true);
        env.additionalProps.put(AdditionalConfigurationConstants.OPT_LOCK_MAX_ATTEMPTS, 5);
        env.additionalProps.put(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_DELAY_MS, 100);
        env.additionalProps.put(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MAX_DELAY_MS, 1000);
        env.additionalProps.put(AdditionalConfigurationConstants.OPT_LOCK_BACKOFF_MULTIPLIER, 2.0);

        when(env.config.getGraphQl()).thenReturn(true);
        when(env.config.getOptimisticLocking()).thenReturn(true);

        final List<InvocationOnMock> templateInvocations = new ArrayList<>();
        final List<InvocationOnMock> writerInvocations = new ArrayList<>();

        try (final MockedStatic<ContainerUtils> cont = mockStatic(ContainerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl =
                     mockStatic(FreeMarkerTemplateProcessorUtils.class, invocation -> {
                         templateInvocations.add(invocation);
                         if ("processTemplate".equals(invocation.getMethod().getName())) {
                             String templateName = invocation.getArgument(0, String.class);
                             return "TEMPLATE-" + templateName;
                         }
                         return null;
                     });
             final MockedStatic<FileWriterUtils> writer =
                     mockStatic(FileWriterUtils.class, invocation -> {
                         writerInvocations.add(invocation);
                         return null;
                     })) {

            cont.when(() -> ContainerUtils.isEmpty(env.additionalProps))
                    .thenReturn(false);

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ADDITIONAL_CONFIG))
                    .thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.GRAPHQL_CONFIGURATION))
                    .thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.OPTIMISTIC_LOCKING_RETRY))
                    .thenReturn(false);
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example.app", env.pkgConfig))
                    .thenReturn("com.example.app.config");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(env.pkgConfig))
                    .thenReturn("config");
            pkg.when(() -> PackageUtils.computeAnnotationPackage("com.example.app", env.pkgConfig))
                    .thenReturn("com.example.app.annotation");
            pkg.when(() -> PackageUtils.computeAnnotationSubPackage(env.pkgConfig))
                    .thenReturn("annotation");

            env.generator.generate("out");

            final boolean scalarTemplateUsed = templateInvocations.stream()
                    .anyMatch(inv -> "processTemplate".equals(inv.getMethod().getName())
                            && "configuration/scalar-configuration.ftl".equals(inv.getArgument(0)));

            final boolean retryConfigTemplateUsed = templateInvocations.stream()
                    .anyMatch(inv -> "processTemplate".equals(inv.getMethod().getName())
                            && "configuration/retry-configuration.ftl".equals(inv.getArgument(0)));

            final AtomicReference<Map<String, Object>> retryableCtxRef = new AtomicReference<>();
            final boolean retryableTemplateUsed = templateInvocations.stream()
                    .filter(inv -> "processTemplate".equals(inv.getMethod().getName())
                            && "annotation/retryable-annotation.ftl".equals(inv.getArgument(0)))
                    .peek(inv -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ctx = inv.getArgument(1, Map.class);
                        retryableCtxRef.set(ctx);
                    })
                    .findAny()
                    .isPresent();

            assertTrue(scalarTemplateUsed, "Scalar configuration template should be used");
            assertTrue(retryConfigTemplateUsed, "Retry configuration template should be used");
            assertTrue(retryableTemplateUsed, "Retryable annotation template should be used");

            final Map<String, Object> retryableCtx = retryableCtxRef.get();
            assertNotNull(retryableCtx);
            assertEquals(5, retryableCtx.get("maxAttempts"));
            assertEquals(100, retryableCtx.get("delayMs"));
            assertEquals(1000, retryableCtx.get("maxDelayMs"));
            assertEquals(2.0, retryableCtx.get("multiplier"));

            final boolean wroteGraphQlConfig = writerInvocations.stream()
                    .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                            && "out".equals(inv.getArgument(0))
                            && "config".equals(inv.getArgument(1))
                            && "GraphQlConfiguration.java".equals(inv.getArgument(2)));

            final boolean wroteRetryConfig = writerInvocations.stream()
                    .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                            && "out".equals(inv.getArgument(0))
                            && "config".equals(inv.getArgument(1))
                            && "EnableRetryConfiguration.java".equals(inv.getArgument(2)));

            final boolean wroteRetryableAnnotation = writerInvocations.stream()
                    .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                            && "out".equals(inv.getArgument(0))
                            && "annotation".equals(inv.getArgument(1))
                            && "OptimisticLockingRetry.java".equals(inv.getArgument(2)));

            assertTrue(wroteGraphQlConfig, "GraphQlConfiguration.java should be written");
            assertTrue(wroteRetryConfig, "EnableRetryConfiguration.java should be written");
            assertTrue(wroteRetryableAnnotation, "OptimisticLockingRetry.java should be written");
        }
    }

    @Test
    void generate_shouldNotGenerateRetryableAnnotationWhenParamsMissing() {
        
        final Env env = prepareEnv();

        env.additionalProps.put(AdditionalConfigurationConstants.GRAPHQL_SCALAR_CONFIG, true);
        env.additionalProps.put(AdditionalConfigurationConstants.OPT_LOCK_RETRY_CONFIGURATION, true);

        when(env.config.getGraphQl()).thenReturn(true);
        when(env.config.getOptimisticLocking()).thenReturn(true);

        final List<InvocationOnMock> templateInvocations = new ArrayList<>();
        final List<InvocationOnMock> writerInvocations = new ArrayList<>();

        try (final MockedStatic<ContainerUtils> cont = mockStatic(ContainerUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl =
                     mockStatic(FreeMarkerTemplateProcessorUtils.class, invocation -> {
                         templateInvocations.add(invocation);
                         if ("processTemplate".equals(invocation.getMethod().getName())) {
                             return "TEMPLATE-" + invocation.getArgument(0, String.class);
                         }
                         return null;
                     });
            final MockedStatic<FileWriterUtils> writer =
                     mockStatic(FileWriterUtils.class, invocation -> {
                         writerInvocations.add(invocation);
                         return null;
                     })) {

            cont.when(() -> ContainerUtils.isEmpty(env.additionalProps))
                    .thenReturn(false);

            genCtx.when(() -> GeneratorContext.isGenerated(any()))
                    .thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example.app", env.pkgConfig))
                    .thenReturn("com.example.app.config");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(env.pkgConfig))
                    .thenReturn("config");
            pkg.when(() -> PackageUtils.computeAnnotationPackage("com.example.app", env.pkgConfig))
                    .thenReturn("com.example.app.annotation");
            pkg.when(() -> PackageUtils.computeAnnotationSubPackage(env.pkgConfig))
                    .thenReturn("annotation");

            env.generator.generate("out");

            final boolean retryableTemplateUsed = templateInvocations.stream()
                    .anyMatch(inv -> "processTemplate".equals(inv.getMethod().getName())
                            && "annotation/retryable-annotation.ftl".equals(inv.getArgument(0)));

            final boolean wroteRetryableAnnotation = writerInvocations.stream()
                    .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                            && "OptimisticLockingRetry.java".equals(inv.getArgument(2)));

            assertFalse(retryableTemplateUsed, "Retryable template should NOT be used when params missing");
            assertFalse(wroteRetryableAnnotation, "OptimisticLockingRetry.java should NOT be written when params missing");
        }
    }
}
