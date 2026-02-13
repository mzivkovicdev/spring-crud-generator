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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.imports.common.ImportCommon;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

public class RestControllerImports {
    
    private RestControllerImports() {}

    /**
     * Computes the necessary imports for the given model definition, including UUID if any model has a UUID as its ID,
     * and List and Collectors if any model has a many-to-many or one-to-many relation.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param entities        the list of all model definitions
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeControllerBaseImports(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        final Set<String> imports = new LinkedHashSet<>();

        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());

        if (FieldUtils.isIdFieldUUID(idField)) {
            imports.add(String.format(IMPORT, ImportConstants.Java.UUID));
        }

        if (!manyToManyFields.isEmpty() || !oneToManyFields.isEmpty()) {
            imports.add(String.format(IMPORT, ImportConstants.Java.LIST));
            imports.add(String.format(IMPORT, ImportConstants.Java.COLLECTORS));
        }

        relations.forEach(realtionField -> {

            final ModelDefinition relationModel = entities.stream()
                    .filter(entity -> entity.getName().equals(realtionField.getType()))
                    .findFirst()
                    .orElseThrow();

            final FieldDefinition relationIdField = FieldUtils.extractIdField(relationModel.getFields());

            if (FieldUtils.isIdFieldUUID(relationIdField)) {
                imports.add(String.format(IMPORT, ImportConstants.Java.UUID));
            }
        });

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the given model definition, including the model itself, the related service,
     * the related transfer object, the page transfer object, and the related mapper.
     *
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param outputDir            the directory where the generated code will be written
     * @param swagger              whether to include Swagger annotations
     * @param packageConfiguration the package configuration
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeControllerProjectImports(final ModelDefinition modelDefinition, final String outputDir,
                final boolean swagger, final PackageConfiguration packageConfiguration) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());

        if (swagger) {
            imports.addAll(EnumImports.computeEnumImports(modelDefinition, packagePath, packageConfiguration));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedApiPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), String.format("%ssApi", modelWithoutSuffix))
            ));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiCreateModelName(modelWithoutSuffix))
            ));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiUpdateModelName(modelWithoutSuffix))
            ));
        }

        relations.forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            if (!swagger) {
                imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), ModelNameUtils.computeInputTOModelName(relationModel))));
            } else {
                imports.add(String.format(
                    IMPORT,
                    PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), String.format("%sInput", relationModel))
                ));
            }
        });

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration), String.format("%sBusinessService", modelWithoutSuffix))));
        }

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonInnerElementType(field))
                .forEach(jsonField -> {
                    imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", jsonField))));
                });
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));
        if (!swagger) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", modelWithoutSuffix))));
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), ModelNameUtils.computeCreateTOModelName(modelWithoutSuffix))));
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), ModelNameUtils.computeUpdateTOModelName(modelWithoutSuffix))));
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeTransferObjectPackage(packagePath, packageConfiguration), GeneratorConstants.PAGE_TO)));
        } else {
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiModelName(modelWithoutSuffix))
            ));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), String.format("%ssGet200Response", modelWithoutSuffix))
            ));
        }
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", modelWithoutSuffix))));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for a controller add relation endpoint.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @return a string containing the necessary import statements for a controller add relation endpoint
     */
    public static String computeAddRelationEndpointBaseImports(final ModelDefinition modelDefinition) {
        final Set<String> imports = new LinkedHashSet<>();
        
        final List<FieldDefinition> fields = modelDefinition.getFields();
        ImportCommon.addIf(FieldUtils.isIdFieldUUID(FieldUtils.extractIdField(fields)), imports, ImportConstants.Java.UUID);

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }
    
    /**
     * Compute the imports for a controller test.
     *
     * @param isInstancioEnabled whether Instancio is enabled
     * @param springBootVersion the Spring Boot version
     * @return the imports string for a controller test
     */
    public static String computeAddRelationEndpointTestImports(final boolean isInstancioEnabled, final String springBootVersion) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        
        addOAuth2WebMvcTestImports(imports, springBootVersion);

        if (SpringBootVersionUtils.isSpringBoot3(springBootVersion)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCK_BEAN));
        } else {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        }
        imports.add(String.format(IMPORT, ImportConstants.SpringHttp.MEDIA_TYPE));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.CONTEXT_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKMVC));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.RESULT_ACTIONS));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Compute the necessary imports for a controller delete endpoint test.
     *
     * @param isInstancioEnabled whether Instancio is enabled
     * @param springBootVersion  the Spring Boot version
     * @return A string containing the necessary import statements for a controller delete endpoint test.
     */
    public static String computeDeleteEndpointTestImports(final boolean isInstancioEnabled, final String springBootVersion) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));

        addOAuth2WebMvcTestImports(imports, springBootVersion);

        if (SpringBootVersionUtils.isSpringBoot3(springBootVersion)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCK_BEAN));
        } else {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        }

        imports.add(String.format(IMPORT, ImportConstants.SpringTest.CONTEXT_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKMVC));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Compute the necessary imports for a controller update endpoint test.
     *
     * @param isInstancioEnabled whether Instancio is enabled
     * @param springBootVersion  the Spring Boot version
     * @return a string containing the necessary import statements for a controller update endpoint test
     */
    public static String computeUpdateEndpointTestImports(final boolean isInstancioEnabled, final String springBootVersion) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        
        addOAuth2WebMvcTestImports(imports, springBootVersion);

        if (SpringBootVersionUtils.isSpringBoot3(springBootVersion)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCK_BEAN));
        } else {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        }

        imports.add(String.format(IMPORT, ImportConstants.SpringHttp.MEDIA_TYPE));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.CONTEXT_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKMVC));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.RESULT_ACTIONS));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Compute the imports for a controller test.
     *
     * @param isInstancioEnabled whether instancio is enabled
     * @param springBootVersion the Spring Boot version
     * @return the imports string for a controller test
     */
    public static String computeGetEndpointTestImports(final boolean isInstancioEnabled, final String springBootVersion) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));

        addOAuth2WebMvcTestImports(imports, springBootVersion);

        if (SpringBootVersionUtils.isSpringBoot3(springBootVersion)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCK_BEAN));
        } else {
            imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        }

        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_IMPL));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.CONTEXT_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKMVC));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.RESULT_ACTIONS));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for a controller remove relation endpoint, including UUID if any model has a UUID as its ID,
     * and List and Collectors if any model has a many-to-many or one-to-many relation.
     *
     * @param modelDefinition the model definition containing field information used to determine necessary imports.
     * @param entities        the list of all model definitions.
     * @return A string containing the necessary import statements for the controller remove relation endpoint.
     */
    public static String computeRemoveRelationEndpointBaseImports(final ModelDefinition modelDefinition, final List<ModelDefinition> entities) {

        final Set<String> imports = new LinkedHashSet<>();
        
        final List<FieldDefinition> fields = modelDefinition.getFields();
        ImportCommon.addIf(FieldUtils.isIdFieldUUID(FieldUtils.extractIdField(fields)), imports, ImportConstants.Java.UUID);

        modelDefinition.getFields().stream()
            .filter(field -> Objects.nonNull(field.getRelation()))
            .forEach(field -> {

                final ModelDefinition relatedEntity = entities.stream()
                        .filter(entity -> entity.getName().equals(field.getType()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            String.format(
                                "Related entity not found: %s", field.getType()
                            )
                        ));
                final FieldDefinition idField = FieldUtils.extractIdField(relatedEntity.getFields());
                ImportCommon.addIf(FieldUtils.isIdFieldUUID(idField), imports, ImportConstants.Java.UUID);
            });

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the generated update endpoint test, including the enums if any exist, the model itself,
     * the related service, and any related models.
     *
     * @param modelDefinition                 the model definition containing the class name, table name, and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param swagger                         whether to include Swagger annotations
     * @param packageConfiguration            the package configuration
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     * @return A string containing the necessary import statements for the generated update endpoint test.
     */
    public static String computeUpdateEndpointTestProjectImports(final ModelDefinition modelDefinition, final String outputDir,
                final boolean swagger, final PackageConfiguration packageConfiguration, final Boolean isGlobalExceptionHandlerEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        if (swagger) {
            imports.addAll(EnumImports.computeEnumImports(modelDefinition, packagePath, packageConfiguration));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiUpdateModelName(modelWithoutSuffix))
            ));
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration), String.format("%sBusinessService", modelWithoutSuffix))));
        }

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonInnerElementType(field))
                .forEach(jsonField -> {
                    imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", jsonField))));
                });
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));
        if (!swagger) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", modelWithoutSuffix))));
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), ModelNameUtils.computeUpdateTOModelName(modelWithoutSuffix))));
        } else {
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiModelName(modelWithoutSuffix))
            ));
        }
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", modelWithoutSuffix))));

        if (isGlobalExceptionHandlerEnabled != null && isGlobalExceptionHandlerEnabled) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionHandlerPackage(packagePath, packageConfiguration), GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER)));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the generated update endpoint test, including the enums if any exist, the model itself,
     * the related service, and any related models.
     *
     * @param modelDefinition                 the model definition containing the class name, table name, and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param swagger                         whether to include Swagger annotations
     * @param packageConfiguration            the package configuration
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     * @return A string containing the necessary import statements for the generated update endpoint test.
     */
    public static String computeCreateEndpointTestProjectImports(final ModelDefinition modelDefinition, final String outputDir,
                final boolean swagger, final PackageConfiguration packageConfiguration, final Boolean isGlobalExceptionHandlerEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        Stream.concat(manyToManyFields.stream(), oneToManyFields.stream()).forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            if (!swagger) {
                imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), ModelNameUtils.computeInputTOModelName(relationModel))));
            } else {
                imports.add(String.format(
                    IMPORT,
                    PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiInputModelName(relationModel))
                ));
            }
        });

        if (swagger) {
            imports.addAll(EnumImports.computeEnumImports(modelDefinition, packagePath, packageConfiguration));
            imports.add(String.format(
                    IMPORT,
                    PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiCreateModelName(modelWithoutSuffix))
            ));
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration), String.format("%sBusinessService", modelWithoutSuffix))));
        }

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonInnerElementType(field))
                .forEach(jsonField -> {
                    imports.add(String.format(
                        IMPORT,
                        PackageUtils.join(PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", jsonField))
                    ));
                });
        }

        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));
        if (!swagger) {
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", modelWithoutSuffix))
            ));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), ModelNameUtils.computeCreateTOModelName(modelWithoutSuffix))
            ));
        } else {
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiModelName(modelWithoutSuffix))
            ));
        }
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", modelWithoutSuffix))));

        if (isGlobalExceptionHandlerEnabled != null && isGlobalExceptionHandlerEnabled) {
            imports.add(String.format(
                IMPORT, 
                PackageUtils.join(PackageUtils.computeExceptionHandlerPackage(packagePath, packageConfiguration), GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER)
            ));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the generated controller test, including the enums if any exist, the model itself,
     * the related service, and any related models.
     *
     * @param modelDefinition                 the model definition containing the class name, table name, and field definitions
     * @param outputDir                       the directory where the generated code will be written
     * @param swagger                         whether to include Swagger annotations
     * @param restEndpointOperation           the rest endpoint operation for which the imports are computed
     * @param packageConfiguration            the package configuration
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     * @return A string containing the necessary import statements for the generated controller test.
     */
    public static String computeControllerTestProjectImports(final ModelDefinition modelDefinition, final String outputDir,
                final boolean swagger, final RestEndpointOperation restEndpointOperation, final PackageConfiguration packageConfiguration,
                final Boolean isGlobalExceptionHandlerEnabled) {

        return computeControllerTestProjectImports(
                modelDefinition, outputDir, swagger, restEndpointOperation, null, packageConfiguration, isGlobalExceptionHandlerEnabled
        );
    }

    /**
     * Compute the imports for a controller test.
     *
     * @param modelDefinition       the model definition containing the class name, table name, and field definitions
     * @param outputDir             the directory where the generated code will be written
     * @param swagger               whether to generate swagger imports
     * @param restEndpointOperation the rest endpoint operation for which the imports are computed
     * @param fieldToBeAdded        the field to be added
     * @param packageConfiguration  the package configuration
     * @param isGlobalExceptionHandlerEnabled indicates if the global exception handler is enabled
     * @return the imports string for a controller test
     */
    public static String computeControllerTestProjectImports(final ModelDefinition modelDefinition, final String outputDir,
                final boolean swagger, final RestEndpointOperation restEndpointOperation, final FieldDefinition fieldToBeAdded,
                final PackageConfiguration packageConfiguration, final Boolean isGlobalExceptionHandlerEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        if (RestEndpointOperation.ADD_RELATION.equals(restEndpointOperation)) {
            final String relationModel = ModelNameUtils.stripSuffix(fieldToBeAdded.getType());
            if (!swagger) {
                imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), String.format("%sInputTO", relationModel))));
            } else {
                imports.add(String.format(
                        IMPORT,
                        PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), String.format("%sInput", relationModel))
                ));
            }
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeBusinessServicePackage(packagePath, packageConfiguration), String.format("%sBusinessService", modelWithoutSuffix))));
        }

        if (!RestEndpointOperation.REMOVE_RELATION.equals(restEndpointOperation) && !RestEndpointOperation.DELETE.equals(restEndpointOperation)) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        }
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));
        if (!swagger) {
            if (!RestEndpointOperation.DELETE.equals(restEndpointOperation) && !RestEndpointOperation.REMOVE_RELATION.equals(restEndpointOperation)) {
                imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", modelWithoutSuffix))));
            }
            ImportCommon.addIf(RestEndpointOperation.GET.equals(restEndpointOperation), imports, String.format(IMPORT, PackageUtils.join(PackageUtils.computeTransferObjectPackage(packagePath, packageConfiguration), GeneratorConstants.PAGE_TO)));
        } else {
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), ModelNameUtils.computeOpenApiModelName(modelWithoutSuffix))
            ));
            ImportCommon.addIf(RestEndpointOperation.GET.equals(restEndpointOperation), imports, String.format(IMPORT, PackageUtils.join(PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, unCapModelWithoutSuffix), String.format("%ssGet200Response", modelWithoutSuffix))));
        }
        if (!RestEndpointOperation.DELETE.equals(restEndpointOperation) && !RestEndpointOperation.REMOVE_RELATION.equals(restEndpointOperation)) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", modelWithoutSuffix))));
        }
        if (isGlobalExceptionHandlerEnabled != null && isGlobalExceptionHandlerEnabled) {
            imports.add(String.format(
                IMPORT, 
                PackageUtils.join(PackageUtils.computeExceptionHandlerPackage(packagePath, packageConfiguration), GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER)
            ));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Adds necessary imports for OAuth2 WebMvc tests based on the Spring Boot version.
     *
     * @param imports the list of imports to add to
     * @param springBootVersion the Spring Boot version
     */
    private static void addOAuth2WebMvcTestImports(final Set<String> imports, final String springBootVersion) {
        
        if (SpringBootVersionUtils.isSpringBoot3(springBootVersion)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_CLIENT_AUTO_CONFIGURATION));
            imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.SpringBoot3.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
            imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.SpringBoot3.AUTO_CONFIGURE_MOCK_MVC));
            imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.SpringBoot3.WEB_MVC_TEST));
            return;
        }

        if (SpringBootVersionUtils.isSpringBoot4(springBootVersion)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_CLIENT_AUTO_CONFIGURATION));
            imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.SpringBoot4.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
            imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.SpringBoot4.AUTO_CONFIGURE_MOCK_MVC));
            imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.SpringBoot4.WEB_MVC_TEST));
            return;
        }
    }

    public enum RestEndpointOperation {
        GET, CREATE, ADD_RELATION, REMOVE_RELATION, UPDATE, DELETE
    }
}
