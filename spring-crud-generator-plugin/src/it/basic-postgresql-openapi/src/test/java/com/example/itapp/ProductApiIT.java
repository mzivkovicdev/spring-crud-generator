package com.example.itapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ProductApiIT {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BASE_URL = System.getProperty("it.baseUrl", "http://localhost:18080");

    @Test
    void shouldExecuteCrudFlowAgainstGeneratedEndpoints() throws Exception {

        final HttpResponse<String> createResponse = send(
                "/api/products",
                "POST",
                """
                {
                  "name": "Product From IT"
                }
                """
        );
        assertEquals(200, createResponse.statusCode());

        final JsonNode created = OBJECT_MAPPER.readTree(createResponse.body());
        final long productId = created.path("id").asLong(-1L);
        assertTrue(productId > 0, "Expected generated ID to be > 0");
        assertEquals("Product From IT", created.path("name").asText());

        final HttpResponse<String> getAllResponse = send("/api/products?pageNumber=0&pageSize=20", "GET", null);
        assertEquals(200, getAllResponse.statusCode());

        final JsonNode page = OBJECT_MAPPER.readTree(getAllResponse.body());
        assertTrue(page.path("content").isArray(), "Expected 'content' to be an array");
        assertTrue(page.path("content").size() >= 1, "Expected at least one product in the page");

        final HttpResponse<String> getByIdResponse = send("/api/products/" + productId, "GET", null);
        assertEquals(200, getByIdResponse.statusCode());

        final JsonNode loaded = OBJECT_MAPPER.readTree(getByIdResponse.body());
        assertEquals(productId, loaded.path("id").asLong());
        assertEquals("Product From IT", loaded.path("name").asText());

        final HttpResponse<String> updateResponse = send(
                "/api/products/" + productId,
                "PUT",
                """
                {
                  "name": "Updated Product From IT"
                }
                """
        );
        assertEquals(200, updateResponse.statusCode());

        final JsonNode updated = OBJECT_MAPPER.readTree(updateResponse.body());
        assertEquals(productId, updated.path("id").asLong());
        assertEquals("Updated Product From IT", updated.path("name").asText());

        final HttpResponse<String> deleteResponse = send("/api/products/" + productId, "DELETE", null);
        assertEquals(204, deleteResponse.statusCode());
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
