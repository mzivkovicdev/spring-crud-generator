package dev.markozivkovic.springcrudgenerator.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;

class ExceptionImportsTest {

    private static final String OUTPUT_DIR = "/some/output/dir";

    @Test
    @DisplayName("REST handler: no relations → ResourceNotFoundException + HttpResponse, no InvalidResourceStateException")
    void computeGlobalRestExceptionHandlerProjectImports_noRelations() {
        
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(OUTPUT_DIR))
                    .thenReturn("com.example");

            pkg.when(() -> PackageUtils.computeExceptionPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception");
            pkg.when(() -> PackageUtils.computeExceptionResponsePackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception.response");

            pkg.when(() -> PackageUtils.join("com.example.exception", "ResourceNotFoundException"))
                    .thenReturn("com.example.exception.ResourceNotFoundException");
            pkg.when(() -> PackageUtils.join("com.example.exception", "InvalidResourceStateException"))
                    .thenReturn("com.example.exception.InvalidResourceStateException");
            pkg.when(() -> PackageUtils.join("com.example.exception.response", "HttpResponse"))
                    .thenReturn("com.example.exception.response.HttpResponse");

            final String result = ExceptionImports.computeGlobalRestExceptionHandlerProjectImports(
                    false,
                    OUTPUT_DIR,
                    packageConfiguration
            );

            assertTrue(result.contains("com.example.exception.ResourceNotFoundException"),
                    "Should contain ResourceNotFoundException import");
            assertTrue(result.contains("com.example.exception.response.HttpResponse"),
                    "Should contain HttpResponse import");
            assertFalse(result.contains("InvalidResourceStateException"),
                    "Should NOT contain InvalidResourceStateException import when hasRelations=false");
        }
    }

    @Test
    @DisplayName("REST handler: with relations → ResourceNotFoundException + HttpResponse + InvalidResourceStateException")
    void computeGlobalRestExceptionHandlerProjectImports_withRelations() {
        
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(OUTPUT_DIR))
                    .thenReturn("com.example");

            pkg.when(() -> PackageUtils.computeExceptionPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception");
            pkg.when(() -> PackageUtils.computeExceptionResponsePackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception.response");

            pkg.when(() -> PackageUtils.join("com.example.exception", "ResourceNotFoundException"))
                    .thenReturn("com.example.exception.ResourceNotFoundException");
            pkg.when(() -> PackageUtils.join("com.example.exception", "InvalidResourceStateException"))
                    .thenReturn("com.example.exception.InvalidResourceStateException");
            pkg.when(() -> PackageUtils.join("com.example.exception.response", "HttpResponse"))
                    .thenReturn("com.example.exception.response.HttpResponse");

            final String result = ExceptionImports.computeGlobalRestExceptionHandlerProjectImports(
                    true,
                    OUTPUT_DIR,
                    packageConfiguration
            );

            assertTrue(result.contains("com.example.exception.ResourceNotFoundException"),
                    "Should contain ResourceNotFoundException import");
            assertTrue(result.contains("com.example.exception.response.HttpResponse"),
                    "Should contain HttpResponse import");
            assertTrue(result.contains("com.example.exception.InvalidResourceStateException"),
                    "Should contain InvalidResourceStateException import when hasRelations=true");
        }
    }

    @Test
    @DisplayName("GraphQL handler: no relations → only ResourceNotFoundException (no HttpResponse, no InvalidResourceStateException)")
    void computeGlobalGraphQlExceptionHandlerProjectImports_noRelations() {
        
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(OUTPUT_DIR))
                    .thenReturn("com.example");

            pkg.when(() -> PackageUtils.computeExceptionPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception");

            pkg.when(() -> PackageUtils.computeExceptionResponsePackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception.response");

            pkg.when(() -> PackageUtils.join("com.example.exception", "ResourceNotFoundException"))
                    .thenReturn("com.example.exception.ResourceNotFoundException");
            pkg.when(() -> PackageUtils.join("com.example.exception", "InvalidResourceStateException"))
                    .thenReturn("com.example.exception.InvalidResourceStateException");
            pkg.when(() -> PackageUtils.join("com.example.exception.response", "HttpResponse"))
                    .thenReturn("com.example.exception.response.HttpResponse");

            final String result = ExceptionImports.computeGlobalGraphQlExceptionHandlerProjectImports(
                    false,
                    OUTPUT_DIR,
                    packageConfiguration
            );

            assertTrue(result.contains("com.example.exception.ResourceNotFoundException"),
                    "Should contain ResourceNotFoundException import");
            assertFalse(result.contains("HttpResponse"),
                    "GraphQL handler should NOT import HttpResponse");
            assertFalse(result.contains("InvalidResourceStateException"),
                    "Should NOT contain InvalidResourceStateException when hasRelations=false");
        }
    }

    @Test
    @DisplayName("GraphQL handler: with relations → ResourceNotFoundException + InvalidResourceStateException, but no HttpResponse")
    void computeGlobalGraphQlExceptionHandlerProjectImports_withRelations() {
        final PackageConfiguration packageConfiguration = new PackageConfiguration();

        try (final MockedStatic<PackageUtils> pkg = Mockito.mockStatic(PackageUtils.class)) {
            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(OUTPUT_DIR))
                    .thenReturn("com.example");

            pkg.when(() -> PackageUtils.computeExceptionPackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception");
            pkg.when(() -> PackageUtils.computeExceptionResponsePackage("com.example", packageConfiguration))
                    .thenReturn("com.example.exception.response");

            pkg.when(() -> PackageUtils.join("com.example.exception", "ResourceNotFoundException"))
                    .thenReturn("com.example.exception.ResourceNotFoundException");
            pkg.when(() -> PackageUtils.join("com.example.exception", "InvalidResourceStateException"))
                    .thenReturn("com.example.exception.InvalidResourceStateException");
            pkg.when(() -> PackageUtils.join("com.example.exception.response", "HttpResponse"))
                    .thenReturn("com.example.exception.response.HttpResponse");

            final String result = ExceptionImports.computeGlobalGraphQlExceptionHandlerProjectImports(
                    true,
                    OUTPUT_DIR,
                    packageConfiguration
            );

            assertTrue(result.contains("com.example.exception.ResourceNotFoundException"),
                    "Should contain ResourceNotFoundException import");
            assertTrue(result.contains("com.example.exception.InvalidResourceStateException"),
                    "Should contain InvalidResourceStateException import when hasRelations=true");
            assertFalse(result.contains("HttpResponse"),
                    "GraphQL handler should NOT import HttpResponse");
        }
    }
}
