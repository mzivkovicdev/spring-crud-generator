package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.markozivkovic.codegen.imports.ModelImports;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.templates.JpaEntityTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

class JpaEntityGeneratorTest {

    private ModelDefinition newModel(final String name, final  String storageName, final  List<FieldDefinition> fields) {
        
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getStorageName()).thenReturn(storageName);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldGenerateHelperEntityForJsonFieldsAndJpaEntity() {

        final ModelDefinition addressModel = newModel("Address", "address", List.of());
        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final ModelDefinition userModel = newModel("User", "users", List.of(jsonField));

        final List<ModelDefinition> allEntities = List.of(userModel, addressModel);

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final Map<String, Object> helperCtx = new HashMap<>();
        final Map<String, Object> entityCtx = new HashMap<>();
        final List<String> writtenHelpers = new ArrayList<>();
        final List<String> writtenEntities = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("Address");

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(userModel, allEntities))
                    .thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example.app");

            pkg.when(() -> PackageUtils.computeHelperEntityPackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.helper");
            pkg.when(() -> PackageUtils.computeHelperEntitySubPackage(pkgCfg))
                    .thenReturn("helper");

            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.computeEntitySubPackage(pkgCfg))
                    .thenReturn("entity");

            modelImports.when(() -> ModelImports.getBaseImport(addressModel, true, false))
                    .thenReturn("//HELPER_BASE_IMPORTS\n");
            modelImports.when(() -> ModelImports.getBaseImport(userModel, true, true))
                    .thenReturn("//ENTITY_BASE_IMPORTS\n");

            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(addressModel, outputDir, pkgCfg))
                    .thenReturn("//HELPER_ENUM_IMPORTS\n");
            modelImports.when(() -> ModelImports.computeEnumsAndHelperEntitiesImport(userModel, outputDir, pkgCfg))
                    .thenReturn("//ENTITY_ENUM_IMPORTS\n");

            modelImports.when(() -> ModelImports.computeJakartaImports(userModel, true))
                    .thenReturn("//JAKARTA_IMPORTS\n");

            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(addressModel))
                    .thenReturn(helperCtx);
            jpaCtx.when(() -> JpaEntityTemplateContext.computeJpaModelContext(userModel))
                    .thenReturn(entityCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    startsWith("model/component/"), anyMap()
            )).thenReturn("//COMPONENT_TEMPLATE\n");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("model/model-class-template.ftl"),
                    anyMap()
            )).thenReturn("//MODEL_CLASS_TEMPLATE\n");

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("helper"), eq("Address"), anyString()))
                .thenAnswer(invocation -> {
                    writtenHelpers.add(invocation.getArgument(2, String.class));
                    return null;
                });

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("entity"), eq("User"), anyString()))
                .thenAnswer(invocation -> {
                    writtenEntities.add(invocation.getArgument(2, String.class));
                    return null;
                });

            generator.generate(userModel, outputDir);
        }

        assertEquals(1, writtenHelpers.size());
        assertEquals("Address", writtenHelpers.get(0));
        assertEquals(1, writtenEntities.size());
        assertEquals("User", writtenEntities.get(0));
        assertEquals(true, helperCtx.get("embedded"), "Helper context should have embedded=true");
        assertEquals(true, entityCtx.get("optimisticLocking"), "Entity context should have optimisticLocking=true");
    }

    @Test
    void generate_shouldSkipJpaEntityWhenModelIsUsedAsJsonField() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.isOptimisticLocking()).thenReturn(false);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = newModel("JsonOnlyModel", "json_only", List.of());
        final List<ModelDefinition> allEntities = List.of(model);
        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);

        final String outputDir = "out";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isJsonField(any()))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(model, allEntities))
                    .thenReturn(true);

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
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("User", "users", List.of(jsonField));
        final List<ModelDefinition> allEntities = List.of(model);

        final JpaEntityGenerator generator = new JpaEntityGenerator(cfg, allEntities, pkgCfg);

        final String outputDir = "out";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelImports> modelImports = mockStatic(ModelImports.class);
             final MockedStatic<JpaEntityTemplateContext> jpaCtx = mockStatic(JpaEntityTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("Address");

            assertThrows(IllegalArgumentException.class,
                    () -> generator.generate(model, outputDir));

            writer.verifyNoInteractions();
        }
    }
}
