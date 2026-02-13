package dev.markozivkovic.springcrudgenerator.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.CacheConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.TestConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudSpecification;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ValidationDefinition;

class SpecificationValidatorTest {

    private CrudSpecification buildValidSpecification() {
        final CrudSpecification spec = new CrudSpecification();

        final CrudConfiguration config = new CrudConfiguration();
        config.setJavaVersion(17);
        config.setDocker(null);
        config.setCache(null);
        config.setDatabase(DatabaseType.MYSQL);
        spec.setConfiguration(config);

        final ModelDefinition user = new ModelDefinition();
        user.setName("User");
        user.setStorageName("user");

        final FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        idField.setType("Long");
        idField.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        final FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");
        nameField.setType("String");
        nameField.setId(null);
        nameField.setValidation(new ValidationDefinition().setPattern("^[A-Za-z]+$"));

        user.setFields(new ArrayList<>(List.of(idField, nameField)));
        spec.setEntities(new ArrayList<>(List.of(user)));

        return spec;
    }

    private static FieldDefinition newIdField(final String name, final String type) {
        final FieldDefinition id = new FieldDefinition();
        id.setName(name);
        id.setType(type);
        id.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));
        return id;
    }

    @Test
    @DisplayName("Should throw when specification is null")
    void validate_nullSpecification_throwsIllegalArgumentException() {

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(null)
        );
        assertTrue(ex.getMessage().contains("CRUD specification, configuration and entities must not be null"));
    }

    @Test
    @DisplayName("Should throw when configuration is null")
    void validate_nullConfiguration_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.setConfiguration(null);

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("CRUD specification, configuration and entities must not be null"));
    }

    @Test
    @DisplayName("Should throw when database is null")
    void validate_databaseNull_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setDatabase(null);

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Database must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when entities list is null or empty")
    void validate_emptyEntities_throwsIllegalArgumentException() {
        
        final CrudSpecification spec = new CrudSpecification();
        final CrudConfiguration config = new CrudConfiguration();
        config.setJavaVersion(17);
        config.setDatabase(DatabaseType.MYSQL);

        spec.setConfiguration(config);
        spec.setEntities(null);

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("CRUD specification, configuration and entities must not be null"));
    }

    @Test
    @DisplayName("Should allow null Java version and proceed with validation")
    void validate_nullJavaVersion_allowed() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setJavaVersion(null);

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    void validate_invalidRegexPattern_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();

        final FieldDefinition invalidField = new FieldDefinition();
        invalidField.setName("description");
        invalidField.setType("String");
        invalidField.setId(null);
        invalidField.setValidation(new ValidationDefinition().setPattern("[abc"));

        spec.getEntities().get(0).getFields().add(invalidField);

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Regex pattern"));
    }

    @Test
    @DisplayName("Should throw when Java version is below minimum supported")
    void validate_javaVersionTooLow_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setJavaVersion(16);

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Minimum supported version is 17"));
    }

    @Test
    @DisplayName("Should throw when Java version is above maximum supported")
    void validate_javaVersionTooHigh_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setJavaVersion(26);

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Maximum supported version is 25"));
    }

    @Test
    @DisplayName("Should allow Java version at lower bound (17)")
    void validate_javaVersionAtLowerBound_ok() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setJavaVersion(17);

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should allow Java version at upper bound (25)")
    void validate_javaVersionAtUpperBound_ok() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setJavaVersion(25);

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should throw when model name is blank")
    void validate_modelNameBlank_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getEntities().get(0).setName("   ");

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Model name must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when storageName is blank for non-JSON model")
    void validate_modelStorageNameBlank_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getEntities().get(0).setStorageName("  ");

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Model storage name must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when storageName does not match lower_snake_case")
    void validate_modelStorageNameInvalidPattern_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getEntities().get(0).setStorageName("UserTable");

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Invalid storageName"));
        assertTrue(ex.getMessage().contains("lower_snake_case"));
    }

    @Test
    @DisplayName("Should throw when model has no fields")
    void validate_modelWithoutFields_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getEntities().get(0).setFields(Collections.emptyList());

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("must have at least one field defined"));
    }

    @Test
    @DisplayName("Should throw when model has no id field")
    void validate_modelWithoutIdField_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = new FieldDefinition();
        field.setName("name");
        field.setType("String");
        field.setId(null);

        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("must have id field defined"));
    }

    @Test
    @DisplayName("Should throw when model has multiple id fields")
    void validate_modelWithMultipleIdFields_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id1 = newIdField("id1", "Long");
        final FieldDefinition id2 = newIdField("id2", "Integer");

        model.setFields(List.of(id1, id2));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("must have only one id field defined"));
    }

    @Test
    @DisplayName("Should throw when field name is blank")
    void validate_fieldNameBlank_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = new FieldDefinition();
        field.setName("   ");
        field.setType("String");
        field.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Field name in model"));
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when field name is duplicated within model")
    void validate_duplicateFieldNames_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition idField = newIdField("id", "Long");

        final FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");
        nameField.setType("String");

        final FieldDefinition duplicatedNamefield = new FieldDefinition();
        duplicatedNamefield.setName("name");
        duplicatedNamefield.setType("String");

        model.setFields(List.of(idField, nameField, duplicatedNamefield));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("is duplicated"));
    }

    @Test
    @DisplayName("Should throw when field type is blank")
    void validate_fieldTypeBlank_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = newIdField("id", "  ");
        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Field type for field"));
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when field type is invalid (not basic, not special, not model reference)")
    void validate_invalidFieldType_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = newIdField("id", "UnknownType");
        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Field type UnknownType"));
        assertTrue(ex.getMessage().contains("is invalid"));
    }

    @Test
    @DisplayName("Should allow field type referencing another model")
    void validate_fieldTypeModelReference_ok() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition user = spec.getEntities().get(0);

        final ModelDefinition address = new ModelDefinition();
        address.setName("Address");
        address.setStorageName("address");
        address.setFields(List.of(newIdField("id", "String")));

        final FieldDefinition addressField = new FieldDefinition();
        addressField.setName("address");
        addressField.setType("Address");

        user.setFields(List.of(user.getFields().get(0), addressField));
        spec.setEntities(List.of(user, address));

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should accept fully valid specification")
    void validate_validSpecification_ok() {

        final CrudSpecification spec = buildValidSpecification();
        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should allow simple collection field with basic inner type (List<String>)")
    void validate_simpleCollection_listWithBasicInnerType_ok() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition phoneNumbers = new FieldDefinition();
        phoneNumbers.setName("phoneNumbers");
        phoneNumbers.setType("List<String>");

        model.setFields(List.of(id, phoneNumbers));

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should allow simple collection field with basic inner type (Set<Long>)")
    void validate_simpleCollection_setWithBasicInnerType_ok() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition tags = new FieldDefinition();
        tags.setName("tags");
        tags.setType("Set<Long>");

        model.setFields(List.of(id, tags));

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should throw when simple collection inner type is NOT basic (List<Address>)")
    void validate_simpleCollection_innerTypeNotBasic_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition invalidCollection = new FieldDefinition();
        invalidCollection.setName("addresses");
        invalidCollection.setType("List<Address>");

        model.setFields(List.of(id, invalidCollection));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Inner type Address"));
        assertTrue(ex.getMessage().contains("collection field addresses"));
        assertTrue(ex.getMessage().contains("must be a basic type"));
    }

    @Test
    @DisplayName("Should throw when simple collection inner type is another special type (Set<Enum>)")
    void validate_simpleCollection_innerTypeSpecialType_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition invalidCollection = new FieldDefinition();
        invalidCollection.setName("statuses");
        invalidCollection.setType("Set<Enum>");

        model.setFields(List.of(id, invalidCollection));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Inner type Enum"));
        assertTrue(ex.getMessage().contains("collection field statuses"));
    }

    @Test
    @DisplayName("Should allow JSON field with basic inner type (JSON<String>)")
    void validate_json_basicInner_ok() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition meta = new FieldDefinition();
        meta.setName("meta");
        meta.setType("JSON<String>");

        model.setFields(List.of(id, meta));

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should allow JSON field with model inner type (JSON<Address>)")
    void validate_json_modelInner_ok() {

        final CrudSpecification spec = buildValidSpecification();

        final ModelDefinition address = new ModelDefinition();
        address.setName("Address");
        address.setStorageName("address");
        address.setFields(List.of(newIdField("id", "Long")));

        final ModelDefinition user = spec.getEntities().get(0);
        final FieldDefinition userId = user.getFields().get(0);

        final FieldDefinition addressJson = new FieldDefinition();
        addressJson.setName("addressJson");
        addressJson.setType("JSON<Address>");

        user.setFields(List.of(userId, addressJson));
        spec.setEntities(List.of(user, address));

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should allow JSON field with collection inner type (JSON<List<String>>)")
    void validate_json_collectionInner_list_ok() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition tagsJson = new FieldDefinition();
        tagsJson.setName("tagsJson");
        tagsJson.setType("JSON<List<String>>");

        model.setFields(List.of(id, tagsJson));

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should allow JSON field with collection inner type (JSON<Set<Long>>)")
    void validate_jsonb_collectionInner_set_ok() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition numbersJson = new FieldDefinition();
        numbersJson.setName("numbersJson");
        numbersJson.setType("JSON<Set<Long>>");

        model.setFields(List.of(id, numbersJson));

        assertDoesNotThrow(() -> SpecificationValidator.validate(spec));
    }

    @Test
    @DisplayName("Should throw when JSON inner type is invalid (JSON<UnknownType>)")
    void validate_json_invalidInner_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition bad = new FieldDefinition();
        bad.setName("badJson");
        bad.setType("JSON<UnknownType>");

        model.setFields(List.of(id, bad));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Inner type UnknownType"));
        assertTrue(ex.getMessage().contains("JSON field User.badJson") || ex.getMessage().contains("field User.badJson"));
        assertTrue(ex.getMessage().contains("is invalid"));
    }

    @Test
    @DisplayName("Should throw when JSON has invalid format (cannot extract inner type)")
    void validate_json_invalidFormat_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id = model.getFields().get(0);

        final FieldDefinition bad = new FieldDefinition();
        bad.setName("badJson");
        bad.setType("JSON<>");

        model.setFields(List.of(id, bad));

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("has invalid inner type or invalid format"));
        assertTrue(ex.getMessage().contains("JSON field User.badJson"));
    }

    @Test
    @DisplayName("Should delegate configuration validation to Docker, Cache and Test validators")
    void validate_shouldDelegateToNestedConfigurationValidators() {

        final CrudSpecification spec = buildValidSpecification();
        final CrudConfiguration config = spec.getConfiguration();

        final DockerConfiguration dockerCfg = new DockerConfiguration();
        final CacheConfiguration cacheCfg = new CacheConfiguration();
        final TestConfiguration testCfg = new TestConfiguration();

        config.setDocker(dockerCfg);
        config.setCache(cacheCfg);
        config.setTests(testCfg);

        try (final MockedStatic<DockerConfigurationValidator> dockerMock = mockStatic(DockerConfigurationValidator.class);
             final MockedStatic<CacheConfigurationValidator> cacheMock = mockStatic(CacheConfigurationValidator.class);
             final MockedStatic<TestConfigurationValidator> testMock = mockStatic(TestConfigurationValidator.class)) {

            assertDoesNotThrow(() -> SpecificationValidator.validate(spec));

            dockerMock.verify(() -> DockerConfigurationValidator.validate(dockerCfg));
            cacheMock.verify(() -> CacheConfigurationValidator.validate(cacheCfg));
            testMock.verify(() -> TestConfigurationValidator.validate(testCfg));
        }
    }
}
