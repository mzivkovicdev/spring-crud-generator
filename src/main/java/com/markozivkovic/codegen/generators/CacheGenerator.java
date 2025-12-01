package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class CacheGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheGenerator.class);
    
    private final CrudConfiguration crudConfiguration;
    private final PackageConfiguration packageConfiguration;

    public CacheGenerator(final CrudConfiguration crudConfiguration, final PackageConfiguration packageConfiguration) {
        this.crudConfiguration = crudConfiguration;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final String outputDir) {
     
        if (Objects.isNull(crudConfiguration.getCache()) || !Boolean.TRUE.equals(this.crudConfiguration.getCache().getEnabled())) {
            LOGGER.info("Skipping CacheGenerator, as cache is not enabled.");
            return;
        }

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION)) {
            return;
        }

        final Map<String, Object> context = new HashMap<>();
        
        if (Objects.nonNull(this.crudConfiguration.getCache().getType())) {
            context.put("type", this.crudConfiguration.getCache().getType());
        } else {
            context.put("type", CacheTypeEnum.SIMPLE);
        }

        if (Objects.nonNull(this.crudConfiguration.getCache().getMaxSize())) {
            context.put("maxSize", this.crudConfiguration.getCache().getMaxSize());
        }

        if (Objects.nonNull(this.crudConfiguration.getCache().getExpiration())) {
            context.put("expiration", this.crudConfiguration.getCache().getExpiration());
        }
        
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "configuration/cache-configuration.ftl", context
                ));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "CacheConfiguration.java", sb.toString()
        );

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION);
    }

}
