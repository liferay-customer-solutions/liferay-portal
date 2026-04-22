# Business Logic

Implements system-spec §4. Consolidates Object Actions, Scheduled Tasks, Validations, Workflows, and the Role + Permission catalog. Each section is a pointer into implementation work — the "what runs when" list the rest of the workspace takes as given.

---

## 1. Object Actions

Object Actions fire synchronously on Liferay Object lifecycle events. Most run in-process; a few long-running actions (trial provisioning, license generation, Jira calls) delegate to `liferay-one-etc-spring-boot` via internal REST.

### 1.1 Customer domain

| Object | Trigger | Action | Impl notes |
|---|---|---|---|
| `AccountEntry` | `onBeforeCreate` / `onBeforeUpdate` | Uppercase `koroneikiAccountCode`; generate from `name` if null (slug transform + collision suffix) | Groovy Object Action |
| `AccountEntry` | `onAfterCreate` / `onAfterUpdate` | `syncDefaultTeam(accountEntryId)`: ensure `system=true` Team exists; reconcile Team members with account's Customer users | Delegates to Spring Boot — needs cross-table membership query |
| `AccountEntry` | `onAfterCreate` / `onAfterUpdate` | Ensure `ExternalLink` rows for `salesforceId` / `dossieraId` (dual-write during migration) | Groovy |
| `AccountNote` | `onBeforeCreate` | Populate frozen `creatorName` / `creatorUID` from `ServiceContext.user` | Groovy |
| `AccountNote` | `onBeforeUpdate` | Populate frozen `modifierName` / `modifierUID`; prevent writes to `creatorName` / `creatorUID` | Groovy |
| `Team` | `onBeforeCreate` | Uppercase `teamKey`; generate from `name` if null | Groovy |
| `Team` | `onBeforeDelete` | Reject when `system=true` unless caller has Administrator role | Groovy |
| `AccountFlag` | `onBeforeCreate` | Validate `flagValue` is required/absent per `flagCode`'s value type | Groovy |

### 1.2 Commerce & Deployment

| Object | Trigger | Action | Impl notes |
|---|---|---|---|
| `Deployment` | `onAfterAdd` / `onAfterUpdate` | Trigger entitlement re-sync scoped to this deployment's account; push snapshot to Admin UI cache | Groovy + POST to `/entitlements/recompute?accountEntryId=...` |
| Commerce subscription lifecycle (internal Liferay event) | On `Active` transition | Call `POST /license-key/generate/{subscriptionId}`; send notification; re-sync entitlements | Spring Boot bean registered on Liferay's internal Commerce event bus — see [`./provisioning-hub.md`](./provisioning-hub.md) |
| Commerce subscription lifecycle (internal Liferay event) | On `Expired` / `Cancelled` | Revoke LicenseKeys (status → Revoked); re-sync entitlements | Spring Boot bean on Liferay Commerce event bus |
| `LicenseKey` | `onBeforeCreate` | Reject if `key` collides; default `status=Active`; populate `legacyLicenseKeyId=null` (migration-only path sets it explicitly) | Groovy |
| `LicenseKey` | `onBeforeUpdate` | Reject transitions from terminal states (`Expired`, `Revoked`, `Superseded`) back to `Active` | Groovy |

### 1.3 Entitlement

| Object | Trigger | Action | Impl notes |
|---|---|---|---|
| `EntitlementDefinition` | `onBeforeCreate` / `onBeforeUpdate` | Validate `ruleBody` against the JSON schema for `ruleType`; reject cycles in `cascadeAfter` | Spring Boot JSON-schema validator |
| `EntitlementDefinition` | `onAfterUpdate` (status Draft→Approved) | Enqueue an immediate `EntitlementSync` scoped to this definition | Groovy + POST to `/entitlements/recompute?definitionCode=...` |

### 1.4 Marketplace

| Object | Trigger | Action | Impl notes |
|---|---|---|---|
| `Publisher` | `onBeforeCreate` | Enforce 1:1 with AccountEntry (query existing); normalize slug | Groovy |
| `Publisher` | `onAfterUpdate` (approvalStatus → `Approved`) | Create Commerce `CommerceCatalog`; populate `commerceCatalogId`; send approval email via `MARKETPLACE-PUBLISHER-APPROVED-TEMPLATE` | Spring Boot — Commerce API + notification |
| `Publisher` | `onAfterUpdate` (approvalStatus → `Suspended` / `Rejected`) | Disable Commerce catalog; hide assets | Spring Boot |
| `PublisherAsset` | `onAfterAdd` | Email dispatch via `MARKETPLACE-PRODUCT-SUBMIT-TEMPLATE` | Groovy |
| `PublisherAsset` | `onAfterUpdate` (publishStatus → `Published`) | Flip linked Commerce `CPDefinition` to published; notify publisher | Spring Boot |
| `TrialProvisioning` | `onAfterAdd` | If `OrderType.provisioningFlow` in (trial-cloud / paid-cloud), call `POST /trial/provision/{subscriptionId}` | Spring Boot |
| `RequestPublisherAccount` | `onAfterUpdate` (status → `Approved`) | Create AccountEntry + Publisher pair; send welcome email | Spring Boot |

### 1.5 Support

| Object | Trigger | Action | Impl notes |
|---|---|---|---|
| `SupportTicket` | `onAfterAdd` | `POST` to Jira REST to create issue; stash returned key in `jiraIssueKey`; send "ticket created" notification | Spring Boot — Jira client |
| `SupportTicket` | render-time (`etc-spring-boot` pre-read hook) | If `lastSyncedAt > 1h ago`, fetch Jira issue; update `statusCached` / `priorityCached` / `assigneeEmail` / `lastSyncedAt` | Spring Boot |
| `TicketAttachment` | `onAfterAdd (state=Approved)` / `onAfterUpdate (state → Approved)` | Post Jira ADF comment with signed download URL; on success clear `draftCommentBody`, on failure persist for retry | Spring Boot — GCS signed URL + Jira client |
| `TicketAttachment` | `onAfterUpdate (state → Trashed)` | Mark the row for GCS deletion (picked up by `TicketAttachmentTrashDrain`) | Groovy — set internal flag |
| `BusinessEvent` | `onAfterAdd` / `onAfterUpdate` | Write `BusinessEventVersion`; push Jira label update (`impacting_business_event`, `<heat>_be`) to `associatedTicketIds`; update Jira Assets Koroneiki object | Spring Boot — Jira client |
| `BusinessEvent` | `onAfterUpdate` (eventStatus → `Overdue`) | Email primary contact + Liferay owner; Slack-bridge notify support channel | Spring Boot |
| `CallbackRequest` | `onAfterAdd` | Slack bridge + oncall email | Spring Boot |
| `SupportTicketEscalation` | `onAfterAdd` | Route to Kaleo workflow `support-ticket-escalation-review` | native Liferay |
| `ReplacementActivationKeyRequest` | `onAfterUpdate` (status → `Issued`) | Call `POST /license-key/generate/{subscriptionId}`; email key artifact | Spring Boot |

### 1.6 Banned-email check pattern

Every form-submission Object (CallbackRequest, RequestPublisherAccount, SupportTicketEscalation, ReplacementActivationKeyRequest) carries `onBeforeCreate` — query `BannedEmailDomain` by the email's domain part; reject with `BannedEmailException` on hit. Implemented as a shared Groovy utility and invoked from each Object's Action.

---

## 2. Scheduled Tasks

All scheduled tasks live in `liferay-one-etc-cron` (Spring Boot + Quartz). Concurrency note: each task is **single-writer** — a lease in a `SchedulerLock` table (or Liferay's built-in clustered scheduler lock) prevents double-runs under load balancing.

| Name | Cron | Purpose | Implements |
|---|---|---|---|
| `EntitlementSync` | `0 */15 * * * *` (every 15 min) | Walk all Approved `EntitlementDefinition` rows; grant/revoke `Entitlement` per `ruleBody`; respect `cascadeAfter` (two-phase: AccountEntry then User) | system-spec §4.2; replaces Koroneiki `SynchronizeEntitlementsMessageListener` |
| `TrialLifecycleTick` | `0 0 */6 * * *` (every 6h) | For each `TrialProvisioning`: expire past-end-date trials; promote on-hold trials when seats free; send end-of-trial reminders 1 day out; auto-complete free pending orders | Marketplace `_processInProgressTrials`, `_processOnHoldTrials`, `_processPendingOrders` |
| `PublisherSalesSummaryRoll` | `0 0 2 * * *` (nightly 02:00 UTC) | Aggregate completed Commerce orders by (publisher, quarter) → `PublisherSalesSummary` | Marketplace `_processPublisherSalesSummary` |
| `RequestProductFeedbackFan` | `0 0 */6 * * *` (every 6h) | Email feedback survey to buyers whose orders are 7–14 days old | Marketplace `_processRequestProductFeedback` |
| `TicketAttachmentCleanup` | `0 0 0,12 * * *` (00:00 + 12:00 UTC) | Trash attachments 7–8 days after Jira ticket close (fetch ticket status from Jira) | Support `scheduledCleanUp` |
| `TicketAttachmentTrashDrain` | `0 0 * * * *` (hourly) | Delete GCS objects for rows with `state=Trashed` | Support `scheduledDeleteTicketAttachment` |
| `TicketAttachmentDraftCommentRetry` | `0 0 * * * *` (hourly) | For rows with non-null `draftCommentBody`, retry Jira comment post | Support `scheduledUpdateTicketAttachmentDraftCommentBody` |
| `BusinessEventOverdueSweep` | `0 0 6 * * *` (daily 06:00 UTC) | Flip `Planned`/`InProgress`/`AtRisk` events past `expectedGoLiveDateTime` to `Overdue`; notify | Support `BusinessEventService.scheduled` |
| `JiraHeatTagSync` | `0 0 7 * * *` (daily 07:00 UTC) | Replay heat labels + Jira Assets Koroneiki object writes for all open BusinessEvents | Support `AccountsRestController.scheduledHeatTagUpdate` |
| `LiferayStaffUserGroupSync` | `0 0 3 * * *` (daily 03:00 UTC) | Assign "Liferay Staff" user group + SSA-ACCOUNT membership to users with `@liferay.com` email | Marketplace `_processLiferayStaffUserGroups` |
| `ProjectsUsingMarketplaceReport` | `0 0 4 * * *` (nightly 04:00 UTC) | Aggregate Marketplace orders + Deployment/AccountEntry lookups into a `Report` entry | Marketplace `_processProjectsUsingMarketplaceApps` |

### Concurrency & failure handling

- Each task acquires a named lock at start; subsequent invocations on other nodes no-op.
- Failures log to workspace `SchedulerFailure` table (not modeled as an Object — internal Spring Boot table) with a retry-count. Beyond 3 failures, Slack-alert the engineering channel.
- `EntitlementSync` has a dedicated `POST /entitlements/recompute` escape hatch (see `./api.md`) for manual forcing — bypasses the 15-min cadence.

---

## 3. Validations

Field-level constraints enumerated per Object in their domain doc. This section captures the cross-Object invariants that don't fit a single Object's validation block.

| Invariant | Enforced by | Notes |
|---|---|---|
| `AccountEntry.koroneikiAccountCode` unique (CI) | DB-unique index + Object Action collision-suffix | Uppercased on write |
| `AccountEntry.parentAccountEntryId` is acyclic | Object Action depth check | Liferay-core also rejects self-reference |
| `CommerceSubscriptionEntry.originalEndDate >= startDate` | Commerce custom-field validator | Defaults to `endDate` at create |
| `Deployment.startDate <= endDate` | Object Action | |
| `LicenseKey.key` globally unique | DB-unique index | |
| `LicenseKey` status transitions (no `Active` from terminal) | Object Action | Terminal: Expired, Revoked, Superseded |
| `TicketAttachment` MD5 dedupe `(fileName, supportTicketId, md5Checksum)` unique unless `state=Draft` | Object Action | |
| Form-submission banned-email check | Shared Groovy utility called from each form's `onBeforeCreate` | Consumers: CallbackRequest, RequestPublisherAccount, SupportTicketEscalation, ReplacementActivationKeyRequest |
| `(Publisher.accountEntryId)` 1:1 | Object Action query | Enforce at `Publisher.onBeforeCreate` |
| `(EntitlementDefinition.targetClassName)` matches `Entitlement.targetClassName` for all child rows | `EntitlementSync` reconciliation | Non-matching rows deleted on sync |
| `BusinessEvent.actualGoLiveDateTime` only set when `eventStatus=Completed` | Object Action | |

---

## 4. Workflows (Kaleo)

| Workflow | Target | States | Transitions |
|---|---|---|---|
| `product-approver-workflow` | `PublisherAsset` | Draft → Under Review → (Approved | Rejected) | Content author submits; reviewer approves or rejects |
| `publisher-onboarding-workflow` | `RequestPublisherAccount` | Submitted → Under Review → (Approved | Rejected) | Auto-submit on create; admin reviews |
| `support-ticket-escalation-review` | `SupportTicketEscalation` | Submitted → Under Review → (Resolved | Rejected) | |

Each workflow is a Kaleo definition checked into `liferay-one-site-initializer/site-initializer/workflow-definitions/`. Transitions that write other Objects (e.g., approval → create Publisher) are scripted via Kaleo Groovy conditions + the Object Action triggers above.

---

## 5. Roles & Permissions

### 5.1 Role catalog

Two role layers:

**Instance-level roles** — apply across the workspace.

| Role | Purpose |
|---|---|
| `Administrator` | Liferay-built-in super-admin. |
| `Guest` | Liferay-built-in anonymous. |
| `Liferay Staff` | Internal Liferay employees. Assigned by `LiferayStaffUserGroupSync` to `@liferay.com` users. Does **not** auto-grant account-level Worker roles — those are assigned manually per account in the Admin UI. |
| `Publisher` | Instance-wide marker for users who own a Publisher record. Read-access to publisher-dashboard pages. |

**Account-level roles** — scoped per `AccountEntry`. Prefixed per system-spec D2 so Customer vs Worker distinction survives in the permissions layer.

| Role | Typical user | Notes |
|---|---|---|
| `Customer_Admin` | Customer admin on their account | CRUD own-account records; update select AccountEntry fields |
| `Customer_Manager` | Customer manager | CR on own-account records, no delete |
| `Customer_Member` | Customer member | R on own-account records |
| `Worker_Admin` | Liferay support / sales worker assigned to the account | Full CRUD on the account's records, can write BusinessEvent / AccountNote |
| `Worker_Manager` | Liferay worker with limited write | CRU on the account's records |
| `Worker_Member` | Liferay worker with read-only access | R on the account's records |
| `Partner_Manager` | Partner-account manager | (Partner tier — specific entitlements; see entitlement rule #62) |
| `Partner_Member` | Partner-account member | |

Dropped from the legacy Koroneiki catalog: `Partner Marketing User`, `Partner Sales User`, `Partner Technical User` (deprecated per entitlement review #62 notes).

### 5.2 Permission matrix (object baseline)

| Object | Administrator | Worker_Admin | Worker_Manager | Worker_Member | Customer_Admin | Customer_Manager | Customer_Member |
|---|---|---|---|---|---|---|---|
| `AccountEntry` | CRUD | CRUD | RU | R | RU (profile fields only) | R | R |
| `AccountFlag` | CRUD | CRUD | R | R | R | — | — |
| `AccountNote` | CRUD | CRUD | CRUD | R | R (General only) | R (General only) | — |
| `Team` | CRUD | CRUD | CRU | R | CRU (no system-team delete) | R | R |
| `ExternalLink` | CRUD | CRUD | R | R | — | — | — |
| `Deployment` | CRUD | CRUD | RU | R | RU (status/notes) | R | R |
| `LicenseKey` | CRUD | CRUD | R | R | R (own account) | R | R |
| `EntitlementDefinition` | CRUD | — | — | — | — | — | — |
| `Entitlement` | CRUD | R | R | R | R (own) | R (own) | R (own) |
| `Publisher` | CRUD | CRUD | R | R | RU (own profile) | — | — |
| `PublisherAsset` | CRUD | CRUD | R | R | CRUD (own publisher) | CRU | R |
| `PublisherAssetAttachment` | CRUD | CRUD | R | R | CRUD (own publisher) | CRU | R |
| `TrialProvisioning` | CRUD | CRUD | R | R | R | R | R |
| `OrderType` | CRUD | R | R | R | R | R | R |
| `SupportTicket` | CRUD | CRUD | CRU | R | CR | CR | R |
| `TicketAttachment` | CRUD | CRUD | CRU | R | CRUD (own account, draft/approved) | CRU | R |
| `SupportTicketEscalation` | CRUD | CRUD | R | R | CR | CR | — |
| `CallbackRequest` | CRUD | CRUD | R | R | C (anonymous form) | — | — |
| `ReplacementActivationKeyRequest` | CRUD | CRUD | R | R | CR (own account) | — | — |
| `BusinessEvent` | CRUD | CRUD | CRU | R | R | R | R |
| `BusinessEventVersion` | R | R | R | R | R | R | R |
| `Region` / `DataCenter` / `BannedEmailDomain` | CRUD | R | R | R | R | R | R |

Notation: `C` create, `R` read, `U` update, `D` delete. Row-level scoping (Customer_* roles only see own-account rows) is enforced by `accountEntryRestricted: true` on each Object plus Liferay's account-role filtering.

### 5.3 Role assignment

- **Customer roles** — assigned per AccountEntry membership. `Customer_Admin` assignments flow from the Salesforce contact record (D12 subscriber reads the SF contact role and maps to `Customer_Admin` / `Customer_Manager` / `Customer_Member`). Customer users may also be added via the customer-portal invitation flow.
- **Worker roles** — assigned manually via the Admin UI (`liferay-one-custom-element` → Admin → Accounts → Assignments). `LiferayStaffUserGroupSync` assigns the `Liferay Staff` user group and SSA-ACCOUNT membership only — it does **not** auto-grant any account-level Worker roles. A Liferay employee must be explicitly assigned a Worker role on each account they need to work on.
- **Partner roles** — assigned as part of the partner activation / onboarding workflow in the frontend. When the workflow is submitted and approved, the frontend call sets `Partner_Manager` on the designated contact user for that account. No backend Object Action drives this — the frontend workflow submission is the trigger.

### 5.4 Entitlements vs roles

Per `entitlement.md`:

- **Roles** gate UI access and CRUD permissions.
- **Entitlements** gate business logic (feature availability, SLA level, support coverage tier).

A user may have `Customer_Admin` on an account that lacks the `ACTIVE_SUBSCRIPTION` entitlement — they see the pages but the pages render "subscription lapsed" empty states. Conversely, a customer may have entitlement to a premium feature but lack write role — they see, but can't edit.

---

## 6. Billing Flows

Three billing scenarios drive Commerce events, license generation, and entitlement sync. Full Commerce listener contracts in [`./integrations/commerce.md`](./integrations/commerce.md).

### Flow 1 — Renewal billing

Renewal date reached → Aggregate all products in subscription → Single renewal invoice (all products + add-ons) → Customer Pays → Subscription extended for all products

### Flow 2 — Usage-based billing

Usage-based product active in subscription → Usage events captured daily → Usage aggregated per 30-day period → Monthly consumption invoice generated → Consumption Invoice Generated → Usage reset for next period

### Flow 3 — Mid-subscription addition (pro-rata)

Customer adds product mid-subscription → System checks subscription renewal date → Calculate remaining months in period → Pro-rata invoice generated → Add-On invoice raised → Product activated via Provisioning Hub → All products renew together at renewal date

---

## Cross-references

- [`./api.md`](./api.md) — OAuth2 scopes align with role capabilities.
- [`./data-model.md`](./data-model.md) — rule syntax, cascade rules, and per-Object field/validation details.
- [`./provisioning-hub.md`](./provisioning-hub.md) — Commerce event listener contracts and license/entitlement flows.
- [`./migration.md`](./migration.md) — role seeding sequence, backfill of Partner and Liferay Staff assignments.

## Open questions

1. **Scheduler coexistence.** `liferay-one-etc-cron` (Quartz) vs Liferay's built-in scheduler (available as `SchedulerEntry` components) vs Liferay's new Object-scheduler (object-defined scheduled actions). Defaulted to `etc-cron` because it maps cleanly to existing customer-workspace patterns. Revisit if the Object-scheduler matures in time for phase 2.
2. **Workflow rigor.** The three Kaleo workflows above are minimum-viable. Support-ops may want richer escalation workflow (tiered queues, SLA timers). Defer to phase 5.
3. **Permission matrix completeness.** Every row above is baseline — phase-1 review with product will surface row-level edge cases (e.g., Customer_Admin on a parent account seeing child-account records?).
4. **Role assignment automation via Salesforce.** Mapping SF contact roles to Liferay account roles is new code in the D12 subscriber. Defined in `./integrations/salesforce-pubsub.md`.
