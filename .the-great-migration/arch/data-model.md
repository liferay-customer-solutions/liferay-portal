# one.liferay — Data Model

## Liferay One Objects

### Account Management

#### Account

ERC: core extension of Liferay `AccountEntry`

| Field | Notes |
|---|---|
| PK `accountId` | |
| FK `sfdcAccountId` | |
| `name`, `type`, `parentAccountId` | `parentAccountId` must not create cycles |
| `koroneikiAccountCode` | unique CI, uppercased, max 75; replaces `Koroneiki_Account.code` |
| `region` | picklist: Americas, EMEA, APAC, … |
| `tier` | picklist: Platinum, Gold, Silver, Bronze, Trial, Community |
| `status` | picklist: Active, Suspended, Cancelled, Expired |
| `internal` | Boolean; Liferay employee test accounts |
| `profileEmailAddress` | primary notification address, separate from login email |
| `salesforceId` | unique when non-null, 15/18-char SF ID |
| `dossieraId` | **migration-only** — drop after cut-over |
| `maxRequestors` | cap on requestor seats; null = unlimited |
| ★ `creditLimit` | amount; Liferay finance-set, A/R risk control |
| ★ `availableCredit` | |
| ★ `creditStatus`, `holdReason` | |

> ★ = new fields added in this model.

**Migration source**
- `kor.Koroneiki_Account` — 18,390 rows
- `e5a2_lpartition_1860468.O_KoroneikiAccount` — 2,313 rows (side-car; reconcile against Koroneiki primary)
- `kor.Koroneiki_ExternalLink` (domain=salesforce/dossiera) — feeds `salesforceId` / `dossieraId` and dual-writes to `ExternalLink`
- `logoId` — **not migrated** (deprecated)

---

#### AccountFlag (`ONE_CUS_ACCNT_FLAG`)

Account-scoped flag records. Account-restricted.

| Field | Notes |
|---|---|
| PK `accountFlagId` | |
| FK `accountId` | |
| `flagCode` | picklist |
| `flagValue` | picklist |
| `startDate`, `endDate` | |
| `note` | |
| `finished` | Boolean |
| `accountKey` | denormalized account identifier |

**Migration source**
- `e5a2_lpartition_1860468.O_AccountFlag` — 557 rows (support workspace)

---

#### AccountNote (`ONE_CUS_ACCNT_NOTE`)

Internal notes attached to an account. Account-restricted. Replaces Koroneiki `AccountNote`.

| Field | Notes |
|---|---|
| PK `accountNoteId` | |
| FK `accountId` | |
| `summary` | |
| `content` | Clob |
| `format` | picklist |
| `type` | picklist |
| `priority` | picklist |
| `status` | picklist |
| `creatorName`, `creatorUID` | frozen at creation; do not overwrite on edit |
| `modifierName`, `modifierUID` | |

---

#### BannedEmailDomain (`ONE_REF_BANNED_EMAIL`)

Block list of email domains used to reject account creation.

| Field | Notes |
|---|---|
| PK `bannedEmailDomainId` | |
| `domain` | e.g. `mailinator.com`; unique |
| `reason` | |
| `addedAt` | |
| `addedByUserId` | |

**Migration source**
- `e5a2_lpartition_1860468.O_BannedEmailDomain` — ~4,800 rows (customer workspace)

---

#### ExternalLink (`ONE_CUS_EXT_LINK`)

Polymorphic external-system link. Replaces `kor.Koroneiki_ExternalLink`.

| Field | Notes |
|---|---|
| PK `externalReferenceId` | |
| `domain` | external system name (e.g. `salesforce`, `dossiera`, `jira`) |
| `entityName`, `entityId` | external record identifier |
| `ownerClassName`, `ownerClassPK` | the one.liferay record being linked (polymorphic FK) |
| `label` | human-readable label |

**Migration source**
- `kor.Koroneiki_ExternalLink` — 254,202 rows

---

#### Publisher (`ONE_MKT_PUBLISHER`)

Marketplace publisher profile. Account-restricted. Merges marketplace `PublisherDetails`.

| Field | Notes |
|---|---|
| PK `publisherId` | |
| FK `accountId` | |
| `publisherName` | |
| `slug` | URL-safe unique identifier |
| `emailAddress` | |
| `description` | Clob |
| `logo` | attachment |
| `commerceCatalogId` | linked Commerce catalog |
| `approvalStatus` | picklist |
| `payoutMethod` | |
| `payoutReference` | |
| `website` | |

**Migration source**
- `e5a2_lpartition_11706165.O_PublisherDetails` — 134 rows (marketplace workspace)

---

#### PublisherAsset (`ONE_MKT_PUB_ASSET`)

A versioned marketplace asset (app listing). Child of Publisher.

| Field | Notes |
|---|---|
| PK `publisherAssetId` | |
| FK `publisherId` | |
| `appName` | |
| `version` | |
| `releaseNotes` | Clob |
| `compatibility` | supported Liferay versions |
| `publishStatus` | picklist |

**Migration source**
- `e5a2_lpartition_11706165.O_PublisherAssets` — 16 rows

---

#### PublisherAssetAttachment (`ONE_MKT_PUB_ASSET_ATTACH`)

GCS-backed file artifact for a PublisherAsset. 200 MB max per file.

| Field | Notes |
|---|---|
| PK `attachmentId` | |
| FK `publisherAssetId` | |
| `fileName`, `fileSize` | |
| `md5Checksum` | |
| `gcsBucket`, `gcsObject` | storage coordinates |
| `uploadStatus` | picklist |
| `processed` | Boolean |
| `sourceCode` | Boolean; flags source-code archives |

**Migration source**
- `e5a2_lpartition_11706165.O_PublisherAssetAttachments` — 6 rows

---

#### PublisherSalesSummary (`ONE_MKT_PUB_SALES_SUM`)

Quarterly payout summary per publisher.

| Field | Notes |
|---|---|
| PK `salesSummaryId` | |
| FK `publisherId` | |
| `quarter` | e.g. `2024-Q3` |
| `grossAmount`, `netAmount` | BigDecimal |
| `currency` | |
| `orderCount` | |
| `paidBy` | |
| `paidDate` | |
| `payoutReference` | |

**Migration source**
- `e5a2_lpartition_11706165.O_PublisherSalesSummary` — 8 rows

---

#### Report (`ONE_MKT_REPORT`)

Pre-computed report snapshot produced by scheduled tasks. Read-only by UI; written only by the `ProjectsUsingMarketplaceReport` scheduler.

| Field | Notes |
|---|---|
| PK `reportId` | |
| `reportType` | picklist: `projects_using_marketplace` · (future types added here) |
| `periodStart`, `periodEnd` | date range the report covers |
| `payload` | Clob; JSON blob of report rows |
| `generatedAt` | datetime |
| `status` | `Generating` · `Ready` · `Failed` |

**No legacy equivalent.** Replaces the ad-hoc Marketplace `_processProjectsUsingMarketplaceApps` output.

---

#### RequestPublisherAccount (`ONE_MKT_REQ_PUB_ACCNT`)

Onboarding form submission for publisher account approval.

| Field | Notes |
|---|---|
| PK `requestId` | |
| `firstName`, `lastName` | |
| `emailAddress` | |
| `phoneNumber`, `intlCode`, `extension` | |
| `requestDescription` | Clob |
| `companyName` | |
| `status` | picklist |

**Migration source**
- `e5a2_lpartition_11706165.O_RequestPublisherAccount` — 12 rows

---

#### Team (`ONE_CUS_TEAM`)

Account-scoped team for grouping contacts. Account-restricted. Replaces `kor.Koroneiki_Team`.

| Field | Notes |
|---|---|
| PK `teamId` | |
| FK `accountId` | |
| `name` | |
| `description` | |
| `system` | Boolean; system-managed teams (e.g. All Members) cannot be deleted |
| `teamKey` | stable external ID |

**Migration source**
- `kor.Koroneiki_Team` — 18,570 rows
- `kor.Koroneiki_ContactTeamRole` — 29,262 rows → Team ↔ User membership (role-within-team dropped)
- `kor.Koroneiki_TeamRole` — 2 rows → **dropped**
- `kor.Koroneiki_TeamAccountRole` — 39 rows → **dropped**

---

### Subscription Management

> **D4 amendment:** the system-spec proposed using OOTB Commerce subscription objects for the full subscription lifecycle. In practice, Commerce's native subscription model doesn't cover all required fields (`developerCount`, `billingCadence`, multi-line billing, usage metering, credit holds, or invoice tracking). The design uses Commerce for the **order and catalog layer** (`CommerceOrder`, `CommerceOrderItem`, `CPDefinition`) but adds custom `Subscription` and `SubscriptionItem` Objects for the subscription contract and billing fields on top. Commerce subscription lifecycle events (Active / Expired / Cancelled) still drive the provisioning and entitlement flows via internal event listeners. `arch/` wins over the system-spec on this point per the conventions in `arch/README.md`.

#### Commerce Order Item

Uses Liferay Commerce `CommerceOrderItem` system Object with custom fields.

| Field | Notes |
|---|---|
| PK `cpInstanceId` | |
| FK `subscriptionId` | via `CommerceSubscriptionEntry` |
| Mirror of SFDC Product | nightly sync |
| `koroneikiProductPurchaseKey` | **migration-only** |
| `salesforceOpportunityId` | populated by Salesforce Pub/Sub subscriber |
| | drives marketplace listings |

**Migration source**
- `kor.Koroneiki_ProductPurchase` — item-level fields (65,271 rows)

---

#### Commerce Product

Uses Liferay Commerce `CPDefinition` system Object with custom fields.

| Field | Notes |
|---|---|
| PK `cpInstanceId` | |
| FK `sfdcProductId` | |
| Mirror of SFDC Product Catalog | nightly sync |
| `isPrimary` | Boolean; required for 5 entitlement rules; default `false` |
| `licenseKeyProductVersion` | version string embedded in generated keys (`dxp-7.4`, `dxp-2024.q3`, …); null for non-key products |
| `productFamily` | picklist: DXP · Portal · Analytics · Commerce · AIHub · CMP · Partner · Support · Training · Other |
| `koroneikiProductKey` | **migration-only** — original `Koroneiki_ProductEntry.productKey` |
| `metricCoverage` | rules (from SFDC Product Catalog) |
| | drives marketplace listings |

**Migration source**
- `kor.Koroneiki_ProductEntry` — 505 rows
- `kor.Koroneiki_ProductField` — 260,826 name/value pairs; extract distinct `name` set to determine which become structured fields vs. dropped

---

#### Consumption Metric

| Field | Notes |
|---|---|
| PK `metricId`, `code` | |
| `unit`, `aggregation` | e.g. `count` / `sum` |
| `period` | metering window |
| `version` | immutable history; new version on rule change |

---

#### Credit Hold

| Field | Notes |
|---|---|
| PK `creditHoldId` | |
| FK `accountId` | |
| `reason` | `limit` · `aging` · `manual` |
| `openedAt`, `closedAt` | |
| `blocksOrder`, `blocksProvision` | control-flow flags to downstream systems |

> Finance-set A/R risk control. Auto-opened by NAV via `nav.account.aged` PubSub event.

**No legacy equivalent.** Previously handled by hard-coded Provisioning logic.

---

#### Entitlement (`ONE_ENT_ENTITLEMENT`)

Materialized grant records. Do NOT port rows from legacy — regenerate on first EntitlementSync run.

| Field | Notes |
|---|---|
| PK `entitlementId` | |
| FK `entitlementDefinitionId` | |
| `entitlementDefinitionCode` | denormalized for query performance |
| `targetClassName`, `targetClassPK` | polymorphic FK to the granted resource |
| FK `subscriptionItemId` | |
| FK `subscriptionId` | denormalized for query performance |
| FK `metricId` | for metered entitlements |
| `prepaidQuota`, `overageRate` | |
| `enforcement` | |
| `grantedAt`, `lastConfirmedAt` | |
| `effectiveStart`, `effectiveEnd` | |

**62 active rules** (all retained):
- 59 object-filter rules — "account has active subscription for product X." Five rules (#06 Developer Tools, #08 DXP, #21 PaaS, #24 SaaS, #54–59 Contact rules) require `isPrimary=true` on the Commerce Product.
- 3 scripted rules — #02 cloudNativeWithoutSaaS (AWS/Azure/Google Ready AND NOT SaaS/PaaS), #14 futureSubscriptionCoverageGap (future-dated gap detection), #62 partnerContactWithGrantedAccount (cascades on #40 Account:Partner; 2-phase execution order required).

**Execution phases**
- Phase A — AccountEntry targets (no dependencies)
- Phase B — User targets; topological order respecting `cascadeAfter`; max 3 passes; rule #62 waits on #40

**Migration source**
- `kor.Koroneiki_Entitlement` — 9,311 rows; regenerated on first EntitlementSync run (do not migrate)
- `kor.Koroneiki_EntitlementDefinition` — 62 rows → `EntitlementDefinition` Object (migrate rule definitions, not grants)
- `[$NOW$]` token in legacy SQL → replaced by entitlement sync execution time

---

#### EntitlementDefinition (`ONE_ENT_DEFINITION`)

Entitlement rule definitions. 62 active rules ported from `Koroneiki_EntitlementDefinition`.

| Field | Notes |
|---|---|
| PK `entitlementDefinitionId` | |
| `name` | |
| `code` | unique; stable cross-system identifier |
| `description` | |
| `targetClassName` | the Object class the grant targets (AccountEntry or User) |
| `ruleType` | `filter` · `scripted` |
| `ruleBody` | JSON (see schema below) |
| `cascadeAfter` | optional FK to another `EntitlementDefinition`; enforces execution order |
| `status` | `Active` · `Inactive` |
| `legacyKoroneikiId` | **migration-only** — original `Koroneiki_EntitlementDefinition.id` |

**Filter rule `ruleBody` shape**

```json
{
  "className": "com.liferay.object.model.ObjectEntry",
  "filter": "subscriptionStatus eq 'active' and isPrimary eq true",
  "relationshipChain": ["accountToSubscription", "subscriptionToProduct"]
}
```

**Scripted rule `ruleBody` shape**

```json
{
  "function": "cloudNativeWithoutSaaS",
  "scriptParams": {}
}
```

Registered scripted functions: `cloudNativeWithoutSaaS` (#02), `futureSubscriptionCoverageGap` (#14), `partnerContactWithGrantedAccount` (#62).

**Migration source**
- `kor.Koroneiki_EntitlementDefinition` — 62 rows; SQL `WHERE` clauses become Object filter or script expressions

---

#### Invoice Request

| Field | Notes |
|---|---|
| PK `invoiceRequestId` | |
| FK `accountId`, `subscriptionId` | |
| `period`, `lines` | prepaid + overage line items |
| `taxJurisdiction`, `totalGross` | |
| `status`, `navInvoiceId` | `navInvoiceId` populated by NAV after posting |

**No legacy equivalent.** Replaces ad-hoc RabbitMQ-driven async invoice generation in Provisioning.

---

#### Payment Method

| Field | Notes |
|---|---|
| PK `paymentMethodId` | |
| FK `accountId` | |
| `type` | `eft` · `po` · future: `stripe_card` · `stripe_ach` |
| `isDefault`, `status` | |

---

#### Spend Limit

| Field | Notes |
|---|---|
| PK `spendLimitId` | |
| FK `subscriptionId` | |
| FK `subscriptionItemId`, `metricId` | optional finer scope |
| `period` | `month` · `quarter` · `term` |
| `limitAmount`, `alertThresholds` | |
| `enforcement` | `alert` · `block_overage` · `suspend` |
| `setBy` | customer admin |

> Liferay credit limit (Account.creditLimit) takes precedence — most restrictive wins at runtime.

**No legacy equivalent.** New capability.

---

#### Subscription

| Field | Notes |
|---|---|
| PK `subscriptionId` | |
| FK `accountId` | |
| FK `sfdcContractId` | |
| `status`, `startDate`, `endDate` | |
| `originalEndDate` | sales-original end before extensions; defaults to `endDate` |
| `billingCadence`, `currency` | |
| `renewalDate` | |
| `developerCount` | cap on developer seats; null = unlimited |
| `koroneikiProductPurchaseKey` | **migration-only** — original purchase key for lookup continuity |

**Migration source**
- `kor.Koroneiki_ProductPurchase` — 65,271 rows (subscription-enabled items become `CommerceSubscriptionEntry`; one-time items become `CommerceOrderItem`)

---

#### Subscription Item

| Field | Notes |
|---|---|
| PK `subscriptionItemId` | |
| FK `subscriptionId` | |
| FK `productId` | → Commerce Product |
| FK `quoteLineId` | → SFDC CPQ Quote Line |
| `quantity`, `unitPrice` | `unitPrice` locked at quote acceptance |
| `effectiveStart`, `effectiveEnd` | |
| `addedBy` | |
| `billings`, `renewal`, `addOn` | flags |
| `koroneikiProductPurchaseKey` | **migration-only** |
| `salesforceOpportunityId` | populated by Salesforce Pub/Sub subscriber |

**Migration source**
- `kor.Koroneiki_ProductPurchase` — line-level fields → `CommerceOrderItem` custom fields
- `kor.Koroneiki_ProductField` — 260,826 name/value pair rows; distinct `name` set maps to structured fields on Subscription Item, Commerce Product, and Environment

---

#### Usage Event

| Field | Notes |
|---|---|
| PK `usageEventId` | |
| FK `environmentId` | |
| FK `subscriptionItemId` | |
| FK `metricId` | |
| `eventTimestamp`, `qty` | |
| `dedupKey` | idempotent; client-assigned |

---

#### Usage Report

| Field | Notes |
|---|---|
| PK `usageAggregateId` | |
| FK `subscriptionItemId` | |
| FK `environmentId`, `metricId` | |
| `periodStart`, `periodEnd` | |
| `granularity` | hourly · day · month |
| `quantity` | signed at close |

**Migration source**
- `kor.Koroneiki_ProductConsumption` — 148,901 rows; different granularity. Map to closest-matching Usage Report period; do not attempt 1:1 row conversion.

---

### Environment Management

#### Activation - File Key (`ONE_COM_LICENSE_KEY`)

Issued license key authorizing a deployment to run. New Object; replaces `Provisioning_LicenseKey`.

| Field | Notes |
|---|---|
| PK `activationKeyId` | |
| FK `environmentId` | |
| `key` | **unique; string must be preserved byte-identical from legacy** |
| `payload` | signed artifact |
| `productVersion` | e.g. `dxp-7.4`, `dxp-2024.q3`; from Commerce Product `licenseKeyProductVersion` at issuance |
| `issuedAt`, `expiresAt` | |
| `maxServers` | null = unlimited; legacy sentinel: `10000` |
| `maxDevelopers` | null = unlimited |
| `hostNames` | newline-delimited (DXP license format) |
| `ipAddresses` | newline-delimited (legacy DXP licenses) |
| `clustered` | Boolean |
| `licenseType` | Production · NonProduction · Development · Trial · Internal |
| `status` | `Active` · `Expired` · `Revoked` · `Superseded`; no transition from terminal back to `Active` |
| `legacyLicenseKeyId` | **migration-only** — original `Provisioning_LicenseKey.licenseKeyId` |

> **Preservation constraint:** Every existing `key` string must land in the new table unchanged. Regenerating keys breaks running deployments. Confirm the private-key/signature-algorithm carries over, or plan a coordinated re-issue.

**Relationship:** `CommerceSubscriptionEntry` → `LicenseKey` 1:N (`CASCADE`). `LicenseKey` → `Environment` N:1 (`DISASSOCIATE`).

**Migration source**
- `prov.Provisioning_LicenseKey` — 230,466 rows (**ownership TBD**; owning module not found in audited source — locate before phase 3)
- `prov.OSB_LicenseKey` — 201,897 legacy rows (overlap check required)
- `prov.Provisioning_CommonLicenseKey` — 476 rows (investigate; may be definition templates)
- `prov.Provisioning_LicenseEntry` — 48 rows; `Provisioning_ProductVersion` — 36 rows (likely lookup tables; fold into Commerce Product `licenseKeyProductVersion` or drop)

---

#### Activation - Heartbeat

Keyless mTLS heartbeat record for cloud-native environments.

| Field | Notes |
|---|---|
| PK `heartbeatId` | |
| FK `environmentId` | |
| `receivedAt`, `entitlementHash` | |
| `signatureValid`, `clientVersion` | |
| TTL | 1h · 24 misses → suspend environment |

---

#### DataCenter (`ONE_REF_DATA_CENTER`)

Physical or logical data center record. Merges `DXPCDataCenterRegion` and `AnalyticsCloudDataCenterLocation`.

| Field | Notes |
|---|---|
| PK `dataCenterId` | |
| FK `regionId` | → Region |
| `code` | unique short identifier |
| `name` | |
| `type` | `DXPCloud` · `AnalyticsCloud` · `LXC` |
| `cloudProvider` | `AWS` · `GCP` · `Azure` · `Liferay` |
| `providerRegion` | e.g. `us-east-1` |
| `active` | Boolean |
| `capacity` | Integer; null = unlimited |

---

#### Environment (`ONE_COM_DEPLOYMENT`)

A customer's deployment environment. Account-restricted. Replaces Support `AccountSubscription`.

| Field | Notes |
|---|---|
| PK `environmentId` | |
| FK `subscriptionId` | |
| `deploymentKey` | stable external ID, uppercased; preserved from `AccountSubscription.accountSubscriptionERC` |
| `deploymentGroupERC` | optional grouping; replaces `AccountSubscriptionGroup` |
| `type` | `cloud_native` · `on_prem` |
| `name` | picklist: Production · Non-Production · Development · Backup · HA Production |
| `instanceSize` | picklist: XS · S · M · L · XL · XXL |
| `productVersion` | DXP/Portal version string: `7.4`, `2024.q3`, … |
| `hasDisasterDataCenterRegion` | Boolean; default `false` |
| `startDate`, `endDate` | |
| `activationMode` | `file_key` · `heartbeat` |
| `region` | FK to `Region` |
| `dataCenterId` | FK to `DataCenter` |
| `status` | picklist: Active · Decommissioned · Suspended; default `Active` |
| `lastHeartbeatAt` | |
| `currentEntitlementHash` | |

**Relationships:** `AccountEntry` → `Environment` 1:N (`CASCADE`). `Environment` ↔ `CommerceSubscriptionEntry` M:N (`DISASSOCIATE`). `Environment` → `DataCenter` N:1 (`PREVENT`). `Environment` → `Region` N:1 (`PREVENT`).

**Migration source**
- `e5a2_lpartition_1860468.O_AccountSubscription` — 4,572 rows (primary; maps `name` / `instanceSize` / dates directly)
- `e5a2_lpartition_1860468.O_AccountSubscriptionGroup` — 3,073 rows → `deploymentGroupERC` field (no separate Object)
- `e5a2_lpartition_1860468.O_AccountSubscriptionTerm` — 13 rows → **dropped**
- `e5a2_lpartition_1860468.O_DXPCloudEnvironment` — 5 rows → fold fields into Environment
- `e5a2_lpartition_1860468.O_LXCEnvironment` / `O_LiferayExperienceCloudEnvironment` — 9 rows → fold into Environment
- `e5a2_lpartition_1860468.O_CloudNativeEnvironment` — 16 rows → fold into Environment

---

#### OrderType (`ONE_MKT_ORDER_TYPE`)

Reference Object driving post-purchase provisioning branching. One row per commerce order flow.

| Field | Notes |
|---|---|
| PK `orderTypeId` | |
| `externalReferenceCode` | stable key consumed by provisioning logic |
| `displayName` | |
| `provisioningFlow` | `trial-cloud` · `paid-cloud` · `free-activation` · `ai-hub-beta` · `paid-onprem` · `partner-self-service` |
| `trialDurationDays` | null for non-trial flows |
| `requiresManualReview` | Boolean |
| `provisionsAnalytics` | Boolean |
| `provisionsConsole` | Boolean |
| `defaultDeploymentName` | picklist value written to the new Environment |
| `notificationTemplateIn` | notification template ERC sent on provisioning start |
| `notificationTemplateOut` | notification template ERC sent on provisioning completion |

---

#### Region (`ONE_REF_REGION`)

Geographic/support region definitions. Replaces hard-coded support-region map.

| Field | Notes |
|---|---|
| PK `regionId` | |
| `code` | unique; seed values: AMER, EMEA, APAC, LATAM, ANZ, JPN, CHN, IND |
| `name` | |
| `timeZone` | IANA time zone string |
| `supportCoverage` | business-hours description |
| `active` | Boolean |

---

#### TrialProvisioning (`ONE_MKT_TRIAL_PROV`)

Tracks cloud trial provisioning state per Commerce order. Replaces JSON blobs on `CommerceOrder`.

| Field | Notes |
|---|---|
| PK `trialProvisioningId` | |
| FK `commerceOrderId` | |
| FK `orderTypeId` | |
| `trialEndDate`, `trialNotifyEndDate` | |
| `provisioningStatus` | picklist |
| `cloudInstanceId`, `cloudRegion`, `cloudTier` | cloud environment coordinates |
| `koroneikiProjectIds` | **migration-only** — original Koroneiki project IDs |
| `provisioningPayload` | Clob; raw cloud API request/response snapshot |
| `errorMessage` | Clob; last error if provisioning failed |

---

### Ticket Management

#### BusinessEvent (`ONE_SUP_BIZ_EVENT`)

Go-live event tracking for customer implementations. Account-restricted.

| Field | Notes |
|---|---|
| PK `businessEventId` | |
| FK `deploymentId` (→ Environment) | |
| FK `primaryContactUserId`, `liferayOwnerUserId` | |
| `eventStatus` | picklist |
| `eventType` | picklist |
| `heat` | picklist |
| `description` | Clob |
| `currentLiferayVersion`, `targetLiferayVersion` | |
| `expectedGoLiveDateTime`, `actualGoLiveDateTime` | datetime |
| `dxpEnabled`, `dxpCloudEnabled`, `commerceEnabled` | Boolean flags |
| `analyticsEnabled`, `contentEnabled`, `experienceEnabled` | Boolean flags |
| `aiHubEnabled`, `customElementsEnabled` | Boolean flags |
| `associatedTicketIds` | Clob; comma-separated Jira keys |
| `estimatedUserCount` | Integer |
| `geoScope` | |

**Migration source**
- `e5a2_lpartition_1860468.O_BusinessEvent` — 120 rows

---

#### BusinessEventVersion (`ONE_SUP_BIZ_EVENT_VER`)

Immutable change log for BusinessEvent. Cascade-deletes with parent.

| Field | Notes |
|---|---|
| PK `businessEventVersionId` | |
| FK `businessEventId` | |
| `change` | what changed |
| `comment` | |
| `changedAt` | datetime |
| `changedByUserId` | |
| `diffSnapshot` | Clob; JSON diff |

> Rows are immutable — no update after insert.

**Migration source**
- `e5a2_lpartition_1860468.O_BusinessEventVersion` — 347 rows

---

#### CallbackRequest (`ONE_SUP_CALLBACK_REQ`)

Customer callback scheduling request.

| Field | Notes |
|---|---|
| PK `callbackRequestId` | |
| `name`, `emailAddress` | |
| `phoneNumber`, `countryCode` | |
| `description` | Clob |
| `relatedTicketIds` | Clob; comma-separated Jira keys |
| `status` | picklist |

**Migration source**
- `e5a2_lpartition_1860468.O_CallbackRequest` — 50 rows

---

#### ReplacementActivationKeyRequest (`ONE_SUP_REPL_ACT_KEY`)

Support request for replacing an activation key.

| Field | Notes |
|---|---|
| PK `requestId` | |
| `companyName` | |
| `contactEmailAddress` | |
| `activeLiferaySubscription` | |
| `clustered` | Boolean |
| `explainReplacement` | Clob |
| `acknowledgement` | Boolean; customer must check box |
| `status` | picklist |

**Migration source**
- `e5a2_lpartition_1860468.O_ReplacementActivationKeyRequest` — 11 rows

---

#### SupportTicket (`ONE_SUP_TICKET`)

Thin wrapper around a Jira issue for customer-visible ticket tracking. Account-restricted. Created lazily — no bulk backfill.

| Field | Notes |
|---|---|
| PK `supportTicketId` | |
| FK `deploymentId` (→ Environment) | optional |
| `jiraIssueKey` | unique; e.g. `LRHC-1234` |
| `jiraProject` | `LRHC` · `LRFLS` · `LSV` |
| `subject` | |
| `statusCached`, `priorityCached` | refreshed by Jira sync; not editable via UI |
| `reporterEmail`, `assigneeEmail` | |
| `lastSyncedAt` | datetime |

---

#### SupportTicketEscalation (`ONE_SUP_TICKET_ESC`)

Escalation record for a support ticket or set of tickets.

| Field | Notes |
|---|---|
| PK `escalationId` | |
| `ticketIds` | Clob; comma-separated Jira keys |
| `description` | Clob |
| `customerEmailAddress` | |
| `phoneNumber` | |
| `status` | picklist |

**Migration source**
- `e5a2_lpartition_1860468.O_SupportTicketEscalation` — 49 rows

---

#### TicketAttachment (`ONE_SUP_TICKET_ATTACH`)

GCS-backed file attachment for a support ticket. Account-restricted.

| Field | Notes |
|---|---|
| PK `attachmentId` | |
| FK `supportTicketId` | |
| `jiraIssueKey` | denormalized for Jira sync |
| `fileName`, `fileSize` | |
| `md5Checksum` | |
| `gcsBucket`, `gcsObject` | storage coordinates |
| `state` | `Draft` · `Approved` · `Trashed` |
| `draftCommentBody` | Clob; pending Jira comment text |
| `storageProvider` | `GCS` (drop `zendeskTicketId` — not migrated) |

**Migration source**
- `e5a2_lpartition_1860468.O_TicketAttachment` — 75 rows

---

## External Services

> **liferay-one IS the Provisioning Hub.** It is not an external system — it is the service that orchestrates provisioning by calling Liferay Cloud, the Console, Analytics Cloud, and the license-signing infrastructure. See [`../provisioning-hub.md`](../provisioning-hub.md) for the full design.

| Entity | Role |
|---|---|
| **Decoupled Deploy Tools** | Legacy on-prem; provision LXP/AIHub etc. via file key issued by liferay-one |
| **Cloud-Native Deploy Tools** | Helm charts + Kubernetes operators; replaces legacy Cloud API; registers Environment on deploy via the liferay-one API |
| **NAV / Microsoft Dynamics 365 BC** | Finance system. liferay-one posts `InvoiceRequest` data outbound; NAV returns `navInvoiceId` on posting and pushes A/R aging events back via Pub/Sub `nav.account.aged`, which auto-opens `CreditHold`. See [`integrations/nav.md`](./integrations/nav.md). |

---

## ERC and FriendlyURL Registry

Friendly URL separators are 4 lowercase letters, unique across all Objects in the workspace.

| Object | ERC | Separator | Source |
|---|---|---|---|
| `AccountFlag` | `ONE_CUS_ACCNT_FLAG` | `cpaf` | ported (customer) |
| `AccountNote` | `ONE_CUS_ACCNT_NOTE` | `cpan` | new |
| `BannedEmailDomain` | `ONE_REF_BANNED_EMAIL` | `cpbd` | ported (customer) |
| `BusinessEvent` | `ONE_SUP_BIZ_EVENT` | `cpbe` | ported (customer) |
| `BusinessEventVersion` | `ONE_SUP_BIZ_EVENT_VER` | `cpbv` | ported (customer) |
| `CallbackRequest` | `ONE_SUP_CALLBACK_REQ` | `cpcr` | ported (customer) |
| `DataCenter` | `ONE_REF_DATA_CENTER` | `cpdc` | new |
| `Entitlement` | `ONE_ENT_ENTITLEMENT` | `cpen` | new |
| `EntitlementDefinition` | `ONE_ENT_DEFINITION` | `cped` | new |
| `Environment` (Deployment) | `ONE_COM_DEPLOYMENT` | `cpdp` | new |
| `ExternalLink` | `ONE_CUS_EXT_LINK` | `cpel` | new |
| `LicenseKey` (Activation - File Key) | `ONE_COM_LICENSE_KEY` | `cplk` | new |
| `OrderType` | `ONE_MKT_ORDER_TYPE` | `cpot` | new |
| `Publisher` | `ONE_MKT_PUBLISHER` | `cppu` | new |
| `PublisherAsset` | `ONE_MKT_PUB_ASSET` | `cppa` | new |
| `PublisherAssetAttachment` | `ONE_MKT_PUB_ASSET_ATTACH` | `cpaa` | new |
| `PublisherSalesSummary` | `ONE_MKT_PUB_SALES_SUM` | `cpss` | new |
| `Region` | `ONE_REF_REGION` | `cprg` | new |
| `ReplacementActivationKeyRequest` | `ONE_SUP_REPL_ACT_KEY` | `cprk` | ported (customer) |
| `Report` | `ONE_MKT_REPORT` | `cprr` | new |
| `RequestPublisherAccount` | `ONE_MKT_REQ_PUB_ACCNT` | `cprp` | new |
| `SupportTicket` | `ONE_SUP_TICKET` | `cpst` | new |
| `SupportTicketEscalation` | `ONE_SUP_TICKET_ESC` | `cpse` | ported (customer) |
| `Team` | `ONE_CUS_TEAM` | `cpte` | new |
| `TicketAttachment` | `ONE_SUP_TICKET_ATTACH` | `cpta` | ported (customer) |
| `TrialProvisioning` | `ONE_MKT_TRIAL_PROV` | `cptp` | new |

---

## See Also

- [`business-logic.md §6`](./business-logic.md) — Billing flows (renewal, usage-based, mid-term pro-rata)
- [`integrations/salesforce.md`](./integrations/salesforce.md) — Salesforce-owned objects (Account, Contract, CPQ Quote Line, Product Catalog) and inbound Pub/Sub integration
- [`migration.md`](./migration.md) — Phase-by-phase sequencing; cut-over gates
