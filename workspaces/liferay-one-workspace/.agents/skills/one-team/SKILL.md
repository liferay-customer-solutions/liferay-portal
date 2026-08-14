---

allowed-tools: [Agent, AskUserQuestion, Bash, Edit, Glob, Grep, Read, SendMessage, Skill, TaskCreate, TaskGet, TaskList, TaskUpdate, Write]
description: Use when a Jira ticket should be taken from design through implementation, end-to-end testing, and final review by a coordinated agent team (planner, developer, tester, reviewer) in one session — either Liferay One workspace development or Liferay One migration script development in the scripts repo. Invoke as /one-team <TICKET> plus any extra context or constraints.
name: one-team

---

# One Team

Run a four-agent team — planner, developer, tester, reviewer — that takes a Jira ticket from context gathering to reviewed, committed code on a ticket-named branch. The session that invokes this skill is the **coordinator**: it spawns the teammates, relays every handoff, enforces the phase gates, arbitrates disagreements, and keeps the team log. The coordinator never plans, writes, tests, or reviews the work itself — it orchestrates the specialists who do.

The team delivers into **one target repo per run** — either this workspace or the Liferay One `scripts` repo — and reads every other checkout as context. The repo you invoke from is the repo the team writes in; which one that is decides a handful of commands and nothing else, since the phases, the roles, and the gates are identical.

Run from the target repo's root. A session rooted elsewhere may not expose `/one-team` by name — read this file directly and follow it; everything else works the same.

## Invocation

```
/one-team LPD-12345 [--lane workspace|scripts] [extra context, constraints, links]
```

A ticket ID is required; when missing, ask for one before doing anything else. `--lane` is rarely needed — the working directory already decides the lane — and exists only to override it. Everything else after the ticket ID is kickoff context — pass it verbatim into the planner's briefing, minus the consumed `--lane` flag.

## Lanes

Two lanes, one protocol. Everything lane-specific is in this table; the rest of this file and the charters are shared.

| | Workspace lane | Scripts lane |
| --- | --- | --- |
| Delivers | client extensions, objects, site content — the Liferay One product | `one/` ETL and migration scripts that load data into it |
| Not covered | anything outside `client-extensions/` | the repo's `partner/` and `customer/` Python areas — same phases, but the planner reads neighbouring scripts for convention and records the missing rule file as a risk |
| Target repo `<TARGET>` | `<WORKSPACE>` — the checkout itself | `<SCRIPTS>/.claude/worktrees/<TICKET>` — a per-ticket worktree |
| Isolation | none; the run works in the checkout, so **one workspace-lane run at a time** (see Concurrency) | a per-ticket worktree, so any number of runs proceed in parallel |
| Base ref `<BASE>` | `liferay-one/master-temp` | `liferay-one/main` |
| Rules the reviewer enforces | every file in `<TARGET>/.agents/rules/` | every file in `<TARGET>/.agents/rules/`, plus `<WORKSPACE>/.agents/rules/data-access.md` — the lane table in `one-review/criteria.md` is authoritative |
| Pattern sources | `<TARGET>/client-extensions/` | `<TARGET>/one/scripts/migration/`, `one/services/`, `one/core/`, `one/utils/` |
| Build gate | `./gradlew formatSource build` | `bunx prettier --write <touched paths>` then `bun run lint`, from `<TARGET>` — `bun run format` would reformat pre-existing drift across the whole repo and pull foreign files into the diff |
| Phase 4 proof | deploy client extensions, exercise the UI at `http://localhost:8080` | run the script against the local environment, verify the loaded data, re-run for idempotency |
| Scaffolding recipe | `<TARGET>/.agents/skills/` (`one-deploy`, `one-env-up`, `one-format`) | `<TARGET>/.agents/skills/one-new-script/SKILL.md` |
| PR recipe (Phase 6 tells the user, never runs it) | `<WORKSPACE>/.agents/skills/one-pr/SKILL.md` | `<SCRIPTS>/.agents/skills/one-pr/SKILL.md` |

**The invoking directory is the lane.** A session rooted anywhere inside the `scripts` checkout is the scripts lane; one rooted inside `liferay-one-workspace` is the workspace lane. That is the default and needs no confirmation — the session is already sitting in the repo the ticket is being developed in.

Two departures from it, and no others:

- An explicit `--lane` in the invocation wins, as a deliberate override.

- A working directory inside neither checkout answers nothing — ask with `AskUserQuestion` rather than picking.

The ticket never sets the lane; it only audits it. When the digest reads clearly like the other lane's work — a migration ticket in a workspace-lane run, an object-model ticket in a scripts-lane run — say so in the kickoff status and let the user redirect before Phase 1. Never switch lanes on the ticket's say-so: the directory is what the user chose, and silently retargeting the run sends the developer at the wrong repo.

A ticket that genuinely needs both repos is **two runs**, not one straddling run: deliver the target repo's half, record the other repo's half as owed work in `team-log.md`, and tell the user in the Phase 6 report so they can file or schedule the companion ticket. One repo per run is a hard rule.

## The Team

| Role | Model | Charter |
| --- | --- | --- |
| Planner | `fable` | `<WORKSPACE>/.agents/skills/one-team/roles/planner.md` |
| Developer | `opus` | `<WORKSPACE>/.agents/skills/one-team/roles/developer.md` |
| Tester | `sonnet` | `<WORKSPACE>/.agents/skills/one-team/roles/tester.md` |
| Reviewer | `fable` | `<WORKSPACE>/.agents/skills/one-team/roles/reviewer.md` |

The charters are lane-neutral and live only in the workspace; both lanes read the same four files. The reviewer's charter carries the role and the protocol but not the review substance — that lives in `<WORKSPACE>/.agents/skills/one-review/criteria.md`, shared with the interactive `/one-review` skill so the two can never drift. The reviewer reads it in place from `<WORKSPACE>` in both lanes, like the rule files. In both lanes the reviewer runs `/one-review --read-only`, whose flag keeps every check — formatting included, via the lane's check-only command — while writing nothing to the tree. That skill is lane-aware for the same reason the charters are, so the two lanes run the same review; where a scripts-lane session does not expose it by name, the reviewer reads `<WORKSPACE>/.agents/skills/one-review/SKILL.md` in place and follows it, exactly as it reads `criteria.md`.

Spawn each teammate with the `Agent` tool using `subagent_type: "claude"`, `run_in_background: true`, `name` set to the role, and the `model` from the table. Never let a teammate default to the session model — the split is deliberate: frontier reasoning where judgment concentrates (the plan, the final review), cheaper models where the work is more mechanical. When the harness does not offer `fable`, fall back to `opus` and say so in the kickoff status.

The tiering cascades: subagents spawned by any teammate always run on `haiku` or `sonnet` — research sweeps, file inventories, log scans, isolated mechanical edits. Frontier tokens are reserved for the teammates' own reasoning. The one exception is the reviewer's independent review passes under `--adversarial`, which are not research subagents but whole reviews standing in for the reviewer's own reading, and whose tier `one-review` sets; everything those passes spawn in turn obeys the cascade. A teammate whose runtime cannot spawn subagents does that work itself rather than blocking on the missing capability.

**Small-ticket downgrades.** Two downgrades are available when the work is genuinely small, each keyed to evidence that exists at the moment the role is spawned. When the user's kickoff context frames the ticket as small or trivial, spawn the planner on `opus`. When the drafted `plan.md` — which the coordinator reads anyway to summarize it for the user — clears the lane's small bar, spawn the developer on `sonnet`. Workspace lane: no data-model impact, no new objects or endpoints, and roughly fifty changed lines or fewer. Scripts lane: roughly fifty changed lines or fewer **and** no write path touched — a read-only export, a log or CSV change, a filter narrowed. Do not reuse the workspace bar there: "no data-model impact, no new objects or endpoints" is true of nearly every migration script, so it would downgrade the developer on work whose whole risk is writing the wrong data into the product. Should the review or the user's approval then enlarge the plan past those bounds, respawn the developer on `opus` before Phase 3 and log the swap. The reviewer stays on `fable` in every case — it is the last gate — and the tester's end-to-end pass is never trimmed. Log whichever downgrades apply.

**Tester upgrade.** Spawn the tester on `opus` when Phase 4 has to **construct** its fixtures rather than exercise data that already exists — a scripts-lane run needing seeded records, an acceptance criterion whose setup spans systems, anything where the test data must be made valid before the matrix can run. The `sonnet` default rests on Phase 4 being mechanical execution, and that assumption breaks exactly here: a measured run had the tester repeat one wrong fixture assumption *after* it had been corrected, which is the signature of a model at its limit rather than a knowledge gap. The plan's Test Plan says which case this is, so the call is available when the tester is spawned in Phase 3. When it only becomes clear mid-phase, respawn on `opus` against `test-report.md` and log the swap.

## Cost Discipline

A run's token cost is `turns × resident context`, not the size of what anyone reads. Measured across past runs: cache reads are 99.7% of input tokens, average context per turn is 411–442k with a peak near 996k, and the **coordinator alone** consumed 301–681M cache-read tokens over 732–1,540 turns — more than any teammate. Every rule below attacks one of those two multipliers.

None of them trades accuracy for cost. That line was drawn deliberately: research depth, the number of gates, what a reviewer reads, and what a planner verifies for itself are all untouched, because the cheapest defect in this workflow is still far more expensive than the tokens that would have caught it.

- **The coordinator orchestrates and nothing else.** Its writes are confined to `<TEAMDIR>`, its own control files (`run.lock` and its registry entry under `~/.claude/one-team/portal/`), and the Phase 0 worktree bootstrap. Its `Bash` use is confined to: the Phase 0 Jira fetches and path-resolution checks, `git fetch`, `git worktree add` and `git checkout -b`, the Phase 0 bootstrap commands (the `.env` copy, the store-DB clone, `bun install`), the branch-drift check, `git cherry` and `git reflog` during the resume check, the registry and generation reads at every gate, the Phase 6 `git log` and `git diff` verification, and read-only lookups it needs to arbitrate a dispute it has been asked to settle. The bootstrap is setup, not work product — it writes no file the ticket ships, which is why it does not breach the one-writer rule. It never builds, deploys, greps the codebase for its own answers, or edits a repository file. A measured run made 389 `Bash` and 66 `Edit` calls from this seat at ~440k context each — the most expensive habit in the workflow, and a violation of the coordinator's own mandate: work done here is work no charter governed and no role reviewed.

- **`PROGRESS` is log-only.** Append it to `team-log.md` and fold it into the next gate report. It never earns a relay turn or a user-facing line of its own.

- **Ten lines per message.** Dispatches and replies carry paths, verdicts, and decisions — never artifact content, never a restatement of `plan.md`. Measured average is ~770 tokens per message. Completeness always outranks the budget: a `FAIL`'s reproduction steps, a review objection's reasoning, and a dispatch's done-condition are written to be acted on without a follow-up question, however many lines that takes. One full message is cheaper than three partial ones.

- **Respawn beats resume for a later phase, once the transcript is large.** A teammate's transcript never shrinks, so resuming one late in the run re-reads its entire history every turn — and an agent operating near the top of its window follows its charter *worse*, which is the real reason for this rule. Artifacts carry the state, which is exactly what makes an interrupted run resumable. So a role re-entering a **later phase** with an accumulated transcript past roughly 60–100k is spawned fresh against the artifacts; a smaller transcript, or a role continuing work **inside its current phase**, is resumed. Log each respawn with the phase it re-entered.

- **Log turns per phase** in every `team-log.md` gate entry, so the next run is measurable without transcript archaeology.

## How Teammates Actually Work

These mechanics were verified live; the protocol depends on them:

- A background teammate is **turn-based**, not a live process. It handles the message it was given, acts, replies, and stops. `SendMessage` to it resumes it with its full context intact. Idling is free once spawned, but the spawn is not — a measured thirty to sixty thousand tokens of baseline context per agent, before any work. Spawn each role just in time, at its first real assignment.
- Because teammates are spawned with `name` set to the role, `SendMessage` addresses them by that name and the agents panel lists them readably. Still record the role → agent ID mapping from each spawn result in the team log: a reused name belongs to the newest agent, so IDs disambiguate respawns. Never show raw agent IDs to the user — say "the planner", "the developer".
- Teammates reply by calling `SendMessage` with `to: "main"`. Replies arrive at the coordinator automatically; there is no inbox to poll. A teammate's final text also arrives in its completion notification — the fallback when a teammate forgets to message.
- A teammate's **background subagent** reports its completion to the coordinator, not to the teammate that spawned it — a teammate that stops to wait for its own background child stalls until nudged. The charters therefore require synchronous subagents; when a grandchild result lands on the coordinator anyway, persist it to the team directory and resume the owning teammate with the path. Synchronous is not serial: independent subagents issued in a single message run concurrently.
- **A background command's wake-up is best-effort — never end a turn waiting for one.** This file used to call background commands safe because they re-invoke their owner. They do, usually; a measured run lost three wake-ups on plain `run_in_background` Bash and stranded the tester each time. A wake-up that never arrives is indistinguishable from a command still running, so waiting is the one thing that cannot detect it. Three shapes that do work, in order of preference: run it in the **foreground** when it fits the tool's timeout; or start it and, in the same turn, wait on a *separate* command that exits when the condition holds (`until <check>; do sleep 5; done`) — that exit is the reliable signal, not the original command's; or **verify the artifact rather than the notification** — the image exists, the log line landed, the process is gone. Silence is never success. When a wait genuinely must span turns, end the turn telling the coordinator you are waiting and what to check, so it watches you instead of you stranding.
- A turn whose only output is `SendMessage` calls looks empty to the harness, which re-prompts the agent — and can loop it. Teammates end every turn with a short line of plain final text after their messages.
- After dispatching work, end the turn with a one-line status for the user. The teammate's reply resumes the session.
- Teammates share the filesystem. Handoffs carry **paths, not contents** — point at `plan.md`, list the touched files, reference the diff. Pasting file bodies into messages pays for them twice.

Spawn prompts follow one shape: the role name; the ticket ID; the lane; the absolute team directory path; the instruction to read the role's charter copy in `<TEAMDIR>/roles/` and then `paths.md` before anything else (absolute paths — the teammate reads them itself, so the coordinator never loads charters into its own context); the kickoff context; and the first assignment. That first assignment is always real work — never an acknowledgment. A respawned role gets the same shape plus the artifacts covering the phases it missed.

## Team Directory

All run artifacts live in `<TEAMDIR>` — `~/.claude/one-team/<TICKET>/`, outside every checkout, deliberately:

The team directory is the run's only irreplaceable output. The code lives in git; the plan, the handoffs, the test report and the log do not. A team directory inside the target repo is exposed to everything else that touches that repo — another run's git operations, a worktree removal that takes it along, a `git add --all` that stages it — and one of those emptied a run's directory in practice, costing every written document it held. It also has to be reachable from both lanes, from any worktree, and by a resumed session whose worktree is gone, which nothing under a repo's `.git` can be. Keeping it out of the tree is also what makes the kickoff `.git/info/exclude` append unnecessary: an artifact that was never inside the repo cannot be staged by `git add --all`.

`paths.md` records the resolved absolute path. Every spawn prompt passes it, and no teammate derives it:

| File | Writer | Content |
| --- | --- | --- |
| `team-log.md` | coordinator | lane, roster, phase gate outcomes with turn counts, agreements, arbitrations, escalations, respawns |
| `paths.md` | coordinator | the lane, `<TARGET>`, `<BASE>`, and the resolved absolute path of every context repo — the first thing every teammate reads after its charter |
| `ticket-digest.md`, `initiative-digest.md` | coordinator | the Jira context teammates actually read — flattened ticket, one line per initiative issue |
| `ticket.json`, `initiative.json` | coordinator | raw Jira responses, kept only for targeted `jq` lookups — never read whole |
| `roles/` | coordinator | charter copies frozen at kickoff — what every spawn prompt points at (the ticket branch may predate the skill) |
| `plan.md` | planner | the implementation plan (template in the planner charter) |
| `dev-handoff.md` | developer | Phase 3 handoff: changes, per-AC verification hints for the tester, notes |
| `test-report.md` | tester | AC matrix, regression matrix, evidence, verdict per round |
| `review.md` | reviewer | findings with severity and verdict per round |

The coordinator creates the directory and `team-log.md` at kickoff and appends a log entry at every gate. Artifacts persist across sessions — they are how an interrupted run resumes, and what a respawned teammate is briefed from. Because they sit outside the checkouts they survive a removed worktree, a branch that never landed, and any cleanup of the repo, so a resume never depends on the tree still being there.

Two control files live beside the team directories rather than inside any one of them, because they coordinate runs against each other: `~/.claude/one-team/<TICKET>/run.lock` (this run's own claim on its ticket) and everything under `~/.claude/one-team/portal/` (the machine-wide portal registry). Both are specified in Concurrency.

## Jira Context

Read-only. Never transition tickets or post comments from this workflow.

The recipes live in `reference/jira.md` — ticket fetch and validation, the initiative pull with its pagination loop, the ticket's own issue graph, and the digest commands that make both readable. Read that file once in Phase 0 and work from the digests afterward.

## Resolving the Repos

The coordinator resolves every checkout once, at kickoff, and writes the results to `paths.md`. Teammates use those absolute paths and never re-derive them — relative hops break the moment a subagent runs from a different directory.

| Variable | How to resolve it |
| --- | --- |
| `<TEAMDIR>` | this run's artifact directory — `~/.claude/one-team/<TICKET>/`, expanded absolute (no `~` in `paths.md`; a teammate's shell may not expand it) |
| `<CHECKOUT>` | the target repo's **main** checkout — the directory the run was invoked from. In the workspace lane `<TARGET>` and `<CHECKOUT>` are the same path; in every other lane `<TARGET>` is a worktree of `<CHECKOUT>` and the two differ, which is why both are recorded |
| `<WORKSPACE>` | the Liferay One workspace — `<PORTAL>/workspaces/liferay-one-workspace` |
| `<PORTAL>` | the `liferay-portal` checkout — `<WORKSPACE>/../..`, or from the scripts lane the sibling checkout that contains `workspaces/liferay-one-workspace` (conventionally `../liferay-portal`) |
| `<SCRIPTS>` | the `liferay-one/scripts` checkout — a sibling of `<PORTAL>`, conventionally `<PORTAL>/../scripts`; confirm with `git remote -v` naming `liferay-one/scripts` |
| `<LEGACY_OSB>` | `<PORTAL>/../liferay-portal-7.2.x/modules/dxp/apps/osb/` (the checkout sits on branch `7.2.x-temp`) |
| `<LEGACY_CUSTOMER>` | `<PORTAL>/../liferay-portal-7.0.x/modules/dxp/apps/osb/osb-customer/` |
| `<LEGACY_KORONEIKI>`, `<LEGACY_PROVISIONING>` | `<PORTAL>/../lfris-koroneiki`, `<PORTAL>/../lfris-provisioning` |
| `<LEGACY_SUPPORT>`, `<LEGACY_MARKETPLACE>` | `<PORTAL>/workspaces/liferay-customer-workspace`, `<PORTAL>/workspaces/liferay-marketplace-workspace` |

Test each path before recording it and mark the absent ones absent — a teammate that reads "absent" records the gap in its plan or report, where a teammate that reads a broken path invents history instead.

Which source answers which question, and the cross-repo contract each lane owes the other, are the planner's and reviewer's substance: the planner charter's research order and `one-review/criteria.md` are authoritative. The coordinator only resolves the paths.

## Concurrency

Several runs may be in flight on this machine at once — the common shape is one workspace-lane run plus one or more runs in other repos. Files stop being the problem once each non-workspace run has its own worktree and every run's artifacts live outside the checkouts. **One local Liferay instance is what remains shared, and it cannot be duplicated per run.** So runs coordinate on the portal instead of taking turns at it: almost everything proceeds in parallel, and only the operations that genuinely disrupt other runs are serialized.

Most of a run never touches the portal and never coordinates: Jira and kickoff, planning, plan review, the developer's implementation, the final review, and the commit. This section binds two stretches — **Phase 4**, and the **tester's prep** that overlaps Phase 3, since prep brings the environment up and may hit the bootstrap branch of env-up. The developer's `buildDockerImage` warm-up in Phase 3 builds an image and restarts nothing, which is why it stays uncoordinated.

**Coordination state lives at `~/.claude/one-team/portal/`.** Machine-global on purpose: the portal is `localhost`, shared by every lane, so its coordination state cannot live under any one repo's `.git` — a repo-local lock would let a workspace-lane run and a scripts-lane run each pass their own check and still collide.

| Path | Owner | Contents |
| --- | --- | --- |
| `runs/<TICKET>.json` | coordinator | ticket, lane, session, PID, current phase, `activity` (`idle`/`active`) with a timestamp, and the run's data-scope claim. Written at kickoff, refreshed at each gate, deleted at Phase 6 or abandonment |
| `ops.lock` | whoever holds it | a directory, created with `mkdir` so creation is atomic. Holds `ticket`, `operation`, `PID`, `acquired-at`. Held for one operation, never for a phase or a run |
| `generation` | last disruptor | a monotonic counter plus an append-only log line per disruptive operation: `<n> <ticket> <operation> <timestamp>` |

### Three Tiers of Portal Work

| Tier | What it is | Coordination |
| --- | --- | --- |
| **A — free** | authenticated reads and UI reads at `8080`; reading object definitions (files, not the portal); `docker compose logs` reads; migration and fixture writes scoped to the run's own records; the developer's `buildDockerImage` warm-up (it builds an image and restarts nothing); `docker compose up --detach` when containers already run; hot-deploy of `liferay-one-custom-element` or `liferay-one-global-css` | none |
| **B — quiet window** | the idempotency row, the data-integrity row, and any first-run measurement or global count. The portal is fine with concurrent writes; the *verdict* is not, because these compare a before and an after | `ops.lock` for the row group, intent `quiet-verification`. No generation bump — nothing persistent changed |
| **C — disruptive** | `docker compose up --detach --force-recreate liferay-one-etc-spring-boot` (mandatory after any Spring Boot change: restarts the container, fails every in-flight `58081` call, and serves new code afterwards); hot-deploy of `liferay-one-batch`, `liferay-one-site-initializer` or `liferay-one-instance-settings` (no restart, but changes the schema, objects or site every other run is verifying against); `/one-site-reset`; `/one-instance-reset` (tears down all records and structure with no restart at all — it looks gentle and is the second most destructive thing available); `/one-env-reset` (volume wipe, taking the `local-dev` OAuth2 app with it); `one-env-up`'s first-run bootstrap branch; re-creating the OAuth app | `ops.lock` + drain, then bump `generation` |

The rule behind the table, for anything it does not name: **restarts a container, redeploys an extension, or deletes-and-reseeds → Tier C. Depends on nobody else writing → Tier B. Otherwise A.**

### Acquiring, Draining, Releasing

Take the lock by creating the directory — `mkdir` fails when it already exists, which is the whole mechanism:

```
until mkdir ~/.claude/one-team/portal/ops.lock 2>/dev/null; do sleep 5; done
```

For Tier C, then **drain**: wait until every other registered run reads `activity: idle`, polling the registry the same way. Cap the wait around ten minutes and escalate to the user rather than forcing it — a run that will not go idle is a run that needs a human, not a deadline. Run the operation, verify health by its own recipe's checks, append the generation line, then release by removing the lock directory.

On the other side of it, every tester checks for a held `ops.lock` **before each unit of portal work** — a script run, a matrix row group — and waits rather than starting; between units it flips its `activity` flag to `idle`. Unit granularity is what keeps a pending Tier C operation draining in minutes instead of waiting out a whole matrix. Set the flag to `idle` *before* beginning to wait, never after: a run that waits on the lock while still advertising itself as `active` is waiting for a drain that its own flag is blocking, which is the one way these two rules could deadlock each other.

A lock whose PID is dead is reclaimed by the next waiter — but the operation may have half-completed, so reclaiming means: log it, run the recipe's health checks (`http://localhost:8080/c/portal/status`, and `58081/ready` where relevant), run day-to-day env-up if unhealthy (idempotent and safe), append a `reclaimed` generation line so every run knows an operation may have partly applied, then proceed or escalate. Stale registry entries with dead PIDs are swept by any coordinator at kickoff, logged.

**The three resets additionally need the user's approval whenever another run is registered.** Exclusivity is not enough for them: their destruction outlives the lock, so draining around them protects nobody. A tester that believes a reset is the only way forward reports `BLOCKED` with its reasoning and lets the coordinator ask.

### What Sharing One Portal Costs, Stated Plainly

Two things one instance can never give concurrent runs, and the protocol says so rather than pretending otherwise:

- **Overlapping write scopes.** Object ERCs are unique instance-wide, so one run's upsert can repoint the very record another run is asserting against, with neither touching a shared file. Runs therefore declare a data scope at the Phase 1 gate (the planner's Data Scope line) — what the run writes, and what its assertions read. The coordinator copies it into the registry and compares it against every other live run's claim, in both directions, counting deployed extensions and called endpoints as writes. Disjoint → both Phase 4s run concurrently. Overlapping → the coordinator asks the user to sequence the two Phase 4s, or logs the overlap as an accepted risk that the Phase 6 report names. Pairs that genuinely cannot overlap: two runs writing the same object, anything asserting global counts or first-run measurements, and two workspace-lane runs.
- **A private log stream.** `docker compose logs liferay` is instance-wide, so with another run registered a new `ERROR` may not be yours. The tester charter's log rule degrades honestly rather than silently: errors attributable to the row's own operation still fail it; unattributable ones are recorded, with the other run named, and cross-checked instead of auto-failed. A row that needs the old strictness takes a Tier B quiet window, which buys a private log window too.

Fixture ERCs carry the ticket ID (`<TICKET>-FIXTURE-*`) so fixtures can never collide and cleanup stays targetable.

**Capacity, not correctness:** every run costs a coordinator plus four teammates plus their subagents, so several at once multiply exposure to the session-limit kills the Circuit Breakers already handle. Nothing structural — just expect them sooner with three runs than with one.

## Phase Protocol

Seven phases. Each has an owner, an exit gate, and a `team-log.md` entry; no phase's gate can pass before the previous gate is logged — verdicts and confirmations keep their order even where work overlaps (tester prep during Phase 3 and the reviewer's early pass during Phase 4 are the two sanctioned overlaps). Gates are evidence-based, not immutable: when later evidence invalidates a logged gate — a regression surfacing after `APPROVED`, a failed retest, **another run's Tier C operation landing after this run measured its result** — the coordinator reopens the run at the earliest affected phase, logs why, and the standard loops rerun. Ship never proceeds over a known-stale gate. Concurrency makes that last case the one a run cannot feel, so it is detected rather than noticed: every Phase 4 verdict records the `generation` it was measured at, and the coordinator re-reads the counter before accepting the Phase 5 verdict and again at Phase 6. Mirror the phases on the shared task board at kickoff — one task per phase, chained with `addBlockedBy` — and advance statuses as gates pass. The coordinator owns the board; teammates report through messages.

Every `git` command in every phase runs in `<TARGET>` — a teammate that shells out from a subagent's default directory can land in the wrong repo, and the sibling checkouts are all git repos too. Before logging any gate and before dispatching any phase assignment, verify `git -C <TARGET> branch --show-current` prints `<TICKET>`; on a mismatch, freeze the team with HOLD messages, read the reflog to see what happened, and escalate to the user before anything else runs. A worktree lane is largely immune to this — the worktree exists for this ticket alone and git will not check its branch out elsewhere — but the check still runs, because it costs nothing and the workspace lane, which works directly in a checkout the user and other sessions can move mid-run, needs it exactly as much as before.

### Phase 0 — Kickoff (Coordinator)

1. Read the lane off the working directory (see Lanes) and verify that directory is `<CHECKOUT>`'s root, then resolve every path and write `paths.md` — the lane, `<CHECKOUT>`, `<TEAMDIR>`, `<BASE>`, and each variable from Resolving the Repos marked present or absent. `<TARGET>` is recorded once it exists: the same path as `<CHECKOUT>` in the workspace lane, the worktree path below in every other lane.

1. Claim the ticket before changing anything: create `<TEAMDIR>` and take `~/.claude/one-team/<TICKET>/run.lock` per Concurrency. A live holder means another session is already running this ticket — stop and tell the user which, rather than running a second coordinator over one team directory.

1. Resume check — before any tree-state judgment: when `<TEAMDIR>/team-log.md` exists, follow Resuming an Interrupted Run instead of continuing here; a resumed run's staged, uncommitted work is its persisted state, not a dirty tree. A branch named `<TICKET>` with no team log is a leftover, not a resume: when `git cherry <BASE> <TICKET>` shows its work already upstream (the usual case for follow-ups on completed tickets), rename it aside — `git branch -m <TICKET> <TICKET>-pre-one-team` — and continue; when it carries unique unmerged commits, stop and ask the user which base to build on.

1. Register the run in the portal registry and check what else is live, per Concurrency. In the workspace lane, another registered workspace-lane run is a stop: that lane has no worktree, so two runs would share one working tree. Registered runs in other lanes are expected and fine — note them in the kickoff status so the user knows what this run is sharing the portal with.

1. Fresh runs only: `git -C <CHECKOUT> status --porcelain` must be clean. Dirty tree → stop and ask the user. A worktree lane checks `<CHECKOUT>` here because the worktree does not exist yet; from Phase 3 on, the tree that matters is `<TARGET>`.

1. Write `team-log.md` (ticket, lane, date, phase checklist, roster placeholder) into `<TEAMDIR>`, and copy the four charter files from `<WORKSPACE>/.agents/skills/one-team/roles/` into `<TEAMDIR>/roles/` — every spawn prompt points at these copies, in every lane.

1. Fetch and digest the Jira context per `reference/jira.md`, and validate it.

1. Fetch and branch, in `<CHECKOUT>`: `git fetch liferay-one master-temp` in the workspace lane or `git fetch liferay-one main` in the scripts lane. Then create the tree the run works in:

    - **Workspace lane:** `git checkout -b <TICKET> <BASE>`. `<TARGET>` is `<CHECKOUT>`.
    - **Every other lane:** `git worktree add .claude/worktrees/<TICKET> -b <TICKET> <BASE>`, and record that path as `<TARGET>` in `paths.md`. Nothing downstream changes — every teammate already runs `git -C <TARGET>` against absolute paths. Reuse the worktree when it already exists on the right branch; git refuses to check one branch out twice, which backstops a same-ticket collision the `run.lock` should already have caught.

1. Bootstrap a fresh worktree so the tester can actually run in it — a new worktree contains none of the gitignored working state. In the scripts lane: copy `<CHECKOUT>/one/.env` to `<TARGET>/one/.env`; `mkdir -p <TARGET>/one/db <TARGET>/one/output` (both are gitignored, so a fresh worktree has neither, and `cp` into a missing directory fails); clone the local stores with `cp -c <CHECKOUT>/one/db/*.db <TARGET>/one/db/` (APFS clone-on-write, so a populated store costs no space and no wait, and the run gets a private copy that no other run can contend on); then `bun install` in `<TARGET>/one`. Skip whatever the source lacks and say so in the kickoff status — a missing `.env` is the tester's `BLOCKED`, not a value to invent. The workspace lane skips this step: it works in the checkout, which is already provisioned.

1. Create the phase tasks on the task board.

1. Spawn the planner, whose first assignment is Phase 1; log the roster and extend it as the other roles spawn.

### Phase 1 — Plan (Planner)

Brief the planner with the digest paths, the user's kickoff context, and the assignment: produce `plan.md` per its charter.

The planner researches as deeply as the ticket needs — fanning its mechanical sweeps out to cheap subagents in a single message rather than taking a turn per sweep — and **asks instead of guessing**: `QUESTION` messages come to the coordinator, which answers from established run context or puts the question to the user via `AskUserQuestion`, then relays the answer verbatim. Batched questions cost one round-trip; a trickle costs one each.

Exit gate: `plan.md` written; planner reports `DONE`. At this gate the coordinator also copies the plan's **Data Scope** line into the run's registry entry and compares it against every other live run's claim per Concurrency — this is the earliest point the claim exists, and the last point before Phase 4 where sequencing two runs is still cheap. A conflict is raised with the user now, not discovered mid-matrix.

### Phase 2 — Plan Review (Developer) and the Human Gate, Concurrently

Dispatch both reads at once: spawn the developer with this review as its first assignment — read `plan.md` critically before any code exists — feasibility, missing steps, pattern conformance, testability, scope — and in the same breath post the compact plan summary to the user in chat (goal, approach, files, test plan, open risks), point at `plan.md`, and ask them to approve or request changes. **Phase 3 starts only when both the developer–planner agreement and the user's approval are in.**

Developer objections are relayed to the planner for revision; loop until **both explicitly agree**. When they still disagree after one rebuttal round each, take both positions to the user instead of forcing agreement. When a revision lands while the user is still reading, tell them what changed — an approval given on stale text is re-confirmed against a one-line delta. Log the outcome and any accepted risks.

### Phase 3 — Implement (Developer)

Dispatch two assignments the moment the plan gate closes: the developer implements `plan.md` under its charter's rules, and the tester — spawned now, prep as its first assignment — runs that prep in parallel (its charter's Prep section; nothing in it needs the diff). While this phase runs:

- The developer is the **only writer** of repository files, and only inside `<TARGET>`. Nobody else — planner, tester, reviewer, coordinator — edits them, ever, and no role writes anything in any other checkout. The `<TEAMDIR>` artifacts are the one exception: each role maintains its own, per the artifact table.
- Deviations from the plan are flagged to the coordinator; material design changes go back to the planner for agreement before proceeding, which respawns it against the artifacts when Phase 2 is long behind. The planner stays the only writer of `plan.md`: it is the design of record, and a developer editing it is a developer grading its own deviation.
- Done means the lane's build gate passes, unit tests exist where the target repo already has patterns for them, and everything is staged with `git add --all` — no commits.

Exit gate: developer writes `dev-handoff.md` (touched files, change summary, per-AC verification hints mapped to the plan's test scenarios, notes) and reports `DONE` with the path.

### Phase 4 — Deploy and Test (Tester)

The tester — already prepped, briefed with `dev-handoff.md` and the plan's Test Plan — proves the staged work through the running system per its charter, then sweeps for **regressions**: every flow or script that consumes code the developer touched gets exercised too, with logs watched for new errors throughout. The lane's proof is the Lanes table's Phase 4 row; the tester charter carries the procedure.

`FAIL` goes back to the developer with reproduction steps, the developer fixes under Phase 3 rules, the tester redeploys and retests the failed cases plus the fix's blast radius. Loop until the full matrix passes on the currently deployed build.

Exit gate: `test-report.md` complete; developer and tester both explicitly confirm the acceptance criteria are met with no regressions. Log the joint agreement, and log the `generation` the matrix was measured at — the report carries it too, per the tester charter. A verdict with no generation recorded cannot be checked for staleness later, which makes it unshippable under Phase 6 step 4.

### Phase 5 — Final Review (Reviewer)

Spawn the reviewer — unless the early pass below already did — and brief it with the plan, test report, and diff scope (`git diff <BASE>` — the work is staged, so this shows everything, new files included).

**Decide here whether this ticket is `--adversarial`-eligible**, and tell the reviewer which it is. Two triggers earn it and nothing else does:

- the diff touches a **write path in the scripts lane** — a migration that loads or mutates data, where `criteria.md` already makes an idempotency defect a blocker and a bad run is expensive to undo;
- the diff changes a **contract another repo consumes** — an ERC, a field name, an endpoint path, a payload shape.

Neither present, every round runs standard. Either present, the reviewer still runs standard rounds and escalates only on the round it would otherwise approve — see its charter. Running the flag on every round multiplies its cost by the number of rounds and spends the most where it buys the least: a round ending in `CHANGES_REQUESTED` is catching things the next round would catch anyway, while the round that produces `APPROVED` is the one where a miss actually ships.

Log the call in `team-log.md`. On an escalated round, brief with `git diff <BASE> --name-only` scope rather than content: under the flag the reviewer reads no diff itself, and a coordinator that hands over the content has spent its fresh eyes before a pass exists. Those artifacts and nothing more: no account of how the developer arrived at the change, no summary of what has already been checked, no reassurance that a shape was deliberate. The coordinator watched this get built, and every sentence of that history it forwards is a judgment the reviewer no longer derives for itself. The reviewer works read-only per its charter.

For small diffs (roughly under two hundred changed lines), the coordinator may start the reviewer's passes during Phase 4, with the verdict held until the tester's `PASS` lands — a `FAIL` that changes the diff voids them. Findings are only ever issued against the tested, final diff.

Where `--adversarial` was asked for and the reviewer reports it could not spawn subagents — so worked the lenses inline rather than through fresh readers — that is a degraded review, not a failed one. `APPROVED` still passes the gate; log the degradation in `team-log.md` as an accepted risk and name it in the Phase 6 report, so the user decides whether a separate fresh review runs before `/one-pr`. An independence shortfall that nobody surfaces is the one this protocol would ship silently.

`CHANGES_REQUESTED` → developer fixes (Phase 3 rules) → **tester retests the fixes and their blast radius** (Phase 4 rules — a `PASS` on those rows suffices in these rounds; the full joint confirmation is not re-taken) → reviewer re-reviews the delta only. Every finding ends adjudicated: fixed, or explicitly rejected with a reason the reviewer accepts. Loop until `APPROVED`.

Exit gate: reviewer's `APPROVED` logged, and the generation check passes. Before accepting the verdict the coordinator re-reads `~/.claude/one-team/portal/generation` and diffs it against the one Phase 4 recorded. Unchanged, or changed only by events irrelevant to this run, passes with a log line. **Relevant** means: a Spring Boot recreate when this run's work reaches that extension (the tester charter's `SPRING_BOOT_URL` test draws the same line), a batch or site-initializer deploy touching an object inside this run's data scope, or any reset — resets are relevant to everyone. A relevant event after a logged `PASS` reopens Phase 4 for the affected rows.

### Phase 6 — Ship (Developer Commits, Everyone Signs)

1. The developer runs the lane's build gate one final time. The pass must produce no diff — when it changes anything, re-stage and return to Phase 5 for a delta re-review before continuing.

1. The developer composes the commits: minimal and organized — one commit is the default; split only when the history is genuinely clearer for the human reviewer (for example, regenerated output apart from hand-written code). Every message reads `<TICKET> <concise summary>` — sentence case, no trailing period, under 72 characters. Plain `git commit` under the user's git identity: **never** add Claude as author or co-author, no `Co-Authored-By` trailer, no tool attribution anywhere.

1. The coordinator verifies the mechanical properties: `git log <BASE>..HEAD --format='%an %s'` shows the user as author and the ticket prefix on every commit; `git diff <BASE> --name-only` shows only files in this ticket's scope, all of them inside `<TARGET>`; the working tree is clean.

1. The coordinator runs the generation check one last time, exactly as in Phase 5. **Ship never proceeds over a generation the gate has not seen** — a relevant event that landed since the review reopens Phase 4 for the affected rows, however late it arrives. This is the last point where a concurrent run's redeploy or reset can be caught before the work is presented as verified.

1. The reviewer takes one last look at the commit structure — message quality, sensible organization, nothing stray. Author and prefix are mechanical and already checked; whether a message describes the outcome rather than the code is a judgment, which is why it stays with the reviewer. A problem named here follows the usual adjudication loop: the developer amends the commits (soft-reset and recommit when structure or messages are wrong), the coordinator re-runs its step 3 checks, the reviewer looks again.

1. Release what this run was holding: delete `runs/<TICKET>.json` from the registry and remove `run.lock`, so a later session on this ticket is a clean resume rather than a blocked one. `<TEAMDIR>` stays — it is the record. **The worktree stays too.** A worktree is only ever removed once its ticket has shipped or been abandoned: the run's staged, uncommitted work is its resume state, and removing the worktree destroys that while leaving the branch ref behind to look intact. Tell the user the worktree path in the report and let them decide when it goes.

1. **Do not push. Do not open a PR** (unless the user has explicitly ordered it — log the override). Report to the user: the lane and target repo, what was built, where the plan/test/review artifacts live, the commit list, the branch name, the worktree path when the lane used one, any cross-repo work the run recorded as owed, and that the lane's `/one-pr` is the next step once they are satisfied. In the workspace lane, warn them that `liferay-one/master-temp` force-rewrites frequently — a branch that sits unmerged shows PR "conflicts" even when its files are untouched; re-rebasing onto the current tip and force-pushing with lease fixes it in seconds, and prompt merging avoids it entirely.

## Communication Rules

- Hub and spoke for everything that matters: handoffs, verdicts, gates, escalations, and disagreements go through the coordinator and into `team-log.md` as they happen, not retroactively. Pure clarification questions between teammates go directly — address the role name (a verified mechanic) — with the exchange reflected in the asker's next report to main; anything touching scope, design, verdicts, or gates returns to the spoke.
- Every teammate reply starts with a status word — `DONE`, `PASS`, `FAIL`, `BLOCKED`, `PROGRESS`, `QUESTION`, `APPROVED`, `CHANGES_REQUESTED` — followed by the payload. Each charter names the subset its role uses. `PROGRESS` is non-terminal and log-only: a milestone heartbeat the coordinator records without a relay turn or a reply.
- The user outranks the protocol: a mid-run user instruction that overrides a rule — ship before review, push, skip a phase — is followed and logged as a user override, never resisted and never silently absorbed. Work it displaces (a deferred final review, for example) is recorded in the log as still owed.
- Every dispatch names the phase, the assignment, the artifact paths, and what done looks like — complete, in one message. Each extra round-trip re-processes that teammate's whole transcript, so one full dispatch costs less than three partial ones. Dispatches routinely run past the ten-line budget for exactly this reason and should: the budget binds hardest on replies and relays, where length is usually restatement.
- **A silent teammate is watched, not waited on.** Probe frugality applies to a teammate that is working, never to one that might be stuck. Any teammate known to be waiting on a long command, or silent past the point its work should have finished, gets checked — and checked by looking (is the process alive, did the artifact appear, what do the logs say) rather than by waiting for a notification that may never arrive. A probe costs one round-trip; a stranded phase costs the run.
- Disagreements get one rebuttal round per side. Execution-level disputes (fix approach, finding severity, retest scope) are then decided by the coordinator, reasoning logged. Disputes over the plan's content, the ticket's scope or meaning, or anything that would overturn a user-approved plan go to the user with both positions summarized.
- Nothing ships unexamined: the plan is reviewed by the developer, the code by the tester and the reviewer, the test report by the reviewer, every review finding by the developer (adjudication), and the commits by the coordinator and the reviewer. At least one other teammate has analyzed every artifact — that invariant is not negotiable.

## Circuit Breakers

- Three dev–test rounds without the failure count shrinking, or six rounds total regardless of progress → stop, summarize both positions with evidence, escalate to the user.
- Three review rounds without `APPROVED` → same.
- Any `BLOCKED` reply the coordinator cannot clear itself, or three failed attempts at a single gate (a build that will not go green, an environment that will not start, missing credentials) → same.
- Round tallies and per-phase turn counts go into the `team-log.md` gate entries as they happen, so a resumed run inherits its breaker counts instead of resetting them.
- A teammate reported it is waiting on a long command → **watch it from the start**, do not wait for its next message. Check the thing itself on a cadence matched to how long the command should take — the process, the artifact, the log — because wake-ups get lost and the loss is silent. The moment the command has plainly finished and the owner has not reported, probe it. Do not wait "several minutes" past that point: the earlier version of this rule required observing the finish before probing, which is precisely what a coordinator that is waiting rather than looking never does.
- A teammate stops replying or its replies degrade → probe it once. Teammates killed by transient API or session-limit errors resume from their transcript with full context once capacity returns — prefer that resume when the teammate is mid-phase. Otherwise spawn a fresh teammate on the same charter and point it at the artifacts; note the swap in the log.
- A teammate-initiated remote write — push, force-push, branch deletion — → refuse. Fetches are routine, and a user-ordered push is executed by the coordinator as a logged override.

## Resuming an Interrupted Run

Artifacts and the branch carry the state; teammate transcripts do not survive a session restart. When `/one-team <TICKET>` finds `<TEAMDIR>/team-log.md`: take `run.lock` first, exactly as a fresh run does, and re-register the run in the portal registry — a resumed run shares the portal like any other, and its old registry entry may have been swept as stale. Then take the lane and the resolved paths from `paths.md` rather than re-deriving them — re-verify each path still exists, and rewrite the file when a checkout moved. Restore the tree: check out the existing branch, or in a worktree lane re-add the worktree from the surviving branch (`git worktree add .claude/worktrees/<TICKET> <TICKET>`) and re-run the Phase 0 bootstrap, or create the branch per Phase 0 when only the team directory survived. Then read the log, find the last recorded gate, and carry on from there — spawning fresh teammates just in time as the remaining phases need them, briefed from the artifacts. Do not redo passed gates; trust the log over memory. A branch with no team log is not a resume — handle it per Phase 0 step 3.

One caveat the log cannot record for you: a worktree that was removed took the staged, uncommitted diff with it, even though the branch ref survived and looks complete. When the log's last gate implies staged work and the restored tree has none, the run resumes at the start of the phase that produced it rather than after it — say so in the log and in the next status, because silently continuing would present unwritten work as done. Any Phase 4 verdict in the log is also re-checked against the current `generation` before it is trusted, per Concurrency: an interrupted run is exactly the case where other runs kept working.

## Hard Rules

- The coordinator orchestrates; it never produces the work products itself, and its tool use stays inside the Cost Discipline bounds.
- One repo per run: the whole team writes only inside `<TARGET>`. Every other checkout — the workspace from the scripts lane, the scripts repo from the workspace lane, all legacy sources — is read-only context. Work the other repo needs is recorded as owed, never done here.
- One writer: only the developer edits repository files, and only in Phases 3–6; every other role writes only inside `<TEAMDIR>` — its own artifacts, plus relayed results the coordinator persists there. The coordinator's Phase 0 worktree bootstrap (gitignored working state only, never a file the ticket ships), and the tester's repointing of the gitignored `<TARGET>/one/.env` at the local environment, are the only exceptions.
- A run touches nothing belonging to another run: not its team directory, not its worktree, not its registry entry, and not a lease it does not hold. The one shared resource is the portal, and the only sanctioned way to affect it while others are live is the Concurrency procedure.
- Planner and reviewer run on `fable`, the developer on `opus`, the tester on `sonnet`, except for the two evidence-keyed downgrades the small-ticket lane allows; every teammate subagent runs on `haiku` or `sonnet`, apart from the reviewer's `--adversarial` review passes, whose tier `one-review` sets.
- No commits before Phase 6; no pushes except on an explicit user order, logged as an override; no Claude authorship ever.
- No phase advances without its gate logged in `team-log.md`.
- Jira is read-only.
- Never write to a production system and never test with production credentials — every write lands in the local environment, with local or dev integration values. A migration's extraction sources are the one read-only exception, and even a read against a production source needs the user's explicit approval, logged as an override and bounded.
- Never commit `.env`, credentials, or exported data — including the extract files and local stores a scripts-lane run produces (`<SCRIPTS>/.agents/rules/sensitive-data.md`).
- Raw agent IDs never appear in user-facing text.

## Quick Reference

| Phase | Owner | Exit gate | Artifact |
| --- | --- | --- | --- |
| 0 Kickoff | coordinator | `run.lock` held, run registered, lane, paths, tree (worktree bootstrapped where the lane uses one), context, roster ready | `team-log.md`, `paths.md` |
| 1 Plan | planner | plan written; data scope claimed and compared against live runs | `plan.md` |
| 2 Plan review | developer + user, concurrent | planner and developer agree, user approves | log entry |
| 3 Implement | developer | build green, staged, handoff written | staged diff + `dev-handoff.md` |
| 4 Deploy and test | tester | full matrix passes at a recorded `generation`, developer and tester agree | `test-report.md` |
| 5 Final review | reviewer | `APPROVED`, all findings adjudicated | `review.md` |
| 6 Ship | developer | commits verified, tree clean, user briefed | commits on `<TICKET>` |