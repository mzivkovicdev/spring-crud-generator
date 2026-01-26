package dev.markozivkovic.springcrudgenerator.generators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;

class SpringCrudGeneratorTest {

    @SuppressWarnings("unchecked")
    @Test
    void generate_shouldDelegateToAllProjectArtifactGenerators() throws Exception {

        final CrudConfiguration crudConfiguration = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final List<ModelDefinition> entities = List.of();

        final SpringCrudGenerator generator = new SpringCrudGenerator(crudConfiguration, entities, projectMetadata, packageConfiguration);

        final Field artifactField = SpringCrudGenerator.class.getDeclaredField("ARTIFACT_GENERATORS");
        artifactField.setAccessible(true);

        final Map<String, ProjectArtifactGenerator> artifactGenerators =
                (Map<String, ProjectArtifactGenerator>) artifactField.get(generator);

        final List<ProjectArtifactGenerator> mocks = new ArrayList<>();

        List.copyOf(artifactGenerators.keySet()).forEach(key -> {
            final ProjectArtifactGenerator mockGen = mock(ProjectArtifactGenerator.class);
            artifactGenerators.put(key, mockGen);
            mocks.add(mockGen);
        });

        final String outputDir = "out";

        generator.generate(outputDir);

        mocks.forEach(g -> verify(g).generate(outputDir));
    }

    @SuppressWarnings("unchecked")
    @Test
    void generate_shouldDelegateToAllCodeGeneratorsForGivenModel() throws Exception {

        final CrudConfiguration crudConfiguration = mock(CrudConfiguration.class);
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final PackageConfiguration packageConfiguration = mock(PackageConfiguration.class);
        final List<ModelDefinition> entities = List.of();

        final SpringCrudGenerator generator =
                new SpringCrudGenerator(crudConfiguration, entities, projectMetadata, packageConfiguration);

        final ModelDefinition model = mock(ModelDefinition.class);
        when(model.getName()).thenReturn("UserEntity");

        final Field genField = SpringCrudGenerator.class.getDeclaredField("GENERATORS");
        genField.setAccessible(true);

        final Map<String, CodeGenerator> codeGenerators =
                (Map<String, CodeGenerator>) genField.get(generator);

        final List<CodeGenerator> mocks = new ArrayList<>();
        List.copyOf(codeGenerators.keySet()).forEach(key -> {
            final CodeGenerator mockGen = mock(CodeGenerator.class);
            codeGenerators.put(key, mockGen);
            mocks.add(mockGen);
        });

        final String outputDir = "out";

        generator.generate(model, outputDir);

        mocks.forEach(g -> verify(g).generate(model, outputDir));
    }
}
