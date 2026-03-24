package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.AdditionalConfigurationConstants;
import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants.GeneratorContextKeys;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.imports.ModelImports;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.JpaEntityTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class JpaEntityGeneratorTest {

    private ModelDefinition newModel(final String name, final String storageName, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getStorageName()).thenReturn(storageName);
        when(m.getFields()).thenReturn(fields);
        when(m.getAudit()).thenReturn(null);
        when(m.getSoftDelete()).thenReturn(null);
        return m;
    }

    private static FieldDefinition mockIdField(final IdStrategyEnum strategy) {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idDef.getStrategy()).thenReturn(strategy);
        when(idField.getId()).thenReturn(idDef);
        when(idField.getName()).thenReturn("id");
        return idField;
    }

    @Test
    void generate_shouldGenerateHelperEntityForJsonFieldsAndJpaEntity() {

        final ModelDefinition addressModel = newModel("Address", "address", List.of());

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final List<FieldDefinition> userFields = List.of(jsonField);
        final ModelDefinition userModel = newModel("User", "users", userFields);

        final List<ModelDefinition> allEntities = List.of(userModel, addressModel);

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);
        when(cfg.getAdditionalProperties()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final Map<String, Object> helperCtx = new HashMap<>();
        final Map<String, Object> entityCtx = new HashMap<>();
        final List<String> writtenHelpers = new ArrayList<>();
        final List<String> writtenEntities = new ArrayList<>();

        final AtomicReference<Map<String, Object>> userModelClassCtx = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(false);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG))
                    .thenAnswer(inv -> null);
            addProps.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Address");
            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(userModel, allEntities)).thenReturn(false);

            final FieldDefinition idField = mockIdField(IdStrategyEnum.AUTO);
            fieldUtils.when(() -> FieldUtils.extractIdField(userFields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(userFields)).thenReturn(List.of("users", "tags"));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeHelperEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.helper");
            pkg.when(() -> PackageUtils.computeHelperEntitySubPackage(pkgCfg)).thenReturn("helper");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.computeEntitySubPackage(pkgCfg)).thenReturn("entity");

            modelImports.when(() -> ModelImports.getBaseImport(addressModel, true, false)).thenReturn("//HELPER_BASE_IMPORTS\n");
            modelImports.when(() -> ModelImports.getBaseImport(userModel, true, true)).thenReturn("//ENTITY_BASE_IMPORTS\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(addressModel, outputDir, pkgCfg)).thenReturn("//HELPER_ENUM_IMPORTS\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(userModel, outputDir, pkgCfg)).thenReturn("//ENTITY_ENUM_IMPORTS\n");
            modelImports.when(() -> ModelImports.computeJakartaImports(userModel, true, true, false))
                    .thenReturn("//JAKARTA_IMPORTS\n");

            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(addressModel)).thenReturn(helperCtx);
            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(userModel)).thenReturn(entityCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("model/component/"), anyMap()))
                    .thenReturn("//COMPONENT_TEMPLATE\n");

            modelNameUtils.when(() -> ModelNameUtils.computeEntityGraphName(eq("User"), anyList())).thenReturn("User.withUsersTags");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("id")).thenReturn("id");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("model/model-class-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = (Map<String, Object>) inv.getArgument(1);
                        if ("User".equals(ctx.get("className"))) {
                            userModelClassCtx.set(new HashMap<>(ctx));
                        }
                        return "//MODEL_CLASS_TEMPLATE\n";
                    });

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("helper"), eq("Address"), anyString()))
                    .thenAnswer(inv -> { writtenHelpers.add(inv.getArgument(2)); return null; });

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("entity"), eq("User"), anyString()))
                    .thenAnswer(inv -> { writtenEntities.add(inv.getArgument(2)); return null; });

            generator.generate(userModel, outputDir);

            modelImports.verify(() -> ModelImports.computeJakartaImports(userModel, true, true, false));
        }

        assertEquals(List.of("Address"), writtenHelpers);
        assertEquals(List.of("User"), writtenEntities);

        assertEquals(true, helperCtx.get("embedded"), "Helper context should have embedded=true");
        assertEquals(true, entityCtx.get("optimisticLocking"), "Entity context should have optimisticLocking=true");

        final Map<String, Object> ctx = userModelClassCtx.get();
        assertNotNull(ctx, "User model-class-template context must be captured");

        assertEquals(false, ctx.get("openInView"));
        assertEquals(List.of("users", "tags"), ctx.get("lazyFields"));
        assertEquals("User.withUsersTags", ctx.get("entityGraphName"));
    }

    @Test
    void generate_shouldSkipJpaEntityWhenModelIsUsedAsJsonField() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);
        when(cfg.getAdditionalProperties()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final ModelDefinition model = newModel("JsonOnlyModel", "json_only", List.of());
        final List<ModelDefinition> allEntities = List.of(model);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(false);
            addProps.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(model, allEntities)).thenReturn(true);

            generator.generate(model, outputDir);

            writer.verifyNoInteractions();
            pkg.verifyNoInteractions();
            modelImports.verifyNoInteractions();
            jpaCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldThrowWhenJsonModelNotFound() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);
        when(cfg.getAdditionalProperties()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final List<FieldDefinition> fields = List.of(jsonField);

        final ModelDefinition model = newModel("User", "users", fields);
        final List<ModelDefinition> allEntities = List.of(model);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class)) {

            addProps.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonInnerElementType(jsonField)).thenReturn("Address");

            assertThrows(IllegalArgumentException.class, () -> generator.generate(model, outputDir));

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateJpaAuditingConfig_whenAuditEnabledOnAnyEntity_evenIfFirstEntityHasNoAudit() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);
        when(cfg.getAdditionalProperties()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final ModelDefinition modelWithoutAudit = newModel("User", "users", List.of());
        when(modelWithoutAudit.getAudit()).thenReturn(null);

        final ModelDefinition modelWithAudit = newModel("Order", "orders", List.of());
        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.isEnabled()).thenReturn(true);
        when(modelWithAudit.getAudit()).thenReturn(audit);

        final List<ModelDefinition> allEntities = List.of(modelWithoutAudit, modelWithAudit);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final List<String> writtenConfigFiles = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class)) {

            addProps.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(any(), eq(allEntities))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(any())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(anyList())).thenReturn(Collections.emptyList());

            final FieldDefinition idField = mockIdField(IdStrategyEnum.IDENTITY);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);
            modelNameUtils.when(() -> ModelNameUtils.computeEntityGraphName(anyString(), anyList())).thenReturn("X");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase(anyString())).thenReturn("id");
            modelImports.when(() -> ModelImports.getBaseImport(any(), eq(true), eq(true))).thenReturn("//BASE\n");
            modelImports.when(() -> ModelImports.computeJakartaImports(any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn("//JAKARTA\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(any(), anyString(), eq(pkgCfg))).thenReturn("");

            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(any())).thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("model/component/"), anyMap())).thenReturn("//COMP\n");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("model/model-class-template.ftl"), anyMap())).thenReturn("//MODEL\n");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.computeEntitySubPackage(pkgCfg)).thenReturn("entity");

            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.config");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(pkgCfg)).thenReturn("config");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("configuration/jpa-auditing-configuration.ftl"), anyMap()))
                    .thenReturn("//AUDITING_CONFIG\n");

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(false);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenAnswer(inv -> null);

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("config"), eq("EnableAuditingConfiguration.java"), anyString()))
                    .thenAnswer(inv -> { writtenConfigFiles.add(inv.getArgument(2)); return null; });

            generator.generate(modelWithoutAudit, outputDir);
            generator.generate(modelWithAudit, outputDir);
        }

        assertEquals(List.of("EnableAuditingConfiguration.java"), writtenConfigFiles);
    }

    @Test
    void generate_shouldGenerateJpaAuditingConfig_onlyOnce_evenIfMultipleEntitiesHaveAuditEnabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);
        when(cfg.getAdditionalProperties()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.isEnabled()).thenReturn(true);

        final ModelDefinition m1 = newModel("User", "users", List.of());
        when(m1.getAudit()).thenReturn(audit);

        final ModelDefinition m2 = newModel("Order", "orders", List.of());
        when(m2.getAudit()).thenReturn(audit);

        final List<ModelDefinition> allEntities = List.of(m1, m2);
        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final boolean[] generated = { false };
        final List<String> writtenConfigFiles = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class)) {

            addProps.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(any(), eq(allEntities))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(any())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(anyList())).thenReturn(Collections.emptyList());

            final FieldDefinition idField = mockIdField(IdStrategyEnum.IDENTITY);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);
            modelNameUtils.when(() -> ModelNameUtils.computeEntityGraphName(anyString(), anyList())).thenReturn("X");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase(anyString())).thenReturn("id");
            modelImports.when(() -> ModelImports.getBaseImport(any(), eq(true), eq(true))).thenReturn("//BASE\n");
            modelImports.when(() -> ModelImports.computeJakartaImports(any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn("//JAKARTA\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(any(), anyString(), eq(pkgCfg))).thenReturn("");

            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(any())).thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("model/component/"), anyMap())).thenReturn("//COMP\n");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("model/model-class-template.ftl"), anyMap())).thenReturn("//MODEL\n");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.example.app");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.computeEntitySubPackage(pkgCfg)).thenReturn("entity");
            pkg.when(() -> PackageUtils.computeConfigurationPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.config");
            pkg.when(() -> PackageUtils.computeConfigurationSubPackage(pkgCfg)).thenReturn("config");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("configuration/jpa-auditing-configuration.ftl"), anyMap()))
                    .thenReturn("//AUDITING_CONFIG\n");
            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenAnswer(inv -> generated[0]);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenAnswer(inv -> { generated[0] = true; return null; });

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("config"), eq("EnableAuditingConfiguration.java"), anyString()))
                    .thenAnswer(inv -> { writtenConfigFiles.add(inv.getArgument(2)); return null; });

            generator.generate(m1, outputDir);
            generator.generate(m2, outputDir);
        }

        assertEquals(1, writtenConfigFiles.size(), "Auditing config must be generated only once.");
    }

    @Test
    void generate_shouldPassOpenInViewEnabledTrue_toJakartaImportsAndTemplateContext() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);
        final Map<String, Object> props = new HashMap<>();
        props.put(AdditionalConfigurationConstants.JPA_OPEN_IN_VIEW, Boolean.TRUE);
        when(cfg.getAdditionalProperties()).thenReturn(props);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final List<FieldDefinition> fields = List.of(new FieldDefinition());
        final ModelDefinition model = newModel("User", "users", fields);
        final List<ModelDefinition> allEntities = List.of(model);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);

        final AtomicReference<Map<String, Object>> userCtx = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(false);
            addProps.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(props)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isJsonField(any())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(model, allEntities)).thenReturn(false);

            final FieldDefinition idField = mockIdField(IdStrategyEnum.IDENTITY);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(List.of("tags"));
            modelNameUtils.when(() -> ModelNameUtils.computeEntityGraphName(eq("User"), eq(List.of("tags")))).thenReturn("User.withTags");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("id")).thenReturn("id");
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.computeEntitySubPackage(pkgCfg)).thenReturn("entity");

            modelImports.when(() -> ModelImports.getBaseImport(model, true, true)).thenReturn("//BASE\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(model, "out", pkgCfg)).thenReturn("");
            modelImports.when(() -> ModelImports.computeJakartaImports(model, false, false, true)).thenReturn("//JAKARTA\n");

            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(model)).thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("model/component/"), anyMap())).thenReturn("//COMP\n");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("model/model-class-template.ftl"), anyMap()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> ctx = (Map<String, Object>) inv.getArgument(1);
                    userCtx.set(new HashMap<>(ctx));
                    return "//MODEL\n";
                });

            generator.generate(model, "out");

            modelImports.verify(() -> ModelImports.computeJakartaImports(model, false, false, true));
        }

        assertNotNull(userCtx.get());
        assertEquals(true, userCtx.get().get("openInView"));
        assertEquals("User.withTags", userCtx.get().get("entityGraphName"));
    }

    @Test
    void generate_shouldPassSoftDeleteAndSnakeCaseIdField_toModelTemplateContext() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);
        when(cfg.getAdditionalProperties()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final List<FieldDefinition> fields = List.of(new FieldDefinition());
        final ModelDefinition model = newModel("User", "users", fields);
        when(model.getSoftDelete()).thenReturn(true);

        final List<ModelDefinition> allEntities = List.of(model);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);

        final AtomicReference<Map<String, Object>> ctxRef = new AtomicReference<>();

        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idDef.getStrategy()).thenReturn(IdStrategyEnum.IDENTITY);
        when(idField.getId()).thenReturn(idDef);
        when(idField.getName()).thenReturn("userId");

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<ModelNameUtils> modelNameUtils = mockStatic(ModelNameUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(false);

            addProps.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isJsonField(any())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(model, allEntities)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(anyList())).thenReturn(Collections.emptyList());

            modelNameUtils.when(() -> ModelNameUtils.computeEntityGraphName(eq("User"), eq(Collections.emptyList())))
                    .thenReturn("User");
            modelNameUtils.when(() -> ModelNameUtils.toSnakeCase("userId")).thenReturn("user_id");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.computeEntitySubPackage(pkgCfg)).thenReturn("entity");

            modelImports.when(() -> ModelImports.getBaseImport(model, true, true)).thenReturn("//BASE\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(model, "out", pkgCfg)).thenReturn("");
            modelImports.when(() -> ModelImports.computeJakartaImports(model, false, false, false)).thenReturn("//JAKARTA\n");

            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(model)).thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("model/component/"), anyMap()))
                    .thenReturn("//COMP\n");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("model/model-class-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> ctx = (Map<String, Object>) inv.getArgument(1);
                        ctxRef.set(new HashMap<>(ctx));
                        return "//MODEL\n";
                    });

            generator.generate(model, "out");
        }

        final Map<String, Object> ctx = ctxRef.get();
        assertNotNull(ctx);

        assertEquals(true, ctx.get(TemplateContextConstants.SOFT_DELETE_ENABLED));
        assertEquals("user_id", ctx.get(TemplateContextConstants.ID_FIELD));
    }
}