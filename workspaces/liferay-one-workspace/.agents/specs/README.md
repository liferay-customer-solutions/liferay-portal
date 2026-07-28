# Liferay One — Unified Portal Specs

Source-of-truth for the `liferay-one-workspace` implementation. Each document narrows a decision to the level an engineer can build from without follow-up.

Target workspace: `workspaces/liferay-one-workspace/`

## Reading order

1. [`workspace.md`](./workspace.md) — shell layout, client extensions, naming conventions

1. [`data-model.md`](./data-model.md) — full entity index, ERC + FriendlyURL registry, field mappings

For the API surface, page/route map, and integration contracts, read the code
directly — the Spring Boot controllers under
`client-extensions/liferay-one-etc-spring-boot`, the frontend service layer and
`src/pages/` under `client-extensions/liferay-one-custom-element`, and the
`.agents/rules/` conventions. Earlier standalone `api.md`, `ui.md`, and
`integrations/` specs were removed after drifting out of sync with the
implementation.