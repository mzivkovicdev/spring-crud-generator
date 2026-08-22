# OpenAPI document

Read this only when `docs/project-profile.md` records an OpenAPI contract document. Use it when producing, structuring, or improving that document. Apply
every rule from `../SKILL.md`, `project-naming-conventions` for every name, and `spring-boot-patterns`
for the shapes being described.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

1. [Which OpenAPI version and serialization format](#which-openapi-version-and-serialization-format)
2. [Document structure](#document-structure)
3. [Describing an operation](#describing-an-operation)
4. [Describing errors](#describing-errors)
5. [Code-first production and the drift gate](#code-first-production-and-the-drift-gate)
6. [Exposure](#exposure)

## Which OpenAPI version and serialization format

Record both in `docs/project-profile.md` and keep one of each throughout the document.

| | OpenAPI 3.0 | OpenAPI 3.1 |
| --- | --- | --- |
| Nullability | `nullable: true` beside the type | The type is a list that includes `"null"` |
| JSON Schema | A dialect of its own | Full JSON Schema 2020-12 |
| Tool support | Universal | Good, but verify each consumer's generator |

springdoc 2.x emits 3.0 by default and 3.1 on request. Choose 3.1 only after confirming that every
consumer's tooling reads it; a document a consumer's generator cannot parse is worse than an older
dialect. Whichever is chosen, express nullability that one way everywhere — a document mixing both
styles will be read inconsistently.

**The serialization format is JSON or YAML, and the extension of the committed document path in the
profile is what selects it.** Both are valid OpenAPI and no rule here prefers one; what matters is
that the repository holds exactly one document in exactly one format, because two copies drift and
a reviewer cannot tell which one a consumer reads.

- YAML diffs better in a pull request and is easier to hand-author, which usually makes it the fit for a contract-first document a person maintains.
- JSON is what a generator emits without extra configuration, which usually makes it the fit for a code-first document nobody edits by hand.
- The examples in this reference are YAML for readability and the drift gate below is JSON for brevity. **Neither choice is a recommendation.** Follow the extension recorded in the profile, and make the endpoint the gate reads match it: springdoc serves the document at `/v3/api-docs` as JSON and `/v3/api-docs.yaml` as YAML, so a YAML project reads the second and parses it with a YAML mapper.
- Do not commit both formats, and do not convert between them as a side effect of another change. A format change is a change to the artifact every consumer diffs.

## Document structure

- `servers` carries the base path, including the API version prefix. Path Items stay resource-relative, such as `/users/{userId}`, so the version never reaches `operationId` or a handler method name. `spring-boot-patterns` owns that split.
- `info` carries the title, a description aimed at a caller, the contract version, and a contact. The contract version tracks the API, not the build.
- `tags` group operations by resource, and every operation carries exactly one. Untagged operations land in a default bucket that reads as an afterthought.
- `components.schemas` holds every reusable shape. Inline a schema only when it is genuinely used once and is small.
- `components.securitySchemes` describes the model `application-security` defines. Reference it globally, and override per operation when an operation differs.
- `components.responses` holds the shared error responses so an operation references them instead of restating them.

## Describing an operation

```yaml
paths:
  /users/{userId}:
    get:
      tags: [users]
      operationId: usersUserIdGet
      summary: Returns a single user by identifier
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        "200":
          description: The user
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/UserTO"
        "400":
          $ref: "#/components/responses/ValidationFailed"
        "401":
          $ref: "#/components/responses/Unauthenticated"
        "403":
          $ref: "#/components/responses/AccessDenied"
        "404":
          $ref: "#/components/responses/ResourceNotFound"
```

Notes:

- `operationId` equals the controller handler method name, derived from the Path Item plus the HTTP method. `project-naming-conventions` owns that derivation and the carve-out that permits a noun-first name here.
- The schema name is the TO name without invention: `UserTO`, not `User` or `UserResponse`.
- Every status the endpoint can actually produce is listed. The `401` and `403` come from the security filter chain rather than from the controller, and are still part of the contract a caller must handle.
- `description` on a response says what the payload is, not that the request succeeded.

## Describing errors

Declare the problem shape once and reference it. Every entry in the project's error catalog that an
operation can produce is named in the operation's responses.

```yaml
components:
  schemas:
    ProblemDetail:
      type: object
      required: [type, title, status]
      properties:
        type:
          type: string
          format: uri
          description: Stable identifier of the problem. Branch on this value.
        title:
          type: string
          description: Short human-readable summary. May change without notice.
        status:
          type: integer
        detail:
          type: string
          description: Human-readable explanation. May change without notice.
        correlationId:
          type: string
          description: Identifier of the request, for support requests.
  responses:
    ResourceNotFound:
      description: The requested resource does not exist
      content:
        application/problem+json:
          schema:
            $ref: "#/components/schemas/ProblemDetail"
          example:
            type: https://api.acme.example/problems/resource-not-found
            title: Resource not found
            status: 404
            detail: The requested resource does not exist.
            correlationId: 6f1c2f2a-6a5f-4a2e-9a8f-0d4a1f9c2b77
```

Rules:

- The schema is named `ProblemDetail` after the type the application actually serializes, Spring's `ProblemDetail`. It is the one schema in the document with no `TO` suffix, because there is no project-owned transfer object behind it; naming it `ProblemTO` would invent a Java type that does not exist. `project-naming-conventions` owns schema naming, and this is its single documented exception.
- The media type is `application/problem+json`, not `application/json`.
- `type` is documented as the value consumers branch on, and `title` and `detail` as text that may change. Saying so in the contract is what stops a consumer from matching on the message.
- The body carries no second error identifier. `correlationId` identifies the request, not the failure; `spring-boot-patterns` owns that decision.
- Never document `traceId`, `spanId`, a stack trace, an exception class name, or an internal hostname.
- The example uses a synthetic correlation identifier. Examples never carry real data.

## Code-first production and the drift gate

With code-first, the document is generated from the code and committed, so a contract change is
visible in the pull request diff. The gate that keeps it honest is a test that regenerates and
compares.

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OpenApiContractIntegrationTest {

    private static final Path COMMITTED_DOCUMENT = Path.of("src/main/resources/openapi/openapi.json");

    private final TestRestTemplate testRestTemplate;

    OpenApiContractIntegrationTest(final TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    @Test
    void apiDocs_whenGenerated_matchesCommittedDocument() throws Exception {
        final String generated = this.testRestTemplate.getForObject("/v3/api-docs", String.class);
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode generatedTree = objectMapper.readTree(generated);
        final JsonNode committedTree = objectMapper.readTree(Files.readString(COMMITTED_DOCUMENT));

        assertThat(generatedTree)
                .as("The API contract changed. Review the diff, then update %s deliberately.",
                        COMMITTED_DOCUMENT)
                .isEqualTo(committedTree);
    }
}
```

Notes:

- **The example uses a JSON path only to keep it short.** Replace the constant, the endpoint, and the parser with the `.json`, `.yaml`, or `.yml` document recorded in `docs/project-profile.md`; the example never selects the format for the repository.
- Comparing parsed trees rather than text avoids failures from key ordering and formatting, and it is what makes the format substitution above a one-line change: a YAML mapper produces the same tree type, so only the mapper and the endpoint differ.
- The failure message tells the reader what to do. A contract gate that fails with a wall of JSON teaches people to regenerate without looking, which defeats the gate.
- Provide a documented command that rewrites the committed document, so updating it is deliberate and one step.
- The test needs the application context, so it is an integration test and follows the naming and phase rules in `spring-boot-testing`.
- **The document endpoint is behind the filter chain like everything else.** With the `denyAll` fallback that `application-security` prescribes, an unauthenticated request to `/v3/api-docs` returns `401` and the test fails for the wrong reason. Resolve it one of two ways, and record which:
  - permit the document endpoint explicitly in the non-production profile the test runs under, matching it the same way any other route is matched; or
  - have the test obtain a credential through `AccessTokenTestClient`, exactly as every other integration test does.

  Do not disable the filter chain for this test. A drift gate that runs outside the real configuration proves less than it appears to, and it hides the case where the document endpoint is unintentionally public.
- Annotate controllers and TOs enough that the generated document is useful. Summaries, descriptions, and examples come from annotations in code-first; without them the generated document is a type dump.

## Exposure

- The document endpoint and any interactive UI are exposed deliberately, never by default. Decide per environment and record it.
- Swagger UI in production is an `application-security` decision. Absent an explicit decision, it is off.
- Publication follows `../SKILL.md`; what follows is the document itself.
- The document describes only what the service actually serves. Do not publish internal, management, or debug endpoints in a consumer-facing contract.
