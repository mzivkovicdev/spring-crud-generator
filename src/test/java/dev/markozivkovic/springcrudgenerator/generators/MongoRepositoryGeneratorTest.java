package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;

class MongoRepositoryGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generate_shouldCreateMongoRepositoryWithSaveAndFlushBridge() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/mongo");
        Files.createDirectories(outputDir);

        final ModelDefinition model = new ModelDefinition()
                .setName("UserEntity")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("name").setType("String")
                ));

        final MongoRepositoryGenerator generator = new MongoRepositoryGenerator(new PackageConfiguration());
        generator.generate(model, outputDir.toAbsolutePath().toString());

        final Path repositoryFile = outputDir.resolve("repositories/UserRepository.java");
        assertTrue(Files.exists(repositoryFile));

        final String content = Files.readString(repositoryFile);
        assertTrue(content.contains("package com.example.mongo.repositories;"));
        assertTrue(content.contains("extends MongoRepository<UserEntity, String>"));
        assertTrue(content.contains("default UserEntity saveAndFlush(final UserEntity entity)"));
        assertTrue(content.contains("return this.save(entity);"));
    }

    @Test
    void generate_shouldSkipWhenIdFieldMissing() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/mongo-skip");
        Files.createDirectories(outputDir);

        final ModelDefinition model = new ModelDefinition()
                .setName("UserEntity")
                .setFields(List.of(new FieldDefinition().setName("name").setType("String")));

        final MongoRepositoryGenerator generator = new MongoRepositoryGenerator(new PackageConfiguration());
        generator.generate(model, outputDir.toAbsolutePath().toString());

        final Path repositoryFile = outputDir.resolve("repositories/UserRepository.java");
        assertFalse(Files.exists(repositoryFile));
    }
}
