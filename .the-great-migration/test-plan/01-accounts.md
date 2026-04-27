# Accounts — Test Scenarios

Covers: `AccountEntry`, `AccountFlag`, `AccountNote`, `Team`, `ExternalLink`, `BannedEmailDomain`.

Source of truth: [`../arch/data-model.md §Account Management`](../arch/data-model.md), [`../arch/business-logic.md §1.1`](../arch/business-logic.md).

---

## TC-ACC-001: Create account from Admin UI @smoke

**Actors:** `adminUser`
**Preconditions:** No existing account with `koroneikiAccountCode=TESTCO001`

### Steps

1. Navigate to `/admin/accounts`.
2. Click **New Account**.
3. Fill in: Name = `Test Company`, Region = `Americas`, Tier = `Gold`, Status = `Active`, Internal = false.
4. Leave `koroneikiAccountCode` blank.
5. Submit.

### Assertions

- [ ] Account appears in the accounts list with auto-generated `koroneikiAccountCode` (format: slug of name + suffix).
- [ ] `koroneikiAccountCode` value is uppercase.
- [ ] A default system Team is automatically created for the account (name = `All Members`, `system=true`).
- [ ] The default Team appears on the account's Teams tab.

### Playwright notes

Intercept `POST /o/c/accountEntries` to capture the created `id` for teardown. Assert `koroneikiAccountCode` via `GET /o/c/accountEntries/{id}`.

---

## TC-ACC-002: Account code uniqueness — collision generates suffix @regression

**Actors:** `adminUser`
**Preconditions:** Account with `koroneikiAccountCode=ACME` exists.

### Steps

1. Create a second account with name `Acme` (slug resolves to `ACME`).

### Assertions

- [ ] Second account is created successfully.
- [ ] Second account's `koroneikiAccountCode` differs from `ACME` by a numeric suffix (e.g., `ACME1`).
- [ ] Both accounts coexist without error.

---

## TC-ACC-003: Account code uniqueness — direct collision rejected @regression

**Actors:** `adminUser`
**Preconditions:** Account with `koroneikiAccountCode=ACME` exists.

### Steps

1. Navigate to `/admin/accounts` → New Account.
2. Manually set `koroneikiAccountCode=acme` (lowercase).
3. Submit.

### Assertions

- [ ] Form is rejected with a uniqueness validation error.
- [ ] Error message references `koroneikiAccountCode`.

---

## TC-ACC-004: Edit account — update allowed fields @smoke

**Actors:** `workerManager` on the test account
**Preconditions:** Test account exists.

### Steps

1. Navigate to `/admin/accounts/{accountId}`.
2. Update `region` to `APAC`.
3. Update `tier` to `Platinum`.
4. Save.

### Assertions

- [ ] Changes are saved and reflected in the account detail view.
- [ ] `Worker_Member` cannot see the **Save** button (permission test in `10-permissions.md`).

---

## TC-ACC-005: Parent account hierarchy — set and display @regression

**Actors:** `adminUser`
**Preconditions:** Parent account and child account exist.

### Steps

1. Open child account in Admin UI.
2. Set `parentAccountEntryId` to the parent account.
3. Save.

### Assertions

- [ ] Child account shows parent account name in the hierarchy field.
- [ ] Parent account detail shows child account in a sub-accounts list.

---

## TC-ACC-006: Self-reference cycle rejected @regression

**Actors:** `adminUser`
**Preconditions:** Test account exists.

### Steps

1. Open test account.
2. Set `parentAccountEntryId` to itself.
3. Save.

### Assertions

- [ ] Save is rejected with a validation error.
- [ ] Error message references the cycle constraint.

---

## TC-ACC-007: Add AccountFlag to account @smoke

**Actors:** `workerAdmin` on the test account
**Preconditions:** Test account exists.

### Steps

1. Navigate to account detail → **Flags** tab.
2. Add a flag with `flagCode=compliance_hold`, `flagValue=true`.
3. Set `startDate` to today.
4. Save.

### Assertions

- [ ] Flag appears in the Flags list with correct `flagCode` and `flagValue`.
- [ ] Flag is visible on the Admin account detail page.

---

## TC-ACC-008: AccountNote — creator fields frozen after create @regression

**Actors:** `workerAdmin` on the test account
**Preconditions:** Test account exists.

### Steps

1. Create an AccountNote with `content=Initial note`, `type=General`.
2. Note the `creatorName` and `creatorUID` values.
3. Edit the note and change `content=Edited note`.
4. Save.

### Assertions

- [ ] `creatorName` and `creatorUID` remain unchanged after the edit.
- [ ] `modifierName` is updated to the editing user.

---

## TC-ACC-009: Customer_Admin cannot write to AccountNote @regression

**Actors:** `customerAdmin` on the test account
**Preconditions:** Test account has an AccountNote with `type=Internal`.

### Steps

1. Log in as `customerAdmin`.
2. Navigate to account → Notes tab.

### Assertions

- [ ] Internal notes are not visible to `customerAdmin`.
- [ ] `customerAdmin` can see notes with `type=General`.
- [ ] No **Edit** or **Delete** action is available for any note.

---

## TC-ACC-010: Create team and add members @smoke

**Actors:** `workerAdmin` on the test account
**Preconditions:** Test account exists with at least two users as Customer members.

### Steps

1. Navigate to account → **Teams** tab.
2. Click **New Team**.
3. Set name = `Engineering`, `system=false`.
4. Add both Customer users as members.
5. Save.

### Assertions

- [ ] Team appears in the Teams list.
- [ ] Both users appear in the team's member list.
- [ ] Team `teamKey` is auto-generated uppercase from the name.

---

## TC-ACC-011: System team cannot be deleted @regression

**Actors:** `workerAdmin` on the test account
**Preconditions:** Test account exists; default system Team (`All Members`, `system=true`) exists.

### Steps

1. Navigate to account → Teams tab.
2. Attempt to delete the `All Members` system team.

### Assertions

- [ ] Delete action is absent or disabled for the system team.
- [ ] If attempted via API: `DELETE /o/c/teams/{id}` returns `403` with a descriptive error.

---

## TC-ACC-012: ExternalLink created on account save @regression

**Actors:** `adminUser`
**Preconditions:** None.

### Steps

1. Create a new account with `salesforceId=0015000001ABCDE`.
2. Save.

### Assertions

- [ ] An `ExternalLink` row is created with `domain=salesforce`, `entityId=0015000001ABCDE`, pointing to the new account.
- [ ] Verify via `GET /o/c/externalLinks?filter=ownerClassPK eq '{accountId}'`.

---

## TC-ACC-013: BannedEmailDomain blocks form submissions @smoke

**Actors:** `guestUser` (unauthenticated)
**Preconditions:** `BannedEmailDomain` row exists for `mailinator.com`.

### Steps

1. Navigate to `/publisher-onboarding`.
2. Fill in the `RequestPublisherAccount` form with email `test@mailinator.com`.
3. Submit.

### Assertions

- [ ] Form submission is rejected.
- [ ] Error message indicates the email domain is not allowed.
- [ ] No `RequestPublisherAccount` row is created.

---

## TC-ACC-014: Search and filter accounts in Admin UI @regression

**Actors:** `liferayStaff`
**Preconditions:** At least 10 accounts exist with varying `region` and `tier`.

### Steps

1. Navigate to `/admin/accounts`.
2. Search for `Test` in the search bar.
3. Apply filter: Region = `Americas`.
4. Apply filter: Tier = `Gold`.

### Assertions

- [ ] Only accounts matching all filters appear in the list.
- [ ] Clearing filters restores the full (paginated) list.

---

## TC-ACC-015: Account ExternalLink browser in Admin UI @regression

**Actors:** `adminUser`
**Preconditions:** Account with at least one Salesforce ExternalLink.

### Steps

1. Navigate to `/admin/external-links`.
2. Filter by account.

### Assertions

- [ ] ExternalLink entries display `domain`, `entityId`, `ownerClassName`, and the linked account name.
- [ ] Clicking the row navigates to the account detail or the external URL.

---

## TC-ACC-016: Credit hold — blocked fields when creditStatus=Hold @regression

**Actors:** `adminUser`
**Preconditions:** Account has `creditStatus=Hold`.

### Steps

1. Navigate to account detail.
2. Attempt to create a new Subscription for the account.

### Assertions

- [ ] UI shows a "Credit Hold" warning banner on the account detail page.
- [ ] Subscription creation is blocked with an explanatory message referencing the hold.
