# Marketo (client-side forms)

Marketing form submissions from Marketplace pages. Client-side only — no workspace server involvement.

## Overview

**Direction:** outbound (browser → Marketo).

**Scope:** Marketing capture forms on Marketplace pages: contact-sales, product-feedback, trial-request.

## Integration mode

Forms use Marketo's Munchkin JS SDK embedded in Marketplace custom-element pages. Submissions POST directly from the browser to Marketo's endpoint — the workspace is not in the request path.

## Config

Single workspace-instance config: `marketo-munchkin-id` in `liferay-one-instance-settings`. Custom element reads it at page load.

Form IDs (ported from `liferay-marketplace-workspace`): `3738` (contact-sales), `6253` (product-feedback). Referenced in the custom element's form-embedding code.

## What the workspace does NOT do

- Does not proxy form submissions.
- Does not receive webhooks from Marketo.
- Does not store form responses (Marketo is the record of truth).

## What the workspace DOES do

- Renders the Marketo form widget inside the Marketplace custom element.
- Forwards the current AccountEntry context as hidden form fields (so Marketo knows which account the submitter belongs to).

## Failure handling

Marketo down = form submission fails client-side. Custom element surfaces a retry message. No workspace-side handling.

## Migration notes

Direct port from `liferay-marketplace-workspace`. Form IDs, Munchkin token, and embedding code unchanged.

## Open questions

1. **Double-submit prevention** — if a user submits a form and the redirect doesn't fire quickly enough, they may re-submit. Marketo-side dedup relies on email address. Confirm the UX pattern still works.
2. **Contact-sync back from Marketo** — today nothing syncs Marketo leads into the workspace. If product wants "these Marketplace visitors are hot leads → create Contact record," that's a new integration (outside scope).
