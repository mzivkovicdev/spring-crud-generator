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

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.imports.RepositoryImports;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

/**
 * Generates Spring Data MongoDB repository interfaces.
 */
public class MongoRepositoryGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoRepositoryGenerator.class);

    private final PackageConfiguration packageConfiguration;

    public MongoRepositoryGenerator(final PackageConfiguration packageConfiguration) {
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        LOGGER.info("Generating MongoDB repository for model: {}", modelDefinition.getName());

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            LOGGER.warn("Model {} does not have an ID field. Skipping repository generation.", modelDefinition.getName());
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String className = String.format("%sRepository", ModelNameUtils.stripSuffix(modelDefinition.getName()));
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeRepositoryPackage(packagePath, this.packageConfiguration)));
        sb.append(RepositoryImports.computeMongoRepositoryImports(packagePath, this.packageConfiguration, modelDefinition.getName()));
        sb.append(System.lineSeparator());

        final Map<String, Object> context = Map.of(
                "className", className,
                "modelName", modelDefinition.getName(),
                "idType", idField.getType()
        );

        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate("repository/mongo-repository-interface-template.ftl", context));

        FileWriterUtils.writeToFile(
                outputDir,
                PackageUtils.computeRepositorySubPackage(this.packageConfiguration),
                className,
                sb.toString()
        );

        LOGGER.info("MongoDB repository generation completed for model: {}", modelDefinition.getName());
    }
}
