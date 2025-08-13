package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_UUID;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class TransferObjectGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferObjectGenerator.class);

    private static final String TRANSFER_OBJECTS = "transferobjects";
    private static final String TRANSFER_OBJECTS_PACKAGE = "." + TRANSFER_OBJECTS;
    private static final String TRANSFER_OBJECTS_HELPERS = "transferobjects/helpers";
    private static final String TRANSFER_OBJECTS_HELPERS_PACKAGE =  TRANSFER_OBJECTS_PACKAGE + ".helpers";

    private static boolean PAGE_TO_GENERATED = false;

    private final List<ModelDefinition> entities;

    public TransferObjectGenerator(final List<ModelDefinition> entities) {
        this.entities = entities;
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }
        
        LOGGER.info("Generator transfer object for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

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
                    
                    this.generateHelperTO(jsonModel, outputDir, packagePath);
                });

        final String transferObjName = String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName()));

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath + TRANSFER_OBJECTS_PACKAGE));

        final String imports = ImportUtils.getBaseImport(modelDefinition, false);
        sb.append(imports);

        final String enumAndHelperEntityImports = ImportUtils.computeEnumsAndHelperEntitiesImport(
                modelDefinition, outputDir, true, true
        );
        
        if (StringUtils.isNotBlank(enumAndHelperEntityImports)) {
            sb.append(ImportUtils.computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir))
                .append("\n");
        }

        final Map<String, Object> toContext = TemplateContextUtils.computeTransferObjectContext(modelDefinition);
        final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("transferobject/transfer-object-template.ftl", toContext);
        
        sb.append(transferObjectTemplate);

        FileWriterUtils.writeToFile(outputDir, TRANSFER_OBJECTS, transferObjName, sb.toString());

        generatePageTO(packagePath, outputDir);
        generateInputTO(modelDefinition, outputDir, packagePath);
        
        LOGGER.info("Generator transfer object for model: {}", modelDefinition.getName());
    }

    /**
     * Generates a helper transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path of the directory where the generated class will be written
     */
    public void generateHelperTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath) {
        
        final String transferObjName = String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName()));

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath + TRANSFER_OBJECTS_HELPERS_PACKAGE));

        final String imports = ImportUtils.getBaseImport(modelDefinition, false);
        sb.append(imports);

        final String enumAndHelperEntityImports = ImportUtils.computeEnumsAndHelperEntitiesImport(
                modelDefinition, outputDir, false, false
        );
        
        if (StringUtils.isNotBlank(enumAndHelperEntityImports)) {
            sb.append(ImportUtils.computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir))
                .append("\n");
        }

        final Map<String, Object> toContext = TemplateContextUtils.computeTransferObjectContext(modelDefinition);
        final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("transferobject/transfer-object-template.ftl", toContext);
        
        sb.append(transferObjectTemplate);

        FileWriterUtils.writeToFile(outputDir, TRANSFER_OBJECTS_HELPERS, transferObjName, sb.toString());
    }

    /**
     * Generates input transfer objects for each relation of the given model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path to use as the prefix for the generated class
     */
    public void generateInputTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath) {
        
        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return;
        }

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());

        relations.forEach(relation -> {

            final ModelDefinition relationModelDefinition = entities.stream()
                    .filter(model -> model.getName().equals(relation.getType()))
                    .findFirst()
                    .orElseThrow();

            final StringBuilder sb = new StringBuilder();
            sb.append(String.format(PACKAGE, packagePath + TRANSFER_OBJECTS_PACKAGE));
    
            final String transferObjName = String.format("%sInputTO", ModelNameUtils.stripSuffix(relationModelDefinition.getName()));
            final FieldDefinition idField = FieldUtils.extractIdField(relationModelDefinition.getFields());
    
            if (FieldUtils.isIdFieldUUID(idField)) {
                sb.append(String.format(IMPORT, JAVA_UTIL_UUID))
                        .append("\n");
            }
    
            final Map<String, Object> toContext = TemplateContextUtils.computeInputTransferObjectContext(relationModelDefinition);
            final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "transferobject/transfer-object-input-template.ftl", toContext
            );
    
            sb.append(transferObjectTemplate);
    
            FileWriterUtils.writeToFile(outputDir, TRANSFER_OBJECTS, transferObjName, sb.toString());
        });
    }

    /**
     * Generates the PageTO record. This record is used as a transfer object for paged resources.
     * It contains the total number of pages, the total number of elements, the page size, the page number,
     * and the content of the page.
     * 
     * @param packagePath the package path to use as the prefix for the generated class
     * @param outputDir the directory where the generated class will be written
     */
    public static void generatePageTO(final String packagePath, final String outputDir) {

        if (!PAGE_TO_GENERATED) {

            final StringBuilder pageSb = new StringBuilder();
            pageSb.append(String.format(PACKAGE, packagePath + TRANSFER_OBJECTS_PACKAGE));

            final String pageTOObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "transferobject/page-transfer-object-template.ftl",
                    Map.of()
            );

            pageSb.append(pageTOObjectTemplate);

            FileWriterUtils.writeToFile(outputDir, TRANSFER_OBJECTS, "PageTO", pageSb.toString());
            PAGE_TO_GENERATED = true;
        }
    }

}
