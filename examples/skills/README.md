# AI Skills Examples

This directory contains examples demonstrating the AI Skills system — composable,
reusable knowledge blocks that can be injected into any model or agent.

## Sample SKILL.md Files

The `.ai/skills/` subdirectory contains three example skills you can run the
file-based examples against:

| Directory | Skill |
|-----------|-------|
| `sql-optimizer` | SQL query optimisation best practices |
| `writing-style` | Concise prose and documentation guidelines |
| `boxlang-expert` | BoxLang language best practices |

## Examples

| File | What it shows |
|------|---------------|
| `01-inline-skills.bxs` | Create skills in code (no files needed) and attach to a model |
| `02-file-skills.bxs` | Load `SKILL.md` files from disk — single file or whole directory |
| `03-agent-with-skills.bxs` | Agent with **always-on** skills injected on every call |
| `04-lazy-skills.bxs` | Agent with **lazy** skill pool — only index included until `loadSkill()` is called |
| `05-pipeline-skills.bxs` | Skills inside a model pipeline |
| `06-global-skills.bxs` | Global skill pool auto-injected into every agent |

## Running an Example

```bash
# From the examples/ directory (requires a running Ollama or API key)
box bx:run skills/01-inline-skills.bxs
```

## SKILL.md File Format

```markdown
---
description: What this skill does and when to apply it.
---

## Skill Title

Your instruction content here.  The LLM receives this verbatim when the skill
is active.
```

> **Tip:** If frontmatter is absent, the first paragraph of the body is used as
> the description automatically.

## Key BIFs

| BIF | Purpose |
|-----|---------|
| `aiSkill( path )` | Load from a file or directory |
| `aiSkill( name, description, content )` | Create inline, no files needed |
| `aiGlobalSkills()` | Get the globally shared skill pool |

## Key Methods

| Method | Where | Purpose |
|--------|-------|---------|
| `withSkills( skills )` | `AiModel` / `AiAgent` | Always-on skills |
| `withAvailableSkills( skills )` | `AiModel` / `AiAgent` | Lazy skill pool |
| `addSkill( skill )` | `AiModel` / `AiAgent` | Add single always-on skill |
| `addAvailableSkill( skill )` | `AiModel` / `AiAgent` | Add single lazy skill |
| `activateSkill( name )` | `AiModel` / `AiAgent` | Promote lazy → always-on |
| `buildSkillsContent()` | `AiModel` / `AiAgent` | Render skills system-message block |
