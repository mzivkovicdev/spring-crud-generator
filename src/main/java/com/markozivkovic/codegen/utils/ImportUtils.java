package com.markozivkovic.codegen.utils;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.imports.EnumImports;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;

public class ImportUtils {

    private static final String PAGE_TO = "PageTO";
    private static final String EXCEPTIONS_PACKAGE = ".exceptions";
    private static final String MODELS_PACKAGE = ".models";
    private static final String TRANSFER_OBJECTS = "transferobjects";
    private static final String TRANSFER_OBJECTS_PACKAGE = "." + TRANSFER_OBJECTS;
    private static final String TRANSFER_OBJECTS_REST_PACKAGE = "." + TRANSFER_OBJECTS + ".rest";
    private static final String SERVICES_PACKAGE = ".services";
    private static final String BUSINESS_SERVICES_PACKAGE = ".businessservices";
    private static final String MAPPERS_PACKAGE = ".mappers";
    private static final String MAPPERS_REST_PACKAGE = MAPPERS_PACKAGE + ".rest";
    private static final String MAPPERS_REST_HELPERS_PACKAGE = MAPPERS_REST_PACKAGE + ".helpers";
    private static final String GENERATED_RESOURCE_MODEL_RESOURCE = ".generated.%s.model.%s";

    private static final String TRANSFER_OBJECTS_GRAPHQL_PACKAGE = "." + TRANSFER_OBJECTS + ".graphql";
    private static final String MAPPERS_GRAPHQL_PACKAGE = MAPPERS_PACKAGE + ".graphql";
    private static final String MAPPERS_GRAPHQL_HELPERS_PACKAGE = MAPPERS_GRAPHQL_PACKAGE + ".helpers";
    
    private ImportUtils() {

    }

    /**
     * Computes the necessary imports for the given model definition, including imports for the types of its fields,
     * as well as imports for the types of its relations, if any.
     *
     * @param modelDefinition the model definition containing field information used to determine necessary imports.
     * @param entities        the list of all model definitions.
     * @param importObjects   whether to include the java.util.Objects import.
     * @param importList      whether to include the java.util.List import.
     * @param relationIds     whether to include the UUID import if any of the relations have a UUID ID.
     * @param importAuditing  whether to include the auditing imports
     * @param importOptional  whether to include the java.util.Optional import
     * @return A string containing the necessary import statements for the model.
     */
    private static String getBaseImport(final ModelDefinition modelDefinition, final List<ModelDefinition> entities, final boolean importObjects,
            final boolean importList, final boolean relationIds, final boolean importAuditing, final boolean importOptional) {
        
        final StringBuilder sb = new StringBuilder();

        final List<FieldDefinition> fields = modelDefinition.getFields();
        final Set<String> imports = new LinkedHashSet<>();

        addIf(FieldUtils.isAnyFieldBigDecimal(fields), imports, ImportConstants.Java.BIG_DECIMAL);
        addIf(FieldUtils.isAnyFieldBigInteger(fields), imports, ImportConstants.Java.BIG_INTEGER);
        addIf(FieldUtils.isAnyFieldLocalDate(fields), imports, ImportConstants.Java.LOCAL_DATE);
        addIf(FieldUtils.isAnyFieldLocalDateTime(fields), imports, ImportConstants.Java.LOCAL_DATE_TIME);
        addIf(importOptional, imports, ImportConstants.Java.OPTIONAL);
        addIf(importObjects, imports, ImportConstants.Java.OBJECTS);
        
        if (modelDefinition.getAudit() != null) {
            addIf(
                importAuditing && Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled(),
                imports,
                AuditUtils.resolveAuditingImport(modelDefinition.getAudit().getType())
            );
        }
        addIf(FieldUtils.isAnyFieldUUID(fields), imports, ImportConstants.Java.UUID);

        if (relationIds) {
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
                    addIf(FieldUtils.isIdFieldUUID(idField), imports, ImportConstants.Java.UUID);
                });
        }
        
        final boolean hasLists = FieldUtils.isAnyRelationOneToMany(fields) ||
                FieldUtils.isAnyRelationManyToMany(fields);

        addIf(hasLists || importList, imports, ImportConstants.Java.LIST);

        final String sortedImports = imports.stream()
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        sb.append(sortedImports);

        if (StringUtils.isNotBlank(sb.toString())) {
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Generates a string of import statements based on the fields present in the given model definition, with options to include
     * the java.util.Objects class and the java.util.List interface.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @param importObjects   Whether to include the java.util.Objects import.
     * @param importList      Whether to include the java.util.List import.
     * @param importAuditing  Whether to include the auditing imports
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final boolean importObjects, final boolean importList, final boolean importAuditing) {

        return getBaseImport(modelDefinition, List.of(), importObjects, importList, false, importAuditing, false);
    }

    /**
     * Adds the given value to the given set if the condition is true.
     *
     * @param condition The condition to check.
     * @param set       The set to add to.
     * @param value     The value to add.
     */
    private static void addIf(final boolean condition, final Set<String> set, final String value) {
        if (condition) {
            set.add(value);
        }
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
            imports.add(String.format(IMPORT, packagePath + BUSINESS_SERVICES_PACKAGE + "." + modelWithoutSuffix + "BusinessService"));
        }

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(IMPORT, packagePath + MAPPERS_REST_HELPERS_PACKAGE + "." + jsonField + "RestMapper"));
                });
        }

        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));
        if (!swagger) {
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + modelWithoutSuffix + "TO"));
        } else {
            imports.add(String.format(
                IMPORT,
                String.format(packagePath + GENERATED_RESOURCE_MODEL_RESOURCE, unCapModelWithoutSuffix, modelWithoutSuffix)
            ));
        }
        imports.add(String.format(IMPORT, packagePath + MAPPERS_REST_PACKAGE + "." + modelWithoutSuffix + "RestMapper"));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.OBJECT_MAPPER));
        imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + ".handlers.GlobalRestExceptionHandler"));

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
    public static String computeCreateEndpointTestProjectImports(final ModelDefinition modelDefinition, final String outputDir, final boolean swagger) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        Stream.concat(manyToManyFields.stream(), oneToManyFields.stream()).forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            if (!swagger) {
                imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + relationModel + "TO"));    
            } else {
                imports.add(String.format(
                    IMPORT,
                    String.format(packagePath + GENERATED_RESOURCE_MODEL_RESOURCE, unCapModelWithoutSuffix, relationModel)
                ));
            }
        });

        if (swagger) {
            imports.addAll(EnumImports.computeEnumImports(modelDefinition, outputDir, packagePath));
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, packagePath + BUSINESS_SERVICES_PACKAGE + "." + modelWithoutSuffix + "BusinessService"));
        }

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(IMPORT, packagePath + MAPPERS_REST_HELPERS_PACKAGE + "." + jsonField + "RestMapper"));
                });
        }

        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));
        if (!swagger) {
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + modelWithoutSuffix + "TO"));
        } else {
            imports.add(String.format(
                IMPORT,
                String.format(packagePath + GENERATED_RESOURCE_MODEL_RESOURCE, unCapModelWithoutSuffix, modelWithoutSuffix)
            ));
        }
        imports.add(String.format(IMPORT, packagePath + MAPPERS_REST_PACKAGE + "." + modelWithoutSuffix + "RestMapper"));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.OBJECT_MAPPER));
        imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + ".handlers.GlobalRestExceptionHandler"));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Compute the imports for a controller test.
     *
     * @param modelDefinition    the model definition containing the class name, table name, and field definitions
     * @param outputDir          the directory where the generated code will be written
     * @param swagger            whether to generate swagger imports
     * @param importInputObjects whether to import the input objects of the relations
     * @param importObjectMapper whether to import the object mapper
     * @return the imports string for a controller test
     */
    public static String computeControllerTestProjectImports(final ModelDefinition modelDefinition, final String outputDir,
                final boolean swagger, final boolean importInputObjects, final boolean importObjectMapper) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String unCapModelWithoutSuffix = StringUtils.uncapitalize(modelWithoutSuffix);

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());

        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        if (swagger) {
            imports.addAll(EnumImports.computeEnumImports(modelDefinition, outputDir, packagePath));
        }

        Stream.concat(manyToManyFields.stream(), oneToManyFields.stream()).forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            if (!swagger) {
                imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + relationModel + "TO"));    
            } else {
                imports.add(String.format(
                    IMPORT,
                    String.format(packagePath + GENERATED_RESOURCE_MODEL_RESOURCE, unCapModelWithoutSuffix, relationModel)
                ));
            }
        });

        if (importInputObjects) {
            relations.forEach(field -> {
                final String relationModel = ModelNameUtils.stripSuffix(field.getType());
                if (!swagger) {
                    imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + relationModel + "InputTO"));
                } else {
                    imports.add(String.format(
                        IMPORT,
                        String.format(packagePath + GENERATED_RESOURCE_MODEL_RESOURCE, unCapModelWithoutSuffix, relationModel + "Input")
                    ));
                }
            });
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, packagePath + BUSINESS_SERVICES_PACKAGE + "." + modelWithoutSuffix + "BusinessService"));
        }

        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));
        if (!swagger) {
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + modelWithoutSuffix + "TO"));
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + PAGE_TO));
            imports.add(String.format(IMPORT, ImportConstants.Jackson.TYPE_REFERENCE));
        } else {
            imports.add(String.format(
                IMPORT,
                String.format(packagePath + GENERATED_RESOURCE_MODEL_RESOURCE, unCapModelWithoutSuffix, modelWithoutSuffix)
            ));
            imports.add(String.format(
                IMPORT,
                String.format(packagePath + GENERATED_RESOURCE_MODEL_RESOURCE, unCapModelWithoutSuffix, String.format("%ssGet200Response", modelWithoutSuffix))
            ));
        }
        imports.add(String.format(IMPORT, packagePath + MAPPERS_REST_PACKAGE + "." + modelWithoutSuffix + "RestMapper"));
        if (importObjectMapper) {
            imports.add(String.format(IMPORT, ImportConstants.Jackson.OBJECT_MAPPER));
        }
        imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + ".handlers.GlobalRestExceptionHandler"));

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
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + relationModel + "TO"));
        });

        relations.forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_PACKAGE + "." + relationModel + "InputTO"));
        });

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(IMPORT, packagePath + MAPPERS_GRAPHQL_HELPERS_PACKAGE + "." + jsonField + "GraphQLMapper"));
                });
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, packagePath + BUSINESS_SERVICES_PACKAGE + "." + modelWithoutSuffix + "BusinessService"));
        }
        
        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "TO"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "CreateTO"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "UpdateTO"));
        imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + ".handlers.GlobalGraphQlExceptionHandler"));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.OBJECT_MAPPER));
        imports.add(String.format(IMPORT, ImportConstants.Jackson.TYPE_REFERENCE));

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
            imports.add(String.format(IMPORT, packagePath + BUSINESS_SERVICES_PACKAGE + "." + modelWithoutSuffix + "BusinessService"));
        }

        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "TO"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + PAGE_TO));
        imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + ".handlers.GlobalGraphQlExceptionHandler"));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
