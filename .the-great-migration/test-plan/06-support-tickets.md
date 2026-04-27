# Support Tickets — Test Scenarios

Covers: `SupportTicket`, `TicketAttachment`, `SupportTicketEscalation`, Jira sync, GCS upload flow.

Source of truth: [`../arch/data-model.md §Ticket Management`](../arch/data-model.md), [`../arch/business-logic.md §1.5`](../arch/business-logic.md), [`../arch/integrations/jira.md`](../arch/integrations/jira.md).

---

## TC-SUP-001: Customer creates a support ticket @smoke

**Actors:** `customerAdmin`
**Preconditions:** Customer account exists. Jira REST endpoint mocked.

### Steps

1. Log in as `customerAdmin`.
2. Navigate to `/tickets/new`.
3. Fill in: subject, description, priority.
4. Submit.

### Assertions

- [ ] `SupportTicket` row is created with `jiraIssueKey` populated (e.g., `LRHC-9999`).
- [ ] The Jira create-issue API is called (intercepted mock returns `LRHC-9999`).
- [ ] New ticket appears in `/tickets` list.
- [ ] Customer receives a "ticket created" notification (intercept).

### Playwright notes

Mock: `page.route('**/o/one/v1/jira/**', ...)`. Capture the `jiraIssueKey` from the created row for use in downstream tests.

---

## TC-SUP-002: Ticket list shows only own-account tickets @smoke

**Actors:** `customerMember`
**Preconditions:** Multiple accounts with tickets exist.

### Steps

1. Log in as `customerMember`.
2. Navigate to `/tickets`.

### Assertions

- [ ] Only tickets belonging to `customerMember`'s account are shown.
- [ ] Ticket count matches `GET /o/c/supportTickets?filter=accountEntryId eq {id}`.

---

## TC-SUP-003: Ticket detail live-fetches from Jira @regression

**Actors:** `customerAdmin`
**Preconditions:** `SupportTicket` with `lastSyncedAt` > 1h ago. Jira mock returns updated status.

### Steps

1. Navigate to `/tickets/{jiraIssueKey}`.

### Assertions

- [ ] `GET /o/one/v1/jira/issue/{jiraIssueKey}` is called (intercepted).
- [ ] `statusCached` is updated with the Jira response.
- [ ] `lastSyncedAt` is refreshed to current time.
- [ ] If `lastSyncedAt` < 1h, Jira is NOT called (cached path).

---

## TC-SUP-004: Ticket detail — worker sees Jira-live status @regression

**Actors:** `workerAdmin`
**Preconditions:** Ticket with `lastSyncedAt > 1h` ago.

### Steps

1. Navigate to `/tickets/{jiraIssueKey}` as `workerAdmin`.

### Assertions

- [ ] Same Jira live-fetch happens as for the customer.
- [ ] Worker sees `assigneeEmail` displayed.
- [ ] Worker can see internal Jira fields that the customer view hides (if any).

---

## TC-SUP-005: Large-file upload — full GCS resumable flow @smoke

**Actors:** `customerAdmin`
**Preconditions:** `SupportTicket` exists. GCS endpoint mocked.

### Steps

1. Navigate to `/large-file-uploader`.
2. Select the ticket.
3. Select a file (e.g., 50 MB log).
4. Click **Upload**.

### Assertions

- [ ] `POST /o/one/v1/ticket-attachments/initiate-upload` is called; `TicketAttachment` created with `state=Draft`.
- [ ] Browser initiates upload to the GCS resumable URL (intercepted).
- [ ] After upload, `POST /o/one/v1/ticket-attachments/{id}/complete-upload` is called.
- [ ] MD5 is validated; `state` transitions to `Approved`.
- [ ] Object Action fires: Jira ADF comment posted (intercept) with a signed download link.
- [ ] Attachment appears in the ticket's attachments list.

---

## TC-SUP-006: TicketAttachment MD5 dedup — duplicate rejected @regression

**Actors:** `customerAdmin`
**Preconditions:** `TicketAttachment` with `state=Approved` exists for `(fileName=log.txt, ticketId=X, md5=ABC)`.

### Steps

1. Upload the same file again (same name + md5).

### Assertions

- [ ] On `complete-upload`, the second attachment is rejected.
- [ ] Error references the MD5 dedup constraint.
- [ ] No duplicate row in `state=Approved`.

---

## TC-SUP-007: Attachment download returns signed GCS URL @smoke

**Actors:** `customerAdmin`
**Preconditions:** `TicketAttachment` with `state=Approved` exists.

### Steps

1. Navigate to the ticket detail page.
2. Click the download icon next to the attachment.

### Assertions

- [ ] `GET /o/one/v1/ticket-attachments/by-id/{id}/download` is called.
- [ ] A signed URL is returned; browser initiates a download.
- [ ] URL expires after 15 minutes (verify expiry header or parameter).

---

## TC-SUP-008: Attachment Jira comment retry — draftCommentBody path @regression

**Actors:** System (`TicketAttachmentDraftCommentRetry` task)
**Preconditions:** `TicketAttachment` with `state=Approved` and non-null `draftCommentBody` (previous Jira call failed).

### Steps

1. Trigger `TicketAttachmentDraftCommentRetry` via debug panel.

### Assertions

- [ ] Jira comment POST is retried (intercept).
- [ ] On success, `draftCommentBody` is cleared.
- [ ] On failure, `draftCommentBody` remains set for the next retry.

---

## TC-SUP-009: Trash attachment — GCS object deleted by drain task @regression

**Actors:** `customerAdmin`
**Preconditions:** `TicketAttachment` with `state=Approved` exists.

### Steps

1. Navigate to the ticket's attachment list.
2. Click **Delete** on the attachment.
3. Wait for `TicketAttachmentTrashDrain` to run (or trigger via debug panel).

### Assertions

- [ ] `TicketAttachment.state` transitions to `Trashed` immediately on UI action.
- [ ] After drain task runs, GCS object is deleted (intercept `DELETE https://storage.googleapis.com/...`).
- [ ] `TicketAttachment` row is removed or marked deleted.

---

## TC-SUP-010: TicketAttachmentCleanup trashes attachments after ticket close @regression

**Actors:** System (`TicketAttachmentCleanup` task)
**Preconditions:** Jira ticket was closed 7 days ago; `TicketAttachment` still in `state=Approved`.

### Steps

1. Trigger `TicketAttachmentCleanup` via debug panel.

### Assertions

- [ ] Jira is queried for ticket status (intercept; returns `status=Done`).
- [ ] `TicketAttachment.state` transitions to `Trashed`.
- [ ] Attachment is NOT trashed if the ticket closed < 7 days ago.

---

## TC-SUP-011: Customer submits escalation — workflow started @smoke

**Actors:** `customerAdmin`
**Preconditions:** Test account has at least one ticket.

### Steps

1. Navigate to `/support-ticket-escalation`.
2. Fill in: description, customerEmail, phoneNumber, ticketIds (one or more Jira keys).
3. Submit.

### Assertions

- [ ] `SupportTicketEscalation` row is created with `status=Submitted`.
- [ ] Kaleo `support-ticket-escalation-review` workflow is started.
- [ ] Admin/support team receives a review notification (intercept).

---

## TC-SUP-012: Escalation blocked for banned email @regression

**Actors:** `guestUser`
**Preconditions:** `mailinator.com` in BannedEmailDomain.

### Steps

1. Submit an escalation with `customerEmailAddress=test@mailinator.com`.

### Assertions

- [ ] Submission is rejected with domain-blocked error.
- [ ] No `SupportTicketEscalation` row created.

---

## TC-SUP-013: Security vulnerabilities proxy page @regression

**Actors:** `customerAdmin` with access to security project
**Preconditions:** User's account has entitlement to security project. Jira LSV project mocked.

### Steps

1. Navigate to `/security-vulnerabilities`.

### Assertions

- [ ] Page renders a list of security vulnerabilities proxied from Jira LSV project.
- [ ] `GET /o/one/v1/jira/security-vulnerabilities/*` is called (intercept).
- [ ] A user without the entitlement sees an access-denied state.

---

## TC-SUP-014: Admin flushes Jira cache @regression

**Actors:** `adminUser`
**Preconditions:** Jira cache is populated.

### Steps

1. Navigate to `/admin/debug`.
2. Click **Clear Jira Cache**.

### Assertions

- [ ] `DELETE /o/one/v1/jira/cache` is called.
- [ ] Next ticket-detail load triggers a fresh Jira fetch.
