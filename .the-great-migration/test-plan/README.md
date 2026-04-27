---
name: Test Plan — The Great Migration
description: Master index and guide for all e2e test scenarios for the liferay-one-workspace consolidated platform
type: reference
---

# Test Plan — The Great Migration

This folder contains the full test specification for `liferay-one-workspace`, the consolidated replacement for Koroneiki, Provisioning, Marketplace, and Support. Each document maps directly to a Playwright test file and is structured so a test author can convert scenarios into specs without additional design work.

## Structure

| File | Domain | Playwright file (target) |
|---|---|---|
| [`01-accounts.md`](./01-accounts.md) | Account, AccountFlag, AccountNote, Team, ExternalLink | `tests/liferay-one/accounts.spec.ts` |
| [`02-subscriptions.md`](./02-subscriptions.md) | Subscription, SubscriptionItem, Environment, LicenseKey | `tests/liferay-one/subscriptions.spec.ts` |
| [`03-entitlements.md`](./03-entitlements.md) | EntitlementDefinition, EntitlementSync | `tests/liferay-one/entitlements.spec.ts` |
| [`04-marketplace.md`](./04-marketplace.md) | Marketplace storefront, purchase, trial flows | `tests/liferay-one/marketplace.spec.ts` |
| [`05-publisher.md`](./05-publisher.md) | Publisher onboarding, assets, sales summary | `tests/liferay-one/publisher.spec.ts` |
| [`06-support-tickets.md`](./06-support-tickets.md) | SupportTicket, TicketAttachment, escalation | `tests/liferay-one/support-tickets.spec.ts` |
| [`07-support-forms.md`](./07-support-forms.md) | CallbackRequest, ReplacementActivationKeyRequest | `tests/liferay-one/support-forms.spec.ts` |
| [`08-business-events.md`](./08-business-events.md) | BusinessEvent, BusinessEventVersion | `tests/liferay-one/business-events.spec.ts` |
| [`09-admin.md`](./09-admin.md) | Admin UI — all /admin/* pages | `tests/liferay-one/admin.spec.ts` |
| [`10-permissions.md`](./10-permissions.md) | Role-based access control matrix | `tests/liferay-one/permissions.spec.ts` |
| [`11-integrations.md`](./11-integrations.md) | Salesforce, Jira, GCS, Liferay Cloud integration smoke tests | `tests/liferay-one/integrations.spec.ts` |
| [`12-scheduled-tasks.md`](./12-scheduled-tasks.md) | Scheduled task behavior and failure handling | `tests/liferay-one/scheduled-tasks.spec.ts` |

## Test ID convention

Each scenario is prefixed with a unique ID: `TC-{domain}-{number}`. The domain codes:

| Code | Domain |
|---|---|
| `ACC` | Accounts |
| `SUB` | Subscriptions & Environments |
| `ENT` | Entitlements |
| `MKT` | Marketplace |
| `PUB` | Publisher |
| `SUP` | Support Tickets |
| `SFM` | Support Forms |
| `BEV` | Business Events |
| `ADM` | Admin UI |
| `PRM` | Permissions |
| `INT` | Integrations |
| `SCH` | Scheduled Tasks |

## Scenario format

Each scenario follows this template:

```
## TC-XXX: Scenario title

**Actors:** [role list]
**Preconditions:** [state required before the test runs]

### Steps
1. ...

### Assertions
- [ ] ...

### Playwright notes
[implementation hints: selectors, fixtures, mocks needed]
```

## Test tiers

Tests are tagged to indicate scope:

| Tag | Description | Runs in CI |
|---|---|---|
| `@smoke` | Critical golden path — must pass before any deploy | Always |
| `@regression` | Full feature coverage — catches edge-case breaks | On PR merge |
| `@integration` | Requires live external endpoints (Jira, GCS, Salesforce) | Nightly only |
| `@migration` | Validates migrated data integrity post-cut-over | Post-migration gates |

Playwright tests in `tests/liferay-one/` inherit project config from `playwright.config.ts`. Integration tests that call real external APIs use a separate project with `testMatch: /integrations.spec.ts/` and `retries: 2`.

## Actors and fixtures

All tests run as one of these fixture users. Create them in the Playwright global setup or use pre-seeded portal accounts:

| Fixture | Role | Notes |
|---|---|---|
| `adminUser` | `Administrator` | Full access everywhere |
| `liferayStaff` | `Liferay Staff` + `Worker_Admin` on test account | Internal employee |
| `workerManager` | `Worker_Manager` on test account | Limited write |
| `workerMember` | `Worker_Member` on test account | Read-only worker |
| `customerAdmin` | `Customer_Admin` on test account | Customer org admin |
| `customerManager` | `Customer_Manager` on test account | Customer manager |
| `customerMember` | `Customer_Member` on test account | Customer read-only |
| `publisherUser` | `Publisher` instance role | Marketplace publisher |
| `guestUser` | Unauthenticated | Public pages only |

## External service mocks

Integration tests that touch real external systems are tagged `@integration` and run nightly. All other tests mock external calls at the `etc-spring-boot` layer using `WireMock` stubs or Playwright's `page.route()` intercepts:

| External call | Mock strategy |
|---|---|
| Jira REST (issue create, comment, status) | `page.route('**/jira/**')` intercept + fixture response |
| GCS resumable upload | `page.route('**/storage.googleapis.com/**')` intercept |
| Liferay Cloud trial provision | `page.route('**/trial/provision/**')` intercept |
| Salesforce Pub/Sub subscriber | Trigger via direct API call to `POST /o/c/subscriptions` instead |
| Marketo form submission | `page.route('**/marketo/**')` intercept |
| NAV / Dynamics | `page.route('**/nav/**')` intercept |

## Relationship to architecture docs

| This file references | Source of truth |
|---|---|
| Object fields and ERCs | [`../arch/data-model.md`](../arch/data-model.md) |
| Business logic triggers | [`../arch/business-logic.md`](../arch/business-logic.md) |
| API paths and scopes | [`../arch/api.md`](../arch/api.md) |
| Page URLs and nav | [`../arch/ui.md`](../arch/ui.md) |
| Integration contracts | [`../arch/integrations/`](../arch/integrations/) |
| Role catalog | [`../arch/business-logic.md §5`](../arch/business-logic.md) |
