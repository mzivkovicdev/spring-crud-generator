package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import dev.markozivkovic.springcrudgenerator.imports.ExceptionImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ErrorResponse;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.GraphQLDefinition;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class GlobalExceptionHandlerGeneratorTest {

    private ModelDefinition modelWithFields(final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenErrorResponseNone() {
        
        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        when(crudConfig.getErrorResponse()).thenReturn(ErrorResponse.NONE);

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final List<ModelDefinition> entities = List.of(modelWithFields(List.of()));

        final GlobalExceptionHandlerGenerator generator =
                new GlobalExceptionHandlerGenerator(crudConfig, entities, pkgConfig);

        try (final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ExceptionImports> exImports = mockStatic(ExceptionImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generator.generate("out");

            pkg.verifyNoInteractions();
            fieldUtils.verifyNoInteractions();
            exImports.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateHttpResponseRestAndGraphQlHandlersDetailedWithRelations() {
        
        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        when(crudConfig.getErrorResponse()).thenReturn(ErrorResponse.DETAILED);
        
        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(crudConfig.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final FieldDefinition f2 = mock(FieldDefinition.class);
        final List<ModelDefinition> entities = List.of(
                modelWithFields(List.of(f1)),
                modelWithFields(List.of(f2))
        );

        final GlobalExceptionHandlerGenerator generator =
                new GlobalExceptionHandlerGenerator(crudConfig, entities, pkgConfig);

        final AtomicReference<Map<String, Object>> restCtxRef = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> graphQlCtxRef = new AtomicReference<>();

        final List<InvocationOnMock> templateInvocations = new ArrayList<>();
        final List<InvocationOnMock> writerInvocations = new ArrayList<>();

        try (final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ExceptionImports> exImports = mockStatic(ExceptionImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl =
                     mockStatic(FreeMarkerTemplateProcessorUtils.class, invocation -> {
                         templateInvocations.add(invocation);
                         return "TEMPLATE-" + invocation.getArgument(0, String.class);
                     });
             final MockedStatic<FileWriterUtils> writer =
                     mockStatic(FileWriterUtils.class, invocation -> {
                         writerInvocations.add(invocation);
                         return null;
                     })) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(anyList()))
                    .thenReturn(List.of("REL"));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeExceptionResponsePackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.exception.response");
            pkg.when(() -> PackageUtils.computeExceptionResponseSubPackage(pkgConfig))
                    .thenReturn("exception/response");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.exception.handler");
            pkg.when(() -> PackageUtils.computeExceptionHandlerSubPackage(pkgConfig))
                    .thenReturn("exception/handler");

            exImports.when(() -> ExceptionImports.computeGlobalRestExceptionHandlerProjectImports(
                    eq(true), eq("out"), eq(pkgConfig)))
                    .thenReturn("REST_IMPORTS");
            exImports.when(() -> ExceptionImports.computeGlobalGraphQlExceptionHandlerProjectImports(
                    eq(true), eq("out"), eq(pkgConfig)))
                    .thenReturn("GRAPHQL_IMPORTS");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("exception/rest-exception-handler-template.ftl"),
                    anyMap()
            )).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                restCtxRef.set(ctx);
                return "REST_EXCEPTION_TEMPLATE";
            });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("exception/graphql-exception-handler-template.ftl"),
                    anyMap()
            )).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                graphQlCtxRef.set(ctx);
                return "GRAPHQL_EXCEPTION_TEMPLATE";
            });

            generator.generate("out");
        }

        final boolean usedDetailedResponseTemplate = templateInvocations.stream()
                .anyMatch(inv -> "processTemplate".equals(inv.getMethod().getName())
                        && "exception/response/detailed-response-template.ftl".equals(inv.getArgument(0)));

        assertTrue(usedDetailedResponseTemplate,
                "Detailed response template should be used for HttpResponse");

        final Map<String, Object> restCtx = restCtxRef.get();
        assertNotNull(restCtx);
        assertEquals(true, restCtx.get("hasRelations"));
        assertEquals("REST_IMPORTS", restCtx.get("projectImports"));
        assertEquals(true, restCtx.get("isDetailed"));

        final Map<String, Object> graphQlCtx = graphQlCtxRef.get();
        assertNotNull(graphQlCtx);
        assertEquals(true, graphQlCtx.get("hasRelations"));
        assertEquals("GRAPHQL_IMPORTS", graphQlCtx.get("projectImports"));
        assertEquals(true, graphQlCtx.get("isDetailed"));

        final boolean wroteHttpResponse = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && "HttpResponse".equals(inv.getArgument(2)));

        final boolean wroteRestHandler = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && "GlobalRestExceptionHandler".equals(inv.getArgument(2)));

        final boolean wroteGraphQlHandler = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && "GlobalGraphQlExceptionHandler".equals(inv.getArgument(2)));

        assertTrue(wroteHttpResponse, "HttpResponse should be generated");
        assertTrue(wroteRestHandler, "GlobalRestExceptionHandler should be generated");
        assertTrue(wroteGraphQlHandler, "GlobalGraphQlExceptionHandler should be generated");
    }

    @Test
    void generate_shouldGenerateMinimalHttpResponseAndRestHandlerWithoutGraphQlAndRelations() {
        
        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        when(crudConfig.getErrorResponse()).thenReturn(ErrorResponse.MINIMAL);

        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(crudConfig.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(false);

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);

        final FieldDefinition f1 = mock(FieldDefinition.class);
        final List<ModelDefinition> entities = List.of(
                modelWithFields(List.of(f1))
        );

        final GlobalExceptionHandlerGenerator generator =
                new GlobalExceptionHandlerGenerator(crudConfig, entities, pkgConfig);

        final AtomicReference<Map<String, Object>> restCtxRef = new AtomicReference<>();
        final List<InvocationOnMock> templateInvocations = new ArrayList<>();
        final List<InvocationOnMock> writerInvocations = new ArrayList<>();

        try (final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ExceptionImports> exImports = mockStatic(ExceptionImports.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl =
                     mockStatic(FreeMarkerTemplateProcessorUtils.class, invocation -> {
                         templateInvocations.add(invocation);
                         return "TEMPLATE-" + invocation.getArgument(0, String.class);
                     });
             final MockedStatic<FileWriterUtils> writer =
                     mockStatic(FileWriterUtils.class, invocation -> {
                         writerInvocations.add(invocation);
                         return null;
                     })) {

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(anyList()))
                    .thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeExceptionResponsePackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.exception.response");
            pkg.when(() -> PackageUtils.computeExceptionResponseSubPackage(pkgConfig))
                    .thenReturn("exception/response");
            pkg.when(() -> PackageUtils.computeExceptionHandlerPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.exception.handler");
            pkg.when(() -> PackageUtils.computeExceptionHandlerSubPackage(pkgConfig))
                    .thenReturn("exception/handler");

            exImports.when(() -> ExceptionImports.computeGlobalRestExceptionHandlerProjectImports(
                    eq(false), eq("out"), eq(pkgConfig)))
                    .thenReturn("REST_IMPORTS_NO_REL");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("exception/rest-exception-handler-template.ftl"),
                    anyMap()
            )).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                restCtxRef.set(ctx);
                return "REST_TEMPLATE";
            });

            generator.generate("out");
        }

        final boolean usedMinimalResponseTemplate = templateInvocations.stream()
                .anyMatch(inv -> "processTemplate".equals(inv.getMethod().getName())
                        && "exception/response/minimal-response-template.ftl".equals(inv.getArgument(0)));

        assertTrue(usedMinimalResponseTemplate,
                "Minimal response template should be used for HttpResponse");

        final Map<String, Object> restCtx = restCtxRef.get();
        assertNotNull(restCtx);
        assertEquals(false, restCtx.get("hasRelations"));
        assertEquals("REST_IMPORTS_NO_REL", restCtx.get("projectImports"));
        assertEquals(false, restCtx.get("isDetailed"));

        final boolean usedGraphQlTemplate = templateInvocations.stream()
                .anyMatch(inv -> "processTemplate".equals(inv.getMethod().getName())
                        && "exception/graphql-exception-handler-template.ftl".equals(inv.getArgument(0)));

        final boolean wroteGraphQlHandler = writerInvocations.stream()
                .anyMatch(inv -> "writeToFile".equals(inv.getMethod().getName())
                        && "GlobalGraphQlExceptionHandler".equals(inv.getArgument(2)));

        assertFalse(usedGraphQlTemplate, "GraphQL exception handler template should NOT be used");
        assertFalse(wroteGraphQlHandler, "GlobalGraphQlExceptionHandler should NOT be written");
    }
}
