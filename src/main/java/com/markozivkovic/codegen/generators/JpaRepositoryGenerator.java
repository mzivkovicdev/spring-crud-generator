package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JPAConstants.SPRING_DATA_PACKAGE_JPA_REPOSITORY;
import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_UUID;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class JpaRepositoryGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRepositoryGenerator.class);
    
    private static final String REPOSITORIES = "repositories";
    private static final String REPOSITORIES_PACKAGE = "." + REPOSITORIES;
    private static final String MODELS_PACKAGE = ".models";

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

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + REPOSITORIES_PACKAGE));

        if (FieldUtils.isIdFieldUUID(idField)) {
            sb.append(String.format(IMPORT, JAVA_UTIL_UUID))
                    .append("\n");
        }

        final Map<String, Object> context = TemplateContextUtils.computeJpaInterfaceContext(modelDefinition);
        final String jpaInterface = FreeMarkerTemplateProcessorUtils.processTemplate(
                "repository/repository-interface-template.ftl", context
        );

        sb.append(String.format(IMPORT, SPRING_DATA_PACKAGE_JPA_REPOSITORY))
                .append("\n")
                .append(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()))
                .append("\n")
                .append(jpaInterface);

        FileWriterUtils.writeToFile(outputDir, REPOSITORIES, className, sb.toString());
        
        LOGGER.info("JPA repository generation completed for model: {}", modelDefinition.getName());
    }
    
}
