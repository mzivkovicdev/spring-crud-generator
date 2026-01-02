package dev.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.GeneratorConstants.GeneratorContextKeys;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.imports.ModelImports;
import dev.markozivkovic.codegen.models.AuditDefinition;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.IdDefinition;
import dev.markozivkovic.codegen.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.JpaEntityTemplateContext;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

class JpaEntityGeneratorTest {

    private ModelDefinition newModel(final String name, final String storageName, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getStorageName()).thenReturn(storageName);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    private static FieldDefinition mockIdField(final IdStrategyEnum strategy) {
        final FieldDefinition idField = mock(FieldDefinition.class);
        final IdDefinition idDef = mock(IdDefinition.class);
        when(idDef.getStrategy()).thenReturn(strategy);
        when(idField.getId()).thenReturn(idDef);
        return idField;
    }


    @Test
    void generate_shouldGenerateHelperEntityForJsonFieldsAndJpaEntity() {

        final ModelDefinition addressModel = newModel("Address", "address", List.of());
        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final ModelDefinition userModel = newModel("User", "users", List.of(jsonField));

        final List<ModelDefinition> allEntities = List.of(userModel, addressModel);

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(true);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final Map<String, Object> helperCtx = new HashMap<>();
        final Map<String, Object> entityCtx = new HashMap<>();
        final List<String> writtenHelpers = new ArrayList<>();
        final List<String> writtenEntities = new ArrayList<>();

        final Boolean auditingGenerated = false;

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
            final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
            final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
            final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
            final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(auditingGenerated);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("Address");

            final FieldDefinition idField = mockIdField(IdStrategyEnum.AUTO);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(userModel, allEntities)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(addressModel, allEntities)).thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.example.app");

            pkg.when(() -> PackageUtils.computeHelperEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.helper");
            pkg.when(() -> PackageUtils.computeHelperEntitySubPackage(pkgCfg)).thenReturn("helper");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.computeEntitySubPackage(pkgCfg)).thenReturn("entity");

            modelImports.when(() -> ModelImports.getBaseImport(addressModel, true, false))
                    .thenReturn("//HELPER_BASE_IMPORTS\n");
            modelImports.when(() -> ModelImports.getBaseImport(userModel, true, true)).thenReturn("//ENTITY_BASE_IMPORTS\n");

            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(addressModel, outputDir, pkgCfg))
                    .thenReturn("//HELPER_ENUM_IMPORTS\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(userModel, outputDir, pkgCfg))
                    .thenReturn("//ENTITY_ENUM_IMPORTS\n");

            modelImports.when(() -> ModelImports.computeJakartaImports(userModel, true, false))
                    .thenReturn("//JAKARTA_IMPORTS\n");

            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(addressModel)).thenReturn(helperCtx);
            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(userModel)).thenReturn(entityCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("model/component/"), anyMap()))
                    .thenReturn("//COMPONENT_TEMPLATE\n");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("model/model-class-template.ftl"), anyMap()))
                    .thenReturn("//MODEL_CLASS_TEMPLATE\n");

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("helper"), eq("Address"), anyString()))
                    .thenAnswer(inv -> { writtenHelpers.add(inv.getArgument(2)); return null; });

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("entity"), eq("User"), anyString()))
                    .thenAnswer(inv -> { writtenEntities.add(inv.getArgument(2)); return null; });

            generator.generate(userModel, outputDir);
        }

        assertEquals(List.of("Address"), writtenHelpers);
        assertEquals(List.of("User"), writtenEntities);

        assertEquals(true, helperCtx.get("embedded"), "Helper context should have embedded=true");
        assertEquals(true, entityCtx.get("optimisticLocking"), "Entity context should have optimisticLocking=true");
    }

    @Test
    void generate_shouldSkipJpaEntityWhenModelIsUsedAsJsonField() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final ModelDefinition model = newModel("JsonOnlyModel", "json_only", List.of());
        final List<ModelDefinition> allEntities = List.of(model);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final Boolean generated = false;

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(generated);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(true);

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

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("User", "users", List.of(jsonField));
        final List<ModelDefinition> allEntities = List.of(model);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final Boolean generated = false;

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(generated);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(true);

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("Address");

            final FieldDefinition idField = mockIdField(IdStrategyEnum.AUTO);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);

            assertThrows(IllegalArgumentException.class, () -> generator.generate(model, outputDir));

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateJpaAuditingConfig_whenAuditEnabledOnAnyEntity_evenIfFirstEntityHasNoAudit() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);
        when(cfg.getDatabase()).thenReturn(DatabaseType.MYSQL);

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

        final Boolean generated = false;
        final List<String> writtenConfigFiles = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(any(), eq(allEntities))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(any())).thenReturn(false);

            final FieldDefinition idField = mockIdField(IdStrategyEnum.IDENTITY);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);

            modelImports.when(() -> ModelImports.getBaseImport(any(), eq(true), eq(true))).thenReturn("//BASE\n");
            modelImports.when(() -> ModelImports.computeJakartaImports(any(), anyBoolean(), anyBoolean())).thenReturn("//JAKARTA\n");
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

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(generated);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenReturn(true);

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
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(any(), eq(allEntities))).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isJsonField(any())).thenReturn(false);

            final FieldDefinition idField = mockIdField(IdStrategyEnum.IDENTITY);
            fieldUtils.when(() -> FieldUtils.extractIdField(anyList())).thenReturn(idField);

            modelImports.when(() -> ModelImports.getBaseImport(any(), eq(true), eq(true))).thenReturn("//BASE\n");
            modelImports.when(() -> ModelImports.computeJakartaImports(any(), anyBoolean(), anyBoolean())).thenReturn("//JAKARTA\n");
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
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)).thenAnswer(inv -> {
                generated[0] = true;
                return null;
            });

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("config"), eq("EnableAuditingConfiguration.java"), anyString()))
                    .thenAnswer(inv -> { writtenConfigFiles.add(inv.getArgument(2)); return null; });

            generator.generate(m1, outputDir);
            generator.generate(m2, outputDir);
        }

        assertEquals(1, writtenConfigFiles.size(), "Auditing config must be generated only once.");
    }
}
