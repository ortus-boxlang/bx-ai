# Security & Guardrails Examples

Prompt-injection defense examples for the BoxLang AI module. All examples run
**fully offline** against the built-in `mock` provider — no API keys required.

```bash
# From the examples directory
boxlang security/01-input-sanitizer.bxs
```

## Examples

| Example                          | Demonstrates                                                                 |
| -------------------------------- | ---------------------------------------------------------------------------- |
| `01-input-sanitizer.bxs`         | `InputSanitizerMiddleware` actions (block/strip/flag/log), custom patterns, direct `PromptSecurity::scan()` usage |
| `02-global-security-settings.bxs`| The `settings.security` block, default-on unicode hygiene, flag-then-enforce rollout, the `secure: false` escape hatch |
| `03-mock-provider-testing.bxs`   | The `mock` provider: scripted responses, offline tool-calling loops, request recording for guardrail assertions |

## The layers

1. **Unicode hygiene (default ON)** — NFKC normalization + zero-width/invisible
   character stripping on every inbound user message. Defeats hidden-instruction
   attacks with no configuration. Opt out per flag
   (`security.input.normalizeUnicode` / `stripZeroWidth`) or per request
   (`secure: false`).

2. **Input sanitizer (opt-in)** — heuristic detectors for instruction overrides,
   role impersonation, jailbreak markers, invisible unicode, base64-smuggled
   payloads, and exfiltration URLs. Runs before the LLM call and (optionally)
   on every tool/MCP result. Homoglyph folding on the detection copy prevents
   lookalike-character evasion.

3. **Enable globally** in `boxlang.json`:

   ```json
   "modules": {
       "bxai": {
           "settings": {
               "security": {
                   "enabled": true,
                   "input": { "action": "block" }
               }
           }
       }
   }
   ```

   Or per request with the same shape via the `security` option.

## Rollout recipe

Start with `action: "flag"` in production — requests proceed, findings are
stamped on `chatRequest.providerOptions.securityFindings` and logged to the
`ai` log. Once you've tuned detectors/custom patterns against real traffic,
flip to `action: "block"` (or `strip`).
