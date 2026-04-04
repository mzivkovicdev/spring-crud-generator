package dev.markozivkovic.springcrudgenerator.resolvers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
    void resolveValidationForField_stringAddsPattern() {
        
        final ValidationDefinition validation = new ValidationDefinition();;
        final String pattern = "^[a-zA-Z0-9_]+$";
        validation.setPattern(pattern);

        final FieldDefinition field = field("String", validation, null);

        final List<String> result = FieldValidationResolver.resolveValidationForField(field);

        assertEquals(List.of(String.format(AnnotationConstants.PATTERN_ANNOTATION, pattern)), result);
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

    @Test
    @DisplayName("hasAnyColumnValidation returns false when no field has column validations")
    void hasAnyColumnValidation_shouldReturnFalse_whenNoValidations() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(null);

        final ColumnDefinition col = column(true, null);
        final FieldDefinition f2 = fieldWithNameAndType("age", "Integer");
        f2.setColumn(col);

        final List<FieldDefinition> fields = List.of(f1, f2);

        final boolean result = FieldValidationResolver.hasAnyColumnValidation(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("hasAnyColumnValidation returns true when any field has nullable=false")
    void hasAnyColumnValidation_shouldReturnTrue_whenAnyFieldNonNullable() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(column(false, null));

        final List<FieldDefinition> fields = List.of(f1);

        final boolean result = FieldValidationResolver.hasAnyColumnValidation(fields);

        assertTrue(result);
    }

    @Test
    @DisplayName("hasAnyColumnValidation returns true when any field has length specified")
    void hasAnyColumnValidation_shouldReturnTrue_whenAnyFieldHasLength() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(column(true, 100));

        final List<FieldDefinition> fields = List.of(f1);

        final boolean result = FieldValidationResolver.hasAnyColumnValidation(fields);

        assertTrue(result);
    }

    @Test
    void hasAnyFieldValidation_shouldReturnFalse_forEmptyList() {
        assertFalse(FieldValidationResolver.hasAnyFieldValidation(List.of()));
    }

    @Test
    void hasAnyFieldValidation_shouldReturnFalse_whenAllFieldsHaveNullValidation() {

        final List<FieldDefinition> fields = List.of(
                fieldWithValidation(null),
                fieldWithValidation(null)
        );

        assertFalse(FieldValidationResolver.hasAnyFieldValidation(fields));
    }

    @Test
    void hasAnyFieldValidation_shouldReturnFalse_whenValidationObjectExistsButAllFlagsNullOrFalse() {
        final ValidationDefinition v = new ValidationDefinition();
        v.setRequired(false);
        v.setNotBlank(false);
        v.setNotEmpty(false);
        v.setEmail(false);

        final List<FieldDefinition> fields = List.of(fieldWithValidation(v));

        assertFalse(FieldValidationResolver.hasAnyFieldValidation(fields));
    }

    private static Stream<Consumer<ValidationDefinition>> anySingleValidationEnabled() {
        return Stream.of(
                v -> v.setRequired(true),
                v -> v.setNotBlank(true),
                v -> v.setNotEmpty(true),
                v -> v.setMinLength(1),
                v -> v.setMaxLength(10),
                v -> v.setMin(BigDecimal.ONE),
                v -> v.setMax(new BigDecimal("999")),
                v -> v.setMinItems(1),
                v -> v.setMaxItems(5),
                v -> v.setEmail(true)
        );
    }

    @ParameterizedTest
    @MethodSource("anySingleValidationEnabled")
    void hasAnyFieldValidation_shouldReturnTrue_whenAnyValidationIsDefined(final Consumer<ValidationDefinition> enable) {
        final ValidationDefinition v = new ValidationDefinition();
        enable.accept(v);

        List<FieldDefinition> fields = List.of(fieldWithValidation(v));

        assertTrue(FieldValidationResolver.hasAnyFieldValidation(fields));
    }

    @Test
    void hasAnyFieldValidation_shouldReturnTrue_whenOnlyOneFieldAmongManyHasValidation() {

        final FieldDefinition noVal1 = fieldWithValidation(null);
        final FieldDefinition noVal2 = fieldWithValidation(null);

        final ValidationDefinition v = new ValidationDefinition();
        v.setMaxLength(250);
        final FieldDefinition withVal = fieldWithValidation(v);

        final List<FieldDefinition> fields = List.of(noVal1, noVal2, withVal);

        assertTrue(FieldValidationResolver.hasAnyFieldValidation(fields));
    }

    @Test
    @DisplayName("hasAnyFieldLengthValidation returns false when no field has length defined")
    void hasAnyFieldLengthValidation_shouldReturnFalse_whenNoLengthDefined() {

        final FieldDefinition f1 = fieldWithNameAndType("name", "String");
        f1.setColumn(null);

        final FieldDefinition f2 = fieldWithNameAndType("description", "String");
        f2.setColumn(column(false, null));

        final List<FieldDefinition> fields = List.of(f1, f2);

        final boolean result = FieldValidationResolver.hasAnyFieldLengthValidation(fields);

        assertFalse(result);
    }

    @Test
    @DisplayName("hasAnyFieldLengthValidation returns true when at least one field has length defined")
    void hasAnyFieldLengthValidation_shouldReturnTrue_whenAnyFieldHasLength() {

        final FieldDefinition f1 = fieldWithNameAndType("description", "String");
        f1.setColumn(column(true, 255));

        final List<FieldDefinition> fields = List.of(f1);

        final boolean result = FieldValidationResolver.hasAnyFieldLengthValidation(fields);

        assertTrue(result);
    }

    private static FieldDefinition fieldWithNameAndType(final String name, final String type) {
        final FieldDefinition f = new FieldDefinition();
        f.setName(name);
        f.setType(type);
        return f;
    }

    private static FieldDefinition fieldWithValidation(final ValidationDefinition validation) {
        final FieldDefinition f = new FieldDefinition();
        f.setName("field");
        f.setType("String");
        f.setValidation(validation);
        return f;
    }

}
