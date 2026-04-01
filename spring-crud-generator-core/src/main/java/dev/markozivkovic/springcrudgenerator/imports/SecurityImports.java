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

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class SecurityImports {

    private SecurityImports() {}

    public static String getBasicAuthSecurityConfigImports() {
        return buildGroupedImports(
            List.of("java.util.ArrayList",
                    "java.util.List"),
            List.of(),
            List.of("org.springframework.context.annotation.Bean",
                    "org.springframework.context.annotation.Configuration",
                    "org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity",
                    "org.springframework.security.config.annotation.web.builders.HttpSecurity",
                    "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity",
                    "org.springframework.security.config.http.SessionCreationPolicy",
                    "org.springframework.security.core.userdetails.User",
                    "org.springframework.security.core.userdetails.UserDetails",
                    "org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder",
                    "org.springframework.security.crypto.password.PasswordEncoder",
                    "org.springframework.security.provisioning.InMemoryUserDetailsManager",
                    "org.springframework.security.web.SecurityFilterChain"),
            List.of()
        );
    }

    public static String getJwtSecurityConfigImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of("org.springframework.context.annotation.Bean",
                    "org.springframework.context.annotation.Configuration",
                    "org.springframework.security.authentication.AuthenticationManager",
                    "org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration",
                    "org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity",
                    "org.springframework.security.config.annotation.web.builders.HttpSecurity",
                    "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity",
                    "org.springframework.security.config.http.SessionCreationPolicy",
                    "org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder",
                    "org.springframework.security.crypto.password.PasswordEncoder",
                    "org.springframework.security.web.SecurityFilterChain",
                    "org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter"),
            List.of()
        );
    }

    public static String getOAuth2SecurityConfigImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of("org.springframework.context.annotation.Bean",
                    "org.springframework.context.annotation.Configuration",
                    "org.springframework.security.config.annotation.web.builders.HttpSecurity",
                    "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity",
                    "org.springframework.security.config.http.SessionCreationPolicy",
                    "org.springframework.security.web.SecurityFilterChain"),
            List.of()
        );
    }

    public static String getApiKeySecurityConfigImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of("org.springframework.context.annotation.Bean",
                    "org.springframework.context.annotation.Configuration",
                    "org.springframework.security.config.annotation.web.builders.HttpSecurity",
                    "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity",
                    "org.springframework.security.config.http.SessionCreationPolicy",
                    "org.springframework.security.web.SecurityFilterChain",
                    "org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter"),
            List.of()
        );
    }

    public static String getJwtTokenProviderImports() {
        return buildGroupedImports(
            List.of("java.util.Date",
                    "java.util.List",
                    "java.util.stream.Collectors"),
            List.of("javax.crypto.SecretKey"),
            List.of("org.springframework.beans.factory.annotation.Value",
                    "org.springframework.security.core.GrantedAuthority",
                    "org.springframework.stereotype.Component"),
            List.of("io.jsonwebtoken.Claims",
                    "io.jsonwebtoken.Jwts",
                    "io.jsonwebtoken.security.Keys")
        );
    }

    public static String getJwtAuthenticationFilterImports() {
        return buildGroupedImports(
            List.of("java.io.IOException"),
            List.of("jakarta.servlet.FilterChain",
                    "jakarta.servlet.ServletException",
                    "jakarta.servlet.http.HttpServletRequest",
                    "jakarta.servlet.http.HttpServletResponse"),
            List.of("org.springframework.beans.factory.annotation.Autowired",
                    "org.springframework.security.authentication.UsernamePasswordAuthenticationToken",
                    "org.springframework.security.core.context.SecurityContextHolder",
                    "org.springframework.security.core.userdetails.UserDetails",
                    "org.springframework.security.web.authentication.WebAuthenticationDetailsSource",
                    "org.springframework.stereotype.Component",
                    "org.springframework.web.filter.OncePerRequestFilter"),
            List.of()
        );
    }

    public static String getAuthControllerImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of("org.springframework.beans.factory.annotation.Autowired",
                    "org.springframework.http.ResponseEntity",
                    "org.springframework.security.authentication.AuthenticationManager",
                    "org.springframework.security.authentication.UsernamePasswordAuthenticationToken",
                    "org.springframework.security.core.Authentication",
                    "org.springframework.security.core.context.SecurityContextHolder",
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.web.bind.annotation.RequestBody",
                    "org.springframework.web.bind.annotation.RequestMapping",
                    "org.springframework.web.bind.annotation.RestController"),
            List.of()
        );
    }

    public static String getUserDetailsServiceImports() {
        return buildGroupedImports(
            List.of("java.util.List"),
            List.of(),
            List.of("org.springframework.security.core.GrantedAuthority",
                    "org.springframework.security.core.authority.SimpleGrantedAuthority",
                    "org.springframework.security.core.userdetails.User",
                    "org.springframework.security.core.userdetails.UserDetails",
                    "org.springframework.security.core.userdetails.UserDetailsService",
                    "org.springframework.security.core.userdetails.UsernameNotFoundException",
                    "org.springframework.stereotype.Service"),
            List.of()
        );
    }

    public static String getApiKeyFilterImports() {
        return buildGroupedImports(
            List.of("java.io.IOException"),
            List.of("jakarta.servlet.FilterChain",
                    "jakarta.servlet.ServletException",
                    "jakarta.servlet.http.HttpServletRequest",
                    "jakarta.servlet.http.HttpServletResponse"),
            List.of("org.springframework.http.HttpStatus",
                    "org.springframework.security.core.context.SecurityContextHolder",
                    "org.springframework.stereotype.Component",
                    "org.springframework.web.filter.OncePerRequestFilter"),
            List.of()
        );
    }

    public static String getApiKeyTokenImports() {
        return buildGroupedImports(
            List.of("java.util.Collection",
                    "java.util.List",
                    "java.util.stream.Collectors"),
            List.of(),
            List.of("org.springframework.security.authentication.AbstractAuthenticationToken",
                    "org.springframework.security.core.GrantedAuthority",
                    "org.springframework.security.core.authority.SimpleGrantedAuthority"),
            List.of()
        );
    }

    public static String getJwtRoleConverterImports() {
        return buildGroupedImports(
            List.of("java.util.Collection",
                    "java.util.List",
                    "java.util.Map"),
            List.of(),
            List.of("org.springframework.beans.factory.annotation.Value",
                    "org.springframework.core.convert.converter.Converter",
                    "org.springframework.security.core.GrantedAuthority",
                    "org.springframework.security.core.authority.SimpleGrantedAuthority",
                    "org.springframework.security.oauth2.jwt.Jwt",
                    "org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter",
                    "org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter"),
            List.of()
        );
    }

    /**
     * Builds a properly grouped and sorted import block.
     * Groups are: java.*, jakarta.*, org.*, io.* / com.* (third-party).
     * Each non-empty group is separated from the next by a blank line.
     *
     * @param javaImports      fully qualified class names in java.*  / javax.*
     * @param jakartaImports   fully qualified class names in jakarta.*
     * @param orgImports       fully qualified class names in org.*
     * @param thirdPartyImports fully qualified class names in io.* / com.*
     * @return formatted import block string
     */
    private static String buildGroupedImports(final List<String> javaImports, final List<String> jakartaImports,
            final List<String> orgImports, final List<String> thirdPartyImports) {

        final List<List<String>> groups = Arrays.asList(javaImports, jakartaImports, orgImports, thirdPartyImports);

        return groups.stream()
            .filter(group -> group != null && !group.isEmpty())
            .map(group -> group.stream()
                .sorted()
                .map(imp -> String.format(IMPORT, imp))
                .collect(Collectors.joining()))
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
