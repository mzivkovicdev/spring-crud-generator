package dev.markozivkovic.springcrudgenerator.generators.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.generators.CodeGenerator;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;

class SpringCrudTestGeneratorTest {

    @Test
    void generate_shouldDelegateToAllInnerGenerators() throws Exception {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final List<ModelDefinition> entities = List.of();

        final SpringCrudTestGenerator generator = new SpringCrudTestGenerator(cfg, entities, pkgCfg);

        final CodeGenerator jpaServiceTestGen = mock(CodeGenerator.class);
        final CodeGenerator businessServiceTestGen = mock(CodeGenerator.class);
        final CodeGenerator mapperTestGen = mock(CodeGenerator.class);
        final CodeGenerator controllerTestGen = mock(CodeGenerator.class);
        final CodeGenerator graphQlTestGen = mock(CodeGenerator.class);

        final Map<String, CodeGenerator> mockedGenerators = new LinkedHashMap<>();
        mockedGenerators.put("jpa-service-test", jpaServiceTestGen);
        mockedGenerators.put("business-service-test", businessServiceTestGen);
        mockedGenerators.put("mapper-test", mapperTestGen);
        mockedGenerators.put("controller-test", controllerTestGen);
        mockedGenerators.put("graphql-test", graphQlTestGen);

        final Field field = SpringCrudTestGenerator.class.getDeclaredField("GENERATORS");
        field.setAccessible(true);
        field.set(generator, mockedGenerators);

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("UserEntity");

        final String outputDir = "out";

        generator.generate(model, outputDir);

        verify(jpaServiceTestGen).generate(model, outputDir);
        verify(businessServiceTestGen).generate(model, outputDir);
        verify(mapperTestGen).generate(model, outputDir);
        verify(controllerTestGen).generate(model, outputDir);
        verify(graphQlTestGen).generate(model, outputDir);

        verifyNoMoreInteractions(jpaServiceTestGen, businessServiceTestGen,
                mapperTestGen, controllerTestGen, graphQlTestGen);
    }
}
