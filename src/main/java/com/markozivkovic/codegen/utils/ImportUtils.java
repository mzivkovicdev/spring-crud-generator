package com.markozivkovic.codegen.utils;

import static com.markozivkovic.codegen.constants.CacheConstants.ORG_SPRINGFRAMEWORK_CACHE_ANNOTATION_CACHEABLE;
import static com.markozivkovic.codegen.constants.CacheConstants.ORG_SPRINGFRAMEWORK_CACHE_ANNOTATION_CACHE_EVICT;
import static com.markozivkovic.codegen.constants.CacheConstants.ORG_SPRINGFRAMEWORK_CACHE_ANNOTATION_CACHE_PUT;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENTITY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENUMERATED;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENUM_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATED_VALUE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATION_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ID;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_TABLE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_CASCADE_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_COLUMN;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_FETCH_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_JOIN_COLUMN;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_JOIN_TABLE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_MANY_TO_MANY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_MANY_TO_ONE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_ONE_TO_MANY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_ONE_TO_ONE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_VERSION;
import static com.markozivkovic.codegen.constants.JPAConstants.SPRING_DATA_PACKAGE_DOMAIN_PAGE;
import static com.markozivkovic.codegen.constants.JPAConstants.SPRING_DATA_PACKAGE_DOMAIN_PAGE_REQUEST;
import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_MATH_BIG_DECIMAL;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_MATH_BIG_INTEGER;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE_TIME;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_LIST;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_OBJECTS;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_STREAM_COLLECTORS;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_UUID;
import static com.markozivkovic.codegen.constants.LoggerConstants.SL4J_LOGGER;
import static com.markozivkovic.codegen.constants.LoggerConstants.SL4J_LOGGER_FACTORY;
import static com.markozivkovic.codegen.constants.SpringConstants.SPRING_FRAMEWORK_STEREOTYPE_SERVICE;
import static com.markozivkovic.codegen.constants.TransactionConstants.SPRING_FRAMEWORK_TRANSACTION_ANNOTATION_TRANSACTIONAL;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;

public class ImportUtils {

    private static final String PAGE_TO = "PageTO";

    private static final String MANY_TO_MANY = "ManyToMany";

    private static final String ENUMS = "enums";
    private static final String ENUMS_PACKAGE = "." + ENUMS;
    private static final String REPOSITORIES_PACKAGE = ".repositories";
    private static final String MODELS_PACKAGE = ".models";
    private static final String MODELS_HELPERS_PACKAGE = MODELS_PACKAGE + ".helpers";
    private static final String TRANSFER_OBJECTS_PACKAGE = ".transferobjects";
    private static final String SERVICES_PACKAGE = ".services";
    private static final String BUSINESS_SERVICES_PACKAGE = ".businessservices";
    private static final String MAPPERS_PACKAGE = ".mappers";
    private static final String TRANSFER_OBJECTS_HELPERS_PACKAGE = TRANSFER_OBJECTS_PACKAGE + ".helpers";
    
    private ImportUtils() {

    }

    /**
     * Generates a string of import statements based on the fields present in the given model definition.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @param importObjects   Whether to include the java.util.Objects import.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final boolean importObjects) {

        return getBaseImport(modelDefinition, importObjects, false);
    }

    /**
     * Generates a string of import statements based on the fields present in the given model definition, with options to include
     * the java.util.Objects class and the java.util.List interface.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @param importObjects   Whether to include the java.util.Objects import.
     * @param importList      Whether to include the java.util.List import.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final boolean importObjects, final boolean importList) {

        final StringBuilder sb = new StringBuilder();

        final List<FieldDefinition> fields = modelDefinition.getFields();
        final Set<String> imports = new LinkedHashSet<>();

        addIf(FieldUtils.isAnyFieldBigDecimal(fields), imports, JAVA_MATH_BIG_DECIMAL);
        addIf(FieldUtils.isAnyFieldBigInteger(fields), imports, JAVA_MATH_BIG_INTEGER);
        addIf(FieldUtils.isAnyFieldLocalDate(fields), imports, JAVA_TIME_LOCAL_DATE);
        addIf(FieldUtils.isAnyFieldLocalDateTime(fields), imports, JAVA_TIME_LOCAL_DATE_TIME);
        addIf(importObjects, imports, JAVA_UTIL_OBJECTS);
        addIf(FieldUtils.isAnyFieldUUID(fields), imports, JAVA_UTIL_UUID);
        
        final boolean hasLists = FieldUtils.isAnyRelationOneToMany(fields) ||
                FieldUtils.isAnyRelationManyToMany(fields);

        addIf(hasLists || importList, imports, JAVA_UTIL_LIST);

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
            JAKARTA_PERSISTANCE_ENTITY, JAKARTA_PERSISTANCE_GENERATED_VALUE, JAKARTA_PERSISTANCE_GENERATION_TYPE,
            JAKARTA_PERSISTANCE_ID, JAKARTA_PERSISTANCE_TABLE
        ));
        
        if (FieldUtils.isAnyFieldEnum(fields)) {
            imports.add(JAKARTA_PERSISTANCE_ENUM_TYPE);
            imports.add(JAKARTA_PERSISTANCE_ENUMERATED);
        }

        final boolean hasAnyFieldColumn = fields.stream()
                .anyMatch(field -> Objects.nonNull(field.getColumn()));

        addIf(!relations.isEmpty(), imports, JAKARTA_PERSISTENCE_JOIN_COLUMN);
        addIf(relations.contains(MANY_TO_MANY), imports, JAKARTA_PERSISTENCE_JOIN_TABLE);
        addIf(FieldUtils.isAnyRelationManyToMany(fields), imports, JAKARTA_PERSISTENCE_MANY_TO_MANY);
        addIf(FieldUtils.isAnyRelationManyToOne(fields), imports, JAKARTA_PERSISTENCE_MANY_TO_ONE);
        addIf(FieldUtils.isAnyRelationOneToMany(fields), imports, JAKARTA_PERSISTENCE_ONE_TO_MANY);
        addIf(FieldUtils.isAnyRelationOneToOne(fields), imports, JAKARTA_PERSISTENCE_ONE_TO_ONE);
        addIf(FieldUtils.isFetchTypeDefined(fields), imports, JAKARTA_PERSISTENCE_FETCH_TYPE);
        addIf(FieldUtils.isCascadeTypeDefined(fields), imports, JAKARTA_PERSISTENCE_CASCADE_TYPE);
        addIf(optimisticLocking, imports, JAKARTA_PERSISTENCE_VERSION);
        addIf(hasAnyFieldColumn, imports, JAKARTA_PERSISTENCE_COLUMN);

        return imports.stream()
                  .map(imp -> String.format(IMPORT, imp))
                  .sorted()
                  .collect(Collectors.joining());
    }

    /**
     * Generates a string of import statements for the generated enums, if any.
     * 
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return A string containing the necessary import statements for the generated enums.
     */
    public static String computeEnumsAndHelperEntitiesImport(final ModelDefinition modelDefinition, final String outputDir) {

        return computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir, true, false);
    }

    public static String computeEnumsAndHelperEntitiesImport(final ModelDefinition modelDefinition, final String outputDir,
            final boolean importJsonFields, final boolean transferObjects) {

        final Set<String> imports = new LinkedHashSet<>();
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        if (!FieldUtils.isAnyFieldEnum(modelDefinition.getFields()) && !FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            return "";
        }

        final List<FieldDefinition> enumFields = FieldUtils.extractEnumFields(modelDefinition.getFields());

        enumFields.forEach(enumField -> {
            
            final String enumName;
            if (!enumField.getName().endsWith("Enum")) {
                enumName = String.format("%sEnum", StringUtils.capitalize(enumField.getName()));
            } else {
                enumName = StringUtils.capitalize(enumField.getName());
            }

            imports.add(String.format(IMPORT, packagePath + ENUMS_PACKAGE + "." + enumName));
        });

        if (importJsonFields) {
            final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
            jsonFields.stream()
                    .map(FieldUtils::extractJsonFieldName)
                    .forEach(fieldName -> {
                        if (transferObjects) {
                            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_HELPERS_PACKAGE + "." + fieldName));
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
     * Computes the base import statements for a JPA service.
     *
     * @param cache Whether to include the Spring caching annotations.
     * @return A string containing the necessary import statements for the base JPA service.
     */
    public static String computeJpaServiceBaseImport(final boolean cache) {

        final Set<String> imports = new LinkedHashSet<>();

        imports.add(String.format(IMPORT, SL4J_LOGGER));
        imports.add(String.format(IMPORT, SL4J_LOGGER_FACTORY));
        imports.add(String.format(IMPORT, SPRING_DATA_PACKAGE_DOMAIN_PAGE));
        imports.add(String.format(IMPORT, SPRING_DATA_PACKAGE_DOMAIN_PAGE_REQUEST));
        imports.add(String.format(IMPORT, SPRING_FRAMEWORK_STEREOTYPE_SERVICE));
        imports.add(String.format(IMPORT, SPRING_FRAMEWORK_TRANSACTION_ANNOTATION_TRANSACTIONAL));

        if (cache) {
            imports.add(String.format(IMPORT, ORG_SPRINGFRAMEWORK_CACHE_ANNOTATION_CACHEABLE));
            imports.add(String.format(IMPORT, ORG_SPRINGFRAMEWORK_CACHE_ANNOTATION_CACHE_EVICT));
            imports.add(String.format(IMPORT, ORG_SPRINGFRAMEWORK_CACHE_ANNOTATION_CACHE_PUT));
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
            imports.add(String.format(IMPORT, JAVA_UTIL_UUID));
        }

        if (!manyToManyFields.isEmpty() || !oneToManyFields.isEmpty()) {
            imports.add(String.format(IMPORT, JAVA_UTIL_LIST));
            imports.add(String.format(IMPORT, JAVA_UTIL_STREAM_COLLECTORS));
        }

        relations.forEach(realtionField -> {

            final ModelDefinition relationModel = entities.stream()
                    .filter(entity -> entity.getName().equals(realtionField.getType()))
                    .findFirst()
                    .orElseThrow();

            final FieldDefinition relationIdField = FieldUtils.extractIdField(relationModel.getFields());

            if (FieldUtils.isIdFieldUUID(relationIdField)) {
                imports.add(String.format(IMPORT, JAVA_UTIL_UUID));
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
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeControllerProjectImports(final ModelDefinition modelDefinition, final String outputDir) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());

        final List<FieldDefinition> manyToManyFields = FieldUtils.extractManyToManyRelations(modelDefinition.getFields());
        final List<FieldDefinition> oneToManyFields = FieldUtils.extractOneToManyRelations(modelDefinition.getFields());

        Stream.concat(manyToManyFields.stream(), oneToManyFields.stream()).forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + relationModel + "TO"));    
        });

        relations.forEach(field -> {
            final String relationModel = ModelNameUtils.stripSuffix(field.getType());
            imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + relationModel + "InputTO"));
        });

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, packagePath + BUSINESS_SERVICES_PACKAGE + "." + modelWithoutSuffix + "BusinessService"));
        }

        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + SERVICES_PACKAGE + "." + modelWithoutSuffix + "Service"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + modelWithoutSuffix + "TO"));
        imports.add(String.format(IMPORT, packagePath + TRANSFER_OBJECTS_PACKAGE + "." + PAGE_TO));
        imports.add(String.format(IMPORT, packagePath + MAPPERS_PACKAGE + "." + modelWithoutSuffix + "Mapper"));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
