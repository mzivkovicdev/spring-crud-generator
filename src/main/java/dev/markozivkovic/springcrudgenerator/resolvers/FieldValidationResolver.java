package dev.markozivkovic.springcrudgenerator.resolvers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.constants.AnnotationConstants;
import dev.markozivkovic.springcrudgenerator.enums.BasicType;
import dev.markozivkovic.springcrudgenerator.enums.SpecialType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.utils.RegexUtils;

public final class FieldValidationResolver {
    
    private FieldValidationResolver() {}

    /**
     * Resolves the validation annotations for a given field definition.
     * 
     * @param field the field definition for which to resolve the validation annotations
     * @return a string of validation annotations for the given field definition
     */
    public static List<String> resolveValidationForField(final FieldDefinition field) {

        if (Objects.isNull(field.getValidation()) && Objects.isNull(field.getColumn())) {
            return new ArrayList<>();
        }

        final boolean basicType = BasicType.isBasicType(field.getType());
        final boolean isCollection = SpecialType.isCollectionType(field.getType());

        final List<String> out = new ArrayList<>();

        if (Objects.nonNull(field.getValidation()) && Boolean.TRUE.equals(field.getValidation().getRequired()))
            out.add(AnnotationConstants.NOT_NULL_ANNOTATION);

        if (Objects.nonNull(field.getColumn()) && Boolean.FALSE.equals(field.getColumn().getNullable()))
            out.add(AnnotationConstants.NOT_NULL_ANNOTATION);

        if (basicType) resolveBasicTypeValidations(field, out);
        if (isCollection) resolveCollectionValidations(field, out);

        return out;
    }

    /**
     * Resolves the validation annotations for a given field definition that is a collection.
     * Currently only supports the following annotations: @NotNull, @Size(min = %d, max = %d).
     * If the field is required, adds the @NotNull annotation.
     * If the field has either minItems or maxItems set, adds the @Size(min = %d, max = %d) annotation.
     * 
     * @param field the field definition for which to resolve the validation annotations
     * @param out the list of strings to which to add the resolved validation annotations
     */
    private static void resolveCollectionValidations(final FieldDefinition field, final List<String> out) {

        if (Boolean.TRUE.equals(field.getValidation().isNotEmpty())) out.add(AnnotationConstants.NOT_EMPTY_ANNOTATION);

        if (Objects.nonNull(field.getValidation().getMinItems()) || Objects.nonNull(field.getValidation().getMaxItems())) 
            out.add(computeSizeAnnotation(field.getValidation().getMinItems(), field.getValidation().getMaxItems()));
    }

    /**
     * Resolves the validation annotations for a given field definition.
     * Currently only supports basic types (String, Integer, Long, BigInteger, Double, Float, BigDecimal).
     * For String, supports the following annotations: @NotNull, @NotEmpty, @NotBlank, @Email.
     * For numeric types, supports the following annotations: @Min, @Max.
     * 
     * @param field the field definition to resolve the validation annotations for
     * @param out the list to which the resolved validation annotations should be added
     */
    private static void resolveBasicTypeValidations(final FieldDefinition field, final List<String> out) {

        final BasicType basicType = BasicType.fromString(field.getType().trim());

        switch (basicType) {
            case STRING:
                Integer minLength = Objects.nonNull(field.getValidation()) ? field.getValidation().getMinLength() : null;
                Integer maxLength = Objects.nonNull(field.getValidation()) ? field.getValidation().getMaxLength() : null;
                if (Objects.isNull(maxLength) && Objects.nonNull(field.getColumn()) && Objects.nonNull(field.getColumn().getLength())) {
                    maxLength = field.getColumn().getLength();
                }

                if (Objects.nonNull(minLength) || Objects.nonNull(maxLength)) out.add(computeSizeAnnotation(minLength, maxLength));
                
                if (Objects.nonNull(field.getValidation())) {
                    if (Boolean.TRUE.equals(field.getValidation().getNotBlank())) out.add(AnnotationConstants.NOT_BLANK_ANNOTATION);
                    if (Boolean.TRUE.equals(field.getValidation().getNotEmpty())) out.add(AnnotationConstants.NOT_EMPTY_ANNOTATION);
                    if (Boolean.TRUE.equals(field.getValidation().getEmail())) out.add(AnnotationConstants.EMAIL_ANNOTATION);
                    if (Objects.nonNull(field.getValidation().getPattern())) {
                        final String normalizedPattern = RegexUtils.normalizeRegexPattern(field.getValidation().getPattern());
                        out.add(String.format(AnnotationConstants.PATTERN_ANNOTATION, normalizedPattern));
                    }
                }
                break;
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
                if (Objects.nonNull(field.getValidation())) {
                    if (Objects.nonNull(field.getValidation().getMin())) 
                        out.add(String.format(AnnotationConstants.MIN_ANNOTATION, field.getValidation().getMin().longValue()));
                    
                    if (Objects.nonNull(field.getValidation().getMax())) 
                        out.add(String.format(AnnotationConstants.MAX_ANNOTATION, field.getValidation().getMax().longValue()));
                }
                break;
            case DOUBLE:
            case FLOAT:
            case BIG_DECIMAL:
                if (Objects.nonNull(field.getValidation())) {
                    if (Objects.nonNull(field.getValidation().getMin())) 
                        out.add(String.format(AnnotationConstants.DECIMAL_MIN_ANNOTATION, field.getValidation().getMin()));
                    
                    if (Objects.nonNull(field.getValidation().getMax())) 
                        out.add(String.format(AnnotationConstants.DECIMAL_MAX_ANNOTATION, field.getValidation().getMax()));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Computes the string size annotations for the given min and max values.
     * If both min and max are present, generates a @Size(min = %d, max = %d) annotation.
     * If only min is present, generates a @Size(min = %d) annotation.
     * If only max is present, generates a @Size(max = %d) annotation.
     * 
     * @param min the minimum length of the string
     * @param max the maximum length of the string
     * @return the string size annotation as a string
     */
    private static String computeSizeAnnotation(final Integer min, final Integer max) {
        
        if (Objects.nonNull(min) && Objects.nonNull(max))
            return String.format(AnnotationConstants.SIZE_MIN_MAX_ANNOTATION, min, max);
        
        if (Objects.nonNull(min))
            return String.format(AnnotationConstants.SIZE_MIN_ANNOTATION, min);
        
        return String.format(AnnotationConstants.SIZE_MAX_ANNOTATION, max);
    }

}
