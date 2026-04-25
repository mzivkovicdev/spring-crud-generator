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

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;

public final class SecurityImports {

    private SecurityImports() {}

    /**
     * Returns the import block for {@code SecurityConfiguration} when using BASIC_AUTH security.
     * Includes java.util types, Spring Security configuration, in-memory user store, and password encoding.
     *
     * @return formatted import block string
     */
    public static String getBasicAuthSecurityConfigImports() {
        return buildGroupedImports(
            List.of(ImportConstants.Java.ARRAY_LIST,
                    ImportConstants.Java.LIST),
            List.of(),
            List.of(ImportConstants.SpringContext.BEAN,
                    ImportConstants.SpringContext.CONFIGURATION,
                    ImportConstants.SpringSecurity.BCRYPT_PASSWORD_ENCODER,
                    ImportConstants.SpringSecurity.ENABLE_METHOD_SECURITY,
                    ImportConstants.SpringSecurity.ENABLE_WEB_SECURITY,
                    ImportConstants.SpringSecurity.HTTP_SECURITY,
                    ImportConstants.SpringSecurity.IN_MEMORY_USER_DETAILS_MANAGER,
                    ImportConstants.SpringSecurity.PASSWORD_ENCODER,
                    ImportConstants.SpringSecurity.SECURITY_FILTER_CHAIN,
                    ImportConstants.SpringSecurity.SESSION_CREATION_POLICY,
                    ImportConstants.SpringSecurity.USER,
                    ImportConstants.SpringSecurity.USER_DETAILS),
            List.of()
        );
    }

    /**
     * Returns the import block for {@code SecurityConfiguration} when using JWT security.
     * Includes Spring Security configuration, authentication management, and password encoding.
     *
     * @return formatted import block string
     */
    public static String getJwtSecurityConfigImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of(ImportConstants.SpringContext.BEAN,
                    ImportConstants.SpringContext.CONFIGURATION,
                    ImportConstants.SpringSecurity.AUTHENTICATION_CONFIGURATION,
                    ImportConstants.SpringSecurity.AUTHENTICATION_MANAGER,
                    ImportConstants.SpringSecurity.BCRYPT_PASSWORD_ENCODER,
                    ImportConstants.SpringSecurity.ENABLE_METHOD_SECURITY,
                    ImportConstants.SpringSecurity.ENABLE_WEB_SECURITY,
                    ImportConstants.SpringSecurity.HTTP_SECURITY,
                    ImportConstants.SpringSecurity.PASSWORD_ENCODER,
                    ImportConstants.SpringSecurity.SECURITY_FILTER_CHAIN,
                    ImportConstants.SpringSecurity.SESSION_CREATION_POLICY,
                    ImportConstants.SpringSecurity.USERNAME_PASSWORD_AUTH_FILTER),
            List.of()
        );
    }

    /**
     * Returns the import block for {@code SecurityConfiguration} when using OAuth2 Resource Server security.
     * Includes Spring Security configuration and JWT role conversion support.
     *
     * @return formatted import block string
     */
    public static String getOAuth2SecurityConfigImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of(ImportConstants.SpringContext.BEAN,
                    ImportConstants.SpringContext.CONFIGURATION,
                    ImportConstants.SpringSecurity.ENABLE_METHOD_SECURITY,
                    ImportConstants.SpringSecurity.ENABLE_WEB_SECURITY,
                    ImportConstants.SpringSecurity.HTTP_SECURITY,
                    ImportConstants.SpringSecurity.SECURITY_FILTER_CHAIN,
                    ImportConstants.SpringSecurity.SESSION_CREATION_POLICY),
            List.of()
        );
    }

    /**
     * Returns the import block for {@code SecurityConfiguration} when using API_KEY security.
     * Includes Spring Security configuration and filter chain setup.
     *
     * @return formatted import block string
     */
    public static String getApiKeySecurityConfigImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of(ImportConstants.SpringContext.BEAN,
                    ImportConstants.SpringContext.CONFIGURATION,
                    ImportConstants.SpringSecurity.ENABLE_METHOD_SECURITY,
                    ImportConstants.SpringSecurity.ENABLE_WEB_SECURITY,
                    ImportConstants.SpringSecurity.HTTP_SECURITY,
                    ImportConstants.SpringSecurity.SECURITY_FILTER_CHAIN,
                    ImportConstants.SpringSecurity.SESSION_CREATION_POLICY,
                    ImportConstants.SpringSecurity.USERNAME_PASSWORD_AUTH_FILTER),
            List.of()
        );
    }

    /**
     * Returns the import block for the {@code JwtTokenProvider} component.
     * Includes java.util types, javax.crypto for key handling, Spring component and value injection,
     * and the jjwt library.
     *
     * @return formatted import block string
     */
    public static String getJwtTokenProviderImports() {
        return buildGroupedImports(
            List.of(ImportConstants.Java.COLLECTORS,
                    ImportConstants.Java.DATE,
                    ImportConstants.Java.LIST),
            List.of(ImportConstants.Javax.SECRET_KEY),
            List.of(ImportConstants.SpringBean.VALUE,
                    ImportConstants.SpringSecurity.GRANTED_AUTHORITY,
                    ImportConstants.SpringStereotype.COMPONENT),
            List.of(ImportConstants.Jjwt.CLAIMS,
                    ImportConstants.Jjwt.JWTS,
                    ImportConstants.Jjwt.KEYS)
        );
    }

    /**
     * Returns the import block for the {@code JwtAuthenticationFilter} component.
     * Includes java.io, Jakarta servlet API, and Spring Security filter chain imports.
     *
     * @return formatted import block string
     */
    public static String getJwtAuthenticationFilterImports() {
        return buildGroupedImports(
            List.of(ImportConstants.Java.IO_EXCEPTION),
            List.of(ImportConstants.Jakarta.FILTER_CHAIN,
                    ImportConstants.Jakarta.HTTP_SERVLET_REQUEST,
                    ImportConstants.Jakarta.HTTP_SERVLET_RESPONSE,
                    ImportConstants.Jakarta.SERVLET_EXCEPTION),
            List.of(ImportConstants.SpringBean.AUTOWIRED,
                    ImportConstants.SpringSecurity.ONCE_PER_REQUEST_FILTER,
                    ImportConstants.SpringSecurity.SECURITY_CONTEXT_HOLDER,
                    ImportConstants.SpringSecurity.USER_DETAILS,
                    ImportConstants.SpringSecurity.USERNAME_PASSWORD_AUTH_TOKEN,
                    ImportConstants.SpringSecurity.WEB_AUTH_DETAILS_SOURCE,
                    ImportConstants.SpringStereotype.COMPONENT),
            List.of()
        );
    }

    /**
     * Returns the import block for the {@code AuthController} REST controller.
     * Includes Spring web, security authentication, and HTTP response imports.
     *
     * @return formatted import block string
     */
    public static String getAuthControllerImports() {
        return buildGroupedImports(
            List.of(),
            List.of(),
            List.of(ImportConstants.SpringBean.AUTOWIRED,
                    ImportConstants.SpringHttp.RESPONSE_ENTITY,
                    ImportConstants.SpringSecurity.AUTHENTICATION,
                    ImportConstants.SpringSecurity.AUTHENTICATION_MANAGER,
                    ImportConstants.SpringSecurity.SECURITY_CONTEXT_HOLDER,
                    ImportConstants.SpringSecurity.USERNAME_PASSWORD_AUTH_TOKEN,
                    ImportConstants.SpringWeb.POST_MAPPING,
                    ImportConstants.SpringWeb.REQUEST_BODY,
                    ImportConstants.SpringWeb.REQUEST_MAPPING,
                    ImportConstants.SpringWeb.REST_CONTROLLER),
            List.of()
        );
    }

    /**
     * Returns the import block for the {@code UserDetailsServiceImpl} service.
     * Includes java.util types and Spring Security user details imports.
     *
     * @return formatted import block string
     */
    public static String getUserDetailsServiceImports() {
        return buildGroupedImports(
            List.of(ImportConstants.Java.LIST),
            List.of(),
            List.of(ImportConstants.SpringSecurity.GRANTED_AUTHORITY,
                    ImportConstants.SpringSecurity.SIMPLE_GRANTED_AUTHORITY,
                    ImportConstants.SpringSecurity.USER,
                    ImportConstants.SpringSecurity.USER_DETAILS,
                    ImportConstants.SpringSecurity.USER_DETAILS_SERVICE,
                    ImportConstants.SpringSecurity.USERNAME_NOT_FOUND_EXCEPTION,
                    ImportConstants.SpringStereotype.SERVICE),
            List.of()
        );
    }

    /**
     * Returns the import block for the {@code ApiKeyAuthenticationFilter} component.
     * Includes java.io, Jakarta servlet API, and Spring Security filter chain imports.
     *
     * @return formatted import block string
     */
    public static String getApiKeyFilterImports() {
        return buildGroupedImports(
            List.of(ImportConstants.Java.IO_EXCEPTION),
            List.of(ImportConstants.Jakarta.FILTER_CHAIN,
                    ImportConstants.Jakarta.HTTP_SERVLET_REQUEST,
                    ImportConstants.Jakarta.HTTP_SERVLET_RESPONSE,
                    ImportConstants.Jakarta.SERVLET_EXCEPTION),
            List.of(ImportConstants.SpringHttp.HTTP_STATUS,
                    ImportConstants.SpringSecurity.ONCE_PER_REQUEST_FILTER,
                    ImportConstants.SpringSecurity.SECURITY_CONTEXT_HOLDER,
                    ImportConstants.SpringStereotype.COMPONENT),
            List.of()
        );
    }

    /**
     * Returns the import block for the {@code ApiKeyAuthenticationToken} class.
     * Includes java.util collection types and Spring Security authentication imports.
     *
     * @return formatted import block string
     */
    public static String getApiKeyTokenImports() {
        return buildGroupedImports(
            List.of(ImportConstants.Java.COLLECTION,
                    ImportConstants.Java.COLLECTORS,
                    ImportConstants.Java.LIST),
            List.of(),
            List.of(ImportConstants.SpringSecurity.ABSTRACT_AUTH_TOKEN,
                    ImportConstants.SpringSecurity.GRANTED_AUTHORITY,
                    ImportConstants.SpringSecurity.SIMPLE_GRANTED_AUTHORITY),
            List.of()
        );
    }

    /**
     * Returns the import block for the {@code JwtRoleConverter} component.
     * Includes java.util collection types, Spring Core converter, Spring Security OAuth2 JWT,
     * and granted authority imports.
     *
     * @return formatted import block string
     */
    public static String getJwtRoleConverterImports() {
        return buildGroupedImports(
            List.of(ImportConstants.Java.COLLECTION,
                    ImportConstants.Java.LIST,
                    ImportConstants.Java.MAP),
            List.of(),
            List.of(ImportConstants.SpringBean.VALUE,
                    ImportConstants.SpringSecurity.ABSTRACT_AUTH_TOKEN,
                    ImportConstants.SpringCore.CONVERTER,
                    ImportConstants.SpringSecurity.GRANTED_AUTHORITY,
                    ImportConstants.SpringSecurity.JWT_AUTHENTICATION_TOKEN,
                    ImportConstants.SpringSecurity.JWT_GRANTED_AUTHORITIES_CONVERTER,
                    ImportConstants.SpringSecurity.JWT_OAUTH2,
                    ImportConstants.SpringSecurity.SIMPLE_GRANTED_AUTHORITY,
                    ImportConstants.SpringStereotype.COMPONENT),
            List.of()
        );
    }

    /**
     * Builds a properly grouped and sorted import block.
     * Groups are: java.* / javax.*, jakarta.*, org.*, io.* / com.* (third-party).
     * Each non-empty group is separated from the next by a blank line.
     *
     * @param javaImports       fully qualified class names in java.* / javax.*
     * @param jakartaImports    fully qualified class names in jakarta.*
     * @param orgImports        fully qualified class names in org.*
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
