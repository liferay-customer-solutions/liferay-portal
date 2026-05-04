# Data Model

## Liferay One Objects

---

### Account Management

#### Account (`AccountEntry` — core extension)

| Field | Type | Notes |
|---|---|---|
| PK `accountId` | long | |
| `name` | string | |
| `type` | string | |
| `parentAccountId` | long | Must not create cycles |
| `koroneikiAccountCode` | string | Unique CI, uppercased, max 75 |
| `region` | picklist | Americas, EMEA, APAC, … |
| `tier` | picklist | Platinum, Gold, Silver, Bronze, Trial, Community |
| `status` | picklist | Active, Suspended, Cancelled, Expired |
| `internal` | boolean | Liferay employee test accounts |
| `profileEmailAddress` | string | Primary notification address, separate from login email |
| `salesforceId` | string | Unique when non-null, 15/18-char SF ID |
| `maxRequestors` | int | Cap on requestor seats; null = unlimited |
| `creditLimit` | decimal | Amount; Liferay finance-set, A/R risk control |
| `availableCredit` | decimal | |
| `creditStatus`, `holdReason` | string | |

---

#### AccountFlag (`ONE_ACCNT_FLAG`)

| Field | Type | Notes |
|---|---|---|
| PK `accountFlagId` | long | |
| FK `accountId` | long | |
| `flagCode` | picklist | |
| `flagValue` | picklist | |
| `startDate`, `endDate` | datetime | |
| `note` | string | |
| `finished` | boolean | |
| `accountKey` | string | Denormalized account identifier |

---

#### AccountNote (`ONE_ACCNT_NOTE`)

| Field | Type | Notes |
|---|---|---|
| PK `accountNoteId` | long | |
| FK `accountId` | long | |
| `summary` | string | |
| `content` | Clob | |
| `format` | picklist | |
| `type` | picklist | |
| `priority` | picklist | |
| `status` | picklist | |
| `creatorName`, `creatorUID` | string | Frozen at creation; do not overwrite on edit |
| `modifierName`, `modifierUID` | string | |

---

#### BannedEmailDomain (`ONE_BANNED_EMAIL`)

| Field | Type | Notes |
|---|---|---|
| PK `bannedEmailDomainId` | long | |
| `domain` | string | e.g. `mailinator.com`; unique |
| `reason` | string | |
| `addedAt` | datetime | |
| `addedByUserId` | long | |

---

#### ExternalLink (`ONE_EXT_LINK`)

| Field | Type | Notes |
|---|---|---|
| PK `externalReferenceId` | long | |
| `domain` | string | External system name (`salesforce`, `dossiera`, `jira`) |
| `entityName`, `entityId` | string | External record identifier |
| `ownerClassName`, `ownerClassPK` | string / long | Polymorphic FK to the one.liferay record |
| `label` | string | Human-readable label |

---

#### Publisher (`ONE_PUBLISHER`)

| Field | Type | Notes |
|---|---|---|
| PK `publisherId` | long | |
| FK `accountId` | long | |
| `publisherName` | string | |
| `slug` | string | URL-safe unique identifier |
| `emailAddress` | string | |
| `description` | Clob | |
| `logo` | attachment | |
| `commerceCatalogId` | long | Linked Commerce catalog |
| `approvalStatus` | picklist | |
| `payoutMethod` | string | |
| `payoutReference` | string | |
| `website` | string | |

---

#### TrialProvisioning (`ONE_TRIAL_PROV`)

Tracks the lifecycle of a cloud trial provisioning request.

| Field | Type | Notes |
|---|---|---|
| PK `trialProvisioningId` | long | |
| FK `subscriptionId` | long | |
| FK `accountEntryId` | long | |
| `provisioningStatus` | picklist | `Pending` · `Provisioning` · `Active` · `Expiring` · `Expired` · `Failed` |
| `provisioningFlow` | string | Matches `OrderType.provisioningFlow` |
| `externalProvisioningId` | string | ID returned by Liferay Cloud / Console API |
| `startedAt` | datetime | |
| `completedAt` | datetime | |
| `failureReason` | string | |

**State machine:**

```
Pending → Provisioning → Active → Expiring → Expired
                       ↘ Failed
```

Transitions driven by `TrialProvisioning.onAfterAdd` Object Action (Pending → Provisioning → Active) and `TrialLifecycleTick` scheduled task (Active → Expiring → Expired).

---

#### Team (`ONE_TEAM`)

| Field | Type | Notes |
|---|---|---|
| PK `teamId` | long | |
| FK `accountId` | long | |
| `name` | string | |
| `description` | string | |
| `system` | boolean | System-managed teams cannot be deleted |
| `teamKey` | string | Stable external ID |

---

### Subscription Management

#### Subscription (`ONE_SUBSCRIPTION`)

| Field | Type | Notes |
|---|---|---|
| PK `subscriptionId` | long | |
| FK `accountEntryId` | long | |
| `externalReferenceCode` | string | Salesforce contract ID |
| `status` | string | |
| `startDate` | datetime | Earliest SubscriptionItem startDate |
| `endDate` | datetime | Latest SubscriptionItem endDate |
| `originalEndDate` | datetime | Sales-original end before extensions; defaults to `endDate` |
| `subscriptionOwner` | string | Email address |
| `term` | int | Contract term in months |
| `contractBillingCadence` | int | e.g. 12 (months) |
| `overageBillingCadence` | int | e.g. 3 (months) |
| `billingCadence` | string | |
| `renewalDate` | datetime | |
| `renewalState` | string | |
| `currency` | string | |
| `developerCount` | int | Cap on developer seats; null = unlimited |
| `gsOpportunity` | boolean | |
| `liferayVersion` | string | |
| `premiumService` | boolean | |
| `ldpWorkspaceName` | string | |

---

#### SubscriptionItem (`ONE_SUB_ITEM`)

| Field | Type | Notes |
|---|---|---|
| PK `subscriptionItemId` | long | |
| FK `subscriptionId` | long | |
| `externalReferenceCode` | string | Salesforce opportunity line item ID |
| `opportunityId` | string | Salesforce opportunity ID |
| `productId` | long | Commerce Product ID |
| `currency` | string | |
| `quoteLineId` | string | Salesforce CPQ value |
| `quantity` | int | |
| `unitPrice` | double | |
| `startDate` | datetime | |
| `endDate` | datetime | Contract end date |
| `effectiveEndDate` | datetime | Grace period end date |
| `type` | string | `salesforce` / `marketplace` |
| `status` | string | Approved / Canceled / On Hold |
| `cloudRegion` | string | e.g. `us-central` |
| `machineType` | string | Standard / High |
| `orderType` | string | New Business / Renewal |
| `sizing` | int | Optional; needed for licenses |
| `salesforceOpportunityId` | string | |
| `billings`, `renewal`, `addOn` | boolean | Flags |

---

#### Entitlement (`ONE_ENTITLEMENT`)

Materialized grant records.

| Field | Type | Notes |
|---|---|---|
| PK `entitlementId` | long | |
| FK `entitlementDefinitionId` | long | |
| `entitlementDefinitionCode` | string | Denormalized for query performance |
| `targetClassName`, `targetClassPK` | string / long | Polymorphic FK to the granted resource |
| FK `subscriptionItemId` | long | |
| FK `subscriptionId` | long | Denormalized |
| FK `usageDefinitionId` | long | For metered entitlements |
| `prepaidQuota` | double | Soft cap |
| `maxQuantity` | double | Hard cap |
| `overageRate` | double | |
| `enforcement` | string | |
| `grantedAt`, `lastConfirmedAt` | datetime | |
| `effectiveStart`, `effectiveEnd` | datetime | |
| `name` | string | e.g. Marketing Activities / cpu / maxservers / maxNodes |
| `grantType` | string | fixed / rollover / prepaid / metered |
| `value` | string | |

**62 active rules** — 59 object-filter rules + 3 scripted rules. Five rules (#06 Developer Tools, #08 DXP, #21 PaaS, #24 SaaS, #54–59 Contact rules) require `isPrimary=true` on Commerce Product.

---

#### EntitlementDefinition (`ONE_ENTITLEMENT_DEFINITION`)

| Field | Type | Notes |
|---|---|---|
| PK `entitlementDefinitionId` | long | |
| `name` | string | |
| `code` | string | Unique; stable cross-system identifier |
| `description` | string | |
| `targetClassName` | string | `AccountEntry` or `User` |
| `ruleType` | string | `filter` · `scripted` |
| `ruleBody` | Clob | JSON |
| `cascadeAfter` | long | Optional FK to another `EntitlementDefinition` |
| `status` | string | `Active` · `Inactive` |

---

#### UsageDefinition (`ONE_USAGE_DEFINITION`)

> Also referred to as `ConsumptionMetric` in some arch docs.

| Field | Type | Notes |
|---|---|---|
| PK `usageDefinitionId` | long | |
| `externalReferenceCode` | string | |
| `unit` | string | e.g. GB, page views, vcpu, AI tokens |
| `aggregation` | string | count / sum |
| `period` | string | Per month, day, hour |
| `quantity` | double | Base unit quantity |
| `overageRate` | double | |
| `overageCurrency` | string | USD / EUR / JPY |

---

#### UsageEvent (`ONE_USAGE_EVENT`)

| Field | Type | Notes |
|---|---|---|
| PK `usageEventId` | long | |
| FK `environmentId` | long | |
| FK `subscriptionId` | long | |
| FK `usageDefinitionId` | long | |
| `eventTimestamp` | datetime | |
| `quantity` | double | |
| `dedupeKey` | string | Idempotent; client-assigned |

---

#### Property (`ONE_PROPERTY`)

| Field | Type | Notes |
|---|---|---|
| `accountEntryId` | long | |
| `classNameId` | long | order / account / subscriptionitem |
| `classPK` | long | orderId / accountId / subscriptionItemId |
| `name` | string | e.g. `nonprodsubscriptionuuid` |
| `value` | string | e.g. `12345-abcde` |
| `metadataJson` | text | JSON metadata blob |

---

### Environment Management

#### Environment (`ONE_ENVIRONMENT`)

| Field | Type | Notes |
|---|---|---|
| PK `environmentId` | long | |
| FK `subscriptionId` | long | |
| `deploymentKey` | string | Stable external ID, uppercased |
| `deploymentGroupERC` | string | Optional grouping |
| `type` | picklist | `cloud_native` · `on_prem` (SaaS, PaaS, CNE, On-prem) |
| `name` | picklist | Production · Non-Production · Development · Backup · HA Production |
| `instanceSize` | picklist | XS · S · M · L · XL · XXL |
| `productVersion` | string | e.g. `7.4`, `2024.q3` |
| `hasDisasterDataCenterRegion` | boolean | Default `false` |
| `startDate`, `endDate` | datetime | |
| `activationMode` | string | `file_key` · `heartbeat` |
| `region` | FK | → `Region` |
| `dataCenterId` | FK | → `DataCenter` |
| `status` | picklist | Active · Decommissioned · Suspended; default `Active` |
| `lastHeartbeatAt` | datetime | |
| `currentEntitlementHash` | string | Identity hash for heartbeat server |
| `hostName`, `domain` | string | File-key environments |
| `ipAddresses`, `macAddresses` | string | File-key environments |
| `serverId` | string | File-key environments |

---

### Commerce

#### Commerce Product (CPDefinition with custom fields)

| Field | Type | Notes |
|---|---|---|
| PK `CProductId` | long | |
| `externalReferenceCode` | string | salesforceProductId / OOTB key for marketplace |
| `catalog` | string | Salesforce / Liferay / AccountEntry |
| `name` | string | |
| `description` | string | |
| `isPrimary` | boolean | Required for 5 entitlement rules; default `false` |
| `licenseKeyProductVersion` | string | Version string in generated keys (e.g. `dxp-7.4`); null for non-key products |
| `productFamily` | picklist | DXP · Portal · Analytics · Commerce · AIHub · CMP · Partner · Support · Training · Other |
| `metricCoverage` | string | Rules from SFDC Product Catalog |

**Product Specifications**

| Specification | Group |
|---|---|
| `type` | — |
| `database-size` | Liferay SaaS Plans |
| `document-library-size` | Liferay SaaS Plans |
| `domains` | Liferay SaaS Plans |
| `project-workspaces` | Liferay SaaS Plans |
| `ram` | Liferay SaaS Plans |
| `sites` | Liferay SaaS Plans |
| `transactions` | Liferay SaaS Plans |
| `vcpu` | Liferay SaaS Plans |
| `high-database` | Machine Type High |
| `high-extensions-ram` | Machine Type High |
| `high-extensions-vcpus` | Machine Type High |
| `high-logs` | Machine Type High |
| `high-storage` | Machine Type High |
| `high-traffic-networking` | Machine Type High |
| `standard-database` | Machine Type Standard |
| `standard-extensions-ram` | Machine Type Standard |
| `standard-extensions-vcpus` | Machine Type Standard |
| `standard-logs` | Machine Type Standard |
| `standard-storage` | Machine Type Standard |
| `standard-traffic-networking` | Machine Type Standard |

**CPSpecificationOption values (Marketplace app metadata)**

| Specification |
|---|
| `App API Reference URL` |
| `App Beta` |
| `App Documentation URL` |
| `App Entry` |
| `App Entry UUID` |
| `App Installation Guide URL` |
| `App Settings` |
| `App Usage Terms URL` |
| `Current Requirements` |
| `Developer Name` |
| `Downloadable Cloud App` |
| `Latest Version` |
| `Licence Duration` |
| `License` |
| `License Term` |
| `License Type` |
| `Liferay Product Capabilities` |
| `Liferay Product Categories` |
| `Liferay Version` |
| `Lifetime License` |
| `Number of CPUs` |
| `Orphan` |
| `Our Selection` |
| `Partnership Type` |
| `Past Versions Work With` |
| `Price Model` |
| `Product Downloads` |
| `Product Notes` |
| `Publisher Name` |
| `Publisher Web site URL` |
| `Ram in GB` |
| `Solution Company Description` |
| `Solution Company Email` |
| `Solution Company Phone` |
| `Solution Company Website` |
| `Solution Contact Email` |
| `Solution Details Blocks` |
| `Solution Header Description` |
| `Solution Header Title` |
| `Solution Header Video Description` |
| `Solution Header Video URL` |
| `Solution Type` |
| `Source Code URL` |
| `Support Email` |
| `Support Email Address` |
| `Support Phone` |
| `Support URL` |
| `Trial Length` |
| `Type` |

**Categories**

| Category |
|---|
| Marketplace App Category |
| Marketplace App Tags |
| Marketplace Availability |
| Marketplace Category |
| Marketplace Liferay Platform Offering |
| Marketplace Liferay Version |
| Marketplace Product Type |
| Marketplace Solution Category |
| Marketplace Solution Tags |

---

### User & Organization

#### User

| Field | Type | Notes |
|---|---|---|
| `userId` | long | |
| `uuid_` | string | Custom field |
| `emailAddress` | string | |
| `firstName` | string | |
| `middleName` | string | |
| `lastName` | string | |
| `verified` | boolean | Custom field |

---

#### Organization

| Field | Type | Notes |
|---|---|---|
| PK `organizationId` | long | |
| `name` | string | |
| `accountEntryId` | long | Custom field; relates org to AccountEntry |

---

## ERC and FriendlyURL Registry

| Object | ERC | Separator |
|---|---|---|
| `AccountFlag` | `ONE_ACCNT_FLAG` | `cpaf` |
| `AccountNote` | `ONE_ACCNT_NOTE` | `cpan` |
| `BannedEmailDomain` | `ONE_BANNED_EMAIL` | `cpbd` |
| `Entitlement` | `ONE_ENTITLEMENT` | `cpen` |
| `EntitlementDefinition` | `ONE_ENTITLEMENT_DEFINITION` | `cped` |
| `Environment` | `ONE_ENVIRONMENT` | `cpdp` |
| `ExternalLink` | `ONE_EXT_LINK` | `cpel` |
| `Property` | `ONE_PROPERTY` | `cppr` |
| `Publisher` | `ONE_PUBLISHER` | `cppu` |
| `Subscription` | `ONE_SUBSCRIPTION` | `cpsc` |
| `SubscriptionItem` | `ONE_SUB_ITEM` | `cpsi` |
| `Team` | `ONE_TEAM` | `cpte` |
| `TrialProvisioning` | `ONE_TRIAL_PROV` | `cptp` |
| `UsageDefinition` | `ONE_USAGE_DEFINITION` | `cpud` |
| `UsageEvent` | `ONE_USAGE_EVENT` | `cpue` |

---

## See Also

- [`api.md`](./api.md) — headless conventions and custom REST contracts
- [`integrations/salesforce.md`](./integrations/salesforce.md) — Salesforce-owned objects and inbound Pub/Sub