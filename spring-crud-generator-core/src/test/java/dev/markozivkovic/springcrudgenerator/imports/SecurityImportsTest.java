/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecurityImportsTest {

    @Test
    @DisplayName("getBasicAuthSecurityConfigImports: contains java.util and spring security imports")
    void getBasicAuthSecurityConfigImports_containsExpectedImports() {
        final String result = SecurityImports.getBasicAuthSecurityConfigImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import java.util.ArrayList;"));
        assertTrue(result.contains("import java.util.List;"));
        assertTrue(result.contains("import org.springframework.context.annotation.Bean;"));
        assertTrue(result.contains("import org.springframework.context.annotation.Configuration;"));
        assertTrue(result.contains("import org.springframework.security.core.userdetails.User;"));
        assertTrue(result.contains("import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;"));
        assertTrue(result.contains("import org.springframework.security.provisioning.InMemoryUserDetailsManager;"));
        assertTrue(result.contains("import org.springframework.security.web.SecurityFilterChain;"));
    }

    @Test
    @DisplayName("getBasicAuthSecurityConfigImports: java group appears before org group with blank line between")
    void getBasicAuthSecurityConfigImports_groupOrder() {
        final String result = SecurityImports.getBasicAuthSecurityConfigImports();

        final int javaPos = result.indexOf("import java.");
        final int orgPos = result.indexOf("import org.");
        assertTrue(javaPos < orgPos, "java.* imports should appear before org.* imports");

        // blank line separates the groups
        final String separator = System.lineSeparator();
        assertTrue(result.contains("import java.util.List;" + separator + separator + "import org."),
                "Expected blank line between java and org import groups");
    }

    @Test
    @DisplayName("getJwtSecurityConfigImports: contains JWT-specific spring security imports")
    void getJwtSecurityConfigImports_containsExpectedImports() {
        final String result = SecurityImports.getJwtSecurityConfigImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import org.springframework.context.annotation.Bean;"));
        assertTrue(result.contains("import org.springframework.security.authentication.AuthenticationManager;"));
        assertTrue(result.contains("import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;"));
        assertTrue(result.contains("import org.springframework.security.web.SecurityFilterChain;"));
        assertTrue(result.contains("import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;"));
    }

    @Test
    @DisplayName("getJwtSecurityConfigImports: does NOT contain basic-auth-only imports")
    void getJwtSecurityConfigImports_excludesBasicAuthImports() {
        final String result = SecurityImports.getJwtSecurityConfigImports();

        assertFalse(result.contains("import java.util.ArrayList;"));
        assertFalse(result.contains("import org.springframework.security.core.userdetails.User;"));
        assertFalse(result.contains("import org.springframework.security.provisioning.InMemoryUserDetailsManager;"));
    }

    @Test
    @DisplayName("getOAuth2SecurityConfigImports: contains oauth2 resource server imports")
    void getOAuth2SecurityConfigImports_containsExpectedImports() {
        final String result = SecurityImports.getOAuth2SecurityConfigImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import org.springframework.context.annotation.Bean;"));
        assertTrue(result.contains("import org.springframework.security.config.annotation.web.builders.HttpSecurity;"));
        assertTrue(result.contains("import org.springframework.security.web.SecurityFilterChain;"));
    }

    @Test
    @DisplayName("getApiKeySecurityConfigImports: contains UsernamePasswordAuthenticationFilter import")
    void getApiKeySecurityConfigImports_containsExpectedImports() {
        final String result = SecurityImports.getApiKeySecurityConfigImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import org.springframework.security.web.SecurityFilterChain;"));
        assertTrue(result.contains("import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;"));
    }

    @Test
    @DisplayName("getJwtTokenProviderImports: contains java, org, and io.jsonwebtoken groups")
    void getJwtTokenProviderImports_containsAllGroups() {
        final String result = SecurityImports.getJwtTokenProviderImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import java.util.Date;"));
        assertTrue(result.contains("import java.util.List;"));
        assertTrue(result.contains("import javax.crypto.SecretKey;"));
        assertTrue(result.contains("import org.springframework.beans.factory.annotation.Value;"));
        assertTrue(result.contains("import org.springframework.security.core.GrantedAuthority;"));
        assertTrue(result.contains("import org.springframework.stereotype.Component;"));
        assertTrue(result.contains("import io.jsonwebtoken.Claims;"));
        assertTrue(result.contains("import io.jsonwebtoken.Jwts;"));
        assertTrue(result.contains("import io.jsonwebtoken.security.Keys;"));
    }

    @Test
    @DisplayName("getJwtTokenProviderImports: java group appears before jakarta before org before io")
    void getJwtTokenProviderImports_groupOrder() {
        final String result = SecurityImports.getJwtTokenProviderImports();

        final int javaPos = result.indexOf("import java.");
        final int javaxPos = result.indexOf("import javax.");
        final int orgPos = result.indexOf("import org.");
        final int ioPos = result.indexOf("import io.");

        assertTrue(javaPos < javaxPos, "java.* should appear before javax.*");
        assertTrue(javaxPos < orgPos, "javax.* should appear before org.*");
        assertTrue(orgPos < ioPos, "org.* should appear before io.*");
    }

    @Test
    @DisplayName("getJwtAuthenticationFilterImports: contains java, jakarta, and org groups")
    void getJwtAuthenticationFilterImports_containsAllGroups() {
        final String result = SecurityImports.getJwtAuthenticationFilterImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import java.io.IOException;"));
        assertTrue(result.contains("import jakarta.servlet.FilterChain;"));
        assertTrue(result.contains("import jakarta.servlet.http.HttpServletRequest;"));
        assertTrue(result.contains("import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;"));
        assertTrue(result.contains("import org.springframework.web.filter.OncePerRequestFilter;"));
    }

    @Test
    @DisplayName("getAuthControllerImports: contains spring web and security imports")
    void getAuthControllerImports_containsExpectedImports() {
        final String result = SecurityImports.getAuthControllerImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import org.springframework.security.authentication.AuthenticationManager;"));
        assertTrue(result.contains("import org.springframework.web.bind.annotation.PostMapping;"));
        assertTrue(result.contains("import org.springframework.web.bind.annotation.RestController;"));
    }

    @Test
    @DisplayName("getUserDetailsServiceImports: contains java and spring security imports")
    void getUserDetailsServiceImports_containsExpectedImports() {
        final String result = SecurityImports.getUserDetailsServiceImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import java.util.List;"));
        assertTrue(result.contains("import org.springframework.security.core.userdetails.UserDetails;"));
        assertTrue(result.contains("import org.springframework.security.core.userdetails.UserDetailsService;"));
        assertTrue(result.contains("import org.springframework.security.core.userdetails.UsernameNotFoundException;"));
        assertTrue(result.contains("import org.springframework.stereotype.Service;"));
    }

    @Test
    @DisplayName("getApiKeyFilterImports: contains java, jakarta, and org groups")
    void getApiKeyFilterImports_containsAllGroups() {
        final String result = SecurityImports.getApiKeyFilterImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import java.io.IOException;"));
        assertTrue(result.contains("import jakarta.servlet.FilterChain;"));
        assertTrue(result.contains("import jakarta.servlet.http.HttpServletRequest;"));
        assertTrue(result.contains("import org.springframework.http.HttpStatus;"));
        assertTrue(result.contains("import org.springframework.web.filter.OncePerRequestFilter;"));
    }

    @Test
    @DisplayName("getApiKeyTokenImports: contains java and org groups")
    void getApiKeyTokenImports_containsExpectedImports() {
        final String result = SecurityImports.getApiKeyTokenImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import java.util.Collection;"));
        assertTrue(result.contains("import java.util.List;"));
        assertTrue(result.contains("import java.util.stream.Collectors;"));
        assertTrue(result.contains("import org.springframework.security.authentication.AbstractAuthenticationToken;"));
        assertTrue(result.contains("import org.springframework.security.core.GrantedAuthority;"));
        assertTrue(result.contains("import org.springframework.security.core.authority.SimpleGrantedAuthority;"));
    }

    @Test
    @DisplayName("getJwtRoleConverterImports: contains java and OAuth2 spring security imports")
    void getJwtRoleConverterImports_containsExpectedImports() {
        final String result = SecurityImports.getJwtRoleConverterImports();

        assertFalse(result.isBlank());
        assertTrue(result.contains("import java.util.Collection;"));
        assertTrue(result.contains("import java.util.List;"));
        assertTrue(result.contains("import java.util.Map;"));
        assertTrue(result.contains("import org.springframework.security.authentication.AbstractAuthenticationToken;"));
        assertTrue(result.contains("import org.springframework.stereotype.Component;"));
        assertTrue(result.contains("import org.springframework.security.oauth2.jwt.Jwt;"));
        assertTrue(result.contains("import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;"));
        assertTrue(result.contains("import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;"));
    }

    @Test
    @DisplayName("getApiKeyTokenImports: java imports are sorted alphabetically within group")
    void getApiKeyTokenImports_javaImportsSortedAlphabetically() {
        final String result = SecurityImports.getApiKeyTokenImports();

        final int collectionPos = result.indexOf("import java.util.Collection;");
        final int listPos = result.indexOf("import java.util.List;");
        final int streamPos = result.indexOf("import java.util.stream.Collectors;");

        assertTrue(collectionPos < listPos, "Collection should appear before List (alphabetical)");
        assertTrue(listPos < streamPos, "List should appear before stream.Collectors (alphabetical)");
    }
}
