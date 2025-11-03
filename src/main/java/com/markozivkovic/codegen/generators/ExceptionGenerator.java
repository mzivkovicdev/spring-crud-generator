package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class ExceptionGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionGenerator.class);

    private static final String EXCEPTIONS = "exceptions";

    private static final List<String> EXCEPTION_CLASS_LIST = List.of(
            "ResourceNotFoundException", "InvalidResourceStateException"
    );
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (GeneratorContext.isGenerated(EXCEPTIONS)) { return; }

        LOGGER.info("Generating exceptions");
        
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        EXCEPTION_CLASS_LIST.forEach(exceptionClassName -> {
            
            final StringBuilder sb = new StringBuilder();
            
            final String exceptionTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                "exception/exception-template.ftl", Map.of("className", exceptionClassName)
            );

            sb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.EXCEPTIONS)))
                    .append(exceptionTemplate);
    
            FileWriterUtils.writeToFile(outputDir, GeneratorConstants.DefaultPackageLayout.EXCEPTIONS, exceptionClassName, sb.toString());
        });

        GeneratorContext.markGenerated(EXCEPTIONS);
        
        LOGGER.info("Finished generating exceptions");
    }

}
