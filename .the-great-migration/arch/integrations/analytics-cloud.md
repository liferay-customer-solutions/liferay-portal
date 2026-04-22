# Analytics Cloud (Faro)

Workspace-provisioning for Liferay Analytics Cloud customers.

## Overview

**Direction:** outbound.

- Provision Faro workspace for new Analytics Cloud subscriptions.
- Deprovision on cancellation.
- Report usage back (inbound callback — optional, deferred).

**Trigger:** Commerce subscription activation for any `CommerceSubscriptionEntry` whose `CPDefinition.productFamily=Analytics` AND `OrderType.provisionsAnalytics=true`. Handled by `LicenseKeyLifecycleListener` (see `./commerce.md`).

## Auth

OAuth2 client credentials issued by Analytics Cloud team. Stored in `liferay-one-instance-settings` secret `analytics-cloud-credentials`.

---

## Provision workspace

Entry point: `POST /o/one/v1/analytics/provision/{subscriptionId}` (triggered by listener, but also callable from admin UI).

Workspace:
1. Load `CommerceSubscriptionEntry` + account.
2. Determine `DataCenter` (type=`AnalyticsCloud`) matching `AccountEntry.region`.
3. Call Faro:
   ```
   POST {faroBaseUrl}/api/v1/workspaces
   Authorization: Bearer <analytics-token>
   {
     "accountKey": "<AccountEntry.koroneikiAccountCode>",
     "accountName": "<AccountEntry.name>",
     "tier": "<tier name>",
     "dataCenter": "<DataCenter.providerRegion>",
     "adminEmail": "<primary contact email>",
     "features": ["dashboards", "segments", "reports"]
   }
   ```
4. Stash `response.workspaceId`, `response.workspaceUrl` on `ExternalLink(domain=analytics-cloud, entityName=workspace, ownerClass=AccountEntry)`.
5. Send welcome email via `ANALYTICS-CLOUD-WELCOME-TEMPLATE` notification template.

Idempotency: if an `ExternalLink` already exists for the same subscription, skip create and update the existing workspace instead.

## Deprovision

Triggered when the Analytics Cloud subscription cancels / expires.

```
DELETE {faroBaseUrl}/api/v1/workspaces/{workspaceId}
```

Workspace:
1. Flip the `ExternalLink` to `label=Decommissioned` (don't delete the row — audit trail).
2. Send "workspace decommissioned" notification.

## Tier upgrade / downgrade

On Commerce subscription amendment (seat count change, feature-tier change):

```
PATCH {faroBaseUrl}/api/v1/workspaces/{workspaceId}
{
  "tier": "<new tier>",
  "seatCount": <new count>
}
```

---

## Usage callback (deferred)

Faro emits workspace-usage events on a per-customer cadence. The workspace could consume these to populate a `UsageReport` record. Not modeled in this phase — add if product wants billing-adjacent usage visibility.

---

## Rate limits

Faro: ~30 req/min. Provision calls are spiky (burst during phase 3 backfill), so clients use token-bucket rate limiting.

---

## Failure handling

| Failure | Behavior |
|---|---|
| 409 (workspace exists) | Treat as success; look up workspace ID and update `ExternalLink` |
| 5xx / timeout | Retry × 3; fail-loud if still failing (Slack alert) |
| Deprovision 404 | Treat as success (already gone) |

---

## Observability

- Metric `one.analytics.workspaces.provisioned` — counter
- Metric `one.analytics.workspaces.active` — gauge
- Metric `one.analytics.requests` — counter `{operation, status}`

---

## Migration notes

- **Existing workspaces.** Today, some customers have Analytics Cloud workspaces provisioned via Marketplace post-purchase action. Migration-time: backfill `ExternalLink(domain=analytics-cloud, entityName=workspace)` rows from Faro's customer list (admin export) joined with AccountEntry by koroneikiAccountCode. No re-provision.
- **Hardcoded product-name match in legacy code** — `osb-provisioning` detects Analytics Cloud subscriptions by substring match on product name (`"Liferay Analytics Cloud Subscription - Business"` / `"Enterprise"`). New logic uses `CPDefinition.productFamily=Analytics` + `OrderType.provisionsAnalytics=true`, which requires correct seeding of those rows during phase 2.

## Open questions

1. **Faro workspace naming** — today's convention is `<accountKey>-analytics`. Confirm this naming matches Faro's expectations when the workspace starts creating them.
2. **DataCenter selection** — simple region→datacenter map works for AMER/EMEA/APAC. Edge cases (regulated regions, customer-specific placement) may need a `preferredDataCenter` custom field on `AccountEntry`.
3. **Billing integration** — Faro consumption isn't metered by the workspace today. If metering becomes a requirement, add the usage-callback handler.
