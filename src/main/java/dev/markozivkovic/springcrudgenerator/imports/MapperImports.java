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

package dev.markozivkovic.springcrudgenerator.imports;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

public class MapperImports {

    private MapperImports() {}

    /**
     * Computes the necessary imports for a mapper, including the necessary enums, models, services, and transfer objects.
     *
     * @param packagePath          the package path of the project
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param packageConfiguration the package configuration for the project
     * @param swagger              whether to include swagger annotations
     * @param isGraphQl            whether the mapper is for graphql
     * @return a string containing the necessary import statements for a mapper
     */
    public static String computeMapperImports(final String packagePath, final ModelDefinition modelDefinition,
                final PackageConfiguration packageConfiguration, final Boolean swagger, final Boolean isGraphQl) {

        final Set<String> imports = new LinkedHashSet<>();
        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String transferObjectName = String.format("%sTO", strippedModelName);
        final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());

        jsonFields.stream()
                .map(FieldUtils::extractJsonInnerElementType)
                .forEach(field -> {
                    
                    final String resolvedPackage = isGraphQl ?
                            PackageUtils.join(PackageUtils.computeHelperGraphQlMapperPackage(packagePath, packageConfiguration), String.format("%sGraphQLMapper", field)) :
                            PackageUtils.join(PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", field));
                    imports.add(String.format(IMPORT, resolvedPackage));
                });

        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        if (swagger) {
            final String resolvedPackagePath = PackageUtils.join(
                    PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(strippedModelName)), ModelNameUtils.computeOpenApiModelName(strippedModelName)
            );
            imports.add(String.format(IMPORT, resolvedPackagePath));
        }

        final String transferObjectImport;
        
        if (isGraphQl) {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        } else {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        }

        imports.add(transferObjectImport);

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for a helper mapper, including the necessary enums, models, services, and transfer objects.
     *
     * @param packagePath          the package path of the project
     * @param jsonModel            the json model definition containing the class name, table name, and field definitions
     * @param parentModel          the parent model definition containing the class name, table name, and field definitions
     * @param packageConfiguration the package configuration for the project
     * @param swagger              whether to include swagger annotations
     * @param isGraphQl            whether the mapper is for graphql
     * @return a string containing the necessary import statements for a helper mapper
     */
    public static String computeHelperMapperImports(final String packagePath, final ModelDefinition jsonModel,
                final ModelDefinition parentModel, final PackageConfiguration packageConfiguration,
                final Boolean swagger, final Boolean isGraphQl) {

        final Set<String> imports = new LinkedHashSet<>();

        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));

        final String transferObjectImport;
        if (isGraphQl) {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        } else {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        }

        imports.add(transferObjectImport);

        if (swagger) {
            final String resolvedPackagePath = PackageUtils.join(
                    PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(parentModel.getName()))),
                    ModelNameUtils.computeOpenApiModelName(jsonModel.getName())
            );
            imports.add(String.format(IMPORT, resolvedPackagePath));
        }

        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration), jsonModel.getName())));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for a test mapper
     *
     * @param packagePath          the package path of the project
     * @param modelDefinition      the json model definition containing the class name, table name, and field definitions
     * @param packageConfiguration the package configuration for the project
     * @param swagger              whether to include swagger annotations
     * @param isGraphQl            whether the mapper is for graphql
     * @return a string containing the necessary import statements for a test mapper
     */
    public static String computeTestMapperImports(final String packagePath, final ModelDefinition modelDefinition,
                final PackageConfiguration packageConfiguration, final boolean swagger, final boolean isGraphQl) {

        final Set<String> imports = new LinkedHashSet<>();

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String transferObjectName = String.format("%sTO", strippedModelName);
        final String transferObjectImport;
        
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        } else {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        }

        if (swagger) {
            imports.add(String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(strippedModelName)), ModelNameUtils.computeOpenApiModelName(strippedModelName))
            ));
        }

        imports.add(transferObjectImport);
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for a test helper mapper
     *
     * @param packagePath          the package path of the project
     * @param jsonModel            the json model definition containing the class name, table name, and field definitions
     * @param parentModel          the parent model definition
     * @param packageConfiguration the package configuration for the project
     * @param swagger              whether to include swagger annotations
     * @param isGraphQl            whether the mapper is for graphql
     * @return a string containing the necessary import statements for a test helper mapper
     */
    public static String computeTestHelperMapperImports(final String packagePath, final ModelDefinition jsonModel,
                final ModelDefinition parentModel, final PackageConfiguration packageConfiguration, final boolean swagger,
                final boolean isGraphQl) {

        final Set<String> imports = new LinkedHashSet<>();
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));

        final String transferObjectImport;
        if (isGraphQl) {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        } else {
            transferObjectImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName));
        }

        if (swagger) {
            imports.add(String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(parentModel.getName()))), ModelNameUtils.computeOpenApiModelName(jsonModel.getName()))
            ));
        }

        imports.add(transferObjectImport);
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration), jsonModel.getName())));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }
    
}
