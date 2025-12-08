package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;

class FreeMarkerTemplateProcessorUtilsTest {

    @TempDir
    Path tempDir;

    private Configuration getInternalConfiguration() throws Exception {
        final var cfgField = FreeMarkerTemplateProcessorUtils.class.getDeclaredField("cfg");
        cfgField.setAccessible(true);
        return (Configuration) cfgField.get(null);
    }

    @AfterEach
    void resetTemplateLoader() throws Exception {
        final Configuration cfg = getInternalConfiguration();
        cfg.setClassLoaderForTemplateLoading(
                FreeMarkerTemplateProcessorUtils.class.getClassLoader(),
                "templates"
        );
    }

    @Test
    @DisplayName("processTemplate renders template from physically created .ftl file")
    void processTemplate_shouldRenderTemplate_fromPhysicalFile() throws Exception {

        final Path templateFile = tempDir.resolve("test.ftl");
        final String templateContent = "Hello ${name}! Today is ${day}.";
        Files.writeString(templateFile, templateContent);

        final Configuration cfg = getInternalConfiguration();
        cfg.setTemplateLoader(new FileTemplateLoader(tempDir.toFile()));

        final String templatePath = "test.ftl";
        final Map<String, Object> dataModel = Map.of(
                "name", "Test",
                "day", "Friday"
        );

        final String result = FreeMarkerTemplateProcessorUtils.processTemplate(templatePath, dataModel);

        assertNotNull(result);
        assertEquals("Hello Test! Today is Friday.", result.trim());
    }

    @Test
    @DisplayName("processTemplate wraps exceptions in RuntimeException when template does not exist")
    void processTemplate_shouldWrapException_whenTemplateNotFound() throws Exception {

        final Configuration cfg = getInternalConfiguration();
        cfg.setTemplateLoader(new FileTemplateLoader(tempDir.toFile()));

        final String missingTemplate = "missing.ftl";

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> FreeMarkerTemplateProcessorUtils.processTemplate(missingTemplate, Map.of())
        );

        assertTrue(ex.getMessage().contains("Error processing template: missing.ftl"));
        assertNotNull(ex.getCause());
    }
}
