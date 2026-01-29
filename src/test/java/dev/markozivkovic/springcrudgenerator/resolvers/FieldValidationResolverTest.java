package dev.markozivkovic.springcrudgenerator.resolvers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.constants.AnnotationConstants;
import dev.markozivkovic.springcrudgenerator.models.ColumnDefinition;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;

class FieldValidationResolverTest {

    private static final String COLLECTION_TYPE = "List<String>";

    private FieldDefinition field(final String type, final ValidationDefinition validation, final ColumnDefinition column) {
        final FieldDefinition field = new FieldDefinition();
        field.setType(type);
        field.setValidation(validation);
        field.setColumn(column);
        return field;
    }

    private ColumnDefinition column(final Boolean nullable, final Integer length) {
        final ColumnDefinition column = new ColumnDefinition();
        column.setNullable(nullable);
        column.setLength(length);
        return column;
    }

    @Test
    void resolveValidationForField_returnsEmptyWhenValidationAndColumnNull() {
        
        final FieldDefinition field = field("String", null, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveValidationForField_addsNotNullWhenRequiredTrue() {
        
        final ValidationDefinition validation = new ValidationDefinition();
        validation.setRequired(true);

        final FieldDefinition field = field("String", validation, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(AnnotationConstants.NOT_NULL_ANNOTATION), result);
    }

    @Test
    void resolveValidationForField_addsNotNullWhenColumnNullableFalse() {
        
        final ColumnDefinition column = column(false, null);

        final FieldDefinition field = field("String", null, column);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(AnnotationConstants.NOT_NULL_ANNOTATION), result);
    }

    @Test
    void resolveValidationForField_addsNotNullTwiceWhenBothRequiredAndNonNullable() {
        
        final ValidationDefinition validation = new ValidationDefinition();
        validation.setRequired(true);

        final ColumnDefinition column = column(false, null);
        final FieldDefinition field = field("String", validation, column);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(
                AnnotationConstants.NOT_NULL_ANNOTATION,
                AnnotationConstants.NOT_NULL_ANNOTATION
        ), result);
    }

    @Test
    void resolveValidationForField_stringAddsSizeUsingColumnLengthFallbackForMax() {
        
        final ValidationDefinition validation = new ValidationDefinition();
        validation.setMinLength(2);
        validation.setMaxLength(null);

        final ColumnDefinition c = column(true, 10);
        final FieldDefinition f = field("  String  ", validation, c);

        final List<String> result = FieldValidationResolver.resolveValidationForField(f);

        assertEquals(List.of(
                String.format(AnnotationConstants.SIZE_MIN_MAX_ANNOTATION, 2, 10)
        ), result);
    }

    @Test
    void resolveValidationForField_stringAddsNotBlankNotEmptyEmail() {
        
        final ValidationDefinition validation = new ValidationDefinition();;
        validation.setNotBlank(true);
        validation.setNotEmpty(true);
        validation.setEmail(true);

        final FieldDefinition field = field("String", validation, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(
                AnnotationConstants.NOT_BLANK_ANNOTATION,
                AnnotationConstants.NOT_EMPTY_ANNOTATION,
                AnnotationConstants.EMAIL_ANNOTATION
        ), result);
    }

    @Test
    void resolveValidationForField_integerAddsMinAndMax() {
        
        final ValidationDefinition validation = new ValidationDefinition();;
        validation.setMin(new BigDecimal("1"));
        validation.setMax(new BigDecimal("99"));

        final FieldDefinition field = field("Integer", validation, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(
                String.format(AnnotationConstants.MIN_ANNOTATION, new BigDecimal("1").longValue()),
                String.format(AnnotationConstants.MAX_ANNOTATION, new BigDecimal("99").longValue())
        ), result);
    }

    @Test
    void resolveValidationForField_bigDecimalAddsDecimalMinAndDecimalMax() {
        
        final ValidationDefinition validation = new ValidationDefinition();
        validation.setMin(new BigDecimal("5.5"));
        validation.setMax(new BigDecimal("10.25"));

        final FieldDefinition field = field("BigDecimal", validation, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(
                String.format(AnnotationConstants.DECIMAL_MIN_ANNOTATION, new BigDecimal("5.5")),
                String.format(AnnotationConstants.DECIMAL_MAX_ANNOTATION, new BigDecimal("10.25"))
        ), result);
    }

    @Test
    void resolveValidationForField_collectionAddsNotEmptyAndSizeMinMax() {

        final ValidationDefinition validation = new ValidationDefinition();
        validation.setNotEmpty(true);
        validation.setMinItems(1);
        validation.setMaxItems(3);

        final FieldDefinition field = field(COLLECTION_TYPE, validation, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(
                AnnotationConstants.NOT_EMPTY_ANNOTATION,
                String.format(AnnotationConstants.SIZE_MIN_MAX_ANNOTATION, 1, 3)
        ), result);
    }

    @Test
    void resolveValidationForField_collectionAddsSizeMaxOnly() {
        
        final ValidationDefinition validation = new ValidationDefinition();
        validation.setNotEmpty(false);
        validation.setMinItems(null);
        validation.setMaxItems(5);

        final FieldDefinition field = field(COLLECTION_TYPE, validation, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(
                String.format(AnnotationConstants.SIZE_MAX_ANNOTATION, 5)
        ), result);
    }

}
