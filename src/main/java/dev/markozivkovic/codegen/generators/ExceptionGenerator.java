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

package dev.markozivkovic.codegen.generators;

import static dev.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

public class ExceptionGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionGenerator.class);

    private static final List<String> EXCEPTION_CLASS_LIST = List.of(
            "ResourceNotFoundException", "InvalidResourceStateException"
    );

    private final PackageConfiguration packageConfiguration;

    public ExceptionGenerator(final PackageConfiguration packageConfiguration) {
        this.packageConfiguration = packageConfiguration;
    }
    
    @Override
    public void generate(final String outputDir) {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.EXCEPTIONS)) { return; }

        LOGGER.info("Generating exceptions");
        
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        EXCEPTION_CLASS_LIST.forEach(exceptionClassName -> {
            
            final StringBuilder sb = new StringBuilder();
            
            final String exceptionTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "exception/exception-template.ftl",
                Map.of(TemplateContextConstants.CLASS_NAME, exceptionClassName)
            );

            sb.append(String.format(PACKAGE, PackageUtils.computeExceptionPackage(packagePath, packageConfiguration)))
                    .append(exceptionTemplate);
    
            FileWriterUtils.writeToFile(
                    outputDir, PackageUtils.computeExceptionSubPackage(packageConfiguration), exceptionClassName, sb.toString()
            );
        });

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.EXCEPTIONS);
        
        LOGGER.info("Finished generating exceptions");
    }

}
