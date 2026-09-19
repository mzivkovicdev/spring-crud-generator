# Engineering standards for coding agents

This folder holds the project's engineering standard for Java 21+ Spring Boot, written as
[Agent Skills](https://agentskills.io/specification): thirteen skills in `skills/`, plus `skills/_core/`,
which is shared reference material and not a skill. The same folder serves Codex, GitHub Copilot, and
Claude Code.

Do not edit these files inside a project. They are maintained in one source repository and copied into
every project, so a local change is overwritten by the next update.

## How each agent finds the skills

| Agent | Setup | Call a skill by name |
| --- | --- | --- |
| Codex (CLI, IDE extension) | None; Codex reads `.agents/skills` | `$modern-java-21` |
| GitHub Copilot in VS Code | None | `/modern-java-21` |
| GitHub Copilot in IntelliJ | An organization admin enables the "Editor preview features" policy; then Settings > GitHub Copilot > Chat > Agent > Agent Skills | `/modern-java-21` |
| GitHub Copilot CLI | None; `/skills list` shows what loaded | `/modern-java-21` |
| Claude Code | Accept the trust prompt the first time you open the project. `.claude/settings.json` then loads this folder as the plugin `backend-standards`, so there is no copy of the skills under `.claude/`. After a new version of the skills arrives, run `claude plugin update backend-standards@backend-standards-local --scope project` | `/backend-standards:modern-java-21` |

Agents choose skills on their own from each skill's description. Calling a skill by name is only
needed when an agent missed one.

## What else belongs in a project

One file outside this folder: `.claude/settings.json`, which points Claude Code at this folder. Codex and
Copilot read `.agents/skills` directly and ignore it. When a project already has its own
`.claude/settings.json`, merge the two keys `extraKnownMarketplaces` and `enabledPlugins` into it instead
of replacing the file. Never copy the skills into `.claude/skills`: GitHub Copilot reads that folder too
and would list every skill twice.

One optional, one-time step: the confidentiality block in
[`application-security`](skills/application-security/references/data-protection-and-confidentiality.md#one-time-repository-setup)
can be added to the project's root `AGENTS.md`, so it applies even before any skill is loaded.

## Layout

```text
.agents/
├── README.md                 # this file
├── .claude-plugin/
│   ├── plugin.json           # this folder as a Claude Code plugin
│   └── marketplace.json      # lets .claude/settings.json find that plugin
└── skills/
    ├── _core/                # ownership map, routing table, rules card; not a skill
    └── <skill>/              # SKILL.md, references/, assets/
```
