package com.markozivkovic.codegen.plugins;

import java.io.File;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.markozivkovic.codegen.generators.SpringCrudGenerator;
import com.markozivkovic.codegen.model.CrudSpecification;
import com.markozivkovic.codegen.model.ProjectMetadata;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CrudGeneratorMojo extends AbstractMojo {
    
    @Parameter(property = "inputSpecFile", required = true)
    private String inputSpecFile;

    @Parameter(property = "outputDir", required = true)
    private String outputDir;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true)
    private String artifactId;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String version;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private String projectBaseDir;

    public void execute() throws MojoExecutionException {

        if (Objects.isNull(inputSpecFile)) {
            throw new MojoExecutionException("inputSpecFile must be specified");
        }
        
        if (Objects.isNull(outputDir)) {
            throw new MojoExecutionException("outputDir must be specified");
        }
        
        try {
            final YAMLMapper yamlMapper = YAMLMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                    .build();
            
            final CrudSpecification spec = yamlMapper.readValue(new File(inputSpecFile), CrudSpecification.class);
            final ProjectMetadata projectMetadata = new ProjectMetadata(artifactId, version, projectBaseDir);
            final SpringCrudGenerator generator = new SpringCrudGenerator(spec.getConfiguration(), spec.getEntities(), projectMetadata);

            spec.getEntities().stream().forEach(entity -> {
                    generator.generate(entity, outputDir);
            });
        } catch (final Exception e) {
            throw new MojoExecutionException("Code generation failed", e);
        }
    }

}
