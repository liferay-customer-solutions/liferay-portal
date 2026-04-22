# Entitlement Definition Review

Source: live `kor` database, `Koroneiki_EntitlementDefinition` table, status = Approved. 62 rules total — 49 targeting `Account`, 13 targeting `Contact`. Related to **D5** in [`system-spec.md`](./system-spec.md); phase-2 translation work tracked there.

**Purpose:** classify each rule into one of the four handling strategies so we can estimate migration work for D5, and capture how each rule translates into the new Commerce + Object model.

## Classification options

- **(a) Object filter** — the rule resolves to a criteria query on Object relationships (e.g., "Account has an Active Subscription for product X"). The new `EntitlementDefinition` Object stores a filter JSON; the sync scheduler evaluates it natively.
- **(b) Scripted action** — needs conditional logic, cross-entitlement dependency, or `NOT EXISTS` / `NOT IN` patterns that filters can't cleanly express. Implemented as a small function in `etc-spring-boot`, invoked by the Entitlement sync scheduler.
- **(c) Scheduled task** — bespoke scheduled task with its own logic. Escape hatch for anything not covered above.
- **(d) Retire** — dropped during migration.

## Summary

| Classification | Count | Notes |
|---|---:|---|
| (a) Object filter | 59 | Vast majority — simple "has active subscription for product X" (+ some require `isPrimary` custom field on CPDefinition) |
| (b) Scripted action | 3 | `NOT EXISTS` / `NOT IN` / cross-entitlement dependency (listed below) |
| (c) Scheduled task | 0 | None needed |
| (d) Retire | 0 | Per user instruction, no rules retired even at zero-matl count |
| **Total** | **62** | |

## Scripted-action rules

1. **#02 Cloud Native** (Account) — has AWS/Azure/Google Ready product AND NOT any SaaS/PaaS product. ✓ walked
2. **#14 Future Subscription** (Account) — has future-dated subscription product AND NOT currently active (coverage-gap detection). ✓ walked
3. **#62 Partner** (Contact) — Contact with `Partner_Manager` or `Partner_Member` Account Role on Account with Partner Entitlement. Cascade on #40 Account:Partner. ✓ walked (role catalog narrowed from 5 roles to 2 — `Partner Marketing User`, `Partner Sales User`, `Partner Technical User` deprecated).

**Cascade execution order:** the sync scheduler must run #40 Account:Partner **before** #62 Partner (Contact). Scheduler design: two-phase sync (Account entitlements, then Contact entitlements that depend on them).

## Open migration tasks (surfaced during review)

- **Add `isPrimary` custom field to Commerce `CPDefinition`** — required by many (a) rules: #06 Developer Tools, #08 DXP, #21 Liferay PaaS, #24 Liferay SaaS, and the four Customer-X Contact rules (#54, #57, #58, #59).
- **Rewrite #61 Liferay Employee** to look up the Liferay AccountEntry by code, not hardcoded `accountId = 15097278`.
- **EntitlementDefinition Object shape** — the new Object stores filter rules for (a) + a reference to a registered function for (b). Implementation: a `ruleType` discriminator field (`filter` / `scripted`) and a `ruleBody` JSON (either filter criteria or a function-name + params).
- **Partner role catalog narrowed** — only `Partner_Manager` and `Partner_Member` Account Roles defined in the new workspace. `Partner Marketing User`, `Partner Sales User`, `Partner Technical User` deprecated (confirmed during #62 review).

---

## 01. Active Subscription

- **ID:** 36723716
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 307

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
    (Koroneiki_ProductEntry.name in ('Gold Subscription', 'Limited Subscription', 'Platinum Subscription', 'Premium Subscription', 'Silver Subscription', 'Standard 8/5 Support', 'Global 24/7 Support', 'Premier 24/7 Support', 'Strategic 24/7 Support'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 02. Cloud Native

- **ID:** 30686858
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 7

```sql
SELECT
    DISTINCT(Koroneiki_Account.accountId)
FROM
    Koroneiki_Account
INNER JOIN
    Koroneiki_ProductPurchase AS ProductPurchase1 ON
        ProductPurchase1.accountId = Koroneiki_Account.accountId
INNER JOIN
    Koroneiki_ProductEntry AS ProductEntry1 ON
        ProductEntry1.productEntryId = ProductPurchase1.productEntryId
WHERE
    (ProductPurchase1.status = 0) AND
    (
        (
            (ProductPurchase1.startDate IS NULL) OR
            (ProductPurchase1.startDate <= '[$NOW$]')
        ) AND
        (
            (ProductPurchase1.endDate IS NULL) OR
            (ProductPurchase1.endDate >= '[$NOW$]')
        )
    ) AND
    (ProductEntry1.name IN ('AWS Ready', 'Azure Ready', 'Google Ready'))
    AND NOT EXISTS (
        SELECT 1
        FROM Koroneiki_ProductPurchase AS ProductPurchase2
        INNER JOIN Koroneiki_ProductEntry AS ProductEntry2 ON
            ProductEntry2.productEntryId = ProductPurchase2.productEntryId
        WHERE
            ProductPurchase2.accountId = Koroneiki_Account.accountId AND
            (ProductPurchase2.status = 0) AND
            (
                (
                    (ProductPurchase2.startDate IS NULL) OR
                    (ProductPurchase2.startDate <= '[$NOW$]')
                ) AND
                (
                    (ProductPurchase2.endDate IS NULL) OR
                    (ProductPurchase2.endDate >= '[$NOW$]')
                )
            ) AND
            (ProductEntry2.name LIKE '%SaaS%' OR ProductEntry2.name LIKE '%PaaS%')
    )
```

**Classification:** **(b) Scripted action** — "has cloud-native product AND NOT SaaS/PaaS" pattern. NOT EXISTS is awkward in Object filters.

**Notes:** _Walked through — see session notes. Cloud Native: has AWS/Azure/Google Ready product AND NOT any SaaS/PaaS product. Future Subscription: has future-dated support/subscription product AND NOT currently-active equivalent (gap detection)._

---

## 03. Content Management System

- **ID:** 30689817
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Content Management System')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 04. Content Marketing Platform

- **ID:** 30689843
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 2

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Content Marketing Platform')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 05. Developer Services

- **ID:** 30353567
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
    (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Developer Services'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 06. Developer Tools

- **ID:** 30353437
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 280

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND 
  (Koroneiki_ProductField.value = 'primary') AND
  (
    (Koroneiki_ProductEntry.name LIKE '%DXP%') OR
        (Koroneiki_ProductEntry.name LIKE '%Liferay Platform%') OR
        (Koroneiki_ProductEntry.name LIKE '%Liferay Self-Hosted%') OR
    (Koroneiki_ProductEntry.name LIKE '%LXC SM%')
  ) AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (
      (Koroneiki_ProductPurchase.startDate IS NULL) OR
      (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
    ) AND
    (
      (Koroneiki_ProductPurchase.endDate IS NULL) OR
      (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
    )
  )
```

**Classification:** **(a) Object filter**

**Notes:** Depends on Koroneiki `ProductField` (type=primary). Migration: add `isPrimary` custom field to Commerce `CPDefinition`. Filter: `CommerceSubscriptionEntry.status = Active AND CPDefinition.isPrimary = true AND CPDefinition.name LIKE <pattern>`.

---

## 07. Digital Asset Management

- **ID:** 30689830
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Digital Asset Management')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 08. DXP

- **ID:** 25777092
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 296

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND 
  (Koroneiki_ProductField.value = 'primary') AND
  (
    (Koroneiki_ProductEntry.name LIKE '%DXP%') OR
        (Koroneiki_ProductEntry.name LIKE '%Liferay Platform%') OR
        (Koroneiki_ProductEntry.name LIKE '%Liferay Self-Hosted%') OR
 (Koroneiki_ProductEntry.name LIKE '%Liferay PaaS%')
  ) AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (
      (Koroneiki_ProductPurchase.startDate IS NULL) OR
      (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
    ) AND
    (
      (Koroneiki_ProductPurchase.endDate IS NULL) OR
      (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
    )
  )
```

**Classification:** **(a) Object filter**

**Notes:** Depends on Koroneiki `ProductField` (type=primary). Migration: add `isPrimary` custom field to Commerce `CPDefinition`. Filter: `CommerceSubscriptionEntry.status = Active AND CPDefinition.isPrimary = true AND CPDefinition.name LIKE <pattern>`.

---

## 09. EPS 7.0

- **ID:** 30353476
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Extended Premium Support - Liferay Self-Hosted 7.0'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 10. EPS 7.1

- **ID:** 30353489
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Extended Premium Support - Liferay Self-Hosted 7.1'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 11. EPS 7.2

- **ID:** 30353502
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Extended Premium Support - Liferay Self-Hosted 7.2'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 12. EPS 7.3

- **ID:** 30353515
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 4

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Extended Premium Support - Liferay Self-Hosted 7.3'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 13. EPS Portal

- **ID:** 30353528
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Extended Premium Support - Portal'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 14. Future Subscription

- **ID:** 30592281
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 10

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (Koroneiki_ProductPurchase.startDate >= '[$NOW$]') AND
  (
    (Koroneiki_ProductEntry.name = 'Gold Subscription') OR
    (Koroneiki_ProductEntry.name = 'Limited Subscription') OR
    (Koroneiki_ProductEntry.name = 'Platinum Subscription') OR
    (Koroneiki_ProductEntry.name = 'Premium Subscription') OR
    (Koroneiki_ProductEntry.name = 'Silver Subscription') OR
                (Koroneiki_ProductEntry.name = 'Standard 8/5 Support') OR
                (Koroneiki_ProductEntry.name = 'Global 24/7 Support') OR
                (Koroneiki_ProductEntry.name = 'Premier 24/7 Support') OR
                (Koroneiki_ProductEntry.name = 'Strategic 24/7 Support')
  ) AND
  (Koroneiki_Account.accountId NOT IN (
    SELECT
      DISTINCT(Koroneiki_Account.accountId)
    FROM
      Koroneiki_Account
    INNER JOIN
      Koroneiki_ProductPurchase ON
        Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
    INNER JOIN
      Koroneiki_ProductEntry ON
          Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
    WHERE
      (Koroneiki_ProductPurchase.status = 0) AND
      (
        (
          (Koroneiki_ProductPurchase.startDate IS NULL) OR
          (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
        ) AND
        (
          (Koroneiki_ProductPurchase.endDate IS NULL) OR
          (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
        )
      ) AND
      (
        (Koroneiki_ProductEntry.name = 'Gold Subscription') OR
        (Koroneiki_ProductEntry.name = 'Limited Subscription') OR
        (Koroneiki_ProductEntry.name = 'Platinum Subscription') OR
        (Koroneiki_ProductEntry.name = 'Premium Subscription') OR
        (Koroneiki_ProductEntry.name = 'Silver Subscription') OR
                                (Koroneiki_ProductEntry.name = 'Standard 8/5 Support') OR
                                (Koroneiki_ProductEntry.name = 'Global 24/7 Support') OR
                                (Koroneiki_ProductEntry.name = 'Premier 24/7 Support') OR
                                (Koroneiki_ProductEntry.name = 'Strategic 24/7 Support')
      )
    )
  )
```

**Classification:** **(b) Scripted action** — "has future-dated subscription AND NOT currently active." NOT IN subquery.

**Notes:** _Walked through — see session notes. Cloud Native: has AWS/Azure/Google Ready product AND NOT any SaaS/PaaS product. Future Subscription: has future-dated support/subscription product AND NOT currently-active equivalent (gap detection)._

---

## 15. Global 24/7 Support

- **ID:** 30753543
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 6

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Global 24/7 Support')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 16. Gold Subscription

- **ID:** 35645
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 119

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Gold Subscription')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 17. Liferay Analytics Cloud

- **ID:** 30353424
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 107

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Analytics Cloud Basic', 'Analytics Cloud Business', 'Analytics Cloud Enterprise'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 18. Liferay Commerce

- **ID:** 30353450
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 89

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name like ('%Commerce Subscription%') OR Koroneiki_ProductEntry.name = 'Commerce' OR Koroneiki_ProductEntry.name = 'Commerce for PaaS')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 19. Liferay Data Platform

- **ID:** 30812233
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 43

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Liferay Data Platform')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 20. Liferay Enterprise Search

- **ID:** 30353463
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 36

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name like ('%Enterprise Search%'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 21. Liferay PaaS

- **ID:** 29043196
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 50

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN  
        Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND
  (Koroneiki_ProductField.value = 'primary') AND
  (
    (Koroneiki_ProductEntry.name LIKE '%Liferay PaaS%') OR
    (Koroneiki_ProductEntry.name = 'PaaS Experience')
  ) AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (
      (Koroneiki_ProductPurchase.startDate IS NULL) OR
      (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
    ) AND
    (
      (Koroneiki_ProductPurchase.endDate IS NULL) OR
      (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
    )
  )
```

**Classification:** **(a) Object filter**

**Notes:** Depends on Koroneiki `ProductField` (type=primary). Migration: add `isPrimary` custom field to Commerce `CPDefinition`. Filter: `CommerceSubscriptionEntry.status = Active AND CPDefinition.isPrimary = true AND CPDefinition.name LIKE <pattern>`.

---

## 22. Liferay PaaS Premium Security

- **ID:** 30199883
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 2

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Premium Security')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 23. Liferay Portal

- **ID:** 30353411
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 50

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Portal Production', 'Portal OEM', 'Portal Non-Production', 'Portal Limited', 'Portal Enterprise', 'Portal Backup', 'Portal Development'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 24. Liferay SaaS

- **ID:** 29043183
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 29

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND
  (Koroneiki_ProductField.value = 'primary') AND
  (
    (Koroneiki_ProductEntry.name LIKE 'Liferay SaaS%') OR
    (Koroneiki_ProductEntry.name = 'SaaS Experience')
  ) AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (
      (Koroneiki_ProductPurchase.startDate IS NULL) OR
      (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
    ) AND
    (
      (Koroneiki_ProductPurchase.endDate IS NULL) OR
      (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
    )
  )
```

**Classification:** **(a) Object filter**

**Notes:** Depends on Koroneiki `ProductField` (type=primary). Migration: add `isPrimary` custom field to Commerce `CPDefinition`. Filter: `CommerceSubscriptionEntry.status = Active AND CPDefinition.isPrimary = true AND CPDefinition.name LIKE <pattern>`.

---

## 25. Liferay SaaS - Business

- **ID:** 30353346
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 10

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Liferay SaaS - Business Plan'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 26. Liferay SaaS - CSP

- **ID:** 30353398
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Liferay SaaS - CSP Plan'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 27. Liferay SaaS - Engage

- **ID:** 30353372
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Liferay SaaS - Engage Plan'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 28. Liferay SaaS - Enterprise

- **ID:** 30353320
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 5

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Liferay SaaS - Enterprise Plan'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 29. Liferay SaaS - Pro

- **ID:** 30353333
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 6

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Liferay SaaS - Pro Plan'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 30. Liferay SaaS - Support

- **ID:** 30353385
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Liferay SaaS - Support Plan'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 31. Liferay SaaS - Transact

- **ID:** 30353359
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Liferay SaaS - Transact Plan'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 32. Liferay SaaS Disaster Recovery

- **ID:** 30199896
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Disaster Recovery Add-On for SaaS')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 33. Liferay SaaS Flexibility

- **ID:** 30199909
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Flexibility Add-On for SaaS')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 34. Liferay Self-Hosted

- **ID:** 29043209
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 262

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('DXP Backup', 'DXP Development', 'DXP Flex', 'DXP Limited', 'DXP Non-Production', 'DXP OEM', 'DXP Production', 'DXP Unlimited Enterprise-Wide'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 35. Limited Subscription

- **ID:** 35658
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 21

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Limited Subscription')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 36. Maintenance Services

- **ID:** 30353554
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Maintenance Services'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 37. Managed Services

- **ID:** 30353541
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 4

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name like ('%Managed Services%'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 38. Marketplace Calendar Widget

- **ID:** 18767753
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 1

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
(Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.productEntryId = 18767691)
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 39. PaaS Experience

- **ID:** 30689922
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 6

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('PaaS Experience'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 40. Partner

- **ID:** 35697
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 61

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('Service Partnership', 'Basic Reseller Partnership', 'Value-Added Reseller Partnership', 'Reseller Partnership', 'Distribution Partnership', 'Service Partnership - Referral Program', 'Solution Partnership - Reseller Program', 'DXP OEM', 'Portal OEM'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 41. Platinum Subscription

- **ID:** 35671
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 166

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Platinum Subscription')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 42. Premier 24/7 Support

- **ID:** 30753556
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 4

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Premier 24/7 Support')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 43. Premium Security Trial

- **ID:** 30627609
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Premium Security Trial')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 44. Premium Subscription

- **ID:** 12174673
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 4

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Premium Subscription')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 45. SaaS Experience

- **ID:** 30689909
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 5

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  ((Koroneiki_ProductPurchase.status = 0) AND
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name in ('SaaS Experience'))
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 46. Silver Subscription

- **ID:** 35684
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Silver Subscription')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 47. Standard 8/5 Support

- **ID:** 30753530
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 7

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Standard 8/5 Support')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 48. Strategic 24/7 Support

- **ID:** 30753597
- **Target:** Account
- **Description:** _(none)_
- **Materialized count:** 4

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
      Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name = 'Strategic 24/7 Support')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 49. TAM Services

- **ID:** 29408907
- **Target:** Account
- **Description:** Has a Technical Account Management Services product purchase.
- **Materialized count:** 14

```sql
SELECT
  DISTINCT(Koroneiki_Account.accountId)
FROM
  Koroneiki_Account
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
(Koroneiki_ProductPurchase.status = 0) AND
  (
      (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.startDate <= '[$NOW$]')
  ) AND
  (
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
  ) AND
  (Koroneiki_ProductEntry.name LIKE '%Technical Account Management%')
```

**Classification:** **(a) Object filter**

**Notes:** Straightforward product-name filter. New model: `CommerceSubscriptionEntry.status = Active AND CPDefinition.name IN (...)`. Date filtering becomes Commerce subscription status (Active), not `startDate`/`endDate` math.

---

## 50. Customer (Contact)

- **ID:** 35736
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 3304

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
WHERE
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active `CommerceSubscriptionEntry` for matching `CPDefinition`. Joins Contact-Account-Purchase-Product rewrite to User-AccountEntry-Commerce.

---

## 51. Customer - Commerce (Contact)

- **ID:** 35775
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 996

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (
                (Koroneiki_ProductEntry.name LIKE 'Commerce for PaaS%' ) OR
    (Koroneiki_ProductEntry.name LIKE 'Commerce Subscription%' )
         ) AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active `CommerceSubscriptionEntry` for matching `CPDefinition`. Joins Contact-Account-Purchase-Product rewrite to User-AccountEntry-Commerce.

---

## 52. Customer - Commerce Connector to PunchOut2Go (Contact)

- **ID:** 35801
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductEntry.name LIKE '%Connector to PunchOut2Go%') AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active `CommerceSubscriptionEntry` for matching `CPDefinition`. Joins Contact-Account-Purchase-Product rewrite to User-AccountEntry-Commerce.

---

## 53. Customer - Commerce Connector to Salesforce (Contact)

- **ID:** 35788
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 2

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductEntry.name LIKE '%Connector to Salesforce%') AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active `CommerceSubscriptionEntry` for matching `CPDefinition`. Joins Contact-Account-Purchase-Product rewrite to User-AccountEntry-Commerce.

---

## 54. Customer - DXP (Contact)

- **ID:** 35749
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 1208

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND 
  (Koroneiki_ProductField.value = 'primary') AND
  (
    (Koroneiki_ProductEntry.name LIKE '%DXP%') OR
                (Koroneiki_ProductEntry.name LIKE '%Liferay Self-Hosted%') OR
    (Koroneiki_ProductEntry.name LIKE '%LXC SM%')
  ) AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active primary-product `CommerceSubscriptionEntry` where `CPDefinition.isPrimary = true` AND `CPDefinition.name LIKE`<pattern>. Depends on `isPrimary` custom field migration.

---

## 55. Customer - Enterprise Search (Contact)

- **ID:** 35814
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 129

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (
    (Koroneiki_ProductEntry.name = 'Enterprise Search - Backup') OR
    (Koroneiki_ProductEntry.name = 'Enterprise Search - Non-Production') OR
    (Koroneiki_ProductEntry.name = 'Enterprise Search - Production') OR
                (Koroneiki_ProductEntry.name = 'Enterprise Search Enterprise-Wide Subscription')
  ) AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active `CommerceSubscriptionEntry` for matching `CPDefinition`. Joins Contact-Account-Purchase-Product rewrite to User-AccountEntry-Commerce.

---

## 56. Customer - Enterprise Search - Standard (Contact)

- **ID:** 35827
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductEntry.name LIKE 'Enterprise Search - Standard%') AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active `CommerceSubscriptionEntry` for matching `CPDefinition`. Joins Contact-Account-Purchase-Product rewrite to User-AccountEntry-Commerce.

---

## 57. Customer - Liferay PaaS (Contact)

- **ID:** 29043250
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 116

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_ContactRole ON
    Koroneiki_ContactRole.contactRoleId = Koroneiki_ContactAccountRole.contactRoleId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND
  (Koroneiki_ProductField.value = 'primary') AND
  (Koroneiki_ProductEntry.name LIKE '%Liferay PaaS%') AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  ) AND
  (Koroneiki_ContactRole.name in ('Support Administrator', 'Support Requester', 'Support User', 'Support Closed Watcher'))
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active primary-product `CommerceSubscriptionEntry` where `CPDefinition.isPrimary = true` AND `CPDefinition.name LIKE`<pattern>. Plus ContactRole restriction → filter by Account Role in {Support_Administrator, Support_Requester, Support_User, Support_Closed_Watcher} (per D2 role catalog). Depends on `isPrimary` custom field migration.

---

## 58. Customer - Liferay SaaS (Contact)

- **ID:** 29043237
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 137

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND
  (Koroneiki_ProductField.value = 'primary') AND
  (Koroneiki_ProductEntry.name LIKE '%Liferay SaaS%') AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active primary-product `CommerceSubscriptionEntry` where `CPDefinition.isPrimary = true` AND `CPDefinition.name LIKE`<pattern>. Depends on `isPrimary` custom field migration.

---

## 59. Customer - Liferay Self-Hosted (Contact)

- **ID:** 29043224
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 0

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
INNER JOIN
  Koroneiki_ProductField ON
    Koroneiki_ProductField.classPK = Koroneiki_ProductEntry.productEntryId
WHERE
  (Koroneiki_ProductField.name = 'type') AND
  (Koroneiki_ProductField.value = 'primary') AND
  (Koroneiki_ProductEntry.name LIKE '%Liferay Self-Hosted%') AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active primary-product `CommerceSubscriptionEntry` where `CPDefinition.isPrimary = true` AND `CPDefinition.name LIKE`<pattern>. Depends on `isPrimary` custom field migration.

---

## 60. Customer - Portal (Contact)

- **ID:** 35762
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 805

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_ProductPurchase ON
    Koroneiki_ProductPurchase.accountId = Koroneiki_Account.accountId
INNER JOIN
  Koroneiki_ProductEntry ON
    Koroneiki_ProductEntry.productEntryId = Koroneiki_ProductPurchase.productEntryId
WHERE
  (Koroneiki_ProductEntry.name LIKE '%Portal%') AND
  (Koroneiki_ProductPurchase.status = 0) AND
  (
    (Koroneiki_ProductPurchase.startDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate IS NULL) OR
    (Koroneiki_ProductPurchase.endDate >= '[$NOW$]')
  )
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is Account member + Account has active `CommerceSubscriptionEntry` for matching `CPDefinition`. Joins Contact-Account-Purchase-Product rewrite to User-AccountEntry-Commerce.

---

## 61. Liferay Employee (Contact)

- **ID:** 35723
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 251

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
WHERE
  Koroneiki_Account.accountId = 15097278
```

**Classification:** **(a) Object filter**

**Notes:** Filter: User is member of AccountEntry whose code matches "LIFERAY" (or equivalent). Original rule hard-codes `accountId = 15097278`; migrate to code-based lookup.

---

## 62. Partner (Contact)

- **ID:** 35710
- **Target:** Contact
- **Description:** _(none)_
- **Materialized count:** 338

```sql
SELECT
  DISTINCT(Koroneiki_Contact.contactId)
FROM
  Koroneiki_Contact
INNER JOIN
  Koroneiki_ContactAccountRole ON
    Koroneiki_ContactAccountRole.contactId = Koroneiki_Contact.contactId
INNER JOIN
  Koroneiki_ContactRole ON
    Koroneiki_ContactRole.contactRoleId = Koroneiki_ContactAccountRole.contactRoleId
INNER JOIN
  Koroneiki_Account ON
    Koroneiki_Account.accountId = Koroneiki_ContactAccountRole.accountId
INNER JOIN
  Koroneiki_Entitlement ON
    Koroneiki_Entitlement.classPK = Koroneiki_Account.accountId
WHERE
  (Koroneiki_ContactRole.name = 'Partner Manager' OR Koroneiki_ContactRole.name = 'Partner Member' OR Koroneiki_ContactRole.name = 'Partner Marketing User' OR Koroneiki_ContactRole.name = 'Partner Sales User' OR Koroneiki_ContactRole.name = 'Partner Technical User') AND
  (Koroneiki_Entitlement.name = 'Partner') AND
  (Koroneiki_Account.status = 0)
```

**Classification:** **(b) Scripted action** — Contact with Partner-family Account Role on Account with Partner Entitlement. Cascade on #40 Account:Partner.

**Notes:** Walked through — reviewer confirmed the Partner role catalog has been narrowed. Only **`Partner_Manager`** and **`Partner_Member`** are active in the new model; `Partner Marketing User`, `Partner Sales User`, `Partner Technical User` are deprecated and drop from the filter. New implementation:
```
def isContactPartner(user):
  PARTNER_ROLES = ['Partner_Manager', 'Partner_Member']
  for account in user.accountMemberships(status=Approved):
    if user.hasAccountRoleIn(account, PARTNER_ROLES) and account.hasEntitlement('Partner'):
      return True
  return False
```
Scheduler runs Account entitlements first (including #40 Account:Partner), then Contact entitlements. Two-phase sync.

---

