# Subscriptions & Environments — Test Scenarios

Covers: `Subscription`, `SubscriptionItem`, `Environment` (Deployment), `LicenseKey` (Activation - File Key), `OrderType`, `TrialProvisioning`, Commerce `CommerceOrder` / `CommerceOrderItem`.

Source of truth: [`../arch/data-model.md §Subscription Management`](../arch/data-model.md), [`../arch/business-logic.md §1.2`](../arch/business-logic.md), [`../arch/provisioning-hub.md`](../arch/provisioning-hub.md).

---

## TC-SUB-001: Create environment for an account @smoke

**Actors:** `workerAdmin` on the test account
**Preconditions:** Test account exists with at least one active Subscription.

### Steps

1. Navigate to `/admin/deployments` → **New Environment**.
2. Select the test account.
3. Fill in: Name = `Production`, Type = `on_prem`, InstanceSize = `M`, startDate = today, Status = `Active`.
4. Set `activationMode=file_key`.
5. Save.

### Assertions

- [ ] Environment appears in the deployments list.
- [ ] `deploymentKey` is auto-generated (uppercase).
- [ ] Environment detail page shows the correct account and subscription linkage.

---

## TC-SUB-002: Environment date validation @regression

**Actors:** `workerAdmin`
**Preconditions:** Test account exists.

### Steps

1. Create an Environment with `startDate` = tomorrow and `endDate` = today.
2. Submit.

### Assertions

- [ ] Save is rejected.
- [ ] Error message references the `startDate ≤ endDate` constraint.

---

## TC-SUB-003: Entitlements re-sync triggered on environment create @regression

**Actors:** `adminUser`
**Preconditions:** Test account has EntitlementDefinitions active.

### Steps

1. Create a new Environment for the test account via the Admin UI.
2. Wait for the Object Action to complete (or confirm via `/entitlements/recompute` log).

### Assertions

- [ ] `POST /o/one/v1/entitlements/recompute?accountEntryId={id}` is called (intercept in Playwright).
- [ ] After recompute, entitlement rows for the account reflect the new environment.

---

## TC-SUB-004: Generate license key for active subscription @smoke

**Actors:** `adminUser`
**Preconditions:** Active Subscription + Environment exist for the test account.

### Steps

1. Navigate to `/admin/license-keys` → **Generate Key**.
2. Select the test Subscription.
3. Fill: `licenseType=Production`, `maxServers=2`, `maxDevelopers=10`.
4. Submit.

### Assertions

- [ ] LicenseKey row is created with `status=Active`.
- [ ] `key` field is populated (non-empty string).
- [ ] Key appears on the Environment's Keys tab.

---

## TC-SUB-005: LicenseKey uniqueness — duplicate key rejected @regression

**Actors:** `adminUser`
**Preconditions:** LicenseKey with `key=ABC123` exists.

### Steps

1. Attempt to create a second LicenseKey with the same `key=ABC123` via the API: `POST /o/c/licenseKeys`.

### Assertions

- [ ] Response is `409 Conflict` or `400 Bad Request`.
- [ ] No duplicate LicenseKey row is created.

---

## TC-SUB-006: LicenseKey status — cannot reactivate from terminal state @regression

**Actors:** `adminUser`
**Preconditions:** LicenseKey with `status=Revoked` exists.

### Steps

1. Attempt to update the LicenseKey to `status=Active` via `PATCH /o/c/licenseKeys/{id}`.

### Assertions

- [ ] Request is rejected.
- [ ] Key remains `status=Revoked`.
- [ ] Error message references the terminal-state constraint.

---

## TC-SUB-007: Revoke license key from Admin UI @smoke

**Actors:** `adminUser`
**Preconditions:** LicenseKey with `status=Active` exists.

### Steps

1. Navigate to `/admin/license-keys`.
2. Find the active key.
3. Click **Revoke**.
4. Confirm the revocation dialog.

### Assertions

- [ ] Key `status` changes to `Revoked`.
- [ ] A notification is sent to the account owner (intercept outbound email or notification).
- [ ] The revoked key cannot be downloaded from the key download endpoint.

---

## TC-SUB-008: Download license key artifact @smoke

**Actors:** `customerAdmin` on the test account
**Preconditions:** Active LicenseKey exists for the account.

### Steps

1. Navigate to `/projects/{deploymentKey}` → **License Keys** tab.
2. Click the download icon on an active key.

### Assertions

- [ ] Browser triggers a file download.
- [ ] Downloaded file is a valid DXP license format (XML).
- [ ] File content matches the key's `payload` field.

### Playwright notes

Use `page.waitForEvent('download')` to intercept the file. Assert `download.suggestedFilename()` contains the key identifier.

---

## TC-SUB-009: Commerce subscription lifecycle event generates license key @regression

**Actors:** System (Commerce event)
**Preconditions:** Test Subscription + Environment exist but no LicenseKey yet. Subscription item has `activationMode=file_key`.

### Steps

1. Transition the `CommerceSubscriptionEntry` status to `Active` via `PATCH /o/commerceSubscriptionEntries/{id}` with `subscriptionStatus=active`.
2. Wait for the Commerce event listener in `etc-spring-boot` to process.

### Assertions

- [ ] A `LicenseKey` row is created with `status=Active` linked to the subscription.
- [ ] Account owner receives a notification (intercept notification service call).
- [ ] Entitlement sync is triggered for the account.

---

## TC-SUB-010: Commerce subscription expiry revokes license key @regression

**Actors:** System
**Preconditions:** Active Subscription with an Active LicenseKey.

### Steps

1. Transition `CommerceSubscriptionEntry` to `status=expired`.
2. Wait for event listener.

### Assertions

- [ ] LicenseKey `status` changes to `Revoked`.
- [ ] Entitlement sync is triggered.

---

## TC-SUB-011: TrialProvisioning — trial-cloud flow triggers cloud provisioning @smoke

**Actors:** `customerAdmin` completing a Marketplace checkout
**Preconditions:** OrderType with `provisioningFlow=trial-cloud` exists. Liferay Cloud mock is configured.

### Steps

1. Complete a purchase for the trial-cloud OrderType in Marketplace.
2. Wait for the `TrialProvisioning.onAfterAdd` Object Action to fire.

### Assertions

- [ ] `TrialProvisioning` row is created with `provisioningStatus=Provisioning`.
- [ ] `POST /o/one/v1/trial/provision/{subscriptionId}` is called (intercept).
- [ ] After mock Liferay Cloud responds, `provisioningStatus` transitions to `Provisioned`.
- [ ] `trialEndDate` is set to today + `OrderType.trialDurationDays`.

### Playwright notes

Mock the Liferay Cloud endpoint with `page.route('**/o/one/v1/trial/provision/**', ...)`. Assert the TrialProvisioning row via headless API.

---

## TC-SUB-012: TrialLifecycleTick — expire past-end-date trial @regression

**Actors:** System (`TrialLifecycleTick` scheduled task)
**Preconditions:** `TrialProvisioning` row with `provisioningStatus=Provisioned` and `trialEndDate` in the past.

### Steps

1. Trigger `TrialLifecycleTick` manually via `/admin/debug` → Run Task.

### Assertions

- [ ] `TrialProvisioning.provisioningStatus` transitions to `Expired`.
- [ ] `POST /o/one/v1/trial/expire/{subscriptionId}` is called (intercept).
- [ ] Notification email is sent to the customer (intercept).

---

## TC-SUB-013: Trial availability check — seat limit respected @regression

**Actors:** `guestUser` (or `customerAdmin`) browsing Marketplace
**Preconditions:** `OrderType` with limited trial seats, all seats currently taken.

### Steps

1. Navigate to the Marketplace app page for the trial OrderType.
2. Observe the trial CTA.

### Assertions

- [ ] `GET /o/one/v1/trial/availability?orderTypeExternalReferenceCode={erc}` returns `available=false`.
- [ ] UI shows a "Trial not available" or waitlist message.
- [ ] The **Start Trial** button is disabled.

---

## TC-SUB-014: Regenerate license key supersedes original @regression

**Actors:** `adminUser`
**Preconditions:** Active LicenseKey exists.

### Steps

1. Navigate to `/admin/license-keys`.
2. Click **Regenerate** on an active key.
3. Confirm.

### Assertions

- [ ] A new LicenseKey is created with `status=Active` and the same `maxServers`, `maxDevelopers`.
- [ ] The original key `status` changes to `Superseded`.
- [ ] `POST /o/one/v1/license-key/regenerate/{id}` is called.

---

## TC-SUB-015: Admin subscriptions browser — filter by account and status @regression

**Actors:** `liferayStaff`
**Preconditions:** Multiple subscriptions exist across accounts.

### Steps

1. Navigate to `/admin/subscriptions`.
2. Filter by test account.
3. Filter by `status=Active`.

### Assertions

- [ ] Only active subscriptions for the selected account are shown.
- [ ] Each row shows: account name, subscription dates, `billingCadence`, linked environment count.

---

## TC-SUB-016: Customer sees own environments on portal @smoke

**Actors:** `customerMember`
**Preconditions:** Test account has 2 active Environments.

### Steps

1. Log in as `customerMember`.
2. Navigate to `/projects`.

### Assertions

- [ ] Both environments for the account are listed.
- [ ] Environments for other accounts are not visible.
- [ ] Each card shows: `name`, `instanceSize`, `status`, `startDate`.

---

## TC-SUB-017: Customer navigates to environment detail @smoke

**Actors:** `customerAdmin`
**Preconditions:** Environment with active LicenseKey exists.

### Steps

1. Navigate to `/projects`.
2. Click an environment card.

### Assertions

- [ ] Environment detail page loads at `/projects/{deploymentKey}`.
- [ ] License keys tab shows active keys with a download link.
- [ ] The **Generate Key** button is absent (customer role cannot generate).
