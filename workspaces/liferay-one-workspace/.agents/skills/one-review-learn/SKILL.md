---

allowed-tools: [Agent, Bash, Edit, Glob, Grep, Read, Write]
description: After a review session (AI or human), extract correction patterns and encode them as workspace rules, ESLint checks, or memory to prevent recurrence.
name: one-review-learn

---

# Review Learn

Harvest correction patterns from a review session and encode them as durable guardrails so the same issue does not recur.

## When to Run

Run after any of these:
- A GitHub reviewer leaves inline comments and you respond to them
- `/code-review` or `/code-review --fix` applies corrections to the branch
- You notice the same type of fix appearing across multiple files

## Signal Sources

Collect from all available sources. Skip any that don't apply.

### 1. GitHub PR Review Comments

```bash
# Resolve the PR for the current branch
PR=$(gh pr view --json number -q .number 2>/dev/null)

# Inline comments (each has path, line, body)
gh api "repos/liferay-one/liferay-portal/pulls/${PR}/comments" --paginate \
  --jq '.[] | {path: .path, line: .original_line, body: .body, resolved: .resolved}'

# Review-level summaries
gh api "repos/liferay-one/liferay-portal/pulls/${PR}/reviews" --paginate \
  --jq '.[] | {state: .state, body: .body}'
```

When no PR exists for the current branch, skip this source.

### 2. Recent Post-Review Commits

Look at the commit log since the branch diverged. Commits with terse messages (SF, cleanup, fixup, update, fix) that follow the initial feature work are correction signals.

```bash
git log --oneline "$(git merge-base HEAD liferay-one/master-temp)...HEAD"
git show --stat <short-sha>   # for any suspect commit
```

### 3. Current Uncommitted Changes

```bash
git diff HEAD         # unstaged
git diff --cached     # staged
```

These are the freshest corrections — direct output of the current review session.

## Analysis

Use an agent to read all collected signals and produce a list of correction patterns. For each pattern, capture:

- **What changed** — a concrete before/after example
- **Why it was wrong** — the rule being violated
- **How general is it** — does it apply to the whole codebase, or just this one file?
- **Category** — one of: `naming`, `structure`, `style`, `pr-hygiene`, `object-naming`, `logic`, `other`

Cluster related comments and diffs that point to the same root issue into a single pattern. A reviewer leaving five "sort this" comments is one pattern, not five.

## Guardrail Selection

For each pattern, pick the highest-enforcement option that fits:

| Pattern type | Detectable in TS/TSX AST? | Where to encode |
| --- | --- | --- |
| Naming — file, class, function | Yes (filename, export name) | New ESLint rule in `tools/eslint-plugin-local/src/rules/` |
| Naming — variable, prop | Sometimes | ESLint rule if automatable; else `.agents/rules/naming.md` |
| File/folder structure | Yes (path patterns) | ESLint rule in `tools/eslint-plugin-local/src/rules/` |
| Sort order in TS/TSX | Yes | ESLint rule |
| CSS/SCSS conventions | No | `.agents/rules/code-style.md` |
| Import conventions | Yes | ESLint rule (prefer `@liferay/eslint-plugin` config first) |
| PR hygiene | No | `.agents/rules/pr-hygiene.md` |
| Object ERCs / field names | No | `.agents/rules/object-naming.md` |
| General code style | No | `.agents/rules/code-style.md` |
| Non-obvious project context | No | Memory (`~/.claude/projects/.../memory/`) |
| Workflow/procedure | No | Skill update |

**Before encoding anything:** check whether the pattern is already covered.

```bash
# Rules docs
grep -ri "<keyword>" /home/ry/repos/liferay-portal/workspaces/liferay-one-workspace/.agents/rules/

# Existing ESLint rules
ls /home/ry/repos/liferay-portal/workspaces/liferay-one-workspace/tools/eslint-plugin-local/src/rules/
```

Skip patterns fully covered. Sharpen a rule if it is partially covered.

## Applying Guardrails

### Rule Doc Update

Append to the most relevant `.agents/rules/<file>.md`. Use the same style as the existing content (table rows for naming rules, fenced code examples for before/after, prose for rationale).

If no existing file fits, create a new one under `.agents/rules/`. After writing, also note in the output which file was updated so the user can review it.

### New ESLint Rule

When a pattern is mechanically detectable in TypeScript/TSX files:

1. **Read an existing rule for template.** Start with `tools/eslint-plugin-local/src/rules/filenameCamelcase.ts` (simple) or `tools/eslint-plugin-local/src/rules/pageFolderStructure.ts` (path-based) depending on what the new rule needs.

1. **Write the rule** to `tools/eslint-plugin-local/src/rules/<camelCaseName>.ts`. Use `@typescript-eslint/experimental-utils`, always export with `export =`. Include the SPDX header from the existing rules.

1. **Register it** in `tools/eslint-plugin-local/src/index.ts`:
   - Add `import <camelCaseName> = require('./rules/<camelCaseName>');`
   - Add `'<kebab-case-name>': <camelCaseName>` to `rules`
   - Add `'local/<kebab-case-name>': 'error'` (or `'warn'`) to `configs.recommended.rules`

1. **Build and verify:**

   ```bash
   cd /home/ry/repos/liferay-portal/workspaces/liferay-one-workspace
   yarn build:plugin && yarn install --check-files
   yarn lint 2>&1 | grep -E "local/<kebab-case-name>|<RuleName>" | head -20
   ```

   The rule should fire on files that match the pattern and be silent on clean files. If it fires unexpectedly, tighten the condition. If it never fires on known violations, widen it.

1. **Fix any existing violations** in the codebase before committing, or downgrade the rule to `'warn'` temporarily and document why in a TODO comment inside the rule file.

### Memory Entry

When a correction reveals something non-obvious about the project that Claude should remember across sessions (a surprising constraint, a naming landmine, a workflow quirk), write a memory entry.

Follow the memory system format — write a file under `~/.claude/projects/-home-ry-repos-liferay-portal/memory/` with the appropriate frontmatter (`type: feedback` or `type: project`), then add a one-line pointer to `MEMORY.md`.

### Skill Update

When a correction exposes a gap in an existing skill's procedure (missing precondition, wrong command, omitted step), append or correct the relevant section of that skill's `SKILL.md`. Keep the change minimal — only what was missing.

## Output

Report what was done, grouped by guardrail type:

```
## Patterns Found

1. <Pattern name>
   - Before: ...
   - After: ...
   - Encoded as: ESLint rule `local/css-filename-upper-camel-case` (tools/eslint-plugin-local/src/rules/cssFilenameUpperCamelCase.ts)

2. <Pattern name>
   - ...
   - Encoded as: `.agents/rules/naming.md` (appended)

## Skipped

- <Pattern>: already covered by `.agents/rules/code-style.md` line 42
```

Keep the report concise. If no actionable patterns emerged (everything was logic/behavior-specific), say so explicitly rather than forcing a rule.