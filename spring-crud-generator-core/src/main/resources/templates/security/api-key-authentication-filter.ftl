${filterImports}

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "${headerName}";

    private static final java.util.Map<String, java.util.List<String>> VALID_KEYS;

    static {
        VALID_KEYS = new java.util.HashMap<>();
<#if apiKeys?? && apiKeys?has_content>
    <#list apiKeys as entry>
        VALID_KEYS.put("${entry.value}", java.util.Arrays.asList(<#list entry.roles as role>"${role}"<#if role_has_next>, </#if></#list>));
    </#list>
</#if>
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final String apiKey = request.getHeader(HEADER_NAME);

        if (apiKey == null || !VALID_KEYS.containsKey(apiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: invalid or missing API key");
            return;
        }

        final ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(apiKey, VALID_KEYS.get(apiKey));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
