# Untrusted Input and Dangerous Sinks

## Contents

1. [Trace sources to sinks](#trace-sources-to-sinks)
2. [Queries and identifiers](#queries-and-identifiers)
3. [Commands, expressions, and reflection](#commands-expressions-and-reflection)
4. [HTML and active content](#html-and-active-content)
5. [URLs, redirects, and WebClient](#urls-redirects-and-webclient)
6. [Files and archives](#files-and-archives)
7. [XML and deserialization](#xml-and-deserialization)
8. [Headers, logs, regexes, and resource exhaustion](#headers-logs-regexes-and-resource-exhaustion)
9. [Verification checklist](#verification-checklist)

## Trace sources to sinks

Treat request bodies, paths, query parameters, headers, cookies, multipart content, filenames, database values originally supplied by users, messages, webhooks, partner responses, files, DNS, environment values, and external tool output as untrusted until proven otherwise.

For each flow, identify:

1. the source and its encoding;
2. authentication and authorization context;
3. canonicalization rules;
4. structural and business validation;
5. size, count, depth, time, and concurrency bounds;
6. the final interpreter or sink;
7. sink-specific encoding or parameterization;
8. safe failure behavior and tests.

Validation at one boundary does not make a value safe for every later context.

## Queries and identifiers

Bind query values:

```java
@Query("""
        select user
        from UserEntity user
        where user.email = :email
        """)
Optional<UserEntity> findByEmail(@Param("email") final String email);
```

Reject concatenation:

```java
String jpql = "select user from UserEntity user where user.email = '" + email + "'";
```

- Parameterize JPQL, native SQL, JDBC, search expressions, and stored-procedure values.
- Use `spring-data-jpa` for query construction, plans, bounds, and tests.
- Query parameters cannot safely bind identifiers such as column names or sort direction. Translate an external sort key through an explicit allowlist to a known property.
- Do not expose arbitrary Specifications, predicates, operators, joins, or query languages to untrusted callers.
- Scope queries by authenticated tenant and ownership where applicable.
- Apply result, page, offset, and execution-time limits.

## Commands, expressions, and reflection

- Do not invoke an operating-system shell with untrusted input.
- Prefer a library API over a process.
- If a process is unavoidable, use a fixed executable, separate allowlisted arguments, a controlled working directory and environment, timeouts, output limits, least privilege, and no shell expansion.
- Do not evaluate untrusted SpEL, scripts, templates, regular expressions, class names, bean names, method names, or expressions.
- Never use reflection or class loading to turn an untrusted string into executable behavior.
- Map external discriminator values to a closed set of approved implementations.
- Treat template engines, report generators, image processors, document converters, and archive tools as interpreters with their own sandboxing and resource-limit requirements.

## HTML and active content

REST APIs can create stored XSS when another client later renders returned data.

- Prefer plain text and let the rendering client perform context-appropriate output encoding.
- Validation is not HTML sanitization. Do not detect unsafe HTML with a regular expression or a small blocklist.
- If the business requires rich HTML, use an approved, maintained allowlist sanitizer and define permitted elements, attributes, URL schemes, and CSS behavior.
- Encode for the actual output context: HTML text, HTML attribute, JavaScript, CSS, and URL contexts are different.
- Do not return attacker-controlled content with an executable media type.
- Set explicit content type and safe download disposition for user-controlled files.
- Add regression tests with malformed markup, encoded payloads, event handlers, dangerous URL schemes, SVG, and parser edge cases relevant to the chosen sanitizer.

## URLs, redirects, and WebClient

Prefer a configured base URL and internally constructed relative paths:

```java
public Mono<PartnerResponse> fetchOrder(final String orderId) {
    return this.partnerWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/orders/{orderId}")
            .build(orderId))
        .retrieve()
        .bodyToMono(PartnerResponse.class);
}
```

The configured client must still enforce timeouts, TLS, response limits, and the approved destination.

For any caller-influenced destination:

- allowlist schemes, hosts, ports, and path shapes;
- reject user information, fragments, ambiguous encodings, invalid Unicode, and unexpected authority syntax;
- resolve and check addresses according to policy, including loopback, private, link-local, multicast, cloud-metadata, and internal control-plane ranges;
- account for DNS rebinding and revalidate redirects;
- disable redirects unless required, or validate every redirect target;
- do not forward inbound authorization, cookies, API keys, or sensitive tracing baggage;
- restrict outbound network access at the infrastructure layer as well as in code;
- set connection, response, total-operation, retry, and maximum-body limits.

For redirects returned to clients, prefer server-owned destinations or map a short allowlisted identifier to a configured URL. Do not reflect arbitrary URLs into `Location`.

## Files and archives

- Permit only required upload types and set per-file, total-request, count, filename-length, and processing-time limits.
- Treat extension, declared content type, and file signature as separate signals; none is sufficient alone.
- Generate server-side storage names. Keep original names only as sanitized metadata when needed.
- Normalize and resolve paths, then verify the result remains under the approved root before access.
- Store uploads outside executable and web-served locations with least-privilege permissions.
- Scan or sandbox files when the risk and available platform require it.
- Do not execute, include, compile, or interpret uploaded content.
- Protect downloads with the same object and tenant authorization as other resources.
- Set a safe `Content-Disposition` and an explicit media type.

For archives:

- limit entries, expanded bytes, compression ratio, recursion depth, and processing time;
- reject absolute paths, parent traversal, links, device files, and duplicate or conflicting entries;
- validate every extracted path against the destination root;
- clean temporary data on success and failure.

## XML and deserialization

- Disable external entities, DTDs, XInclude, and external schema access unless a reviewed use case requires them.
- Set parser depth, entity, document-size, and timeout limits where supported.
- Do not use Java native serialization for untrusted data.
- Do not enable permissive Jackson polymorphic default typing for untrusted JSON.
- Map input into explicit TOs with allowlisted fields and validate it before use.
- Avoid polymorphic deserialization unless the type set is closed, explicit, and configured with a restrictive validator.
- Treat deserialization hooks, setters, constructors, converters, and validation callbacks as executable attack surface.
- Use safe formats and authenticated transport or signatures when message integrity is required.

## Headers, logs, regexes, and resource exhaustion

- Reject CR/LF and invalid characters in values used for response headers, redirects, filenames, or logs.
- Use framework APIs for header construction and structured logging.
- Do not copy arbitrary inbound headers to outbound responses or remote requests.
- Never log raw request bodies, authorization headers, tokens, cookies, or attacker-controlled multiline values.
- Use simple bounded regexes for validation. Avoid nested ambiguous quantifiers and test hostile long input.
- Prefer parsers to complex validation regexes for structured formats.
- Bound collection sizes, nesting depth, string length, multipart counts, page size, sort fields, concurrent work, queue depth, retries, backoff, cache value size, remote response size, and decompression.
- Stream large content only when authorization, total-size limits, cleanup, cancellation, and backpressure are correctly handled.
- Reject or safely ignore unknown input fields according to the public contract; do not treat Jackson's unknown-property setting as a security control.

## Verification checklist

- [ ] Every untrusted source is traced to its final interpreter or sink.
- [ ] Query values are bound and dynamic identifiers use an allowlist.
- [ ] Commands, expressions, reflection, templates, and class loading cannot be selected by arbitrary input.
- [ ] HTML is encoded or sanitized for the actual rendering context.
- [ ] URLs, redirects, and WebClient destinations resist SSRF and credential forwarding.
- [ ] File and archive handling prevents traversal, executable content, and resource exhaustion.
- [ ] XML and deserialization use restrictive configuration and explicit types.
- [ ] Headers and logs resist injection.
- [ ] Size, count, depth, time, retry, and concurrency limits are tested.

## References

- [OWASP Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Injection_Prevention_Cheat_Sheet.html)
- [OWASP SSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)
- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [OWASP Deserialization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html)
- [OWASP Cross Site Scripting Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
