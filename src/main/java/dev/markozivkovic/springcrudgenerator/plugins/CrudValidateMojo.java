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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.markozivkovic.springcrudgenerator.models.CrudSpecification;
import dev.markozivkovic.springcrudgenerator.utils.CrudMojoUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;
import dev.markozivkovic.springcrudgenerator.validators.PackageConfigurationValidator;
import dev.markozivkovic.springcrudgenerator.validators.SpecificationValidator;

@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE)
public class CrudValidateMojo extends AbstractMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrudValidateMojo.class);

    @Parameter(property = "inputSpecFile", required = true)
    private String inputSpecFile;

    @Parameter(defaultValue = "${project.parent.version}", readonly = true)
    private String parentVersion;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor pluginDescriptor;

    @Override
    public void execute() throws MojoExecutionException {

        if (Objects.isNull(inputSpecFile)) {
            throw new MojoExecutionException("inputSpecFile must be specified");
        }

        try {
            CrudMojoUtils.printBanner(
                    pluginDescriptor, inputSpecFile, "N/A (validate goal - dry run)"
            );
            final ObjectMapper mapper = CrudMojoUtils.createSpecMapper(inputSpecFile);
            final Path specPath = Paths.get(inputSpecFile).toAbsolutePath().normalize();

            LOGGER.info("Validation started for file: {}", specPath);

            final CrudSpecification spec = mapper.readValue(specPath.toFile(), CrudSpecification.class);
            SpecificationValidator.validate(spec);
            PackageConfigurationValidator.validate(spec.getPackages(), spec.getConfiguration());
            SpringBootVersionUtils.resolveAndSetSpringBootMajor(spec, parentVersion);

            LOGGER.info("Spec file is valid for generation: {}", specPath);
        } catch (final Exception e) {
            throw new MojoExecutionException("Spec validation failed", e);
        }
    }
}
