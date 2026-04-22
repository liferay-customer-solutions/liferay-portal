# Workspace Shell

Target: `workspaces/liferay-one-workspace/`.

Modeled after `liferay-customer-workspace/` and `liferay-marketplace-workspace/`, which are the two sources being absorbed. All client-extension directories use the `liferay-one-*` prefix.

## Directory layout

```
workspaces/liferay-one-workspace/
├── client-extensions/
│   ├── liferay-one-custom-element/                 # React — single element for Marketplace + Support + Admin (ported + new features; see ui.md).
│   ├── liferay-one-etc-spring-boot/                # Custom REST + Salesforce Pub/Sub subscriber + integration clients. See api.md, integrations/.
│   ├── liferay-one-global-css/                     # Shared branding tokens.
│   ├── liferay-one-instance-settings/              # Global Liferay instance config: OAuth2 apps, notification templates, BannedEmailDomain seed.
│   └── liferay-one-site-initializer/               # Single site. Page groups: Marketplace + Support + Admin. All Object definitions, roles, fragments, DDM templates.
├── configs/local/                                  # Local dev configs.
├── gradle/
├── gradlew, gradlew.bat, settings.gradle, gradle.properties
├── package.json, yarn.lock
├── copyright.js
└── test.properties
```

## D9 — single site initializer

Per system-spec D9, all three audiences (Marketplace public, Support authenticated, internal admin) ship inside one `liferay-one-site-initializer`. Navigation and page-level permissions segment them. The future-split trigger and role-to-page matrix live in `ui.md`.

## Naming conventions

### Object ERCs

Pattern: `ONE_{domainPrefix}_{objectSlug}`. Max 40 chars. Uppercase. Slug is the Object name truncated/abbreviated so the ERC stays ≤40.

| Domain | Prefix | Example ERC |
|---|---|---|
| Customer | `CUS` | `ONE_CUS_ACCNT_FLAG` |
| Commerce / Deployment | `COM` | `ONE_COM_DEPLOYMENT` |
| Entitlement | `ENT` | `ONE_ENT_DEFINITION` |
| Marketplace | `MKT` | `ONE_MKT_PUBLISHER` |
| Support | `SUP` | `ONE_SUP_TICKET` |
| Reference / admin | `REF` | `ONE_REF_REGION` |

Field ERCs append `_{fieldName}`: `ONE_CUS_ACCNT_FLAG_flagCode`.

Relationship ERCs use `_REL_{relationshipName}` on the source side.

### Object names

PascalCase, no domain prefix: `AccountFlag`, `SupportTicket`, `LicenseKey`. This is the Liferay Object-level `name` field and becomes the REST path segment (`/o/c/accountFlags`).

### Field names

camelCase: `accountEntryId`, `koroneikiAccountCode`, `jiraIssueKey`. Booleans read as questions: `internal`, `clustered`, `hasDisasterDataCenterRegion`.

### Relationship field names

Liferay auto-generates `r_{relationshipName}_{fkField}`. The relationship-name convention:

- One-to-many: `{singularSource}To{singularTarget}` → generated field `r_accountEntryToAccountFlag_accountEntryId` on AccountFlag.
- Many-to-many: `{singularSource}{PluralTarget}` → join-table only, no direct field.

Fixed relationship names are captured in each domain doc's "Relationships" section.

### Friendly URL separators

Four-letter, lowercase, matches the ERC suffix: `AccountFlag` → `cpaf` (already in use in the customer workspace, preserved). New Objects pick a fresh 4-letter slug. Full registry in `objects/README.md`.

### Client-extension component IDs

Match the directory name: `liferay-one-custom-element`, `liferay-one-global-css`, `liferay-one-site-initializer`, etc. These are the IDs site-initializer page definitions reference when embedding the element.

## Scope

All Objects are `scope: "company"`. The single Liferay company runs the whole platform. Multi-tenancy via AccountEntry, not via Liferay companies. (System-spec open risk #9 — confirm before phase 1 finalizes.)

## Build / deploy

Standard Liferay SaaS client-extension workflow:

```bash
cd workspaces/liferay-one-workspace && ./gradlew deploy
```

No Ant path — this is a pure SaaS workspace. Site initializer runs once on first site provisioning; subsequent re-deploys update client extensions in place. Object schema changes that add fields are additive; removals or type changes require explicit data-migration in `migration.md`.

## Credentials, secrets, external config

Managed via `liferay-one-instance-settings` and Liferay SaaS secret storage:

- Salesforce Pub/Sub subscriber service account (JSON key).
- Jira API token + project keys.
- GCS bucket + service account.
- Liferay Cloud / Analytics Cloud API credentials.
- OAuth2 client-credentials apps for each caller (per `api.md`).

No secrets checked into the workspace repo.

## What does not live here

- Portal core modules (`portal-kernel`, `portal-impl`, `portal-web`) — stay in the parent repo; the workspace consumes their APIs.
- Salesforce Pub/Sub topic configuration — owned by the Salesforce admin team (D12). The workspace only holds the subscriber credentials.
- Jira project definitions — owned by the Atlassian admin team. The workspace holds API credentials and project-key constants.
- The RabbitMQ → Pub/Sub bridge (system-spec open risk #5) — retired during phase 6, not rebuilt here.

## Open questions

- **OSGi module for deeper integration?** Today the analogous workspaces have only React + site-initializer + spring-boot. If any Liferay-kernel-level hook (e.g., custom user pre-registration step, listener on a core entity that Object Actions can't reach) is required, a `modules/` directory with an OSGi bundle may be needed. Defer until a concrete need surfaces.
- **`liferay-one-etc-cron` vs Object scheduler-triggered actions.** Some scheduled tasks in system-spec §4.2 (e.g. `EntitlementSync`) are stateful enough to want a single-writer cron. Others (`TicketAttachmentTrashDrain`) could live as periodic Object Actions. Classification captured in `business-logic.md`; keeping both mechanisms available.
- **Workspace version alignment.** The consolidated workspace has to pin compatible versions of Liferay Commerce, Objects runtime, and the Customer workspace fragments being ported. Defer version-pin decision to phase 1 kickoff — not an arch-level concern.
