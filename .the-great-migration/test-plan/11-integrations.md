# Integrations — Test Scenarios

Tests for all external integration boundaries. Most scenarios in this file are tagged `@integration` and run against mocked endpoints by default; scenarios tagged `@live` require real external credentials and run in the nightly pipeline only.

Source of truth: [`../arch/integrations/`](../arch/integrations/), [`../arch/provisioning-hub.md`](../arch/provisioning-hub.md).

---

## Salesforce Pub/Sub Subscriber

### TC-INT-001: Salesforce close-won event upserts AccountEntry and creates Commerce order @smoke

**Actors:** System (Salesforce Pub/Sub subscriber in `etc-spring-boot`)
**Preconditions:** `etc-spring-boot` is running. Pub/Sub subscriber is configured with a test topic. Salesforce event mock is available.

### Steps

1. Publish a synthetic Salesforce close-won opportunity event to the test Pub/Sub topic with:
   - `accountId=SF-ACCT-001`, `opportunityId=SF-OPP-001`
   - Contact with role = `Decision Maker`
   - Products: 1 DXP Gold subscription SKU
2. Wait for subscriber to process.

### Assertions

- [ ] `AccountEntry` is created (or updated if existing) with `salesforceId=SF-ACCT-001`.
- [ ] `koroneikiAccountCode` is generated (non-empty, uppercase).
- [ ] `ExternalLink` row is created with `domain=salesforce`, `entityId=SF-OPP-001`.
- [ ] `CommerceOrder` is created with `CommerceOrderItem` for the DXP Gold SKU.
- [ ] `CommerceSubscriptionEntry` is created for the subscription-enabled item.
- [ ] Contact is reconciled to a Liferay `User` and assigned `Customer_Admin` role on the account.

---

### TC-INT-002: Salesforce subscriber — new-biz vs renewal distinction @regression

**Actors:** System
**Preconditions:** AccountEntry with `salesforceId=SF-ACCT-002` already exists.

### Steps

1. Publish a close-won event for `SF-ACCT-002` (renewal opportunity type).

### Assertions

- [ ] Existing `AccountEntry` is updated (not a new one created).
- [ ] New `CommerceOrder` is added to the existing account.
- [ ] A warning flag is logged if `opportunityType=New Business` but account already exists (expected mismatch behavior).

---

### TC-INT-003: Salesforce subscriber — developer count enforced @regression

**Actors:** System
**Preconditions:** Subscription includes a `developerCount=5` constraint.

### Steps

1. Publish a close-won event with `developerCount=5`.

### Assertions

- [ ] `Subscription.developerCount=5` is set on the created subscription.

---

## Jira

### TC-INT-004: SupportTicket create — Jira issue is created @smoke @integration

**Actors:** `customerAdmin`
**Preconditions:** Jira project `LRHC` is configured. Using live Jira (nightly) or mocked Jira (default).

### Steps

1. Create a `SupportTicket` via the portal.

### Assertions

- [ ] Jira issue `LRHC-XXXX` is created.
- [ ] `SupportTicket.jiraIssueKey` matches the created Jira issue key.
- [ ] Jira issue `summary` matches the `SupportTicket.subject`.

---

### TC-INT-005: TicketAttachment Jira comment posted with ADF and signed URL @smoke @integration

**Actors:** `customerAdmin`
**Preconditions:** SupportTicket with jiraIssueKey exists. Attachment uploaded and approved.

### Steps

1. Complete the GCS upload + approve the attachment.

### Assertions

- [ ] Jira `POST /rest/api/3/issue/{key}/comment` is called (intercept).
- [ ] Comment body is ADF format (JSON) containing the download URL.
- [ ] Signed URL in the comment is valid and expires in 15 minutes.

---

### TC-INT-006: Jira sync — cache miss refreshes status @regression

**Actors:** `customerAdmin`
**Preconditions:** `SupportTicket.lastSyncedAt` > 1h ago.

### Steps

1. Visit ticket detail.

### Assertions

- [ ] Fresh Jira status is fetched and `statusCached` updated.
- [ ] `lastSyncedAt` is updated.
- [ ] Second immediate visit does NOT re-fetch (cache hit).

---

### TC-INT-007: JiraHeatTagSync — heat labels applied correctly per heat level @regression

**Actors:** System
**Preconditions:** BusinessEvents with `heat=Critical`, `heat=High`, `heat=Medium` exist.

### Steps

1. Trigger `JiraHeatTagSync`.

### Assertions

- [ ] `critical_be` label applied for Critical heat events.
- [ ] `high_be` label applied for High heat events.
- [ ] `medium_be` label applied for Medium heat events.
- [ ] `impacting_business_event` label applied to all.

---

## Google Cloud Storage

### TC-INT-008: GCS resumable upload — full flow completes @smoke @integration

**Actors:** `customerAdmin`
**Preconditions:** GCS bucket is configured. Using mocked GCS by default.

### Steps

1. Initiate upload → get resumable URL.
2. Upload file to GCS resumable URL (mocked response).
3. Call complete-upload.

### Assertions

- [ ] MD5 matches the uploaded content.
- [ ] `TicketAttachment.gcsBucket` and `gcsObject` are set.
- [ ] `state=Approved`.

---

### TC-INT-009: GCS signed download URL expires @regression

**Actors:** `customerAdmin`
**Preconditions:** TicketAttachment with `state=Approved` exists.

### Steps

1. Request download URL.
2. Record the URL and its expiry.
3. Attempt to fetch the URL after expiry (simulate or check the signed URL parameters).

### Assertions

- [ ] URL includes a 15-minute expiry parameter.
- [ ] After expiry, URL returns 403 (GCS signed URL behavior, tested in `@integration` mode only).

---

### TC-INT-010: GCS object deleted when attachment trashed @regression

**Actors:** System (`TicketAttachmentTrashDrain`)
**Preconditions:** Attachment `state=Trashed`. GCS mocked.

### Steps

1. Trigger `TicketAttachmentTrashDrain`.

### Assertions

- [ ] GCS `DELETE` call is made for the object (intercept).
- [ ] `TicketAttachment` row is removed or marked as deleted.

---

## Liferay Cloud (Trial Provisioning)

### TC-INT-011: Trial provision calls Liferay Cloud API @smoke

**Actors:** System
**Preconditions:** `TrialProvisioning` created for a `trial-cloud` OrderType. Liferay Cloud endpoint mocked.

### Steps

1. `POST /o/one/v1/trial/provision/{subscriptionId}`.

### Assertions

- [ ] Liferay Cloud API is called (intercept).
- [ ] `TrialProvisioning.cloudInstanceId` is populated from the response.
- [ ] `provisioningStatus=Provisioned`.

---

### TC-INT-012: Trial expire calls Liferay Cloud decomm @regression

**Actors:** System
**Preconditions:** `TrialProvisioning` with `provisioningStatus=Provisioned`.

### Steps

1. `POST /o/one/v1/trial/expire/{subscriptionId}`.

### Assertions

- [ ] Liferay Cloud decomm API is called (intercept).
- [ ] `provisioningStatus=Expired`.

---

## NAV / Microsoft Dynamics

### TC-INT-013: CreditHold auto-opened on NAV aged-AR event @regression

**Actors:** System (NAV Pub/Sub event)
**Preconditions:** Account exists with no credit hold.

### Steps

1. Publish a synthetic `nav.account.aged` Pub/Sub event for the test account.

### Assertions

- [ ] `CreditHold` row is created with `reason=aging`, `openedAt=now`.
- [ ] `Account.creditStatus` is updated.
- [ ] Downstream: subscription creation attempt for this account is blocked (see TC-ACC-016).

---

## Analytics Cloud / Faro

### TC-INT-014: Analytics workspace provisioned on trial-cloud order @regression

**Actors:** System
**Preconditions:** OrderType has `provisionsAnalytics=true`. Analytics Cloud endpoint mocked.

### Steps

1. Complete a trial-cloud purchase for this OrderType.

### Assertions

- [ ] `POST /o/one/v1/analytics/provision/{subscriptionId}` is called (intercept).
- [ ] Analytics workspace is created in the mock response.

---

## Marketo

### TC-INT-015: Marketo form submission does not send PII to other systems @regression

**Actors:** `guestUser`
**Preconditions:** Marketo form 3738 on the Contact Sales page.

### Steps

1. Submit the contact sales form.

### Assertions

- [ ] Only the Marketo API is called (intercept — no other outbound calls made with form data).
- [ ] No form data is stored in a Liferay Object.

---

## Email / SMTP

### TC-INT-016: Notification templates send correctly formatted emails @regression

**Actors:** System (triggered by Object Actions)
**Preconditions:** SMTP is configured (use Mailhog or similar in test). Email templates exist in site-initializer.

### Steps

1. Create a `SupportTicket` (triggers "ticket created" template).
2. Approve a `Publisher` (triggers "publisher approved" template).
3. Expire a trial (triggers trial-end template).

### Assertions

- [ ] Each email arrives in the test SMTP inbox.
- [ ] Subject line matches the notification template.
- [ ] Recipient matches the expected field (e.g., `profileEmailAddress` for account, publisher's `emailAddress`).
- [ ] No template variable placeholders remain unexpanded (e.g., no `${accountName}` in the sent email).
