# Workspace Shell

## Location

`workspaces/liferay-one-workspace/`

## Client Extensions

| Extension | Description |
|---|---|
| `liferay-one-batch` | Batch import of Object definitions, list types, roles, and other headless resources |
| `liferay-one-custom-element` | React + TypeScript — all dynamic UI for Marketplace, Support, Admin |
| `liferay-one-etc-spring-boot` | Spring Boot REST service for provisioning, GCS, Jira, license gen, Salesforce Pub/Sub subscriber |
| `liferay-one-global-css` | Shared color tokens + global styles |
| `liferay-one-instance-settings` | Secrets and external credentials (not checked into repo) |
| `liferay-one-site-initializer` | Single site initializer serving Marketplace, Support, Admin page groups |

### Site-Initializer Structure

```
liferay-one-site-initializer/
└── site-initializer/
    ├── data-definitions/
    ├── ddm-templates/
    ├── documents/
    ├── fragments/
    │   └── group/liferay-one/
    │       ├── collection.json
    │       ├── fragments/
    │       └── resources/
    ├── layout-page-templates/
    ├── layout-set/
    ├── layouts/
    │   ├── 01_home/
    │   ├── 02_my-account/
    │   ├── 03_support/
    │   ├── 04_marketplace/
    │   ├── 05_admin/
    │   ├── 06_product-purchase/
    │   ├── 06_search/
    │   └── 07_next-steps/
    ├── notification-templates/
    ├── style-books/
    ├── taxonomy-vocabularies/
    ├── asset-list-entries.json
    ├── commerce-channel.json
    ├── expando-columns.json
    ├── expando-values.json
    ├── resource-permissions.json
    └── site-navigation-menus.json
```

Layouts are ordered by numeric prefix, not grouped by page group. Object
definitions, roles, and OAuth2 applications are imported by the
`liferay-one-batch` client extension, not the site initializer.

### Object Names

PascalCase, no prefix: `AccountFlag`, `SupportTicket`, `LicenseKey`.

### Field Names

camelCase. Booleans phrased as questions: `internal`, `clustered`, `hasDisasterDataCenterRegion`.

### Friendly URL Separators

4 lowercase letters matching the ERC suffix. Must be unique across all Objects in the workspace.

**Exception — `AccountNote` uses `l`** (Liferay's default separator), which skips friendly-URL generation. Its title field `content` can contain slashes/newlines/links that would otherwise throw `MustNotHaveTrailingSlash`.