# Support Forms — Test Scenarios

Covers: `CallbackRequest`, `ReplacementActivationKeyRequest`, `BannedEmailDomain` check pattern applied to form submissions.

Source of truth: [`../arch/data-model.md §Ticket Management`](../arch/data-model.md), [`../arch/business-logic.md §1.5 + §1.6`](../arch/business-logic.md).

---

## TC-SFM-001: Customer submits callback request @smoke

**Actors:** `customerAdmin`
**Preconditions:** Test account exists. Slack bridge and email endpoints mocked.

### Steps

1. Log in as `customerAdmin`.
2. Navigate to `/callback-request`.
3. Fill in: name, emailAddress, phoneNumber, countryCode, description, relatedTicketIds (optional).
4. Submit.

### Assertions

- [ ] `CallbackRequest` row is created with `status` set to the initial picklist value.
- [ ] Object Action fires: Slack bridge + on-call email notification sent (intercept both).
- [ ] Confirmation message is shown on the page.

---

## TC-SFM-002: CallbackRequest blocked for banned email domain @regression

**Actors:** `guestUser` (form accessible anonymously per permission matrix)
**Preconditions:** `mailinator.com` is in `BannedEmailDomain`.

### Steps

1. Navigate to `/callback-request`.
2. Enter `emailAddress=spam@mailinator.com`.
3. Submit.

### Assertions

- [ ] Form is rejected before submission.
- [ ] Error message identifies the blocked domain.
- [ ] No `CallbackRequest` row is created.

---

## TC-SFM-003: Replacement activation key request — full flow @smoke

**Actors:** `customerAdmin`
**Preconditions:** Test account has an active Subscription and Environment. License key endpoint mocked.

### Steps

1. Navigate to `/replacement-activation-key`.
2. Fill in: companyName, contactEmailAddress, activeLiferaySubscription, clustered=false, explainReplacement, acknowledgement=true.
3. Submit.

### Assertions

- [ ] `ReplacementActivationKeyRequest` row is created with `status=Submitted` (initial state).
- [ ] Admin receives a review notification (intercept).

---

## TC-SFM-004: Replacement key request — acknowledgement required @regression

**Actors:** `customerAdmin`
**Preconditions:** None.

### Steps

1. Navigate to `/replacement-activation-key`.
2. Fill all fields except `acknowledgement`.
3. Submit.

### Assertions

- [ ] Form is rejected with a validation error on the `acknowledgement` checkbox.
- [ ] No row is created.

---

## TC-SFM-005: Admin approves replacement key — license key generated and emailed @smoke

**Actors:** `adminUser`
**Preconditions:** `ReplacementActivationKeyRequest` in `status=Submitted`.

### Steps

1. Navigate to `/admin` or the workflow task list.
2. Find the replacement key request.
3. Change `status=Issued`.
4. Save.

### Assertions

- [ ] Object Action fires: `POST /o/one/v1/license-key/generate/{subscriptionId}` is called (intercept).
- [ ] New `LicenseKey` row is created with `status=Active`.
- [ ] Email with the key artifact is sent to `contactEmailAddress` (intercept).

---

## TC-SFM-006: ReplacementActivationKeyRequest blocked for banned domain @regression

**Actors:** `customerAdmin`
**Preconditions:** `mailinator.com` in BannedEmailDomain.

### Steps

1. Submit a replacement key request with `contactEmailAddress=test@mailinator.com`.

### Assertions

- [ ] Submission is rejected.
- [ ] Error message references domain restriction.
- [ ] No row created.

---

## TC-SFM-007: Multiple banned-domain form paths all reject correctly @regression

**Actors:** `guestUser`
**Preconditions:** `throwaway.email` in BannedEmailDomain.

### Steps

Test each of the four form-submission paths:
1. `CallbackRequest` with `emailAddress=test@throwaway.email` → rejected.
2. `RequestPublisherAccount` with `emailAddress=test@throwaway.email` → rejected.
3. `SupportTicketEscalation` with `customerEmailAddress=test@throwaway.email` → rejected.
4. `ReplacementActivationKeyRequest` with `contactEmailAddress=test@throwaway.email` → rejected.

### Assertions

- [ ] All four are rejected at the `onBeforeCreate` check.
- [ ] Error message is consistent across all forms.

---

## TC-SFM-008: Callback request notification includes relatedTicketIds @regression

**Actors:** `customerAdmin`
**Preconditions:** Slack bridge and email mocked.

### Steps

1. Submit a `CallbackRequest` with `relatedTicketIds=LRHC-123,LRHC-456`.

### Assertions

- [ ] Slack notification body contains both ticket keys.
- [ ] On-call email body contains both ticket keys.
