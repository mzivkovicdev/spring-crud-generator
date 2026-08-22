# `_core`

Two shared files that the eleven skills link to instead of duplicating.

| File | What it is | Authority |
| --- | --- | --- |
| [`RULES.md`](RULES.md) | A one-page card of the non-negotiable rules, each naming its owner | **Summary only.** When it disagrees with an owner skill, the owner wins and this card is the defect to fix |
| [`OWNERSHIP.md`](OWNERSHIP.md) | The canonical ownership map, the split-topic seams, and the conflict precedence order | **Authoritative.** When a skill's local seam table disagrees with it, this file wins |

`_core` is not a skill. It has no frontmatter and nothing triggers it. It is reference material the
skills point at, and a place for an agent with a small instruction budget to load the essentials
from.

Neither file replaces reading the owner skill before implementing something it covers. `RULES.md`
states *that* a rule exists; the owner skill states its scope, its exceptions, and what to do at the
edges — and the edges are where the expensive mistakes live.
