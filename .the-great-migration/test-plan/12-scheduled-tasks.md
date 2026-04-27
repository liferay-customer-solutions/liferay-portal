# Scheduled Tasks — Test Scenarios

Covers all 11 tasks in `liferay-one-etc-cron`, concurrency (single-writer lease), failure handling, and the debug-panel manual-trigger surface.

Source of truth: [`../arch/business-logic.md §2`](../arch/business-logic.md).

---

## General

### TC-SCH-001: All scheduled tasks appear in debug panel @smoke

**Actors:** `adminUser`
**Preconditions:** `etc-cron` is running.

### Steps

1. Navigate to `/admin/debug`.

### Assertions

- [ ] All 11 tasks are listed:
  - `EntitlementSync`
  - `TrialLifecycleTick`
  - `PublisherSalesSummaryRoll`
  - `RequestProductFeedbackFan`
  - `TicketAttachmentCleanup`
  - `TicketAttachmentTrashDrain`
  - `TicketAttachmentDraftCommentRetry`
  - `BusinessEventOverdueSweep`
  - `JiraHeatTagSync`
  - `LiferayStaffUserGroupSync`
  - `ProjectsUsingMarketplaceReport`
- [ ] Each row shows: cron expression, last-run timestamp, next-run timestamp, current status (Idle / Running / Failed).

---

### TC-SCH-002: Manual trigger from debug panel executes the task @smoke

**Actors:** `adminUser`

### Steps

1. Navigate to `/admin/debug`.
2. Click **Run Now** on `EntitlementSync`.
3. Wait for completion.

### Assertions

- [ ] Task status changes to `Running` and then `Idle`.
- [ ] Last-run timestamp updates.
- [ ] Task executes the expected business logic (entitlement rows updated — see `03-entitlements.md`).

---

### TC-SCH-003: Concurrent execution — second invocation no-ops while first is running @regression

**Actors:** System
**Preconditions:** `EntitlementSync` is configured with a cluster-safe scheduler lock.

### Steps

1. Trigger `EntitlementSync` manually (it runs for > 1s on test data).
2. Immediately trigger it again from a second request.

### Assertions

- [ ] Second invocation detects the running lock and returns without executing.
- [ ] No duplicate entitlement grant/revoke operations occur.
- [ ] Log or debug panel shows the second invocation was skipped.

---

### TC-SCH-004: Task failure is logged and Slack alert sent after 3 failures @regression

**Actors:** System
**Preconditions:** Jira endpoint unavailable (simulate by removing mock).

### Steps

1. Trigger `TicketAttachmentDraftCommentRetry` 3 times with Jira endpoint returning 500.

### Assertions

- [ ] After the 3rd failure, a Slack alert is sent to the engineering channel (intercept).
- [ ] `SchedulerFailure` log entry exists for each failure.
- [ ] The task does not crash `etc-cron` (other tasks continue running).

---

## EntitlementSync (`0 */15 * * * *`)

### TC-SCH-005: EntitlementSync runs on 15-minute cadence @regression

**Actors:** System
**Preconditions:** Cron is running; time is controlled via test clock or verified empirically.

### Steps

1. Record the last-run time.
2. Wait 15 minutes.

### Assertions

- [ ] Task executes within ±30 seconds of the 15-minute mark.
- [ ] Last-run timestamp is updated.

---

### TC-SCH-006: EntitlementSync skips Inactive definitions @regression

**Actors:** System
**Preconditions:** EntitlementDefinition with `status=Inactive` exists.

### Steps

1. Trigger `EntitlementSync`.

### Assertions

- [ ] No `Entitlement` rows are created or modified for the Inactive definition.
- [ ] Active definitions are still processed.

---

## TrialLifecycleTick (`0 0 */6 * * *`)

### TC-SCH-007: TrialLifecycleTick promotes on-hold trials when seats free @regression

**Actors:** System
**Preconditions:** `TrialProvisioning` with `provisioningStatus=OnHold` exists; trial seat is now available.

### Steps

1. Free a trial seat (expire another trial or adjust seat count).
2. Trigger `TrialLifecycleTick`.

### Assertions

- [ ] On-hold trial is promoted to `Provisioning`.
- [ ] Liferay Cloud provision call is made (intercept).

---

### TC-SCH-008: TrialLifecycleTick auto-completes free pending orders @regression

**Actors:** System
**Preconditions:** `CommerceOrder` with `status=Pending` and `OrderType.provisioningFlow=free-activation`.

### Steps

1. Trigger `TrialLifecycleTick`.

### Assertions

- [ ] Free pending order is moved to `Completed`.
- [ ] LicenseKey is generated for the order's subscription (intercept license generate call).

---

## TicketAttachmentCleanup (`0 0 0,12 * * *`)

### TC-SCH-009: TicketAttachmentCleanup does not trash recent-ticket attachments @regression

**Actors:** System
**Preconditions:** `TicketAttachment` with `state=Approved`; linked Jira ticket closed 3 days ago.

### Steps

1. Trigger `TicketAttachmentCleanup`.

### Assertions

- [ ] Attachment is NOT trashed (< 7 days since ticket close).

---

### TC-SCH-010: TicketAttachmentCleanup trashes 7-day-old attachments @regression

**Actors:** System
**Preconditions:** `TicketAttachment` with `state=Approved`; Jira ticket closed 8 days ago.

### Steps

1. Trigger `TicketAttachmentCleanup`.

### Assertions

- [ ] `state` transitions to `Trashed`.

---

## PublisherSalesSummaryRoll (`0 0 2 * * *`)

### TC-SCH-011: PublisherSalesSummaryRoll is idempotent @regression

**Actors:** System
**Preconditions:** Completed orders exist for the current quarter.

### Steps

1. Trigger `PublisherSalesSummaryRoll` twice in a row.

### Assertions

- [ ] Only one `PublisherSalesSummary` row per (publisher, quarter) is created.
- [ ] Second run updates `orderCount` and `grossAmount` rather than inserting a new row.

---

## LiferayStaffUserGroupSync (`0 0 3 * * *`)

### TC-SCH-012: LiferayStaffUserGroupSync does not auto-grant account Worker roles @regression

**Actors:** System
**Preconditions:** `@liferay.com` user added to Liferay but not yet in the "Liferay Staff" group.

### Steps

1. Trigger `LiferayStaffUserGroupSync`.

### Assertions

- [ ] User is added to "Liferay Staff" user group.
- [ ] SSA-ACCOUNT membership is granted.
- [ ] No account-level `Worker_*` role is auto-assigned (must be done manually per account).

---

## ProjectsUsingMarketplaceReport (`0 0 4 * * *`)

### TC-SCH-013: ProjectsUsingMarketplaceReport does not leave partial state on failure @regression

**Actors:** System
**Preconditions:** Report generation fails halfway through (simulate via injected exception).

### Steps

1. Inject failure at 50% of order processing.
2. Trigger `ProjectsUsingMarketplaceReport`.

### Assertions

- [ ] `Report.status=Failed` (not `Ready`).
- [ ] No partial `payload` is visible to the admin UI.
- [ ] Next successful run produces a complete report.
