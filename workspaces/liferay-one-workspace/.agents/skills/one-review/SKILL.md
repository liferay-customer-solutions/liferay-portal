---

allowed-tools: [Agent, Bash, Edit, Glob, Grep, Read, Skill, Write]
description: Run a full code review for this workspace — formats source, checks workspace-specific rules, and reviews the diff for correctness and quality.
name: one-review

---

# One Review

Run a complete review pass tailored to this workspace. Combines automated formatting, workspace-specific rule checks, and a correctness/quality review of the diff.

## Flags

- `--fix` — apply all safe corrections automatically (format + lint + code-review fixes)
- `--comment` — post review findings as inline GitHub PR comments
- `--effort <low|medium|high|max>` — passed through to `/code-review` (default: `medium`)

## Step 1: Format

Invoke the `one-format` skill. This runs Gradle `formatSource`, `yarn lint:fix`, and `yarn format` in sequence.

If formatting fails, stop and report the error. Do not continue to review on a broken formatter pass.

## Step 2: Workspace Rule Check

Read the five workspace rule files:

- `.agents/rules/code-style.md`
- `.agents/rules/naming.md`
- `.agents/rules/object-naming.md`
- `.agents/rules/page-folder-structure.md`
- `.agents/rules/pr-hygiene.md`

Get the branch diff:

```bash
git diff "$(git merge-base HEAD liferay-one/master-temp)...HEAD" --name-only
git diff "$(git merge-base HEAD liferay-one/master-temp)...HEAD"
```

Check the diff against each rule manually. The ESLint rules in Step 2 catch structural/naming issues mechanically; this step catches the rules that cannot be automated:

- **code-style.md** — sort order in arrays/objects/JSON, log message phrasing, user-facing text ("IDs" not "Id"), FreeMarker variable block grouping
- **naming.md** — brand name casing (ArgoCD, Grafana, etc.), SVG/CSS filenames use underscores, REST endpoint-to-method mapping, service file naming matches URL
- **object-naming.md** — ERC prefixes (`C_`), PascalCase object names, camelCase fields, `className` format
- **page-folder-structure.md** — every sub-page component has its own subfolder at all depths; utility `.ts` files exempt
- **pr-hygiene.md** — all diff files belong to this workspace, every commit has a Jira ticket prefix

Flag any violation with the file, line (if determinable), and the specific rule from the `.agents/rules/` file.

## Step 3: Code Review

Invoke the `/code-review` skill, passing through any `--fix`, `--comment`, and `--effort` flags the user provided.

The workspace-specific findings from Steps 2 and 3 are already collected; the code review focuses on correctness, logic bugs, and quality issues that the rule-based checks above cannot catch.

## Output

Emit a consolidated report with four sections. Omit any section that has no findings.

```
## Format
PASS — no changes needed
(or) Applied N changes; N lint violations remain (list rule + file for each)

## Workspace Rules
PASS
(or) List each violation: rule name, file, what was wrong

## Code Review
(paste the /code-review output here)
```

If `--fix` was passed, note which fixes were applied automatically vs. which require manual attention.

## Step 4: Learn

After emitting the report, invoke the `one-review-learn` skill. This harvests correction patterns from the current session (uncommitted changes, recent commits, PR comments) and encodes them as durable guardrails so the same issues don't recur.