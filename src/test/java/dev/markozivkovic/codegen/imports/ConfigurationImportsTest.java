package dev.markozivkovic.codegen.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.PackageUtils;

class ConfigurationImportsTest {

    @Test
    @DisplayName("getModelImports: empty list -> empty string")
    void getModelImports_emptyList_emptyString() {
        
        final PackageConfiguration pkg = mock(PackageConfiguration.class);

        try (final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class)) {
            
            final String result = ConfigurationImports.getModelImports("com.example", pkg, Collections.emptyList());

            assertEquals("", result);
            
            pkgUtils.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("getModelImports: single entity -> single import line")
    void getModelImports_singleEntity_singleImport() {
        final PackageConfiguration pkg = mock(PackageConfiguration.class);

        try (final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class)) {
            
            pkgUtils.when(() -> PackageUtils.computeEntityPackage("com.example", pkg)).thenReturn("com.example.models");

            final String result = ConfigurationImports.getModelImports(
                    "com.example", pkg, List.of("Product")
            );

            assertEquals(String.format("import com.example.models.Product;%n"), result);

            pkgUtils.verify(() -> PackageUtils.computeEntityPackage("com.example", pkg));
        }
    }

    @Test
    @DisplayName("getModelImports: preserves duplicates (no distinct) and still sorts")
    void getModelImports_duplicates_preservedAndSorted() {

        final PackageConfiguration pkg = mock(PackageConfiguration.class);

        try (final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class)) {
            
            pkgUtils.when(() -> PackageUtils.computeEntityPackage("com.example", pkg)).thenReturn("com.example.models");

            final String result = ConfigurationImports.getModelImports(
                    "com.example", pkg, List.of("User", "User", "Product")
            );

            final String expected = String.format(
                """
                    import com.example.models.Product;
                    import com.example.models.User;
                    import com.example.models.User;
                        """
            );

            assertEquals(expected, result);
        }
    }

    @Test
    @DisplayName("getModelImports: different basePath affects computed package")
    void getModelImports_differentBasePath_changesPackage() {
        
        final PackageConfiguration pkg = mock(PackageConfiguration.class);

        try (final MockedStatic<PackageUtils> pkgUtils = mockStatic(PackageUtils.class)) {
            
            pkgUtils.when(() -> PackageUtils.computeEntityPackage("x.y", pkg)).thenReturn("x.y.domain");

            final String result = ConfigurationImports.getModelImports(
                    "x.y", pkg, List.of("Order")
            );

            assertEquals(String.format("import x.y.domain.Order;%n"), result);
        }
    }
    
}
