package dev.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.models.PackageConfiguration;

class PackageUtilsTest {

    @Test
    @DisplayName("Should throw IllegalArgumentException when outputDir is null")
    void getPackagePathFromOutputDir_whenNull_throwsIllegalArgumentException() {
        
        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PackageUtils.getPackagePathFromOutputDir(null)
        );

        assertEquals("Output directory cannot be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when outputDir is empty")
    void getPackagePathFromOutputDir_whenEmpty_throwsIllegalArgumentException() {
        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PackageUtils.getPackagePathFromOutputDir("")
        );

        assertEquals("Output directory cannot be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when outputDir is blank")
    void getPackagePathFromOutputDir_whenBlank_throwsIllegalArgumentException() {
        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PackageUtils.getPackagePathFromOutputDir("   ")
        );

        assertEquals("Output directory cannot be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when outputDir is not an absolute path")
    void getPackagePathFromOutputDir_whenNotAbsolute_throwsIllegalArgumentException() {
        final String relativePath = "project/src/main/java/com/example";

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PackageUtils.getPackagePathFromOutputDir(relativePath)
        );

        assertEquals("Output directory must be an absolute path", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when outputDir does not contain src/main/java/")
    void getPackagePathFromOutputDir_whenNoSourceJava_throwsIllegalArgumentException() {
        final String outputDir = "/home/user/project/target/generated-sources";

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PackageUtils.getPackagePathFromOutputDir(outputDir)
        );

        assertEquals(
            "Output directory '/home/user/project/target/generated-sources' " +
            "does not contain the source Java directory 'src/main/java/'",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("Should return valid package path for correct absolute path containing src/main/java/")
    void getPackagePathFromOutputDir_whenValid_returnsPackagePath() {
        final String outputDir = "/home/user/project/src/main/java/com/example/model";

        final String result = PackageUtils.getPackagePathFromOutputDir(outputDir);

        assertEquals("com.example.model", result);
    }

    @Test
    @DisplayName("Should return root package name when only one segment follows src/main/java/")
    void getPackagePathFromOutputDir_whenValidRootPackage_returnsSingleSegment() {
        final String outputDir = "/home/user/project/src/main/java/com";

        final String result = PackageUtils.getPackagePathFromOutputDir(outputDir);

        assertEquals("com", result);
    }

    @Test
    @DisplayName("join: should return empty string when all parts are null/blank")
    void join_allNullOrBlank_returnsEmptyString() {
        final String result = PackageUtils.join(null, "", "   ");

        assertEquals("", result);
    }

    @Test
    @DisplayName("join: should join non-blank parts with dots")
    void join_nonBlankParts_joinedWithDots() {
        final String result = PackageUtils.join("com", "example", "model");

        assertEquals("com.example.model", result);
    }

    @Test
    @DisplayName("join: should ignore null, empty and blank parts")
    void join_ignoresNullEmptyAndBlankParts() {
        final String result = PackageUtils.join("com", null, "", "   ", "example");

        assertEquals("com.example", result);
    }

    @Test
    @DisplayName("join: should trim each non-blank part before joining")
    void join_trimsPartsBeforeJoining() {
        final String result = PackageUtils.join("  com  ", "  example.model  ");

        assertEquals("com.example.model", result);
    }

    @Test
    @DisplayName("join: should return single trimmed part when only one non-blank part is provided")
    void join_singlePart_returnsTrimmedPart() {
        final String result = PackageUtils.join("  com.example  ");

        assertEquals("com.example", result);
    }

    @Test
    @DisplayName("computeConfigurationSubPackage: should return default when config is null")
    void computeConfigurationSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeConfigurationSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.CONFIGURATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeConfigurationSubPackage: should return user-defined configuration package when non-blank")
    void computeConfigurationSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setConfigurations("custom.config");

        final String result = PackageUtils.computeConfigurationSubPackage(config);

        assertEquals("custom.config", result);
    }

    @Test
    @DisplayName("computeConfigurationSubPackage: should return default when user-defined value is blank")
    void computeConfigurationSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setConfigurations("   ");

        final String result = PackageUtils.computeConfigurationSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.CONFIGURATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeConfigurationPackage: should join base package with user-defined configuration package")
    void computeConfigurationPackage_withUserDefinedConfig() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setConfigurations("config");

        final String result = PackageUtils.computeConfigurationPackage("com.example", config);

        assertEquals("com.example.config", result);
    }

    @Test
    @DisplayName("computeConfigurationPackage: should join base package with default configuration subpackage when user-defined is blank")
    void computeConfigurationPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setConfigurations("   ");

        final String result = PackageUtils.computeConfigurationPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.CONFIGURATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeConfigurationPackage: should use default configuration subpackage when config is null")
    void computeConfigurationPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeConfigurationPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.CONFIGURATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeConfigurationPackage: should return only subpackage when basePackage is null")
    void computeConfigurationPackage_nullBasePackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setConfigurations("config");

        final String result = PackageUtils.computeConfigurationPackage(null, config);

        assertEquals("config", result);
    }

    @Test
    @DisplayName("computeControllerSubPackage: should return default when config is null")
    void computeControllerSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeControllerSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.CONTROLLERS,
            result
        );
    }

    @Test
    @DisplayName("computeControllerSubPackage: should return user-defined controller package when non-blank")
    void computeControllerSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setControllers("custom.controller");

        final String result = PackageUtils.computeControllerSubPackage(config);

        assertEquals("custom.controller", result);
    }

    @Test
    @DisplayName("computeControllerSubPackage: should return default when user-defined controller value is blank")
    void computeControllerSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setControllers("   ");

        final String result = PackageUtils.computeControllerSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.CONTROLLERS,
            result
        );
    }

    @Test
    @DisplayName("computeControllerPackage: should join base package with user-defined controller package")
    void computeControllerPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setControllers("controller");

        final String result = PackageUtils.computeControllerPackage("com.example", config);

        assertEquals("com.example.controller", result);
    }

    @Test
    @DisplayName("computeControllerPackage: should join base package with default controller subpackage when user-defined is blank")
    void computeControllerPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setControllers("   ");

        final String result = PackageUtils.computeControllerPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.CONTROLLERS,
            result
        );
    }

    @Test
    @DisplayName("computeControllerPackage: should use default controller subpackage when config is null")
    void computeControllerPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeControllerPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.CONTROLLERS,
            result
        );
    }

    @Test
    @DisplayName("computeControllerPackage: should return only subpackage when basePackage is null")
    void computeControllerPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setControllers("controller");

        final String result = PackageUtils.computeControllerPackage(null, config);

        assertEquals("controller", result);
    }

    @Test
    @DisplayName("computeExceptionSubPackage: should return default when config is null")
    void computeExceptionSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeExceptionSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.EXCEPTIONS,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionSubPackage: should return user-defined exception package when non-blank")
    void computeExceptionSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("custom.exception");

        final String result = PackageUtils.computeExceptionSubPackage(config);

        assertEquals("custom.exception", result);
    }

    @Test
    @DisplayName("computeExceptionSubPackage: should return default when user-defined exception value is blank")
    void computeExceptionSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("   ");

        final String result = PackageUtils.computeExceptionSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.EXCEPTIONS,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionPackage: should join base package with user-defined exception package")
    void computeExceptionPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("exception");

        final String result = PackageUtils.computeExceptionPackage("com.example", config);

        assertEquals("com.example.exception", result);
    }

    @Test
    @DisplayName("computeExceptionPackage: should join base package with default exception subpackage when user-defined is blank")
    void computeExceptionPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("   ");

        final String result = PackageUtils.computeExceptionPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.EXCEPTIONS,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionPackage: should use default exception subpackage when config is null")
    void computeExceptionPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeExceptionPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.EXCEPTIONS,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionPackage: should return only subpackage when basePackage is null")
    void computeExceptionPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("exception");

        final String result = PackageUtils.computeExceptionPackage(null, config);

        assertEquals("exception", result);
    }

    @Test
    @DisplayName("computeExceptionResponseSubPackage: should return default EXCEPTIONS.RESPONSES when config is null")
    void computeExceptionResponseSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeExceptionResponseSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.EXCEPTIONS + "." + GeneratorConstants.DefaultPackageLayout.RESPONSES,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionResponseSubPackage: should return user-defined exceptions + responses when non-blank")
    void computeExceptionResponseSubPackage_nonBlank_returnsUserDefinedPlusResponses() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("custom.exceptions");

        final String result = PackageUtils.computeExceptionResponseSubPackage(config);

        assertEquals("custom.exceptions." + GeneratorConstants.DefaultPackageLayout.RESPONSES, result);
    }

    @Test
    @DisplayName("computeExceptionResponseSubPackage: should return default EXCEPTIONS.RESPONSES when user-defined exceptions is blank")
    void computeExceptionResponseSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("   ");

        final String result = PackageUtils.computeExceptionResponseSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.EXCEPTIONS + "." + GeneratorConstants.DefaultPackageLayout.RESPONSES,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionResponsePackage: should join base package with computed exception response subpackage")
    void computeExceptionResponsePackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("custom.exceptions");

        final String result = PackageUtils.computeExceptionResponsePackage("com.example", config);

        assertEquals("com.example.custom.exceptions." + GeneratorConstants.DefaultPackageLayout.RESPONSES, result);
    }

    @Test
    @DisplayName("computeExceptionResponsePackage: should join base package with default subpackage when user-defined is blank")
    void computeExceptionResponsePackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("");

        final String result = PackageUtils.computeExceptionResponsePackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.EXCEPTIONS + "." + GeneratorConstants.DefaultPackageLayout.RESPONSES,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionResponsePackage: should return only subpackage when basePackage is null")
    void computeExceptionResponsePackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("custom.exceptions");

        final String result = PackageUtils.computeExceptionResponsePackage(null, config);

        assertEquals("custom.exceptions." + GeneratorConstants.DefaultPackageLayout.RESPONSES, result);
    }

    @Test
    @DisplayName("computeExceptionHandlerSubPackage: should return default EXCEPTIONS.HANDLERS when config is null")
    void computeExceptionHandlerSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeExceptionHandlerSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.EXCEPTIONS + "." + GeneratorConstants.DefaultPackageLayout.HANDLERS,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionHandlerSubPackage: should return user-defined exceptions + handlers when non-blank")
    void computeExceptionHandlerSubPackage_nonBlank_returnsUserDefinedPlusHandlers() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("custom.exceptions");

        final String result = PackageUtils.computeExceptionHandlerSubPackage(config);

        assertEquals("custom.exceptions." + GeneratorConstants.DefaultPackageLayout.HANDLERS, result);
    }

    @Test
    @DisplayName("computeExceptionHandlerSubPackage: should return default EXCEPTIONS.HANDLERS when user-defined exceptions is blank")
    void computeExceptionHandlerSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("   ");

        final String result = PackageUtils.computeExceptionHandlerSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.EXCEPTIONS + "." + GeneratorConstants.DefaultPackageLayout.HANDLERS,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionHandlerPackage: should join base package with user-defined exception handler subpackage")
    void computeExceptionHandlerPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("custom.exceptions");

        final String result = PackageUtils.computeExceptionHandlerPackage("com.example", config);

        assertEquals("com.example.custom.exceptions." + GeneratorConstants.DefaultPackageLayout.HANDLERS, result);
    }

    @Test
    @DisplayName("computeExceptionHandlerPackage: should join base package with default handler subpackage when user-defined exceptions is blank")
    void computeExceptionHandlerPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("");

        final String result = PackageUtils.computeExceptionHandlerPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.EXCEPTIONS + "." + GeneratorConstants.DefaultPackageLayout.HANDLERS,
            result
        );
    }

    @Test
    @DisplayName("computeExceptionHandlerPackage: should return only subpackage when basePackage is null")
    void computeExceptionHandlerPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setExceptions("custom.exceptions");

        final String result = PackageUtils.computeExceptionHandlerPackage(null, config);

        assertEquals("custom.exceptions." + GeneratorConstants.DefaultPackageLayout.HANDLERS, result);
    }

    @Test
    @DisplayName("computeEnumSubPackage: should return default ENUMS when config is null")
    void computeEnumSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeEnumSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.ENUMS,
            result
        );
    }

    @Test
    @DisplayName("computeEnumSubPackage: should return user-defined enum package when non-blank")
    void computeEnumSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setEnums("custom.enums");

        final String result = PackageUtils.computeEnumSubPackage(config);

        assertEquals("custom.enums", result);
    }

    @Test
    @DisplayName("computeEnumSubPackage: should return default ENUMS when user-defined enum package is blank")
    void computeEnumSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setEnums("   ");

        final String result = PackageUtils.computeEnumSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.ENUMS,
            result
        );
    }

    @Test
    @DisplayName("computeEnumPackage: should join base package with user-defined enum package")
    void computeEnumPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setEnums("enums");

        final String result = PackageUtils.computeEnumPackage("com.example", config);

        assertEquals("com.example.enums", result);
    }

    @Test
    @DisplayName("computeEnumPackage: should join base package with default enum subpackage when user-defined is blank")
    void computeEnumPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setEnums("");

        final String result = PackageUtils.computeEnumPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.ENUMS,
            result
        );
    }

    @Test
    @DisplayName("computeEnumPackage: should use default enum subpackage when config is null")
    void computeEnumPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeEnumPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.ENUMS,
            result
        );
    }

    @Test
    @DisplayName("computeEnumPackage: should return only enum subpackage when basePackage is null")
    void computeEnumPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setEnums("custom.enums");

        final String result = PackageUtils.computeEnumPackage(null, config);

        assertEquals("custom.enums", result);
    }

    @Test
    @DisplayName("computeGeneratedModelPackage: should join base, generated, strippedModel and MODEL when generated is user-defined")
    void computeGeneratedModelPackage_withUserDefinedGenerated() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setGenerated("gen");

        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedModelPackage("com.example", config, strippedModel);

        assertEquals(
            "com.example.gen." + strippedModel + "." + GeneratorConstants.DefaultPackageLayout.MODEL,
            result
        );
    }

    @Test
    @DisplayName("computeGeneratedModelPackage: should use default GENERATED when generated is blank")
    void computeGeneratedModelPackage_blankGenerated_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setGenerated("   ");

        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedModelPackage("com.example", config, strippedModel);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.GENERATED
            + "." + strippedModel
            + "." + GeneratorConstants.DefaultPackageLayout.MODEL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGeneratedModelPackage: should use default GENERATED when config is null")
    void computeGeneratedModelPackage_nullConfig_usesDefault() {
        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedModelPackage("com.example", null, strippedModel);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.GENERATED
            + "." + strippedModel
            + "." + GeneratorConstants.DefaultPackageLayout.MODEL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGeneratedModelPackage: should return only generated model subpackage when basePackage is null")
    void computeGeneratedModelPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setGenerated("gen");

        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedModelPackage(null, config, strippedModel);

        final String expected =
            "gen."
            + strippedModel
            + "." + GeneratorConstants.DefaultPackageLayout.MODEL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGeneratedApiPackage: should join base, generated, strippedModel and API when generated is user-defined")
    void computeGeneratedApiPackage_withUserDefinedGenerated() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setGenerated("gen");

        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedApiPackage("com.example", config, strippedModel);

        assertEquals(
            "com.example.gen." + strippedModel + "." + GeneratorConstants.DefaultPackageLayout.API,
            result
        );
    }

    @Test
    @DisplayName("computeGeneratedApiPackage: should use default GENERATED when generated is blank")
    void computeGeneratedApiPackage_blankGenerated_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setGenerated("");

        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedApiPackage("com.example", config, strippedModel);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.GENERATED
            + "." + strippedModel
            + "." + GeneratorConstants.DefaultPackageLayout.API;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGeneratedApiPackage: should use default GENERATED when config is null")
    void computeGeneratedApiPackage_nullConfig_usesDefault() {
        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedApiPackage("com.example", null, strippedModel);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.GENERATED
            + "." + strippedModel
            + "." + GeneratorConstants.DefaultPackageLayout.API;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGeneratedApiPackage: should return only generated API subpackage when basePackage is null")
    void computeGeneratedApiPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setGenerated("gen");

        final String strippedModel = "user";

        final String result = PackageUtils.computeGeneratedApiPackage(null, config, strippedModel);

        final String expected =
            "gen."
            + strippedModel
            + "." + GeneratorConstants.DefaultPackageLayout.API;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeAnnotationSubPackage: should return default ANNOTATIONS when config is null")
    void computeAnnotationSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeAnnotationSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.ANNOTATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeAnnotationSubPackage: should return user-defined annotations package when non-blank")
    void computeAnnotationSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setAnnotations("custom.annotations");

        final String result = PackageUtils.computeAnnotationSubPackage(config);

        assertEquals("custom.annotations", result);
    }

    @Test
    @DisplayName("computeAnnotationSubPackage: should return default ANNOTATIONS when user-defined value is blank")
    void computeAnnotationSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setAnnotations("   ");

        final String result = PackageUtils.computeAnnotationSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.ANNOTATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeAnnotationPackage: should join base package with user-defined annotations package")
    void computeAnnotationPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setAnnotations("annotations");

        final String result = PackageUtils.computeAnnotationPackage("com.example", config);

        assertEquals("com.example.annotations", result);
    }

    @Test
    @DisplayName("computeAnnotationPackage: should join base package with default annotations subpackage when user-defined is blank")
    void computeAnnotationPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setAnnotations("");

        final String result = PackageUtils.computeAnnotationPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.ANNOTATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeAnnotationPackage: should use default annotations subpackage when config is null")
    void computeAnnotationPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeAnnotationPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.ANNOTATIONS,
            result
        );
    }

    @Test
    @DisplayName("computeAnnotationPackage: should return only annotations subpackage when basePackage is null")
    void computeAnnotationPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setAnnotations("custom.annotations");

        final String result = PackageUtils.computeAnnotationPackage(null, config);

        assertEquals("custom.annotations", result);
    }

    @Test
    @DisplayName("computeBusinessServiceSubPackage: should return default BUSINESS_SERVICES when config is null")
    void computeBusinessServiceSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeBusinessServiceSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeBusinessServiceSubPackage: should return user-defined business service package when non-blank")
    void computeBusinessServiceSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setBusinessservices("custom.business");

        final String result = PackageUtils.computeBusinessServiceSubPackage(config);

        assertEquals("custom.business", result);
    }

    @Test
    @DisplayName("computeBusinessServiceSubPackage: should return default BUSINESS_SERVICES when user-defined value is blank")
    void computeBusinessServiceSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setBusinessservices("   ");

        final String result = PackageUtils.computeBusinessServiceSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeBusinessServicePackage: should join base package with user-defined business service package")
    void computeBusinessServicePackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setBusinessservices("business");

        final String result = PackageUtils.computeBusinessServicePackage("com.example", config);

        assertEquals("com.example.business", result);
    }

    @Test
    @DisplayName("computeBusinessServicePackage: should join base package with default subpackage when user-defined is blank")
    void computeBusinessServicePackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setBusinessservices("");

        final String result = PackageUtils.computeBusinessServicePackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeBusinessServicePackage: should use default subpackage when config is null")
    void computeBusinessServicePackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeBusinessServicePackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.BUSINESS_SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeBusinessServicePackage: should return only subpackage when basePackage is null")
    void computeBusinessServicePackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setBusinessservices("custom.business");

        final String result = PackageUtils.computeBusinessServicePackage(null, config);

        assertEquals("custom.business", result);
    }

    @Test
    @DisplayName("computeResolversSubPackage: should return default RESOLVERS when config is null")
    void computeResolversSubPackage_nullConfig_returnsDefault() {
        
        final String result = PackageUtils.computeResolversSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.RESOLVERS,
            result
        );
    }

    @Test
    @DisplayName("computeResolversSubPackage: should return user-defined resolvers package when non-blank")
    void computeResolversSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setResolvers("custom.resolvers");

        final String result = PackageUtils.computeResolversSubPackage(config);

        assertEquals("custom.resolvers", result);
    }

    @Test
    @DisplayName("computeResolversSubPackage: should return default RESOLVERS when user-defined resolvers is blank")
    void computeResolversSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setResolvers("   ");

        final String result = PackageUtils.computeResolversSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.RESOLVERS,
            result
        );
    }

    @Test
    @DisplayName("computeResolversPackage: should join base package with user-defined resolvers package")
    void computeResolversPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setResolvers("resolvers");

        final String result = PackageUtils.computeResolversPackage("com.example", config);

        assertEquals("com.example.resolvers", result);
    }

    @Test
    @DisplayName("computeResolversPackage: should join base package with default resolvers subpackage when user-defined is blank")
    void computeResolversPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setResolvers("");

        final String result = PackageUtils.computeResolversPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.RESOLVERS,
            result
        );
    }

    @Test
    @DisplayName("computeResolversPackage: should use default resolvers subpackage when config is null")
    void computeResolversPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeResolversPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.RESOLVERS,
            result
        );
    }

    @Test
    @DisplayName("computeResolversPackage: should return only resolvers subpackage when basePackage is null")
    void computeResolversPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setResolvers("custom.resolvers");

        final String result = PackageUtils.computeResolversPackage(null, config);

        assertEquals("custom.resolvers", result);
    }

    @Test
    @DisplayName("computeRepositorySubPackage: should return default REPOSITORIES when config is null")
    void computeRepositorySubPackage_nullConfig_returnsDefault() {
        
        final String result = PackageUtils.computeRepositorySubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.REPOSITORIES,
            result
        );
    }

    @Test
    @DisplayName("computeRepositorySubPackage: should return user-defined repositories package when non-blank")
    void computeRepositorySubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setRepositories("custom.repo");

        final String result = PackageUtils.computeRepositorySubPackage(config);

        assertEquals("custom.repo", result);
    }

    @Test
    @DisplayName("computeRepositorySubPackage: should return default REPOSITORIES when user-defined value is blank")
    void computeRepositorySubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setRepositories("   ");

        final String result = PackageUtils.computeRepositorySubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.REPOSITORIES,
            result
        );
    }

    @Test
    @DisplayName("computeRepositoryPackage: should join base package with user-defined repository package")
    void computeRepositoryPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setRepositories("repository");

        final String result = PackageUtils.computeRepositoryPackage("com.example", config);

        assertEquals("com.example.repository", result);
    }

    @Test
    @DisplayName("computeRepositoryPackage: should join base package with default repository package when user-defined is blank")
    void computeRepositoryPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setRepositories("");

        final String result = PackageUtils.computeRepositoryPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.REPOSITORIES,
            result
        );
    }

    @Test
    @DisplayName("computeRepositoryPackage: should use default repository subpackage when config is null")
    void computeRepositoryPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeRepositoryPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.REPOSITORIES,
            result
        );
    }

    @Test
    @DisplayName("computeRepositoryPackage: should return only repository subpackage when basePackage is null")
    void computeRepositoryPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setRepositories("custom.repo");

        final String result = PackageUtils.computeRepositoryPackage(null, config);

        assertEquals("custom.repo", result);
    }

    @Test
    @DisplayName("computeEntitySubPackage: should return default MODELS when config is null")
    void computeEntitySubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeEntitySubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.MODELS,
            result
        );
    }

    @Test
    @DisplayName("computeEntitySubPackage: should return user-defined models package when non-blank")
    void computeEntitySubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("custom.models");

        final String result = PackageUtils.computeEntitySubPackage(config);

        assertEquals("custom.models", result);
    }

    @Test
    @DisplayName("computeEntitySubPackage: should return default MODELS when user-defined models value is blank")
    void computeEntitySubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("   ");

        final String result = PackageUtils.computeEntitySubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.MODELS,
            result
        );
    }

    @Test
    @DisplayName("computeEntityPackage: should join base package with user-defined models package")
    void computeEntityPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("models");

        final String result = PackageUtils.computeEntityPackage("com.example", config);

        assertEquals("com.example.models", result);
    }

    @Test
    @DisplayName("computeEntityPackage: should join base package with default MODELS when user-defined is blank")
    void computeEntityPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("");

        final String result = PackageUtils.computeEntityPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.MODELS,
            result
        );
    }

    @Test
    @DisplayName("computeEntityPackage: should use default MODELS when config is null")
    void computeEntityPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeEntityPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.MODELS,
            result
        );
    }

    @Test
    @DisplayName("computeEntityPackage: should return only entity subpackage when basePackage is null")
    void computeEntityPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("custom.models");

        final String result = PackageUtils.computeEntityPackage(null, config);

        assertEquals("custom.models", result);
    }

    @Test
    @DisplayName("computeHelperEntityPackage: should append HELPERS to entity package with user-defined models")
    void computeHelperEntityPackage_withUserDefinedModels() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("models");

        final String result = PackageUtils.computeHelperEntityPackage("com.example", config);

        assertEquals(
            "com.example.models." + GeneratorConstants.DefaultPackageLayout.HELPERS,
            result
        );
    }

    @Test
    @DisplayName("computeHelperEntityPackage: should use default MODELS when config is null")
    void computeHelperEntityPackage_nullConfig_usesDefaultModels() {
        final String result = PackageUtils.computeHelperEntityPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MODELS
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperEntityPackage: should use default MODELS when user-defined models is blank")
    void computeHelperEntityPackage_blankModels_usesDefaultModels() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("   ");

        final String result = PackageUtils.computeHelperEntityPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MODELS
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperEntityPackage: should work when basePackage is null (only entity + helpers)")
    void computeHelperEntityPackage_nullBasePackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("models");

        final String result = PackageUtils.computeHelperEntityPackage(null, config);

        final String expected =
            "models."
            + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeTransferObjectSubPackage: should return default TRANSFEROBJECTS when config is null")
    void computeTransferObjectSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeTransferObjectSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS,
            result
        );
    }

    @Test
    @DisplayName("computeTransferObjectSubPackage: should return user-defined transferobjects package when non-blank")
    void computeTransferObjectSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeTransferObjectSubPackage(config);

        assertEquals("custom.dto", result);
    }

    @Test
    @DisplayName("computeTransferObjectSubPackage: should return default TRANSFEROBJECTS when user-defined value is blank")
    void computeTransferObjectSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("   ");

        final String result = PackageUtils.computeTransferObjectSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS,
            result
        );
    }

    @Test
    @DisplayName("computeTransferObjectPackage: should join base package with user-defined transferobjects package")
    void computeTransferObjectPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("dto");

        final String result = PackageUtils.computeTransferObjectPackage("com.example", config);

        assertEquals("com.example.dto", result);
    }

    @Test
    @DisplayName("computeTransferObjectPackage: should join base package with default subpackage when user-defined is blank")
    void computeTransferObjectPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("");

        final String result = PackageUtils.computeTransferObjectPackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS,
            result
        );
    }

    @Test
    @DisplayName("computeTransferObjectPackage: should use default subpackage when config is null")
    void computeTransferObjectPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeTransferObjectPackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS,
            result
        );
    }

    @Test
    @DisplayName("computeTransferObjectPackage: should return only transferobjects subpackage when basePackage is null")
    void computeTransferObjectPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeTransferObjectPackage(null, config);

        assertEquals("custom.dto", result);
    }

    @Test
    @DisplayName("computeRestTransferObjectSubPackage: should append REST to user-defined transferobjects package")
    void computeRestTransferObjectSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeRestTransferObjectSubPackage(config);

        assertEquals(
            "custom.dto." + GeneratorConstants.DefaultPackageLayout.REST,
            result
        );
    }

    @Test
    @DisplayName("computeRestTransferObjectSubPackage: should use default TRANSFEROBJECTS when config is null")
    void computeRestTransferObjectSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeRestTransferObjectSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestTransferObjectSubPackage: should use default TRANSFEROBJECTS when user-defined is blank")
    void computeRestTransferObjectSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("   ");

        final String result = PackageUtils.computeRestTransferObjectSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestTransferObjectPackage: should join base package with rest transferobjects subpackage (user-defined)")
    void computeRestTransferObjectPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeRestTransferObjectPackage("com.example", config);

        assertEquals(
            "com.example.custom.dto." + GeneratorConstants.DefaultPackageLayout.REST,
            result
        );
    }

    @Test
    @DisplayName("computeRestTransferObjectPackage: should join base package with default rest subpackage when user-defined is blank")
    void computeRestTransferObjectPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("");

        final String result = PackageUtils.computeRestTransferObjectPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestTransferObjectPackage: should use default rest subpackage when config is null")
    void computeRestTransferObjectPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeRestTransferObjectPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestTransferObjectPackage: should return only rest transferobjects subpackage when basePackage is null")
    void computeRestTransferObjectPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeRestTransferObjectPackage(null, config);

        assertEquals(
            "custom.dto." + GeneratorConstants.DefaultPackageLayout.REST,
            result
        );
    }

    @Test
    @DisplayName("computeHelperRestTransferObjectSubPackage: should append HELPERS to rest transferobjects subpackage (user-defined)")
    void computeHelperRestTransferObjectSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeHelperRestTransferObjectSubPackage(config);

        final String expected =
            "custom.dto."
            + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestTransferObjectSubPackage: should use default TRANSFEROBJECTS when config is null")
    void computeHelperRestTransferObjectSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperRestTransferObjectSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestTransferObjectSubPackage: should use default TRANSFEROBJECTS when user-defined is blank")
    void computeHelperRestTransferObjectSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("   ");

        final String result = PackageUtils.computeHelperRestTransferObjectSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestTransferObjectPackage: should join base package with helper rest transferobjects subpackage (user-defined)")
    void computeHelperRestTransferObjectPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeHelperRestTransferObjectPackage("com.example", config);

        final String expected =
            "com.example.custom.dto."
            + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestTransferObjectPackage: should use default when config is null")
    void computeHelperRestTransferObjectPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperRestTransferObjectPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestTransferObjectPackage: should use default when user-defined is blank")
    void computeHelperRestTransferObjectPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("");

        final String result = PackageUtils.computeHelperRestTransferObjectPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestTransferObjectPackage: should return only helper rest subpackage when basePackage is null")
    void computeHelperRestTransferObjectPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeHelperRestTransferObjectPackage(null, config);

        final String expected =
            "custom.dto."
            + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphqlTransferObjectSubPackage: should append GRAPHQL to user-defined transferobjects subpackage")
    void computeGraphqlTransferObjectSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeGraphqlTransferObjectSubPackage(config);

        final String expected =
            "custom.dto." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphqlTransferObjectSubPackage: should use default TRANSFEROBJECTS when config is null")
    void computeGraphqlTransferObjectSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeGraphqlTransferObjectSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphqlTransferObjectSubPackage: should use default TRANSFEROBJECTS when user-defined is blank")
    void computeGraphqlTransferObjectSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("   ");

        final String result = PackageUtils.computeGraphqlTransferObjectSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphqlTransferObjectPackage: should join base package with graphql transferobjects subpackage (user-defined)")
    void computeGraphqlTransferObjectPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeGraphqlTransferObjectPackage("com.example", config);

        final String expected =
            "com.example.custom.dto."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphqlTransferObjectPackage: should join base package with default graphql transferobjects subpackage when user-defined is blank")
    void computeGraphqlTransferObjectPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("");

        final String result = PackageUtils.computeGraphqlTransferObjectPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphqlTransferObjectPackage: should use default graphql transferobjects subpackage when config is null")
    void computeGraphqlTransferObjectPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeGraphqlTransferObjectPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphqlTransferObjectPackage: should return only graphql transferobjects subpackage when basePackage is null")
    void computeGraphqlTransferObjectPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeGraphqlTransferObjectPackage(null, config);

        final String expected =
            "custom.dto."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphqlTransferObjectSubPackage: should append HELPERS to graphql transferobjects subpackage (user-defined)")
    void computeHelperGraphqlTransferObjectSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeHelperGraphqlTransferObjectSubPackage(config);

        final String expected =
            "custom.dto."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphqlTransferObjectSubPackage: should use default TRANSFEROBJECTS when config is null")
    void computeHelperGraphqlTransferObjectSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperGraphqlTransferObjectSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphqlTransferObjectSubPackage: should use default TRANSFEROBJECTS when user-defined is blank")
    void computeHelperGraphqlTransferObjectSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("   ");

        final String result = PackageUtils.computeHelperGraphqlTransferObjectSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphqlTransferObjectPackage: should join base package with helper graphql transferobjects subpackage (user-defined)")
    void computeHelperGraphqlTransferObjectPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeHelperGraphqlTransferObjectPackage("com.example", config);

        final String expected =
            "com.example.custom.dto."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphqlTransferObjectPackage: should use default helper graphql subpackage when config is null")
    void computeHelperGraphqlTransferObjectPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperGraphqlTransferObjectPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphqlTransferObjectPackage: should use default helper graphql subpackage when user-defined is blank")
    void computeHelperGraphqlTransferObjectPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("");

        final String result = PackageUtils.computeHelperGraphqlTransferObjectPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphqlTransferObjectPackage: should return only helper graphql subpackage when basePackage is null")
    void computeHelperGraphqlTransferObjectPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setTransferobjects("custom.dto");

        final String result = PackageUtils.computeHelperGraphqlTransferObjectPackage(null, config);

        final String expected =
            "custom.dto."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperEntitySubPackage: should append HELPERS to entity subpackage with user-defined models")
    void computeHelperEntitySubPackage_withUserDefinedModels() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("custom.models");

        final String result = PackageUtils.computeHelperEntitySubPackage(config);

        final String expected =
            "custom.models."
            + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperEntitySubPackage: should use default MODELS when config is null")
    void computeHelperEntitySubPackage_nullConfig_usesDefaultModels() {
        final String result = PackageUtils.computeHelperEntitySubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MODELS
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperEntitySubPackage: should use default MODELS when user-defined models is blank")
    void computeHelperEntitySubPackage_blankModels_usesDefaultModels() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setModels("   ");

        final String result = PackageUtils.computeHelperEntitySubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MODELS
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeServiceSubPackage: should return default SERVICES when config is null")
    void computeServiceSubPackage_nullConfig_returnsDefault() {
        final String result = PackageUtils.computeServiceSubPackage(null);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeServiceSubPackage: should return user-defined services package when non-blank")
    void computeServiceSubPackage_nonBlank_returnsUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setServices("custom.services");

        final String result = PackageUtils.computeServiceSubPackage(config);

        assertEquals("custom.services", result);
    }

    @Test
    @DisplayName("computeServiceSubPackage: should return default SERVICES when user-defined services value is blank")
    void computeServiceSubPackage_blank_returnsDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setServices("   ");

        final String result = PackageUtils.computeServiceSubPackage(config);

        assertEquals(
            GeneratorConstants.DefaultPackageLayout.SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeServicePackage: should join base package with user-defined services package")
    void computeServicePackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setServices("services");

        final String result = PackageUtils.computeServicePackage("com.example", config);

        assertEquals("com.example.services", result);
    }

    @Test
    @DisplayName("computeServicePackage: should join base package with default services subpackage when user-defined is blank")
    void computeServicePackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setServices("");

        final String result = PackageUtils.computeServicePackage("com.example", config);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeServicePackage: should use default services subpackage when config is null")
    void computeServicePackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeServicePackage("com.example", null);

        assertEquals(
            "com.example." + GeneratorConstants.DefaultPackageLayout.SERVICES,
            result
        );
    }

    @Test
    @DisplayName("computeServicePackage: should return only services subpackage when basePackage is null")
    void computeServicePackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setServices("custom.services");

        final String result = PackageUtils.computeServicePackage(null, config);

        assertEquals("custom.services", result);
    }

    @Test
    @DisplayName("computeRestMappersSubPackage: should append REST to user-defined mappers subpackage")
    void computeRestMappersSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeRestMappersSubPackage(config);

        final String expected =
            "custom.mappers." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestMappersSubPackage: should use default MAPPERS when config is null")
    void computeRestMappersSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeRestMappersSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestMappersSubPackage: should use default MAPPERS when user-defined is blank")
    void computeRestMappersSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("   ");

        final String result = PackageUtils.computeRestMappersSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestMapperPackage: should join base package with rest mappers subpackage (user-defined)")
    void computeRestMapperPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeRestMapperPackage("com.example", config);

        final String expected =
            "com.example.custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestMapperPackage: should join base package with default rest mappers subpackage when user-defined is blank")
    void computeRestMapperPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("");

        final String result = PackageUtils.computeRestMapperPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestMapperPackage: should use default rest mappers subpackage when config is null")
    void computeRestMapperPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeRestMapperPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeRestMapperPackage: should return only rest mappers subpackage when basePackage is null")
    void computeRestMapperPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeRestMapperPackage(null, config);

        final String expected =
            "custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.REST;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestMappersSubPackage: should append HELPERS to rest mappers subpackage (user-defined)")
    void computeHelperRestMappersSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeHelperRestMappersSubPackage(config);

        final String expected =
            "custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestMappersSubPackage: should use default MAPPERS when config is null")
    void computeHelperRestMappersSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperRestMappersSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestMappersSubPackage: should use default MAPPERS when user-defined is blank")
    void computeHelperRestMappersSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("   ");

        final String result = PackageUtils.computeHelperRestMappersSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestMapperPackage: should join base package with helper rest mappers subpackage (user-defined)")
    void computeHelperRestMapperPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeHelperRestMapperPackage("com.example", config);

        final String expected =
            "com.example.custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestMapperPackage: should use default helper rest mappers subpackage when config is null")
    void computeHelperRestMapperPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperRestMapperPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestMapperPackage: should use default helper rest mappers subpackage when user-defined is blank")
    void computeHelperRestMapperPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("");

        final String result = PackageUtils.computeHelperRestMapperPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperRestMapperPackage: should return only helper rest mappers subpackage when basePackage is null")
    void computeHelperRestMapperPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeHelperRestMapperPackage(null, config);

        final String expected =
            "custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.REST
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphQlMappersSubPackage: should append GRAPHQL to user-defined mappers subpackage")
    void computeGraphQlMappersSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeGraphQlMappersSubPackage(config);

        final String expected =
            "custom.mappers." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphQlMappersSubPackage: should use default MAPPERS when config is null")
    void computeGraphQlMappersSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeGraphQlMappersSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphQlMappersSubPackage: should use default MAPPERS when user-defined is blank")
    void computeGraphQlMappersSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("   ");

        final String result = PackageUtils.computeGraphQlMappersSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphQlMapperPackage: should join base package with graphql mappers subpackage (user-defined)")
    void computeGraphQlMapperPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeGraphQlMapperPackage("com.example", config);

        final String expected =
            "com.example.custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphQlMapperPackage: should join base package with default graphql mappers subpackage when user-defined is blank")
    void computeGraphQlMapperPackage_withDefaultWhenBlank() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("");

        final String result = PackageUtils.computeGraphQlMapperPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphQlMapperPackage: should use default graphql mappers subpackage when config is null")
    void computeGraphQlMapperPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeGraphQlMapperPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeGraphQlMapperPackage: should return only graphql mappers subpackage when basePackage is null")
    void computeGraphQlMapperPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeGraphQlMapperPackage(null, config);

        final String expected =
            "custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphQlMappersSubPackage: should append HELPERS to graphql mappers subpackage (user-defined)")
    void computeHelperGraphQlMappersSubPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeHelperGraphQlMappersSubPackage(config);

        final String expected =
            "custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphQlMappersSubPackage: should use default MAPPERS when config is null")
    void computeHelperGraphQlMappersSubPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperGraphQlMappersSubPackage(null);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphQlMappersSubPackage: should use default MAPPERS when user-defined is blank")
    void computeHelperGraphQlMappersSubPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("   ");

        final String result = PackageUtils.computeHelperGraphQlMappersSubPackage(config);

        final String expected =
            GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphQlMapperPackage: should join base package with helper graphql mappers subpackage (user-defined)")
    void computeHelperGraphQlMapperPackage_withUserDefined() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeHelperGraphQlMapperPackage("com.example", config);

        final String expected =
            "com.example.custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphQlMapperPackage: should use default helper graphql mappers subpackage when config is null")
    void computeHelperGraphQlMapperPackage_nullConfig_usesDefault() {
        final String result = PackageUtils.computeHelperGraphQlMapperPackage("com.example", null);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphQlMapperPackage: should use default helper graphql mappers subpackage when user-defined is blank")
    void computeHelperGraphQlMapperPackage_blank_usesDefault() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("");

        final String result = PackageUtils.computeHelperGraphQlMapperPackage("com.example", config);

        final String expected =
            "com.example."
            + GeneratorConstants.DefaultPackageLayout.MAPPERS
            + "." + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("computeHelperGraphQlMapperPackage: should return only helper graphql mappers subpackage when basePackage is null")
    void computeHelperGraphQlMapperPackage_nullBasePackage_returnsOnlySubPackage() {
        final PackageConfiguration config = new PackageConfiguration();
        config.setMappers("custom.mappers");

        final String result = PackageUtils.computeHelperGraphQlMapperPackage(null, config);

        final String expected =
            "custom.mappers."
            + GeneratorConstants.DefaultPackageLayout.GRAPHQL
            + "." + GeneratorConstants.DefaultPackageLayout.HELPERS;

        assertEquals(expected, result);
    }

}
