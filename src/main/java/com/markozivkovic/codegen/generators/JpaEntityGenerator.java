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

package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.AnnotationConstants.AUDITING_ENTITY_LISTENER_CLASS;
import static com.markozivkovic.codegen.constants.AnnotationConstants.ENTITY_ANNOTATION;
import static com.markozivkovic.codegen.constants.AnnotationConstants.ENTITY_LISTENERS_ANNOTATION;
import static com.markozivkovic.codegen.constants.AnnotationConstants.TABLE_ANNOTATION;
import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants.GeneratorContextKeys;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.imports.ModelImports;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.templates.JpaEntityTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

/**
 * Generates a JPA entity class based on the provided model definition.
 * The generated class includes fields, getters, setters, equals, hashCode, and toString methods.
 */
public class JpaEntityGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaEntityGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public JpaEntityGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
                final PackageConfiguration packageConfiguration) {
        this.configuration = configuration;
        this.entities = entities;
        this.packageConfiguration = packageConfiguration;
    }
    
    /**
     * Generates a JPA entity class based on the provided model definition.
     * The generated class includes fields, getters, setters, equals, hashCode, and toString methods.
     * 
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     */
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generator JPA entity for model: {}", modelDefinition.getName());
        
        modelDefinition.getFields().stream()
                .filter(FieldUtils::isJsonField)
                .forEach(field -> {

                    final String jsonFieldName = FieldUtils.extractJsonFieldName(field);
                    final ModelDefinition jsonModel = this.entities.stream()
                            .filter(model -> model.getName().equals(jsonFieldName))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                String.format(
                                    "JSON model not found: %s", jsonFieldName
                                )
                            ));
                    
                    this.generateHelperEntity(jsonModel, outputDir);
                });
       
        this.generateJpaEntity(modelDefinition, outputDir);

        this.generateJpaAuditingConfiguration(modelDefinition, outputDir);
        
        LOGGER.info("Generator JPA entity finished for model: {}", modelDefinition.getName());
    }

    /**
     * Generates a helper JPA entity class for the given model definition.
     * 
     * @param model     The model definition containing the class name and field definitions.
     * @param outputDir The directory where the generated helper class will be written.
     */
    private void generateHelperEntity(final ModelDefinition model, final String outputDir) {
        
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        
        final String className = model.getName();

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration)));
        sb.append(ModelImports.getBaseImport(model, true, false));

        final String enumImports = ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, packageConfiguration);
        
        if (StringUtils.isNotBlank(enumImports)) {
            sb.append(enumImports)
                .append(System.lineSeparator());
        }

        final Map<String, Object> classContext = JpaEntityTemplateContext.computeJpaModelContext(model);
        classContext.put("embedded", true);

        final String fieldsTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/fields-template.ftl", classContext);
        final String defaultConstructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/default-constructor-template.ftl", classContext);
        final String constructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/constructor-template.ftl", classContext);
        final String gettersAndSetters = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/getters-setters-template.ftl", classContext);
        final String equals = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/equals-template.ftl", classContext);
        final String hashCode = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/hashcode-template.ftl", classContext);
        final String toString = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/tostring-template.ftl", classContext);

        final Map<String, Object> classTemplateContext = Map.of(
                "fields", fieldsTemplate,
                "defaultConstructor", defaultConstructor,
                "constructor", constructor,
                "gettersAndSetters", gettersAndSetters,
                "hashCode", equals,
                "equals", hashCode,
                "toString", toString,
                "className", className
        );

        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate("model/model-class-template.ftl", classTemplateContext));
        
        FileWriterUtils.writeToFile(
                outputDir, PackageUtils.computeHelperEntitySubPackage(packageConfiguration), className, sb.toString()
        );
    }

    /**
     * Generates a Java entity class file for the given model definition.
     *
     * @param model The model definition containing the class name, table name, and field definitions.
     */
    private void generateJpaEntity(final ModelDefinition model, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(model, this.entities)) {
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        
        final String className = model.getName();
        final String tableName = model.getStorageName();
        final boolean optimisticLocking = (Objects.nonNull(configuration) && Objects.nonNull(configuration.isOptimisticLocking())) ?
                configuration.isOptimisticLocking() : false;

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, PackageUtils.computeEntityPackage(packagePath, packageConfiguration)));
        sb.append(ModelImports.getBaseImport(model, true, true));
                
        sb.append(ModelImports.computeJakartaImports(model, optimisticLocking))
                .append(System.lineSeparator());

        final String enumAndHelperEntitiesImports = ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, packageConfiguration);
        
        if (StringUtils.isNotBlank(enumAndHelperEntitiesImports)) {
            sb.append(enumAndHelperEntitiesImports)
                .append(System.lineSeparator());
        }

        sb.append(ENTITY_ANNOTATION)
                .append(System.lineSeparator())
                .append(String.format(TABLE_ANNOTATION, tableName))
                .append(System.lineSeparator());

        if (Objects.nonNull(model.getAudit()) && model.getAudit().isEnabled()) {
            sb.append(String.format(ENTITY_LISTENERS_ANNOTATION, AUDITING_ENTITY_LISTENER_CLASS))
                    .append(System.lineSeparator());
        }

        final Map<String, Object> classContext = JpaEntityTemplateContext.computeJpaModelContext(model);
        classContext.put("optimisticLocking", optimisticLocking);
        
        final String fieldsTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/fields-template.ftl", classContext);
        final String defaultConstructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/default-constructor-template.ftl", classContext);
        final String constructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/constructor-template.ftl", classContext);
        final String gettersAndSetters = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/getters-setters-template.ftl", classContext);
        final String equals = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/equals-template.ftl", classContext);
        final String hashCode = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/hashcode-template.ftl", classContext);
        final String toString = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/tostring-template.ftl", classContext);

        final Map<String, Object> classTemplateContext = Map.of(
                "fields", fieldsTemplate,
                "defaultConstructor", defaultConstructor,
                "constructor", constructor,
                "gettersAndSetters", gettersAndSetters,
                "hashCode", equals,
                "equals", hashCode,
                "toString", toString,
                "className", className
        );

        sb.append(FreeMarkerTemplateProcessorUtils.processTemplate("model/model-class-template.ftl", classTemplateContext));

        FileWriterUtils.writeToFile(outputDir, PackageUtils.computeEntitySubPackage(packageConfiguration), className, sb.toString());
    }

    /**
     * Generates a JPA auditing configuration class if the model has auditing enabled.
     *
     * @param model     The model definition containing the class name, table name, and field definitions.
     * @param outputDir The directory where the generated configuration code will be written.
     */
    private void generateJpaAuditingConfiguration(final ModelDefinition model, final String outputDir) {

        if (GeneratorContext.isGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG)) return;

        if (Objects.nonNull(model.getAudit()) && model.getAudit().isEnabled()) {

            final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
            final StringBuilder sb = new StringBuilder();
        
            sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                    .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "configuration/jpa-auditing-configuration.ftl", Map.of()
                    ));
            
            FileWriterUtils.writeToFile(
                outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration),
                "EnableAuditingConfiguration.java", sb.toString()
            );

            GeneratorContext.markGenerated(GeneratorContextKeys.JPA_AUDITING_CONFIG);
        }
    }

}
