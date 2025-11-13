package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants.DefaultPackageLayout;
import com.markozivkovic.codegen.imports.common.ImportCommon;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
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
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeGraphQlResolverImports(final ModelDefinition modelDefinition, final String outputDir) {
        
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
                        PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.GRAPHQL, DefaultPackageLayout.HELPERS, String.format("%sGraphQLMapper", jsonField))
                    ));
                });
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.SERVICES, String.format("%sService", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sCreateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sUpdateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.GRAPHQL, String.format("%sGraphQLMapper", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, "PageTO")));

        if (!FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.BUSINESS_SERVICES, String.format("%sBusinessService", modelWithoutSuffix))));
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
        imports.add(String.format(IMPORT, ImportConstants.SpringContext.BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.TEST_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringFrameworkGraphQL.RUNTIME_WIRING_CONFIGURER));
        
        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * computes the necessary imports for a mutation resolver test.
     * 
     * @param isInstancioEnabled whether Instancio is enabled
     * @return a string containing the necessary import statements for a mutation resolver test
     */
    public static String computeMutationResolverTestImports(final boolean isInstancioEnabled) {
        
        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringBean.AUTOWIRED));
        imports.add(String.format(IMPORT, ImportConstants.SpringContext.IMPORT));
        imports.add(String.format(IMPORT, ImportConstants.MapStruct.FACTORY_MAPPERS));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.TEST_PROPERTY_SORUCE));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.AUTO_CONFIGURE_GRAPH_QL_TESTER));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.GRAPH_QL_TEST));
        imports.add(String.format(IMPORT, ImportConstants.GraphQLTest.GRAPH_QL_TESTER));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_CLIENT_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootAutoConfigure.OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringContext.BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringBootTest.TEST_CONFIGURATION));
        imports.add(String.format(IMPORT, ImportConstants.SpringFrameworkGraphQL.RUNTIME_WIRING_CONFIGURER));
        
        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for a query unit test, including the necessary enums, models, services, and transfer objects.
     *
     * @param outputDir the directory where the generated code will be written
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @return a string containing the necessary import statements for a query unit test
     */
    public static String computeProjectImportsForQueryUnitTests(final String outputDir, final ModelDefinition modelDefinition) {
     
        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.BUSINESS_SERVICES, String.format("%sBusinessService", modelWithoutSuffix))));
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.SERVICES, String.format("%sService", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.PAGE_TO)));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, DefaultPackageLayout.HANDLERS, GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER)));        

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * computes the necessary imports for a mutation unit test, including the necessary imports for the fields, relations, and service.
     *
     * @param outputDir the directory where the generated code will be written
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @return a string containing the necessary import statements for a mutation unit test
     */
    public static String computeProjectImportsForMutationUnitTests(final String outputDir, final ModelDefinition modelDefinition) {
     
        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        Stream.concat(manyToManyFields.stream(), oneToManyFields.stream()).forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.REST, String.format("%sTO", relationModel))));
        });

        relations.forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.REST, String.format("%sInputTO", relationModel))));
        });

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(
                        IMPORT,
                        PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.GRAPHQL, DefaultPackageLayout.HELPERS, String.format("%sGraphQLMapper", jsonField))
                    ));
                });
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.BUSINESS_SERVICES, String.format("%sBusinessService", modelWithoutSuffix))));
        }

        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.SERVICES, String.format("%sService", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sCreateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sUpdateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, DefaultPackageLayout.HANDLERS, GeneratorConstants.GLOBAL_GRAPHQL_EXCEPTION_HANDLER)));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.OBJECT_MAPPER));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.TYPE_REFERENCE));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
