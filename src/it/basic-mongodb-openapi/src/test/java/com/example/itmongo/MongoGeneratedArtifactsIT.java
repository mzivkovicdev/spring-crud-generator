package com.example.itmongo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class MongoGeneratedArtifactsIT {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BASE_URL = System.getProperty("it.baseUrl", "http://localhost:18082");

    @Test
    void shouldExecuteCrudFlowAgainstGeneratedMongoEndpoints() throws Exception {

        final HttpResponse<String> createUserResponse = send(
                "/api/users",
                "POST",
                """
                {
                  "email": "mongo.user.it@example.com"
                }
                """
        );
        assertEquals(200, createUserResponse.statusCode());

        final JsonNode createdUser = OBJECT_MAPPER.readTree(createUserResponse.body());
        final String userId = createdUser.path("id").asText();
        assertTrue(!userId.isBlank(), "Expected generated Mongo user id to be present");
        assertEquals("mongo.user.it@example.com", createdUser.path("email").asText());

        final HttpResponse<String> createProductResponse = send(
                "/api/products",
                "POST",
                """
                {
                  "name": "Mongo Product IT",
                  "users": [
                    {
                      "id": "%s"
                    }
                  ]
                }
                """.formatted(userId)
        );
        assertEquals(200, createProductResponse.statusCode());

        final JsonNode createdProduct = OBJECT_MAPPER.readTree(createProductResponse.body());
        final String productId = createdProduct.path("id").asText();
        assertTrue(!productId.isBlank(), "Expected generated Mongo product id to be present");
        assertEquals("Mongo Product IT", createdProduct.path("name").asText());
        assertTrue(createdProduct.path("users").isArray());
        assertTrue(createdProduct.path("users").size() >= 1, "Expected at least one related user reference");
        assertEquals(userId, createdProduct.path("users").get(0).path("id").asText());

        final HttpResponse<String> getAllProductsResponse = send("/api/products?pageNumber=0&pageSize=20", "GET", null);
        assertEquals(200, getAllProductsResponse.statusCode());

        final JsonNode productsPage = OBJECT_MAPPER.readTree(getAllProductsResponse.body());
        assertTrue(productsPage.path("content").isArray(), "Expected paged 'content' array");
        assertTrue(productsPage.path("content").size() >= 1, "Expected at least one product");

        final HttpResponse<String> getProductByIdResponse = send("/api/products/" + productId, "GET", null);
        assertEquals(200, getProductByIdResponse.statusCode());

        final JsonNode loadedProduct = OBJECT_MAPPER.readTree(getProductByIdResponse.body());
        assertEquals(productId, loadedProduct.path("id").asText());
        assertEquals("Mongo Product IT", loadedProduct.path("name").asText());

        final HttpResponse<String> updateProductResponse = send(
                "/api/products/" + productId,
                "PUT",
                """
                {
                  "name": "Mongo Product Updated IT",
                  "users": [
                    {
                      "id": "%s"
                    }
                  ]
                }
                """.formatted(userId)
        );
        assertEquals(200, updateProductResponse.statusCode());

        final JsonNode updatedProduct = OBJECT_MAPPER.readTree(updateProductResponse.body());
        assertEquals(productId, updatedProduct.path("id").asText());
        assertEquals("Mongo Product Updated IT", updatedProduct.path("name").asText());

        final HttpResponse<String> deleteProductResponse = send("/api/products/" + productId, "DELETE", null);
        assertEquals(204, deleteProductResponse.statusCode());
    }

    @Test
    void shouldGenerateMongoArtifactsForCrudFlow() throws Exception {

        final Path basedir = Path.of(System.getProperty("user.dir"));
        final Path modelFile = basedir.resolve("src/main/java/com/example/itmongo/models/ProductModel.java");
        final Path repositoryFile = basedir.resolve("src/main/java/com/example/itmongo/repositories/ProductRepository.java");
        final Path swaggerFile = basedir.resolve("src/main/resources/swagger/product-api.yaml");
        final Path migrationDirectory = basedir.resolve("src/main/resources/db/migration");
        final Path productsMigration = basedir.resolve("src/main/java/com/example/itmongo/migration/V001__Create_Products_Collection.java");
        final Path usersMigration = basedir.resolve("src/main/java/com/example/itmongo/migration/V002__Create_Users_Collection.java");

        assertTrue(Files.isRegularFile(modelFile), "Mongo model must be generated");
        assertTrue(Files.isRegularFile(repositoryFile), "Mongo repository must be generated");
        assertTrue(Files.isRegularFile(swaggerFile), "OpenAPI YAML must be generated");
        assertFalse(Files.exists(migrationDirectory), "SQL migration directory must not be generated for MongoDB");
        assertTrue(Files.isRegularFile(productsMigration), "Mongock create-products migration must be generated");
        assertTrue(Files.isRegularFile(usersMigration), "Mongock create-users migration must be generated");

        final String modelContent = Files.readString(modelFile);
        assertTrue(modelContent.contains("@Document(collection = \"products\")"));
        assertTrue(modelContent.contains("@DBRef"));
        assertTrue(modelContent.contains("@Id"));

        final String repositoryContent = Files.readString(repositoryFile);
        assertTrue(repositoryContent.contains("extends MongoRepository<ProductModel, String>"));
        assertTrue(repositoryContent.contains("saveAndFlush"));

        final String productsMigrationContent = Files.readString(productsMigration);
        assertTrue(productsMigrationContent.contains("@ChangeUnit"));
        assertTrue(productsMigrationContent.contains("mongoTemplate.createCollection(\"products\")"));

        final String usersMigrationContent = Files.readString(usersMigration);
        assertTrue(usersMigrationContent.contains("@ChangeUnit"));
        assertTrue(usersMigrationContent.contains("mongoTemplate.createCollection(\"users\")"));
        assertTrue(usersMigrationContent.contains("ensureIndex"));
    }

    private HttpResponse<String> send(final String path, final String method, final String body) throws Exception {

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json");

        if (body != null) {
            requestBuilder.header("Content-Type", "application/json");
        }

        final HttpRequest request = switch (method) {
            case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body)).build();
            case "DELETE" -> requestBuilder.DELETE().build();
            default -> requestBuilder.GET().build();
        };

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
