package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class UtilsGeneratorTest {

    @Test
    void generate_shouldReturnWhenAlreadyGenerated() {

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final UtilsGenerator generator = new UtilsGenerator(packageConfiguration);

        try (final MockedStatic<GeneratorContext> generatorContext = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> packageUtils = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> templates = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generatorContext.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ARGUMENT_VERIFIER))
                    .thenReturn(true);

            generator.generate("out");

            generatorContext.verify(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ARGUMENT_VERIFIER));
            generatorContext.verifyNoMoreInteractions();
            packageUtils.verifyNoInteractions();
            templates.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldWriteArgumentVerifierAndMarkGenerated() {

        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final UtilsGenerator generator = new UtilsGenerator(packageConfiguration);
        final AtomicInteger markGeneratedCount = new AtomicInteger(0);
        final String outputDir = "out";
        final String testOutputDir = "out";

        try (final MockedStatic<GeneratorContext> generatorContext = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> packageUtils = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> templates = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            generatorContext.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ARGUMENT_VERIFIER))
                    .thenReturn(false);
            generatorContext.when(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.ARGUMENT_VERIFIER))
                    .thenAnswer(invocation -> {
                        markGeneratedCount.incrementAndGet();
                        return null;
                    });

            packageUtils.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                    .thenReturn("com.example");
            packageUtils.when(() -> PackageUtils.computeUtilsPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.utils");
            packageUtils.when(() -> PackageUtils.computeExceptionPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exceptions");
            packageUtils.when(() -> PackageUtils.join("com.example.exceptions", "EtArgumentException"))
                    .thenReturn("com.example.exceptions.EtArgumentException");
            packageUtils.when(() -> PackageUtils.computeUtilsSubPackage(packageConfiguration))
                    .thenReturn("utils");

            templates.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    "utils/argument-verifier-template.ftl",
                    Map.of(
                            TemplateContextConstants.PROJECT_IMPORTS,
                            "import com.example.exceptions.EtArgumentException;\n"
                    )
            )).thenReturn("ARGUMENT_VERIFIER_TEMPLATE");
            templates.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    "test/unit/utils/argument-verifier-test-template.ftl",
                    Map.of(
                            TemplateContextConstants.PROJECT_IMPORTS,
                            "import com.example.exceptions.EtArgumentException;\n"
                    )
            )).thenReturn("ARGUMENT_VERIFIER_TEST_TEMPLATE");

            generator.generate(outputDir);

            writer.verify(() -> FileWriterUtils.writeToFile(
                    outputDir,
                    "utils",
                    "ArgumentVerifier",
                    "package com.example.utils;\n\nARGUMENT_VERIFIER_TEMPLATE"
            ));
            writer.verify(() -> FileWriterUtils.writeToFile(
                    testOutputDir,
                    "utils",
                    "ArgumentVerifierTest",
                    "package com.example.utils;\n\nARGUMENT_VERIFIER_TEST_TEMPLATE"
            ));
            assertEquals(1, markGeneratedCount.get(), "GeneratorContext.markGenerated(ARGUMENT_VERIFIER) should be called once");
        }
    }
}
