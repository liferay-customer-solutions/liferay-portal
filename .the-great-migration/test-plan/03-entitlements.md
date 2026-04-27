# Entitlements — Test Scenarios

Covers: `EntitlementDefinition`, `Entitlement`, `EntitlementSync` scheduled task, the three scripted rules, and the two-phase cascade execution.

Source of truth: [`../arch/data-model.md §Entitlement`](../arch/data-model.md), [`../arch/business-logic.md §1.3`](../arch/business-logic.md), [`../plan/entitlement-rules-review.md`](../plan/entitlement-rules-review.md).

---

## TC-ENT-001: Create a filter-type EntitlementDefinition @smoke

**Actors:** `adminUser`
**Preconditions:** None.

### Steps

1. Navigate to `/admin/entitlement-definitions` → **New Definition**.
2. Set: `name=Gold Subscription Access`, `code=GOLD_SUBSCRIPTION`, `targetClassName=AccountEntry`, `ruleType=filter`.
3. Enter `ruleBody` JSON:
   ```json
   {
     "className": "com.liferay.object.model.ObjectEntry",
     "filter": "subscriptionStatus eq 'active' and tier eq 'Gold'",
     "relationshipChain": ["accountToSubscription"]
   }
   ```
4. Set `status=Active`.
5. Save.

### Assertions

- [ ] EntitlementDefinition is created with the correct `code` and `ruleType`.
- [ ] The rule-JSON editor validates the schema and shows no errors.
- [ ] Definition appears in the entitlement-definitions list with status `Active`.

---

## TC-ENT-002: Invalid ruleBody JSON rejected @regression

**Actors:** `adminUser`
**Preconditions:** None.

### Steps

1. Create an EntitlementDefinition with `ruleType=filter` and `ruleBody={"filter": "invalid syntax here"}` (missing required fields).
2. Submit.

### Assertions

- [ ] Save is rejected with a JSON schema validation error.
- [ ] Error message identifies the missing or invalid field.

---

## TC-ENT-003: cascadeAfter cycle rejected @regression

**Actors:** `adminUser`
**Preconditions:** EntitlementDefinition `DEF_A` exists.

### Steps

1. Set `DEF_A.cascadeAfter = DEF_A` (self-reference).
2. Save.

### Assertions

- [ ] Save is rejected.
- [ ] Error message references the cycle constraint in `cascadeAfter`.

---

## TC-ENT-004: EntitlementSync grants filter-rule entitlement @smoke

**Actors:** System (`EntitlementSync` task)
**Preconditions:** Account has an active Subscription matching the filter criteria of `GOLD_SUBSCRIPTION` definition.

### Steps

1. Ensure the definition is `status=Active`.
2. Trigger `EntitlementSync` via `/admin/debug` → **Recompute Entitlements** (or wait 15 min).

### Assertions

- [ ] An `Entitlement` row is created linking `entitlementDefinitionId=GOLD_SUBSCRIPTION` to the matching account.
- [ ] `grantedAt` and `lastConfirmedAt` are set to the sync time.
- [ ] Accounts not matching the filter have no grant for this definition.

---

## TC-ENT-005: EntitlementSync revokes stale entitlement @regression

**Actors:** System
**Preconditions:** Account has an `Entitlement` grant for a definition. The account's subscription has since expired.

### Steps

1. Expire the subscription.
2. Trigger `EntitlementSync`.

### Assertions

- [ ] The `Entitlement` row for the account is deleted.
- [ ] No other accounts lose their entitlement erroneously.

---

## TC-ENT-006: Manual recompute via Admin debug panel @smoke

**Actors:** `adminUser`
**Preconditions:** At least one active EntitlementDefinition and one matching account.

### Steps

1. Navigate to `/admin/debug`.
2. Click **Recompute Entitlements** for a specific account.

### Assertions

- [ ] `POST /o/one/v1/entitlements/recompute?accountEntryId={id}` is called.
- [ ] UI shows a success confirmation.
- [ ] Entitlement rows for the account are updated within a few seconds.

---

## TC-ENT-007: Phase A then Phase B execution — rule #62 waits on rule #40 @regression

**Actors:** System
**Preconditions:** EntitlementDefinitions for rule #40 (Account:Partner) and rule #62 (partnerContactWithGrantedAccount) exist with `cascadeAfter` set correctly.

### Steps

1. Trigger `EntitlementSync`.
2. Observe execution order in the task logs.

### Assertions

- [ ] Rule #40 is processed before rule #62.
- [ ] If rule #40 grants an `Entitlement` to an account, rule #62 subsequently grants a `User` entitlement on that account's contacts.
- [ ] Reversing the execution order (breaking `cascadeAfter`) causes rule #62 to grant nothing — this is a negative test verifiable by disabling `cascadeAfter` and running again.

---

## TC-ENT-008: Scripted rule — cloudNativeWithoutSaaS (#02) @regression

**Actors:** System
**Preconditions:** Account has `cloud_native` Environment AND active subscription for an AWS/Azure/Google-Ready product, but NOT a SaaS or PaaS subscription.

### Steps

1. Trigger `EntitlementSync`.

### Assertions

- [ ] `Entitlement` for `cloudNativeWithoutSaaS` is granted to the account.
- [ ] An account that has SaaS subscription does NOT receive this entitlement.

---

## TC-ENT-009: EntitlementDefinition Draft→Approved triggers immediate sync @regression

**Actors:** `adminUser`
**Preconditions:** EntitlementDefinition `status=Draft` exists.

### Steps

1. Open the definition in the Admin UI.
2. Change `status` to `Active` (Approved).
3. Save.

### Assertions

- [ ] `POST /o/one/v1/entitlements/recompute?definitionCode={code}` is called immediately (intercept).
- [ ] New entitlement grants appear within seconds for matching accounts.

---

## TC-ENT-010: Entitlement browser shows grants per account @smoke

**Actors:** `workerMember` on the test account
**Preconditions:** Test account has at least two active entitlements.

### Steps

1. Navigate to `/admin/entitlements`.
2. Filter by test account.

### Assertions

- [ ] Both entitlement rows are shown.
- [ ] Columns display: definition name, definition code, `targetClassName`, `grantedAt`.
- [ ] No **Edit** or **Delete** actions are available (entitlement browser is read-only for all roles except Administrator).

---

## TC-ENT-011: isPrimary flag required for 5 specific rules @regression

**Actors:** System
**Preconditions:** 
- Account has an active subscription for a Product (`CPDefinition`) with `isPrimary=false`.
- One of rules #06, #08, #21, #24, or #54–59 is active.

### Steps

1. Run `EntitlementSync`.

### Assertions

- [ ] No entitlement is granted for the rule because the matched product lacks `isPrimary=true`.
2. Set the product's `isPrimary=true`.
3. Run `EntitlementSync` again.

### Assertions (continued)

- [ ] Entitlement is now granted.
