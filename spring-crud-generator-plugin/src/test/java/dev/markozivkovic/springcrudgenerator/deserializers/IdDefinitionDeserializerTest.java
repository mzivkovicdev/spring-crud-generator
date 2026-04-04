package dev.markozivkovic.springcrudgenerator.deserializers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import dev.markozivkovic.springcrudgenerator.models.CrudSpecification;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.springcrudgenerator.utils.CrudMojoUtils;

class IdDefinitionDeserializerTest {

    @Test
    void deserialize_booleanIdMarker_shouldSetMarkerOnlyTrue() throws Exception {

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
                """;

        final CrudSpecification spec = mapper.readValue(yaml, CrudSpecification.class);
        final var id = spec.getEntities().get(0).getFields().get(0).getId();

        assertNotNull(id);
        assertTrue(id.isMarkerOnly());
        assertNull(id.getStrategy());
    }

    @Test
    void deserialize_objectIdDefinition_shouldSetMarkerOnlyFalseAndStrategy() throws Exception {

        final ObjectMapper mapper = CrudMojoUtils.createSpecMapper("crud-spec.yaml");
        final String yaml = """
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
                          generatorName: product_id_gen
                      - name: name
                        type: String
                """;

        final CrudSpecification spec = mapper.readValue(yaml, CrudSpecification.class);
        final var id = spec.getEntities().get(0).getFields().get(0).getId();

        assertNotNull(id);
        assertFalse(id.isMarkerOnly());
        assertEquals(IdStrategyEnum.TABLE, id.getStrategy());
        assertEquals("product_id_gen", id.getGeneratorName());
    }

    @Test
    void deserialize_falseIdValue_shouldTreatIdAsNull() throws Exception {

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
                        id: false
                """;

        final CrudSpecification spec = mapper.readValue(yaml, CrudSpecification.class);
        assertNull(spec.getEntities().get(0).getFields().get(0).getId());
    }

    @Test
    void deserialize_invalidScalar_shouldThrowMismatchedInputException() {

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
                        id: 123
                """;

        assertThrows(MismatchedInputException.class, () -> mapper.readValue(yaml, CrudSpecification.class));
    }
}
