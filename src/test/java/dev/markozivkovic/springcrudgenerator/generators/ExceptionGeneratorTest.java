package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class ExceptionGeneratorTest {
    
    @Test
    void generate_shouldReturnWhenExceptionsAlreadyGenerated() {
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final ExceptionGenerator generator = new ExceptionGenerator(pkgConfig);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.EXCEPTIONS))
                    .thenReturn(true);

            generator.generate("out");

            genCtx.verify(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.EXCEPTIONS));
            genCtx.verifyNoMoreInteractions();

            pkg.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateExceptionClassesAndMarkGenerated() {
        
        final PackageConfiguration pkgConfig = mock(PackageConfiguration.class);
        final ExceptionGenerator generator = new ExceptionGenerator(pkgConfig);

        final List<String> expectedExceptions = List.of(
                "ResourceNotFoundException",
                "InvalidResourceStateException"
        );

        final List<InvocationOnMock> templateInvocations = new ArrayList<>();
        final List<InvocationOnMock> writerInvocations = new ArrayList<>();
        final AtomicInteger markGeneratedCount = new AtomicInteger(0);

        try (final MockedStatic<GeneratorContext> genCtx = mockStatic(GeneratorContext.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl =
                     mockStatic(FreeMarkerTemplateProcessorUtils.class, invocation -> {
                         templateInvocations.add(invocation);
                         if ("processTemplate".equals(invocation.getMethod().getName())) {
                             String templateName = invocation.getArgument(0, String.class);
                             return "TEMPLATE-" + templateName;
                         }
                         return null;
                     });
            final MockedStatic<FileWriterUtils> writer =
                     mockStatic(FileWriterUtils.class, invocation -> {
                         writerInvocations.add(invocation);
                         return null;
                     })) {

            genCtx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.EXCEPTIONS))
                    .thenReturn(false);
            genCtx.when(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.EXCEPTIONS))
                    .thenAnswer(invocation -> {
                        markGeneratedCount.incrementAndGet();
                        return null;
                    });

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir("out"))
                    .thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeExceptionPackage("com.example.app", pkgConfig))
                    .thenReturn("com.example.app.exception");
            pkg.when(() -> PackageUtils.computeExceptionSubPackage(pkgConfig))
                    .thenReturn("exception");

            generator.generate("out");

            assertEquals(expectedExceptions.size(), templateInvocations.stream()
                    .filter(inv -> "processTemplate".equals(inv.getMethod().getName()))
                    .count());

            for (String exceptionName : expectedExceptions) {
                final boolean found = templateInvocations.stream()
                        .filter(inv -> "processTemplate".equals(inv.getMethod().getName()))
                        .anyMatch(inv -> {
                            final String templateName = inv.getArgument(0, String.class);
                            if (!"exception/exception-template.ftl".equals(templateName)) {
                                return false;
                            }
                            @SuppressWarnings("unchecked")
                            final Map<String, Object> ctx = inv.getArgument(1, Map.class);
                            return exceptionName.equals(ctx.get(TemplateContextConstants.CLASS_NAME));
                        });

                assertTrue(found, "Template should be called for exception: " + exceptionName);
            }

            for (final String exceptionName : expectedExceptions) {
                final boolean wrote = writerInvocations.stream()
                        .filter(inv -> "writeToFile".equals(inv.getMethod().getName()))
                        .anyMatch(inv ->
                                "out".equals(inv.getArgument(0)) &&
                                "exception".equals(inv.getArgument(1)) &&
                                exceptionName.equals(inv.getArgument(2)) &&
                                inv.getArgument(3).toString().contains("TEMPLATE-exception/exception-template.ftl")
                        );
                assertTrue(wrote, "FileWriterUtils.writeToFile should be called for " + exceptionName);
            }

            assertEquals(1, markGeneratedCount.get(), "GeneratorContext.markGenerated(EXCEPTIONS) should be called once");
        }
    }

}
