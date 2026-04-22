# Microsoft NAV (Dynamics 365 Business Central)

Microsoft NAV is Liferay's finance and accounting system. liferay-one integrates with it in two directions: outbound to request invoice generation, and inbound to receive A/R aging events that trigger credit holds.

## Overview

**Direction:** bidirectional.

**Transport (outbound):** REST API calls from `liferay-one-etc-spring-boot` to NAV's OData v4 endpoint.

**Transport (inbound):** Google Pub/Sub topic `nav.account.aged` published by NAV's integration layer; `liferay-one-etc-spring-boot` subscribes.

**Auth:** OAuth2 client credentials via Azure AD (NAV is a Microsoft cloud service). Token audience is NAV's Business Central API resource URI.

**Config:**
```properties
liferay.one.nav.base-url=${LIFERAY_ONE_NAV_BASE_URL}
liferay.one.nav.tenant-id=${LIFERAY_ONE_NAV_TENANT_ID}
liferay.one.nav.client-id=${LIFERAY_ONE_NAV_CLIENT_ID}
liferay.one.nav.client-secret=${LIFERAY_ONE_NAV_CLIENT_SECRET}
liferay.one.nav.company-id=${LIFERAY_ONE_NAV_COMPANY_ID}
liferay.one.nav.aging-pubsub-subscription=${LIFERAY_ONE_NAV_AGING_PUBSUB_SUBSCRIPTION}
```

Secrets stored in `liferay-one-instance-settings` as `nav-client-credentials`.

---

## Outbound — Invoice request

When a billing cycle closes (renewal, usage-based month-end, or mid-term pro-rata add-on), liferay-one creates an `InvoiceRequest` Object row and then POSTs the invoice data to NAV. NAV generates the actual invoice document, assigns a `navInvoiceId`, and returns it in the response.

### Trigger

Business Logic `billing.md §Billing Flows`:

- **Renewal:** Commerce subscription renewal event → aggregate line items → POST to NAV.
- **Usage-based:** `UsageBillingMonthClose` scheduled task (end of billing period) → aggregate `UsageReport` rows → POST.
- **Pro-rata add-on:** `SubscriptionItem` added mid-term → calculate remaining months → POST.

### Request — create invoice

```
POST {navBaseUrl}/v2.0/{tenantId}/{companyId}/api/liferay/billing/v1/invoices
Authorization: Bearer {azureAdToken}
Content-Type: application/json
```

```json
{
  "externalInvoiceRef": "ONE-INV-{invoiceRequestId}",
  "accountCode": "{account.koroneikiAccountCode}",
  "currencyCode": "USD",
  "billingPeriodStart": "2026-04-01",
  "billingPeriodEnd": "2026-04-30",
  "paymentMethod": "eft",
  "lines": [
    {
      "lineRef": "{subscriptionItemId}",
      "description": "DXP Gold Subscription — Production",
      "quantity": 1,
      "unitPrice": 80000.00,
      "taxJurisdiction": "US-CA",
      "lineType": "prepaid"
    },
    {
      "lineRef": "{subscriptionItemId}-overage",
      "description": "Analytics Cloud — Overage (2,500 MAU over 10,000 limit)",
      "quantity": 2500,
      "unitPrice": 0.05,
      "taxJurisdiction": "US-CA",
      "lineType": "overage"
    }
  ]
}
```

### Response

```json
{
  "navInvoiceId": "INV-2026-004821",
  "status": "Posted",
  "totalGross": 80125.00,
  "dueDate": "2026-05-15"
}
```

`InvoiceRequest.navInvoiceId` is populated on success; `InvoiceRequest.status` transitions to `Posted`.

### Failure handling

| Failure | Behavior |
|---|---|
| 4xx (validation) | Log + set `InvoiceRequest.status=Failed`; Slack-alert finance channel; do not retry |
| 5xx / timeout | Retry up to 3× with exponential backoff; beyond retry budget → `status=Failed` + Slack alert |
| NAV returns duplicate `externalInvoiceRef` | Check `InvoiceRequest.navInvoiceId` — if already set, idempotent; skip |

---

## Inbound — A/R aging events

NAV publishes aging events to the `nav.account.aged` Pub/Sub topic when an account's receivables cross aging thresholds. liferay-one subscribes and auto-opens a `CreditHold`.

**Subscription:** `projects/{gcpProject}/subscriptions/one-nav-aged`

**Dead-letter topic:** `projects/{gcpProject}/topics/one-deadletter` (shared).

### Message payload

```json
{
  "eventType": "AccountAged",
  "eventTime": "2026-04-27T08:00:00Z",
  "account": {
    "navAccountCode": "ACME-001",
    "koroneikiAccountCode": "ACME",
    "agingBucket": "60-90",
    "totalOverdue": 95000.00,
    "currency": "USD"
  }
}
```

`agingBucket` values: `30-60`, `60-90`, `90+`.

### Handler flow

1. Resolve `AccountEntry` by `koroneikiAccountCode`.
2. Check for an open `CreditHold` on this account — if already open, ack and drop (duplicate event).
3. Create `CreditHold` row: `reason=aging`, `openedAt=eventTime`, `blocksOrder=true`, `blocksProvision=true`.
4. Set `AccountEntry.creditStatus=Hold`, `holdReason=A/R aging ({agingBucket})`.
5. Email `AccountEntry.profileEmailAddress` + notify assigned `Worker_Admin` users via workspace notification.
6. Ack.

### Credit hold release

When NAV marks an account current (receivables paid), it publishes an `AccountAgedCleared` event on the same topic.

```json
{
  "eventType": "AccountAgedCleared",
  "eventTime": "2026-04-30T14:22:00Z",
  "account": {
    "koroneikiAccountCode": "ACME"
  }
}
```

Handler: close the open `CreditHold` (`closedAt=eventTime`), reset `AccountEntry.creditStatus=Current`, notify the same recipients.

---

## Objects involved

| Object | Direction | Notes |
|---|---|---|
| `InvoiceRequest` | Written by liferay-one; `navInvoiceId` written back by NAV response | See `data-model.md §Invoice Request` |
| `CreditHold` | Written by liferay-one on aging event | See `data-model.md §Credit Hold` |
| `AccountEntry.creditStatus` / `holdReason` | Updated by both aging and clearing flows | |
| `PaymentMethod` | Read by the invoice request flow to set `paymentMethod` on invoice lines | See `data-model.md §Payment Method` |

---

## Observability

- Metric `one.nav.invoice.requests.total` — counter, labels `{outcome: success|failure}`
- Metric `one.nav.invoice.request.duration` — histogram
- Metric `one.nav.aging.events.total` — counter, labels `{eventType, outcome}`
- Metric `one.nav.credit_holds.opened` / `.closed` — counters
- Dashboard: `grafana.internal/d/one-nav` (TBD)
- Log correlation: `correlation_id={invoiceRequestId}` on outbound calls; `correlation_id={koroneikiAccountCode}` on inbound aging events

---

## Open questions

1. **NAV API contract** — confirm the OData endpoint path and payload schema with the Finance / IT team before phase 2. The shapes above are best-guess based on standard Dynamics 365 BC patterns.
2. **Aging bucket thresholds** — confirm which buckets trigger `blocksProvision=true` vs. only `blocksOrder=true`. 90+ seems clear; 60-90 may be advisory only.
3. **`nav.account.aged` topic owner** — confirm GCP project and topic name with the Finance team. Subscription must be provisioned in the liferay-one GCP project.
4. **Invoice line granularity** — does NAV want one line per `SubscriptionItem`, or one line per `Subscription`? Affects the request payload shape.
5. **Currency** — Liferay currently invoices in USD. If multi-currency is needed, `InvoiceRequest.currency` must be validated against NAV's enabled currencies.
6. **Phase assignment** — NAV integration is new (not in the original 6-phase plan). Recommend phasing it alongside or after phase 3 (Provisioning/Subscription migration), since `InvoiceRequest` depends on `CommerceSubscriptionEntry` being populated.
