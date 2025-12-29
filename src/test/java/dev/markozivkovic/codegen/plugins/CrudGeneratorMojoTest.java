package dev.markozivkovic.codegen.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import dev.markozivkovic.codegen.models.GeneratorState;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.utils.GeneratorStateUtils;

class CrudGeneratorMojoTest {

    private CrudGeneratorMojo newMojo() {
        return new CrudGeneratorMojo();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectMapper invokeCreateSpecMapper(CrudGeneratorMojo mojo, String fileName) throws Exception {
        try {
            final Method m = CrudGeneratorMojo.class.getDeclaredMethod("createSpecMapper", String.class);
            m.setAccessible(true);
            return (ObjectMapper) m.invoke(mojo, fileName);
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ModelDefinition> invokeComputeEntitiesToGenerate(
            final CrudGeneratorMojo mojo,
            final List<ModelDefinition> activeEntities,
            final boolean forceRegeneration,
            final GeneratorState generatorState,
            final Map<String, String> fingerprints,
            final String configurationFingerprints) {
        try {
            Method m = CrudGeneratorMojo.class.getDeclaredMethod(
                    "computeEntitiesToGenerate",
                    List.class,
                    boolean.class,
                    GeneratorState.class,
                    Map.class,
                    String.class);
            m.setAccessible(true);
            return (List<ModelDefinition>) m.invoke(
                    mojo,
                    activeEntities,
                    forceRegeneration,
                    generatorState,
                    fingerprints,
                    configurationFingerprints);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void execute_shouldThrowWhenInputSpecFileIsNull() {
        final CrudGeneratorMojo mojo = newMojo();

        setField(mojo, "inputSpecFile", null);
        setField(mojo, "outputDir", "target/out");
        setField(mojo, "projectBaseDir", new File("."));

        final MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);

        assertTrue(ex.getMessage().contains("inputSpecFile must be specified"));
    }

    @Test
    void execute_shouldThrowWhenOutputDirIsNull() {

        final CrudGeneratorMojo mojo = newMojo();

        setField(mojo, "projectBaseDir", new File("."));

        setField(mojo, "inputSpecFile", "spec.yaml");
        setField(mojo, "outputDir", null);
        setField(mojo, "projectBaseDir", new File("."));

        final MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);

        assertTrue(ex.getMessage().contains("outputDir must be specified"));
    }

    @Test
    void createSpecMapper_yamlExtension_returnsYamlMapper() throws Exception {
        
        final CrudGeneratorMojo mojo = newMojo();

        final ObjectMapper mapper1 = invokeCreateSpecMapper(mojo, "spec.yaml");
        final ObjectMapper mapper2 = invokeCreateSpecMapper(mojo, "SPEC.YML");

        assertInstanceOf(YAMLMapper.class, mapper1);
        assertInstanceOf(YAMLMapper.class, mapper2);
    }

    @Test
    void createSpecMapper_jsonExtension_returnsJsonMapper() throws Exception {
        
        final CrudGeneratorMojo mojo = newMojo();

        final ObjectMapper mapper = invokeCreateSpecMapper(mojo, "spec.json");

        assertInstanceOf(JsonMapper.class, mapper);
    }

    @Test
    void createSpecMapper_configuresCommonFeatures_forYamlAndJson() throws Exception {
        final CrudGeneratorMojo mojo = newMojo();

        final ObjectMapper yaml = invokeCreateSpecMapper(mojo, "spec.yaml");
        final ObjectMapper json = invokeCreateSpecMapper(mojo, "spec.json");

        assertFalse(yaml.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertFalse(json.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        assertTrue(yaml.getDeserializationConfig().isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
        assertTrue(json.getDeserializationConfig().isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    @Test
    void computeEntitiesToGenerate_forceRegeneration_returnsAllActiveEntities() {
        
        final CrudGeneratorMojo mojo = newMojo();

        final ModelDefinition e1 = mock(ModelDefinition.class);
        final ModelDefinition e2 = mock(ModelDefinition.class);
        when(e1.getName()).thenReturn("User");
        when(e2.getName()).thenReturn("Order");

        final List<ModelDefinition> active = List.of(e1, e2);

        final GeneratorState generatorState = mock(GeneratorState.class);
        when(generatorState.getConfiguration()).thenReturn("config-fp");

        final Map<String, String> fingerprints = Map.of(
                "User", "fp-user",
                "Order", "fp-order"
        );

        final List<ModelDefinition> result = invokeComputeEntitiesToGenerate(
                mojo, active, true, generatorState, fingerprints, "config-fp"
        );

        assertEquals(2, result.size());
        assertTrue(result.containsAll(active));
    }

    @Test
    void computeEntitiesToGenerate_configurationChanged_returnsAllActiveEntities() {
        
        final CrudGeneratorMojo mojo = newMojo();

        final ModelDefinition e1 = mock(ModelDefinition.class);
        when(e1.getName()).thenReturn("User");

        final List<ModelDefinition> active = List.of(e1);

        final GeneratorState generatorState = mock(GeneratorState.class);
        when(generatorState.getConfiguration()).thenReturn("old-config-fp");

        final Map<String, String> fingerprints = Map.of("User", "fp-user");

        final List<ModelDefinition> result = invokeComputeEntitiesToGenerate(
                mojo, active, false, generatorState, fingerprints, "new-config-fp"
        );

        assertEquals(1, result.size());
        assertEquals(e1, result.get(0));
    }

    @Test
    void computeEntitiesToGenerate_fingerprintChanged_returnsOnlyChangedEntities() {
        
        final CrudGeneratorMojo mojo = newMojo();

        final ModelDefinition e1 = mock(ModelDefinition.class);
        final ModelDefinition e2 = mock(ModelDefinition.class);
        when(e1.getName()).thenReturn("User");
        when(e2.getName()).thenReturn("Order");

        final List<ModelDefinition> active = List.of(e1, e2);

        final GeneratorState generatorState = mock(GeneratorState.class);
        when(generatorState.getConfiguration()).thenReturn("config-fp");

        final Map<String, String> fingerprints = Map.of(
                "User", "fp-user-new",
                "Order", "fp-order-same"
        );

        try (final MockedStatic<GeneratorStateUtils> utils = Mockito.mockStatic(GeneratorStateUtils.class)) {
            utils.when(() -> GeneratorStateUtils.findPreviousFingerprint(generatorState, "User"))
                    .thenReturn(Optional.of("fp-user-old"));
            utils.when(() -> GeneratorStateUtils.findPreviousFingerprint(generatorState, "Order"))
                    .thenReturn(Optional.of("fp-order-same"));

            final List<ModelDefinition> result = invokeComputeEntitiesToGenerate(
                    mojo, active, false, generatorState, fingerprints, "config-fp"
            );

            assertEquals(1, result.size());
            assertEquals("User", result.get(0).getName());
        }
    }

    @Test
    void computeEntitiesToGenerate_noPreviousFingerprint_includesEntity() {
        
        final CrudGeneratorMojo mojo = newMojo();

        final ModelDefinition e1 = mock(ModelDefinition.class);
        when(e1.getName()).thenReturn("User");

        final List<ModelDefinition> active = List.of(e1);

        final GeneratorState generatorState = mock(GeneratorState.class);
        when(generatorState.getConfiguration()).thenReturn("config-fp");

        final Map<String, String> fingerprints = Map.of("User", "fp-user");

        try (final MockedStatic<GeneratorStateUtils> utils = Mockito.mockStatic(GeneratorStateUtils.class)) {
            utils.when(() -> GeneratorStateUtils.findPreviousFingerprint(generatorState, "User"))
                    .thenReturn(Optional.empty());

            final List<ModelDefinition> result = invokeComputeEntitiesToGenerate(
                    mojo, active, false, generatorState, fingerprints, "config-fp"
            );

            assertEquals(1, result.size());
            assertEquals("User", result.get(0).getName());
        }
    }

    @Test
    void computeEntitiesToGenerate_noChanges_returnsEmptyList() {
        
        final CrudGeneratorMojo mojo = newMojo();

        final ModelDefinition e1 = mock(ModelDefinition.class);
        final ModelDefinition e2 = mock(ModelDefinition.class);
        when(e1.getName()).thenReturn("User");
        when(e2.getName()).thenReturn("Order");

        final List<ModelDefinition> active = List.of(e1, e2);

        final GeneratorState generatorState = mock(GeneratorState.class);
        when(generatorState.getConfiguration()).thenReturn("config-fp");

        final Map<String, String> fingerprints = Map.of(
                "User", "fp-user",
                "Order", "fp-order"
        );

        try (final MockedStatic<GeneratorStateUtils> utils = Mockito.mockStatic(GeneratorStateUtils.class)) {
            utils.when(() -> GeneratorStateUtils.findPreviousFingerprint(generatorState, "User"))
                    .thenReturn(Optional.of("fp-user"));
            utils.when(() -> GeneratorStateUtils.findPreviousFingerprint(generatorState, "Order"))
                    .thenReturn(Optional.of("fp-order"));

            final List<ModelDefinition> result = invokeComputeEntitiesToGenerate(
                    mojo, active, false, generatorState, fingerprints, "config-fp"
            );

            assertTrue(result.isEmpty());
        }
    }
}
