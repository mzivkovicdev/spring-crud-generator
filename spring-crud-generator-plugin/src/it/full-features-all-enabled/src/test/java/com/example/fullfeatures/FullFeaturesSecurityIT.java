package com.example.fullfeatures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class FullFeaturesSecurityIT {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String BASE_URL = System.getProperty("it.baseUrl", "http://localhost:18081");
    private static final String VALID_ADMIN_AUTH =
            "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
    private static final String INVALID_AUTH =
            "Basic " + Base64.getEncoder().encodeToString("admin:wrong-password".getBytes(StandardCharsets.UTF_8));

    @Test
    void shouldRequireAuthenticationAndAllowAccessWithValidBasicAuth() throws Exception {

        final HttpResponse<String> withoutAuth = send("/api/v1/users?pageNumber=0&pageSize=1", null);
        assertEquals(401, withoutAuth.statusCode(), "Expected 401 when request has no Authorization header");

        final HttpResponse<String> withInvalidAuth = send("/api/v1/users?pageNumber=0&pageSize=1", INVALID_AUTH);
        assertEquals(401, withInvalidAuth.statusCode(), "Expected 401 when credentials are invalid");

        final HttpResponse<String> withValidAuth = send("/api/v1/users?pageNumber=0&pageSize=1", VALID_ADMIN_AUTH);
        assertEquals(200, withValidAuth.statusCode(), "Expected 200 when valid basic auth credentials are provided");
    }

    private HttpResponse<String> send(final String path, final String authHeader) throws Exception {

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Accept", "application/json");

        if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader);
        }

        final HttpRequest request = requestBuilder.GET().build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

