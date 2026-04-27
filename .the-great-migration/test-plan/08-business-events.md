# Business Events — Test Scenarios

Covers: `BusinessEvent`, `BusinessEventVersion`, `JiraHeatTagSync`, `BusinessEventOverdueSweep`.

Source of truth: [`../arch/data-model.md §BusinessEvent`](../arch/data-model.md), [`../arch/business-logic.md §1.5`](../arch/business-logic.md).

---

## TC-BEV-001: Worker creates a business event @smoke

**Actors:** `workerAdmin` on the test account
**Preconditions:** Test account and an active Environment exist.

### Steps

1. Navigate to `/admin/business-events` → **New Event**.
2. Fill in: accountEntryId (test account), deploymentId, eventType, eventStatus = `Planned`, description, expectedGoLiveDateTime = 30 days from now.
3. Set feature flags: `dxpEnabled=true`, `commerceEnabled=false`.
4. Save.

### Assertions

- [ ] `BusinessEvent` row is created.
- [ ] A `BusinessEventVersion` row is auto-created recording the initial state.
- [ ] Jira heat tag update is called for `associatedTicketIds` (intercept).

---

## TC-BEV-002: BusinessEventVersion immutability @regression

**Actors:** `adminUser`
**Preconditions:** `BusinessEventVersion` row exists.

### Steps

1. Attempt `PATCH /o/c/businessEventVersions/{id}` with a changed `comment`.

### Assertions

- [ ] Request is rejected.
- [ ] Error references the immutable-after-insert constraint.

---

## TC-BEV-003: Update business event creates new version @smoke

**Actors:** `workerAdmin`
**Preconditions:** `BusinessEvent` exists with 1 version.

### Steps

1. Open the business event.
2. Change `eventStatus` to `InProgress`.
3. Update `description`.
4. Save.

### Assertions

- [ ] A second `BusinessEventVersion` row is created with the diff of the changes.
- [ ] `changedAt` matches the save time.
- [ ] `changedByUserId` matches the `workerAdmin` user.

---

## TC-BEV-004: actualGoLiveDateTime only allowed when status=Completed @regression

**Actors:** `workerAdmin`
**Preconditions:** `BusinessEvent` with `eventStatus=InProgress`.

### Steps

1. Attempt to set `actualGoLiveDateTime` while `eventStatus=InProgress`.
2. Save.

### Assertions

- [ ] Save is rejected.
- [ ] Error message references the `Completed` status requirement.

---

## TC-BEV-005: BusinessEventOverdueSweep marks past-due events overdue @smoke

**Actors:** System (`BusinessEventOverdueSweep` task)
**Preconditions:** `BusinessEvent` with `eventStatus=Planned` and `expectedGoLiveDateTime` = yesterday.

### Steps

1. Trigger `BusinessEventOverdueSweep` via debug panel.

### Assertions

- [ ] `BusinessEvent.eventStatus` transitions to `Overdue`.
- [ ] A `BusinessEventVersion` is created recording the status change.
- [ ] Primary contact + Liferay owner receive overdue notification emails (intercept).
- [ ] Slack notification is sent to support channel (intercept).

---

## TC-BEV-006: JiraHeatTagSync pushes heat labels to Jira tickets @regression

**Actors:** System (`JiraHeatTagSync` task)
**Preconditions:** `BusinessEvent` with `heat=High` and `associatedTicketIds=LRHC-100`. Jira mocked.

### Steps

1. Trigger `JiraHeatTagSync` via debug panel.

### Assertions

- [ ] Jira label update is called for `LRHC-100` with label `high_be` (intercept).
- [ ] `impacting_business_event` label is also applied.
- [ ] Jira Assets Koroneiki object is updated (intercept second Jira call).

---

## TC-BEV-007: Customer reads business event (read-only) @regression

**Actors:** `customerAdmin`
**Preconditions:** Business event exists for the customer's account.

### Steps

1. Navigate to `/business-events`.

### Assertions

- [ ] Event is listed.
- [ ] All feature-flag fields are readable.
- [ ] No **Edit** or **New** actions are available (customer role is read-only on BusinessEvent).

---

## TC-BEV-008: Business event list — filter by status and account @regression

**Actors:** `liferayStaff`
**Preconditions:** Events with mixed statuses across accounts exist.

### Steps

1. Navigate to `/admin/business-events`.
2. Filter by `eventStatus=AtRisk`.
3. Filter by test account.

### Assertions

- [ ] Only events matching both filters are displayed.
- [ ] Clearing filters restores full list.

---

## TC-BEV-009: Business event version history is visible @smoke

**Actors:** `workerMember`
**Preconditions:** Business event with 3 `BusinessEventVersion` entries.

### Steps

1. Navigate to the business event detail.
2. Open the **History** tab.

### Assertions

- [ ] All 3 version entries are listed in reverse chronological order.
- [ ] Each row shows: `changedAt`, `changedByUserId`, `change`, `comment`.
- [ ] No edit action is available on version rows.

---

## TC-BEV-010: Overdue notification not resent for already-overdue events @regression

**Actors:** System
**Preconditions:** `BusinessEvent` already in `eventStatus=Overdue`.

### Steps

1. Trigger `BusinessEventOverdueSweep`.

### Assertions

- [ ] Already-overdue events are NOT re-processed.
- [ ] No duplicate notification is sent.
- [ ] Sweep only touches events in `Planned`, `InProgress`, `AtRisk` states.
