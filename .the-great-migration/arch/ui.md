# UI — Site Initializer & Custom Element

Implements system-spec §6. One site-initializer (`liferay-one-site-initializer`) containing three page groups: Marketplace (public), Support (customer-authenticated), Admin (internal). One React custom element (`liferay-one-custom-element`) backs every dynamic page.

## Design summary (D9)

Per system-spec D9, start with one site. Future-split trigger: if navigation or branding pressure grows, peel Marketplace into `liferay-one-marketplace-site-initializer`. Defer until pain surfaces.

## Site-initializer structure

```
liferay-one-site-initializer/
├── client-extension.yaml
├── LCP.json
└── site-initializer/
    ├── commerce-channel.config.json
    ├── expando-columns.json
    ├── expando-values.json
    ├── object-definitions/           # §3.1-3.6 Objects (one JSON per Object)
    ├── list-type-definitions/        # Picklists
    ├── object-actions/               # Per-Object action definitions
    ├── object-validations/           # Per-Object validations
    ├── workflow-definitions/         # Kaleo workflows
    ├── roles/                        # Role catalog
    ├── oauth2-applications/          # OAuth2 app definitions
    ├── notification-templates/       # Email templates
    ├── fragments/                    # Page fragments (group + individual)
    │   └── group/liferay-one/
    │       ├── marketplace/
    │       ├── support/
    │       └── admin/
    ├── layout-page-templates/        # Display page + master templates
    ├── journal-articles/             # Static content
    ├── ddm-templates/                # Display templates
    ├── layouts/                      # Page tree (top-level pages + children)
    │   ├── marketplace/
    │   ├── support/
    │   └── admin/
    ├── documents/                    # Static assets (icons, images)
    ├── navigation-menus.json
    ├── assets.json
    ├── permissions/                  # Per-page + per-object permissions
    └── site.json                     # Site config
```

## Page groups

### Marketplace (public)

Ported from `liferay-marketplace-workspace`. Top-level pages:
- `/` — storefront home
- `/apps` — app catalog
- `/apps/{slug}` — display page template `app-detail`
- `/solutions` — solutions listings
- `/solutions/{slug}` — display page template `solutions-details`
- `/publishers` — publisher directory
- `/publishers/{slug}` — publisher detail
- `/publisher-onboarding` — prospective publisher form (`RequestPublisherAccount`)
- `/contact-sales` — Marketo form
- `/pricing` — static content
- `/product-feedback` — Marketo form
- `/cart`, `/checkout` — Commerce
- `/account` — Commerce account portal

Fragments: `marketplace-base-fragments`, `public-sites-navigation`, `migrated-fragments-from-lrdc`. DDM templates: `app-detail`, `solutions-details`. Masters: `marketplace-master`, `marketplace-master-private`, `marketplace-blank`. Kaleo: `product-approver-workflow`, `publisher-onboarding-workflow`.

### Support (customer-authenticated)

Ported from `liferay-customer-workspace`. Top-level pages:
- `/home` — customer portal landing
- `/projects` — list of Deployments
- `/projects/{deploymentKey}` — single deployment view
- `/tickets` — SupportTicket list
- `/tickets/new` — create ticket
- `/tickets/{jiraIssueKey}` — ticket detail with Jira live-fetch
- `/support-ticket-escalation` — escalation form
- `/callback-request` — CallbackRequest form
- `/replacement-activation-key` — ReplacementActivationKeyRequest form
- `/large-file-uploader` — TicketAttachment upload flow
- `/security-vulnerabilities` — LSV project proxy (see `./integrations/jira.md`)
- `/release-notes` — ported from customer workspace
- `/business-events` — BusinessEvent list + detail
- `/cookie-policy` — static
- `/onboarding` — new-customer walkthrough

Feature-flagged `-testing` variant retained for LRSD-6322 / LRSD-12003 rollouts per system-spec.

### Admin (internal-only)

New. Replaces Koroneiki admin portlets + Provisioning portlets. Top-level pages:
- `/admin` — dashboard
- `/admin/accounts` — AccountEntry list + detail (read + edit)
- `/admin/contacts` — User + Account membership
- `/admin/teams` — Team list + detail
- `/admin/subscriptions` — Commerce subscription browser
- `/admin/deployments` — Deployment list + detail
- `/admin/products` — CPDefinition browser
- `/admin/entitlements` — Entitlement browser (read-only)
- `/admin/entitlement-definitions` — EntitlementDefinition editor (with rule-JSON validation)
- `/admin/license-keys` — LicenseKey browser + generate/revoke
- `/admin/publishers` — Publisher review + approval
- `/admin/business-events` — BusinessEvent editor
- `/admin/reports` — Report view
- `/admin/external-links` — ExternalLink browser
- `/admin/debug` — replaces `DebugRabbitMQMVCActionCommand`; exposes `/entitlements/recompute`, Jira cache flush, scheduled-task status

---

## Custom element

One React + TypeScript custom element, `liferay-one-custom-element`, shipped as a single client extension and embedded by every dynamic page across all three page groups. Routing inside the element (React Router) selects the feature to render from the URL path; the site-initializer embeds the element on each page with a route-prefix configuration so the element knows which feature to mount.

### Feature areas

Feature modules inside the element, organized as top-level folders under `src/features/`. Each module owns its own routes, components, and API clients. Shared UI primitives live in `src/shared/`.

**Marketplace features** — ported from `liferay-marketplace-workspace`. ~349 TSX files.
- App catalog + search
- App detail + purchase flow (embeds Commerce)
- Publisher dashboard (asset upload, sales summary)
- Trial-request flow
- Feedback modals

**Support features** — ported from `liferay-customer-workspace`.
- Deployment list + detail
- Ticket create + view (live-fetches from Jira via `/jira/issue/{key}`)
- Attachment upload (GCS resumable upload flow)
- BusinessEvent editor
- Security-vulnerabilities browser
- Release-notes browser

**Admin features** — new. Functional over beautiful; Liferay Clay components, minimal custom styling.
- Account browser with hierarchical tree
- Entitlement-definition editor with rule-JSON validator UI
- Scheduled-task dashboard (status, next-run, manual-trigger)
- OAuth2 app + scope management
- `ExternalLink` audit browser
- Debug panel

### API auth

The element runs under a single user-agent OAuth2 flow that piggybacks on the Liferay session. At request time, the workspace enforces scope based on the caller's role — an admin user's session-token carries every scope; a customer session carries only the customer-scoped ones. Feature modules that require elevated scope (admin, license.admin) render access-denied states when the session lacks them.

A single OAuth2 application — **`one-custom-element`** — backs the element. Its scope set is the union of everything the UI might call; runtime scope is narrowed per-user by role. Separate per-audience apps were considered and rejected because one React bundle carries all three audiences; see [`./api.md §4.2`](./api.md) for the full app registry and the audit-boundary rationale.

### Routing

- `/` and `/apps/*`, `/solutions/*`, `/publishers/*` → Marketplace features
- `/home`, `/tickets/*`, `/projects/*`, `/business-events/*`, etc. → Support features
- `/admin/*` → Admin features

Feature modules are loaded lazily per top-level route so the initial bundle only pulls the needed code path.

### Bundle strategy

Total codebase ~400+ TSX files. Without code-splitting the initial payload is large. Each feature module is a lazy chunk (`React.lazy` + `Suspense`); the entry bundle loads only routing + shared primitives + auth. First-paint cost matches today's single-element workspaces.

---

## Navigation

### Public navigation (Marketplace page group)
Unauthenticated visitors see Marketplace nav + sign-in link. No Support or Admin links visible.

### Customer-authenticated navigation
Authenticated customers see:
- Marketplace (persistent)
- Customer Portal dropdown with Support links (`My Tickets`, `Deployments`, `Business Events`, `Callback Request`)

Navigation items scoped by Account Role (`Customer_*`). Higher roles see additional admin-like pages within their account.

### Internal navigation (Admin page group)
Visible only to users holding `Administrator` or `Liferay Staff` role. Admin nav lives in a separate menu (distinct visual treatment — persistent side nav vs. Marketplace's top nav).

Navigation menus defined in `site-initializer/navigation-menus.json`.

### Menu segmentation

Per system-spec §6, menu items check:
1. Liferay authentication state.
2. `Liferay Staff` user group membership (for Admin nav).
3. Account Role membership for dropdown items that vary per role.

Implementation: Liferay navigation menu items support role/user-group scoping out-of-the-box; configuration in JSON.

---

## Permissions

Per-page permissions in `site-initializer/permissions/`. Defaults:

| Page tree | Default view permission |
|---|---|
| `/` and Marketplace public pages | Guest + authenticated |
| `/publisher-onboarding`, `/contact-sales` | Guest + authenticated (public form) |
| Customer portal (`/home`, `/tickets`, `/projects`, `/business-events`, …) | any account-membered user |
| `/admin/*` | `Liferay Staff` user group + `Administrator` role |

Deeper admin sub-pages further restrict:

| Sub-page | Restricted to |
|---|---|
| `/admin/entitlement-definitions` | `Administrator` only |
| `/admin/license-keys` (generate/revoke) | `Administrator` + `license.admin` scope |
| `/admin/debug` | `Administrator` only |

Page permissions enforced by Liferay core; the custom element additionally hides action buttons per scope.

---

## Branding

One master theme for Marketplace + Support + Admin — Liferay's default DXP theme with workspace-specific color tokens in `liferay-one-global-css`.

Per-page-group master templates differ in header/footer composition:
- `marketplace-master` — storefront header with cart + sign-in
- `marketplace-master-private` — post-auth header with customer-portal dropdown
- `support-master` — customer-portal header
- `admin-master` — minimal top bar + persistent side nav

---

## Fragments & DDM templates

**Fragments** live in `site-initializer/fragments/group/liferay-one/<page-group>/`. Each page group owns its fragment namespace (no cross-group sharing, keeps branding pressure low).

**DDM templates** — display-page templates for Object detail renders (`app-detail` for `CPDefinition`, `publisher-detail` for `Publisher`, etc.).

---

## Migration from existing workspaces

### From `liferay-marketplace-workspace`

- Port `fragments/`, `layouts/`, `layout-page-templates/`, `journal-articles/`, `ddm-templates/`, `documents/` into the Marketplace page group under `liferay-one-site-initializer`.
- Rename top-level layout directories to `marketplace/` under the site initializer.
- Update fragment CSS variables to reference `liferay-one-global-css` tokens.
- Rebase custom-element imports from `liferay-marketplace-*` to `liferay-one-*`.

### From `liferay-customer-workspace`

- Port support-portal pages into the Support page group.
- Drop `koroneiki-account.json` and related side-car object-definitions (now merged into AccountEntry extensions per `./objects/customer.md`).
- Port fragments; merge any duplicate fragment names between Marketplace and Support (likely none — naming conventions differ).

### Admin

- New content. No port source. Built as the Admin feature module of `liferay-one-custom-element`.

---

## Future-split trigger (per D9)

Peel Marketplace into its own site-initializer when any of:
- Public-vs-authenticated navigation can't be cleanly segmented within one site's menu structure.
- Branding pressure diverges (Marketplace wants consumer-friendly theme; Customer Portal wants enterprise).
- Fragment / DDM-template name collisions proliferate across page groups.
- Page-count exceeds ~40 per page group (Liferay UI starts to feel cluttered).

Split shape:
- `liferay-one-marketplace-site-initializer` — Marketplace page group + fragments.
- `liferay-one-site-initializer` — Support + Admin page groups.
- Object definitions, roles, OAuth2 apps stay in `liferay-one-site-initializer` (shared backend).

---

## Open questions

1. **Marketplace-shared auth.** Authenticated Marketplace visitors have Customer Portal access — single auth session. Confirm session-cookie + SSO behavior with the existing Liferay auth infrastructure during phase 4.
2. **Admin IA.** The admin page list above is exhaustive but may be too deep for UX. Phase 1 builds skeletal admin pages; refinement in phase 5.
3. **Single OAuth2 app vs separate per audience.** The consolidation into one `one-custom-element` OAuth2 application widens the credential's scope surface. Revisit if audit pressure requires tighter scoping — splitting back into three apps is mechanical once session-token minting can pick the right app per role.
4. **Feature flags.** Today's `-testing` variant pages (LRSD-6322 / LRSD-12003) live as separate page copies. Consider migrating to a true feature-flag mechanism (GrowthBook or similar) post-launch.
