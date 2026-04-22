# Salesforce

Implements system-spec **D12**. Replaces the Dossiera relay; Salesforce publishes opportunity events to a Google Pub/Sub topic and the workspace subscribes directly.

## Overview

**Direction:** inbound — Salesforce → workspace.

**Transport:** Google Pub/Sub pull subscription.

**Topic:** existing SF-owned topic `projects/<gcp-proj>/topics/salesforce-opportunities` (confirm exact ID during phase 3 handoff — SF admin team owns).

**Subscription:** `projects/<gcp-proj>/subscriptions/one-salesforce-opportunities` (workspace-owned, new).

**Runtime:** `liferay-one-etc-spring-boot` registers `SalesforceOpportunitySubscriber` on startup.

**Dead-letter topic:** `projects/<gcp-proj>/topics/one-deadletter` (workspace-owned). Messages that fail after 5 redeliveries go here.

---

## Message payload

Salesforce publishes one message per closed opportunity event (and per update). Payload is a JSON envelope on the Pub/Sub message body:

```json
{
  "eventType": "OpportunityClosedWon | OpportunityUpdated | OpportunityAmended",
  "eventTime": "2026-04-22T14:37:22Z",
  "opportunity": {
    "salesforceId": "0061Q00000XXXXXX",
    "name": "Acme Co — DXP Gold 3yr",
    "type": "New Business | Renewal | Upgrade | Addendum",
    "closeDate": "2026-04-21",
    "amount": 240000,
    "currency": "USD",
    "stage": "Closed Won"
  },
  "account": {
    "salesforceId": "0011Q00000XXXXXX",
    "name": "Acme Co",
    "billingCountry": "US",
    "billingState": "CA",
    "region": "AMER",
    "parentSalesforceAccountId": null
  },
  "contacts": [
    { "salesforceId": "0031Q00000XXXXXX", "firstName": "Ada", "lastName": "Lovelace", "email": "ada@acme.co", "role": "Technical Admin" }
  ],
  "products": [
    {
      "salesforceLineId": "00k1Q00000XXXXXX",
      "productCode": "DXP-GOLD-3YR",
      "productFamily": "DXP",
      "quantity": 1,
      "startDate": "2026-05-01",
      "endDate": "2029-04-30",
      "originalEndDate": "2029-04-30",
      "unitPrice": 80000,
      "recurring": true,
      "environment": "Production",
      "instanceSize": "L",
      "developerCount": 10
    }
  ],
  "externalReferences": {
    "dossieraId": "DSS-123",
    "salesforceProjectId": "a0T1Q00000XXXXXX"
  }
}
```

Contract details:

- `eventType` determines flow: `OpportunityClosedWon` creates/updates AccountEntry + Commerce order; `OpportunityUpdated` updates only (no new order); `OpportunityAmended` adjusts an existing order's line items.
- `contacts[].role` values map to Account Roles: `Technical Admin` → `Customer_Admin`, `Operations` → `Customer_Manager`, `User` → `Customer_Member`. Unknown roles default to `Customer_Member`.
- `products[].environment` + `instanceSize` feed `Deployment` creation when a new deployment is needed; existing deployments matching `(accountEntryId, name)` are augmented.

---

## Handler flow

1. **Ingest** — deserialize message; log with correlation ID = `opportunity.salesforceId`.
2. **Idempotency check** — look up `ExternalLink(domain=salesforce, entityName=opportunity, entityId=opportunity.salesforceId)`. If present and `lastProcessedAt >= eventTime`, ack and drop (duplicate delivery).
3. **Upsert AccountEntry** —
   - Find by `salesforceId=account.salesforceId`; else by `koroneikiAccountCode = derived code`; else create.
   - On create: generate `koroneikiAccountCode` from account name (uppercased slug + collision suffix). Set `region`, `tier` (default from product family), `status=Active`, `internal=false` (configurable), `salesforceId`.
   - Create `ExternalLink` rows for `salesforceId`, `externalReferences.dossieraId` (migration-only), `externalReferences.salesforceProjectId`.
4. **Upsert contact Users + account roles** — for each contact:
   - Find Liferay User by email; else create.
   - Add account membership; assign Account Role per contact role mapping.
5. **Create / update Commerce order** —
   - For `OpportunityClosedWon`: create `CommerceOrder`. One `CommerceOrderItem` per `products[]` entry. For `recurring=true`, create a subscription-enabled item → `CommerceSubscriptionEntry` with `startDate` / `endDate` / `originalEndDate`.
   - For `OpportunityAmended`: load existing order via `ExternalLink`; adjust line items (add new, update quantities, mark removed lines cancelled).
   - Populate `CommerceOrderItem.salesforceOpportunityId` and `CommerceOrderItem.koroneikiProductPurchaseKey` (null on new flow; populated during migration for continuity).
6. **Upsert Deployment** — for products with `environment` set, find `Deployment(accountEntryId, name=environment)`; else create. Link to the new `CommerceSubscriptionEntry` via the M:N relationship.
7. **Enforce developer-count cap** — set `CommerceSubscriptionEntry.developerCount` per product; warning + Slack alert if exceeds a configured threshold (ported from DossieraCreateMessageSubscriber logic).
8. **Entitlement re-sync** — POST `/entitlements/recompute?accountEntryId={id}` (fire-and-forget).
9. **Notifications** — send onboarding email if new-biz; skip on renewal/amendment. Fire "Analytics Cloud welcome" notification when a product matches Analytics Cloud SKUs.
10. **Ack** — ack the Pub/Sub message.

Any step that writes but fails midway rolls back (Spring `@Transactional` on the handler method). Pub/Sub nacks → redelivery.

---

## Opportunity-type vs account-existence warnings

Ported from DossieraCreateMessageSubscriber:

| Opportunity type | Account exists | Warning |
|---|---|---|
| New Business | yes | `[Warning] Opportunity marked New Business but account exists` |
| Renewal / Amendment | no | `[Warning] Opportunity marked Renewal but no existing account` |
| Upgrade | no | `[Warning] Opportunity marked Upgrade but no existing account` |

Warnings are non-fatal — prepend `[Warning]` to notification subject lines and log to Slack-bridged channel.

---

## Mapping tables

### Contact role → Account Role

| Salesforce contact role | Liferay Account Role |
|---|---|
| Technical Admin | `Customer_Admin` |
| Operations | `Customer_Manager` |
| Financial | `Customer_Manager` |
| User | `Customer_Member` |
| Partner Manager | `Partner_Manager` |
| Partner | `Partner_Member` |
| _(unknown)_ | `Customer_Member` |

### Product family → Tier default

Used only when creating a new AccountEntry and `tier` isn't already set.

| Product family (SF) | `AccountEntry.tier` default |
|---|---|
| DXP (Platinum SKU) | `Platinum` |
| DXP (Gold SKU) | `Gold` |
| DXP (Silver SKU) | `Silver` |
| DXP (Limited SKU) | `Bronze` |
| Trial products | `Trial` |
| No paid product | `Community` |

Exact SKU-to-tier mapping lives in a configuration table (`OrderType.defaultTier` — add a field; or an explicit `SkuToTier` picklist — decide in phase 3).

### Environment string → `Deployment.name`

| SF environment | `Deployment.name` |
|---|---|
| Production | Production |
| Non-Production / Staging | Non-Production |
| Dev / Development | Development |
| Backup / DR | Backup |
| HA / High Availability | HA Production |

---

## Failure handling

| Failure | Behavior |
|---|---|
| JSON parse error | Log + move to dead-letter immediately (don't retry unparseable messages) |
| Duplicate (idempotency hit) | Ack, drop |
| Transient DB error | Nack → Pub/Sub redelivers with backoff |
| `AccountEntry` collision (code dup beyond suffix limit) | Log + Slack alert; nack; dead-letter after 5 redeliveries |
| Jira / Liferay Cloud dependency failure during post-processing | **Don't fail the message** — post-processing runs in a separate `@Async` block so ingest succeeds. Failures log to `IntegrationFailure` and retry on a 1h scheduled task |
| Validation failure (banned email on a contact) | Log warning; skip that contact only; continue processing |

Dead-letter queue alerts route to the support-engineering Slack channel.

---

## Observability

- Metric `one.salesforce.messages.received` — counter
- Metric `one.salesforce.messages.processed` — counter, labels `{eventType, outcome}`
- Metric `one.salesforce.processing.duration` — histogram
- Metric `one.salesforce.deadletter.count` — gauge from dead-letter subscription
- Dashboard: `grafana.internal/d/one-salesforce` (TBD)
- Log correlation: all writes within a message's scope tagged with `correlation_id=opportunity.salesforceId`

---

## Migration notes

- **Phase 3 cut-over** — stand up this subscriber in parallel with the Dossiera relay. Validate against the same topic for 48h (both handlers process, but only Dossiera writes to Koroneiki; the new subscriber writes to a staging schema). Compare results.
- **SF-side config** — no changes required. Existing topic; only the subscription endpoint swaps.
- **Historical opportunities** — not replayed. New messages from cutover forward. For back-fill of rows that exist only in Koroneiki, use the phase-2 bulk extract.

## Referenced Salesforce Objects

Salesforce is the system of record for these. one.liferay holds FK references only.

### Account

| Field | Notes |
|---|---|
| PK `sfdcAccountId` | |
| `name`, `type`, `parentAccount` | |
| `currency` | |

### Contract / Project

| Field | Notes |
|---|---|
| PK `sfdcContractId` | |
| `term`, `start`, `end` | |
| `opportunityId`, `signedAt` | |

### CPQ Quote Line

| Field | Notes |
|---|---|
| PK `quoteLineId` | |
| FK `productId`, `quantity` | |
| `unitPrice` | locked at quote acceptance |

### Product Catalog

| Field | Notes |
|---|---|
| PK `productId`, `sku` | |
| `name`, `pricebookEntries` | |
| `metricCoverage` | entitlement rules |

---

## Open questions

1. **Exact topic and subscription IDs** — confirm with SF admin team before phase 3.
2. **Envelope schema stability** — verify the payload matches what Salesforce actually publishes (SF admin may have modified fields since the Dossiera integration was last reviewed). Capture actual payloads from the existing Dossiera queue for a week before cut-over.
3. ~~**Ordering guarantees.**~~ _Confirmed: no ordering key on the SF topic. The edge case (amendment arriving before create) is considered unlikely in practice, but the handler must be defensive:_ if an `OpportunityAmended` message arrives and no matching `ExternalLink(domain=salesforce, entityName=opportunity)` is found, log a warning, nack the message, and let Pub/Sub redeliver with backoff — by the time it redelivers the create should have been processed. After 3 redeliveries without a match, route to dead-letter for manual review.
4. **Dead-letter SLA** — define an SLA on dead-letter resolution. Default: 24h to human triage.
