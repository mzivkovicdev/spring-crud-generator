package com.markozivkovic.codegen.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudSpecification;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.IdDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.IdDefinition.IdStrategyEnum;

class SpecificationValidatorTest {

    private CrudSpecification buildValidSpecification() {
        CrudSpecification spec = new CrudSpecification();

        CrudConfiguration config = new CrudConfiguration();
        config.setJavaVersion(17);
        config.setDocker(null);
        config.setCache(null);
        spec.setConfiguration(config);

        ModelDefinition user = new ModelDefinition();
        user.setName("User");
        user.setStorageName("user");
        
        FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        idField.setType("Long");
        idField.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        user.setFields(List.of(idField));

        spec.setEntities(List.of(user));

        return spec;
    }

    @Test
    @DisplayName("Should throw when specification is null")
    void validate_nullSpecification_throwsIllegalArgumentException() {
        
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(null));
        assertTrue(ex.getMessage().contains("CRUD specification, configuration and entities must not be null"));
    }

    @Test
    @DisplayName("Should throw when configuration is null")
    void validate_nullConfiguration_throwsIllegalArgumentException() {
        
        final CrudSpecification spec = new CrudSpecification();
        spec.setConfiguration(null);
        spec.setEntities(Collections.emptyList());

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
                assertTrue(ex.getMessage().contains("CRUD specification, configuration and entities must not be null"));
    }

    @Test
    @DisplayName("Should throw when entities list is null or empty")
    void validate_emptyEntities_throwsIllegalArgumentException() {
        
        final CrudSpecification spec = new CrudSpecification();
        final CrudConfiguration config = new CrudConfiguration();

        config.setJavaVersion(17);
        spec.setConfiguration(config);
        spec.setEntities(null);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
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
    @DisplayName("Should throw when Java version is below minimum supported")
    void validate_javaVersionTooLow_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setJavaVersion(16);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
        assertTrue(ex.getMessage().contains("Minimum supported version is 17"));
    }

    @Test
    @DisplayName("Should throw when Java version is above maximum supported")
    void validate_javaVersionTooHigh_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        spec.getConfiguration().setJavaVersion(26);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
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
        final ModelDefinition model = spec.getEntities().get(0);
        model.setName("   ");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        assertTrue(ex.getMessage().contains("Model name must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when storageName is blank for non-JSON model")
    void validate_modelStorageNameBlank_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);
        model.setStorageName("  ");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
        assertTrue(ex.getMessage().contains("Model storage name must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when storageName does not match lower_snake_case")
    void validate_modelStorageNameInvalidPattern_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);
        model.setStorageName("UserTable");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
        assertTrue(ex.getMessage().contains("Invalid storageName"));
        assertTrue(ex.getMessage().contains("lower_snake_case"));
    }

    @Test
    @DisplayName("Should throw when model has no fields")
    void validate_modelWithoutFields_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);
        model.setFields(Collections.emptyList());

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
        assertTrue(ex.getMessage().contains("must have at least one field defined"));
    }

    @Test
    @DisplayName("Should throw when model has no id field")
    void validate_modelWithoutIdField_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = new FieldDefinition();
        field.setName("name");
        field.setType("string");

        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        assertTrue(ex.getMessage().contains("must have id field defined"));
    }

    @Test
    @DisplayName("Should throw when model has multiple id fields")
    void validate_modelWithMultipleIdFields_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition id1 = new FieldDefinition();
        id1.setName("id1");
        id1.setType("Long");
        id1.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        final FieldDefinition id2 = new FieldDefinition();
        id2.setName("id2");
        id2.setType("Integer");
        id2.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        model.setFields(List.of(id1, id2));

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        
        assertTrue(ex.getMessage().contains("must have only one id field defined"));
    }

    @Test
    @DisplayName("Should throw when field name is blank")
    void validate_fieldNameBlank_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = new FieldDefinition();
        field.setName("   ");
        field.setType("string");
        field.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));

        assertTrue(ex.getMessage().contains("Field name in model"));
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when field name is duplicated within model")
    void validate_duplicateFieldNames_throwsIllegalArgumentException() {
        
        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        idField.setType("Long");
        idField.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        final FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");
        nameField.setType("string");

        final FieldDefinition duplicatedNamefield = new FieldDefinition();
        duplicatedNamefield.setName("name");
        duplicatedNamefield.setType("string");

        model.setFields(List.of(idField, nameField, duplicatedNamefield));

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        assertTrue(ex.getMessage().contains("is duplicated"));
    }

    @Test
    @DisplayName("Should throw when field type is blank")
    void validate_fieldTypeBlank_throwsIllegalArgumentException() {
        
        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = new FieldDefinition();
        field.setName("id");
        field.setType("  ");
        field.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
        assertTrue(ex.getMessage().contains("Field type for field"));
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }

    @Test
    @DisplayName("Should throw when field type is invalid (not basic, not special, not model reference)")
    void validate_invalidFieldType_throwsIllegalArgumentException() {
        
        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition model = spec.getEntities().get(0);

        final FieldDefinition field = new FieldDefinition();
        field.setName("id");
        field.setType("UnknownType");
        field.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        model.setFields(List.of(field));

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec));
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

        final FieldDefinition addressId = new FieldDefinition();
        addressId.setName("id");
        addressId.setType("string");
        addressId.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));

        address.setFields(List.of(addressId));

        final FieldDefinition addressField = new FieldDefinition();
        addressField.setName("address");
        addressField.setType("Address");
        addressField.setId(null);

        user.setFields(List.of(
                user.getFields().get(0),
                addressField
        ));

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

        assertTrue(ex.getMessage().contains("Inner type Address"), "Message should mention inner type");
        assertTrue(ex.getMessage().contains("collection field addresses"), "Message should mention field name");
        assertTrue(ex.getMessage().contains("must be a basic type"), "Message should mention basic types constraint");
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
    @DisplayName("Should throw when collection type has non-basic inner type even if model exists")
    void validate_simpleCollection_innerTypeModelReferenceStillInvalid_throwsIllegalArgumentException() {

        final CrudSpecification spec = buildValidSpecification();
        final ModelDefinition address = new ModelDefinition();
        address.setName("Address");
        address.setStorageName("address");

        final FieldDefinition addressId = new FieldDefinition();
        addressId.setName("id");
        addressId.setType("Long");
        addressId.setId(new IdDefinition().setStrategy(IdStrategyEnum.IDENTITY));
        address.setFields(List.of(addressId));

        final ModelDefinition user = spec.getEntities().get(0);
        final FieldDefinition userId = user.getFields().get(0);

        final FieldDefinition addresses = new FieldDefinition();
        addresses.setName("addresses");
        addresses.setType("List<Address>");

        user.setFields(List.of(userId, addresses));
        spec.setEntities(List.of(user, address));

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SpecificationValidator.validate(spec)
        );

        assertTrue(ex.getMessage().contains("Inner type Address"));
        assertTrue(ex.getMessage().contains("must be a basic type"));
    }

}
