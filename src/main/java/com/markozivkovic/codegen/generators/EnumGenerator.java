package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class EnumGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EnumGenerator.class);

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        final boolean isAnyFieldEnum = FieldUtils.isAnyFieldEnum(modelDefinition.getFields());

        if (!isAnyFieldEnum) {
            return;
        }

        LOGGER.info("Generating enum for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final List<FieldDefinition> enumFields = FieldUtils.extractEnumFields(modelDefinition.getFields());

        enumFields.forEach(enumField -> {

            LOGGER.info("Generating enum for field: {}", enumField.getName());

            final String enumName;
            if (!enumField.getName().endsWith("Enum")) {
                enumName = String.format("%sEnum", StringUtils.capitalize(enumField.getName()));
            } else {
                enumName = StringUtils.capitalize(enumField.getName());
            }
            
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.ENUMS)));

            final Map<String, Object> context = TemplateContextUtils.createEnumContext(enumName, enumField.getValues());
            final String enumTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("enum/enum-template.ftl", context);

            sb.append(enumTemplate);

            FileWriterUtils.writeToFile(outputDir, GeneratorConstants.DefaultPackageLayout.ENUMS, enumName, sb.toString());
        });

        LOGGER.info("Finished generating enums for model: {}", modelDefinition.getName());
    }

}
