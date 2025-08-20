package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class MapperGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MapperGenerator.class);

    private static final String MAPPERS = "mappers";
    private static final String MAPPERS_PACKAGE = "." + MAPPERS;
    private static final String MAPPERS_HELPERS = "mappers/helpers";
    private static final String MAPPERS_HELPERS_PACKAGE = MAPPERS_PACKAGE + ".helpers";
    private static final String MODELS_PACKAGE = ".models";
    private static final String MODELS_HELPERS_PACKAGE = MODELS_PACKAGE + ".helpers";
    private static final String TRANSFER_OBJECTS_PACKAGE = ".transferobjects";
    private static final String TRANSFER_OBJECTS_HELPERS_PACKAGE = TRANSFER_OBJECTS_PACKAGE + ".helpers";

    private final List<ModelDefinition> entities;

    public MapperGenerator(final List<ModelDefinition> entities) {
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }
        
        LOGGER.info("Generating mapper for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String mapperName = String.format("%sMapper", ModelNameUtils.stripSuffix(modelDefinition.getName()));
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName()));

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
                    
                    this.generateHelperMapper(jsonModel, outputDir, packagePath);
                });

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

        sb.append(String.format(PACKAGE, packagePath + MAPPERS_PACKAGE))
                .append(mapperTemplate);

        FileWriterUtils.writeToFile(outputDir, MAPPERS, mapperName, sb.toString());
    }

    /**
     * Generates a helper mapper for the given json model.
     *
     * @param jsonModel the json model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path of the directory where the generated class will be written
     */
    private void generateHelperMapper(ModelDefinition jsonModel, String outputDir, String packagePath) {
        
        final String mapperName = String.format("%sMapper", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));

        final String modelImport = String.format(IMPORT, packagePath + MODELS_HELPERS_PACKAGE + "." + jsonModel.getName());
        final String transferObjectImport = String.format(IMPORT, packagePath + TRANSFER_OBJECTS_HELPERS_PACKAGE + "." + transferObjectName);

        final Map<String, Object> context = new HashMap<>();
        context.put("modelImport", modelImport);
        context.put("transferObjectImport", transferObjectImport);
        context.put("modelName", jsonModel.getName());
        context.put("mapperName", mapperName);
        context.put("transferObjectName", transferObjectName);

        final String mapperTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("mapper/mapper-template.ftl", context);

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + MAPPERS_HELPERS_PACKAGE))
                .append(mapperTemplate);

        FileWriterUtils.writeToFile(outputDir, MAPPERS_HELPERS, mapperName, sb.toString());
    }

}
