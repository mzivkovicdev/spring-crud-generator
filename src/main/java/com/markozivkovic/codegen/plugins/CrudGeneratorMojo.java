package com.markozivkovic.codegen.plugins;

import java.io.File;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.markozivkovic.codegen.generators.SpringCrudGenerator;
import com.markozivkovic.codegen.model.CrudSpecification;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CrudGeneratorMojo extends AbstractMojo {
    
    @Parameter(property = "inputSpecFile", required = true)
    private String inputSpecFile;

    @Parameter(property = "outputDir", required = true)
    private String outputDir;

    public void execute() throws MojoExecutionException {

        if (Objects.isNull(inputSpecFile)) {
            throw new MojoExecutionException("inputSpecFile must be specified");
        }
        
        if (Objects.isNull(outputDir)) {
            throw new MojoExecutionException("outputDir must be specified");
        }
        
        try {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            final CrudSpecification spec = mapper.readValue(
                    new File(inputSpecFile), CrudSpecification.class
            );

            final SpringCrudGenerator generator = new SpringCrudGenerator(spec.getEntities());

            spec.getEntities().stream().forEach(entity -> {
                    generator.generate(entity, outputDir);
            });
        } catch (final Exception e) {
            throw new MojoExecutionException("Code generation failed", e);
        }
    }

}
