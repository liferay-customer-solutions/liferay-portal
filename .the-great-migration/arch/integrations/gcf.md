# Google Cloud Functions

Provides usage metric data for customer environments. The workspace calls two GCF functions to fetch current consumption — one for SaaS/Analytics Cloud environments, one for composable/PaaS environments — and surfaces the results via the `GET /accounts/{erc}/usage` endpoint.

## Overview

**Direction:** outbound — workspace → GCF (HTTP GET).

**Functions:**

| Function | Path | Environment type |
|---|---|---|
| `customer_usage_api` | `/customer_usage_api/api/v1/customer/usage/accounts/{accountKey}` | SaaS / Analytics Cloud |
| `composable_usage_api` | `/composable_usage_api/api/v1/accounts/{accountKey}/usage/month/{month}` | Composable / PaaS |

**Runtime:** `liferay-customer-etc-spring-boot` calls `GoogleCloudFunctionService` synchronously on each usage request. Responses are cached in-process.

**Auth:** Google Service Account ID tokens. Two separate service accounts — one per function.

---

## Auth

Each function has its own service account JSON key, passed as an environment variable. On each request the client:

1. Loads the service account JSON key bytes from config.
2. Creates `IdTokenCredentials` with the function URL as the target audience (`{baseUrl}/{functionPath}`).
3. Attaches the resulting Bearer token via `HttpCredentialsAdapter`.

Credentials are not cached across requests — a fresh ID token is acquired per call (token lifetime is 1 hour; the Google Auth library handles refresh transparently).

**Config properties:**

```properties
liferay.customer.gcf.base.url=${LIFERAY_CUSTOMER_GCF_BASE_URL}
liferay.customer.gcf.customer.service.account.key=${LIFERAY_CUSTOMER_GCF_CUSTOMER_SERVICE_ACCOUNT_KEY}
liferay.customer.gcf.composable.service.account.key=${LIFERAY_CUSTOMER_GCF_COMPOSABLE_SERVICE_ACCOUNT_KEY}
```

Keys are full service-account JSON strings, never file paths. Delivered via `spring-boot.env` at container startup.

---

## customer_usage_api — SaaS / Analytics Cloud

### Request

```
GET {baseUrl}/customer_usage_api/api/v1/customer/usage/accounts/{accountKey}
Authorization: Bearer {idToken}
```

`accountKey` is the stable account identifier (currently `koroneikiAccountCode`; in the new model this maps to `Account.koroneikiAccountCode`).

### Response

```json
{
  "totalAnonymousPageViewsCount": 123456,
  "totalClientExtensionsCapacityCPUCount": 8,
  "totalClientExtensionsCapacityRAM": 16.5,
  "totalMonthlyActiveLoggedInUsersCount": 5000,
  "totalSitesCount": 42,
  "totalStorageCapacityDocumentLibrary": 500.25
}
```

| Field | Unit |
|---|---|
| `totalAnonymousPageViewsCount` | count |
| `totalClientExtensionsCapacityCPUCount` | vCPU |
| `totalClientExtensionsCapacityRAM` | GB |
| `totalMonthlyActiveLoggedInUsersCount` | count |
| `totalSitesCount` | count |
| `totalStorageCapacityDocumentLibrary` | GB |

---

## composable_usage_api — Composable / PaaS

### Request

```
GET {baseUrl}/composable_usage_api/api/v1/accounts/{accountKey}/usage/month/{month}
Authorization: Bearer {idToken}
```

`month` is formatted `yyyy-MM` (e.g. `2026-04`).

### Response

```json
{
  "databaseStorage": 42.5,
  "clientExtensionsCPU": 4.0,
  "clientExtensionsRAM": 8.0,
  "logStorage": 12.3,
  "networkTraffic": 150.7,
  "documentLibraryAndBackupStorage": 300.0
}
```

| Field | Unit |
|---|---|
| `databaseStorage` | GB |
| `clientExtensionsCPU` | vCPU |
| `clientExtensionsRAM` | GB |
| `logStorage` | GB |
| `networkTraffic` | GB |
| `documentLibraryAndBackupStorage` | GB |

---

## Caller flow

`AccountsRestController.getUsage()` drives both functions:

1. Resolve `AccountEntry` from `externalReferenceCode`.
2. Fetch product purchases; determine environment type (SaaS vs. composable).
3. Call the appropriate GCF function via `GoogleCloudFunctionService`.
4. Wrap the raw JSON in `SaaSUsageStrategy` or `ExperienceUsageStrategy`.
5. Return merged usage + entitlement data to the caller.

---

## Caching

Responses are cached in-process using Caffeine.

| Property | Value |
|---|---|
| Cache name | `accountUsage` |
| Max entries | 1,000 |
| Eviction | Scheduled — full cache clear every hour (`0 0 * * * *`) |
| Annotation | `@Cacheable("accountUsage")` on both fetch methods |

Hourly full-eviction is intentionally coarse — usage data is informational (displayed to customers) and slight staleness is acceptable. If near-real-time accuracy is required in a future phase, switch to per-entry TTL.

---

## Error handling

| Condition | Behavior |
|---|---|
| 404 Not Found | Returns `null`; caller renders "no data available" |
| Any other non-2xx | Throws exception with status code + message + `accountKey` |
| JSON parse error | Exception propagates to REST controller; controller logs at ERROR |
| Network failure | Exception propagates; no retry in `GoogleCloudFunctionService` |

`setThrowExceptionOnExecuteError(false)` is set on the HTTP client so error codes are inspected manually rather than thrown on receipt. `httpResponse.disconnect()` is guaranteed in a `finally` block.

No retry logic exists in the service layer. Transient failures surface as HTTP 500 to the browser. If reliability becomes a concern, add retries with exponential backoff in `GoogleCloudFunctionService`.

---

## Observability

No dedicated metrics are emitted today. Failures are logged at ERROR level in `AccountsRestController`. Improvements planned for phase 5:

- Metric `one.gcf.requests.total` — counter, labels `{function, outcome}`
- Metric `one.gcf.request.duration` — histogram, label `{function}`
- Metric `one.gcf.cache.hit.ratio` — gauge (from Caffeine stats)

---

## Migration notes

- **`accountKey` mapping** — the current caller passes `koroneikiAccountCode` as the account key. In the new model, `Account.koroneikiAccountCode` carries forward unchanged, so the GCF call site requires no change during migration.
- **Ownership** — both GCF functions are owned by an external team (Cloud/Infrastructure). Confirm function URLs and service-account grants are provisioned for the new workspace's GCP project before phase 5 cut-over.
- **Per-environment granularity** — the current API is account-scoped. If the product direction moves toward per-`Environment` usage (tied to `Environment.deploymentKey`), the GCF contracts will need updating — raise with the owning team early in phase 4.
- **Composable month parameter** — the `yyyy-MM` month must be passed by the caller; there is no default. The REST endpoint currently derives it from the request. Verify the new `Usage Report` model's `periodStart`/`periodEnd` fields can be used as the authoritative source for this value post-migration.

---

## Open questions

1. **GCF function URLs** — confirm exact base URL per environment (`dev`, `staging`, `prod`) with the Cloud Infrastructure team before phase 5.
2. **Service account scope** — verify the new workspace GCP project has the same service-account grants as the current customer-workspace. Do not assume they carry over automatically.
3. **Per-environment usage** — assess whether account-scoped aggregation is sufficient for the new `Environment` / `Subscription` model or whether the GCF API contracts need to be extended.
4. **Retry policy** — decide whether to add retry-with-backoff in `GoogleCloudFunctionService` or push retries into a queue-backed pattern before exposing to the UI.
