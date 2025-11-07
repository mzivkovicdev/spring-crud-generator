package com.markozivkovic.codegen.utils;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;

public class ImportUtils {

    private static final String RETRYABLE_ANNOTATION = "retryableAnnotation";

    private static final String PAGE_TO = "PageTO";
    private static final String MANY_TO_MANY = "ManyToMany";

    private static final String ANNOTATIONS_PACKAGE = ".annotations";
    private static final String ENUMS = "enums";
    private static final String ENUMS_PACKAGE = "." + ENUMS;
    private static final String REPOSITORIES_PACKAGE = ".repositories";
    private static final String EXCEPTIONS_PACKAGE = ".exceptions";
    private static final String EXCEPTIONS_RESPONSES_PACKAGE = EXCEPTIONS_PACKAGE + ".responses";
    private static final String MODELS_PACKAGE = ".models";
    private static final String MODELS_HELPERS_PACKAGE = MODELS_PACKAGE + ".helpers";
    private static final String TRANSFER_OBJECTS = "transferobjects";
    private static final String TRANSFER_OBJECTS_PACKAGE = "." + TRANSFER_OBJECTS;
    private static final String TRANSFER_OBJECTS_REST_PACKAGE = "." + TRANSFER_OBJECTS + ".rest";
    private static final String SERVICES_PACKAGE = ".services";
    private static final String BUSINESS_SERVICES_PACKAGE = ".businessservices";
    private static final String MAPPERS_PACKAGE = ".mappers";
    private static final String MAPPERS_REST_PACKAGE = MAPPERS_PACKAGE + ".rest";
    private static final String MAPPERS_REST_HELPERS_PACKAGE = MAPPERS_REST_PACKAGE + ".helpers";
    private static final String TRANSFER_OBJECTS_REST_HELPERS_PACKAGE = TRANSFER_OBJECTS_REST_PACKAGE + ".helpers";
    private static final String TRANSFER_OBJECTS_GRAPH_QL_HELPERS_PACKAGE = "." + TRANSFER_OBJECTS + ".graphql.helpers";
    private static final String GENERATED_RESOURCE_API_RESOURCE_API = ".generated.%s.api.%ssApi";
    private static final String GENERATED_RESOURCE_MODEL_RESOURCE = ".generated.%s.model.%s";

    private static final String TRANSFER_OBJECTS_GRAPHQL_PACKAGE = "." + TRANSFER_OBJECTS + ".graphql";
    private static final String MAPPERS_GRAPHQL_PACKAGE = MAPPERS_PACKAGE + ".graphql";
    private static final String MAPPERS_GRAPHQL_HELPERS_PACKAGE = MAPPERS_GRAPHQL_PACKAGE + ".helpers";

    private static final String INVALID_RESOURCE_STATE_EXCEPTION = "InvalidResourceStateException";
    private static final String RESOURCE_NOT_FOUND_EXCEPTION = "ResourceNotFoundException";

    private static final String HTTP_RESPONSE = "HttpResponse";
    
    private ImportUtils() {

    }

    /**
     * Generates a string of import statements based on the fields present in the given model definition.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @param importObjects   Whether to include the java.util.Objects import.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final boolean importObjects, final boolean importAuditing) {

        return getBaseImport(modelDefinition, List.of(), importObjects, false, false, importAuditing, false);
    }

    /**
     * Computes the necessary imports for the given model definition, including imports for the types of its fields,
     * as well as imports for the types of its relations, if any.
     *
     * @param modelDefinition the model definition containing field information used to determine necessary imports.
     * @param entities        the list of all model definitions, used to determine the necessary imports for relations.
     * @param realtionIds     whether to include the imports for the relation IDs.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final List<ModelDefinition> entities, final boolean realtionIds) {

        return getBaseImport(modelDefinition, entities, false, false, realtionIds, false, false);
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
     * Generates a string of import statements based on the fields present in the given model definition, with options to include
     * the java.util.List interface.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getTestBaseImport(final ModelDefinition modelDefinition) {
        
        return getBaseImport(modelDefinition, List.of(), false, true, false, false, true);
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
     * Generates a string of import statements for the base jakarta persistence annotations.
     * 
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param optimisticLocking whether to include the version field
     * @return A string containing the necessary import statements for the base jakarta persistence annotations.
     */
    public static String computeJakartaImports(final ModelDefinition modelDefinition, final Boolean optimisticLocking) {

        final Set<String> imports = new LinkedHashSet<>();
        final List<FieldDefinition> fields = modelDefinition.getFields();
        final List<String> relations = FieldUtils.extractRelationTypes(fields);

        imports.addAll(Set.of(
            ImportConstants.Jakarta.ENTITY, ImportConstants.Jakarta.GENERATED_VALUE, ImportConstants.Jakarta.GENERATION_TYPE,
            ImportConstants.Jakarta.ID, ImportConstants.Jakarta.TABLE
        ));
        
        if (FieldUtils.isAnyFieldEnum(fields)) {
            imports.add(ImportConstants.Jakarta.ENUM_TYPE);
            imports.add(ImportConstants.Jakarta.ENUMERATED);
        }

        final boolean hasAnyFieldColumn = FieldUtils.isAnyFieldJson(fields) || fields.stream()
                .anyMatch(field -> Objects.nonNull(field.getColumn()));
        final boolean isAuditingEnabled = Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled();
        
        addIf(!relations.isEmpty(), imports, ImportConstants.Jakarta.JOIN_COLUMN);
        addIf(relations.contains(MANY_TO_MANY), imports, ImportConstants.Jakarta.JOIN_TABLE);
        addIf(FieldUtils.isAnyRelationManyToMany(fields), imports, ImportConstants.Jakarta.MANY_TO_MANY);
        addIf(FieldUtils.isAnyRelationManyToOne(fields), imports, ImportConstants.Jakarta.MANY_TO_ONE);
        addIf(FieldUtils.isAnyRelationOneToMany(fields), imports, ImportConstants.Jakarta.ONE_TO_MANY);
        addIf(FieldUtils.isAnyRelationOneToOne(fields), imports, ImportConstants.Jakarta.ONE_TO_ONE);
        addIf(FieldUtils.isFetchTypeDefined(fields), imports, ImportConstants.Jakarta.FETCH_TYPE);
        addIf(FieldUtils.isCascadeTypeDefined(fields), imports, ImportConstants.Jakarta.CASCADE_TYPE);
        addIf(optimisticLocking, imports, ImportConstants.Jakarta.VERSION);
        addIf(hasAnyFieldColumn || isAuditingEnabled, imports, ImportConstants.Jakarta.COLUMN);
        addIf(isAuditingEnabled, imports, ImportConstants.Jakarta.ENTITY_LISTENERS);

        final String jakartaImports = imports.stream()
                  .map(imp -> String.format(IMPORT, imp))
                  .sorted()
                  .collect(Collectors.joining());

        final Set<String> orgImports = new LinkedHashSet<>();
        addIf(isAuditingEnabled, orgImports, ImportConstants.SpringData.AUDITING_ENTITY_LISTENER);
        addIf(isAuditingEnabled, orgImports, ImportConstants.SpringData.CREATED_DATE);
        addIf(isAuditingEnabled, orgImports, ImportConstants.SpringData.LAST_MODIFIED_DATE);
        
        if (!FieldUtils.isAnyFieldJson(fields)) {
            if (orgImports.isEmpty()) {
                return jakartaImports;
            }

            final String orgImportsFormatted = orgImports.stream()
                    .map(imp -> String.format(IMPORT, imp))
                    .sorted()
                    .collect(Collectors.joining());

            return String.format("%s\n%s", jakartaImports, orgImportsFormatted);
        }

        final String hibernateImports = Stream.concat(
                    Set.of(ImportConstants.HibernateAnnotation.JDBC_TYPE_CODE, ImportConstants.HibernateAnnotation.SQL_TYPES).stream(),
                    orgImports.stream()
                )
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        return String.format("%s\n%s", jakartaImports, hibernateImports);
    }

    /**
     * Generates a string of import statements for the generated enums, if any.
     * 
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return A string containing the necessary import statements for the generated enums.
     */
    public static String computeEnumsAndHelperEntitiesImport(final ModelDefinition modelDefinition, final String outputDir) {

        return computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir, true, false, false);
    }

    /**
     * Computes the necessary imports for the given model definition, including the enums if any exist.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @param packagePath   the package path where the generated code will be written
     * @return A set of strings containing the necessary import statements for the given model.
     */
    private static Set<String> computeEnumImports(final ModelDefinition modelDefinition, final String outputDir, final String packagePath) {
        
        final List<FieldDefinition> enumFields = FieldUtils.extractEnumFields(modelDefinition.getFields());
        final Set<String> imports = new LinkedHashSet<>();

        enumFields.forEach(enumField -> {
            
            final String enumName;
            if (!enumField.getName().endsWith("Enum")) {
                enumName = String.format("%sEnum", StringUtils.capitalize(enumField.getName()));
            } else {
                enumName = StringUtils.capitalize(enumField.getName());
            }

            imports.add(String.format(IMPORT, packagePath + ENUMS_PACKAGE + "." + enumName));
        });

        return imports;
    }

    /**
     * Generates a string of import statements for the generated enums, helper entities for JSON fields and transfer objects,
     * if any.
     * 
     * @param modelDefinition  the model definition containing the class name, table name, and field definitions
     * @param outputDir        the directory where the generated code will be written
     * @param importJsonFields whether to include the helper entities for JSON fields
     * @param restTOs          whether to include the REST transfer objects
     * @param graphqlTOs       whether to include the GraphQL transfer objects
     * @return A string containing the necessary import statements for the generated enums, helper entities for JSON fields and
     *         transfer objects.
     */
    public static String computeEnumsAndHelperEntitiesImport(final ModelDefinition modelDefinition, final String outputDir,
            final boolean importJsonFields, final boolean restTOs, final boolean graphqlTOs) {

        final Set<String> imports = new LinkedHashSet<>();
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        if (!FieldUtils.isAnyFieldEnum(modelDefinition.getFields()) && !FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            return "";
        }

        imports.addAll(computeEnumImports(modelDefinition, outputDir, packagePath));

        if (importJsonFields) {
            final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
            jsonFields.stream()
                    .map(FieldUtils::extractJsonFieldName)
                    .forEach(fieldName -> {
                        if (graphqlTOs) {
                            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPH_QL_HELPERS_PACKAGE + "." + fieldName + "TO"));
                        } else if (restTOs) {
                            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_REST_HELPERS_PACKAGE + "." + fieldName + "TO"));
                        } else {
                            imports.add(String.format(IMPORT, packagePath + MODELS_HELPERS_PACKAGE + "." + fieldName));
                        }
                    });
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary import statements for the generated test service.
     *
     * @param modelDefinition     the model definition containing the class name, table name, and field definitions
     * @param entities            the list of all model definitions
     * @param isInstancioEnabled  whether Instancio is enabled
     * @return A string containing the necessary import statements for the generated test service.
     */
    public static String computeTestServiceImports(final ModelDefinition modelDefinition, final List<ModelDefinition> entities, 
            final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        final boolean isAnyFieldEnum = FieldUtils.isAnyFieldEnum(modelDefinition.getFields());
        final boolean hasCollectionRelation = FieldUtils.hasCollectionRelation(modelDefinition, entities);

        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.BEFORE_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.EXTEND_WITH));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_IMPL));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_REQUEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.SPRING_EXTENSION));

        addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        addIf(isAnyFieldEnum, imports, String.format(IMPORT, ImportConstants.JUnit.Params.PARAMETERIZED_TEST));
        addIf(isAnyFieldEnum, imports, String.format(IMPORT, ImportConstants.JUnit.Params.ENUM_SOURCE));
        addIf(hasCollectionRelation, imports, String.format(IMPORT, ImportConstants.Java.COLLECTORS));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary import statements for the generated test business service.
     *
     * @param isInstancioEnabled whether Instancio is enabled
     * @return A string containing the necessary import statements for the generated test business service.
     */
    public static String computeTestBusinessServiceImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        if (isInstancioEnabled) {
            imports.add(String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        }
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.BEFORE_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.EXTEND_WITH));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.SPRING_EXTENSION));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the base import statements for a JPA service.
     *
     * @param cache Whether to include the Spring caching annotations.
     * @return A string containing the necessary import statements for the base JPA service.
     */
    public static String computeJpaServiceBaseImport(final boolean cache) {

        final Set<String> imports = new LinkedHashSet<>();

        imports.add(String.format(IMPORT, ImportConstants.Logger.LOGGER));
        imports.add(String.format(IMPORT, ImportConstants.Logger.LOGGER_FACTORY));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_REQUEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringStereotype.SERVICE));
        if (!GeneratorContext.isGenerated(RETRYABLE_ANNOTATION)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringTransaction.TRANSACTIONAL));
        }

        if (cache) {
            imports.add(String.format(IMPORT, ImportConstants.SpringCache.CACHEABLE));
            imports.add(String.format(IMPORT, ImportConstants.SpringCache.CACHE_EVICT));
            imports.add(String.format(IMPORT, ImportConstants.SpringCache.CACHE_PUT));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the given model definition, including the enums if any exist, the model itself, the repository, and any related models.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeModelsEnumsAndRepositoryImports(final ModelDefinition modelDefinition, final String outputDir) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final List<String> relationModels = modelDefinition.getFields().stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .map(field -> field.getType())
                .collect(Collectors.toList());

        final String enumsImport = computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir);

        imports.add(enumsImport);
        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + REPOSITORIES_PACKAGE + "." + modelWithoutSuffix + "Repository"));

        imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + "." + RESOURCE_NOT_FOUND_EXCEPTION));

        if (GeneratorContext.isGenerated(RETRYABLE_ANNOTATION)) {
            imports.add(String.format(IMPORT, packagePath + ANNOTATIONS_PACKAGE + "." + GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY));
        }

        if (!relationModels.isEmpty()) {
            imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + "." + INVALID_RESOURCE_STATE_EXCEPTION));
        }

        relationModels.forEach(relation -> imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + relation)));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the given model definition, including the enums if any exist, the model itself,
     * the related service, and any related models.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeModelsEnumsAndServiceImports(final ModelDefinition modelDefinition, final String outputDir) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final List<FieldDefinition> relationModels = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final String enumsImport = computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir);

        imports.add(enumsImport);
        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));

        relationModels.forEach(relation -> {
            imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + relation.getType()));
            imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + ModelNameUtils.stripSuffix(relation.getType()) + "Service"));
        });

        if (GeneratorContext.isGenerated(RETRYABLE_ANNOTATION)) {
            imports.add(String.format(IMPORT, packagePath + ANNOTATIONS_PACKAGE + "." + GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

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
            imports.addAll(computeEnumImports(modelDefinition, outputDir, packagePath));
            imports.add(String.format(
                IMPORT,
                String.format(packagePath + GENERATED_RESOURCE_API_RESOURCE_API, unCapModelWithoutSuffix, modelWithoutSuffix)
            ));
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
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + PAGE_TO));
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

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the global exception handler, given the relations configuration.
     *
     * @param hasRelations       whether the project has any relations
     * @param outputDir          the directory where the generated code will be written
     * @param importHttpResponse whether to include the HttpResponse import
     * @return A string containing the necessary import statements for the global exception handler.
     */
    private static String computeGlobalExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir, final boolean importHttpResponse) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + "." + RESOURCE_NOT_FOUND_EXCEPTION));
        if (importHttpResponse) {
            imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_RESPONSES_PACKAGE + "." + HTTP_RESPONSE));
        }

        if (hasRelations) {
            imports.add(String.format(IMPORT, packagePath + EXCEPTIONS_PACKAGE + "." + INVALID_RESOURCE_STATE_EXCEPTION));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the global rest exception handler, given the relations configuration.
     * 
     * @param hasRelations whether the project has any relations
     * @param outputDir the directory where the generated code will be written
     * @return A string containing the necessary import statements for the global rest exception handler.
     */
    public static String computeGlobalRestExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir) {

        return computeGlobalExceptionHandlerProjectImports(hasRelations, outputDir, true);
    }

    /**
     * Computes the necessary imports for the global graphql exception handler, given the relations configuration.
     * 
     * @param hasRelations whether the project has any relations
     * @param outputDir the directory where the generated code will be written
     * @return A string containing the necessary import statements for the global graphql exception handler.
     */
    public static String computeGlobalGraphQlExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir) {

        return computeGlobalExceptionHandlerProjectImports(hasRelations, outputDir, false);
    }

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
                    imports.add(String.format(IMPORT, packagePath + MAPPERS_GRAPHQL_HELPERS_PACKAGE + "." + jsonField + "GraphQLMapper"));
                });
        }

        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "TO"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "CreateTO"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "UpdateTO"));
        imports.add(String.format(IMPORT, packagePath + MAPPERS_GRAPHQL_PACKAGE + "." + modelWithoutSuffix + "GraphQLMapper"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + PAGE_TO));

        if (!FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, packagePath + BUSINESS_SERVICES_PACKAGE + "." + modelWithoutSuffix + "BusinessService"));
        }

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

        addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
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
     * Compute the imports for a controller test.
     *
     * @param isInstancioEnabled whether Instancio is enabled
     * @return the imports string for a controller test
     */
    public static String computeAddRelationEndpointTestImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
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

        addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
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
            imports.addAll(computeEnumImports(modelDefinition, outputDir, packagePath));
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
            imports.addAll(computeEnumImports(modelDefinition, outputDir, packagePath));
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
            imports.addAll(computeEnumImports(modelDefinition, outputDir, packagePath));
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
     * Compute the necessary imports for a controller update endpoint test.
     *
     * @param isInstancioEnabled whether Instancio is enabled
     * @return a string containing the necessary import statements for a controller update endpoint test
     */
    public static String computeUpdateEndpointTestImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
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
     * Computes the necessary imports for a controller add relation endpoint.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @return a string containing the necessary import statements for a controller add relation endpoint
     */
    public static String computeAddRelationEndpointBaseImports(final ModelDefinition modelDefinition) {
        final Set<String> imports = new LinkedHashSet<>();
        
        final List<FieldDefinition> fields = modelDefinition.getFields();
        addIf(FieldUtils.isIdFieldUUID(FieldUtils.extractIdField(fields)), imports, ImportConstants.Java.UUID);

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
        addIf(FieldUtils.isIdFieldUUID(FieldUtils.extractIdField(fields)), imports, ImportConstants.Java.UUID);

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

        addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
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

        addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
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
