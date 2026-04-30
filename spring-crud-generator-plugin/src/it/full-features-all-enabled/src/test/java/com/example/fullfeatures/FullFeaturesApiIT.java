package com.example.fullfeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

class FullFeaturesApiIT {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final String BASE_URL = System.getProperty("it.baseUrl", "http://localhost:18081");
    private static final String BASIC_AUTH_HEADER = "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes());

    @Test
    void shouldExecuteCrudAndGraphQlFlowAgainstGeneratedApp() throws Exception {

        final HttpResponse<String> createResponse = send(
                "/api/v1/users",
                "POST",
                """
                {
                  "username": "integration_user",
                  "email": "integration.user@example.com",
                  "password": "Pass1234",
                  "roles": ["ADMIN"],
                  "permissions": ["READ", "WRITE"],
                  "details": {
                    "firstName": "Integration",
                    "lastName": "User"
                  }
                }
                """
        );
        assertEquals(200, createResponse.statusCode());

        final JsonNode createdUser = OBJECT_MAPPER.readTree(createResponse.body());
        final long userId = createdUser.path("userId").asLong(-1L);
        assertTrue(userId > 0, "Expected generated userId to be > 0");
        assertEquals("integration.user@example.com", createdUser.path("email").asText());

        final HttpResponse<String> getAllResponse = send("/api/v1/users?pageNumber=0&pageSize=20", "GET", null);
        assertEquals(200, getAllResponse.statusCode());

        final JsonNode usersPage = OBJECT_MAPPER.readTree(getAllResponse.body());
        assertTrue(usersPage.path("content").isArray(), "Expected 'content' to be an array");
        assertTrue(usersPage.path("content").size() >= 1, "Expected at least one user in page content");

        final HttpResponse<String> getByIdResponse = send("/api/v1/users/" + userId, "GET", null);
        assertEquals(200, getByIdResponse.statusCode());

        final JsonNode loadedUser = OBJECT_MAPPER.readTree(getByIdResponse.body());
        assertEquals(userId, loadedUser.path("userId").asLong());
        assertEquals("integration.user@example.com", loadedUser.path("email").asText());

        final HttpResponse<String> updateResponse = send(
                "/api/v1/users/" + userId,
                "PUT",
                """
                {
                  "username": "integration_user_updated",
                  "email": "integration.updated@example.com",
                  "password": "Pass5678",
                  "roles": ["ADMIN", "MANAGER"],
                  "permissions": ["READ"],
                  "details": {
                    "firstName": "Updated",
                    "lastName": "User"
                  }
                }
                """
        );
        assertEquals(200, updateResponse.statusCode());

        final JsonNode updatedUser = OBJECT_MAPPER.readTree(updateResponse.body());
        assertEquals(userId, updatedUser.path("userId").asLong());
        assertEquals("integration.updated@example.com", updatedUser.path("email").asText());

        final HttpResponse<String> createProductResponse = send(
                "/api/v1/products",
                "POST",
                """
                {
                  "name": "Integration Product",
                  "price": 25,
                  "users": [
                    {
                      "userId": %d
                    }
                  ],
                  "uuid": "123e4567-e89b-12d3-a456-426614174000",
                  "releaseDate": "2026-03-05",
                  "details": [
                    {
                      "description": "Lightweight product",
                      "weight": 1.2,
                      "option": "DEFAULT"
                    }
                  ],
                  "status": "ACTIVE"
                }
                """.formatted(userId)
        );
        assertEquals(200, createProductResponse.statusCode());

        final JsonNode createdProduct = OBJECT_MAPPER.readTree(createProductResponse.body());
        final long productId = createdProduct.path("id").asLong(-1L);
        assertTrue(productId > 0, "Expected generated product id to be > 0");
        assertEquals("Integration Product", createdProduct.path("name").asText());

        final HttpResponse<String> createOrderResponse = send(
                "/api/v1/orders",
                "POST",
                """
                {
                  "product": {
                    "id": %d
                  },
                  "quantity": 2,
                  "users": [
                    {
                      "userId": %d
                    }
                  ]
                }
                """.formatted(productId, userId)
        );
        assertEquals(200, createOrderResponse.statusCode());

        final JsonNode createdOrder = OBJECT_MAPPER.readTree(createOrderResponse.body());
        final long orderId = createdOrder.path("orderId").asLong(-1L);
        assertTrue(orderId > 0, "Expected generated orderId to be > 0");
        assertEquals(2, createdOrder.path("quantity").asInt());

        final HttpResponse<String> graphQlResponse = send(
                "/graphql",
                "POST",
                """
                {
                  "query": "query($pageNumber:Int!, $pageSize:Int!){ usersPage(pageNumber:$pageNumber, pageSize:$pageSize){ totalElements content { userId username } } }",
                  "variables": {
                    "pageNumber": 0,
                    "pageSize": 20
                  }
                }
                """
        );
        assertEquals(200, graphQlResponse.statusCode());

        final JsonNode graphQl = OBJECT_MAPPER.readTree(graphQlResponse.body());
        assertTrue(graphQl.path("errors").isMissingNode() || graphQl.path("errors").isNull() || graphQl.path("errors").isEmpty(),
                "Expected GraphQL response without errors");
        assertTrue(graphQl.path("data").path("usersPage").path("totalElements").asLong() >= 1L,
                "Expected at least one user in GraphQL usersPage response");

        final HttpResponse<String> deleteResponse = send("/api/v1/users/" + userId, "DELETE", null);
        assertEquals(204, deleteResponse.statusCode());
    }

    private HttpResponse<String> send(final String path, final String method, final String body) throws Exception {

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json")
                .header("Authorization", BASIC_AUTH_HEADER);

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
