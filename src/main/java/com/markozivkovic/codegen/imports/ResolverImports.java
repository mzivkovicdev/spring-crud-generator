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

package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.imports.common.ImportCommon;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class ResolverImports {
    
    private ResolverImports() {}

    /**
     * Computes the necessary imports for the given model definition, including UUID if any model has a UUID as its ID.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeResolverBaseImports(final ModelDefinition modelDefinition) {

        final Set<String> imports = new LinkedHashSet<>();

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        if (FieldUtils.isIdFieldUUID(idField)) {
            imports.add(String.format(IMPORT, ImportConstants.Java.UUID));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the given model definition, including the graphql mappers, graphql mappers helpers, and the page transfer object.
     *
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param outputDir            the directory where the generated code will be written
     * @param packageConfiguration the package configuration for the project
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeGraphQlResolverImports(final ModelDefinition modelDefinition, final String outputDir,
                final PackageConfiguration packageConfiguration) {
        
        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(
                        IMPORT,
                        PackageUtils.join(PackageUtils.computeHelperGraphQlMapperPackage(packagePath, packageConfiguration), String.format("%sGraphQLMapper", jsonField))
                    ));
                });
        }

        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sCreateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sUpdateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphQlMapperPackage(packagePath, packageConfiguration), String.format("%sGraphQLMapper", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeTransferObjectPackage(packagePath, packageConfiguration), "PageTO")));

        if (!FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration), String.format("%sBusinessService", modelWithoutSuffix))));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * computes the necessary imports for a query resolver test.
     * 
     * @param isInstancioEnabled whether Instancio is enabled
     * @return a string containing the necessary import statements for a query resolver test
     */
    public static String computeQueryResolverTestImports(final boolean isInstancioEnabled) {
        
        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        imports.add(String.format(IMPORT, ImportConstants.SpringContext.IMPORT));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_IMPL));
        imports.add(String.format(IMPORT, ImportConstants.SpringCore.PARAMETERIZED_TYPE_REFERENCE));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.TEST_PROPERTY_SORUCE));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.AUTO_CONFIGURE_GRAPH_QL_TESTER));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.GRAPH_QL_TEST));
        imports.add(String.format(IMPORT, ImportConstants.GraphQLTest.GRAPH_QL_TESTER));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
        
        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * computes the necessary imports for a mutation resolver test.
     * 
     * @param isInstancioEnabled whether Instancio is enabled
     * @param hasJsonFields whether the model has json fields
     * @return a string containing the necessary import statements for a mutation resolver test
     */
    public static String computeMutationResolverTestImports(final boolean isInstancioEnabled, final boolean hasJsonFields) {
        
        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        ImportCommon.addIf(hasJsonFields, imports, String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        imports.add(String.format(IMPORT, ImportConstants.SpringContext.IMPORT));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.TEST_PROPERTY_SORUCE));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.AUTO_CONFIGURE_GRAPH_QL_TESTER));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.GRAPH_QL_TEST));
        imports.add(String.format(IMPORT, ImportConstants.GraphQLTest.GRAPH_QL_TESTER));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
        
        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for a query unit test, including the necessary enums, models, services, and transfer objects.
     *
     * @param outputDir            the directory where the generated code will be written
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param packageConfiguration the package configuration for the project
     * @return a string containing the necessary import statements for a query unit test
     */
    public static String computeProjectImportsForQueryUnitTests(final String outputDir, final ModelDefinition modelDefinition,
                final PackageConfiguration packageConfiguration) {
     
        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration), String.format("%sBusinessService", modelWithoutSuffix))));
        }

        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeTransferObjectPackage(packagePath, packageConfiguration), GeneratorConstants.PAGE_TO)));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionHandlerPackage(packagePath, packageConfiguration), GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER)));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * computes the necessary imports for a mutation unit test, including the necessary imports for the fields, relations, and service.
     *
     * @param outputDir            the directory where the generated code will be written
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param packageConfiguration the package configuration for the project
     * @return a string containing the necessary import statements for a mutation unit test
     */
    public static String computeProjectImportsForMutationUnitTests(final String outputDir, final ModelDefinition modelDefinition,
                final PackageConfiguration packageConfiguration) {
     
        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(
                        IMPORT,
                        PackageUtils.join(PackageUtils.computeHelperGraphQlMapperPackage(packagePath, packageConfiguration), String.format("%sGraphQLMapper", jsonField))
                    ));
                });
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration), String.format("%sBusinessService", modelWithoutSuffix))));
        }

        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sCreateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sUpdateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionHandlerPackage(packagePath, packageConfiguration), GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER)));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.OBJECT_MAPPER));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.TYPE_REFERENCE));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
