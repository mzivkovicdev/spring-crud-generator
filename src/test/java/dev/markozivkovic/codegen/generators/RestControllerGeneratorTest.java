package dev.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.imports.RestControllerImports;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.RestControllerTemplateContext;
import dev.markozivkovic.codegen.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

class RestControllerGeneratorTest {

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

        final RestControllerGenerator generator =
                new RestControllerGenerator(cfg, allEntities, pkgCfg);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<RestControllerImports> imports = mockStatic(RestControllerImports.class);
             final MockedStatic<RestControllerTemplateContext> controllerCtx = mockStatic(RestControllerTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(false);

            generator.generate(model, "out");

            fieldUtils.verify(() -> FieldUtils.isAnyFieldId(model.getFields()));
            fieldUtils.verifyNoMoreInteractions();

            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            imports.verifyNoInteractions();
            controllerCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
            addProps.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateRestControllerWithSwaggerAndAllEndpoints() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition idField = mock(FieldDefinition.class);

        final ModelDefinition model = newModel("UserEntity", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(model);

        final RestControllerGenerator generator =
                new RestControllerGenerator(cfg, allEntities, pkgCfg);

        final String outputDir = "out";

        final List<String> writtenSubPackages = new ArrayList<>();
        final List<String> writtenClassNames = new ArrayList<>();
        final List<String> writtenContents = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<RestControllerImports> imports = mockStatic(RestControllerImports.class);
             final MockedStatic<RestControllerTemplateContext> controllerCtx = mockStatic(RestControllerTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(true);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeControllerPackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.web");
            pkg.when(() -> PackageUtils.computeControllerSubPackage(pkgCfg))
                    .thenReturn("controller/rest");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            imports.when(() -> RestControllerImports.computeControllerBaseImports(model, allEntities))
                    .thenReturn("import BASE;\n");

            addProps.when(() -> AdditionalPropertiesUtils.resolveBasePath(cfg))
                    .thenReturn("/api");

            imports.when(() -> RestControllerImports.computeControllerProjectImports(
                    model, outputDir, true, pkgCfg))
                    .thenReturn("import PROJECT;\n");

            controllerCtx.when(() -> RestControllerTemplateContext.computeControllerClassContext(model))
                    .thenReturn(new HashMap<>());

            controllerCtx.when(() -> RestControllerTemplateContext.computeCreateEndpointContext(model, allEntities))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeGetByIdEndpointContext(model))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeGetAllEndpointContext(model))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeUpdateEndpointContext(model, true))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeDeleteEndpointContext(model))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeAddResourceRelationEndpointContext(model, allEntities))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeRemoveResourceRelationEndpointContext(model, allEntities))
                    .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/create-resource.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        final Map<String, Object> ctxMap = inv.getArgument(1);
                        Object swaggerVal = ctxMap.get("swagger");
                        if (!(swaggerVal instanceof Boolean) || !((Boolean) swaggerVal)) {
                            throw new AssertionError("Expected swagger=true in create endpoint context");
                        }
                        return "CREATE_ENDPOINT";
                    });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/get-resource.ftl"), anyMap()))
                    .thenReturn("GET_ENDPOINT");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/get-all-resources.ftl"), anyMap()))
                    .thenReturn("GET_ALL_ENDPOINT");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/update-resource.ftl"), anyMap()))
                    .thenReturn("UPDATE_ENDPOINT");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/delete-resource.ftl"), anyMap()))
                    .thenReturn("DELETE_ENDPOINT");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/add-resource-relation.ftl"), anyMap()))
                    .thenReturn("ADD_RELATION_ENDPOINT");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/remove-resource-relation.ftl"), anyMap()))
                    .thenReturn("REMOVE_RELATION_ENDPOINT");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/controller-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        final Map<String, Object> ctxMap = inv.getArgument(1);
                        if (!"/api".equals(ctxMap.get("basePath"))) {
                            throw new AssertionError("Expected basePath=/api in controller context");
                        }
                        if (!"import PROJECT;\n".equals(ctxMap.get("projectImports"))) {
                            throw new AssertionError("Expected projectImports set in controller context");
                        }
                        return "CONTROLLER_CLASS";
                    });

            writer.when(() -> FileWriterUtils.writeToFile(
                    eq(outputDir), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        writtenSubPackages.add(inv.getArgument(1, String.class));
                        writtenClassNames.add(inv.getArgument(2, String.class));
                        writtenContents.add(inv.getArgument(3, String.class));
                        return null;
                    });

            generator.generate(model, outputDir);
        }

        assertTrue(writtenSubPackages.contains("controller/rest"));
        assertTrue(writtenClassNames.contains("UserController"));
        final String content = writtenContents.get(0);
        assertTrue(content.contains("import BASE;"));
        assertTrue(content.contains("CONTROLLER_CLASS"));
    }

    @Test
    void generate_shouldSkipRelationEndpointsWhenContextsEmpty_andSwaggerDisabled() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);

        when(cfg.getOpenApi()).thenReturn(null);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final FieldDefinition idField = mock(FieldDefinition.class);

        final ModelDefinition model = newModel("UserEntity", List.of(idField));
        final List<ModelDefinition> allEntities = List.of(model);

        final RestControllerGenerator generator =
                new RestControllerGenerator(cfg, allEntities, pkgCfg);

        final String outputDir = "out";

        final List<Map<String, Object>> controllerTemplateContexts = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<RestControllerImports> imports = mockStatic(RestControllerImports.class);
             final MockedStatic<RestControllerTemplateContext> controllerCtx = mockStatic(RestControllerTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AdditionalPropertiesUtils> addProps = mockStatic(AdditionalPropertiesUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(true);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeControllerPackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.web");
            pkg.when(() -> PackageUtils.computeControllerSubPackage(pkgCfg))
                    .thenReturn("controller/rest");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            imports.when(() -> RestControllerImports.computeControllerBaseImports(model, allEntities))
                    .thenReturn("import BASE;\n");

            addProps.when(() -> AdditionalPropertiesUtils.resolveBasePath(cfg))
                    .thenReturn("/api");

            imports.when(() -> RestControllerImports.computeControllerProjectImports(
                    model, outputDir, false, pkgCfg))
                    .thenReturn("import PROJECT_NO_SWAGGER;\n");

            controllerCtx.when(() -> RestControllerTemplateContext.computeControllerClassContext(model))
                    .thenReturn(new HashMap<>());

            controllerCtx.when(() -> RestControllerTemplateContext.computeCreateEndpointContext(model, allEntities))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeGetByIdEndpointContext(model))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeGetAllEndpointContext(model))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeUpdateEndpointContext(model, false))
                    .thenReturn(new HashMap<>());
            controllerCtx.when(() -> RestControllerTemplateContext.computeDeleteEndpointContext(model))
                    .thenReturn(new HashMap<>());

            controllerCtx.when(() -> RestControllerTemplateContext.computeAddResourceRelationEndpointContext(model, allEntities))
                    .thenReturn(Collections.emptyMap());
            controllerCtx.when(() -> RestControllerTemplateContext.computeRemoveResourceRelationEndpointContext(model, allEntities))
                    .thenReturn(Collections.emptyMap());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/create-resource.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        final Map<String, Object> ctx = inv.getArgument(1);
                        if (Boolean.TRUE.equals(ctx.get("swagger"))) {
                            throw new AssertionError("Expected swagger=false");
                        }
                        return "CREATE_ENDPOINT";
                    });
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/get-resource.ftl"), anyMap()))
                    .thenReturn("GET_ENDPOINT");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/get-all-resources.ftl"), anyMap()))
                    .thenReturn("GET_ALL_ENDPOINT");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/update-resource.ftl"), anyMap()))
                    .thenReturn("UPDATE_ENDPOINT");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/delete-resource.ftl"), anyMap()))
                    .thenReturn("DELETE_ENDPOINT");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/controller-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        final Map<String, Object> ctx = inv.getArgument(1);
                        controllerTemplateContexts.add(ctx);
                        return "CONTROLLER_CLASS";
                    });

            generator.generate(model, outputDir);

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/add-resource-relation.ftl"), anyMap()
            ), never());
            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("controller/endpoint/remove-resource-relation.ftl"), anyMap()
            ), never());
        }

        final Map<String, Object> ctx = controllerTemplateContexts.get(0);
        assertNull(ctx.get("addResourceRelation"));
        assertNull(ctx.get("removeResourceRelation"));
    }
}