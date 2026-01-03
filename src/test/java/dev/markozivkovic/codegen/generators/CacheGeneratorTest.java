package dev.markozivkovic.codegen.generators;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.imports.ConfigurationImports;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

class CacheGeneratorTest {

    private static class CrudAndCache {
        CrudConfiguration crudConfig;
        CacheConfiguration cacheConfig;
    }

    private CrudAndCache prepareCrudWithCache() {
        final CrudAndCache cc = new CrudAndCache();
        cc.crudConfig = mock(CrudConfiguration.class);
        cc.cacheConfig = mock(CacheConfiguration.class);
        when(cc.crudConfig.getCache()).thenReturn(cc.cacheConfig);
        return cc;
    }

    private static ModelDefinition model(final String name, final String storageName) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getStorageName()).thenReturn(storageName);
        return m;
    }

    @Test
    void generate_shouldSkipWhenCacheIsNull() {

        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        when(crudConfig.getCache()).thenReturn(null);

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final CacheGenerator generator = new CacheGenerator(crudConfig, packageConfiguration, Collections.emptyList());

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ConfigurationImports> imports = mockStatic(ConfigurationImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate("out");

            genCtx.verifyNoInteractions();
            pkg.verifyNoInteractions();
            imports.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenCacheDisabled() {

        final CrudAndCache cc = prepareCrudWithCache();
        when(cc.cacheConfig.getEnabled()).thenReturn(false);

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final CacheGenerator generator = new CacheGenerator(cc.crudConfig, packageConfiguration, Collections.emptyList());

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ConfigurationImports> imports = mockStatic(ConfigurationImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate("out");

            genCtx.verifyNoInteractions();
            pkg.verifyNoInteractions();
            imports.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenAlreadyGeneratedInContext() {

        final CrudAndCache cc = prepareCrudWithCache();
        when(cc.cacheConfig.getEnabled()).thenReturn(true);

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final CacheGenerator generator = new CacheGenerator(cc.crudConfig, packageConfiguration, Collections.emptyList());

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ConfigurationImports> imports = mockStatic(ConfigurationImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION)).thenReturn(true);

            generator.generate("out");

            pkg.verifyNoInteractions();
            imports.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();

            genCtx.verify(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION));
            genCtx.verifyNoMoreInteractions();
        }
    }

    @Test
    void generate_shouldGenerateCacheConfigurationWithExplicitValues() {

        final CrudAndCache cc = prepareCrudWithCache();
        when(cc.cacheConfig.getEnabled()).thenReturn(true);
        when(cc.cacheConfig.getType()).thenReturn(CacheTypeEnum.CAFFEINE);
        when(cc.cacheConfig.getMaxSize()).thenReturn(100L);
        when(cc.cacheConfig.getExpiration()).thenReturn(3600);

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);

        final List<ModelDefinition> entities = List.of(
                model("Product", "product"),
                model("Ignored", null)
        );

        final CacheGenerator generator = new CacheGenerator(cc.crudConfig, packageConfiguration, entities);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ConfigurationImports> imports = mockStatic(ConfigurationImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION)).thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example.app", packageConfiguration)).thenReturn("com.example.app.config");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(packageConfiguration)).thenReturn("config");

            imports.when(() -> ConfigurationImports.getModelImports(eq("com.example.app"), eq(packageConfiguration), eq(List.of("Product"))))
                    .thenReturn("// IMPORTS");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("configuration/cache-configuration.ftl"), anyMap()))
                    .thenReturn("// TEMPLATE");

            generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("configuration/cache-configuration.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return map.get("type") == CacheTypeEnum.CAFFEINE
                                && Objects.equals(map.get("maxSize"), 100L)
                                && Objects.equals(map.get("expiration"), 3600)
                                && Objects.equals(map.get("modelImports"), "// IMPORTS")
                                && Objects.equals(map.get("entities"), List.of("Product"));
                    })
            ));

            writer.verify(() -> FileWriterUtils.writeToFile(
                    eq("out"),
                    eq("config"),
                    eq("CacheConfiguration.java"),
                    argThat(content -> content.contains("// TEMPLATE"))
            ));

            genCtx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION));
        }
    }

    @Test
    void generate_shouldUseDefaultCacheTypeSimpleWhenTypeIsNull_andNotIncludeOptionalFields() {

        final CrudAndCache cc = prepareCrudWithCache();
        when(cc.cacheConfig.getEnabled()).thenReturn(true);
        when(cc.cacheConfig.getType()).thenReturn(null);
        when(cc.cacheConfig.getMaxSize()).thenReturn(null);
        when(cc.cacheConfig.getExpiration()).thenReturn(null);

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);

        final List<ModelDefinition> entities = List.of(
                model("Product", "product")
        );

        final CacheGenerator generator = new CacheGenerator(cc.crudConfig, packageConfiguration, entities);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ConfigurationImports> imports = mockStatic(ConfigurationImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION)).thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example.app", packageConfiguration)).thenReturn("com.example.app.config");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(packageConfiguration)).thenReturn("config");

            imports.when(() -> ConfigurationImports.getModelImports(eq("com.example.app"), eq(packageConfiguration), eq(List.of("Product"))))
                    .thenReturn("// IMPORTS");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("configuration/cache-configuration.ftl"), anyMap()))
                    .thenReturn("// TEMPLATE");

            generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("configuration/cache-configuration.ftl"),
                    argThat(ctx -> {
                        final Map<String, Object> map = (Map<String, Object>) ctx;
                        return map.get("type") == CacheTypeEnum.SIMPLE
                                && !map.containsKey("maxSize")
                                && !map.containsKey("expiration")
                                && Objects.equals(map.get("modelImports"), "// IMPORTS")
                                && Objects.equals(map.get("entities"), List.of("Product"));
                    })
            ));
        }
    }

    @Test
    void generate_shouldFilterEntitiesByStorageName_andPassNamesToImports() {

        final CrudAndCache cc = prepareCrudWithCache();
        when(cc.cacheConfig.getEnabled()).thenReturn(true);

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);

        final List<ModelDefinition> entities = List.of(
                model("Product", "product"),
                model("AuditLog", null),
                model("User", "users")
        );

        final CacheGenerator generator = new CacheGenerator(cc.crudConfig, packageConfiguration, entities);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ConfigurationImports> imports = mockStatic(ConfigurationImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION)).thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example.app", packageConfiguration)).thenReturn("com.example.app.config");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(packageConfiguration)).thenReturn("config");

            imports.when(() -> ConfigurationImports.getModelImports(eq("com.example.app"), eq(packageConfiguration), eq(List.of("Product", "User"))))
                    .thenReturn("// IMPORTS");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("configuration/cache-configuration.ftl"), anyMap()))
                    .thenReturn("// TEMPLATE");

            generator.generate("out");

            imports.verify(() -> ConfigurationImports.getModelImports(eq("com.example.app"), eq(packageConfiguration), eq(List.of("Product", "User"))));
        }
    }
}
