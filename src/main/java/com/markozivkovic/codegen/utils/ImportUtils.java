package com.markozivkovic.codegen.utils;

import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENTITY;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENUMERATED;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ENUM_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATED_VALUE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_GENERATION_TYPE;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_ID;
import static com.markozivkovic.codegen.constants.JPAConstants.JAKARTA_PERSISTANCE_TABLE;
import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_MATH_BIG_DECIMAL;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_MATH_BIG_INTEGER;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE_TIME;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_OBJECTS;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_UUID;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;

public class ImportUtils {

    private static final String ENUMS = "enums";
    private static final String ENUMS_PACKAGE = "." + ENUMS;
    
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
     * Generates a string of import statements for the base jakarta persistence annotations, as follows:
     * 
     * <ul>
     * <li>{@link Entity}</li>
     * <li>{@link GeneratedValue}</li>
     * <li>{@link GenerationType}</li>
     * <li>{@link Id}</li>
     * <li>{@link Table}</li>
     * </ul>
     * 
     * @return A string containing the necessary import statements for the base jakarta persistence annotations.
     */
    public static String computeJakartaBaseImport(final ModelDefinition modelDefinition) {
        
        final StringBuilder sb = new StringBuilder();
        final boolean isAnyFieldEnum = FieldUtils.isAnyFieldEnum(modelDefinition.getFields());

        sb.append(String.format(IMPORT, JAKARTA_PERSISTANCE_ENTITY));

        if (isAnyFieldEnum) {
            sb.append(String.format(IMPORT, JAKARTA_PERSISTANCE_ENUM_TYPE))
                    .append(String.format(IMPORT, JAKARTA_PERSISTANCE_ENUMERATED));
        }

        return sb.append(String.format(IMPORT, JAKARTA_PERSISTANCE_GENERATED_VALUE))
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_GENERATION_TYPE))
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_ID))
                .append(String.format(IMPORT, JAKARTA_PERSISTANCE_TABLE))
                .toString();
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

            sb.append(String.format(IMPORT, packagePath + ENUMS_PACKAGE + "." + enumName))
                    .append("\n");
        });

        return sb.toString();
    }

}
