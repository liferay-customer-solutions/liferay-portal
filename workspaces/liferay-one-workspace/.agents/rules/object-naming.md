# Object Naming Conventions

ERCs follow `C_{ABBREV}` — a `C_` prefix + uppercase abbreviated object name with underscores (e.g. `C_ACCNT_FLAG`, `C_LICENSE_KEY`, `C_ENTITLEMENT_DEFINITION`). Max 40 chars. Full registry in `specs/data-model.md`.

Object names are PascalCase with no domain prefix (`AccountFlag`, `SupportTicket`). Fields are camelCase. All Objects are `scope: "company"`.

Field ERCs follow `{OBJECT_ERC}_{FIELD}` — the object ERC plus the camelCase field name in UPPER_SNAKE_CASE, acronyms kept whole (e.g. `accountName` → `C_LICENSE_KEY_ACCOUNT_NAME`, `websiteURL` → `C_PUBLISHER_DETAILS_WEBSITE_URL`). Max 75 chars.