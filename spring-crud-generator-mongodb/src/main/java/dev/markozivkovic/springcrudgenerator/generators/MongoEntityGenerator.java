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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.imports.ModelImports;
import dev.markozivkovic.springcrudgenerator.imports.MongoModelImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.JpaEntityTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

/**
 * Generates Spring Data MongoDB document classes.
 */
public class MongoEntityGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoEntityGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public MongoEntityGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
            final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        LOGGER.info("Generating MongoDB document for model: {}", modelDefinition.getName());

        modelDefinition.getFields().stream()
                .filter(FieldUtils::isJsonField)
                .forEach(field -> {
                    final String jsonInnerElementType = FieldUtils.extractJsonInnerElementType(field);
                    final ModelDefinition jsonModel = this.entities.stream()
                            .filter(model -> model.getName().equals(jsonInnerElementType))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    String.format("JSON model not found: %s", jsonInnerElementType)
                            ));
                    this.generateMongoHelperEntity(jsonModel, outputDir);
                });

        this.generateMongoDocument(modelDefinition, outputDir);

        LOGGER.info("MongoDB document generation completed for model: {}", modelDefinition.getName());
    }

    /**
     * Generates a MongoDB helper entity class based on the provided model definition.
     * The helper entity class is used to represent JSON fields in MongoDB documents.
     *
     * @param model     The model definition containing the class name, table name, and field definitions.
     * @param outputDir The directory where the generated code will be written.
     */
    private void generateMongoHelperEntity(final ModelDefinition model, final String outputDir) {

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String className = model.getName();

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeHelperEntityPackage(packagePath, this.packageConfiguration)));
        sb.append(ModelImports.getBaseImport(model, true, true));

        final String mongoImports = MongoModelImports.computeMongoModelImports(model, false);
        if (StringUtils.isNotBlank(mongoImports)) {
            sb.append(mongoImports).append(System.lineSeparator());
        }

        final String enumImports = ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, this.packageConfiguration);
        if (StringUtils.isNotBlank(enumImports)) {
            sb.append(enumImports).append(System.lineSeparator());
        }

        sb.append(this.computeMongoClassBody(model, false, true));

        FileWriterUtils.writeToFile(
                outputDir,
                PackageUtils.computeHelperEntitySubPackage(this.packageConfiguration),
                className,
                sb.toString()
        );
    }

    /**
     * Generates a MongoDB document entity class based on the provided model definition.
     * The MongoDB document entity class is used to represent MongoDB documents.
     * 
     * @param model     The model definition containing the class name, table name, and field definitions.
     * @param outputDir The directory where the generated code will be written.
     */
    private void generateMongoDocument(final ModelDefinition model, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(model, this.entities)) {
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String className = model.getName();

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeEntityPackage(packagePath, this.packageConfiguration)));
        sb.append(ModelImports.getBaseImport(model, true, true));

        final String mongoImports = MongoModelImports.computeMongoModelImports(model, true);
        if (StringUtils.isNotBlank(mongoImports)) {
            sb.append(mongoImports).append(System.lineSeparator());
        }

        final String enumAndHelperImports = ModelImports.computeEnumsAndHelperEntitiesImport(
                model, outputDir, this.packageConfiguration
        );
        if (StringUtils.isNotBlank(enumAndHelperImports)) {
            sb.append(enumAndHelperImports).append(System.lineSeparator());
        }

        sb.append(this.computeMongoClassBody(model, true, false));

        FileWriterUtils.writeToFile(
                outputDir,
                PackageUtils.computeEntitySubPackage(this.packageConfiguration),
                className,
                sb.toString()
        );
    }

    /**
     * Computes the body of a MongoDB document class based on the provided model definition.
     * The computed body includes fields, default constructor, constructor, getters, setters, equals, hashCode, and toString methods.
     * 
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param document        whether the class being generated is a MongoDB document class
     * @param embedded        whether the class being generated is an embedded MongoDB document class
     * @return the computed body of the MongoDB document class
     */
    private String computeMongoClassBody(final ModelDefinition modelDefinition, final boolean document, final boolean embedded) {

        final Map<String, Object> classContext = JpaEntityTemplateContext.computeJpaModelContext(modelDefinition);
        classContext.put("db", this.configuration.getDatabase().name());
        classContext.put("embedded", embedded);

        final String fieldsTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "model/component/mongo-fields-template.ftl", classContext
        );
        final String defaultConstructor = FreeMarkerTemplateProcessorUtils.processTemplate(
                "model/component/default-constructor-template.ftl", classContext
        );
        final String constructor = FreeMarkerTemplateProcessorUtils.processTemplate(
                "model/component/constructor-template.ftl", classContext
        );
        final String gettersAndSetters = FreeMarkerTemplateProcessorUtils.processTemplate(
                "model/component/getters-setters-template.ftl", classContext
        );
        final String equals = FreeMarkerTemplateProcessorUtils.processTemplate(
                "model/component/equals-template.ftl", classContext
        );
        final String hashCode = FreeMarkerTemplateProcessorUtils.processTemplate(
                "model/component/hashcode-template.ftl", classContext
        );
        final String toString = FreeMarkerTemplateProcessorUtils.processTemplate(
                "model/component/tostring-template.ftl", classContext
        );

        final Map<String, Object> classTemplateContext = new HashMap<>();
        classTemplateContext.put("document", document);
        classTemplateContext.put("storageName", modelDefinition.getStorageName());
        classTemplateContext.put("fields", fieldsTemplate);
        classTemplateContext.put("defaultConstructor", defaultConstructor);
        classTemplateContext.put("constructor", constructor);
        classTemplateContext.put("gettersAndSetters", gettersAndSetters);
        classTemplateContext.put("hashCode", hashCode);
        classTemplateContext.put("equals", equals);
        classTemplateContext.put("toString", toString);
        classTemplateContext.put("className", modelDefinition.getName());

        return FreeMarkerTemplateProcessorUtils.processTemplate("model/mongo-document-class-template.ftl", classTemplateContext);
    }
}
