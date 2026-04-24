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
    void generate_shouldCreateMongoRepository() throws Exception {

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
    }

    @Test
    void generate_shouldNotIncludeSoftDeleteMethodsWhenSoftDeleteDisabled() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/mongo-nosd");
        Files.createDirectories(outputDir);

        final ModelDefinition model = new ModelDefinition()
                .setName("UserEntity")
                .setSoftDelete(Boolean.FALSE)
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String")
                ));

        final MongoRepositoryGenerator generator = new MongoRepositoryGenerator(new PackageConfiguration());
        generator.generate(model, outputDir.toAbsolutePath().toString());

        final String content = Files.readString(outputDir.resolve("repositories/UserRepository.java"));
        assertFalse(content.contains("findByIdAndDeletedFalse"));
        assertFalse(content.contains("findAllByDeletedFalse"));
        assertFalse(content.contains("import java.util.Optional"));
        assertFalse(content.contains("import org.springframework.data.domain.Page;"));
        assertFalse(content.contains("import org.springframework.data.domain.Pageable"));
    }

    @Test
    void generate_shouldIncludeSoftDeleteMethodsWhenSoftDeleteEnabled() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/mongo-sd");
        Files.createDirectories(outputDir);

        final ModelDefinition model = new ModelDefinition()
                .setName("UserEntity")
                .setSoftDelete(Boolean.TRUE)
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String")
                ));

        final MongoRepositoryGenerator generator = new MongoRepositoryGenerator(new PackageConfiguration());
        generator.generate(model, outputDir.toAbsolutePath().toString());

        final String content = Files.readString(outputDir.resolve("repositories/UserRepository.java"));
        final String nl = System.lineSeparator();
        assertTrue(content.contains("Optional<UserEntity> findByIdAndDeletedFalse(String id)"));
        assertTrue(content.contains("Page<UserEntity> findAllByDeletedFalse(Pageable pageable)"));
        assertTrue(content.contains("import java.util.Optional"));
        assertTrue(content.contains("import org.springframework.data.domain.Page;"));
        assertTrue(content.contains("import org.springframework.data.domain.Pageable"));
        final String pattern = "(?s).*import java\\.util\\.Optional;" + nl + nl
                + "import org\\.springframework\\.data\\.domain\\.Page;" + nl
                + "import org\\.springframework\\.data\\.domain\\.Pageable;" + nl
                + "import org\\.springframework\\.data\\.mongodb\\.repository\\.MongoRepository;" + nl + nl
                + "import [^;]+UserEntity;.*";
        assertTrue(content.matches(pattern), "Generated imports:\n" + content);
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
