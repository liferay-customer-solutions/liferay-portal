---

allowed-tools: [Agent, AskUserQuestion, Bash, Edit, Glob, Grep, Read, SendMessage, Skill, TaskCreate, TaskGet, TaskList, TaskUpdate, Write]
description: Use when a Jira ticket should be taken from design through implementation, end-to-end testing, and final review by a coordinated agent team (planner, developer, tester, reviewer) in one session. Invoke as /one-team <TICKET> plus any extra context or constraints.
name: one-team

---

# One Team

Run a four-agent team — planner, developer, tester, reviewer — that takes a Jira ticket from context gathering to reviewed, committed code on a ticket-named branch. The session that invokes this skill is the **coordinator**: it spawns the teammates, relays every handoff, enforces the phase gates, arbitrates disagreements, and keeps the team log. The coordinator never plans, writes, tests, or reviews the work itself — it orchestrates the specialists who do.

Run from `workspaces/liferay-one-workspace/`. A session rooted elsewhere may not expose `/one-team` by name — read this file directly and follow it; everything else works the same.

## Invocation

```
/one-team LPD-12345 [extra context, constraints, links]
```

A ticket ID is required; when missing, ask for one before doing anything else. Everything after the ticket ID is kickoff context — pass it verbatim into the planner's briefing.

## The Team

| Role | Model | Charter |
| --- | --- | --- |
| Planner | `fable` | `.agents/skills/one-team/roles/planner.md` |
| Developer | `opus` | `.agents/skills/one-team/roles/developer.md` |
| Tester | `sonnet` | `.agents/skills/one-team/roles/tester.md` |
| Reviewer | `fable` | `.agents/skills/one-team/roles/reviewer.md` |

Spawn each teammate with the `Agent` tool using `subagent_type: "claude"`, `run_in_background: true`, `name` set to the role (planner, developer, tester, reviewer), and the `model` from the table. Never let a teammate default to the session model — the split is deliberate: frontier reasoning where judgment concentrates (the plan, the final review), cheaper models where the work is more mechanical. When the harness does not offer `fable`, fall back to `opus` and say so in the kickoff status.

The tiering cascades: subagents spawned by any teammate always run on `haiku` or `sonnet` — research sweeps, file inventories, log scans, isolated mechanical edits. Frontier tokens are reserved for the teammates' own reasoning. A teammate whose runtime cannot spawn subagents (some execution modes restrict nesting) does that work itself rather than blocking on the missing capability.

**Small-ticket lane.** Two downgrades are available when the work is genuinely small, each keyed to evidence that exists at the moment the role is spawned. When the user's kickoff context frames the ticket as small or trivial, spawn the planner on `opus`. When the drafted `plan.md` — which the coordinator reads anyway to summarize it for the user — reports no data-model impact, no new objects or endpoints, and roughly fifty changed lines or fewer, spawn the developer on `sonnet` for its Phase 2 review and the implementation that follows. Should the review or the user's approval then enlarge the plan past those bounds, respawn the developer on `opus` before Phase 3 and log the swap; the plan is a file, so the replacement loses nothing. The reviewer stays on `fable` in every case — it is the last gate — and the tester's end-to-end pass is never trimmed. Log whichever downgrades apply.

## How Teammates Actually Work

These mechanics were verified live; the protocol depends on them:

- A background teammate is **turn-based**, not a live process. It handles the message it was given, acts, replies, and stops. `SendMessage` to it resumes it with its full context intact. Idling is free once spawned, but the spawn is not — a measured thirty to sixty thousand tokens of baseline context per agent, before it does any work. Spawn each role just in time, at its first real assignment, and keep it alive from there.
- Because teammates are spawned with `name` set to the role, `SendMessage` addresses them by that name and the agents panel lists them readably. Still record the role → agent ID mapping from each spawn result in the team log: a reused name belongs to the newest agent, so IDs disambiguate respawns. Never show raw agent IDs to the user — say "the planner", "the developer".
- Teammates reply by calling `SendMessage` with `to: "main"`. Replies arrive at the coordinator automatically; there is no inbox to poll. A teammate's final text also arrives in its completion notification — the fallback when a teammate forgets to message.
- A teammate's **background subagent** reports its completion to the coordinator, not to the teammate that spawned it — a teammate that stops to wait for its own background child stalls until nudged. The charters therefore require synchronous subagents; when a grandchild result lands on the coordinator anyway, persist it to the team directory and resume the owning teammate with the path. Background **commands** are different: they re-invoke their owner and are safe. Synchronous is not serial: independent subagents issued in a single message run concurrently.
- A turn whose only output is `SendMessage` calls looks empty to the harness, which re-prompts the agent — and can loop it. Teammates end every turn with a short line of plain final text after their messages.
- After dispatching work, end the turn with a one-line status for the user. The teammate's reply resumes the session.
- Teammates share the filesystem. Handoffs carry **paths, not contents** — point at `plan.md`, list the touched files, reference the diff. Pasting file bodies into messages pays for them twice.

Spawn prompts follow one shape: the role name; the ticket ID; the absolute team directory path; the instruction to read the role's charter copy in `.one-team/<TICKET>/roles/` first (absolute path — the teammate reads it itself, so the coordinator never loads charters into its own context); the kickoff context; and the first assignment. That first assignment is always real work — never an acknowledgment. The planner spawns in Phase 1, the developer for the Phase 2 plan review, the tester with its prep dispatch, the reviewer at its first pass; each then stays alive for the rest of the run.

## Team Directory

All run artifacts live in `.one-team/<TICKET>/` at the workspace root (gitignored):

| File | Writer | Content |
| --- | --- | --- |
| `team-log.md` | coordinator | roster, phase gate outcomes, agreements, arbitrations, escalations |
| `ticket-digest.md`, `initiative-digest.md` | coordinator | the Jira context teammates actually read — flattened ticket, one line per initiative issue |
| `ticket.json`, `initiative.json` | coordinator | raw Jira responses, kept only for targeted `jq` lookups — never read whole |
| `roles/` | coordinator | charter copies frozen at kickoff — what every spawn prompt points at (the ticket branch may predate the skill) |
| `plan.md` | planner | the implementation plan (template in the planner charter) |
| `dev-handoff.md` | developer | Phase 3 handoff: changes, per-AC verification hints for the tester, notes |
| `test-report.md` | tester | AC matrix, regression matrix, evidence, verdict per round |
| `review.md` | reviewer | findings with severity and verdict per round |

The coordinator creates the directory and `team-log.md` at kickoff and appends a log entry at every gate. Artifacts persist across sessions — they are how an interrupted run resumes.

## Jira Context

Read-only. Never transition tickets or post comments from this workflow.

```bash
# The ticket
curl --silent --user "${JIRA_API_USER}:${JIRA_API_TOKEN}" \
	"https://liferay.atlassian.net/rest/api/3/issue/<TICKET>"

# Every ticket under the One Liferay initiative, for surrounding context
curl --silent --get --user "${JIRA_API_USER}:${JIRA_API_TOKEN}" \
	--data-urlencode 'jql=issue in portfolioChildIssuesOf("LPD-87600") ORDER BY key' \
	--data-urlencode 'fields=issuetype,status,summary' \
	"https://liferay.atlassian.net/rest/api/3/search/jql"
```

The search endpoint paginates — pass `maxResults` and follow `nextPageToken` until the initiative list is exhausted. Build the token flag with an explicit branch, not `${TOKEN:+--data-urlencode "nextPageToken=${TOKEN}"}` — zsh does not word-split that expansion, so curl receives one malformed argument:

```bash
TOKEN=""
while :; do
	if [ -n "${TOKEN}" ]; then RESP=$(curl <flags as above> --data-urlencode "nextPageToken=${TOKEN}" "<url>"); else RESP=$(curl <flags as above> "<url>"); fi
	echo "${RESP}" >> pages.jsonl
	TOKEN=$(echo "${RESP}" | jq -r '.nextPageToken // empty')
	[ -z "${TOKEN}" ] && break
done

jq -s '{issues: [.[].issues[]]}' pages.jsonl > initiative.json
```

When `portfolioChildIssuesOf` is unavailable, fall back to `parent = LPD-87600` and walk one level down. Save the responses into the team directory so teammates read files instead of refetching.

Then digest them, because the raw responses are far too large to read: a 584-issue initiative measured about a hundred and eighty thousand tokens, and one rich ticket about thirteen thousand. These recipes were run against exactly that data and reduced them to roughly fourteen thousand and four hundred tokens respectively — thirteenfold and thirtyfold — with the acceptance criteria, dev notes, and dependencies intact:

```bash
jq -r '.issues[] | "\(.key) | \(.fields.issuetype.name) | \(.fields.status.name) | \(.fields.summary)"' \
	initiative.json > initiative-digest.md

{
	jq -r '"# \(.key) — \(.fields.summary)\n\nType: \(.fields.issuetype.name)\nStatus: \(.fields.status.name)\n"' ticket.json
	jq -r '.fields.description | [.. | objects | select(.type == "text") | .text] | join(" ")' ticket.json
} > ticket-digest.md
```

The description is Atlassian Document Format, hence the text-node flatten — verify the acceptance criteria survived it, and fall back to reading `.fields.description` alone when a ticket uses tables or panels the flatten mangles. Brief teammates on the digests; the raw JSON stays on disk for `jq` when someone needs a field the digest dropped.

Validate before digesting or briefing anyone: `ticket.json` must contain the requested issue key. When it does not, stop and tell the user which it is — credentials (`JIRA_API_USER`/`JIRA_API_TOKEN` unset or rejected) or an unknown ticket. A planner briefed on an error body plans garbage.

## Where the Answers Live

Teammates ground every decision in existing specs and code, in this order:

| Source | Path | Use |
| --- | --- | --- |
| Workspace specs | `.agents/specs/` | source of truth for entities, ERCs, APIs, UI |
| Workspace rules | `.agents/rules/` | review standards the reviewer enforces |
| Workspace skills | `.agents/skills/` | environment, deploy, format, PR conventions |
| Workspace code | `client-extensions/` | the patterns new code must match |
| Liferay Portal source | `../..` (this workspace lives in `liferay-portal/workspaces/`) | canonical platform patterns |
| Legacy OSB portal | `../../../liferay-portal-7.2.x/modules/dxp/apps/osb/` (branch `7.2.x-temp`) | prior provisioning, Koroneiki, and messaging behavior |
| Legacy Koroneiki server configs | `../../../lfris-koroneiki` | prior osb-koroneiki server configuration |
| Legacy provisioning server configs | `../../../lfris-provisioning` | prior osb-provisioning server configuration |
| Legacy customer portal | `../../../liferay-portal-7.0.x/modules/dxp/apps/osb/osb-customer/` | prior customer.liferay.com behavior |
| Legacy support portal | `../liferay-customer-workspace` | prior support.liferay.com behavior |
| Legacy marketplace | `../liferay-marketplace-workspace` | prior marketplace behavior |

Legacy sources answer "how did this behave before" — they are behavioral references, never style references. New code follows current workspace patterns. When a legacy checkout is missing on this machine, note the gap in the plan instead of guessing.

## Phase Protocol

Seven phases. Each has an owner, an exit gate, and a `team-log.md` entry; no phase's gate can pass before the previous gate is logged — verdicts and confirmations keep their order even where work overlaps (tester prep during Phase 3 and the reviewer's early pass during Phase 4 are the two sanctioned overlaps). Gates are evidence-based, not immutable: when later evidence invalidates a logged gate — a regression surfacing after `APPROVED`, a failed retest — the coordinator reopens the run at the earliest affected phase, logs why, and the standard loops rerun. Ship never proceeds over a known-stale gate. Mirror the phases on the shared task board at kickoff — one task per phase, chained with `addBlockedBy` — and advance statuses as gates pass. The coordinator owns the board; teammates report through messages.

This checkout is shared — other sessions and the user can move it mid-run. Before logging any gate and before dispatching any phase assignment, verify `git branch --show-current` prints `<TICKET>`; on a mismatch, freeze the team with HOLD messages, read the reflog to see what happened, and escalate to the user before anything else runs.

### Phase 0 — Kickoff (Coordinator)

1. Verify the working directory is the workspace root.

1. Resume check — before any tree-state judgment: when `.one-team/<TICKET>/team-log.md` exists, follow Resuming an Interrupted Run instead of continuing here; a resumed run's staged, uncommitted work is its persisted state, not a dirty tree. A branch named `<TICKET>` with no team log is a leftover, not a resume: when `git cherry liferay-one/master-temp <TICKET>` shows its work already upstream (the usual case for follow-ups on completed tickets), rename it aside — `git branch -m <TICKET> <TICKET>-pre-one-team` — and continue; when it carries unique unmerged commits, stop and ask the user which base to build on.

1. Fresh runs only: `git status --porcelain` must be clean. Dirty tree → stop and ask the user.

1. Make the team directory unstageable: append `/workspaces/liferay-one-workspace/.one-team/` to `../../.git/info/exclude` unless already present (repo-local — the committed `.gitignore` entry may not exist on the ticket branch, and the developer stages with `git add --all`).

1. Create the team directory and `team-log.md` (ticket, date, phase checklist, roster placeholder), and copy the four charter files into `.one-team/<TICKET>/roles/` — every spawn prompt points at these copies.

1. Fetch the ticket and initiative JSON into the team directory and validate them (see Jira Context).

1. `git fetch liferay-one master-temp`, then `git checkout -b <TICKET> liferay-one/master-temp`.

1. Create the phase tasks on the task board.

1. Spawn the planner, whose first assignment is Phase 1; log the roster and extend it as the other roles spawn.

### Phase 1 — Plan (Planner)

Brief the planner with the digest paths, the user's kickoff context, and the assignment: produce `plan.md` per its charter.

The planner researches broadly and **asks instead of guessing**: `QUESTION` messages come to the coordinator, which answers from established run context or puts the question to the user via `AskUserQuestion`, then relays the answer verbatim.

Exit gate: `plan.md` written; planner reports `DONE`.

### Phase 2 — Plan Review (Developer) and the Human Gate, Concurrently

Dispatch both reads at once: spawn the developer with this review as its first assignment — read `plan.md` critically before any code exists — feasibility, missing steps, pattern conformance, testability, scope — and in the same breath post the compact plan summary to the user in chat (goal, approach, files, test plan, open risks), point at `plan.md`, and ask them to approve or request changes. **Phase 3 starts only when both the developer–planner agreement and the user's approval are in.**

Developer objections are relayed to the planner for revision; loop until **both explicitly agree**. When they still disagree after one rebuttal round each, take both positions to the user instead of forcing agreement. When a revision lands while the user is still reading, tell them what changed — an approval given on stale text is re-confirmed against a one-line delta. Log the outcome and any accepted risks.

### Phase 3 — Implement (Developer)

Dispatch two assignments the moment the plan gate closes: the developer implements `plan.md` under its charter's rules, and the tester — spawned now, prep as its first assignment — runs that prep in parallel — environment up and healthy, test matrix pre-built from the plan (its charter's Prep section; nothing in it needs the diff). While this phase runs:

- The developer is the **only writer** of repository files. Nobody else — planner, tester, reviewer, coordinator — edits them, ever. The `.one-team/` artifacts are the one exception: each role maintains its own, per the artifact table.
- Deviations from the plan are flagged to the coordinator; material design changes go back to the planner for agreement before proceeding.
- Done means `./gradlew formatSource build` passes, unit tests exist where the workspace already has patterns for them, and everything is staged with `git add --all` — no commits.

Exit gate: developer writes `dev-handoff.md` (touched files, change summary, per-AC verification hints mapped to the plan's test scenarios, notes) and reports `DONE` with the path.

### Phase 4 — Deploy and Test (Tester)

The tester — already prepped, briefed with `dev-handoff.md` and the plan's Test Plan — deploys the staged work and proves it per its charter: client extensions deployed (the tester rebuilds the `liferay-one-etc-spring-boot` image itself — a near-instant no-op when the developer's background warm-up finished — recreates its container, and confirms pickup in the logs), then end-to-end verification through the real UI at `http://localhost:8080` — logged in, exercising every acceptance criterion — plus a **regression sweep**: every flow that consumes code the developer touched gets exercised too, with container logs watched for new errors throughout.

`FAIL` goes back to the developer with reproduction steps, the developer fixes under Phase 3 rules, the tester redeploys and retests the failed cases plus the fix's blast radius. Loop until the full matrix passes on the currently deployed build.

Exit gate: `test-report.md` complete; developer and tester both explicitly confirm the acceptance criteria are met with no regressions. Log the joint agreement.

### Phase 5 — Final Review (Reviewer)

Spawn the reviewer — unless the early pass below already did — and brief it with the plan, test report, and diff scope (`git diff liferay-one/master-temp` — the work is staged, so this shows everything, new files included). The reviewer works read-only per its charter.

For small diffs (roughly under two hundred changed lines), the coordinator may start the reviewer's rule-reading and automated pass during Phase 4, with the verdict held until the tester's `PASS` lands — a `FAIL` that changes the diff voids the early pass. Findings are only ever issued against the tested, final diff.

`CHANGES_REQUESTED` → developer fixes (Phase 3 rules) → **tester retests the fixes and their blast radius** (Phase 4 rules — a `PASS` on those rows suffices in these rounds; the full joint confirmation is not re-taken) → reviewer re-reviews. Every finding ends adjudicated: fixed, or explicitly rejected with a reason the reviewer accepts. Loop until `APPROVED`.

Exit gate: reviewer's `APPROVED` logged.

### Phase 6 — Ship (Developer Commits, Everyone Signs)

1. The developer runs the format pass (`.agents/skills/one-format/SKILL.md`) and `./gradlew build` one final time. The pass must produce no diff — when it changes anything, re-stage and return to Phase 5 for a delta re-review before continuing.

1. The developer composes the commits: minimal and organized — one commit is the default; split only when the history is genuinely clearer for the human reviewer (for example, regenerated output apart from hand-written code). Every message reads `<TICKET> <concise summary>` — sentence case, no trailing period, under 72 characters. Plain `git commit` under the user's git identity: **never** add Claude as author or co-author, no `Co-Authored-By` trailer, no tool attribution anywhere.

1. The coordinator verifies: `git log liferay-one/master-temp..HEAD --format='%an %s'` shows the user as author and the ticket prefix on every commit; `git diff liferay-one/master-temp --name-only` shows only files in this ticket's scope (`.agents/rules/pr-hygiene.md`); the working tree is clean.

1. The reviewer takes one last look at the commit structure — message quality, nothing stray. A problem named here follows the usual adjudication loop: the developer amends the commits (soft-reset and recommit when structure or messages are wrong), the coordinator re-runs its step 3 checks, the reviewer looks again.

1. **Do not push. Do not open a PR** (unless the user has explicitly ordered it — log the override). Report to the user: what was built, where the plan/test/review artifacts live, the commit list, the branch name, and that `/one-pr` is the next step once they are satisfied. Warn them that `liferay-one/master-temp` force-rewrites frequently — a branch that sits unmerged shows PR "conflicts" even when its files are untouched; re-rebasing onto the current tip and force-pushing with lease fixes it in seconds, and prompt merging avoids it entirely.

## Communication Rules

- Hub and spoke for everything that matters: handoffs, verdicts, gates, escalations, and disagreements go through the coordinator and into `team-log.md` as they happen, not retroactively. Pure clarification questions between teammates go directly — address the role name (a verified mechanic) — with the exchange reflected in the asker's next report to main; anything touching scope, design, verdicts, or gates returns to the spoke.
- Every teammate reply starts with a status word — `DONE`, `PASS`, `FAIL`, `BLOCKED`, `PROGRESS`, `QUESTION`, `APPROVED`, `CHANGES_REQUESTED` — followed by the payload. Each charter names the subset its role uses. `PROGRESS` is non-terminal: a milestone heartbeat during long phases (environment ready, deploy confirmed in logs, matrix row N of M, build green); the coordinator relays a one-liner to the user and expects no reply.
- The user outranks the protocol: a mid-run user instruction that overrides a rule — ship before review, push, skip a phase — is followed and logged as a user override, never resisted and never silently absorbed. Work it displaces (a deferred final review, for example) is recorded in the log as still owed.
- Every dispatch names the phase, the assignment, the artifact paths, and what done looks like — complete, in one message. Each extra round-trip re-processes that teammate's whole transcript, so one full dispatch costs less than three partial ones, and a probe only pays for itself when a circuit breaker calls for it.
- Disagreements get one rebuttal round per side. Execution-level disputes (fix approach, finding severity, retest scope) are then decided by the coordinator, reasoning logged. Disputes over the plan's content, the ticket's scope or meaning, or anything that would overturn a user-approved plan go to the user with both positions summarized.
- Nothing ships unexamined: the plan is reviewed by the developer, the code by the tester and the reviewer, the test report by the reviewer, every review finding by the developer (adjudication), and the commits by the coordinator and the reviewer. At least one other teammate has analyzed every artifact — that invariant is not negotiable.

## Circuit Breakers

- Three dev–test rounds without the failure count shrinking, or six rounds total regardless of progress → stop, summarize both positions with evidence, escalate to the user.
- Three review rounds without `APPROVED` → same.
- Any `BLOCKED` reply the coordinator cannot clear itself, or three failed attempts at a single gate (a build that will not go green, an environment that will not start, missing credentials) → same.
- Round tallies go into the `team-log.md` gate entries as they happen, so a resumed run inherits its breaker counts instead of resetting them.
- A background command observably finished (process gone, logs quiet) but its owner has not reported after several minutes → wake-ups get lost; send the owner a status probe.
- A teammate stops replying or its replies degrade → probe it once. Teammates killed by transient API or session-limit errors resume from their transcript with full context once capacity returns — prefer that resume. Only when resume fails, spawn a fresh teammate on the same charter and point it at the artifacts (they carry the state); note the swap in the log.
- A teammate-initiated remote write — push, force-push, branch deletion — → refuse. Fetches are routine, and a user-ordered push is executed by the coordinator as a logged override.

## Resuming an Interrupted Run

Artifacts and the branch carry the state; teammate transcripts do not survive a session restart. When `/one-team <TICKET>` finds `.one-team/<TICKET>/team-log.md`: check out the existing branch (create it per Phase 0 when only the team directory survived), read the log, find the last recorded gate, and carry on from there — spawning fresh teammates just in time as the remaining phases need them, briefed from the artifacts. Do not redo passed gates; trust the log over memory. A branch with no team log is not a resume — handle it per Phase 0 step 2.

## Hard Rules

- The coordinator orchestrates; it never produces the work products itself.
- One writer: only the developer edits repository files, and only in Phases 3–6; every other role writes only inside `.one-team/<TICKET>/` — its own artifacts, plus relayed results the coordinator persists there. The coordinator's kickoff append to `.git/info/exclude` is the single exception outside both.
- Planner and reviewer run on `fable`, the developer on `opus`, the tester on `sonnet`, except for the two evidence-keyed downgrades the small-ticket lane allows; every teammate subagent runs on `haiku` or `sonnet`.
- No commits before Phase 6; no pushes except on an explicit user order, logged as an override; no Claude authorship ever.
- No phase advances without its gate logged in `team-log.md`.
- Jira is read-only.
- Never test against production systems or with production credentials — everything runs against the local environment with local/dev integration values.
- Raw agent IDs never appear in user-facing text.

## Quick Reference

| Phase | Owner | Exit gate | Artifact |
| --- | --- | --- | --- |
| 0 Kickoff | coordinator | branch, context, roster ready | `team-log.md` |
| 1 Plan | planner | plan written | `plan.md` |
| 2 Plan review | developer + user, concurrent | planner and developer agree, user approves | log entry |
| 3 Implement | developer | build green, staged, handoff written | staged diff + `dev-handoff.md` |
| 4 Deploy and test | tester | full matrix passes, developer and tester agree | `test-report.md` |
| 5 Final review | reviewer | `APPROVED`, all findings adjudicated | `review.md` |
| 6 Ship | developer | commits verified, tree clean, user briefed | commits on `<TICKET>` |