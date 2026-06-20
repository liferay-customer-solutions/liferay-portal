# Workspace Shell

## Location

`workspaces/liferay-one-workspace/`

## Client Extensions

| Extension | Description |
|---|---|
| `liferay-one-custom-element` | React + TypeScript — all dynamic UI for Marketplace, Support, Admin |
| `liferay-one-etc-spring-boot` | Spring Boot REST service for provisioning, GCS, Jira, license gen, Salesforce Pub/Sub subscriber |
| `liferay-one-global-css` | Shared color tokens + global styles |
| `liferay-one-instance-settings` | Secrets and external credentials (not checked into repo) |
| `liferay-one-site-initializer` | Single site initializer serving Marketplace, Support, Admin page groups |

### Site-Initializer Structure

```
liferay-one-site-initializer/
└── site-initializer/
    ├── object-definitions/
    ├── list-type-definitions/
    ├── object-actions/
    ├── object-validations/
    ├── workflow-definitions/
    ├── roles/
    ├── oauth2-applications/
    ├── notification-templates/
    ├── fragments/
    │   └── group/liferay-one/
    │       ├── marketplace/
    │       ├── support/
    │       └── admin/
    ├── layout-page-templates/
    ├── journal-articles/
    ├── ddm-templates/
    ├── layouts/
    │   ├── marketplace/
    │   ├── support/
    │   └── admin/
    ├── documents/
    ├── navigation-menus.json
    ├── permissions/
    └── site.json
```

### Object Names

PascalCase, no prefix: `AccountFlag`, `SupportTicket`, `LicenseKey`.

### Field Names

camelCase. Booleans phrased as questions: `internal`, `clustered`, `hasDisasterDataCenterRegion`.

### Friendly URL Separators

4 lowercase letters matching the ERC suffix. Must be unique across all Objects in the workspace.

**Exception — `AccountNote` uses `l`** (Liferay's default separator), which skips friendly-URL generation. Its title field `content` can contain slashes/newlines/links that would otherwise throw `MustNotHaveTrailingSlash`.