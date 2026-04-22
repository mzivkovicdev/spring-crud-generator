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

package dev.markozivkovic.springcrudgenerator.generators;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;
import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

public class UtilsGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsGenerator.class);
    private static final String CLASS_NAME = "ArgumentVerifier";
    private static final String TEST_CLASS_NAME = "ArgumentVerifierTest";
    private static final String ET_ARGUMENT_EXCEPTION = "EtArgumentException";

    private final PackageConfiguration packageConfiguration;

    public UtilsGenerator(final PackageConfiguration packageConfiguration) {
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final String outputDir) {

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.ARGUMENT_VERIFIER)) { return; }

        LOGGER.info("Generating argument verifier");

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String exceptionPackage = PackageUtils.computeExceptionPackage(packagePath, packageConfiguration);
        final String utilsPackage = PackageUtils.computeUtilsPackage(packagePath, packageConfiguration);
        final String utilsSubPackage = PackageUtils.computeUtilsSubPackage(packageConfiguration);

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, utilsPackage));
        sb.append(
                FreeMarkerTemplateProcessorUtils.processTemplate(
                        "utils/argument-verifier-template.ftl",
                        Map.of(
                                TemplateContextConstants.PROJECT_IMPORTS,
                                String.format(IMPORT, PackageUtils.join(exceptionPackage, ET_ARGUMENT_EXCEPTION))
                        )
                )
        );
        final String testOutputDir = outputDir.replace("main", "test");
        final StringBuilder testSb = new StringBuilder();
        testSb.append(String.format(PACKAGE, utilsPackage));
        testSb.append(
                FreeMarkerTemplateProcessorUtils.processTemplate(
                        "test/unit/utils/argument-verifier-test-template.ftl",
                        Map.of(
                                TemplateContextConstants.PROJECT_IMPORTS,
                                String.format(IMPORT, PackageUtils.join(exceptionPackage, ET_ARGUMENT_EXCEPTION))
                        )
                )
        );

        FileWriterUtils.writeToFile(outputDir, utilsSubPackage, CLASS_NAME, sb.toString());
        FileWriterUtils.writeToFile(testOutputDir, utilsSubPackage, TEST_CLASS_NAME, testSb.toString());

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.ARGUMENT_VERIFIER);

        LOGGER.info("Finished generating argument verifier");
    }
}
