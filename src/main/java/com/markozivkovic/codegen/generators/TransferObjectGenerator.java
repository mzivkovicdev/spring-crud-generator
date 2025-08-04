package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.ModelDefinition;
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
    private static boolean PAGE_TO_GENERATED = false;

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generator transfer object for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String transferObjName = String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName()));

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath + TRANSFER_OBJECTS_PACKAGE));

        final String imports = ImportUtils.getBaseImport(modelDefinition, false);
        sb.append(imports);

        final String enumImports = ImportUtils.computeEnumsImport(modelDefinition, outputDir);
        
        if (StringUtils.isNotBlank(enumImports)) {
            sb.append(ImportUtils.computeEnumsImport(modelDefinition, outputDir))
                .append("\n");
        }

        final Map<String, Object> toContext = TemplateContextUtils.computeTransferObjectContext(modelDefinition);
        final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("transferobject/transfer-object-template.ftl", toContext);
        
        sb.append(transferObjectTemplate);

        FileWriterUtils.writeToFile(outputDir, TRANSFER_OBJECTS, transferObjName, sb.toString());

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
        LOGGER.info("Generator transfer object for model: {}", modelDefinition.getName());
    }

}
