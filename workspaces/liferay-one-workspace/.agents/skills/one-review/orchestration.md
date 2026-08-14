# One Review — Orchestration

How a review is *run* when it is run adversarially: who reads the diff, how many independent passes there are, and how their findings combine. What a review *covers* is [`criteria.md`](./criteria.md); the steps themselves are [`SKILL.md`](./SKILL.md).

**This file applies under `--adversarial` and nowhere else.** A plain `/one-review` never reads it and works `SKILL.md` directly — which is the default, because everything here buys accuracy with several times the tokens of a plain run. `SKILL.md`'s flag list says when that trade is worth making.

**A pass never reads this file.** A session whose prompt names it a pass works `SKILL.md` Steps 1 through 5 and nothing here — that is what keeps a pass from spawning passes of its own, and it is why this content lives in a separate file rather than a section a pass is asked to skip.

That cuts both ways, and it is the rule to hold onto when editing either file: **anything a pass must obey belongs in `SKILL.md`, never here.** A pass-binding rule written into this file is invisible to the only reader it governs, and invisible by design — nothing at runtime catches it, because a pass not reading this file is the mechanism rather than a fault. What belongs here is only what the session combining the passes acts on.

## Independence From the Change

A session that wrote the diff — or was briefed by something that watched it get built — reviews it worse than one that did not, and the gap is invisible from inside: every step below is satisfiable from what is already in context, so the procedure runs to completion and finds less.

Two facts go in the report, and in the receipt under Record the Verdict; under `--read-only` there is no receipt, so the caller's own artifact carries them — `review.md` for the `one-team` reviewer. Read them as a pair, never either half alone.

**State**, settled before Step 1. Where it is uncertain — a compacted session, inherited context, any prior contact with this ticket — take the more contaminated one.

- **SELF** — wrote part of the diff.
- **BRIEFED** — did not write it, but carries prior conclusions: a briefing from something that watched it get built, an earlier review of the same diff, a debugging pass over it. The `one-team` reviewer is always BRIEFED — a coordinator that watched the build writes its briefing, `dev-handoff.md` sits in the team directory, and re-review rounds carry the developer's account of each rejected finding.
- **FRESH** — neither: the branch and nothing else.

**Reading**, where the findings actually came from. It does not restate the state — a `FRESH` session in a subagent-poor harness records `contaminated` and means only that it read the diff itself.

- `fresh` — two or more independent passes that could not see each other. The normal path in every state.
- `orchestrated` — one pass run here with its lenses delegated. The fallback where the harness offers no second reader.
- `contaminated` — worked here inline, no subagent of any tier available.

**The bar is parity, not disclosure.** A SELF or BRIEFED review is expected to find what a FRESH one would; the label is worth nothing on its own.

### No session reviews its own target

**Unless its own prompt names it a pass**, a session running this skill does not work the lenses, read the call sites, or judge the diff: it spawns the passes under Passes, verifies what they return, and combines them. A subagent's context is clean by construction, so a pass is not an approximation of a fresh review — it is one, and for a contaminated session the only kind available.

That exemption is structural and overrides everything else in this file. **A session whose prompt names it a pass works Steps 1 through 5 itself, spawns no passes, and skips three sections: this one, Passes, and Combining the Passes.** It records no state of its own and writes no Independence, Passes, or Dropped candidates section — those belong to the session combining it, the only reader that can know them. Say "you are a pass" in the prompt, in those words: without it a pass reads this paragraph and spawns two of its own, and so does each of those.

Delegation collapses most of the difference between the states. What is left of it: nothing from a briefing reaches a pass; a SELF session appears nowhere in its own finding list; and only a FRESH session may settle a severity disagreement by reading, per Combining the Passes.

**Pass prompts carry pointers only:** that it is a pass, `<TARGET>`, `<BASE>`, the lane, the path to this skill, the path to the acceptance criteria, the ticket they came from, and on a re-review round the two object names bounding the delta. No summary of the change, no rationale, no account of what was already checked, no list of what to look at — a steer about where to look is how a contaminated session narrows a review while formally complying. The ticket travels because the criteria file may have been transcribed by the session that wrote the code, and only its source shows a requirement narrowed on the way in.

**The run's flags do not travel.** A pass always runs Step 1 check-only, writes no receipt, and never runs Step 6, `--fix`, or `--comment` — those are the session's, after combining. Passes share one checkout: a mutating formatter in two at once is a write race that corrupts the tree they are reading, feeds each pass the other's edits through Step 2's uncommitted-work rule, and invalidates the `file:line` anchors the merge depends on; under `--comment` each would post its own unmerged findings. **Run Step 1 once, here, before spawning.**

**Passes read code, not the run** — the boundary under What the readers are given, stated in every pass prompt.

**Re-review rounds keep all of it.** The delta bounds the scope, never the machinery: each round runs its own passes over the delta, and a claimed fix is confirmed by a fresh `sonnet` reader, not by this session against the developer's explanation. Accepting a developer's rejection is itself a judgment drop and goes to the adjudicators, this session's tiebreak falling toward keeping. Bound the delta by object name — commits where the branch commits between rounds, tree objects where it does not, since a `one-team` run stages everything until Phase 6 and `HEAD` never moves. Record `git -C <TARGET> write-tree` once a round's findings settle — in that round's entry in the report, or in `review.md` for the `one-team` reviewer, since an unrecorded tree name is the next round's lower bound lost — bound the next round by that snapshot and the current one, and re-snapshot rather than trust an old name after a long idle: those trees are unreferenced and a `gc --prune=now` takes them.

**Where the harness cannot spawn `fable`**, spawn the passes on whatever tier it offers and record that it happened — but do not prefer this. Independence does not compensate for tier: two `sonnet` passes were measured at 26% recall against a single `fable` pass at 51%, so where `fable` can be spawned at all, one `fable` pass beats two cheaper ones and two beats three. Only where no subagent of any tier can be spawned does the shape change: run one orchestrated pass here — every lens in its own subagent, blast-radius call-site reading delegated with the searches at any reference count above zero — record `reading: orchestrated`, and name a separate session as the better option. Where nothing can be spawned, work the lenses inline, record `reading: contaminated`, and name a separate session as the only real remedy; `one-team` Phase 5 consumes that admission rather than failing the gate on it.

### How a candidate dies

This governs the combining session, not a pass: a pass filters its own candidates directly, its context being clean is the whole reason it was spawned, and convening adjudicators against itself costs two agents per discarded thought.

No candidate is dropped on a contaminated session's own judgment. The power to drop is split, and the split is narrower than it reads:

- **Fact** — the citation is wrong: the line does not exist, the quoted code is not in the file, the named identifier is not the one on that line. That is the whole of it. Deciding the code *handles* what the candidate claims — a guard two lines up, a caller that already checks, an unreachable branch — is judgment however factual the reading felt, and it is exactly what a session that wrote the guard is most convinced by.
- **Judgment** — everything else, every "intentional", "not reachable", "acceptable here". Spawn two `sonnet` adjudicators separately, each given the candidate, its `file:line`, the repo to read for itself, and the acceptance criteria — never this session's reason for doubting it, and never a hand-picked excerpt, since choosing what to show frames the outcome while formally complying. Both must reject to drop; a split keeps it.

Report every dropped candidate with the route that dropped it, and a fact drop with the reading that disproved it. The verdict follows the surviving list, not this session's sense of whether the change is good — the last place contamination has to hide.

### What the readers are given

Every reader — each pass, its lens subagents, both adjudicators, the completeness reader, the fix-verification readers — gets the acceptance criteria: `plan.md` and `test-report.md` where a `one-team` run has them, otherwise the criteria file below. A FRESH reviewer would read those, so withholding them breaks parity in the other direction and leaves nobody holding code and intent at once, which is how a change that plausibly implements the wrong requirement passes every reader. What is withheld is this session's **narrative** — the requirements as it remembers them, the rationale, "this is safe because", anything forwarded from a briefing.

Where no `plan.md` exists, this session derives the criteria from the ticket, cites it, and writes them **once, before any pass is spawned**, to `$(git -C <TARGET> rev-parse --path-format=absolute --git-common-dir)/one-review/criteria-$(git -C <TARGET> rev-parse HEAD)` — in the git directory for the reason the receipts are, pinned and absolute for the reason Record the Verdict gives. Passes only read it: leave the derivation to them and two race to write that same path, the later one then reading the earlier's transcription instead of the ticket. Every reader gets the ticket source alongside the file, so a criterion narrowed in transcription stays catchable. Where no external statement of intent exists at all, this session authors them from memory and the report says so: intent is self-attested there, and every completeness verdict inherits the caveat. `--read-only` writes this file anyway — its one exception, since the flag protects the working tree and the git directory is not the tree — and without it there is no artifact to hand a pass at spawn time, leaving the session to inline its own transcription into every prompt, which is the leak this section exists to prevent. The criteria also go at the top of the report.

**Every reader reads code, not the run.** The only run artifacts any of them may open are the acceptance-criteria paths handed to it. Off limits, named in every prompt: the `one-team` team directory — `dev-handoff.md`, `team-log.md`, any round's `review.md` — and everything under `one-review/` in the git common dir except the handed criteria file, receipts and prior reports included. The pointer-only rule governs what a reader is told; without this it governs nothing, since a diligent reader exploring the repo finds the team directory itself and anchors on the developer's account or last round's findings. The adjudicators are where it costs most: they are the drop gate a contaminated session may not operate, and a prior `review.md` tells them which way it went last time.

**Re-derive every check** throughout, per the Evidence rule in `criteria.md`: re-read each changed file from disk, re-grep each ERC and endpoint path, re-trace each symbol — including the ones this session opened while writing the change, which it is most confident about and least likely to reopen. Step 2 is where this starts and where it is cheapest to fake, so state in the report what kind of change this is and, per changed file, what was read around it.

## Passes

One pass finds about half of what is there, and that is measured rather than assumed. Eleven independent passes over the same seven-hundred-line migration diff, scored against the thirteen real defects they found between them: one pass averaged 51% of all of them and 50% of the major ones; two passes 69% and 83%; three passes 77% and every major one. Reading harder does not close that gap. Reading again does.

**Every review runs at least two passes.** A pass is one complete run of Steps 1 through 5 by a reader that holds the branch and nothing else. Spawn them concurrently with the pointer-only prompt above.

Two passes is the floor at every diff size. The second pass is the best marginal purchase on the curve — eighteen points of overall recall and thirty-three of major recall — and the cost scales with the diff rather than sitting on top of it, so a pass over five changed lines is a small agent doing a small job.

**Do not cheapen the tier to save tokens.** It was tried and measured backwards: a `sonnet` pass returned a third of `fable`'s recall, a fifth of its major recall, and burned *twice* the tokens doing it, because it takes far more turns to do worse work. Two `sonnet` passes cost 15M tokens for 26% recall where two `fable` passes cost 7M for 69%. Narrowing a pass to the highest-yield lenses measured no better, and mixing a cheap pass with a strong one was worse than simply running two strong ones. Passes run on `fable`; the only lever that trades cost against accuracy honestly is how many of them there are.

Passes must not know about each other — not each other's findings, not each other's prompts, not the fact that another pass exists. A pass told what an earlier one found stops searching and starts agreeing, which is the anchoring this whole section exists to prevent, reintroduced at the last moment from a source that looks authoritative. This is the one rule here worth more than the count: two passes that saw each other are one pass and a rubber stamp.

Each pass establishes its own diff from `<BASE>`. Handing them a single diff saves almost nothing and makes every pass inherit the same Step 2 mistake — a wrong base, a missed uncommitted file — with no reader left who could notice.

A third pass costs about half again and is where major recall completed in the one study that measured it, 83% to 100% — on a single sample, so treat it as the escalation for a change that must not ship broken rather than as a rule. **Do not trigger it on how much the first two overlapped.** That heuristic was measured and does not predict: the lowest-overlap pair gained nothing from a third reader while the highest-overlap pair gained the most. The one clear signal is two passes that both found nothing on a diff above trivial size — a thin sample twice over, which earns a third pass rather than an approval. State how many ran.

**A pass runs Steps 1 through 5, and none of the orchestration.** It spawns no passes of its own, per the structural exemption in Independence From the Change.

## Combining the Passes

Union, never intersection. Both of the serious defects in the run above came from a single pass, so an intersection would have discarded them and reported a cleaner diff than the truth.

Merge on the defect, not on the wording: two passes describing one cache bug in different sentences are one finding, and one line carrying two unrelated defects is two.

- **Corroborated** — two or more passes reported it. Promote it and carry the count.
- **Single-pass** — one did. Read the cited `file:line` yourself and confirm it before promoting, then promote it and mark it single-pass. Being alone is not evidence against a finding: the two hardest defects in the measured run were alone. Dropping one is a drop like any other — fact only, judgment to the adjudicators.

Where passes disagree on severity, carry the highest — never the average, and never this session's own read where it is `SELF` or `BRIEFED`. Down-rating a blocker to a minor is the same power as dropping it, exercised more quietly, so a contaminated session that believes the lower severity is right routes it through the two adjudicators exactly like a drop. A `FRESH` session may settle it by reading, and says in the report that it did.

Prior rounds settle, but only one way. A finding a previous round **rejected**, with a reason accepted then, does not reopen merely because a new pass that cannot see that history rediscovered it: merge it against that round's record and move on, since the point of hiding prior findings is fresh eyes, not perpetual litigation. A finding previously marked **fixed** is the opposite case — a fresh reader re-reporting the same defect after the fix landed is evidence the fix did not take, so it reopens and is cross-checked against what the fix-verification reader said. Settling that one away leaves a single reader standing between an unfixed blocker and `APPROVED`.

Every finding carries its corroboration count into the report as **provenance, not confidence**. Agreement between passes is not evidence a finding is right: in the study behind these numbers the most corroborated defect of all, reported by eight passes of eleven, was the only one adjudicated false — while two of the four major defects were each found by a single pass. Report the count so a reader knows how a finding was arrived at, and never let it stand in for verification.

**Then one completeness reader over the combined report.** A fresh `sonnet` subagent gets the diff and the merged findings and answers a single question: what would a reviewer seeing this for the first time have examined that this report never mentions? Run it here rather than inside a pass, where it could only ever see one reader's blind spots — over the combination it sees what every pass missed together, which is the gap worth finding. Whatever it names goes to a fresh reader under the pointer-only rule, never to this session, and its candidates enter the merge above like any other. Report what it named and what came of each, even when it named nothing.