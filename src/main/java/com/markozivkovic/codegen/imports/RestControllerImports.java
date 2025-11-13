package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants.DefaultPackageLayout;
import com.markozivkovic.codegen.imports.common.ImportCommon;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

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
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @param swagger         whether to include Swagger annotations
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeControllerProjectImports(final ModelDefinition modelDefinition, final String outputDir, final boolean swagger) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());

        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        if (swagger) {
            imports.addAll(EnumImports.computeEnumImports(modelDefinition, outputDir, packagePath));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(packagePath, DefaultPackageLayout.GENERATED, unCapModelWithoutSuffix, DefaultPackageLayout.API, String.format("%sApi", modelWithoutSuffix))
            ));
        }

        Stream.concat(manyToManyFields.stream(), oneToManyFields.stream()).forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            if (!swagger) {
                imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.REST, String.format("%sTO", relationModel))));    
            } else {
                imports.add(String.format(
                    IMPORT,
                    PackageUtils.join(packagePath, DefaultPackageLayout.GENERATED, unCapModelWithoutSuffix, DefaultPackageLayout.MODEL, relationModel)
                ));
            }
        });

        relations.forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            if (!swagger) {
                imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.REST, String.format("%sInputTO", relationModel))));
            } else {
                imports.add(String.format(
                    IMPORT,
                    PackageUtils.join(packagePath, DefaultPackageLayout.GENERATED, unCapModelWithoutSuffix, DefaultPackageLayout.MODEL, String.format("%sInput", relationModel))
                ));
            }
        });

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.BUSINESS_SERVICES, String.format("%sBusinessService", modelWithoutSuffix))));
        }

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.HELPERS, String.format("%sRestMapper", jsonField))));
                });
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.SERVICES, String.format("%sService", modelWithoutSuffix))));
        if (!swagger) {            
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.REST, String.format("%sTO", modelWithoutSuffix))));
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.PAGE_TO)));
        } else {
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(packagePath, DefaultPackageLayout.GENERATED, unCapModelWithoutSuffix, DefaultPackageLayout.MODEL, modelWithoutSuffix)
            ));
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(packagePath, DefaultPackageLayout.GENERATED, unCapModelWithoutSuffix, DefaultPackageLayout.MODEL, String.format("%ssGet200Response", modelWithoutSuffix))
            ));
        }
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.REST, String.format("%sRestMapper", modelWithoutSuffix))));

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
     * @return the imports string for a controller test
     */
    public static String computeAddRelationEndpointTestImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.AUTO_CONFIGURE_MOCK_MVC));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.WEB_MVC_TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
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
     * @return A string containing the necessary import statements for a controller delete endpoint test.
     */
    public static String computeDeleteEndpointTestImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.AUTO_CONFIGURE_MOCK_MVC));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.WEB_MVC_TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
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
     * @return a string containing the necessary import statements for a controller update endpoint test
     */
    public static String computeUpdateEndpointTestImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.AUTO_CONFIGURE_MOCK_MVC));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.WEB_MVC_TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
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
     * @return the imports string for a controller test
     */
    public static String computeGetEndpointTestImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.AUTO_CONFIGURE_MOCK_MVC));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.WEB_MVC_TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
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
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @param swagger         whether to include Swagger annotations
     * @return A string containing the necessary import statements for the generated update endpoint test.
     */
    public static String computeUpdateEndpointTestProjectImports(final ModelDefinition modelDefinition, final String outputDir, final boolean swagger) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        if (swagger) {
            imports.addAll(EnumImports.computeEnumImports(modelDefinition, outputDir, packagePath));
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.BUSINESS_SERVICES, String.format("%sBusinessService", modelWithoutSuffix))));
        }

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.REST, DefaultPackageLayout.HELPERS, String.format("%sRestMapper", jsonField))));
                });
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.SERVICES, String.format("%sService", modelWithoutSuffix))));
        if (!swagger) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.REST, String.format("%sTO", modelWithoutSuffix))));
        } else {
            imports.add(String.format(
                IMPORT,
                PackageUtils.join(packagePath, DefaultPackageLayout.GENERATED, unCapModelWithoutSuffix, DefaultPackageLayout.MODEL, modelWithoutSuffix)
            ));
        }
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.REST, String.format("%sRestMapper", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.OBJECT_MAPPER));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, DefaultPackageLayout.HANDLERS, GeneratorConstants.GLOBAL_REST_EXCEPTION_HANDLER)));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
