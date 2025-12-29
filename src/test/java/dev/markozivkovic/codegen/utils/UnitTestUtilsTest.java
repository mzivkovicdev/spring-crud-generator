package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.codegen.enums.SupportedIdTypeEnum;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.TestConfiguration.DataGeneratorEnum;
import dev.markozivkovic.codegen.models.FieldDefinition;

class UnitTestUtilsTest {

    @Test
    @DisplayName("computeInvalidIdType: numeric-like ID types should map to STRING invalid type")
    void computeInvalidIdType_numericLike_returnsStringKey() {
        final FieldDefinition idField = new FieldDefinition();
        idField.setType("Long");

        final String invalidType = UnitTestUtils.computeInvalidIdType(idField);

        assertEquals(SupportedIdTypeEnum.STRING.getKey(), invalidType);
    }

    @Test
    @DisplayName("computeInvalidIdType: STRING ID type should map to LONG invalid type")
    void computeInvalidIdType_string_returnsLongKey() {
        final FieldDefinition idField = new FieldDefinition();
        idField.setType("String");

        final String invalidType = UnitTestUtils.computeInvalidIdType(idField);

        assertEquals(SupportedIdTypeEnum.LONG.getKey(), invalidType);
    }

    @Test
    @DisplayName("computeInvalidIdType: unsupported ID type should throw IllegalArgumentException")
    void computeInvalidIdType_unsupportedType_throwsIllegalArgumentException() {
        final FieldDefinition idField = new FieldDefinition();
        idField.setType("Object");

        assertThrows(IllegalArgumentException.class,
                     () -> UnitTestUtils.computeInvalidIdType(idField));
    }

    @Test
    @DisplayName("isUnitTestsEnabled: should return false when configuration is null")
    void isUnitTestsEnabled_nullConfiguration_returnsFalse() {
        final boolean result = UnitTestUtils.isUnitTestsEnabled(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("isUnitTestsEnabled: should return false when tests configuration is null")
    void isUnitTestsEnabled_nullTestsConfig_returnsFalse() {
        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setTests(null);

        final boolean result = UnitTestUtils.isUnitTestsEnabled(configuration);

        assertFalse(result);
    }

    @Test
    @DisplayName("isUnitTestsEnabled: should return false when unit flag is null")
    void isUnitTestsEnabled_nullUnitFlag_returnsFalse() {
        final CrudConfiguration configuration = new CrudConfiguration();
        final TestConfiguration tests = new TestConfiguration();
        tests.setUnit(null);
        configuration.setTests(tests);

        final boolean result = UnitTestUtils.isUnitTestsEnabled(configuration);

        assertFalse(result);
    }

    @Test
    @DisplayName("isUnitTestsEnabled: should return false when unit flag is FALSE")
    void isUnitTestsEnabled_unitFlagFalse_returnsFalse() {
        final CrudConfiguration configuration = new CrudConfiguration();
        final TestConfiguration tests = new TestConfiguration();
        tests.setUnit(Boolean.FALSE);
        configuration.setTests(tests);

        final boolean result = UnitTestUtils.isUnitTestsEnabled(configuration);

        assertFalse(result);
    }

    @Test
    @DisplayName("isUnitTestsEnabled: should return true when unit flag is TRUE")
    void isUnitTestsEnabled_unitFlagTrue_returnsTrue() {
        final CrudConfiguration configuration = new CrudConfiguration();
        final TestConfiguration tests = new TestConfiguration();
        tests.setUnit(Boolean.TRUE);
        configuration.setTests(tests);

        final boolean result = UnitTestUtils.isUnitTestsEnabled(configuration);

        assertTrue(result);
    }

    @Test
    @DisplayName("isInstancioEnabled: should return false when configuration is null")
    void isInstancioEnabled_nullConfiguration_returnsFalse() {
        final boolean result = UnitTestUtils.isInstancioEnabled(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("isInstancioEnabled: should return false when tests configuration is null")
    void isInstancioEnabled_nullTestsConfig_returnsFalse() {
        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setTests(null);

        final boolean result = UnitTestUtils.isInstancioEnabled(configuration);

        assertFalse(result);
    }

    @Test
    @DisplayName("isInstancioEnabled: should return false when dataGenerator is null")
    void isInstancioEnabled_nullDataGenerator_returnsFalse() {
        final CrudConfiguration configuration = new CrudConfiguration();
        final TestConfiguration tests = new TestConfiguration();
        tests.setDataGenerator(null);
        configuration.setTests(tests);

        final boolean result = UnitTestUtils.isInstancioEnabled(configuration);

        assertFalse(result);
    }

    @Test
    @DisplayName("isInstancioEnabled: should return false when dataGenerator is not INSTANCIO")
    void isInstancioEnabled_nonInstancio_returnsFalse() {
        final CrudConfiguration configuration = new CrudConfiguration();
        final TestConfiguration tests = new TestConfiguration();
        tests.setDataGenerator(DataGeneratorEnum.PODAM);
        configuration.setTests(tests);

        final boolean result = UnitTestUtils.isInstancioEnabled(configuration);

        assertFalse(result);
    }

    @Test
    @DisplayName("isInstancioEnabled: should return true when dataGenerator is INSTANCIO")
    void isInstancioEnabled_instancio_returnsTrue() {
        final CrudConfiguration configuration = new CrudConfiguration();
        final TestConfiguration tests = new TestConfiguration();
        tests.setDataGenerator(DataGeneratorEnum.INSTANCIO);
        configuration.setTests(tests);

        final boolean result = UnitTestUtils.isInstancioEnabled(configuration);

        assertTrue(result);
    }

    @Test
    @DisplayName("resolveGeneratorConfig: INSTANCIO config should have correct values")
    void resolveGeneratorConfig_instancio_returnsCorrectConfig() {
        final UnitTestUtils.TestDataGeneratorConfig config =
                UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.INSTANCIO);

        assertEquals("INSTANCIO", config.generator());
        assertEquals("Instancio", config.randomFieldName());
        assertEquals("create", config.singleObjectMethodName());
        assertEquals("ofList", config.multipleObjectsMethodName());
    }

    @Test
    @DisplayName("resolveGeneratorConfig: PODAM config should have correct values")
    void resolveGeneratorConfig_podam_returnsCorrectConfig() {
        final UnitTestUtils.TestDataGeneratorConfig config =
                UnitTestUtils.resolveGeneratorConfig(DataGeneratorEnum.PODAM);

        assertEquals("PODAM", config.generator());
        assertEquals("PODAM_FACTORY", config.randomFieldName());
        assertEquals("manufacturePojo", config.singleObjectMethodName());
        assertEquals("manufacturePojo", config.multipleObjectsMethodName());
    }

}
