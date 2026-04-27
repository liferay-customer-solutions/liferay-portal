# Permissions — Test Scenarios

Tests the full role-based access control matrix from [`../arch/business-logic.md §5`](../arch/business-logic.md). Each scenario uses a specific actor and asserts allowed and denied operations.

Tests are organized by Object. The notation follows: `C`=create, `R`=read, `U`=update, `D`=delete.

---

## Role Reference

| Actor fixture | Role |
|---|---|
| `adminUser` | `Administrator` |
| `liferayStaff` | `Liferay Staff` user group + `Worker_Admin` on test account |
| `workerManager` | `Worker_Manager` on test account |
| `workerMember` | `Worker_Member` on test account |
| `customerAdmin` | `Customer_Admin` on test account |
| `customerManager` | `Customer_Manager` on test account |
| `customerMember` | `Customer_Member` on test account |
| `guestUser` | Unauthenticated |

---

## AccountEntry

### TC-PRM-001: CustomerAdmin can update own account profile fields @smoke

**Actors:** `customerAdmin`

### Steps

1. Navigate to customer portal account settings (if exposed) or via headless: `PATCH /o/accountEntries/{id}` with `profileEmailAddress`.

### Assertions

- [ ] `profileEmailAddress` update succeeds.
- [ ] `koroneikiAccountCode` update is rejected (customer-admin cannot change it).
- [ ] `internal` field update is rejected.

---

### TC-PRM-002: CustomerAdmin cannot read another account @regression

**Actors:** `customerAdmin` on Account A

### Steps

1. `GET /o/c/accountEntries/{idOfAccountB}` (Account B where customerAdmin has no membership).

### Assertions

- [ ] Response is `403` or empty results.

---

### TC-PRM-003: WorkerMember can only read AccountEntry @regression

**Actors:** `workerMember`

### Steps

1. `GET /o/c/accountEntries/{testAccountId}` → succeeds.
2. `PATCH /o/c/accountEntries/{testAccountId}` with any field → rejected.

### Assertions

- [ ] Read succeeds; update rejected with `403`.

---

## AccountNote

### TC-PRM-004: Customer roles see only General notes @regression

**Actors:** `customerAdmin`
**Preconditions:** Account has notes with `type=General` and `type=Internal`.

### Steps

1. `GET /o/c/accountNotes?filter=accountId eq {id}`.

### Assertions

- [ ] Only `type=General` notes are returned.
- [ ] `type=Internal` notes are absent.

---

### TC-PRM-005: WorkerAdmin can create and delete AccountNote @smoke

**Actors:** `workerAdmin`

### Steps

1. `POST /o/c/accountNotes` → should succeed.
2. `DELETE /o/c/accountNotes/{noteId}` → should succeed.

### Assertions

- [ ] Both operations return 2xx.

---

## Team

### TC-PRM-006: CustomerAdmin can create non-system teams @regression

**Actors:** `customerAdmin`

### Steps

1. `POST /o/c/teams` with `system=false` for own account.

### Assertions

- [ ] Team created successfully.

---

### TC-PRM-007: CustomerAdmin cannot delete system team @regression

**Actors:** `customerAdmin`
**Preconditions:** System team (`system=true`) exists for the account.

### Steps

1. Attempt `DELETE /o/c/teams/{systemTeamId}`.

### Assertions

- [ ] Rejected with `403` or validation error referencing `system=true`.

---

## EntitlementDefinition

### TC-PRM-008: Only Administrator can CRUD EntitlementDefinition @smoke

**Actors:** `adminUser`, `workerAdmin`, `customerAdmin`

### Steps

1. Each actor attempts `POST /o/c/entitlementDefinitions`.

### Assertions

- [ ] `adminUser` → 2xx (created).
- [ ] `workerAdmin` → 403.
- [ ] `customerAdmin` → 403.

---

## LicenseKey

### TC-PRM-009: CustomerAdmin can read own account license keys @regression

**Actors:** `customerAdmin`

### Steps

1. `GET /o/c/licenseKeys` (account-restricted).

### Assertions

- [ ] Only keys linked to the customer's accounts are returned.

---

### TC-PRM-010: CustomerAdmin cannot generate or revoke license keys @regression

**Actors:** `customerAdmin`

### Steps

1. `POST /o/one/v1/license-key/generate/{subscriptionId}`.
2. `POST /o/one/v1/license-key/{id}/revoke`.

### Assertions

- [ ] Both return `403`.

---

## SupportTicket

### TC-PRM-011: CustomerAdmin and CustomerManager can create tickets @smoke

**Actors:** `customerAdmin`, `customerManager`

### Steps

1. Each actor submits a new ticket (with Jira mocked).

### Assertions

- [ ] Both succeed with ticket creation.

---

### TC-PRM-012: CustomerMember can only read tickets @regression

**Actors:** `customerMember`

### Steps

1. `GET /o/c/supportTickets` → succeeds (filtered to own account).
2. `POST /o/c/supportTickets` → rejected.

### Assertions

- [ ] Read returns 2xx; create returns 403.

---

## BusinessEvent

### TC-PRM-013: Worker roles can edit BusinessEvent; customer roles cannot @regression

**Actors:** `workerAdmin`, `customerAdmin`

### Steps

1. `PATCH /o/c/businessEvents/{id}` (change `description`) as `workerAdmin`.
2. Same call as `customerAdmin`.

### Assertions

- [ ] `workerAdmin` → success.
- [ ] `customerAdmin` → 403.

---

## Admin pages

### TC-PRM-014: Guest cannot access any /admin page @smoke

**Actors:** `guestUser`

### Steps

1. Navigate to `/admin`.
2. Navigate to `/admin/accounts`.
3. Navigate to `/admin/debug`.

### Assertions

- [ ] All pages redirect to login or return 403.
- [ ] No admin content is rendered.

---

### TC-PRM-015: Liferay Staff can access /admin but not restricted sub-pages @regression

**Actors:** `liferayStaff` (not `Administrator`)

### Steps

1. Navigate to `/admin` → renders.
2. Navigate to `/admin/entitlement-definitions` → denied.
3. Navigate to `/admin/debug` → denied.
4. Navigate to `/admin/license-keys` (generate action) → generate button absent.

### Assertions

- [ ] Dashboard accessible; restricted sub-pages return 403 or hide restricted actions.

---

## Account-restricted object scoping

### TC-PRM-016: account-restricted Objects only return own-account rows @smoke

**Actors:** `customerAdmin` on Account A
**Preconditions:** Accounts A and B each have Deployments.

### Steps

1. `GET /o/c/environments` (no filter).

### Assertions

- [ ] Only Account A's environments are returned.
- [ ] Account B's environments are absent.

---

### TC-PRM-017: WorkerAdmin sees all accounts they are assigned to @regression

**Actors:** `workerAdmin` assigned to accounts A and B

### Steps

1. `GET /o/c/environments` (no filter).

### Assertions

- [ ] Environments for both Account A and Account B are returned.
- [ ] Environments for Account C (no assignment) are absent.

---

## TicketAttachment

### TC-PRM-018: CustomerAdmin can create and delete own-account attachments @regression

**Actors:** `customerAdmin`

### Steps

1. Initiate upload and complete upload (own account ticket).
2. Delete the attachment.

### Assertions

- [ ] Both operations succeed.
- [ ] Upload on a ticket belonging to a different account → 403.

---

## Publisher

### TC-PRM-019: Publisher owner can update own Publisher profile @regression

**Actors:** `publisherUser` (linked to a Publisher record)

### Steps

1. `PATCH /o/c/publishers/{ownPublisherId}` with new `emailAddress`.

### Assertions

- [ ] Update succeeds.
- [ ] `approvalStatus` field cannot be updated by the publisher (admin-only field).
