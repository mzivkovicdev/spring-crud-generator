package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.imports.ServiceImports;
import com.markozivkovic.codegen.imports.ServiceImports.ServiceImportScope;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.templates.ServiceTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

class JpaServiceGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenModelHasNoIdField() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = newModel("UserEntity", List.of());
        final List<ModelDefinition> allEntities = List.of(model);

        final JpaServiceGenerator generator = new JpaServiceGenerator(cfg, allEntities, pkgCfg);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ServiceImports> svcImports = mockStatic(ServiceImports.class);
             final MockedStatic<ServiceTemplateContext> svcCtx = mockStatic(ServiceTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(false);

            generator.generate(model, "out");

            fieldUtils.verify(() -> FieldUtils.isAnyFieldId(model.getFields()));
            fieldUtils.verifyNoMoreInteractions();

            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            svcImports.verifyNoInteractions();
            svcCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateServiceWithCacheDisabledWhenCacheConfigIsNull() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getCache()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("UserEntity", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(model);
        final JpaServiceGenerator generator = new JpaServiceGenerator(cfg, allEntities, pkgCfg);

        final String outputDir = "out";

        final AtomicReference<String> writtenClassName = new AtomicReference<>();
        final AtomicReference<String> writtenContent = new AtomicReference<>();

        final AtomicReference<Map<String, Object>> getByIdCtxRef = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ServiceImports> svcImports = mockStatic(ServiceImports.class);
             final MockedStatic<ServiceTemplateContext> svcCtx = mockStatic(ServiceTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.hasCollectionRelation(model, allEntities))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasRelation(model, allEntities))
                    .thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeServicePackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.service");
            pkg.when(() -> PackageUtils.computeServiceSubPackage(pkgCfg))
                    .thenReturn("service");

            svcImports.when(() -> ServiceImports.getBaseImport(model, false))
                    .thenReturn("//BASE_IMPORTS\n");
            svcImports.when(() -> ServiceImports.computeJpaServiceBaseImport(false))
                    .thenReturn("//JPA_BASE_IMPORTS\n");
            svcImports.when(() -> ServiceImports.computeModelsEnumsAndRepositoryImports(
                    eq(model), eq(outputDir), eq(ServiceImportScope.SERVICE), eq(pkgCfg)))
                    .thenReturn("//MODELS_IMPORTS\n");

            final Map<String, Object> getByIdCtx = new HashMap<>();
            svcCtx.when(() -> ServiceTemplateContext.computeGetByIdContext(model))
                    .thenReturn(getByIdCtx);
            svcCtx.when(() -> ServiceTemplateContext.computeCreateContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.computeUpdateByIdContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.computeDeleteByIdContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.computeGetAllContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.createAddRelationMethodContext(model))
                    .thenReturn(Collections.emptyMap());
            svcCtx.when(() -> ServiceTemplateContext.createRemoveRelationMethodContext(model))
                    .thenReturn(Collections.emptyMap());
            svcCtx.when(() -> ServiceTemplateContext.createGetAllByIdsMethodContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.createGetReferenceByIdMethodContext(model))
                    .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/get-by-id.ftl"), anyMap()
            )).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> ctx = inv.getArgument(1, Map.class);
                getByIdCtxRef.set(ctx);
                return "GET_BY_ID_METHOD";
            });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/get-all.ftl"), anyMap()
            )).thenReturn("GET_ALL_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/create.ftl"), anyMap()
            )).thenReturn("CREATE_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/update-by-id.ftl"), anyMap()
            )).thenReturn("UPDATE_BY_ID_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/delete-by-id.ftl"), anyMap()
            )).thenReturn("DELETE_BY_ID_METHOD");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/service-class-template.ftl"), anyMap()
            )).thenReturn("SERVICE_CLASS_TEMPLATE");

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("service"), anyString(), anyString()))
                .thenAnswer(inv -> {
                    writtenClassName.set(inv.getArgument(2, String.class));
                    writtenContent.set(inv.getArgument(3, String.class));
                    return null;
                });
            
            generator.generate(model, outputDir);
        }

        assertEquals("UserService", writtenClassName.get());

        final String content = writtenContent.get();
        assertNotNull(content);
        assertTrue(content.contains("com.example.app.service"), "Content should contain service package");
        assertTrue(content.contains("//BASE_IMPORTS"), "Content should contain base imports");
        assertTrue(content.contains("//JPA_BASE_IMPORTS"), "Content should contain JPA base imports");
        assertTrue(content.contains("//MODELS_IMPORTS"), "Content should contain model/repository imports");
        assertTrue(content.contains("SERVICE_CLASS_TEMPLATE"), "Content should contain generated service class body");

        final Map<String, Object> byIdCtx = getByIdCtxRef.get();
        assertNotNull(byIdCtx);
        assertEquals(false, byIdCtx.get("cache"), "Cache flag should be false when configuration.getCache() is null");
    }

    @Test
    void generate_shouldGenerateServiceWithCacheEnabledWhenCacheConfigEnabled() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CacheConfiguration cacheCfg = mock(CacheConfiguration.class);
        when(cfg.getCache()).thenReturn(cacheCfg);
        when(cacheCfg.getEnabled()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("OrderEntity", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(model);
        final JpaServiceGenerator generator = new JpaServiceGenerator(cfg, allEntities, pkgCfg);
        final String outputDir = "out";

        final AtomicReference<Map<String, Object>> createCtxRef = new AtomicReference<>();
        final AtomicReference<String> writtenClassName = new AtomicReference<>();
        final AtomicReference<String> writtenContent = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<ServiceImports> svcImports = mockStatic(ServiceImports.class);
             final MockedStatic<ServiceTemplateContext> svcCtx = mockStatic(ServiceTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.hasCollectionRelation(model, allEntities))
                    .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasRelation(model, allEntities))
                    .thenReturn(true);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                    .thenReturn("Order");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeServicePackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.service");
            pkg.when(() -> PackageUtils.computeServiceSubPackage(pkgCfg))
                    .thenReturn("service");

            svcImports.when(() -> ServiceImports.getBaseImport(model, false))
                    .thenReturn("//BASE_IMPORTS\n");
            svcImports.when(() -> ServiceImports.computeJpaServiceBaseImport(true))
                    .thenReturn("//JPA_BASE_IMPORTS_CACHE\n");
            svcImports.when(() -> ServiceImports.computeModelsEnumsAndRepositoryImports(
                    eq(model), eq(outputDir), eq(ServiceImportScope.SERVICE), eq(pkgCfg)))
                    .thenReturn("//MODELS_IMPORTS\n");

            svcCtx.when(() -> ServiceTemplateContext.createAddRelationMethodContext(model))
                    .thenReturn(new HashMap<>(Map.of("rel", "x")));
            svcCtx.when(() -> ServiceTemplateContext.createRemoveRelationMethodContext(model))
                    .thenReturn(new HashMap<>(Map.of("rel", "x")));
            svcCtx.when(() -> ServiceTemplateContext.computeGetAllContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.computeGetByIdContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.computeUpdateByIdContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.computeDeleteByIdContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.computeCreateContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.createGetAllByIdsMethodContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.createGetReferenceByIdMethodContext(model))
                    .thenReturn(new HashMap<>());
            svcCtx.when(() -> ServiceTemplateContext.createServiceClassContext(model))
                    .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("service/method/create.ftl"), anyMap()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> ctxMap = inv.getArgument(1, Map.class);
                    createCtxRef.set(ctxMap);
                    return "CREATE_METHOD";
                });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/get-by-id.ftl"), anyMap()
            )).thenReturn("GET_BY_ID_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/get-all.ftl"), anyMap()
            )).thenReturn("GET_ALL_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/update-by-id.ftl"), anyMap()
            )).thenReturn("UPDATE_BY_ID_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/delete-by-id.ftl"), anyMap()
            )).thenReturn("DELETE_BY_ID_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/add-relation.ftl"), anyMap()
            )).thenReturn("ADD_RELATION_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/remove-relation.ftl"), anyMap()
            )).thenReturn("REMOVE_RELATION_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/get-all-by-ids.ftl"), anyMap()
            )).thenReturn("GET_ALL_BY_IDS_METHOD");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/method/get-reference-by-id.ftl"), anyMap()
            )).thenReturn("GET_REF_BY_ID_METHOD");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("service/service-class-template.ftl"), anyMap()
            )).thenReturn("SERVICE_CLASS_TEMPLATE");

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), eq("service"), anyString(), anyString()))
                .thenAnswer(inv -> {
                    writtenClassName.set(inv.getArgument(2, String.class));
                    writtenContent.set(inv.getArgument(3, String.class));
                    return null;
                });

            generator.generate(model, outputDir);
        }

        assertEquals("OrderService", writtenClassName.get());
        final String content = writtenContent.get();
        assertNotNull(content);

        assertTrue(content.contains("//JPA_BASE_IMPORTS_CACHE"), "Content should contain JPA base imports with cache enabled");

        final Map<String, Object> createCtx = createCtxRef.get();
        assertNotNull(createCtx);
        assertEquals(true, createCtx.get("cache"), "Cache flag in create context should be true when cache is enabled");
    }
}
