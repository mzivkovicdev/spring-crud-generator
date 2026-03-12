package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

class CrudMojoUtilsTest {

    @Test
    void createSpecMapper_yamlExtension_returnsYamlMapper() {
        final ObjectMapper mapper1 = CrudMojoUtils.createSpecMapper("spec.yaml");
        final ObjectMapper mapper2 = CrudMojoUtils.createSpecMapper("SPEC.YML");

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
    void printBanner_withNullPluginDescriptor_doesNotThrow() {
        assertDoesNotThrow(() -> CrudMojoUtils.printBanner(null, "spec.yaml", "target/out"));
    }

    @Test
    void printBanner_withPluginDescriptorVersion_doesNotThrow() {
        final PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setVersion("1.5.0-test");

        assertDoesNotThrow(() -> CrudMojoUtils.printBanner(pluginDescriptor, "spec.yaml", "target/out"));
    }
}
