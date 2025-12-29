package dev.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.ImportConstants;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.JpaRepositoryTemplateContext;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

class JpaRepositoryGeneratorTest {

    private ModelDefinition newModel(final String name, final  List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenModelHasNoIdField() {
        
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final JpaRepositoryGenerator generator = new JpaRepositoryGenerator(pkgConfig);

        final ModelDefinition model = newModel("UserEntity", Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<JpaRepositoryTemplateContext> ctx = mockStatic(JpaRepositoryTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(false);

            generator.generate(model, "out");

            fieldUtils.verify(() -> FieldUtils.isAnyFieldId(model.getFields()));
            fieldUtils.verifyNoMoreInteractions();
            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            ctx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateRepositoryWithoutUuidImportWhenIdIsNotUuid() {

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final JpaRepositoryGenerator generator = new JpaRepositoryGenerator(pkgConfig);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("UserEntity", List.of(idField));

        final AtomicReference<String> writtenClassName = new AtomicReference<>();
        final AtomicReference<String> writtenContent = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<JpaRepositoryTemplateContext> ctx = mockStatic(JpaRepositoryTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeRepositoryPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.repository");
            pkg.when(() -> PackageUtils.computeRepositorySubPackage(pkgConfig))
                    .thenReturn("repository");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.join("com.example.app.entity", "UserEntity"))
                    .thenReturn("com.example.app.entity.UserEntity");

            final Map<String, Object> repoCtx = Map.of("key", "value");
            ctx.when(() -> JpaRepositoryTemplateContext.computeJpaInterfaceContext(model))
                    .thenReturn(repoCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("repository/repository-interface-template.ftl"), eq(repoCtx)))
                    .thenReturn("// REPO BODY");

            writer.when(() -> FileWriterUtils.writeToFile(eq("out"), eq("repository"), anyString(), anyString()))
                .thenAnswer(inv -> {
                    writtenClassName.set(inv.getArgument(2, String.class));
                    writtenContent.set(inv.getArgument(3, String.class));
                    return null;
                });

            generator.generate(model, "out");
        }

        assertEquals("UserRepository", writtenClassName.get(), "Repository class name should be based on stripped model name");

        final String content = writtenContent.get();

        assertNotNull(content);
        assertFalse(content.contains(ImportConstants.Java.UUID),
                "Content should NOT contain UUID import when id is not UUID");

        assertTrue(content.contains(ImportConstants.SpringData.JPA_REPOSITORY),
                "Content should contain JpaRepository import");

        assertTrue(content.contains("com.example.app.entity.UserEntity"),
                "Content should contain entity import");
        
        assertTrue(content.contains("// REPO BODY"),
                "Content should contain repository body from template");
    }

    @Test
    void generate_shouldGenerateRepositoryWithUuidImportWhenIdIsUuid() {

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final JpaRepositoryGenerator generator = new JpaRepositoryGenerator(pkgConfig);

        final FieldDefinition idField = mock(FieldDefinition.class);
        final ModelDefinition model = newModel("OrderEntity", List.of(idField));

        final AtomicReference<String> writtenClassName = new AtomicReference<>();
        final AtomicReference<String> writtenContent = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<JpaRepositoryTemplateContext> ctx = mockStatic(JpaRepositoryTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(model.getFields()))
                    .thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField))
                    .thenReturn(true);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity"))
                    .thenReturn("Order");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeRepositoryPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.repository");
            pkg.when(() -> PackageUtils.computeRepositorySubPackage(pkgConfig))
                    .thenReturn("repository");
            pkg.when(() -> PackageUtils.computeEntityPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.entity");
            pkg.when(() -> PackageUtils.join("com.example.app.entity", "OrderEntity"))
                    .thenReturn("com.example.app.entity.OrderEntity");

            final Map<String, Object> repoCtx = Map.of("key", "value");
            ctx.when(() -> JpaRepositoryTemplateContext.computeJpaInterfaceContext(model))
                    .thenReturn(repoCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("repository/repository-interface-template.ftl"),
                    eq(repoCtx)
            )).thenReturn("// ORDER REPO BODY");

            writer.when(() -> FileWriterUtils.writeToFile(
                    eq("out"),
                    eq("repository"),
                    anyString(),
                    anyString()
            )).thenAnswer(inv -> {
                writtenClassName.set(inv.getArgument(2, String.class));
                writtenContent.set(inv.getArgument(3, String.class));
                return null;
            });

            generator.generate(model, "out");
        }

        assertEquals("OrderRepository", writtenClassName.get());

        final String content = writtenContent.get();
        assertNotNull(content);

        assertTrue(content.contains(ImportConstants.Java.UUID),
                "Content should contain UUID import when id field is UUID");

        assertTrue(content.contains(ImportConstants.SpringData.JPA_REPOSITORY),
                "Content should contain JpaRepository import");

        assertTrue(content.contains("com.example.app.entity.OrderEntity"),
                "Content should contain entity import");

        assertTrue(content.contains("// ORDER REPO BODY"),
                "Content should contain body from template");
    }
}
