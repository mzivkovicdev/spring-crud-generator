package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.EnumTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class EnumGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    private FieldDefinition newEnumField(final String name, final List<String> values) {
        final FieldDefinition f = mock(FieldDefinition.class);
        when(f.getName()).thenReturn(name);
        when(f.getValues()).thenReturn(values);
        return f;
    }

    @Test
    void generate_shouldSkipWhenModelHasNoEnumFields() {
        
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final ModelDefinition model = newModel("User", List.of(mock(FieldDefinition.class)));

        final EnumGenerator generator = new EnumGenerator(pkgConfig);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FieldDefinition> unused = mockStatic(FieldDefinition.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<EnumTemplateContext> enumCtx = mockStatic(EnumTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(model.getFields()))
                    .thenReturn(false);

            generator.generate(model, "out");

            fieldUtils.verify(() -> FieldUtils.isAnyFieldEnum(model.getFields()));
            fieldUtils.verifyNoMoreInteractions();
            pkg.verifyNoInteractions();
            enumCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateEnumForFieldWithoutEnumSuffix() {
        
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final FieldDefinition statusField = newEnumField("status", List.of("ACTIVE", "INACTIVE"));
        final ModelDefinition model = newModel("Order", List.of(statusField));

        final EnumGenerator generator = new EnumGenerator(pkgConfig);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<EnumTemplateContext> enumCtx = mockStatic(EnumTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(model.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractEnumFields(model.getFields()))
                    .thenReturn(List.of(statusField));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeEnumPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.enums");
            pkg.when(() -> PackageUtils.computeEnumSubPackage(pkgConfig))
                    .thenReturn("enums");

            final Map<String, Object> context = new HashMap<>();
            enumCtx.when(() -> EnumTemplateContext.createEnumContext("StatusEnum", statusField.getValues()))
                    .thenReturn(context);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("enum/enum-template.ftl"),
                    eq(context)
            )).thenReturn("// ENUM BODY");

            final AtomicReference<String> writtenClassName = new AtomicReference<>();
            final AtomicReference<String> writtenContent = new AtomicReference<>();

            writer.when(() -> FileWriterUtils.writeToFile(
                    eq("out"),
                    eq("enums"),
                    anyString(),
                    anyString()
            )).thenAnswer(invocation -> {
                writtenClassName.set(invocation.getArgument(2, String.class));
                writtenContent.set(invocation.getArgument(3, String.class));
                return null;
            });

            generator.generate(model, "out");

            assertEquals("StatusEnum", writtenClassName.get());

            final String content = writtenContent.get();
            assertNotNull(content);
            assertTrue(content.contains("// ENUM BODY"));
        }
    }

    @Test
    void generate_shouldNotAppendEnumSuffixIfAlreadyPresent() {
        
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final FieldDefinition statusEnumField = newEnumField("statusEnum", List.of("PENDING", "DONE"));
        final ModelDefinition model = newModel("Task", List.of(statusEnumField));

        final EnumGenerator generator = new EnumGenerator(pkgConfig);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<EnumTemplateContext> enumCtx = mockStatic(EnumTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldEnum(model.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractEnumFields(model.getFields()))
                    .thenReturn(List.of(statusEnumField));

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeEnumPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.enums");
            pkg.when(() -> PackageUtils.computeEnumSubPackage(pkgConfig))
                    .thenReturn("enums");

            final Map<String, Object> context = new HashMap<>();
            enumCtx.when(() -> EnumTemplateContext.createEnumContext("StatusEnum", statusEnumField.getValues()))
                    .thenReturn(context);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("enum/enum-template.ftl"),
                    eq(context)
            )).thenReturn("// ENUM STATUS");

            final AtomicReference<String> writtenClassName = new AtomicReference<>();
            final AtomicReference<String> writtenContent = new AtomicReference<>();

            writer.when(() -> FileWriterUtils.writeToFile(
                    eq("out"),
                    eq("enums"),
                    anyString(),
                    anyString()
            )).thenAnswer(invocation -> {
                writtenClassName.set(invocation.getArgument(2, String.class));
                writtenContent.set(invocation.getArgument(3, String.class));
                return null;
            });

            generator.generate(model, "out");

            assertEquals("StatusEnum", writtenClassName.get());
            assertNotNull(writtenContent.get());
            assertTrue(writtenContent.get().contains("// ENUM STATUS"));
        }
    }
}
