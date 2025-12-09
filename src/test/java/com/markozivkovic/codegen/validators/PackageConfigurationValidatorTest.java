package com.markozivkovic.codegen.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.PackageConfiguration;

class PackageConfigurationValidatorTest {

    @Test
    @DisplayName("Should do nothing when package configuration is null (use defaults)")
    void validate_nullPackageConfiguration_doesNothing() {
        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(true);
        configuration.setOpenApiCodegen(true);

        assertDoesNotThrow(() ->
                PackageConfigurationValidator.validate(null, configuration)
        );
    }

    @Test
    @DisplayName("Should allow all groups undefined and features disabled (use defaults)")
    void validate_noGroupsDefinedAndFeaturesDisabled_doesNothing() {
        
        final PackageConfiguration pkg = new PackageConfiguration();

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(false);
        configuration.setOpenApiCodegen(false);

        assertDoesNotThrow(() ->
                PackageConfigurationValidator.validate(pkg, configuration)
        );
    }

    @Test
    @DisplayName("Should report all missing groups when any group is defined")
    void validate_someGroupsDefined_reportsMissingOthers() {
        
        final PackageConfiguration pkg = new PackageConfiguration();
        pkg.setBusinessservices("com.example.business");

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(false);
        configuration.setOpenApiCodegen(false);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PackageConfigurationValidator.validate(pkg, configuration));

        final String msg = ex.getMessage();
        assertTrue(msg.contains("Invalid package configuration. Missing required package(s):"));
        assertTrue(msg.contains("controllers"));
        assertTrue(msg.contains("enums"));
        assertTrue(msg.contains("exceptions"));
        assertTrue(msg.contains("mappers"));
        assertTrue(msg.contains("models"));
        assertTrue(msg.contains("repositories"));
        assertTrue(msg.contains("services"));
        assertTrue(msg.contains("transferobjects"));
    }

    @Test
    @DisplayName("Should not throw when all groups are defined and features disabled")
    void validate_allGroupsDefined_featuresDisabled_doesNothing() {
        
        final PackageConfiguration pkg = new PackageConfiguration();
        pkg.setBusinessservices("com.example.business");
        pkg.setControllers("com.example.controller");
        pkg.setEnums("com.example.enums");
        pkg.setExceptions("com.example.exception");
        pkg.setMappers("com.example.mapper");
        pkg.setModels("com.example.model");
        pkg.setRepositories("com.example.repository");
        pkg.setServices("com.example.service");
        pkg.setTransferobjects("com.example.to");

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(false);
        configuration.setOpenApiCodegen(false);

        assertDoesNotThrow(() ->
                PackageConfigurationValidator.validate(pkg, configuration)
        );
    }

    @Test
    @DisplayName("Should require resolvers when graphQl is enabled")
    void validate_graphQlEnabled_missingResolvers_throwsIllegalArgumentException() {
        
        final PackageConfiguration pkg = new PackageConfiguration();

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(true);
        configuration.setOpenApiCodegen(false);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PackageConfigurationValidator.validate(pkg, configuration));

        assertTrue(ex.getMessage().contains("resolvers (required when graphQl is enabled)"));
    }

    @Test
    @DisplayName("Should require generated when openApiCodegen is enabled")
    void validate_openApiEnabled_missingGenerated_throwsIllegalArgumentException() {
        
        final PackageConfiguration pkg = new PackageConfiguration();

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(false);
        configuration.setOpenApiCodegen(true);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PackageConfigurationValidator.validate(pkg, configuration));

        assertTrue(ex.getMessage().contains("generated (required when openApiCodegen is enabled)"));
    }

    @Test
    @DisplayName("Should aggregate missing groups, resolvers and generated when multiple are missing")
    void validate_multipleMissingRequirements_aggregatedInMessage() {
        
        final PackageConfiguration pkg = new PackageConfiguration();
        pkg.setBusinessservices("com.example.business");

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(true);
        configuration.setOpenApiCodegen(true);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PackageConfigurationValidator.validate(pkg, configuration));

        final String msg = ex.getMessage();
        assertTrue(msg.contains("controllers"));
        assertTrue(msg.contains("enums"));
        assertTrue(msg.contains("exceptions"));
        assertTrue(msg.contains("mappers"));
        assertTrue(msg.contains("models"));
        assertTrue(msg.contains("repositories"));
        assertTrue(msg.contains("services"));
        assertTrue(msg.contains("transferobjects"));
        assertTrue(msg.contains("resolvers (required when graphQl is enabled)"));
        assertTrue(msg.contains("generated (required when openApiCodegen is enabled)"));
    }

    @Test
    @DisplayName("Should not throw when all groups, resolvers and generated are defined for enabled features")
    void validate_allGroupsAndFeaturePackagesDefined_doesNothing() {
        
        final PackageConfiguration pkg = new PackageConfiguration();
        pkg.setBusinessservices("com.example.business");
        pkg.setControllers("com.example.controller");
        pkg.setEnums("com.example.enums");
        pkg.setExceptions("com.example.exception");
        pkg.setMappers("com.example.mapper");
        pkg.setModels("com.example.model");
        pkg.setRepositories("com.example.repository");
        pkg.setServices("com.example.service");
        pkg.setTransferobjects("com.example.to");
        pkg.setResolvers("com.example.graphql.resolver");
        pkg.setGenerated("com.example.generated");

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(true);
        configuration.setOpenApiCodegen(true);

        assertDoesNotThrow(() ->
                PackageConfigurationValidator.validate(pkg, configuration)
        );
    }

    @Test
    @DisplayName("Should allow only resolvers/generated defined when groups are not defined")
    void validate_onlyFeaturePackagesDefinedWithoutGroups_doesNothing() {
        
        final PackageConfiguration pkg = new PackageConfiguration();
        pkg.setResolvers("com.example.graphql.resolver");
        pkg.setGenerated("com.example.generated");

        final CrudConfiguration configuration = new CrudConfiguration();
        configuration.setGraphQl(true);
        configuration.setOpenApiCodegen(true);

        assertDoesNotThrow(() ->
                PackageConfigurationValidator.validate(pkg, configuration)
        );
    }

}
