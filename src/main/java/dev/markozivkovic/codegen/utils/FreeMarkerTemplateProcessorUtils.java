/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.codegen.utils;

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
