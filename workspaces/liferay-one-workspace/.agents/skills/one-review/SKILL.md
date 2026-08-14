---

allowed-tools: [Agent, Bash, Edit, Glob, Grep, Read, Skill, Write]
description: Run a full code review in either lane — formats source, then reviews the diff against the shared review criteria for correctness, concurrency, efficiency, security, and workspace rules. Invoke from the workspace or from the Liferay One scripts repo.
name: one-review

---

# One Review

Run a complete review pass: automated formatting, then the shared review criteria worked against the branch diff, then the automated code review folded in.

What a review covers — the lenses and their weighting, the rule files behind them, the mechanical sweep, the false-positive calibration, the finding format — lives in [`criteria.md`](./criteria.md), not here. This skill is the interactive workflow around it. The `one-team` reviewer charter reads the same file, which is why a finding from either is interchangeable. **New review heuristics go in `criteria.md`.**

**Under `--adversarial`, read [`orchestration.md`](./orchestration.md) first and follow it** — independent passes, who may read the diff, how findings combine. It replaces this session's own reading with delegated passes and governs everything below. Without the flag it is not read at all, and the steps below are worked here as written.

Two things follow from that file being separate. **A session whose own prompt names it a pass ignores `orchestration.md` entirely**, whatever flags it was given, and works Steps 1 through 5 here — that is what stops a pass spawning passes of its own. And everything a pass must obey lives in *this* file, four obligations especially, because their reasons live in the other one. A pass **always runs Step 1 in its check-only form**, whatever flags the run carries: passes share one checkout, and two mutating formatters at once corrupt the tree they are all reading. It **writes no receipt** — Record the Verdict belongs to the session combining the passes, and a pass's receipt is one `/one-pr` later reads as evidence the branch was reviewed. It **stops after Step 5**, no Step 6. And it **records no independence state**, writing no Independence, Passes, or Dropped candidates section, since those describe a combination it cannot see.

## Lanes

Two lanes, one review. Everything lane-specific is in this table; the rest of this file and `criteria.md`'s shared rows apply to both.

| | Workspace lane | Scripts lane |
| --- | --- | --- |
| Reviews | `<WORKSPACE>` — client extensions, objects, site content | `<SCRIPTS>` — `one/` ETL and migration scripts |
| Base ref `<BASE>` | `liferay-one/master-temp` | `liferay-one/main` |
| Step 1 formatter | the `one-format` skill; `one-format --check` under `--read-only` | `bunx prettier --write <touched paths>` then `bun run lint`, from `<TARGET>`; `--check` in place of `--write` under `--read-only` |
| Step 3 criteria rows | the workspace-tagged rows | the scripts-tagged rows |
| Step 4 blast radius | trace into `<WORKSPACE>`, then into `<SCRIPTS>/one/` for anything crossing the contract | trace into `<SCRIPTS>`, then into the workspace's batch definitions and Spring Boot controllers |
| Step 5 automated pass | run `/code-review` | skipped — `criteria.md` explains why the skill does not fit this lane |
| Step 6 learn | the `one-review-learn` skill | the same skill, read from `<WORKSPACE>`, encoding into `<SCRIPTS>/.agents/rules/` and its ESLint config |

**The invoking directory is the lane.** A session rooted anywhere inside the `scripts` checkout is the scripts lane; one rooted inside `liferay-one-workspace` is the workspace lane. That is the default and needs no confirmation. `criteria.md` is read in place from `<WORKSPACE>` in both lanes, like the rule files — resolve `<WORKSPACE>` as `workspaces/liferay-one-workspace` inside a sibling `liferay-portal` checkout, conventionally `../liferay-portal/workspaces/liferay-one-workspace` from the scripts repo's root, and confirm it by finding `client-extensions/liferay-one-batch/batch/` beneath it.

A diff that reaches outside `<TARGET>` is a blocker in both lanes, per `criteria.md`.

## Flags

- `--read-only` — review everything, change nothing in the working tree. Formatting is still verified, through the lane's check-only command rather than by fixing it; Step 6 is skipped because it writes source; no receipt is recorded; `/code-review` runs plain. Every check still runs, so coverage is identical. Two artifacts are still written, both inside the git common dir and neither in the tree: the derived criteria file that pass prompts point at, and the `write-tree` snapshot bounding a re-review round. The tree is what this flag protects, and the git directory is not the tree — the receipts have lived there for the same reason. Use it when something else owns the formatter, or when the caller is bound by a read-only rule; the `one-team` reviewer is, and this flag is what lets it run this skill.
- `--fix` — apply all safe corrections automatically (format + lint + code-review fixes)
- `--comment` — post review findings as inline GitHub PR comments. In the workspace lane this passes through to `/code-review`; in the scripts lane there is no automated pass to carry it, so post the findings directly. Validate every anchor against the PR head before posting — a comment on a line the diff never touched reads as a false positive.
- `--adversarial` — **off by default.** Replace this session's own reading of the diff with two or more independent passes that cannot see each other, combined here, per [`orchestration.md`](./orchestration.md). It exists because a session that wrote the code, or was briefed by something that watched it get built, reviews it worse than a stranger would and cannot tell from inside. It costs what it sounds like: each pass is a full review, so the run is several times a plain one. Reach for it where being wrong is expensive — a migration that writes data, a contract other code depends on, anything going out to a customer — and where the reviewing session is the one that built the thing. A plain run is the right default everywhere else, and nothing below changes without the flag.
- `--effort <low|medium|high|xhigh|max>` — passed through to `/code-review` (default: `medium`); no effect in the scripts lane

`--read-only` contradicts `--fix` and `--comment`. When they arrive together, stop and ask which was meant rather than guessing.

## Step 1: Format

Run the lane's Step 1 command from the Lanes table.

Under `--read-only`, run the check-only form. It is the non-mutating counterpart of every formatter step, so compliance is fully verified and nothing is written. Each violation becomes a finding under the Repo rules lens — report them, do not fix them, and do not re-run the mutating formatter to "confirm."

Otherwise run the mutating form. If formatting fails, stop and report the error — do not review on a broken formatter pass.

A lint or formatter failure the diff did not introduce is not a finding. **Only the session confirms that**, and only in a throwaway worktree — `git -C <TARGET> worktree add --detach <tmp> <BASE>`, run the command there, remove it. In the workspace lane that materializes the whole portal checkout for one formatter run, so add `--no-checkout` and sparse-checkout just the paths the command needs. Never by checking out or stashing in the shared checkout: in a `one-team` run it holds staged work with no commit behind it, so one stash discards the change under review. Under `--adversarial` there is a second reason — passes are reading that tree concurrently, and every one of them would see a different repository than it started on — and there a pass reports such a failure as unconfirmed and leaves the confirming to the session. When it does fail at `<BASE>` too, say so plainly and move on.

## Step 2: Establish the Diff

```bash
T=<TARGET>
BASE=$(git -C "${T}" merge-base HEAD <BASE>)

git -C "${T}" diff "${BASE}...HEAD" --name-only
git -C "${T}" diff "${BASE}...HEAD"
```

**Pin every git call to `<TARGET>` with `-C`, here and everywhere below.** A shell's working directory is not guaranteed to persist between calls — several harnesses reset it — so a bare `git diff` silently reports on whatever repository the process happens to be sitting in. This is not theoretical: it establishes a review's diff against the wrong repo and nothing downstream can detect it, because every later step trusts the diff it was handed. It matters most in exactly the setups this skill already assumes: a worktree, a sibling workspace checkout, two repos open at once.

Include uncommitted work when there is any — `git -C "${T}" diff HEAD` and `git -C "${T}" diff --cached`. Staged-but-uncommitted is a normal shape, not an edge case: a `one-team` run reaches review with everything staged and nothing committed, so `${BASE}...HEAD` is empty there and `git diff --cached` is the whole change. If every one of them is empty, or the base is ambiguous, stop and ask rather than guessing.

Reviewing one specific commit rather than a branch, `merge-base` does not apply — that commit is not an ancestor of `<BASE>`, so it returns the wrong ancestor or nothing. Use the commit and its parent directly (`<SHA>~1..<SHA>`) and say in the report that this is what the review covered.

Handed two object names instead — an `--adversarial` re-review round, per `orchestration.md` — diff them directly, `git -C "${T}" diff <old> <new>`. That two-argument form is the only one valid for both commits and trees; `merge-base` and the three-dot form both reject a tree outright, so a pass that reaches for the recipe above instead of this one stops on a fatal error.

Reviewing a pull request rather than the local branch: fetch its head into a worktree and read the diff there. A review that runs against the local checkout while reasoning about a remote PR reads the base and reports fixed code as broken.

Read the diff in full and note what kind of change it is: feature, refactor, fix, or deletion. Then read enough surrounding context per changed file to judge it — the rest of the class, the callers, the tests. Read what the lenses need, not the whole subsystem.

## Step 3: Work the Criteria

Read [`criteria.md`](./criteria.md) and work it end to end against the diff: the lane's rule files, then every lens in its order, then the mechanical sweep. Apply the rows tagged for this lane and skip the other lane's. Regression risk is the one lens Step 4 owns instead — it reaches outside the diff, so it gets its own pass rather than a paragraph of attention here.

Under roughly two hundred changed lines, work the lenses inline — every subagent re-reads the diff and the rule files, so a fan-out on a small diff costs more than it saves. Past that, group the lenses into a handful of `sonnet` subagents rather than one per lens — correctness with concurrency, efficiency with architecture, security on its own, rules with simplicity — and put the mechanical sweep on `haiku`. That threshold is the whole rule on a plain run. **Under `--adversarial` this step belongs to a pass instead** — see `orchestration.md` — and is worked here only on that file's `orchestrated` fallback, where the threshold drops away: every lens runs in a subagent at any size, grouped as above, with prompts carrying pointers and the acceptance criteria only, nothing this session remembers or was told. Give each subagent the diff scope, its lenses, and the rule files behind them; set the model explicitly on every `Agent` call.

Verification and the final judgment stay in this session. Under `--adversarial` they come with one limit: a candidate may be dropped here only where its citation is factually wrong, never on a judgment that the code handles it, and judgment goes to two separately spawned adjudicators per `orchestration.md`, every drop reported with the route that made it.

Cross-repo consistency is a lens, not an afterthought: verify every ERC, field name, endpoint path, and payload shape the diff touches against the other repo, per that lens in `criteria.md`.

## Step 4: Blast Radius

The diff is the trigger for this step, not its boundary. Work the Regression risk lens in `criteria.md` as its own pass — it is the one lens whose whole subject is code the diff never touched, so a review that folds it into reading the diff has already skipped it.

**This step runs on every review, at any diff size.** The Step 3 size heuristic governs how the *lens* work is split; it does not apply here. A one-line change to a shared method has a larger blast radius than a two-hundred-line change to a leaf file, so the diff's size predicts nothing about the size of this step.

1. **Build the symbol list.** Derive it from the diff text, not from what the change set out to touch. Read the added and removed lines and take every identifier they declare, rename, or delete, plus every string literal shaped like a contract — an ERC, an endpoint path, a list-type value, a config key, an environment variable, a local-store column. Signatures, exported components and hooks, service methods, payload shapes, and shared types all fall out of that pass. Then drop what is genuinely private to a single file and **name every drop and its reason in the report**. Build the list mechanically because the alternative is recalling which symbols mattered, and recall returns the symbols this diff edited rather than the ones other code references.

1. **Find every reference.** Grep each symbol across `<TARGET>` and across the other repo per the Lanes table — by identifier and by string form both, since ERCs, endpoint paths, and dynamic keys never appear as identifiers. This is pure search, so fan it out: one `haiku` subagent per group of symbols, issued in a single message so they run concurrently, each returning `file:line` references and nothing more. Do not ask a subagent whether a call site is broken — that judgment stays here.

1. **Read the call sites and judge them.** Against the new behavior, not the old, with `criteria.md`'s hardest-first list in hand — behavior changed behind an unchanged signature, parameters reordered where the types still line up, a newly nullable return, a caller's `catch` that no longer matches. Where the references are many, group them by calling module and hand each group to a `sonnet` subagent with the old and new behavior spelled out and a bounded deliverable; verify anything it returns yourself before it becomes a finding. Under `--adversarial` this step belongs to a pass; on that file's `orchestrated` fallback, where it is worked here, the call-site reading is delegated at any reference count above zero — "that caller is fine" is the judgment contamination makes from memory of the change's intent — and an empty search is recorded as a zero rather than handed to a reader to confirm an absence.

1. **Report the coverage.** Which symbols were traced, how many references each had, and which call sites were read — even when nothing was found — plus each symbol dropped as file-private and the reason it dropped. An unstated trace is indistinguishable from one that never happened, and an unstated drop from a symbol nobody thought of.

Set the model explicitly on every `Agent` call. When the session cannot spawn subagents, do the tracing inline and say so; never drop the step for lack of a fan-out.

## Step 5: Automated Code Review

Workspace lane: run the automated pass as `criteria.md` describes, passing any `--fix`, `--comment`, and `--effort` flags through to `/code-review`. Under `--read-only` the invocation is plain apart from `--effort`.

Scripts lane: skip it, per the Lanes table.

## Output

One consolidated report, using the severity tags and finding format from `criteria.md`. Omit Mechanical when it found nothing. Everything else is stated either way — Format's one-line PASS included, since it is coverage like any other. A missing section is indistinguishable from a step that never ran.

**Four sections belong to `--adversarial` alone** and are omitted entirely on a plain run: Independence, Passes, Dropped candidates, and Completeness pass. They report on machinery a plain run does not have.

Under `--adversarial` each pass produces this shape individually and what reaches the reader is the combination, per Combining the Passes in `orchestration.md`: this session adds the Independence, Passes, and Dropped candidates sections — the parts no single pass can know — and changes nothing else a pass wrote except to merge duplicates and set severity. On a plain run those three sections are omitted and this session writes the report itself.

```
## Independence            (--adversarial only)
SELF | BRIEFED | FRESH, and reading: fresh | orchestrated | contaminated
— never omitted under the flag. Wherever this session did the
orchestrating, the change kind and, per changed file, what was read
around it. Where passes ran, their own coverage statements are that
evidence and this session adds nothing it did not do.

## Passes                  (--adversarial only)
How many ran, and their overlap — how many findings appeared in more than
one. A single pass is stated as such, with the reason there was only one.

## Format
PASS — no changes needed
(or) Applied N changes; N lint violations remain (rule + file for each)
(or, read-only) CHECKED — N violations, nothing written (rule + file for each)

## Findings
Grouped by lens, in the criteria.md order — rule violations and verified
/code-review hits included under their lens, never in sections of their own.
Under --adversarial each finding carries its corroboration count
(2/2, 1/3 verified here, …) as provenance, never as confidence —
agreement between passes does not make a finding right.
A lens with nothing to report still gets its one-line coverage statement
here, per the Evidence rule.

## Dropped candidates      (--adversarial only)
Every candidate not promoted to a finding, each tagged `fact` (with the
file:line read showing the citation was wrong) or `adjudicated` (with both
rejections), plus any severity down-rated the same way. "None" is itself
the datum, in every state.

## Completeness pass       (--adversarial only)
What the fresh completeness reader named against the combined report, and
what came of each. Stated even when it named nothing. Owed wherever
subagents could be spawned at all.

## Blast radius
Each traced symbol, its reference count, and what was read, plus each
symbol dropped as file-private and why. Stated even when it found
nothing; findings themselves go under Regression risk above.

## Mechanical
Identifier typos, string typos, then whitespace grouped by type

## Verdict
APPROVED | CHANGES_REQUESTED — one line of reasoning
```

If `--fix` ran, say which fixes were applied automatically and which need a human.

## Record the Verdict

Leave a receipt, so `/one-pr` can tell whether this branch was reviewed and at which commit:

```bash
T=<TARGET>
RECEIPTS="$(git -C "${T}" rev-parse --path-format=absolute --git-common-dir)/one-review/receipts"

mkdir -p "${RECEIPTS}"

{
	echo "verdict: <APPROVED|CHANGES_REQUESTED>"
	echo "commit: $(git -C "${T}" rev-parse HEAD)"
	echo "branch: $(git -C "${T}" rev-parse --abbrev-ref HEAD)"
	echo "lane: <workspace|scripts>"
	echo "mode: <standard|adversarial>"
	echo "independence: <SELF|BRIEFED|FRESH>"   # --adversarial only; omit otherwise
	echo "reading: <fresh|orchestrated|contaminated>"   # --adversarial only
	echo "tree: $([ -z "$(git -C "${T}" status --porcelain)" ] && echo clean || echo dirty)"
	echo "reviewed: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "${RECEIPTS}/$(git -C "${T}" rev-parse HEAD)"
```

`--path-format=absolute` is not decoration: `--git-common-dir` alone returns a path relative to the repository, so a bare `$(git rev-parse --git-common-dir)` resolves against whatever directory the shell is sitting in and writes the receipt into the wrong tree — or into no repository at all.

It lives inside the git directory, so it is never tracked, never reaches a PR diff, and needs no `.gitignore` entry. Use `--git-common-dir` rather than `--git-dir`: the latter is per-worktree, so a review run in a worktree would be invisible when the pull request goes out from the main checkout. The common directory is shared by every worktree of the repo, and since receipts are keyed by commit SHA there is nothing to collide.

Key it to the reviewed commit: a receipt is evidence about that commit and nothing later. Record `tree: dirty` honestly when the review covered staged or uncommitted work — a review of a working tree is not a review of whatever gets committed afterward, and `/one-pr` is right to ask again.

`mode` is always recorded. `independence` and `reading` are written only under `--adversarial`, and a receipt without them simply means a standard run — not a missing field. Under the flag they matter together: `independence` says whose session answered for the branch, `reading` says where the work that produced the findings actually happened, and only the pair tells a later reader anything. `SELF` with `reading: fresh` is a delegated review worth what a fresh one is worth; `SELF` with `reading: contaminated` is the weakest thing this file can carry, and the same two words would have covered both. None of them fails `/one-pr`, which surfaces them rather than blocking.

Skip this under `--read-only`, which writes no receipt. The caller owns the record there; for the `one-team` reviewer that record is `review.md`.

## Step 6: Learn

Skip under `--read-only` — it writes rule files and memory, and a review whose findings are not yet adjudicated has nothing settled to harvest. Whoever owns the change runs it once the dust clears.

Otherwise invoke the `one-review-learn` skill, encoding into the lane's rule files per the Lanes table. It harvests correction patterns from this session — uncommitted changes, recent commits, PR comments — and encodes them as durable guardrails so the same issues do not recur.