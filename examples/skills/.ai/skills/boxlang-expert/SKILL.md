---
description: BoxLang language expertise — apply when writing, reviewing, or explaining BoxLang code.
---

## BoxLang Expert Rules

- **No semicolons required** — BoxLang treats newlines as statement terminators
- **Use `var` for local scope** inside functions to avoid variables leaking into the variables scope
- **Prefer `jsonSerialize()` / `jsonDeserialize()`** over the deprecated CFML-era equivalents
- **Imports belong at the top of the class**, not inside methods
- **Static utility methods** are called with the `::` operator: `TextChunker::chunk( text, options )`
- **Static variables** are accessed via the `static.` scope prefix inside instance methods
- **Property getters/setters are auto-generated** — do not manually define them for `property` declarations
- **Cast types** with the `castAs` operator (`value castAs "float"`), not `javaCast()`
- **Avoid reserved scope names** as variable names: `server`, `request`, `session`, `url`, etc.
