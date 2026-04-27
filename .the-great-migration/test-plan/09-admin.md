# Admin UI — Test Scenarios

Covers: all `/admin/*` pages, the `liferay-one-custom-element` Admin feature module, EntitlementDefinition editor, scheduled-task dashboard, debug panel, reports.

Source of truth: [`../arch/ui.md §Admin`](../arch/ui.md), [`../arch/business-logic.md §5`](../arch/business-logic.md).

---

## TC-ADM-001: Admin home loads for Liferay Staff user @smoke

**Actors:** `liferayStaff`
**Preconditions:** User has `Liferay Staff` user group membership.

### Steps

1. Log in as `liferayStaff`.
2. Navigate to `/admin`.

### Assertions

- [ ] Dashboard renders without errors.
- [ ] Side navigation shows: Accounts, Contacts, Teams, Subscriptions, Deployments, Products, Entitlements, Entitlement Definitions, License Keys, Publishers, Business Events, Reports, External Links, Debug.
- [ ] Guest user navigating to `/admin` receives a 403 or is redirected to login.

---

## TC-ADM-002: Admin accounts list — hierarchical tree view @regression

**Actors:** `adminUser`
**Preconditions:** Parent account with 2 child accounts exists.

### Steps

1. Navigate to `/admin/accounts`.
2. Expand the parent account node.

### Assertions

- [ ] Child accounts appear nested under the parent.
- [ ] Account tree supports at least 2 levels of nesting.
- [ ] Collapsing the parent hides child accounts.

---

## TC-ADM-003: Admin account detail — full field display @smoke

**Actors:** `liferayStaff`
**Preconditions:** Account with all fields populated (region, tier, status, salesforceId, etc.).

### Steps

1. Navigate to `/admin/accounts/{accountId}`.

### Assertions

- [ ] All custom fields are displayed: `koroneikiAccountCode`, `region`, `tier`, `status`, `internal`, `profileEmailAddress`, `salesforceId`.
- [ ] Related Flags, Notes, Teams, Deployments, Subscriptions tabs are visible.
- [ ] ExternalLink entries are shown on a Links tab.

---

## TC-ADM-004: Admin contacts list — user + account role @regression

**Actors:** `adminUser`
**Preconditions:** Users with account memberships exist.

### Steps

1. Navigate to `/admin/contacts`.
2. Search for a user by email.

### Assertions

- [ ] User is found.
- [ ] Account membership and account-level role are shown.
- [ ] Clicking through opens the user's detail with their account assignments.

---

## TC-ADM-005: Admin entitlement-definition editor — rule JSON validated in UI @smoke

**Actors:** `adminUser`
**Preconditions:** None.

### Steps

1. Navigate to `/admin/entitlement-definitions` → **New**.
2. Select `ruleType=filter`.
3. Enter an invalid `ruleBody` JSON (missing `filter` key).
4. Click **Save**.

### Assertions

- [ ] The UI shows inline JSON validation error before saving (client-side).
- [ ] Save is blocked until the JSON is valid.

---

## TC-ADM-006: Admin entitlement-definition editor — scripted rule @smoke

**Actors:** `adminUser`
**Preconditions:** None.

### Steps

1. Create an EntitlementDefinition with `ruleType=scripted`.
2. Set `ruleBody.function=cloudNativeWithoutSaaS`.
3. Save.

### Assertions

- [ ] Definition saved successfully.
- [ ] The function name is shown in the definition detail as a read-only code label.
- [ ] Attempting an unknown `function` value is rejected.

---

## TC-ADM-007: Scheduled-task dashboard shows all tasks with status @smoke

**Actors:** `adminUser`
**Preconditions:** `etc-cron` is running.

### Steps

1. Navigate to `/admin/debug`.

### Assertions

- [ ] All 11 scheduled tasks appear in the list (from `../arch/business-logic.md §2`).
- [ ] Each row shows: task name, cron expression, last-run time, next-run time, status (Running / Idle / Failed).
- [ ] **Run Now** button is available for each task.

---

## TC-ADM-008: Debug panel — manual entitlement recompute @smoke

**Actors:** `adminUser`
**Preconditions:** Test account exists.

### Steps

1. Navigate to `/admin/debug`.
2. Click **Recompute Entitlements**.
3. Select scope = `All accounts` (or a specific account).
4. Confirm.

### Assertions

- [ ] `POST /o/one/v1/entitlements/recompute` is called with appropriate params (intercept).
- [ ] UI shows a success or in-progress indicator.
- [ ] After completion, entitlement rows are updated.

---

## TC-ADM-009: Debug panel — Jira cache flush @regression

**Actors:** `adminUser`
**Preconditions:** Jira cache has entries.

### Steps

1. Navigate to `/admin/debug`.
2. Click **Clear Jira Cache**.

### Assertions

- [ ] `DELETE /o/one/v1/jira/cache` is called (intercept).
- [ ] Success confirmation shown.

---

## TC-ADM-010: Admin license-keys — generate from UI @smoke

**Actors:** `adminUser`
**Preconditions:** Active Subscription exists.

### Steps

1. Navigate to `/admin/license-keys`.
2. Click **Generate Key**.
3. Select subscription and fill parameters.
4. Submit.

### Assertions

- [ ] New LicenseKey row appears in the list with `status=Active`.
- [ ] Row shows: `key` (truncated), `licenseType`, `issuedAt`, `expiresAt`, `status`.

---

## TC-ADM-011: Admin license-keys — non-admin cannot see generate/revoke @regression

**Actors:** `workerManager`
**Preconditions:** LicenseKey list has entries.

### Steps

1. Log in as `workerManager`.
2. Navigate to `/admin/license-keys`.

### Assertions

- [ ] Keys are visible (read permission).
- [ ] **Generate** and **Revoke** buttons are absent or disabled.
- [ ] API call `POST /o/one/v1/license-key/generate/*` returns `403` for `workerManager`.

---

## TC-ADM-012: Admin products (CPDefinition) browser @regression

**Actors:** `liferayStaff`
**Preconditions:** Commerce products exist with `isPrimary` and `licenseKeyProductVersion` fields.

### Steps

1. Navigate to `/admin/products`.
2. Search for a product by name.

### Assertions

- [ ] Product rows show: name, `productFamily`, `isPrimary`, `licenseKeyProductVersion`.
- [ ] Clicking a product opens Commerce's product-admin detail (or an embedded detail view).

---

## TC-ADM-013: Admin reports — marketplace report visible @regression

**Actors:** `adminUser`
**Preconditions:** `Report` row with `reportType=projects_using_marketplace` and `status=Ready` exists.

### Steps

1. Navigate to `/admin/reports`.

### Assertions

- [ ] Report is listed with `generatedAt`, `status=Ready`.
- [ ] Clicking the report shows the `payload` rendered as a table or formatted view.
- [ ] A `status=Generating` report shows a loading indicator.
- [ ] A `status=Failed` report shows an error badge.

---

## TC-ADM-014: Admin external-links browser @regression

**Actors:** `adminUser`
**Preconditions:** ExternalLink rows exist with various `domain` values.

### Steps

1. Navigate to `/admin/external-links`.
2. Filter by `domain=salesforce`.

### Assertions

- [ ] Only Salesforce-domain links are shown.
- [ ] Each row shows: domain, entityId, ownerClassName, linked record name.

---

## TC-ADM-015: Non-admin staff cannot access entitlement-definitions editor @regression

**Actors:** `liferayStaff` (not `Administrator`)
**Preconditions:** None.

### Steps

1. Log in as `liferayStaff`.
2. Navigate to `/admin/entitlement-definitions`.

### Assertions

- [ ] Access is denied (403 or redirect).
- [ ] The page is accessible when logged in as `Administrator`.
