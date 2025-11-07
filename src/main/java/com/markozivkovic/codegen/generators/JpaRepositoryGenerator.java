package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;
import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.templates.JpaRepositoryTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class JpaRepositoryGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRepositoryGenerator.class);

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
        sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.REPOSITORIES)));

        if (FieldUtils.isIdFieldUUID(idField)) {
            sb.append(String.format(IMPORT, ImportConstants.Java.UUID))
                    .append("\n");
        }

        final Map<String, Object> context = JpaRepositoryTemplateContext.computeJpaInterfaceContext(modelDefinition);
        final String jpaInterface = FreeMarkerTemplateProcessorUtils.processTemplate(
                "repository/repository-interface-template.ftl", context
        );

        sb.append(String.format(IMPORT, ImportConstants.SpringData.JPA_REPOSITORY))
                .append("\n")
                .append(String.format(IMPORT, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.MODELS, modelDefinition.getName())))
                .append("\n")
                .append(jpaInterface);

        FileWriterUtils.writeToFile(outputDir, GeneratorConstants.DefaultPackageLayout.REPOSITORIES, className, sb.toString());
        
        LOGGER.info("JPA repository generation completed for model: {}", modelDefinition.getName());
    }
    
}
