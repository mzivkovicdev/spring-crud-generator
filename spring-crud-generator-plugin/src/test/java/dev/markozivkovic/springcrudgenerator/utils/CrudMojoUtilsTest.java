package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import dev.markozivkovic.springcrudgenerator.enums.SortDirection;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.CrudSpecification;

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
    void createSpecMapper_yaml_canDeserializeEntitySortConfiguration() throws Exception {
        final ObjectMapper mapper = CrudMojoUtils.createSpecMapper("crud-spec.yaml");

        final String yaml = """
                configuration:
                  database: postgresql
                entities:
                  - name: ProductModel
                    fields:
                      - name: id
                        type: Long
                        id:
                          strategy: IDENTITY
                      - name: name
                        type: String
                    sort:
                      allowedFields: [name]
                      defaultDirection: ASC
                """;

        final CrudSpecification spec = mapper.readValue(yaml, CrudSpecification.class);
        assertNotNull(spec);
        assertNotNull(spec.getEntities());
        assertNotNull(spec.getEntities().get(0).getSort());
        assertEquals(List.of("name"), spec.getEntities().get(0).getSort().getAllowedFields());
        assertEquals(SortDirection.ASC, spec.getEntities().get(0).getSort().getDefaultDirection());
    }

    @Test
    void createSpecMapper_yaml_sortDefaultDirectionDefaultsToAsc() throws Exception {
        final ObjectMapper mapper = CrudMojoUtils.createSpecMapper("crud-spec.yaml");

        final String yaml = """
                configuration:
                  database: postgresql
                entities:
                  - name: ProductModel
                    fields:
                      - name: id
                        type: Long
                        id:
                          strategy: IDENTITY
                      - name: name
                        type: String
                    sort:
                      allowedFields: [name]
                """;

        final CrudSpecification spec = mapper.readValue(yaml, CrudSpecification.class);
        assertNotNull(spec);
        assertNotNull(spec.getEntities().get(0).getSort());
        assertEquals(SortDirection.ASC, spec.getEntities().get(0).getSort().getDefaultDirection());
    }

    @Test
    void createSpecMapper_yaml_canDeserializeMongoDatabaseAndBooleanIdMarker() throws Exception {
        final ObjectMapper mapper = CrudMojoUtils.createSpecMapper("crud-spec.yaml");

        final String yaml = """
                configuration:
                  database: mongodb
                entities:
                  - name: ProductModel
                    storageName: products
                    fields:
                      - name: id
                        type: String
                        id: true
                      - name: name
                        type: String
                """;

        final CrudSpecification spec = mapper.readValue(yaml, CrudSpecification.class);
        assertNotNull(spec);
        assertEquals(DatabaseType.MONGODB, spec.getConfiguration().getDatabase());
        assertEquals("id", spec.getEntities().get(0).getFields().get(0).getName());
        assertEquals("String", spec.getEntities().get(0).getFields().get(0).getType());
        assertNotNull(spec.getEntities().get(0).getFields().get(0).getId());
        assertTrue(spec.getEntities().get(0).getFields().get(0).getId().isMarkerOnly());
        assertEquals(null, spec.getEntities().get(0).getFields().get(0).getId().getStrategy());
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
