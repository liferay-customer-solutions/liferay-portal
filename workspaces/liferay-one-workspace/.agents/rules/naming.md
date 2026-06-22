# Naming Conventions

## Brand Names

Always use the exact official casing for brand and product names. Brian specifically corrected these:

| Wrong | Correct |
| --- | --- |
| `argoCD` | `ArgoCD` |
| `argoWorkflows` | `Argo Workflows` |
| `grafana` | `Grafana` |
| `koroneiki` | `Koroneiki` |

This applies everywhere: UI labels, log messages, configuration values, and comments.

## File Naming: SVG and CSS

SVG and CSS files use underscores (`_`), not hyphens (`-`):

```
# Wrong
hero-banner.svg
service-card.css

# Correct
hero_banner.svg
service_card.css
```

## REST Controller Endpoints

REST controller mapping paths and method names must be "very robotic" — path segments map directly and mechanically to the method name, one-to-one. Follow the established pattern in the existing controller:

```java
// Pattern: @GetMapping("/security-vulnerabilities/affected-versions")
//          → getSecurityVulnerabilitiesAffectedVersions()

@GetMapping("/business-events")
public ResponseEntity<String> getBusinessEvents() { ... }

@GetMapping("/business-events/{id}")
public ResponseEntity<String> getBusinessEventsById(@PathVariable String id) { ... }
```

Each URL path word becomes a camelCase word in the method name. Do not abbreviate, rephrase, or add words that are not in the URL.

## Service File Naming: `services/headless` and `services/spring-boot`

The file name and exported class/instance name must match the URL the service targets.

**`services/headless/`** — name after the Liferay API group (the segment after `/o/`), converted to PascalCase:

```
/o/headless-admin-user/v1.0/...       → HeadlessAdminUser.ts   / class HeadlessAdminUser
/o/headless-commerce-admin-order/...  → HeadlessCommerceAdminOrder.ts
/o/commerce-ui/...                    → CommerceUI.ts           / class CommerceUI
```

**`services/spring-boot/`** — name after the base path passed to `OneSpringBootOAuth2`, converted to PascalCase. The exported singleton takes the same name; the private inner class appends `OAuth2`:

```
new OneSpringBootOAuth2('/commerce-orders')   → CommerceOrders.ts  / class CommerceOrdersOAuth2  / const CommerceOrders
new OneSpringBootOAuth2('/common-license-keys') → CommonLicenseKeys.ts
new OneSpringBootOAuth2('/trial')             → Trial.ts
```

When the base path is empty and all methods share a common prefix, promote that prefix to the base path and drop it from the method paths. Do not name a file `One.ts`, `Client.ts`, or any other generic name that does not map to a URL segment.