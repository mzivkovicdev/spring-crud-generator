package dev.markozivkovic.springcrudgenerator.templates.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.enums.BasicType;
import dev.markozivkovic.springcrudgenerator.enums.SpecialType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;
import dev.markozivkovic.springcrudgenerator.utils.ContainerUtils;

public final class ValidationContextBuilder {

    private static final String GENERATE_STRING_METHOD = "generateString(%s)";
    private static final String GENERATE_LIST = "generateList(%s, %s)";

    private ValidationContextBuilder() {}

    /**
     * Contributes validation overrides to the given context.
     * For each field in the given model definition, the following rules are applied:
     * 1. If the field has any validation defined, a validation override is generated.
     * 2. If the field has a column length defined, a validation override is generated.
     * 3. If the field is a basic type (i.e., string, numeric, etc.), a validation override is generated.
     * 4. If the field is a collection type, a validation override is generated.
     * The generated validation overrides are added to the given context under the key
     * {@link TemplateContextConstants#VALIDATION_OVERRIDES}.
     * 
     * @param modelDefinition the model definition to contribute from
     * @param context the context to contribute to
     * @param generatorFieldName the name of the generator field
     * @param singleObjectMethodName the name of the single object method
     */
    public static void contribute(final ModelDefinition modelDefinition, final Map<String, Object> context,
                                  final String generatorFieldName, final String singleObjectMethodName) {
        
        final List<Map<String, Object>> overrides = new ArrayList<>();

        modelDefinition.getFields().forEach(field -> {

            final String fieldName = field.getName();
            final String type = field.getResolvedType();
    
            final ValidationDefinition validation = field.getValidation();
            final Integer columnLength = Objects.nonNull(field.getColumn()) ? field.getColumn().getLength() : null;
            final boolean hasAnyValidation = isValidationDefined(validation);
            final boolean hasColumnLength = columnLength != null;
            final boolean isBasicType = BasicType.isBasicType(type);
    
            if (!hasAnyValidation && !hasColumnLength) return;
    
            if (isBasicType && BasicType.STRING.equals(BasicType.fromString(type))) {
                final StringOverride override = buildStringOverride(field, validation, columnLength);
                if (Objects.nonNull(override)) {
                    overrides.add(Map.of(
                        TemplateContextConstants.FIELD, fieldName, TemplateContextConstants.VALID_VALUE,
                        override.validExpr, TemplateContextConstants.INVALID_VALUE, override.invalidExpr
                    ));
                }
                return;
            }
    
            if (isBasicType && isNumeric(type)) {
                final NumericOverride override = buildNumericOverride(type, validation);
                if (Objects.nonNull(override)) {
                    overrides.add(Map.of(
                        TemplateContextConstants.FIELD, fieldName, TemplateContextConstants.VALID_VALUE,
                        override.validExpr, TemplateContextConstants.INVALID_VALUE, override.invalidExpr
                    ));
                }
                return;
            }
    
            if (SpecialType.isCollectionType(type)) {
                final CollectionOverride override = buildCollectionOverride(type, validation, generatorFieldName, singleObjectMethodName);
                if (Objects.nonNull(override)) {
                    overrides.add(Map.of(
                        TemplateContextConstants.FIELD, fieldName, TemplateContextConstants.VALID_VALUE,
                        override.validExpr, TemplateContextConstants.INVALID_VALUE, override.invalidExpr
                    ));
                }
                return;
            }
        });

        if (!ContainerUtils.isEmpty(overrides)) {
            context.put(TemplateContextConstants.VALIDATION_OVERRIDES, overrides);
        }
    }

    /**
     * Checks if any of the fields in the given list have any validation defined.
     * A field is considered to have validation defined if it has any of the following defined:
     * - required
     * - notBlank
     * - notEmpty
     * - minLength
     * - maxLength
     * - min
     * - max
     * - minItems
     * - maxItems
     * - email
     * 
     * @param fields The list of fields to check for validations.
     * @return True if any of the fields has any validation defined, false otherwise.
     */
    private static boolean isValidationDefined(final ValidationDefinition validationDefinition) {

        return Objects.nonNull(validationDefinition) && (
                Objects.nonNull(validationDefinition.getRequired()) ||
                Objects.nonNull(validationDefinition.getNotBlank()) ||
                Objects.nonNull(validationDefinition.getNotEmpty()) ||
                Objects.nonNull(validationDefinition.getMinLength()) ||
                Objects.nonNull(validationDefinition.getMaxLength()) ||
                Objects.nonNull(validationDefinition.getMin()) ||
                Objects.nonNull(validationDefinition.getMax()) ||
                Objects.nonNull(validationDefinition.getMinItems()) ||
                Objects.nonNull(validationDefinition.getMaxItems()) ||
                Objects.nonNull(validationDefinition.getPattern()) ||
                Objects.nonNull(validationDefinition.getEmail())
        );
    }

    /**
     * Builds a StringOverride based on the given field and validation definition.
     * 
     * The StringOverride contains two expressions: a valid expression and an invalid expression.
     * The valid expression is a string that satisfies all the validation rules defined in the given validation definition.
     * The invalid expression is a string that does not satisfy one or more of the validation rules defined in the given validation definition.
     * 
     * The expressions are generated based on the following rules:
     * - If the field has a required validation, the valid expression will be a single character string, and the invalid expression will be null.
     * - If the field has a not blank validation, the valid expression will be a string with a single non-whitespace character, and the invalid expression will be a string with only whitespace characters.
     * - If the field has a not empty validation, the valid expression will be a string with a single character, and the invalid expression will be an empty string.
     * - If the field has an email validation, the valid expression will be a string that satisfies the email regex pattern defined in the given validation definition, and the invalid expression will be a string that does not satisfy the email regex pattern.
     * - If the field has a pattern validation, the valid expression will be a string that satisfies the regex pattern defined in the given validation definition, and the invalid expression will be a string that does not satisfy the regex pattern.
     * - If the field has a min length validation, the valid expression will be a string with a length equal to the min length, and the invalid expression will be a string with a length less than the min length.
     * - If the field has a max length validation, the valid expression will be a string with a length equal to the max length, and the invalid expression will be a string with a length greater than the max length.
     * 
     * @param field        The field definition to generate the StringOverride for.
     * @param validation   The validation definition to generate the StringOverride for.
     * @param columnLength The length of the column associated with the given field.
     * @return A StringOverride containing a valid expression and an invalid expression based on the given field and validation definition.
     */
    private static StringOverride buildStringOverride(final FieldDefinition field, final ValidationDefinition validation,
                final Integer columnLength) {
        
        final boolean required = Objects.nonNull(validation) && Boolean.TRUE.equals(validation.getRequired());
        final boolean notBlank = Objects.nonNull(validation) && Boolean.TRUE.equals(validation.getNotBlank());
        final boolean notEmpty = Objects.nonNull(validation) && Boolean.TRUE.equals(validation.getNotEmpty());
        final boolean email = Objects.nonNull(validation) && Boolean.TRUE.equals(validation.getEmail());
        final String pattern = (validation != null) ? validation.getPattern() : null;

        final Integer minLengthRaw = (validation != null) ? validation.getMinLength() : null;
        final Integer maxLengthRaw = (validation != null) ? validation.getMaxLength() : null;
        final Integer effectiveMin = Math.max(
                (minLengthRaw == null ? 0 : minLengthRaw),
                (required || notBlank || notEmpty) ? 1 : 0
        );

        final Integer effectiveMax = minNonNull(columnLength, maxLengthRaw);

        final String validExpr;
        if (email) {
            validExpr = quote(pickValidEmail(effectiveMax, effectiveMin));
        } else if (Objects.nonNull(pattern)) {
            String sample = trySampleCommonPatterns(pattern);
            if (Objects.nonNull(sample)) {
                sample = clampString(sample, effectiveMin, effectiveMax);
                validExpr = quote(sample);
            } else {
                final Integer length = pickValidLength(effectiveMin, effectiveMax);
                validExpr = String.format(GENERATE_STRING_METHOD, length);
            }
        } else if (Objects.nonNull(effectiveMax) || effectiveMin > 0) {
            final Integer length = pickValidLength(effectiveMin, effectiveMax);
            validExpr = String.format(GENERATE_STRING_METHOD, length);
        } else if (required || notBlank || notEmpty) {
            validExpr = quote("a");
        } else {
            validExpr = quote("a");
        }

        String invalidExpr = null;

        if (required) {
            invalidExpr = "null";
        } else if (notBlank) {
            invalidExpr = quote("   ");
        } else if (notEmpty) {
            invalidExpr = quote("");
        } else if (email) {
            invalidExpr = quote("not-an-email");
        } else if (Objects.nonNull(effectiveMax)) {
            invalidExpr = String.format(GENERATE_STRING_METHOD, (effectiveMax + 1));
        } else if (Objects.nonNull(minLengthRaw) && minLengthRaw > 0) {
            invalidExpr = String.format(GENERATE_STRING_METHOD, Math.max(0, minLengthRaw - 1));
        } else if (Objects.nonNull(pattern)) {
            invalidExpr = quote("###");
        } else {
            return null;
        }

        return new StringOverride(validExpr, invalidExpr);
    }

    /**
     * Returns a valid length given min and max constraints.
     * 
     * If max is null, returns Math.max(1, min).
     * If min is greater than max, returns max.
     * Otherwise, returns Math.max(min, 1).
     * 
     * @param min the minimum length constraint
     * @param max the maximum length constraint
     * @return a valid length given the constraints
     */
    private static int pickValidLength(final Integer min, final Integer max) {
        
        if (max == null) return Math.max(1, min);
        
        if (min > max) return max;
        
        return Math.max(min, 1);
    }

    /**
     * Picks a valid email string which is within the given length range.
     * If the default candidate ("a@b.co") is too long for the given max length, it is returned as is.
     * If the default candidate is too short for the given min length, "test@example.com" is returned.
     * If the default candidate is within the length range, it is returned as is.
     * 
     * @param max    the maximum length of the email string
     * @param min    the minimum length of the email string
     * @return a valid email string which is within the given length range
     */
    private static String pickValidEmail(final Integer max, final Integer min) {
        String candidate = "a@b.co";
        if (max != null && candidate.length() > max) {
            return "a@b.co";
        }
        if (candidate.length() < min) {
            candidate = "test@example.com";
        }
        if (max != null && candidate.length() > max) {
            return "a@b.co";
        }
        return candidate;
    }

    /**
     * Clamps a given string to a certain length range.
     * If the string is too short, it is padded with "a"s until it reaches the minimum length.
     * If the string is too long, it is truncated to the maximum length.
     * If the string is within the length range, it is returned as is.
     * 
     * @param string the string to clamp
     * @param min    the minimum length
     * @param max    the maximum length
     * @return the clamped string
     */
    private static String clampString(final String string, final Integer min, final Integer max) {
        
        if (string.length() < min) {
            return string + "a".repeat(min - string.length());
        }
        
        if (max != null && string.length() > max) {
            return string.substring(0, max);
        }
        
        return string;
    }

    /**
     * Attempts to sample a common pattern given by the regular expression.
     * If the pattern matches the common password pattern (at least one lowercase letter, at least one uppercase letter, at least one digit, and at least 8 characters), returns "Abcdef1g".
     * If the pattern matches the common email pattern (contains "@"), returns "a@b.co".
     * Otherwise, returns null.
     * 
     * @param pattern the regular expression pattern to sample
     * @return a sample string that matches the given pattern, or null if no sample can be found
     */
    private static String trySampleCommonPatterns(String pattern) {

        if (pattern.contains("(?=.*") && pattern.contains("\\d") && (pattern.contains("[A-Za-z]") || pattern.contains("[a-zA-Z]"))) {
            return "Abcdef1g";
        }

        if (pattern.contains("@")) {
            return "a@b.co";
        }
        return null;
    }

    /**
     * Builds a NumericOverride based on the given type and validation definition.
     * 
     * If the given type is a BigDecimal, generates a valid expression that is equal to the minimum or maximum value if present, and an invalid expression that is one more than the maximum value if present, or one less than the minimum value if present. If both minimum and maximum are present, the valid expression will be equal to the minimum value, and the invalid expression will be one more than the maximum value.
     * If the given type is an integer (Integer, Long, etc.), generates a valid expression that is equal to the minimum or maximum value if present, and an invalid expression that is one more than the maximum value if present, or one less than the minimum value if present. If both minimum and maximum are present, the valid expression will be equal to the minimum value, and the invalid expression will be one more than the maximum value.
     * If neither minimum nor maximum value is present and the field is required, generates an invalid expression that is null.
     * 
     * @param type the type of the field to generate the NumericOverride for
     * @param validation the validation definition to generate the NumericOverride for
     * @return a NumericOverride containing a valid expression and an invalid expression, or null if no valid expression and invalid expression can be generated
     */
    private static NumericOverride buildNumericOverride(final String type, final ValidationDefinition validation) {
        
        if (Objects.isNull(validation)) return null;

        final boolean required = Boolean.TRUE.equals(validation.getRequired());
        final BigDecimal min = validation.getMin();
        final BigDecimal max = validation.getMax();

        String validExpr;
        String invalidExpr;

        if (BasicType.isBasicType(type) && BasicType.BIG_DECIMAL.equals(BasicType.fromString(type))) {
            final BigDecimal valid = (min != null) ? min : (max != null ? max : BigDecimal.ONE);
            validExpr = bigDecimalExpr(valid);

            if (max != null) {
                invalidExpr = bigDecimalExpr(max.add(BigDecimal.ONE));
            } else if (min != null) {
                invalidExpr = bigDecimalExpr(min.subtract(BigDecimal.ONE));
            } else if (required) {
                invalidExpr = "null";
            } else {
                return null;
            }

            return new NumericOverride(validExpr, invalidExpr);
        }

        long valid = 1;
        if (min != null) valid = min.longValue();
        else if (max != null) valid = max.longValue();

        validExpr = String.valueOf(valid);

        if (max != null) {
            invalidExpr = String.valueOf(max.longValue() + 1);
        } else if (min != null) {
            invalidExpr = String.valueOf(min.longValue() - 1);
        } else if (required) {
            invalidExpr = "null";
        } else {
            return null;
        }

        return new NumericOverride(validExpr, invalidExpr);
    }

    /**
     * Converts a BigDecimal into a string that can be used to create a new instance of that BigDecimal.
     * 
     * @param value the BigDecimal to convert
     * @return a string that can be used to create a new instance of the given BigDecimal
     */
    private static String bigDecimalExpr(final BigDecimal value) {

        return String.format("new %s(\"%s\")", ImportConstants.Java.BIG_DECIMAL, value.toPlainString());
    }

    private static CollectionOverride buildCollectionOverride(final String type, final ValidationDefinition v,
                final String generatorFieldName, final String singleObjectMethodName) {
        
        if (v == null) return null;

        final boolean required = Boolean.TRUE.equals(v.getRequired());
        final boolean notEmpty = Boolean.TRUE.equals(v.getNotEmpty());

        final Integer minItems = v.getMinItems();
        final Integer maxItems = v.getMaxItems();

        int effMin = Math.max(minItems == null ? 0 : minItems, (required || notEmpty) ? 1 : 0);
        Integer effMax = maxItems;

        int validSize = effMin;
        if (effMax != null && validSize > effMax) validSize = effMax;

        Integer invalidSize = null;
        if (effMax != null) invalidSize = effMax + 1;
        else if (minItems != null) invalidSize = Math.max(0, minItems - 1);
        else if (required || notEmpty) invalidSize = 0;

        if (invalidSize == null) {
            return null;
        }

        final String elementType = extractFirstGeneric(type);
        final String supplier = buildSupplier(elementType, generatorFieldName, singleObjectMethodName);

        final String validExpr = buildCollectionExpr(type, validSize, supplier);
        final String invalidExpr = buildCollectionExpr(type, invalidSize, supplier);

        return new CollectionOverride(validExpr, invalidExpr);
    }

    /**
     * Builds a string expression for a collection with a given size and supplier.
     * If the size is less than or equal to 0, it returns an empty collection of the given type.
     * If the size is greater than 0, it returns a collection of the given type with the given size, populated with objects generated by the supplier.
     * If the collection type is a set, it returns a HashSet. Otherwise, it returns a list.
     * 
     * @param collectionType the type of the collection
     * @param size the size of the collection
     * @param supplier the supplier of objects for the collection
     * @return the string expression for the collection
     */
    private static String buildCollectionExpr(final String collectionType, final Integer size, final String supplier) {
        if (size <= 0) {
            final String collectionTypeImport = SpecialType.isSetType(collectionType) ?
                    ImportConstants.Java.SET : ImportConstants.Java.LIST;
            return String.format("%s.of()", collectionTypeImport);
        }

        final String base = String.format(GENERATE_LIST, size, supplier);

        if (SpecialType.isSetType(collectionType)) {
            return "new java.util.HashSet<>(" + base + ")";
        }
        return base;
    }

    /**
     * Builds a supplier string for the given element type and generator field name and single object method name.
     * 
     * If the element type is null, returns a supplier that returns the string "a".
     * If the element type is a basic type and is a string, returns a supplier that returns the string "a".
     * If the element type is a numeric type, returns a supplier that returns the value 1.
     * Otherwise, returns a supplier that calls the given single object method name on the given generator field name with the given element type as an argument.
     * 
     * @param elementType            the element type to generate the supplier for
     * @param generatorFieldName     the generator field name to use in the supplier
     * @param singleObjectMethodName the single object method name to use in the supplier
     * @return a supplier string for the given element type and generator field name and single object method name
     */
    private static String buildSupplier(final String elementType, final String generatorFieldName, final String singleObjectMethodName) {
        if (elementType == null) {
            return "() -> \"a\"";
        }
        final boolean isBasicType = BasicType.isBasicType(elementType);
        
        if (isBasicType && BasicType.STRING.equals(BasicType.fromString(elementType)))
            return "() -> \"a\"";
        
        if (isNumeric(elementType)) return "() -> 1";

        return String.format("() -> %s.%s(%s.class)", generatorFieldName, singleObjectMethodName, elementType);
    }

    /**
     * Extracts the first generic type from the given type string.
     * This method takes a type string as an argument, and returns the first generic type found in the string.
     * The generic type is the substring between the first '<' and the last '>' characters.
     * If the generic type contains a comma, the substring before the comma is returned.
     * If no generic type is found, this method returns null.
     *
     * @param type the type string to extract the generic type from
     * @return the first generic type found in the string, or null if no generic type is found
     */
    private static String extractFirstGeneric(final String type) {
        
        final int lt = type.indexOf('<');
        final int gt = type.lastIndexOf('>');

        if (lt < 0 || gt < 0 || gt <= lt)
            return null;
        
        String inside = type.substring(lt + 1, gt).trim();
        final int comma = inside.indexOf(',');
        
        if (comma > 0)
            inside = inside.substring(0, comma).trim();

        return inside;
    }

    /**
     * Returns the minimum of two integers, ignoring null values.
     * If one of the numbers is null, the other number is returned.
     * If both numbers are null, null is returned.
     * 
     * @param firstNumber  the first integer
     * @param secondNumber the second integer
     * @return the minimum of the two integers, or null if both are null
     */
    private static Integer minNonNull(final Integer firstNumber, final Integer secondNumber) {
        
        if (firstNumber == null) return secondNumber;
        
        if (secondNumber == null) return firstNumber;
        
        return Math.min(firstNumber, secondNumber);
    }

    /**
     * Quotes the given string. This method will replace any occurrences of
     * backslashes with double backslashes and any occurrences of double quotes
     * with escaped double quotes.
     * 
     * @param string the string to quote
     * @return the quoted string
     */
    private static String quote(final String string) {

        return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Checks if the given type is a numeric type.
     * This method returns true for the following types: INTEGER, LONG, DOUBLE, FLOAT, BIG_DECIMAL, BIG_INTEGER, SHORT.
     * 
     * @param type the type to check
     * @return true if the type is a numeric type, false otherwise
     */
    private static boolean isNumeric(final String type) {
        
        if (!BasicType.isBasicType(type)) {
            return false;
        }
        
        final BasicType basicType = BasicType.fromString(type);
        
        return switch (basicType) {
            case INTEGER, LONG, DOUBLE, FLOAT, BIG_DECIMAL, BIG_INTEGER, SHORT -> true;
            default -> false;
        };
    }

    private record StringOverride(String validExpr, String invalidExpr) {

    }

    private record CollectionOverride(String validExpr, String invalidExpr) {

    }

    private record NumericOverride(String validExpr, String invalidExpr) {
        
    }
}
