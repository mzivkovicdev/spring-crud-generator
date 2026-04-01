${securityImports}<#if type?string == "BASIC_AUTH">
${basicAuthImports}</#if><#if type?string == "JWT">
${jwtImports}</#if><#if type?string == "API_KEY">
${apiKeyImports}</#if>

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

<#if type?string == "BASIC_AUTH">
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
            .httpBasic(customizer -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(final PasswordEncoder passwordEncoder) {
        final List<UserDetails> users = new ArrayList<>();
    <#if users?? && users?has_content>
        <#list users as user>
        users.add(User.withUsername("${user.username}")
            .password(passwordEncoder.encode("${user.password}"))
            .roles(<#list user.roles as role>"${role}"<#if role_has_next>, </#if></#list>)
            .build());
        </#list>
    <#else>
        users.add(User.withUsername("admin")
            .password(passwordEncoder.encode("admin"))
            .roles("ADMIN")
            .build());
    </#if>
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
</#if>
<#if type?string == "JWT">
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http,
            final JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
</#if>
<#if type?string == "OAUTH2_RESOURCE_SERVER">
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtRoleConverter())))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }
</#if>
<#if type?string == "API_KEY">
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http,
            final ApiKeyAuthenticationFilter apiKeyFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
</#if>
}
