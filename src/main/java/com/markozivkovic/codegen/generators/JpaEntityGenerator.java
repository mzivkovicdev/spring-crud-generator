package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JPAConstants.ENTITY_ANNOTATION;
import static com.markozivkovic.codegen.constants.JPAConstants.TABLE_ANNOTATION;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.ModelDefinition;
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

    private static final String MODELS = "models";
    private static final String MODELS_PACKAGE = "." + MODELS;
    
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
       
        this.generateJpaEntity(modelDefinition, outputDir);
        
        LOGGER.info("Generator JPA entity finished for model: {}", modelDefinition.getName());
    }

    /**
     * Generates a Java entity class file for the given model definition.
     *
     * @param model The model definition containing the class name, table name, and field definitions.
     */
    private void generateJpaEntity(final ModelDefinition model, final String outputDir) {

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        
        final String className = model.getName();
        final String tableName = model.getStorageName();

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + MODELS_PACKAGE));
        sb.append(ImportUtils.getBaseImport(model, true));
                
        sb.append(ImportUtils.computeJakartaImports(model))
                .append("\n");

        final String enumImports = ImportUtils.computeEnumsImport(model, outputDir);
        
        if (StringUtils.isNotBlank(enumImports)) {
            sb.append(ImportUtils.computeEnumsImport(model, outputDir))
                .append("\n");
        }

        sb.append(ENTITY_ANNOTATION)
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

        FileWriterUtils.writeToFile(outputDir, MODELS, className, sb.toString());
    }

}
