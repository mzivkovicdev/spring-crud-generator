package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JPAConstants.ENTITY_ANNOTATION;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENTITY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATED_VALUE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATION_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ID;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_TABLE;
import static com.markozivkovic.codegen.constants.JPAConstants.TABLE_ANNOTATION;
import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE_TIME;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_OBJECTS;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_UUID;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

/**
 * Generates a JPA entity class based on the provided model definition.
 * The generated class includes fields, getters, setters, equals, hashCode, and toString methods.
 */
public class JpaEntityGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaEntityGenerator.class);
    private static final String MODELS_PACKAGE = ".models";
    
    /**
     * Generates a JPA entity class based on the provided model definition.
     * The generated class includes fields, getters, setters, equals, hashCode, and toString methods.
     * 
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     */
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        this.generateJpaEntity(modelDefinition, outputDir);
    }

    /**
     * Generates a Java entity class file for the given model definition.
     *
     * @param model The model definition containing the class name, table name, and field definitions.
     */
    private void generateJpaEntity(final ModelDefinition model, final String outputDir) {

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        
        final String className = model.getName();
        final String tableName = model.getTableName();
        final List<FieldDefinition> fields = model.getFields();

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + MODELS_PACKAGE));

        if (FieldUtils.isAnyFieldLocalDate(fields)) {
            sb.append(String.format(IMPORT, JAVA_TIME_LOCAL_DATE));
        }

        if (FieldUtils.isAnyFieldLocalDateTime(fields)) {
            sb.append(String.format(IMPORT, JAVA_TIME_LOCAL_DATE_TIME));
        }

        sb.append(String.format(IMPORT, JAVA_UTIL_OBJECTS));

        if (FieldUtils.isAnyFieldUUID(fields)) {
            sb.append(String.format(IMPORT, JAVA_UTIL_UUID));
        }
                
        sb.append("\n")
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_ENTITY))
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_GENERATED_VALUE))
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_GENERATION_TYPE))
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_ID))
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_TABLE))
                .append("\n")
                .append(ENTITY_ANNOTATION)
                .append("\n")
                .append(String.format(TABLE_ANNOTATION, tableName))
                .append("\n");

        final Map<String, Object> classContext = TemplateContextUtils.computeJpaModelContext(model);
        
        final String fieldsTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("model/fields-template.ftl", classContext);
        final String defaultConstructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/default-constructor-template.ftl", classContext);
        final String constructor = FreeMarkerTemplateProcessorUtils.processTemplate("model/constructor-template.ftl", classContext);
        final String gettersAndSetters = FreeMarkerTemplateProcessorUtils.processTemplate("model/getters-setters-template.ftl", classContext);
        final String equals = FreeMarkerTemplateProcessorUtils.processTemplate("model/equals-template.ftl", classContext);
        final String hashCode = FreeMarkerTemplateProcessorUtils.processTemplate("model/hashcode-template.ftl", classContext);
        final String toString = FreeMarkerTemplateProcessorUtils.processTemplate("model/tostring-template.ftl", classContext);

        sb.append(String.format("public class %s {\n\n", className));

        sb.append(fieldsTemplate)
            .append("\n")
            .append(defaultConstructor)
            .append("\n")
            .append(constructor)
            .append("\n")
            .append(gettersAndSetters)
            .append(equals)
            .append("\n")
            .append(hashCode)
            .append("\n")
            .append(toString);

        sb.append("}\n");

        try (final FileWriter writer = new FileWriter(outputDir + File.separator + "models" + File.separator + className + ".java")) {
            writer.write(sb.toString());
            LOGGER.info("Generated entity class: {}", className);
        } catch (IOException e) {
            LOGGER.error("Failed to write entity class file for {}: {}", className, e.getMessage());
            throw new RuntimeException("Failed to write entity class file", e);
        }
    }

}
