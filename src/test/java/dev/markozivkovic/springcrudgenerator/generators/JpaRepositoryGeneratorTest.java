package dev.markozivkovic.springcrudgenerator.generators;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.AdditionalConfigurationConstants;
import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.JpaRepositoryTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class JpaRepositoryGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void generate_shouldSkipWhenModelHasNoIdField() {

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);

        final JpaRepositoryGenerator generator = new JpaRepositoryGenerator(crudConfig, pkgConfig);
        final ModelDefinition model = newModel("UserEntity", Collections.emptyList());

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<JpaRepositoryTemplateContext> ctx = mockStatic(JpaRepositoryTemplateContext.class);
             final MockedStatic<AdditionalPropertiesUtils> props = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(model.getFields())).thenReturn(false);

            generator.generate(model, "out");

            fieldUtils.verify(() -> FieldUtils.isAnyFieldId(model.getFields()));
            fieldUtils.verifyNoMoreInteractions();

            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            ctx.verifyNoInteractions();
            props.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateRepositoryWithoutUuidImportWhenIdIsNotUuid() {

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        when(crudConfig.getAdditionalProperties()).thenReturn(null);

        final JpaRepositoryGenerator generator = new JpaRepositoryGenerator(crudConfig, pkgConfig);

        final FieldDefinition idField = new FieldDefinition();
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final AtomicReference<String> writtenClassName = new AtomicReference<>();
        final AtomicReference<String> writtenContent = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<JpaRepositoryTemplateContext> ctx = mockStatic(JpaRepositoryTemplateContext.class);
             final MockedStatic<AdditionalPropertiesUtils> props = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeRepositoryPackage("com.example.app", pkgConfig)).thenReturn("com.example.app.repository");
            pkg.when(() -> PackageUtils.computeRepositorySubPackage(pkgConfig)).thenReturn("repository");
            props.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(true);
            final Map<String, Object> repoCtx = Map.of("key", "value");
            ctx.when(() -> JpaRepositoryTemplateContext.computeJpaInterfaceContext(model, true, "com.example.app", pkgConfig))
                    .thenReturn(repoCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("repository/repository-interface-template.ftl"), eq(repoCtx)
            )).thenReturn("// REPO BODY");

            writer.when(() -> FileWriterUtils.writeToFile(eq("out"), eq("repository"), anyString(), anyString()))
                .thenAnswer(inv -> {
                    writtenClassName.set(inv.getArgument(2, String.class));
                    writtenContent.set(inv.getArgument(3, String.class));
                    return null;
                });

            generator.generate(model, "out");
        }

        assertEquals("UserRepository", writtenClassName.get());

        final String content = writtenContent.get();
        assertNotNull(content);
        assertFalse(content.contains("import " + ImportConstants.Java.UUID));
        assertTrue(content.contains("package com.example.app.repository"));
        assertTrue(content.contains("// REPO BODY"));
    }

    @Test
    void generate_shouldGenerateRepositoryWithUuidImportWhenIdIsUuid() {

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        when(crudConfig.getAdditionalProperties()).thenReturn(null);

        final JpaRepositoryGenerator generator = new JpaRepositoryGenerator(crudConfig, pkgConfig);

        final FieldDefinition idField = new FieldDefinition();
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("OrderEntity", fields);

        final AtomicReference<String> writtenClassName = new AtomicReference<>();
        final AtomicReference<String> writtenContent = new AtomicReference<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<JpaRepositoryTemplateContext> ctx = mockStatic(JpaRepositoryTemplateContext.class);
             final MockedStatic<AdditionalPropertiesUtils> props = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(true);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity")).thenReturn("Order");
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeRepositoryPackage("com.example.app", pkgConfig)).thenReturn("com.example.app.repository");
            pkg.when(() -> PackageUtils.computeRepositorySubPackage(pkgConfig)).thenReturn("repository");
            props.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(null)).thenReturn(true);

            final Map<String, Object> repoCtx = Map.of("key", "value");
            ctx.when(() -> JpaRepositoryTemplateContext.computeJpaInterfaceContext(model, true, "com.example.app", pkgConfig))
                    .thenReturn(repoCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("repository/repository-interface-template.ftl"), eq(repoCtx)
            )).thenReturn("// ORDER REPO BODY");

            writer.when(() -> FileWriterUtils.writeToFile(eq("out"), eq("repository"), anyString(), anyString()))
            .thenAnswer(inv -> {
                writtenClassName.set(inv.getArgument(2, String.class));
                writtenContent.set(inv.getArgument(3, String.class));
                return null;
            });

            generator.generate(model, "out");
        }

        assertEquals("OrderRepository", writtenClassName.get());

        final String content = writtenContent.get();
        assertNotNull(content);
        assertTrue(content.contains("import " + ImportConstants.Java.UUID));

        assertTrue(content.contains("package com.example.app.repository"));
        assertTrue(content.contains("// ORDER REPO BODY"));
    }

    @Test
    void generate_shouldPassOpenInViewFlagToTemplateContextBuilder() {

        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);

        final CrudConfiguration crudConfig = mock(CrudConfiguration.class);
        final Map<String, Object> additionalProps = new HashMap<>();
        additionalProps.put(AdditionalConfigurationConstants.JPA_OPEN_IN_VIEW, Boolean.FALSE);
        when(crudConfig.getAdditionalProperties()).thenReturn(additionalProps);

        final JpaRepositoryGenerator generator = new JpaRepositoryGenerator(crudConfig, pkgConfig);

        final FieldDefinition idField = new FieldDefinition();
        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<JpaRepositoryTemplateContext> ctx = mockStatic(JpaRepositoryTemplateContext.class);
             final MockedStatic<AdditionalPropertiesUtils> props = mockStatic(AdditionalPropertiesUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(fields)).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.isIdFieldUUID(idField)).thenReturn(false);

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out")).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeRepositoryPackage("com.example.app", pkgConfig)).thenReturn("com.example.app.repository");
            pkg.when(() -> PackageUtils.computeRepositorySubPackage(pkgConfig)).thenReturn("repository");
            props.when(() -> AdditionalPropertiesUtils.isOpenInViewEnabled(additionalProps)).thenReturn(false);

            final Map<String, Object> repoCtx = Map.of("ctx", "ok");
            ctx.when(() -> JpaRepositoryTemplateContext.computeJpaInterfaceContext(model, false, "com.example.app", pkgConfig))
                    .thenReturn(repoCtx);

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("repository/repository-interface-template.ftl"), eq(repoCtx)
            )).thenReturn("// BODY");
            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString())).thenAnswer(inv -> null);

            generator.generate(model, "out");

            ctx.verify(() -> JpaRepositoryTemplateContext.computeJpaInterfaceContext(model, false, "com.example.app", pkgConfig));
        }
    }
}
