# Copilot Instructions

## Spring CRUD spec files

When creating or editing generator spec files used by `inputSpecFile`:

- Treat `docs/examples/crud-spec-full.yaml` as the primary feature reference.
- Follow `docs/schema/crud-spec.schema.json` as the source of truth for allowed structure and values.
- Never invent schema properties, enum values, or nested sections not supported by the schema or full example.
- If unsure, omit an optional section instead of guessing.
- The spec filename is arbitrary. The only requirement is extension: `.yaml`, `.yml`, or `.json`.
- Preserve the file’s existing format (YAML vs JSON), structure, and naming style when editing.
- Keep key casing and literal values exactly as required (for example `ManyToMany`, `LAZY`).
- Keep entity names, field names, relation targets, and storage names internally consistent.
- Do not rewrite unrelated sections when making targeted edits.

## Useful defaults for suggestions

- Prefer concise minimal valid examples first.
- Add optional sections only when explicitly requested or already present (`docker`, `cache`, `openApi`, `graphql`, `tests`, `additionalProperties`).
- For entity definitions, always include at least one id field and `storageName` for database entities.
- For enum fields (`type: Enum`), include non-empty `values`.
- For `ManyToMany`, include `relation.joinTable` with `name`, `joinColumn`, and `inverseJoinColumn`.
- Prefer examples that are valid for the generator, not just illustrative.