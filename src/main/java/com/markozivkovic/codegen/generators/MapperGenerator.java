package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class MapperGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MapperGenerator.class);

    private static final String MAPPERS = "mappers";
    private static final String MAPPER_PACKAGE = "." + MAPPERS;

    private static final String MODELS_PACKAGE = ".models";
    private static final String TRANSFER_OBJECTS_PACKAGE = ".transferobjects";

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generating mapper for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String mapperName = String.format("%sMapper", ModelNameUtils.stripSuffix(modelDefinition.getName()));
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName()));

        final String modelImport = String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName());
        final String transferObjectImport = String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + transferObjectName);

        final Map<String, Object> context = new HashMap<>();
        context.put("modelImport", modelImport);
        context.put("transferObjectImport", transferObjectImport);
        context.put("modelName", modelDefinition.getName());
        context.put("mapperName", mapperName);
        context.put("transferObjectName", transferObjectName);

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("mapper/mapper-template.ftl", context);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + MAPPER_PACKAGE))
                .append(mapperTemplate);

        FileWriterUtils.writeToFile(outputDir, MAPPERS, mapperName, sb.toString());
    }

}
