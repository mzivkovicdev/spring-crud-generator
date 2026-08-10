# Compatibility and evolution

Use this reference when changing an existing endpoint, deciding whether a change is breaking, or
versioning, deprecating, or retiring an API. Apply every rule from `../SKILL.md`.

Everything here applies whether or not the project publishes a contract document. These are
properties of the API that consumers observe, not of any artefact describing it. Where a rule refers
to marking something in a document, a project recording `none` applies the same rule through
release notes, direct notification of known consumers, and tests.

## Contents

1. [The asymmetry that catches people](#the-asymmetry-that-catches-people)
2. [Breaking change reference](#breaking-change-reference)
3. [Avoiding a breaking change](#avoiding-a-breaking-change)
4. [Versioning](#versioning)
5. [Deprecation and sunset](#deprecation-and-sunset)
6. [Reviewing a contract change](#reviewing-a-contract-change)

## The asymmetry that catches people

The same edit is safe in one direction and breaking in the other, because the roles are reversed:

- In a **request**, the consumer produces and the service consumes. Loosening what the service accepts is safe; tightening it breaks callers.
- In a **response**, the service produces and the consumer consumes. Adding is usually safe; removing or narrowing breaks callers.

So adding an optional request field is safe, adding a required one is breaking. Adding a response
field is safe, removing one is breaking. Accepting a new enum value in a request is safe, returning
a new enum value in a response can break a consumer whose parser rejects unknown values.

State the direction before judging any change. Most incorrect judgements come from applying the
response rule to a request.

## Breaking change reference

| Change | Request | Response |
| --- | --- | --- |
| Add optional field | Safe | Safe |
| Add required field | **Breaking** | Safe |
| Remove field | Safe for the service, but confirm no caller depends on rejection | **Breaking** |
| Make an optional field required | **Breaking** | Safe |
| Make a required field optional | Safe | **Breaking** |
| Make a field nullable | Safe | **Breaking** |
| Change a field's type or format | **Breaking** | **Breaking** |
| Rename a field | **Breaking** | **Breaking** |
| Add an enum value | Safe | **Breaking in practice** — many generated clients reject unknown values |
| Remove an enum value | **Breaking** | Safe |
| Tighten a constraint: shorter max, narrower range, stricter pattern | **Breaking** | Safe |
| Loosen a constraint | Safe | **Breaking** if a caller sized storage from it |
| Add an endpoint | Safe | Safe |
| Remove an endpoint or method | **Breaking** | **Breaking** |
| Change a path or path parameter | **Breaking** | **Breaking** |
| Change a success status code | **Breaking** | **Breaking** |
| Add a new error status | Safe if callers handle unknown 4xx/5xx generically; otherwise announce it | — |
| Change an existing error's status or problem type | **Breaking** | **Breaking** |
| Change `title` or `detail` text of a problem | Safe | Safe — they carry no contract |
| Add a required request header | **Breaking** | — |
| Change default sort, page size, or ordering | **Breaking in practice** — callers depend on observed behaviour | — |
| Change authentication or required scope | **Breaking** | **Breaking** |

Two entries deserve emphasis because they are routinely shipped as safe:

- **Adding an enum value to a response.** Strictly it is an addition, but generated clients commonly fail to deserialize an unknown value. Treat it as breaking unless every consumer is known to tolerate unknown values, and design enums for extension from the start by documenting that consumers must accept unknown values.
- **Changing a default.** Nothing in the schema changed, so no tool reports it, and every caller that never sent the parameter sees different behaviour after deployment.

## Avoiding a breaking change

Most breaking changes are avoidable at design time:

- Add a new optional field instead of changing an existing one, and deprecate the old field.
- Add a new endpoint instead of changing an existing one's shape, and deprecate the old endpoint.
- Accept both the old and the new form of a request for a stated period, then retire the old one.
- Introduce a new optional parameter that opts into the new behaviour, then flip the default only at a version boundary.
- Never reuse a field name for a different meaning. A field whose meaning changes is worse than one that is removed, because nothing fails and the data is silently wrong.

## Versioning

- The API version lives in the URI base path, declared once as `ApiPaths.API_V1` and reflected in `servers`. `spring-boot-patterns` owns that declaration.
- Raise the major version only for a breaking change that could not be avoided. Do not raise it for cleanup, renaming, refactoring, or aesthetics.
- Bundle breaking changes: a new version is expensive for consumers, so ship the accumulated set together rather than raising the version repeatedly.
- Run the old and new versions in parallel for a stated period. State the length before releasing the new version, not after consumers complain.
- Record in the project profile which versions are live, when each was released, and when each retires.
- Do not version individual endpoints. Per-endpoint versions are cheap to introduce and impossible to reason about afterwards.

## Deprecation and sunset

Deprecation without a date is a wish, not a plan.

- With a document, mark the operation or field `deprecated: true` and say in its `description` what replaces it and when it retires. Without one, record the same three facts — what is deprecated, what replaces it, and the retirement date — in the release notes and in the project profile.
- Send the `Deprecation` header on responses from a deprecated endpoint, and `Sunset` with the retirement date.
- Announce to known consumers directly. A flag in a document nobody re-reads is not an announcement.
- Do not remove anything before its sunset date, and do not extend the date silently.
- Before removal, confirm from access logs or metrics that the endpoint or field is actually unused, and keep that evidence with the change.

```yaml
paths:
  /users/{userId}/preferences:
    get:
      deprecated: true
      summary: Returns user preferences
      description: >
        Deprecated. Use GET /users/{userId}/settings instead.
        This endpoint is removed on 2027-01-31.
```

## Reviewing a contract change

With a document, its diff is the part of a pull request a consumer will feel; review it first.
Without one, the same review happens against the controller signatures, the TOs, and the tests, and
it is the only place a breaking change can be caught at all. Either way, for each change answer:

1. Which direction is it, request or response?
2. Is it breaking under the table above?
3. If breaking, was it avoidable by adding instead of changing?
4. If unavoidable, is it going through versioning or deprecation, with a date?
5. Do the known consumers know?

A contract change with no answer to question 5 does not merge. `spring-boot-code-review` owns the
reporting; this reference owns the judgement.
