package com.markozivkovic.codegen.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringWriter;
import java.util.Map;

public class FreeMarkerTemplateProcessorUtils {

    private static final String TEMPLATES = "templates";
    private static final Configuration cfg;

    private FreeMarkerTemplateProcessorUtils() {

    }

    static {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(FreeMarkerTemplateProcessorUtils.class.getClassLoader(), TEMPLATES);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
    }

    /**
     * Processes the given FreeMarker template with the provided data model and returns the result as a string.
     * 
     * @param templatePath the path to the FreeMarker template file
     * @param dataModel the map of data to be used when processing the template
     * @return the processed template as a string
     * @throws RuntimeException if an exception occurs during template processing
     */
    public static String processTemplate(final String templatePath, final Map<String, Object> dataModel) {

        try (final StringWriter out = new StringWriter()) {
            final Template template = cfg.getTemplate(templatePath);
            template.process(dataModel, out);
            return out.toString();
        } catch (final Exception e) {
            throw new RuntimeException("Error processing template: " + templatePath, e);
        }
    }
}
