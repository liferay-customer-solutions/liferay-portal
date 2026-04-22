# Architecture — Detailed Design

Source-of-truth for the `liferay-one-workspace` implementation. Each document narrows a decision from [`../plan/system-spec.md`](../plan/system-spec.md) to the level an engineer can build from without follow-up.

Target workspace location: `workspaces/liferay-one-workspace/`.

## Reading order

1. [`workspace.md`](./workspace.md) — shell layout, build, naming conventions
2. [`data-model.md`](./data-model.md) — full entity index (all domains), ERC + FriendlyURL registry, migration sources
3. `business-logic.md` — Object Actions, Scheduled Tasks, Validations, Workflows
4. `api.md` — Headless conventions + `etc-spring-boot` custom REST contracts, OAuth2 scopes
5. `integrations/` — external-system contracts (Salesforce Pub/Sub, Jira, GCS, Liferay Cloud, Analytics Cloud, NAV)
5a. `provisioning-hub.md` — how liferay-one acts as the Provisioning Hub service (calling Liferay Cloud, Console, Analytics Cloud)
6. `ui.md` — Site-initializer page groups, custom elements, navigation + role-based permissions
7. `migration.md` — Phase-by-phase sequencing and cut-over gates

## Conventions

- Every design choice cites the parent system-spec decision (`D1`–`D14`) so reviewers can trace back.
- Objects are referenced by `externalReferenceCode` (ERC), not display name — ERCs are the stable identifiers downstream tooling keys off.
- Open questions inherit status from [`../plan/system-spec.md` §10](../plan/system-spec.md); change here first, roll back to the spec on consensus.
- JSON in fenced blocks illustrates the target object-definition shape. It is a design artifact — the implementation phase produces the real files under `workspaces/liferay-one-workspace/client-extensions/liferay-one-site-initializer/site-initializer/object-definitions/`.
- When this folder and `../plan/system-spec.md` disagree, **this folder wins** — the spec is the 30,000-ft brief; these are the construction drawings.

## Status

| Doc | Status |
|---|---|
| `workspace.md` | draft |
| `data-model.md` | draft |
| `business-logic.md` | draft |
| `api.md` | draft |
| `provisioning-hub.md` | draft |
| `integrations/README.md` | draft |
| `integrations/salesforce.md` | draft |
| `integrations/jira.md` | draft |
| `integrations/gcs.md` | draft |
| `integrations/liferay-cloud.md` | draft |
| `integrations/analytics-cloud.md` | draft |
| `integrations/nav.md` | draft |
| `integrations/marketo.md` | draft |
| `ui.md` | draft |
| `migration.md` | draft |
