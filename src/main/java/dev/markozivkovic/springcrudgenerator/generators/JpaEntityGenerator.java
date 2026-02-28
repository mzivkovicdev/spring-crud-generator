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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants.GeneratorContextKeys;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.imports.ModelImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.JpaEntityTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

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

                    final String jsonInnerElementType = FieldUtils.extractJsonInnerElementType(field);
                    final ModelDefinition jsonModel = this.entities.stream()
                            .filter(model -> model.getName().equals(jsonInnerElementType))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                String.format(
                                    "JSON model not found: %s", jsonInnerElementType
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
                "hashCode", hashCode,
                "equals", equals,
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
        final FieldDefinition idField = FieldUtils.extractIdField(model.getFields());
        final boolean importSequenceIfAutoStrategy = (DatabaseType.POSTGRESQL.equals(this.configuration.getDatabase())
                || DatabaseType.MSSQL.equals(this.configuration.getDatabase())
                || DatabaseType.MYSQL.equals(this.configuration.getDatabase())
                || DatabaseType.MARIADB.equals(this.configuration.getDatabase()))
                && IdStrategyEnum.AUTO.equals(idField.getId().getStrategy());
        
        final String className = model.getName();
        final String tableName = model.getStorageName();
        final boolean optimisticLocking = (Objects.nonNull(configuration) && Objects.nonNull(configuration.isOptimisticLocking())) ?
                configuration.isOptimisticLocking() : false;
        final boolean openInViewEnabled = AdditionalPropertiesUtils.isOpenInViewEnabled(this.configuration.getAdditionalProperties());

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, PackageUtils.computeEntityPackage(packagePath, packageConfiguration)));
        sb.append(ModelImports.getBaseImport(model, true, true));
                
        sb.append(ModelImports.computeJakartaImports(model, optimisticLocking, importSequenceIfAutoStrategy, openInViewEnabled))
                .append(System.lineSeparator());

        final String enumAndHelperEntitiesImports = ModelImports.computeEnumsAndHelperEntitiesImport(model, outputDir, packageConfiguration);
        
        if (StringUtils.isNotBlank(enumAndHelperEntitiesImports)) {
            sb.append(enumAndHelperEntitiesImports)
                .append(System.lineSeparator());
        }

        final Map<String, Object> classContext = JpaEntityTemplateContext.computeJpaModelContext(model);
        classContext.put(TemplateContextConstants.OPTIMISTIC_LOCKING, optimisticLocking);
        classContext.put("db", this.configuration.getDatabase().name().toUpperCase(Locale.ROOT));
        
        final String fieldsTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/fields-template.ftl", classContext);
        final String defaultConstructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/default-constructor-template.ftl", classContext);
        final String constructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/constructor-template.ftl", classContext);
        final String gettersAndSetters = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/getters-setters-template.ftl", classContext);
        final String equals = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/equals-template.ftl", classContext);
        final String hashCode = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/hashcode-template.ftl", classContext);
        final String toString = FreeMarkerTemplateProcessorUtils.processTemplate("model/component/tostring-template.ftl", classContext);
        final List<String> lazyFieldNames = FieldUtils.extractLazyFetchFieldNames(model.getFields());

        final Map<String, Object> classTemplateContext = Map.ofEntries(
                Map.entry("tableName", tableName),
                Map.entry("auditEnabled", Objects.nonNull(model.getAudit()) && model.getAudit().isEnabled()),
                Map.entry("openInView", openInViewEnabled),
                Map.entry("lazyFields",lazyFieldNames),
                Map.entry("entityGraphName", ModelNameUtils.computeEntityGraphName(model.getName(), lazyFieldNames)),
                Map.entry("fields", fieldsTemplate),
                Map.entry("defaultConstructor", defaultConstructor),
                Map.entry("constructor", constructor),
                Map.entry("gettersAndSetters", gettersAndSetters),
                Map.entry("hashCode", equals),
                Map.entry("equals", hashCode),
                Map.entry("toString", toString),
                Map.entry("className", className),
                Map.entry(TemplateContextConstants.SOFT_DELETE_ENABLED, Boolean.TRUE.equals(model.getSoftDelete())),
                Map.entry(TemplateContextConstants.ID_FIELD, ModelNameUtils.toSnakeCase(idField.getName())),
                Map.entry(TemplateContextConstants.OPTIMISTIC_LOCKING, optimisticLocking)
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
