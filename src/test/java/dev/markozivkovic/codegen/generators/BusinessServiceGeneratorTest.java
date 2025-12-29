package dev.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.imports.BusinessServiceImports;
import dev.markozivkovic.codegen.imports.BusinessServiceImports.BusinessServiceImportScope;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.BusinessServiceTemplateContext;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

class BusinessServiceGeneratorTest {

    private ModelDefinition newModel(final String name) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(Collections.emptyList());
        return m;
    }

    @Test
    void generate_shouldSkipWhenModelHasNoIdField() {
        
        final ModelDefinition model = newModel("UserEntity");
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);

        final BusinessServiceGenerator generator =
                new BusinessServiceGenerator(List.of(model), pkgConfig);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList()))
                    .thenReturn(false);

            generator.generate(model, "out");

            writer.verifyNoInteractions();
            tpl.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenModelHasNoRelationFields() {
        final ModelDefinition model = newModel("UserEntity");
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);

        final BusinessServiceGenerator generator =
                new BusinessServiceGenerator(List.of(model), pkgConfig);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(anyList()))
                    .thenReturn(Collections.emptyList());

            generator.generate(model, "out");

            writer.verifyNoInteractions();
            tpl.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateBusinessServiceClass() {
        
        final ModelDefinition model = newModel("UserEntity");
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);

        final List<ModelDefinition> allEntities = List.of(model);
        final BusinessServiceGenerator generator =
                new BusinessServiceGenerator(allEntities, pkgConfig);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<BusinessServiceImports> bsImports = mockStatic(BusinessServiceImports.class);
             final MockedStatic<BusinessServiceTemplateContext> ctx = mockStatic(BusinessServiceTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(anyList()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(anyList()))
                    .thenReturn(List.of("REL"));
            fieldUtils.when(() -> FieldUtils.hasCollectionRelation(eq(model), eq(allEntities)))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyIdFieldUUID(eq(model), eq(allEntities)))
                    .thenReturn(true);

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.RETRYABLE_ANNOTATION))
                    .thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeBusinessServicePackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.service");
            pkg.when(() -> PackageUtils.computeBusinessServiceSubPackage(pkgConfig))
                    .thenReturn("service");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            bsImports.when(() -> BusinessServiceImports.getBaseImport(eq(model), eq(true)))
                    .thenReturn("//BASE_IMPORTS\n");
            bsImports.when(() -> BusinessServiceImports.computeModelsEnumsAndServiceImports(
                    eq(model), eq("out"), eq(BusinessServiceImportScope.BUSINESS_SERVICE), eq(pkgConfig)
            )).thenReturn("//MODEL_IMPORTS\n");

            ctx.when(() -> BusinessServiceTemplateContext.computeBusinessServiceContext(eq(model)))
                    .thenReturn(new HashMap<>());
            ctx.when(() -> BusinessServiceTemplateContext.computeCreateResourceMethodServiceContext(eq(model), eq(allEntities)))
                    .thenReturn(new HashMap<>());
            ctx.when(() -> BusinessServiceTemplateContext.computeAddRelationMethodServiceContext(eq(model), eq(allEntities)))
                    .thenReturn(new HashMap<>());
            ctx.when(() -> BusinessServiceTemplateContext.computeRemoveRelationMethodServiceContext(eq(model), eq(allEntities)))
                    .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("businessservice/method/create-resource.ftl"), anyMap()
            )).thenReturn("CREATE_METHOD");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("businessservice/method/add-relation.ftl"), anyMap()
            )).thenReturn("ADD_METHOD");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("businessservice/method/remove-relation.ftl"), anyMap()
            )).thenReturn("REMOVE_METHOD");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("businessservice/business-service-class-template.ftl"), anyMap()
            )).thenReturn("CLASS_BODY");

            final AtomicReference<String> writtenClassName = new AtomicReference<>();
            final AtomicReference<String> writtenContent = new AtomicReference<>();

            writer.when(() -> FileWriterUtils.writeToFile(
                    eq("out"),
                    eq("service"),
                    anyString(),
                    anyString()
            )).thenAnswer(invocation -> {
                writtenClassName.set(invocation.getArgument(2, String.class));
                writtenContent.set(invocation.getArgument(3, String.class));
                return null;
            });

            generator.generate(model, "out");
            assertEquals("UserBusinessService", writtenClassName.get());

            final String content = writtenContent.get();
            assertNotNull(content);

            assertTrue(content.contains("//BASE_IMPORTS"));
            assertTrue(content.contains("//MODEL_IMPORTS"));
            assertTrue(content.contains("CLASS_BODY"));
            assertTrue(content.contains("java.util.UUID"));
        }
    }
    
}
