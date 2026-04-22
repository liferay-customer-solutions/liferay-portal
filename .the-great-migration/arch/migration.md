# Migration Sequencing

Implements system-spec §9. Phase-by-phase sequencing, cut-over gates, dual-write patterns, preservation constraints, archival procedures. This doc is the runbook; individual domain docs hold the per-Object transforms.

## Principles

1. **Legacy runs in parallel until cut-over.** Nothing gets decommissioned until the new path passes a gate.
2. **Preserve external keys byte-identical.** `accountKey`, `contactUuid`, `productPurchaseKey`, `teamKey`, `productKey`, `jiraIssueKey`, `salesforceId`, license key strings — all unchanged so external callers continue to resolve.
3. **Dual-read, single-write at cut-over.** A caller migrating to the new API must be able to read from both old and new for a short overlap; writes point at one system per flow.
4. **Gate every phase.** No phase completes without the explicit gate criteria below being verified.
5. **Archive before delete.** Every table dropped gets a JSON-lines dump to GCS (`gs://one-legacy-archive/{system}/{table}.jsonl.gz`) with a manifest.

---


## Source Databases

| Database | Description |
|---|---|
| `kor` | Koroneiki — master ERP for accounts, contacts, products, purchases, entitlements |
| `prov` | Provisioning — license key orchestration; some tables have unknown owning module |
| `e5a2_lpartition_1860468` | Support Liferay instance — Objects-based (AccountSubscription, KoroneikiAccount, BusinessEvent, …) |
| `e5a2_lpartition_11706165` | Marketplace Liferay instance — Objects-based (PublisherDetails, PublisherAssets, …) |

### Legacy Tables Not Modelled (archive or drop)

| Table | Rows | Disposition |
|---|---:|---|
| `kor.Koroneiki_AuditEntry` | 17,462,542 | Archive to GCS JSON lines; do not migrate to Objects |
| `kor.Koroneiki_Contact` | 20,397 | Dissolves into Liferay `User` + account membership |
| `kor.Koroneiki_ContactAccountRole` | 32,647 | Becomes Liferay Account Role assignments |
| `kor.Koroneiki_ContactTeamRole` | 29,262 | Collapses to Team ↔ User membership (role-within-team dropped) |
| `kor.Koroneiki_TeamRole` | 2 | **Dropped** |
| `kor.Koroneiki_TeamAccountRole` | 39 | **Dropped** |
| `kor.Koroneiki_ServiceProducer` | 21 | Replaced by OAuth2 client credentials |
| `kor.Koroneiki_AuthenticationToken` | 27 | Replaced by OAuth2 client credentials |
| `prov.OSB_LicenseKey` | 201,897 | Overlap-check against Provisioning_LicenseKey; archive after migration |
| `prov.OSB_OfferingEntry` | 76,504 | Legacy; archive |
| `prov.OSB_ProductEntry` | 112 | Legacy; archive |
| `e5a2.Marketplace_App` | 344 | Legacy; archive after retention period |
| `e5a2.Marketplace_Module` | 1,817 | Legacy; archive after retention period |

 ---

## Phase 0 — Audit (done)

`../audit/`.

---

## Phase 1 — Workspace shell + Object definitions

### Scope
- Stand up `workspaces/liferay-one-workspace/` per [`./workspace.md`](./workspace.md).
- Deploy all Object definitions per [`./data-model.md`](./data-model.md), including:
  - Customer domain
  - Commerce/Deployment
  - Entitlement
  - Marketplace
  - Support
  - Reference (seed `Region`, `DataCenter`, `BannedEmailDomain`)
- Define Account Role catalog (per [`./business-logic.md §5`](./business-logic.md)).
- Define OAuth2 applications + scopes.
- Deploy `liferay-one-site-initializer` skeleton — all three page groups exist as empty shells.

### Non-goals
- No data migrated.
- No custom elements functional (skeleton only).
- No integrations live (Spring Boot app deploys but subscribers are gated off).

### Gate
- All Object definitions reviewable in the Object admin UI.
- Headless REST returns empty lists for every Object.
- OAuth2 token requests succeed for each registered application.
- Site initializer runs cleanly on a fresh Liferay instance.

---

## Phase 2 — Koroneiki migration

**Biggest phase.** Koroneiki's 19 entities + 62 entitlement rules + 18K accounts + 20K contacts + 254K ExternalLink rows.

### Prereqs
- Phase 1 gate passed.
- ~~**Open risk #2**~~ _Resolved: Liferay owns the license-key module. Signing key and algorithm are available internally. No external dependency._
- **Open risk #6 resolved** (Support KoroneikiAccount sync mechanism identified — informs phase-5 retirement).

### Extract scripts (run once, checked into `migration/` under the workspace)

| Source table | Target | Notes |
|---|---|---|
| `kor.Koroneiki_Account` | Liferay `AccountEntry` + custom fields | Preserve `code` as `koroneikiAccountCode`; `logoId` dropped |
| `kor.Koroneiki_Contact` | Liferay `User` | Reconcile by `uuid`; osb-entity-web bridge dissolves |
| `kor.Koroneiki_Team` | `Team` Object | Preserve `teamKey`; `system` flag intact |
| `kor.Koroneiki_ContactAccountRole` | Liferay Account Role assignments | Role-name remap: `Account Customer Admin` → `Customer_Admin`, etc. |
| `kor.Koroneiki_ContactTeamRole` | `Team` M:N membership | Drop role-within-team (D3) |
| `kor.Koroneiki_AccountNote` | `AccountNote` Object | Preserve frozen `creatorName`/`creatorUID`/`modifierName`/`modifierUID` |
| `kor.Koroneiki_ExternalLink` | `ExternalLink` Object | Filter `domain=web` rows (osb-entity-web) — keep during migration, drop in phase 6 |
| `kor.Koroneiki_ProductEntry` | Commerce `CPDefinition` | Preserve `productKey`; classify `productFamily`; add `isPrimary` (see entitlement review) |
| `kor.Koroneiki_ProductPurchase` | Commerce `CommerceOrder` + `CommerceOrderItem` | One order per purchase; subscription-enabled items for recurring |
| `kor.Koroneiki_ProductConsumption` | `LicenseKey` | One activation = one key |
| `kor.Koroneiki_ProductField` | Schematized fields on target entity | Extract distinct field-name set first; hand-classify per owner type |
| `kor.Koroneiki_EntitlementDefinition` | `EntitlementDefinition` Object | Raw SQL discarded; new `ruleBody` from rule-by-rule translation per [`../plan/entitlement-rules-review.md`](../../plan/entitlement-rules-review.md) |

### Account-code collision handling

Koroneiki enforces `code` unique case-insensitively with a computed default. The new `koroneikiAccountCode` is unique case-insensitive; collisions during migration extract get a `-2`, `-3` suffix with the collision-suffix utility. Mapping captured in the migration's `code-collisions.csv` artifact.

### Contact-User reconcile

Koroneiki Contact `uuid` = osb-entity-web User `uuid`. Migration looks up each Contact's uuid in Liferay Users; if found, adds account membership + roles; if not, creates a Liferay User with the same uuid.

### ProductField extraction

`Koroneiki_ProductField` stores ad-hoc `(classNameId, classPK, name, value)` tuples. Process:
1. Extract distinct `(classNameId, name)` tuples; output `productfield-distinct.csv`.
2. Hand-classify each row: which target Object does it belong on, what DBType?
3. Generate `CPDefinition` / `CommerceSubscriptionEntry` / `Deployment` / `LicenseKey` field-additions as part of the site-initializer.
4. Populate those new fields during the ProductPurchase / ProductConsumption / ProductEntry load.

### Entitlement definition load

Per-rule translation:
1. Take each of the 62 rules from `../plan/entitlement-rules-review.md`.
2. Write the corresponding `ruleBody` JSON.
3. Set `ruleType` (`filter` for 59, `scripted` for 3).
4. Set `cascadeAfter` where applicable (#62 after #40).
5. Load as `EntitlementDefinition` rows; leave `status=Draft` until phase-2 smoke test.

### Prerequisites for rule translation
- `isPrimary` custom field exists on `CPDefinition` (deployed in phase 1 per `./data-model.md §Commerce Product`).
- Rule #61 (Liferay Employee) hardcoded `accountId = 15097278` replaced with AccountEntry lookup by `code`.
- Partner Account Roles (`Partner_Manager`, `Partner_Member`) seeded.

### Archive
- Dump `Koroneiki_AuditEntry` (17M rows) to `gs://one-legacy-archive/koroneiki/audit_entries.jsonl.gz`.
- Leave `kor` database intact — deletion in phase 6.

### Gate
- All 18,390 accounts present in new workspace with matching `koroneikiAccountCode`.
- All 20,397 contacts present as Liferay Users with matching UUID.
- All 18,570 teams migrated.
- All 62 rules loaded as `EntitlementDefinition` rows with classified `ruleType`.
- `EntitlementSync` run against new data produces `Entitlement` row count within 5% of legacy `Koroneiki_Entitlement` row count (exact match not required — legacy data may have drift from last sync).
- Koroneiki Phloem REST still live — external callers not cut over yet.

### Parallel-run period
- 2 weeks: Koroneiki keeps taking writes; workspace reads from new model; diffs flagged in `migration-diff-daily` report.

---

## Phase 3 — Provisioning / LicenseKey migration

### Prereqs
- Phase 2 gate passed.
- ~~**Open risk #2**~~ _Resolved: Liferay owns the license-key module. Extract the signing private key into `liferay-one-instance-settings` secret `license-signing-private-key` before phase 3 starts._
- **Open risk #5 resolved**: the RabbitMQ → Pub/Sub bridge located.

### Extract

| Source table | Target |
|---|---|
| `prov.Provisioning_LicenseKey` | `LicenseKey` |
| `prov.Provisioning_SubscriptionEntry` | Commerce `CommerceSubscriptionEntry` (merge with Koroneiki ProductPurchase data) |
| `prov.Provisioning_ProductVersion` | `CPDefinition.licenseKeyProductVersion` (lookup + merge) |
| `prov.Provisioning_LicenseEntry` | — evaluate; likely drops |
| `prov.Provisioning_CommonLicenseKey` | — evaluate; likely `CPDefinition` metadata |
| `OSB_LicenseKey` | — legacy; confirm overlap, archive |

### Salesforce Pub/Sub subscriber cut-over

1. Stand up `SalesforceOpportunitySubscriber` in `liferay-one-etc-spring-boot` subscribing to the existing SF topic.
2. Run in **shadow mode** for 48h: subscriber logs what it would do but doesn't write. Compare output to Dossiera's output.
3. Flip to **dual-write**: subscriber writes to new workspace, Dossiera continues to write to Koroneiki. 1-week overlap to catch discrepancies.
4. Flip to **single-write**: Dossiera subscriber disabled; new subscriber is authoritative. Koroneiki takes inbound writes only via manual operator actions.

### Retirements in this phase
- Dossiera subscriber disabled (not uninstalled — kept available for rollback until phase 6).
- `ZendeskTicketWebService`, `LCSSubscriptionEntryWebService` in osb-provisioning — stop invoking but leave code for safety through phase 5.
- osb-provisioning deploy marked deprecated; no new code.

### Gate
- Every active `Provisioning_LicenseKey` row has a matching `LicenseKey` row with identical `key` string.
- Salesforce Pub/Sub subscriber runs in single-write mode with zero discrepancies over a 48h window.
- Commerce subscriptions match `Provisioning_SubscriptionEntry` 1:1 by external reference.

### Parallel-run period
- 2 weeks dual-write on the Pub/Sub subscriber before flipping single-write.
- 4 weeks post-single-write before phase 4 opens — gives time to catch rare SF event types.

---

## Phase 4 — Marketplace migration

### Prereqs
- Phase 3 gate passed.

### Extract + port

| Source | Target |
|---|---|
| Marketplace Objects (12) | `data-model.md §Marketplace domain` — direct ports |
| `product-approver-workflow` | `site-initializer/workflow-definitions/` |
| 7 cron jobs | `liferay-one-etc-cron` (per `./business-logic.md §Scheduled Tasks`) |
| 10 Spring Boot controllers | `liferay-one-etc-spring-boot` (per `./api.md`) |
| React custom element | Marketplace features inside `liferay-one-custom-element` |
| Site content (fragments, pages, layouts, masters, DDM templates) | Marketplace page group of `liferay-one-site-initializer` |

### Commerce custom-field blob extraction

`CommerceOrder.customFields.trial-end-date`, `cloud-provisioning`, `koroneiki-project-ids` → `TrialProvisioning` rows. Script reads every Commerce order, extracts JSON, writes TrialProvisioning + links. Runs once; legacy fields stay readable for rollback.

### OrderType seeding

Extract distinct `CommerceOrderType.externalReferenceCode` from current Marketplace; hand-classify each against the switch statement in today's post-purchase controller; write `OrderType` rows per `./objects/marketplace.md`.

### Pub/Sub listener retirement

Today's Marketplace consumes `koroneiki.account.*` via Pub/Sub. In the new workspace those events terminate inside the workspace as Object Actions. Pub/Sub consumer code gets removed when `liferay-marketplace-workspace` is decommissioned.

### Retirements
- `liferay-marketplace-workspace` deploy flagged for removal (remove in phase 6).
- Marketo form-ID references — ported but functionally unchanged.

### Gate
- All 12 Marketplace Objects present with row counts matching legacy.
- All 7 cron jobs running in `liferay-one-etc-cron` on schedule.
- TrialProvisioning rows exist for every active trial.
- Marketplace custom element renders in the Marketplace page group.
- E2E: browse → add to cart → checkout → trial provisioned works end-to-end in staging.

---

## Phase 5 — Support migration

### Prereqs
- Phase 4 gate passed.

### Extract + port

| Source | Target |
|---|---|
| Customer Objects (26 → ~18 after D4 consolidation) | `data-model.md §Ticket Management` + `§Account Management` |
| 5 scheduled tasks | `liferay-one-etc-cron` |
| Spring Boot controllers (Jira, GCS, ticket attachments) | `liferay-one-etc-spring-boot` |
| React custom element | Support features inside `liferay-one-custom-element` |
| Site content | Support page group of `liferay-one-site-initializer` |

### KoroneikiAccount side-car dissolution

`KoroneikiAccount` 2,313 rows fold into `AccountEntry` custom fields (per `./objects/customer.md`). Migration step:
1. For each `KoroneikiAccount` row, update the matching `AccountEntry` (looked up by `code`) with the side-car fields.
2. Convert `partnership*` fields to `AccountFlag` rows.
3. Convert `allowSelfProvisioning` to `AccountFlag(flagCode=SELF_PROVISIONING)`.
4. Archive the source table.

**Open risk #6**: once the external sync mechanism is identified, disable it. `AccountEntry` is now authoritative.

### Jira webhook stand-up

Configure Jira to POST to `/o/one/v1/jira/webhook` on issue-state changes. HMAC secret exchanged out-of-band.

### Retirements
- `liferay-customer-workspace` deploy flagged for removal.
- `KoroneikiAccount` side-car disabled.
- Zendesk `ZendeskTicketWebService` calls removed (no more invocations anywhere after phase 5).

### Gate
- All Support Objects migrated; row counts match.
- Customer portal pages render.
- Jira webhook delivering updates to `SupportTicket.cached*`.
- Attachment upload E2E works (initiate → upload → complete → Jira comment posted).

---

## Phase 6 — Decommission

### Prereqs
- Phase 5 gate passed.
- 30-day quiet period after phase 5 with no production incidents attributable to the migration.

### Sequence

1. Stop osb-provisioning RabbitMQ consumers; retire Koroneiki Xylem publishers.
2. Decommission Dossiera.
3. Drop osb-provisioning deploy.
4. Drop `liferay-marketplace-workspace` deploy.
5. Drop `liferay-customer-workspace` deploy.
6. Drop `osb-koroneiki` deploy.
7. Archive `kor`, `prov`, `e5a2_lpartition_11706165` (Marketplace), `e5a2_lpartition_1860468` (Support) databases to GCS as encrypted dumps.
8. Drop legacy databases after 90-day retention.
9. Drop `OSB_*` and `Marketplace_App` / `Marketplace_Module` tables.
10. Drop `AccountEntry.dossieraId` field (migration-only, no longer referenced).
11. Drop `ExternalLink` rows with `domain=web` (osb-entity-web).
12. Drop migration-only fields: `koroneikiProductPurchaseKey`, `koroneikiProductKey`, `legacyLicenseKeyId`, `AccountFlag.accountKey`, `TrialProvisioning.koroneikiProjectIds`.

### Gate (phase 6 complete)
- Legacy workspaces removed from deploy manifest.
- Legacy databases archived and dropped.
- Migration-only fields removed from Object definitions.
- Slack cross-posted summary; retrospective scheduled.

---

## Preservation constraints (cross-phase)

These values **must carry forward byte-identical**:

| Value | Phase | Reason |
|---|---|---|
| `accountKey` / `koroneikiAccountCode` | 2 | External callers resolve by this |
| `contactUuid` | 2 | Liferay User UUID = osb-entity-web UUID = existing token subject |
| `productPurchaseKey` | 2 | Migration-only on `CommerceSubscriptionEntry`, kept until phase 6 |
| `productKey` | 2 | Migration-only on `CPDefinition` |
| `teamKey` | 2 | External callers reference teams |
| `jiraIssueKey` | 5 | Jira is authoritative; keys are Jira's |
| `salesforceId` (account) | 2/3 | Required by SF Pub/Sub subscriber for idempotency |
| License key string | 3 | Every running deployment depends on this |
| Commerce order IDs | 4 | Commerce layer preserved — IDs unchanged |

---

## Dual-write / dual-read patterns

### Pattern A: Legacy write, new read
Phase 1–2: nothing writes the new workspace yet; extracts run one-way. Callers still hit Koroneiki.

### Pattern B: Shadow mode (SF subscriber, phase 3)
New subscriber runs and logs what it would do. No writes. Lets us verify before enabling.

### Pattern C: Dual-write (phase 3)
Both old and new subscribers write; compare outputs daily. Catches divergences.

### Pattern D: Single-write + dual-read (phase 3–5)
New path is authoritative. Reads can come from either. Legacy remains queryable for fallback.

### Pattern E: Single-write + single-read (phase 5–6)
New path is authoritative for writes and reads. Legacy is read-only for reference.

### Pattern F: Archived (phase 6)
Legacy removed from live infrastructure.

---

## Data-diff reports

During dual-write periods (phases 2–5), a nightly `migration-diff-daily` report compares key aggregates:

- Account count — legacy vs new, diff should be 0.
- Contact-to-Account membership count.
- Active subscription count.
- LicenseKey count and status breakdown.
- Entitlement materialization count, broken down by definition.

Reports land in `gs://one-legacy-archive/diffs/{date}/` and Slack-summarize to `#one-migration`.

---

## Rollback

Per phase:

| Phase | Rollback |
|---|---|
| 1 | Drop the workspace. No production traffic. |
| 2 | Keep the workspace online (read-only). External callers continue hitting Koroneiki. Re-run extract after fix. |
| 3 | Re-enable Dossiera subscriber; disable workspace subscriber. License generation reverts to legacy path. |
| 4 | Re-enable `liferay-marketplace-workspace` deploy; point Marketplace traffic there. Customer Portal remains on new workspace. |
| 5 | Re-enable `liferay-customer-workspace` deploy. Customer Portal reverts. |
| 6 | Not rollback-able beyond the 30-day quiet-period review. |

Every phase's rollback is tested in staging before production cut-over.

---

## Cut-over communication plan

For each phase, 2 weeks before cut-over:
- Email internal teams (Sales, Support, Engineering).
- Slack announce in `#engineering`.
- Status-page entry for scheduled maintenance window.
- Update external-integration partners (e.g., SF admin, LXC team) for cross-team coordination.

---

## Open questions

1. ~~**Provisioning license-key module ownership (open risk #2).**~~ _Resolved: Liferay owns the module. Extract signing key to `license-signing-private-key` secret before phase 3._
2. **KoroneikiAccount sync mechanism (open risk #6).** Unknown source process writes the 2,313 rows. Find before phase 5 so we know what to disable.
3. **RabbitMQ → Pub/Sub bridge location (open risk #5).** Needed for phase 6 decommission.
4. **Legal retention requirements.** Audit logs (17M), old Marketplace tables, OSB_* tables. Confirm retention periods with compliance before starting phase 6.
5. **Cut-over windows.** Every phase needs a maintenance window. Coordinate with product operations to pick times.
6. **External caller inventory.** Every system hitting Koroneiki Phloem today must be re-pointed at the new workspace during phase 2. Inventory (see `./api.md §5.1`) must be complete before phase 2 concludes.
