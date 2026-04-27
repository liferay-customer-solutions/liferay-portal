# Publisher — Test Scenarios

Covers: `Publisher`, `PublisherAsset`, `PublisherAssetAttachment`, `PublisherSalesSummary`, `RequestPublisherAccount`, `product-approver-workflow`, `publisher-onboarding-workflow`.

Source of truth: [`../arch/data-model.md §Marketplace domain`](../arch/data-model.md), [`../arch/business-logic.md §1.4`](../arch/business-logic.md).

---

## TC-PUB-001: Publisher onboarding form submission @smoke

**Actors:** `guestUser`
**Preconditions:** Site is live. `BannedEmailDomain` list does not contain `example.com`.

### Steps

1. Navigate to `/publisher-onboarding`.
2. Fill in: firstName, lastName, emailAddress = `test@example.com`, companyName, phoneNumber, requestDescription.
3. Submit.

### Assertions

- [ ] `RequestPublisherAccount` row is created with `status=Submitted`.
- [ ] Kaleo workflow `publisher-onboarding-workflow` is started.
- [ ] Submitter receives a confirmation email (intercept).
- [ ] Admin receives a review notification (intercept).

---

## TC-PUB-002: Publisher onboarding blocked for banned email domain @regression

**Actors:** `guestUser`
**Preconditions:** `mailinator.com` is in `BannedEmailDomain`.

### Steps

1. Navigate to `/publisher-onboarding`.
2. Enter `email=spam@mailinator.com`.
3. Submit.

### Assertions

- [ ] Form is rejected with a domain-blocked error.
- [ ] No `RequestPublisherAccount` row is created.

---

## TC-PUB-003: Admin approves publisher onboarding — AccountEntry + Publisher auto-created @smoke

**Actors:** `adminUser` (workflow approver)
**Preconditions:** `RequestPublisherAccount` in `Under Review` state.

### Steps

1. Navigate to the workflow task in Admin UI (or `/admin/publishers`).
2. Approve the request.

### Assertions

- [ ] `RequestPublisherAccount.status` transitions to `Approved`.
- [ ] A new `AccountEntry` is created for the company.
- [ ] A `Publisher` row is created linked to the new AccountEntry.
- [ ] A Commerce `CommerceCatalog` is created and `Publisher.commerceCatalogId` is set.
- [ ] Approval email is sent to the applicant (intercept).

---

## TC-PUB-004: 1:1 Publisher–AccountEntry constraint @regression

**Actors:** `adminUser`
**Preconditions:** AccountEntry already has a Publisher linked.

### Steps

1. Attempt to create a second Publisher for the same AccountEntry via `POST /o/c/publishers`.

### Assertions

- [ ] Request is rejected.
- [ ] Error message references the 1:1 constraint.

---

## TC-PUB-005: Publisher slug is URL-safe and unique @regression

**Actors:** `adminUser`
**Preconditions:** Publisher with `slug=acme` exists.

### Steps

1. Create a second Publisher and set `slug=acme`.
2. Submit.

### Assertions

- [ ] Rejected with a uniqueness error on `slug`.

---

## TC-PUB-006: Publisher uploads an asset @smoke

**Actors:** `publisherUser` (owner of the Publisher)
**Preconditions:** Approved Publisher exists. User has `Publisher` instance role.

### Steps

1. Log in as `publisherUser`.
2. Navigate to the publisher dashboard.
3. Click **New Asset**.
4. Fill in: appName, version, releaseNotes, compatibility.
5. Attach a zip file (< 200 MB) as a `PublisherAssetAttachment`.
6. Submit.

### Assertions

- [ ] `PublisherAsset` row is created with `publishStatus=Draft`.
- [ ] `PublisherAssetAttachment` row is created with the GCS bucket/object coordinates.
- [ ] Email dispatch is sent via `MARKETPLACE-PRODUCT-SUBMIT-TEMPLATE` (intercept).
- [ ] Kaleo `product-approver-workflow` is started for the asset.

---

## TC-PUB-007: Product approver workflow — Under Review → Approved @smoke

**Actors:** `adminUser` (workflow reviewer)
**Preconditions:** `PublisherAsset` in `Under Review` state.

### Steps

1. Open the workflow task.
2. Approve the asset.

### Assertions

- [ ] `PublisherAsset.publishStatus` transitions to `Published`.
- [ ] Linked `CPDefinition` is flipped to published in Commerce.
- [ ] Publisher receives a published notification (intercept).

---

## TC-PUB-008: Product approver workflow — Rejected @regression

**Actors:** `adminUser`
**Preconditions:** `PublisherAsset` in `Under Review` state.

### Steps

1. Open the workflow task.
2. Click **Reject** with a reason.

### Assertions

- [ ] `PublisherAsset.publishStatus` transitions to `Rejected`.
- [ ] Publisher receives a rejection notification with the reason (intercept).
- [ ] `CPDefinition` remains unpublished.

---

## TC-PUB-009: Publisher suspension disables catalog @regression

**Actors:** `adminUser`
**Preconditions:** Approved Publisher with published assets.

### Steps

1. Navigate to `/admin/publishers`.
2. Set Publisher `approvalStatus=Suspended`.
3. Save.

### Assertions

- [ ] Commerce catalog is disabled (products no longer purchasable).
- [ ] Publisher's assets are hidden from the storefront.

---

## TC-PUB-010: Publisher sales summary is visible in publisher dashboard @regression

**Actors:** `publisherUser`
**Preconditions:** `PublisherSalesSummary` row exists for the current quarter.

### Steps

1. Log in as `publisherUser`.
2. Navigate to the publisher dashboard → **Sales** tab.

### Assertions

- [ ] Sales summary row is shown with `quarter`, `grossAmount`, `orderCount`.
- [ ] Publisher can view but not edit the summary (read-only).

---

## TC-PUB-011: PublisherAssetAttachment — file size limit enforced @regression

**Actors:** `publisherUser`
**Preconditions:** None.

### Steps

1. Attempt to upload a file > 200 MB as a `PublisherAssetAttachment`.

### Assertions

- [ ] Upload is rejected before or during the GCS initiation step.
- [ ] Error message references the 200 MB limit.

---

## TC-PUB-012: ProjectsUsingMarketplaceReport generates nightly snapshot @regression

**Actors:** System (`ProjectsUsingMarketplaceReport` task)
**Preconditions:** Marketplace orders exist; account environment lookups are available.

### Steps

1. Trigger `ProjectsUsingMarketplaceReport` via debug panel.

### Assertions

- [ ] A `Report` row with `reportType=projects_using_marketplace` and `status=Ready` is created.
- [ ] `payload` is valid JSON with at least one row.
- [ ] Running again updates the existing report row rather than creating a duplicate.
- [ ] Admin can view the report at `/admin/reports`.
