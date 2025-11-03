package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;
import static com.markozivkovic.codegen.constants.AnnotationConstants.AUDITING_ENTITY_LISTENER_CLASS;
import static com.markozivkovic.codegen.constants.AnnotationConstants.ENTITY_ANNOTATION;
import static com.markozivkovic.codegen.constants.AnnotationConstants.ENTITY_LISTENERS_ANNOTATION;
import static com.markozivkovic.codegen.constants.AnnotationConstants.TABLE_ANNOTATION;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

/**
 * Generates a JPA entity class based on the provided model definition.
 * The generated class includes fields, getters, setters, equals, hashCode, and toString methods.
 */
public class JpaEntityGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaEntityGenerator.class);

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;

    public JpaEntityGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities) {
        this.configuration = configuration;
        this.entities = entities;
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
        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MODELS, GeneratorConstants.DefaultPackageLayout.HELPERS)));
        sb.append(ImportUtils.getBaseImport(model, true, false));

        final String enumImports = ImportUtils.computeEnumsAndHelperEntitiesImport(model, outputDir);
        
        if (StringUtils.isNotBlank(enumImports)) {
            sb.append(ImportUtils.computeEnumsAndHelperEntitiesImport(model, outputDir))
                .append("\n");
        }

        final Map<String, Object> classContext = TemplateContextUtils.computeJpaModelContext(model);
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
                outputDir, FileUtils.join(GeneratorConstants.DefaultPackageLayout.MODELS, GeneratorConstants.DefaultPackageLayout.HELPERS),
                className, sb.toString()
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

        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MODELS)));
        sb.append(ImportUtils.getBaseImport(model, true, true));
                
        sb.append(ImportUtils.computeJakartaImports(model, optimisticLocking))
                .append("\n");

        final String enumAndHelperEntitiesImports = ImportUtils.computeEnumsAndHelperEntitiesImport(model, outputDir);
        
        if (StringUtils.isNotBlank(enumAndHelperEntitiesImports)) {
            sb.append(ImportUtils.computeEnumsAndHelperEntitiesImport(model, outputDir))
                .append("\n");
        }

        sb.append(ENTITY_ANNOTATION)
                .append("\n")
                .append(String.format(TABLE_ANNOTATION, tableName))
                .append("\n");

        if (Objects.nonNull(model.getAudit()) && model.getAudit().isEnabled()) {
            sb.append(String.format(ENTITY_LISTENERS_ANNOTATION, AUDITING_ENTITY_LISTENER_CLASS))
                    .append("\n");
        }

        final Map<String, Object> classContext = TemplateContextUtils.computeJpaModelContext(model);
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

        FileWriterUtils.writeToFile(outputDir, GeneratorConstants.DefaultPackageLayout.MODELS, className, sb.toString());
    }

}
