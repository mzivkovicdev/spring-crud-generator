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

package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.templates.EnumTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class EnumGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EnumGenerator.class);

    private final PackageConfiguration packageConfiguration;

    public EnumGenerator(final PackageConfiguration packageConfiguration) {
        this.packageConfiguration = packageConfiguration;
    }

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
            sb.append(String.format(PACKAGE, PackageUtils.computeEnumPackage(packagePath, packageConfiguration)));

            final Map<String, Object> context = EnumTemplateContext.createEnumContext(enumName, enumField.getValues());
            final String enumTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("enum/enum-template.ftl", context);

            sb.append(enumTemplate);

            FileWriterUtils.writeToFile(outputDir, PackageUtils.computeEnumSubPackage(packageConfiguration), enumName, sb.toString());
        });

        LOGGER.info("Finished generating enums for model: {}", modelDefinition.getName());
    }

}
