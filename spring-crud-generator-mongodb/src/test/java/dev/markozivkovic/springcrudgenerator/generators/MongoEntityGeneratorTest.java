package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;

class MongoEntityGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generate_shouldCreateMongoDocumentWithDbRefRelation() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/mongo");
        Files.createDirectories(outputDir);

        final ModelDefinition user = new ModelDefinition()
                .setName("UserEntity")
                .setStorageName("users")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("email").setType("String")
                ));

        final ModelDefinition product = new ModelDefinition()
                .setName("ProductModel")
                .setStorageName("products")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("name").setType("String"),
                        new FieldDefinition()
                                .setName("users")
                                .setType("UserEntity")
                                .setRelation(new RelationDefinition().setType("OneToMany"))
                ));

        final CrudConfiguration configuration = new CrudConfiguration().setDatabase(DatabaseType.MONGODB);
        final MongoEntityGenerator generator = new MongoEntityGenerator(
                configuration, List.of(product, user), new PackageConfiguration()
        );

        generator.generate(product, outputDir.toAbsolutePath().toString());

        final Path modelFile = outputDir.resolve("models/ProductModel.java");
        assertTrue(Files.exists(modelFile));

        final String content = Files.readString(modelFile);
        assertTrue(content.contains("package com.example.mongo.models;"));
        assertTrue(content.contains("@Document(collection = \"products\")"));
        assertTrue(content.contains("@DBRef"));
        assertTrue(content.contains("private List<UserEntity> users"));
        assertTrue(content.contains("new ArrayList<>()"));
        assertTrue(content.contains("@Id"));
        assertFalse(content.contains("@Entity"));
        assertFalse(content.contains("JpaRepository"));
    }

    @Test
    void generate_shouldCreateHelperClassForJsonField() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/mongojson");
        Files.createDirectories(outputDir);

        final ModelDefinition address = new ModelDefinition()
                .setName("Address")
                .setFields(List.of(new FieldDefinition().setName("city").setType("String")));

        final ModelDefinition order = new ModelDefinition()
                .setName("OrderModel")
                .setStorageName("orders")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("shippingAddress").setType("JSON<Address>")
                ));

        final CrudConfiguration configuration = new CrudConfiguration().setDatabase(DatabaseType.MONGODB);
        final MongoEntityGenerator generator = new MongoEntityGenerator(
                configuration, List.of(order, address), new PackageConfiguration()
        );

        generator.generate(order, outputDir.toAbsolutePath().toString());

        final Path helperFile = outputDir.resolve("models/helpers/Address.java");
        final Path modelFile = outputDir.resolve("models/OrderModel.java");

        assertTrue(Files.exists(helperFile));
        assertTrue(Files.exists(modelFile));

        final String modelContent = Files.readString(modelFile);
        assertTrue(modelContent.contains("private Address shippingAddress"));
    }

    @Test
    void generate_shouldIncludeVersionField_whenOptimisticLockingEnabled() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/optlock");
        Files.createDirectories(outputDir);

        final ModelDefinition product = new ModelDefinition()
                .setName("ProductModel")
                .setStorageName("products")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("name").setType("String")
                ));

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.MONGODB)
                .setOptimisticLocking(true);
        final MongoEntityGenerator generator = new MongoEntityGenerator(
                configuration, List.of(product), new PackageConfiguration()
        );

        generator.generate(product, outputDir.toAbsolutePath().toString());

        final String content = Files.readString(outputDir.resolve("models/ProductModel.java"));
        assertTrue(content.contains("@Version"), "Document should contain @Version annotation");
        assertTrue(content.contains("private Long version"), "Document should contain version field");
        assertTrue(content.contains("import org.springframework.data.annotation.Version"),
                "Document should import Spring Data @Version");
    }

    @Test
    void generate_shouldNotIncludeVersionField_whenOptimisticLockingDisabled() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/nooptlock");
        Files.createDirectories(outputDir);

        final ModelDefinition product = new ModelDefinition()
                .setName("ProductModel")
                .setStorageName("products")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("name").setType("String")
                ));

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.MONGODB)
                .setOptimisticLocking(false);
        final MongoEntityGenerator generator = new MongoEntityGenerator(
                configuration, List.of(product), new PackageConfiguration()
        );

        generator.generate(product, outputDir.toAbsolutePath().toString());

        final String content = Files.readString(outputDir.resolve("models/ProductModel.java"));
        assertFalse(content.contains("@Version"), "Document should NOT contain @Version when optimistic locking is disabled");
        assertFalse(content.contains("private Long version"), "Document should NOT contain version field");
    }

    @Test
    void generate_shouldNotIncludeVersionFieldInEmbeddedHelper_whenOptimisticLockingEnabled() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/embeddedoptlock");
        Files.createDirectories(outputDir);

        final ModelDefinition address = new ModelDefinition()
                .setName("Address")
                .setFields(List.of(new FieldDefinition().setName("city").setType("String")));

        final ModelDefinition order = new ModelDefinition()
                .setName("OrderModel")
                .setStorageName("orders")
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("shippingAddress").setType("JSON<Address>")
                ));

        final CrudConfiguration configuration = new CrudConfiguration()
                .setDatabase(DatabaseType.MONGODB)
                .setOptimisticLocking(true);
        final MongoEntityGenerator generator = new MongoEntityGenerator(
                configuration, List.of(order, address), new PackageConfiguration()
        );

        generator.generate(order, outputDir.toAbsolutePath().toString());

        final String helperContent = Files.readString(outputDir.resolve("models/helpers/Address.java"));
        assertFalse(helperContent.contains("@Version"),
                "Embedded helper class should NOT contain @Version even when optimistic locking is enabled");
        assertFalse(helperContent.contains("private Long version"),
                "Embedded helper class should NOT contain version field");

        final String documentContent = Files.readString(outputDir.resolve("models/OrderModel.java"));
        assertTrue(documentContent.contains("@Version"),
                "Main document should still contain @Version when optimistic locking is enabled");
    }

    @Test
    void generate_shouldIncludeSoftDeleteFieldAndAccessors_whenSoftDeleteEnabled() throws Exception {

        final Path outputDir = tempDir.resolve("src/main/java/com/example/softdelete");
        Files.createDirectories(outputDir);

        final ModelDefinition product = new ModelDefinition()
                .setName("ProductModel")
                .setStorageName("products")
                .setSoftDelete(true)
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition().setName("name").setType("String")
                ));

        final CrudConfiguration configuration = new CrudConfiguration().setDatabase(DatabaseType.MONGODB);
        final MongoEntityGenerator generator = new MongoEntityGenerator(
                configuration, List.of(product), new PackageConfiguration()
        );

        generator.generate(product, outputDir.toAbsolutePath().toString());

        final String content = Files.readString(outputDir.resolve("models/ProductModel.java"));
        assertTrue(content.contains("private boolean deleted = Boolean.FALSE;"),
                "Document should contain soft-delete field");
        assertTrue(content.contains("public boolean getDeleted()"),
                "Document should contain soft-delete getter");
        assertTrue(content.contains("public ProductModel setDeleted(final boolean deleted)"),
                "Document should contain soft-delete setter");
    }
}
