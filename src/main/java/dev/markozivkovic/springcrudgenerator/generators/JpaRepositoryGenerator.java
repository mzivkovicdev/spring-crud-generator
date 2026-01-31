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

public class JpaRepositoryGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRepositoryGenerator.class);

    private final CrudConfiguration crudConfiguration;
    private final PackageConfiguration packageConfiguration;

    public JpaRepositoryGenerator(final CrudConfiguration crudConfiguration, final PackageConfiguration packageConfiguration) {
        this.crudConfiguration = crudConfiguration;
        this.packageConfiguration = packageConfiguration;
    }

    /**
     * Generates a JPA repository interface for the given model definition.
     * The generated repository extends JpaRepository and is placed in the
     * appropriate package based on the output directory.
     *
     * @param modelDefinition the model definition containing the class name and field definitions
     * @param outputDir       the directory where the generated repository code will be written
     */
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generating JPA repository for model: {}", modelDefinition.getName());

        final boolean hasIdField = FieldUtils.isAnyFieldId(modelDefinition.getFields());
        
        if (!hasIdField) {
            LOGGER.warn("Model {} does not have an ID field. Skipping repository generation.", modelDefinition.getName());
            return;
        }
        
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String className = String.format("%sRepository", ModelNameUtils.stripSuffix(modelDefinition.getName()));
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final boolean openInViewEnabled = AdditionalPropertiesUtils.isOpenInViewEnabled(this.crudConfiguration.getAdditionalProperties());

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeRepositoryPackage(packagePath, packageConfiguration)));

        if (FieldUtils.isIdFieldUUID(idField)) {
            sb.append(String.format(IMPORT, ImportConstants.Java.UUID))
                    .append(System.lineSeparator());
        }

        final Map<String, Object> context = JpaRepositoryTemplateContext.computeJpaInterfaceContext(
                modelDefinition, openInViewEnabled, packagePath, packageConfiguration
        );
        final String jpaInterface = FreeMarkerTemplateProcessorUtils.processTemplate(
                "repository/repository-interface-template.ftl", context
        );

        sb.append(jpaInterface);

        FileWriterUtils.writeToFile(outputDir, PackageUtils.computeRepositorySubPackage(packageConfiguration), className, sb.toString());
        
        LOGGER.info("JPA repository generation completed for model: {}", modelDefinition.getName());
    }
    
}
