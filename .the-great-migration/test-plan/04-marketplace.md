# Marketplace — Test Scenarios

Covers: public storefront (browse, search, app detail), purchase flows, trial provisioning, `OrderType` branching, `TrialProvisioning`, Marketo forms.

Source of truth: [`../arch/ui.md §Marketplace`](../arch/ui.md), [`../arch/business-logic.md §1.4`](../arch/business-logic.md), [`../arch/data-model.md §Marketplace domain`](../arch/data-model.md).

---

## TC-MKT-001: Marketplace homepage loads for unauthenticated user @smoke

**Actors:** `guestUser`
**Preconditions:** Site initializer deployed with Marketplace page group.

### Steps

1. Navigate to `/`.

### Assertions

- [ ] Page renders with the `marketplace-master` header (cart icon + sign-in link).
- [ ] App catalog section is visible.
- [ ] No support or admin navigation items are shown.
- [ ] Page is accessible (Lighthouse accessibility score ≥ 90 or no critical a11y violations).

---

## TC-MKT-002: Browse app catalog and filter by category @smoke

**Actors:** `guestUser`
**Preconditions:** At least 5 published Commerce products (apps) exist.

### Steps

1. Navigate to `/apps`.
2. Search for a known app name.
3. Apply a category filter.

### Assertions

- [ ] Search results update without full page reload.
- [ ] Filtered results only show apps matching the selected category.
- [ ] Clearing the filter restores all apps.
- [ ] Each app card shows: name, publisher name, price, and a thumbnail.

---

## TC-MKT-003: App detail page renders correctly @smoke

**Actors:** `guestUser`
**Preconditions:** Published app with a Publisher exists.

### Steps

1. Navigate to `/apps/{slug}`.

### Assertions

- [ ] App name, description, publisher name, screenshots, and pricing are displayed.
- [ ] **Add to Cart** or **Get** button is present.
- [ ] Publisher name links to `/publishers/{publisherSlug}`.
- [ ] Display page template `app-detail` is used.

---

## TC-MKT-004: Unauthenticated user is redirected to login on checkout @smoke

**Actors:** `guestUser`
**Preconditions:** Published app exists.

### Steps

1. Navigate to `/apps/{slug}`.
2. Click **Add to Cart**.
3. Navigate to `/cart` and proceed to checkout.

### Assertions

- [ ] User is redirected to the Liferay login page.
- [ ] After login, user is returned to the cart.

---

## TC-MKT-005: Authenticated customer completes a purchase (paid-onprem flow) @smoke

**Actors:** `customerAdmin`
**Preconditions:** `OrderType` with `provisioningFlow=paid-onprem` exists. App is linked to this OrderType.

### Steps

1. Log in as `customerAdmin`.
2. Navigate to `/apps/{slug}`.
3. Add to cart and complete checkout.
4. Confirm order placement.

### Assertions

- [ ] `CommerceOrder` is created with status `Processing`.
- [ ] A `CommerceSubscriptionEntry` is created for the subscription-enabled item.
- [ ] `TrialProvisioning` is NOT created (non-trial flow).
- [ ] Customer receives an order confirmation email (intercept notification).

---

## TC-MKT-006: Trial-cloud purchase triggers TrialProvisioning @smoke

**Actors:** `customerAdmin`
**Preconditions:** `OrderType` with `provisioningFlow=trial-cloud`, `trialDurationDays=30`. Liferay Cloud endpoint mocked.

### Steps

1. Log in as `customerAdmin`.
2. Purchase the trial-cloud app.
3. Complete checkout.

### Assertions

- [ ] `TrialProvisioning` row is created with `provisioningStatus=Provisioning`.
- [ ] `POST /o/one/v1/trial/provision/{subscriptionId}` is called (intercept).
- [ ] After mock responds, `provisioningStatus=Provisioned` and `trialEndDate=today+30`.
- [ ] Customer is shown a "Your trial is being provisioned" confirmation page.

---

## TC-MKT-007: Free-activation purchase — no provisioning call @regression

**Actors:** `customerAdmin`
**Preconditions:** `OrderType` with `provisioningFlow=free-activation`.

### Steps

1. Complete a purchase for the free-activation order type.

### Assertions

- [ ] Order completes immediately.
- [ ] No `POST /o/one/v1/trial/provision/*` call is made.
- [ ] LicenseKey is generated and emailed (intercept).

---

## TC-MKT-008: OrderType rows drive post-purchase branching (no code releases needed) @regression

**Actors:** `adminUser`
**Preconditions:** Existing `OrderType` with `provisioningFlow=trial-cloud`.

### Steps

1. Navigate to `/admin` → Commerce → OrderTypes (or via headless).
2. Add a new `OrderType` row with `provisioningFlow=paid-cloud`, `provisioningConsole=true`.
3. Link a product to this new OrderType.
4. A customer purchases the product.

### Assertions

- [ ] Post-purchase action uses the `paid-cloud` flow (Console provisioning is called, Cloud provisioning is not).
- [ ] No code deployment was required — only a new `OrderType` data row.

---

## TC-MKT-009: Publisher directory lists approved publishers @smoke

**Actors:** `guestUser`
**Preconditions:** At least 2 approved Publishers exist.

### Steps

1. Navigate to `/publishers`.

### Assertions

- [ ] Both publishers appear in the directory.
- [ ] Each card shows publisher name, logo, and description.
- [ ] Publishers with `approvalStatus ≠ Approved` are not listed.

---

## TC-MKT-010: Publisher detail page @smoke

**Actors:** `guestUser`
**Preconditions:** Approved Publisher with published assets exists.

### Steps

1. Navigate to `/publishers/{slug}`.

### Assertions

- [ ] Publisher name, description, logo, and website are shown.
- [ ] Published apps by this publisher are listed.
- [ ] Unpublished assets are not shown.

---

## TC-MKT-011: Contact Sales Marketo form loads and submits @regression

**Actors:** `guestUser`
**Preconditions:** Marketo form `3738` is configured. Marketo endpoint mocked.

### Steps

1. Navigate to `/contact-sales`.
2. Fill in the form.
3. Submit.

### Assertions

- [ ] Marketo submission call is made (intercept `page.route`).
- [ ] Success confirmation is shown.
- [ ] No client-side error in the browser console.

---

## TC-MKT-012: Product Feedback Marketo form loads and submits @regression

**Actors:** `customerAdmin`
**Preconditions:** Marketo form `6253` configured.

### Steps

1. Log in as `customerAdmin`.
2. Navigate to `/product-feedback`.
3. Fill in and submit the form.

### Assertions

- [ ] Marketo submission is made (intercepted).
- [ ] Success message displayed.

---

## TC-MKT-013: Liferay Staff user group sync — @liferay.com employee gets membership @regression

**Actors:** System (`LiferayStaffUserGroupSync` task)
**Preconditions:** A user with `emailAddress=test@liferay.com` exists but does not yet have the "Liferay Staff" user group.

### Steps

1. Trigger `LiferayStaffUserGroupSync` via `/admin/debug`.

### Assertions

- [ ] The user is added to the "Liferay Staff" user group.
- [ ] The user is added to the SSA-ACCOUNT membership.
- [ ] A user with `emailAddress=test@gmail.com` is NOT affected.

---

## TC-MKT-014: RequestProductFeedbackFan emails buyers in 7–14 day window @regression

**Actors:** System (`RequestProductFeedbackFan` task)
**Preconditions:** Commerce order completed 10 days ago.

### Steps

1. Trigger `RequestProductFeedbackFan` via debug panel.

### Assertions

- [ ] Feedback email is sent to the buyer (intercept notification).
- [ ] Orders < 7 days old or > 14 days old do not receive emails.

---

## TC-MKT-015: PublisherSalesSummary rolled up nightly @regression

**Actors:** System (`PublisherSalesSummaryRoll` task)
**Preconditions:** 3 completed Commerce orders exist for Publisher X in Q2 2026.

### Steps

1. Trigger `PublisherSalesSummaryRoll` via debug panel.

### Assertions

- [ ] A `PublisherSalesSummary` row exists for Publisher X, `quarter=2026-Q2`.
- [ ] `orderCount=3` and `grossAmount` matches the sum of the 3 orders.
- [ ] Running the task again does not create a duplicate row (idempotent).
