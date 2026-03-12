package dev.markozivkovic.springcrudgenerator.plugins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import dev.markozivkovic.springcrudgenerator.utils.CrudMojoUtils;

class CrudValidateMojoTest {

    @TempDir
    Path tempDir;

    private CrudValidateMojo newMojo() {
        return new CrudValidateMojo();
    }

    private void setField(final Object target, final String fieldName, final Object value) {
        try {
            final Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void execute_shouldThrowWhenInputSpecFileIsNull() {
        final CrudValidateMojo mojo = newMojo();

        setField(mojo, "inputSpecFile", null);

        final MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("inputSpecFile must be specified"));
    }

    @Test
    void createSpecMapper_yamlExtension_returnsYamlMapper() {
        final ObjectMapper mapper1 = CrudMojoUtils.createSpecMapper("spec.yaml");
        final ObjectMapper mapper2 = CrudMojoUtils.createSpecMapper("spec.YML");

        assertInstanceOf(YAMLMapper.class, mapper1);
        assertInstanceOf(YAMLMapper.class, mapper2);
    }

    @Test
    void createSpecMapper_jsonExtension_returnsJsonMapper() {
        final ObjectMapper mapper = CrudMojoUtils.createSpecMapper("spec.json");

        assertInstanceOf(JsonMapper.class, mapper);
    }

    @Test
    void createSpecMapper_configuresCommonFeatures_forYamlAndJson() {
        final ObjectMapper yaml = CrudMojoUtils.createSpecMapper("spec.yaml");
        final ObjectMapper json = CrudMojoUtils.createSpecMapper("spec.json");

        assertFalse(yaml.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertFalse(json.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        assertTrue(yaml.getDeserializationConfig().isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
        assertTrue(json.getDeserializationConfig().isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    @Test
    void createSpecMapper_unsupportedExtension_throws() {
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CrudMojoUtils.createSpecMapper("spec.txt")
        );
        assertTrue(ex.getMessage().contains("Unsupported file format"));
    }

    @Test
    void execute_validSpec_shouldNotThrow() throws Exception {
        final CrudValidateMojo mojo = newMojo();
        final Path specPath = tempDir.resolve("crud-spec.yaml");

        final String spec = """
                configuration:
                  database: postgresql
                entities:
                  - name: ProductModel
                    storageName: product_table
                    fields:
                      - name: id
                        type: Long
                        id:
                          strategy: TABLE
                      - name: name
                        type: String
                """;
        Files.writeString(specPath, spec);

        setField(mojo, "inputSpecFile", specPath.toString());
        setField(mojo, "parentVersion", "4.0.1");

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void execute_invalidSpec_shouldThrowMojoExecutionException() throws Exception {
        final CrudValidateMojo mojo = newMojo();
        final Path specPath = tempDir.resolve("crud-spec-invalid.yaml");

        final String invalidSpec = """
                configuration:
                  database: postgresql
                entities:
                  - name: ProductModel
                    storageName: product_table
                    fields:
                      - name: id
                        type: Long
                """;
        Files.writeString(specPath, invalidSpec);

        setField(mojo, "inputSpecFile", specPath.toString());

        final MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("Spec validation failed"));
    }
}
