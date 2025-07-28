package com.markozivkovic.codegen;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.markozivkovic.codegen.model.CrudSpecification;

/**
 * Hello world!
 *
 */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(final String[] args ) {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            final CrudSpecification spec = mapper.readValue(
                    new File("src/main/resources/crud-spec.yaml"), CrudSpecification.class
            );
            final SpringCrudGenerator generator = new SpringCrudGenerator();
            spec.getEntities().stream().forEach(entity -> {
                    generator.generate(entity);
            });
        } catch (IOException e) {
            LOGGER.error("Error occurred during generating files. ", e);
        }
    }
}
