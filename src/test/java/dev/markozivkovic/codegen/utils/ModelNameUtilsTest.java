package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModelNameUtilsTest {

    @Test
    @DisplayName("stripSuffix removes known suffixes in a case-insensitive way")
    void stripSuffix_shouldRemoveKnownSuffixes_caseInsensitive() {

        assertEquals("User", ModelNameUtils.stripSuffix("UserEntity"));
        assertEquals("Order", ModelNameUtils.stripSuffix("OrderModel"));
        assertEquals("Product", ModelNameUtils.stripSuffix("ProductTable"));
        assertEquals("Customer", ModelNameUtils.stripSuffix("CustomerJpaEntity"));
        assertEquals("Invoice", ModelNameUtils.stripSuffix("InvoiceDomain"));
        assertEquals("Account", ModelNameUtils.stripSuffix("AccountDAO"));
        assertEquals("Profile", ModelNameUtils.stripSuffix("ProfileData"));
        assertEquals("User", ModelNameUtils.stripSuffix("Userentity"));
        assertEquals("Order", ModelNameUtils.stripSuffix("OrderMODEL"));
    }

    @Test
    @DisplayName("stripSuffix returns original name when it does not end with any known suffix")
    void stripSuffix_shouldReturnOriginal_whenNoSuffixMatches() {

        assertEquals("User", ModelNameUtils.stripSuffix("User"));
        assertEquals("OrderDto", ModelNameUtils.stripSuffix("OrderDto"));
    }

    @Test
    @DisplayName("stripSuffix can return empty string when name equals suffix ignoring case")
    void stripSuffix_canReturnEmpty_whenNameIsOnlySuffix() {

        assertEquals("", ModelNameUtils.stripSuffix("Entity"));
        assertEquals("", ModelNameUtils.stripSuffix("entity"));
        assertEquals("", ModelNameUtils.stripSuffix("MODEL"));
    }

    @Test
    @DisplayName("computeOpenApiModelName strips suffix and appends Payload")
    void computeOpenApiModelName_shouldStripSuffixAndAppendPayload() {

        assertEquals("UserPayload", ModelNameUtils.computeOpenApiModelName("UserEntity"));
        assertEquals("OrderPayload", ModelNameUtils.computeOpenApiModelName("OrderModel"));
        assertEquals("ProductPayload", ModelNameUtils.computeOpenApiModelName("ProductTable"));
    }

    @Test
    @DisplayName("computeOpenApiModelName appends Payload when no suffix matches")
    void computeOpenApiModelName_shouldAppendPayload_whenNoSuffix() {

        assertEquals("UserPayload", ModelNameUtils.computeOpenApiModelName("User"));
        assertEquals("OrderDtoPayload", ModelNameUtils.computeOpenApiModelName("OrderDto"));
    }

    @Test
    @DisplayName("computeOpenApiModelName works when name is exactly a suffix and becomes Payload-only prefix")
    void computeOpenApiModelName_shouldHandleNameEqualToSuffix() {

        assertEquals("Payload", ModelNameUtils.computeOpenApiModelName("Entity"));
    }

    @Test
    @DisplayName("computeOpenApiInputModelName strips suffix and appends Input")
    void computeOpenApiInputModelName_shouldStripSuffixAndAppendInput() {

        assertEquals("UserInput", ModelNameUtils.computeOpenApiInputModelName("UserEntity"));
        assertEquals("OrderInput", ModelNameUtils.computeOpenApiInputModelName("OrderModel"));
        assertEquals("ProductInput", ModelNameUtils.computeOpenApiInputModelName("ProductTable"));
    }

    @Test
    @DisplayName("computeOpenApiInputModelName appends Input when no suffix matches")
    void computeOpenApiInputModelName_shouldAppendInput_whenNoSuffix() {

        assertEquals("UserInput", ModelNameUtils.computeOpenApiInputModelName("User"));
        assertEquals("OrderDtoInput", ModelNameUtils.computeOpenApiInputModelName("OrderDto"));
    }

    @Test
    @DisplayName("computeOpenApiInputModelName works when name is exactly a suffix and becomes Input-only prefix")
    void computeOpenApiInputModelName_shouldHandleNameEqualToSuffix() {

        assertEquals("Input", ModelNameUtils.computeOpenApiInputModelName("Entity"));
    }

    @Test
    @DisplayName("computeOpenApiCreateModelName strips suffix and appends CreatePayload")
    void computeOpenApiCreateModelName_shouldStripSuffixAndAppendCreatePayload() {

        assertEquals("UserCreatePayload", ModelNameUtils.computeOpenApiCreateModelName("UserEntity"));
        assertEquals("OrderCreatePayload", ModelNameUtils.computeOpenApiCreateModelName("OrderModel"));
        assertEquals("ProductCreatePayload", ModelNameUtils.computeOpenApiCreateModelName("ProductTable"));
    }

    @Test
    @DisplayName("computeOpenApiCreateModelName appends CreatePayload when no suffix matches")
    void computeOpenApiCreateModelName_shouldAppendCreatePayload_whenNoSuffix() {

        assertEquals("UserCreatePayload", ModelNameUtils.computeOpenApiCreateModelName("User"));
        assertEquals("OrderDtoCreatePayload", ModelNameUtils.computeOpenApiCreateModelName("OrderDto"));
    }

    @Test
    @DisplayName("computeOpenApiCreateModelName works when name is exactly a suffix and becomes CreatePayload-only prefix")
    void computeOpenApiCreateModelName_shouldHandleNameEqualToSuffix() {

        assertEquals("CreatePayload", ModelNameUtils.computeOpenApiCreateModelName("Entity"));
    }

    @Test
    @DisplayName("computeOpenApiUpdateModelName strips suffix and appends UpdatePayload")
    void computeOpenApiUpdateModelName_shouldStripSuffixAndAppendUpdatePayload() {

        assertEquals("UserUpdatePayload", ModelNameUtils.computeOpenApiUpdateModelName("UserEntity"));
        assertEquals("OrderUpdatePayload", ModelNameUtils.computeOpenApiUpdateModelName("OrderModel"));
        assertEquals("ProductUpdatePayload", ModelNameUtils.computeOpenApiUpdateModelName("ProductTable"));
    }

    @Test
    @DisplayName("computeOpenApiUpdateModelName appends UpdatePayload when no suffix matches")
    void computeOpenApiUpdateModelName_shouldAppendUpdatePayload_whenNoSuffix() {

        assertEquals("UserUpdatePayload", ModelNameUtils.computeOpenApiUpdateModelName("User"));
        assertEquals("OrderDtoUpdatePayload", ModelNameUtils.computeOpenApiUpdateModelName("OrderDto"));
    }

    @Test
    @DisplayName("computeOpenApiUpdateModelName works when name is exactly a suffix and becomes UpdatePayload-only prefix")
    void computeOpenApiUpdateModelName_shouldHandleNameEqualToSuffix() {

        assertEquals("UpdatePayload", ModelNameUtils.computeOpenApiUpdateModelName("Entity"));
    }

    @Test
    @DisplayName("toSnakeCase converts simple camelCase to snake_case")
    void toSnakeCase_shouldConvertSimpleCamelCase() {

        assertEquals("user", ModelNameUtils.toSnakeCase("user"));
        assertEquals("user_name", ModelNameUtils.toSnakeCase("userName"));
        assertEquals("first_name", ModelNameUtils.toSnakeCase("firstName"));
    }

    @Test
    @DisplayName("toSnakeCase converts PascalCase to snake_case")
    void toSnakeCase_shouldConvertPascalCase() {

        assertEquals("user", ModelNameUtils.toSnakeCase("User"));
        assertEquals("user_name", ModelNameUtils.toSnakeCase("UserName"));
        assertEquals("order_item", ModelNameUtils.toSnakeCase("OrderItem"));
    }

    @Test
    @DisplayName("toSnakeCase keeps existing underscores and only splits camel-case boundaries")
    void toSnakeCase_shouldPreserveExistingUnderscores() {

        assertEquals("user_name", ModelNameUtils.toSnakeCase("user_name"));
        assertEquals("user_name_extra", ModelNameUtils.toSnakeCase("userNameExtra"));
        assertEquals("user_name_extra", ModelNameUtils.toSnakeCase("user_nameExtra"));
    }

    @Test
    @DisplayName("toSnakeCase handles sequences with multiple upper-case characters reasonably")
    void toSnakeCase_shouldHandleMultipleUppercaseSequences() {

        assertEquals("user_url", ModelNameUtils.toSnakeCase("userURL"));
        assertEquals("api_v1", ModelNameUtils.toSnakeCase("ApiV1"));
    }
}
