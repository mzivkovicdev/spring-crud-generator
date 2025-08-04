package com.markozivkovic.codegen.utils;

import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENTITY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENUMERATED;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENUM_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATED_VALUE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATION_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ID;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_TABLE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_CASCADE_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_FETCH_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_JOIN_COLUMN;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_MANY_TO_MANY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_MANY_TO_ONE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_ONE_TO_MANY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTENCE_ONE_TO_ONE;
import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_MATH_BIG_DECIMAL;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_MATH_BIG_INTEGER;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE_TIME;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_OBJECTS;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_UUID;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;

public class ImportUtils {

    private static final String ENUMS = "enums";
    private static final String ENUMS_PACKAGE = "." + ENUMS;
    private static final String REPOSITORIES_PACKAGE = ".repositories";
    private static final String MODELS_PACKAGE = ".models";
    
    private ImportUtils() {

    }

    /**
     * Generates a string of import statements based on the fields present in the given model definition.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final boolean importObjects) {

        final StringBuilder sb = new StringBuilder();

        final List<FieldDefinition> fields = modelDefinition.getFields();

        if (FieldUtils.isAnyFieldBigDecimal(fields)) {
            sb.append(String.format(IMPORT, JAVA_MATH_BIG_DECIMAL));
        }

        if (FieldUtils.isAnyFieldBigInteger(fields)) {
            sb.append(String.format(IMPORT, JAVA_MATH_BIG_INTEGER));
        }

        if (FieldUtils.isAnyFieldLocalDate(fields)) {
            sb.append(String.format(IMPORT, JAVA_TIME_LOCAL_DATE));
        }

        if (FieldUtils.isAnyFieldLocalDateTime(fields)) {
            sb.append(String.format(IMPORT, JAVA_TIME_LOCAL_DATE_TIME));
        }

        if (importObjects) {
            sb.append(String.format(IMPORT, JAVA_UTIL_OBJECTS));
        }

        if (FieldUtils.isAnyFieldUUID(fields)) {
            sb.append(String.format(IMPORT, JAVA_UTIL_UUID));
        }

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
     * @return A string containing the necessary import statements for the base jakarta persistence annotations.
     */
    public static String computeJakartaImports(final ModelDefinition modelDefinition) {

        final Set<String> imports = new LinkedHashSet<>();
        final List<FieldDefinition> fields = modelDefinition.getFields();

        imports.addAll(Set.of(
            JAKARTA_PERSISTANCE_ENTITY, JAKARTA_PERSISTANCE_GENERATED_VALUE, JAKARTA_PERSISTANCE_GENERATION_TYPE,
            JAKARTA_PERSISTANCE_ID, JAKARTA_PERSISTANCE_TABLE
        ));
        
        if (FieldUtils.isAnyFieldEnum(fields)) {
            imports.add(JAKARTA_PERSISTANCE_ENUM_TYPE);
            imports.add(JAKARTA_PERSISTANCE_ENUMERATED);
        }

        if (!FieldUtils.extractRelations(fields).isEmpty()) {
            imports.add(JAKARTA_PERSISTENCE_JOIN_COLUMN);
        }

        addIf(FieldUtils.isAnyRelationManyToMany(fields), imports, JAKARTA_PERSISTENCE_MANY_TO_MANY);
        addIf(FieldUtils.isAnyRelationManyToOne(fields), imports, JAKARTA_PERSISTENCE_MANY_TO_ONE);
        addIf(FieldUtils.isAnyRelationOneToMany(fields), imports, JAKARTA_PERSISTENCE_ONE_TO_MANY);
        addIf(FieldUtils.isAnyRelationOneToOne(fields), imports, JAKARTA_PERSISTENCE_ONE_TO_ONE);
        addIf(FieldUtils.isFetchTypeDefined(fields), imports, JAKARTA_PERSISTENCE_FETCH_TYPE);
        addIf(FieldUtils.isCascadeTypeDefined(fields), imports, JAKARTA_PERSISTENCE_CASCADE_TYPE);

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
    public static String computeEnumsImport(final ModelDefinition modelDefinition, final String outputDir) {

        final boolean isAnyFieldEnum = FieldUtils.isAnyFieldEnum(modelDefinition.getFields());
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        if (!isAnyFieldEnum) {
            return "";
        }

        final List<FieldDefinition> enumFields = FieldUtils.extractEnumFields(modelDefinition.getFields());

        final StringBuilder sb = new StringBuilder();

        enumFields.forEach(enumField -> {
            
            final String enumName;
            if (!enumField.getName().endsWith("Enum")) {
                enumName = String.format("%sEnum", StringUtils.capitalize(enumField.getName()));
            } else {
                enumName = StringUtils.capitalize(enumField.getName());
            }

            sb.append(String.format(IMPORT, packagePath + ENUMS_PACKAGE + "." + enumName));
        });

        return sb.toString();
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

        final String enumsImport = computeEnumsImport(modelDefinition, outputDir);

        imports.add(enumsImport);
        imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()));
        imports.add(String.format(IMPORT, packagePath + REPOSITORIES_PACKAGE + "." + modelWithoutSuffix + "Repository"));

        relationModels.forEach(relation -> imports.add(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + relation)));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
