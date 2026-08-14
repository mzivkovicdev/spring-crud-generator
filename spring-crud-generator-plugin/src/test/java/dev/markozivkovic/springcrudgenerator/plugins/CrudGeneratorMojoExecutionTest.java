/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.generators.SpringCrudGenerator;
import dev.markozivkovic.springcrudgenerator.generators.tests.SpringCrudTestGenerator;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudSpecification;
import dev.markozivkovic.springcrudgenerator.models.GeneratorState;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.CrudMojoUtils;
import dev.markozivkovic.springcrudgenerator.utils.DependencyCheckUtils;
import dev.markozivkovic.springcrudgenerator.utils.GeneratorStateUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.validators.PackageConfigurationValidator;
import dev.markozivkovic.springcrudgenerator.validators.SpecificationValidator;

class CrudGeneratorMojoExecutionTest {

    @Test
    void execute_whenOneEntityChanged_passesCompleteActiveModelGraphToGenerators() throws Exception {
        final ModelDefinition changedEntity = new ModelDefinition()
                .setName("OrderEntity")
                .setFields(List.of());
        final ModelDefinition unchangedEntity = new ModelDefinition()
                .setName("CustomerEntity")
                .setFields(List.of());
        final List<ModelDefinition> activeEntities = List.of(changedEntity, unchangedEntity);
        final CrudConfiguration configuration = new CrudConfiguration();
        final PackageConfiguration packageConfiguration = new PackageConfiguration();
        final CrudSpecification specification = new CrudSpecification(
                configuration,
                activeEntities,
                packageConfiguration);
        final GeneratorState generatorState = new GeneratorState()
                .setConfiguration("configuration-fingerprint");
        final ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.readValue(any(File.class), eq(CrudSpecification.class))).thenReturn(specification);

        final File projectBaseDirectory = new File("target/mojo-execution-test").getAbsoluteFile();
        final CrudGeneratorMojo mojo = configuredMojo(projectBaseDirectory);

        try (final MockedStatic<CrudMojoUtils> crudMojoUtils = mockStatic(CrudMojoUtils.class);
             final MockedStatic<DependencyCheckUtils> dependencyCheckUtils = mockStatic(DependencyCheckUtils.class);
             final MockedStatic<GeneratorStateUtils> generatorStateUtils = mockStatic(GeneratorStateUtils.class);
             final MockedStatic<PackageConfigurationValidator> packageValidator = mockStatic(PackageConfigurationValidator.class);
             final MockedStatic<SpecificationValidator> specificationValidator = mockStatic(SpecificationValidator.class);
             final MockedStatic<SpringBootVersionUtils> versionUtils = mockStatic(SpringBootVersionUtils.class);
             final MockedConstruction<SpringCrudGenerator> generatorConstruction = mockConstruction(
                     SpringCrudGenerator.class,
                     (generator, context) -> assertEquals(activeEntities, context.arguments().get(1)));
             final MockedConstruction<SpringCrudTestGenerator> testGeneratorConstruction = mockConstruction(
                     SpringCrudTestGenerator.class,
                     (generator, context) -> assertEquals(activeEntities, context.arguments().get(1)))) {

            crudMojoUtils.when(() -> CrudMojoUtils.createSpecMapper("spec.yaml")).thenReturn(mapper);
            generatorStateUtils.when(() -> GeneratorStateUtils.loadOrEmpty(projectBaseDirectory.getAbsolutePath()))
                    .thenReturn(generatorState);
            generatorStateUtils.when(() -> GeneratorStateUtils.computeFingerprint(changedEntity))
                    .thenReturn("changed-fingerprint");
            generatorStateUtils.when(() -> GeneratorStateUtils.computeFingerprint(unchangedEntity))
                    .thenReturn("unchanged-fingerprint");
            generatorStateUtils.when(() -> GeneratorStateUtils.computeFingerprint(configuration))
                    .thenReturn("configuration-fingerprint");
            generatorStateUtils.when(() -> GeneratorStateUtils.findPreviousFingerprint(generatorState, "OrderEntity"))
                    .thenReturn(Optional.empty());
            generatorStateUtils.when(() -> GeneratorStateUtils.findPreviousFingerprint(generatorState, "CustomerEntity"))
                    .thenReturn(Optional.of("unchanged-fingerprint"));

            mojo.execute();

            final SpringCrudGenerator generator = generatorConstruction.constructed().get(0);
            final SpringCrudTestGenerator testGenerator = testGeneratorConstruction.constructed().get(0);
            verify(generator).generate("target/generated-sources");
            verify(generator).generate(changedEntity, "target/generated-sources");
            verify(generator, never()).generate(unchangedEntity, "target/generated-sources");
            verify(testGenerator).generate(changedEntity, "target/generated-sources");
            verify(testGenerator, never()).generate(unchangedEntity, "target/generated-sources");
        }
    }

    private static CrudGeneratorMojo configuredMojo(final File projectBaseDirectory) {
        final CrudGeneratorMojo mojo = new CrudGeneratorMojo();
        setField(mojo, "inputSpecFile", "spec.yaml");
        setField(mojo, "outputDir", "target/generated-sources");
        setField(mojo, "artifactId", "test-artifact");
        setField(mojo, "version", "1.0.0");
        setField(mojo, "projectBaseDir", projectBaseDirectory);
        setField(mojo, "parentVersion", "3.5.0");
        setField(mojo, "pluginDescriptor", mock(PluginDescriptor.class));
        setField(mojo, "project", mock(MavenProject.class));
        return mojo;
    }

    private static void setField(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("Could not configure test fixture field: " + fieldName, exception);
        }
    }
}
