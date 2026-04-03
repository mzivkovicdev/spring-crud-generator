<#setting number_format="computer">
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
<#if keyStrategy == "AUTHENTICATED_USER">
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
</#if>

import io.github.bucket4j.ConsumptionProbe;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String HEADER_REMAINING = "X-Rate-Limit-Remaining";
    private static final String HEADER_LIMIT = "X-Rate-Limit-Limit";
    private static final String HEADER_RETRY_AFTER = "X-Rate-Limit-Retry-After-Seconds";

    private final RateLimiterService rateLimiterService;
    private final int statusCode;
    private final boolean includeHeaders;
    private final String message;
<#if keyStrategy == "HEADER">
    private final String keyHeader;
</#if>

    public RateLimitingFilter(
            final RateLimiterService rateLimiterService,
            @Value("\${rate.limiting.response.status-code:${statusCode}}") final int statusCode,
            @Value("\${rate.limiting.response.include-headers:${includeHeaders?c}}") final boolean includeHeaders,
            @Value("\${rate.limiting.response.message:Rate limit exceeded. Please try again later.}") final String message<#if keyStrategy == "HEADER">,
            @Value("\${rate.limiting.key-header:${keyHeader}}") final String keyHeader</#if>) {
        this.rateLimiterService = rateLimiterService;
        this.statusCode = statusCode;
        this.includeHeaders = includeHeaders;
        this.message = message;
<#if keyStrategy == "HEADER">
        this.keyHeader = keyHeader;
</#if>
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final String key = resolveKey(request);
        final ConsumptionProbe probe = rateLimiterService.tryConsume(key);

        if (probe.isConsumed()) {
            if (includeHeaders) {
                response.setHeader(HEADER_REMAINING, String.valueOf(probe.getRemainingTokens()));
                response.setHeader(HEADER_LIMIT, String.valueOf(rateLimiterService.getCapacity()));
            }
            filterChain.doFilter(request, response);
        } else {
            if (includeHeaders) {
                response.setHeader(HEADER_RETRY_AFTER,
                        String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
            }
            response.setStatus(statusCode);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\": \"" + message + "\"}");
        }
    }

    private String resolveKey(final HttpServletRequest request) {
<#if keyStrategy == "IP">
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
<#elseif keyStrategy == "API_KEY">
        final String apiKey = request.getHeader("X-API-Key");
        return apiKey != null ? apiKey : request.getRemoteAddr();
<#elseif keyStrategy == "HEADER">
        final String headerValue = request.getHeader(keyHeader);
        return headerValue != null ? headerValue : request.getRemoteAddr();
<#elseif keyStrategy == "AUTHENTICATED_USER">
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
</#if>
    }

}
