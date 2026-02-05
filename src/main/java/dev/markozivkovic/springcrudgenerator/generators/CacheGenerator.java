/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.generators;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.imports.ConfigurationImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.AdditionalPropertiesUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

public class CacheGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheGenerator.class);
    
    private final CrudConfiguration crudConfiguration;
    private final PackageConfiguration packageConfiguration;
    private final List<ModelDefinition> entities;

    public CacheGenerator(final CrudConfiguration crudConfiguration, final PackageConfiguration packageConfiguration,
            final List<ModelDefinition> entities) {
        this.crudConfiguration = crudConfiguration;
        this.packageConfiguration = packageConfiguration;
        this.entities = entities;
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
        final List<String> entityNames = entities.stream()
                .filter(e -> Objects.nonNull(e.getStorageName()))
                .map(ModelDefinition::getName)
                .collect(Collectors.toList());

        final String modelImports = ConfigurationImports.getModelImports(packagePath, packageConfiguration, entityNames);

        context.put("modelImports", modelImports);
        context.put("entities", entityNames);
        context.put(
            TemplateContextConstants.OPEN_IN_VIEW_ENABLED, AdditionalPropertiesUtils.isOpenInViewEnabled(this.crudConfiguration.getAdditionalProperties())
        );

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "configuration/cache-configuration.ftl", context
                ));

        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "CacheConfiguration.java", sb.toString()
        );

        this.generateHibernateLazyNullModule(outputDir, packagePath);

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.CACHE_CONFIGURATION);
    }

    /**
     * Generates a HibernateLazyNullModule Java class file.
     *
     * The HibernateLazyNullModule is a custom Jackson module that is used to serialize and deserialize Hibernate lazy proxies and collections.
     *
     * @param outputDir The directory where the generated file should be written.
     * @param packagePath The package path where the generated file should be written.
     */
    private void generateHibernateLazyNullModule(final String outputDir, final String packagePath) {

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration)))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                "configuration/hibernate-lazy-null-module.ftl", Map.of()
                ));
        
        FileWriterUtils.writeToFile(
            outputDir, PackageUtils.computeConfigurationSubPackage(packageConfiguration), "HibernateLazyNullModule.java", sb.toString()
        );
    }

}
