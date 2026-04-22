# Provisioning Hub

liferay-one **is** the Provisioning Hub. It is not a separate service — the provisioning logic lives inside `liferay-one-etc-spring-boot` and the `liferay-one-site-initializer` Object Actions. This document describes what provisioning means, what external systems it orchestrates, and how the internal flows are triggered.

The legacy `osb-provisioning` service played this role. It retires in phase 3.

---

## What "provisioning" means

Provisioning is the set of actions that happen after a customer purchases or activates a product:

1. **License key generation** — produce a signed file key (for on-prem DXP/Portal environments) or a keyless mTLS heartbeat enrollment (for cloud-native environments).
2. **Environment registration** — create or update an `Environment` Object row representing the customer's running deployment.
3. **Liferay Cloud provisioning** — for trial and paid cloud products, call DXP Cloud / Console APIs to spin up or tear down a cloud instance.
4. **Analytics Cloud provisioning** — for products that include Analytics, call the Faro provisioning API.
5. **Entitlement sync** — after any of the above, re-evaluate the customer's `EntitlementDefinition` rules and grant/revoke `Entitlement` rows.

---

## Triggering flows

Provisioning is triggered by three sources:

### A. Commerce subscription lifecycle events (most common)

Commerce runs inside the same Liferay instance. `liferay-one-etc-spring-boot` registers Spring beans on Liferay's internal Commerce event bus — no HTTP call, no external integration.

| Commerce event | liferay-one action |
|---|---|
| `CommerceSubscriptionEntry` status → `Active` | Generate license key (file or heartbeat per `Environment.activationMode`); send "subscription activated" notification; POST `/entitlements/recompute?accountEntryId={id}` |
| `CommerceSubscriptionEntry` status → `Expired` | Revoke associated `LicenseKey` rows (`status=Revoked`); POST `/entitlements/recompute` |
| `CommerceSubscriptionEntry` status → `Cancelled` | Same as Expired |
| `CommerceOrder` placed (trial flow) | Check `OrderType.provisioningFlow`; if `trial-cloud` or `paid-cloud`, invoke cloud provisioning (see §Cloud provisioning below) |

Spring bean registration:

```java
@Component
public class SubscriptionActivationListener
    implements ModelListener<CommerceSubscriptionEntry> {

    @Override
    public void onAfterUpdate(
            CommerceSubscriptionEntry original,
            CommerceSubscriptionEntry entry) {

        if (isNewlyActive(original, entry)) {
            _provisioningService.onSubscriptionActivated(entry);
        }
        else if (isNewlyTerminal(original, entry)) {
            _provisioningService.onSubscriptionTerminated(entry);
        }
    }
}
```

### B. Object Actions (customer-initiated)

| Trigger | Action |
|---|---|
| `ReplacementActivationKeyRequest.status` → `Issued` | Generate replacement key; supersede original; email artifact |
| `TrialProvisioning.onAfterAdd` | If `OrderType.provisioningFlow` in (`trial-cloud`, `paid-cloud`), call `POST /trial/provision/{subscriptionId}` (internal endpoint) |

### C. Scheduled tasks

| Task | Trigger | Action |
|---|---|---|
| `TrialLifecycleTick` | Every 6h | Expire past-end-date trials; call Liferay Cloud decommission API; flip `Environment.status=Decommissioned` |
| `EntitlementSync` | Every 15 min | Full grant/revoke pass over all Approved `EntitlementDefinition` rules |

---

## License key generation

### File key (on-prem environments)

Generated for `Environment.activationMode=file_key`.

**Signing:** The key is a signed XML/JSON artifact. The private key used for signing lives in `liferay-one-instance-settings` as a secret (`license-signing-private-key`). The signing algorithm must match what the existing license-key module uses so that existing DXP runtime license checks continue to validate without re-issue.

> **Preservation constraint:** the byte-identical key string and its signature must remain valid for all running deployments migrated from `Provisioning_LicenseKey`. Liferay owns the license-key module — extract the private key into the `license-signing-private-key` secret before starting phase 3.

**Generation flow:**

1. Resolve `CommerceSubscriptionEntry` → `SubscriptionItem` → `CPDefinition.licenseKeyProductVersion`.
2. Resolve `Environment` → `name` (Production/Non-Production/etc.) → `licenseType`.
3. Build key payload: `key`, `productVersion`, `licenseType`, `startDate`, `expiresAt`, `maxServers`, `maxDevelopers`, `hostNames`, `ipAddresses`, `clustered`.
4. Sign payload with private key → `LicenseKey.payload`.
5. Write `LicenseKey` row with `status=Active`.
6. Email key artifact to `AccountEntry.profileEmailAddress` + primary `Customer_Admin`.

**Uniqueness:** `LicenseKey.key` is globally unique (DB index). Collision on generate → retry with a different random suffix (max 3 retries, then fail-alert).

### Heartbeat / mTLS (cloud-native environments)

Generated for `Environment.activationMode=heartbeat`. Used by cloud-native (Kubernetes-hosted) DXP environments that don't use file keys.

**How it works:**

- The environment's Kubernetes operator sends a periodic mTLS heartbeat request to `liferay-one-etc-spring-boot`'s `POST /heartbeat/{environmentId}` endpoint.
- The request carries a client certificate issued by Liferay's internal CA; liferay-one validates it.
- On valid heartbeat, liferay-one writes an `Activation - Heartbeat` row and returns a signed entitlement bundle (the set of active entitlements for the environment's account, hashed).
- The operator stores the bundle locally; Liferay DXP checks it on startup and periodically.
- If 24 consecutive heartbeats are missed (TTL: 1h each → 24h window), liferay-one sets `Environment.status=Suspended`.

**`POST /heartbeat/{environmentId}` endpoint** (mutual TLS, not OAuth2):

Request: mTLS client cert identifies the environment. Body: `{ "clientVersion": "7.4.3", "reportedEntitlementHash": "sha256-..." }`.

Response: `{ "entitlementHash": "sha256-...", "validUntil": "2026-04-28T10:00:00Z", "suspended": false }`.

**Certificate authority:** Liferay builds and operates this CA as part of the liferay-one project. Design:

- **Root CA** — offline root, held in a hardware security module (HSM) or a tightly access-controlled GCP KMS key. Never used for day-to-day signing.
- **Intermediate CA** — online intermediate signed by the root. Used to issue per-environment client certificates. Rotated annually.
- **Per-environment leaf certificate** — issued by the intermediate CA when `Environment.activationMode` flips to `heartbeat`. Subject CN = `env-{environmentId}`. Short-lived: 90-day validity, auto-renewed by liferay-one before expiry.
- **Distribution** — the intermediate CA's public cert (not the root) is bundled into DXP Cloud Helm charts so Kubernetes operators can validate the entitlement bundle signature. The root public cert is published at a well-known URL for manual trust anchoring.
- **Revocation** — if an `Environment` is decommissioned, liferay-one adds its cert serial to a CRL published at `{liferayOneBaseUrl}/.well-known/heartbeat-crl.pem`. Operators check the CRL on startup.

This infrastructure must be built and operational before any cloud-native environment can use keyless activation. Plan it as a phase 3 deliverable alongside the license-key migration.

---

## Cloud provisioning

Calls to external cloud systems. The implementation lives in `liferay-one-etc-spring-boot`; the full contract for each target lives in its integration doc.

| Flow | Trigger | External system | Doc |
|---|---|---|---|
| Trial cloud spin-up | `POST /trial/provision/{subscriptionId}` | DXP Cloud / LXC Console | [`integrations/liferay-cloud.md`](./integrations/liferay-cloud.md) |
| Trial decommission | `TrialLifecycleTick` expiry | DXP Cloud / LXC Console | [`integrations/liferay-cloud.md`](./integrations/liferay-cloud.md) |
| Paid cloud spin-up | Commerce `Active` event (when `OrderType.provisioningFlow=paid-cloud`) | DXP Cloud / LXC Console | [`integrations/liferay-cloud.md`](./integrations/liferay-cloud.md) |
| Analytics Cloud provisioning | Commerce `Active` event (when `OrderType.provisionsAnalytics=true`) | Faro provisioning API | [`integrations/analytics-cloud.md`](./integrations/analytics-cloud.md) |

### Provisioning state machine (`TrialProvisioning.provisioningStatus`)

```
Pending → Provisioning → Active → Expiring → Expired
                      ↘ Failed
```

- `Pending` — order placed, provisioning not yet started (seat check or manual review queue).
- `Provisioning` — cloud API call in flight.
- `Active` — cloud instance running.
- `Expiring` — trial end notified; decommission scheduled.
- `Expired` — decommissioned.
- `Failed` — cloud API returned error; `TrialProvisioning.errorMessage` set; Slack alert fired.

---

## Entitlement sync integration

Every provisioning event that changes a customer's subscription state ends with:

```
POST /o/one/v1/entitlements/recompute?accountEntryId={id}
```

This is a fire-and-forget call (async) so the provisioning flow doesn't block on sync. The `EntitlementSync` scheduler also runs every 15 minutes as a safety net.

---

## Internal endpoints in `etc-spring-boot`

These are called by Object Actions and scheduled tasks inside liferay-one — not by external callers.

| Method | Path | Purpose |
|---|---|---|
| POST | `/internal/license-key/generate` | Generate `LicenseKey` from a `CommerceSubscriptionEntry` + `Environment` pair |
| POST | `/internal/license-key/{id}/revoke` | Revoke a key (status → Revoked) |
| POST | `/internal/heartbeat/{environmentId}` | Receive environment heartbeat; return entitlement bundle |
| POST | `/internal/trial/provision` | Spin up cloud trial instance via Liferay Cloud |
| POST | `/internal/trial/expire` | Decommission trial instance |
| POST | `/internal/entitlements/recompute` | Trigger EntitlementSync scoped to account or definition |

These paths are under `/internal/` and are **not** exposed in the public `api.md` surface. They are protected by an internal OAuth2 application (`one-internal-provisioning`) with scope `provisioning.internal`, callable only from within the same deployment.

---

## Migration from `osb-provisioning`

Phase 3 of `migration.md` covers the cut-over:

1. Extract the signing private key from the existing license-key module into `liferay-one-instance-settings` secret `license-signing-private-key` (Liferay owns the module — no external dependency).
2. Verify the signing algorithm and key format match what the existing module produces (cross-check by signing a test payload with both systems and comparing).
3. Extract and migrate `Provisioning_LicenseKey` → `LicenseKey` Object (230K rows, byte-identical `key` strings).
4. Stand up the Commerce subscription listener in shadow mode; verify license generation output matches what osb-provisioning would produce.
5. Flip to single-write; retire osb-provisioning.

---

## Open questions

1. ~~**License signing private key location.**~~ _Resolved: Liferay owns the module. Extract key to `license-signing-private-key` secret before phase 3._
2. ~~**Heartbeat CA ownership.**~~ _Resolved: liferay-one team designs and operates the CA. See the CA design above._
3. **Heartbeat endpoint TLS** — the `POST /heartbeat/{environmentId}` endpoint requires mutual TLS on a dedicated ingress path (separate from the standard OAuth2 surface). Confirm load-balancer / ingress configuration supports mTLS (e.g., a distinct port or SNI-based routing) before implementing.
4. **Seat cap enforcement** — seat availability is checked via `GET /trial/availability`. Where is the per-`OrderType` cap stored and who enforces it? Recommend adding a `maxActiveTrials` field to `OrderType` and enforcing in the trial-provision flow.
5. **Re-issue vs. extend** — on subscription renewal, supersede the existing `LicenseKey` and issue a new one (status of old → `Superseded`). This is the recommended approach for clean audit trails.
