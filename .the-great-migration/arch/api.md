# API Surface

Implements system-spec §5. Splits into three layers:

1. **Headless** — auto-generated per-Object REST + GraphQL. Covers 90% of read and simple write paths.
2. **Custom REST** — `liferay-one-etc-spring-boot` for orchestration workflows that aren't CRUD.
3. **Webhooks / async subscribers** — inbound Pub/Sub subscriber, Jira webhook.

All auth via OAuth2. No ServiceProducer impersonation (retired per D13).

---

## 1. Headless — auto-generated

Every workspace Object exposes:

- REST: `GET|POST|PUT|PATCH|DELETE /o/c/{pluralName}` and `/o/c/{pluralName}/{id}` (or `/by-external-reference-code/{erc}`).
- GraphQL: `c{pluralName}`, `c{singularName}`, `createC{singularName}`, `updateC{singularName}`, `deleteC{singularName}`.

Liferay Object runtime handles pagination, filtering, sorting, field selection, and relationship expand. Design-time settings per Object:

- `indexed: true` on fields that need `filter=` or sort.
- `indexedAsKeyword: true` on exact-match string fields.
- `titleObjectFieldName` on each Object so list views get a default display column.

### 1.1 Path naming

- Plural path uses Liferay's pluralizer — `AccountFlag` → `/o/c/accountFlags`, `LicenseKey` → `/o/c/licenseKeys`, `EntitlementDefinition` → `/o/c/entitlementDefinitions`.
- Verify each Object's pluralized path during site-initializer deploy; override via `pluralLabel` if pluralizer misfires.

### 1.2 Scoping

Account-restricted Objects (`accountEntryRestricted: true`) auto-filter to the calling user's account membership. Worker_* roles see all accounts they're assigned to. Queries from OAuth2 service callers use the configured scope's account list (may be "all").

### 1.3 Filter rules

- Date filtering via OData `filter=startDate gt 2026-01-01T00:00:00Z`.
- Relationship traversal via dotted path: `filter=publisher.approvalStatus eq 'Approved'`.
- `Picklist`-backed fields filtered by exact value — Liferay translates to the stored enum key.

### 1.4 What is NOT auto-generated

- Cross-Object aggregations (e.g., "publisher sales for quarter X"). Handled via a custom REST endpoint or a pre-computed `PublisherSalesSummary` row.
- Business-logic write flows (provision trial, generate license, post Jira comment). Handled in §2.

---

## 2. Custom REST (`liferay-one-etc-spring-boot`)

Single Spring Boot app. Base path `/o/one/v1`. Auth via `Authorization: Bearer <oauth2-token>`.

### 2.1 Trial lifecycle (Marketplace)

| Method | Path | Purpose | OAuth2 scope | Replaces |
|---|---|---|---|---|
| POST | `/trial/provision/{subscriptionId}` | Provision trial Liferay Cloud instance; create `TrialProvisioning`; update status; start timers | `subscription.write` | Marketplace `POST /trial/provisioning` |
| POST | `/trial/expire/{subscriptionId}` | Mark `TrialProvisioning.status=Expired`; call Liferay Cloud decomm | `subscription.write` | Marketplace `POST /trial/expire` |
| POST | `/trial/notify-end/{subscriptionId}` | Send end-of-trial email; update `trialNotifyEndDate` | `subscription.write` | Marketplace `POST /trial/notify-end` |
| GET | `/trial/availability?orderTypeExternalReferenceCode={erc}` | Check seat availability per OrderType | `subscription.read` | Marketplace `GET /trial/availability` |

### 2.2 License keys

| Method | Path | Purpose | Scope |
|---|---|---|---|
| POST | `/license-key/generate/{subscriptionId}` | Generate `LicenseKey` row; sign; return key string | `license.admin` |
| POST | `/license-key/{id}/revoke` | `status=Revoked`; notify | `license.admin` |
| GET | `/license-key/{id}/download` | Return signed key artifact (DXP-format license file) | `license.read` |
| POST | `/license-key/regenerate/{id}` | Issue replacement key with same parameters; supersede original | `license.admin` |

### 2.3 Ticket attachments (GCS-backed)

Pattern: initiate → upload to GCS → complete → approve. Approve triggers the Jira ADF comment post (see `./integrations/jira.md`).

| Method | Path | Purpose | Scope |
|---|---|---|---|
| POST | `/ticket-attachments/initiate-upload` | Create `TicketAttachment` row (state=Draft); return GCS resumable-upload URL | `ticket.write` |
| POST | `/ticket-attachments/{id}/complete-upload` | Validate MD5; flip `state=Approved` (triggers Object Action) | `ticket.write` |
| GET | `/ticket-attachments/by-id/{id}/download` | Return signed download URL (expires 15 min) | `ticket.read` |
| DELETE | `/ticket-attachments/{id}` | Set `state=Trashed`; GCS drain happens in scheduled task | `ticket.write` |

### 2.4 Publisher asset uploads (GCS-backed)

Same pattern as ticket attachments. Separate endpoints so scope separation is clean.

| Method | Path | Purpose | Scope |
|---|---|---|---|
| POST | `/publisher-assets/{id}/attachments/initiate-upload` | | `publisher.write` |
| POST | `/publisher-assets/{id}/attachments/{attachmentId}/complete-upload` | | `publisher.write` |

### 2.5 Jira bridge

| Method | Path | Purpose | Scope |
|---|---|---|---|
| GET | `/jira/issue/{issueKey}` | Live Jira fetch (with 1h cache); updates linked `SupportTicket.cached*` | `ticket.read` |
| DELETE | `/jira/cache` | Admin-only cache flush | `admin` |
| GET | `/jira/security-vulnerabilities/{path...}` | Read-only LSV project proxy | `ticket.read` |
| POST | `/jira/webhook` | **Inbound** — Jira webhook for issue-status changes; updates `SupportTicket.cached*` opportunistically | HMAC-verified, no OAuth2 |

### 2.6 Liferay Cloud

| Method | Path | Purpose | Scope |
|---|---|---|---|
| POST | `/console/provisioning/{subscriptionId}` | Deploy DXP instance | `console.write` |
| GET | `/console/subscriptions/{subscriptionId}` | Status check | `console.read` |
| POST | `/analytics/provision/{subscriptionId}` | Provision Analytics Cloud workspace | `analytics.write` |

### 2.7 Entitlements

| Method | Path | Purpose | Scope |
|---|---|---|---|
| POST | `/entitlements/recompute` | Admin-trigger full `EntitlementSync` | `admin` |
| POST | `/entitlements/recompute?definitionCode={code}` | Scoped to one definition | `admin` |
| POST | `/entitlements/recompute?accountEntryId={id}` | Scoped to one account | `admin` |

### 2.8 Health

| Method | Path | Purpose | Scope |
|---|---|---|---|
| GET | `/ready` | Liveness + dependency status | none (public) |
| GET | `/health` | Deep health (Jira, GCS, DB reachable) | none (public) |

### 2.9 Error contract

All endpoints return:

```json
{ "error": { "code": "string", "message": "string", "details": {} } }
```

HTTP status mapping:
- 400 validation failure
- 401 missing/invalid OAuth2 token
- 403 scope insufficient
- 404 target not found
- 409 conflict (uniqueness, state-transition)
- 429 rate limited (GCS upload initiate — 10/min per user)
- 5xx dependency failure (Jira, GCS, Liferay Cloud)

---

## 3. Webhooks / async subscribers

### 3.1 Salesforce Pub/Sub (inbound)

Not a REST endpoint — `liferay-one-etc-spring-boot` hosts a Google Pub/Sub subscriber on the existing SF topic (D12). Contract in [`./integrations/salesforce.md`](./integrations/salesforce.md).

### 3.2 Commerce listeners (internal Liferay event bus)

Not HTTP — Spring beans wired to Commerce's internal event bus (no external integration). Commerce runs inside the same Liferay instance. See [`./provisioning-hub.md`](./provisioning-hub.md) for the listener contracts that drive license generation and entitlement sync.

### 3.3 Jira webhook (inbound)

`POST /o/one/v1/jira/webhook` — Jira hits this when issue state changes. HMAC-verified (shared secret in `liferay-one-instance-settings`). Updates `SupportTicket.cached*` immediately so the 1h TTL doesn't delay fresh data.

---

## 4. OAuth2 configuration

Each calling system is represented as a **Liferay OAuth2 Application** with client-credentials flow and a narrow scope set. Applications seeded via `liferay-one-instance-settings`.

### 4.1 Scopes

| Scope | Grants |
|---|---|
| `customer.read` | Read AccountEntry / Team / ExternalLink via headless |
| `customer.write` | Write to AccountFlag / AccountNote / Team / AccountEntry custom fields |
| `subscription.read` | Read CommerceSubscriptionEntry and `TrialProvisioning` |
| `subscription.write` | Write subscription custom fields; call trial lifecycle endpoints |
| `license.read` | Read LicenseKey (non-key fields); download own-account key |
| `license.admin` | Generate / revoke / regenerate keys |
| `entitlement.read` | Read Entitlement |
| `ticket.read` | Read SupportTicket / TicketAttachment / Jira proxy GETs |
| `ticket.write` | Create tickets, upload attachments |
| `publisher.read` | Read Publisher and assets |
| `publisher.write` | Write Publisher, upload asset attachments |
| `console.read` / `console.write` | DXP Console proxy |
| `analytics.read` / `analytics.write` | Analytics Cloud proxy |
| `admin` | Trigger recompute endpoints, cache flush, internal ops |

### 4.2 Registered applications

| Application | Scopes | Caller |
|---|---|---|
| `one-salesforce-subscriber` | `customer.write`, `subscription.write` | The D12 Pub/Sub subscriber identity (runs in-process, still holds a scoped client) |
| `one-jira-webhook` | `ticket.write` | Jira webhook POSTs |
| `one-custom-element` | All scopes (narrowed per-user by role at request time) | `liferay-one-custom-element` — user-agent OAuth2 flow. Effective scope per request is the intersection of the app's scope set with the caller's role grants. |
| `one-license-generator` | `license.admin` | Internal license-gen service (same process, separate identity for audit) |
| `one-liferay-cloud-callback` | `subscription.write`, `license.admin` | Liferay Cloud callbacks for trial lifecycle |

Service-to-service applications (subscriber, webhook, license-generator, Liferay Cloud callback) are **separate identities** for audit clarity — the OAuth2 authorization log tells us which system did what, and a compromised credential has a narrow blast radius. The browser-facing `one-custom-element` is a single identity by necessity (one React bundle per user session); per-request scope narrowing by role is the substitute for separate audiences. Revisit if audit pressure requires tighter splits.

### 4.3 Token lifetime

- Client-credentials tokens: 1h expiry, silently refreshed by callers.
- User-agent tokens: Liferay session-bound.
- Webhook tokens (Jira): long-lived, rotate quarterly, stored in `liferay-one-instance-settings` secrets.

---

## 5. Retired APIs

Per system-spec §5.3:

- **Koroneiki Phloem** `/o/koroneiki-rest/v1.0/*` — downstream callers migrate to this workspace's headless + custom endpoints. See [`./migration.md §Phase 2 caller cut-over`](./migration.md).
- **Provisioning portlet JSON-WS** — replaced by Admin UI + headless.
- **Zendesk integration endpoints** — dropped with provisioning phase-3 retire.

### 5.1 Caller re-pointing checklist

Before phase 2 concludes, inventory every caller of Koroneiki Phloem:

| Caller | Today | New |
|---|---|---|
| Provisioning DossieraCreateMessageSubscriber | Phloem REST | Retires — logic absorbed by D12 subscriber |
| Marketplace `/koroneiki` controllers | Phloem REST | Headless `/o/c/accountEntries` etc. |
| Support `AccountsRestController.scheduledHeatTagUpdate` | Phloem + Jira Assets | Headless + Jira |
| LCS, osb-entity-web | Phloem REST | N/A — both retiring |
| Any Salesforce GCF | Phloem REST | Direct workspace REST (new OAuth2 app) |
| Customer-portal UI | Phloem | Headless via `one-custom-element` app (Support features) |

---

## Cross-references

- [`./data-model.md`](./data-model.md) — per-Object headless path + field details (ERC registry, friendly-URL separators).
- [`./business-logic.md`](./business-logic.md) — the Object Actions that call these endpoints.
- [`./integrations/`](./integrations/) — per-integration REST/webhook/subscriber detail.
- [`./provisioning-hub.md`](./provisioning-hub.md) — internal provisioning flows (Commerce events, license generation).
- [`./migration.md`](./migration.md) — cut-over sequencing for external callers.

## Open questions

1. **Rate limiting.** Liferay headless endpoints don't rate-limit by default. The GCS initiate-upload endpoint needs limiting (10/min/user) — implement as a Spring Boot filter.
2. **Caching.** Jira proxy 1h cache lives in Spring Boot memory — restart loses it. Consider Redis or Liferay's clustered cache if the cost matters. Defer until load-tested.
3. **Custom element auth.** Marketplace + Support + Admin custom elements call the workspace from the browser. User-agent OAuth2 flow piggybacks on Liferay session. Confirm CSRF posture for mutating endpoints.
4. **Webhook signing.** Jira webhook HMAC secret rotation is manual today. Schedule a quarterly rotation calendar entry.
