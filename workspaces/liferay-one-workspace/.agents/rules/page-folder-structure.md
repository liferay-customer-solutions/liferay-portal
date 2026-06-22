# Page Folder Structure

Every sub-page component inside a section under `src/pages/` must live in its own named subfolder, not directly in the section root.

## The Rule

Within any section directory (e.g., `pages/MyAccount/`, `pages/ProductPurchase/`), the **only** files permitted directly at the section root are:

- `{Section}.tsx` — the root page component
- `{Section}Router.tsx` — the HashRouter/router component
- `{section}Routes.tsx` — the route definitions
- `{Section}.css` — section-level styles
- `components/` — shared sub-components used by multiple pages within the section
- `hooks/` — shared hooks used by multiple pages within the section
- `types.ts` / `types.tsx` — shared type definitions
- `utils.ts` — shared utilities

Everything else — route guards, redirects, step pages, detail pages — must be in its own subfolder named after the component:

```
# Wrong
pages/MyAccount/AccountGuard.tsx
pages/ProductPurchase/AccountSelection.tsx

# Correct
pages/MyAccount/AccountGuard/AccountGuard.tsx
pages/ProductPurchase/AccountSelection/AccountSelection.tsx
```

## Existing Correct Structure (use as reference)

`pages/Admin/` follows this pattern — every sub-page has its own folder: `Apps/`, `Environments/`, `LicenseKeyUploads/`, `MPSummary/`, etc.

## Import Path Impact

When a file moves into a subfolder, update its relative imports accordingly:

- Imports of sibling files (`./Projects/projects`) become `../Projects/projects`
- Imports of parent-level utilities (`./components/...`, `./hooks/...`) become `../components/...`, `../hooks/...`
- Absolute `~/pages/...` imports require the new folder segment in the path