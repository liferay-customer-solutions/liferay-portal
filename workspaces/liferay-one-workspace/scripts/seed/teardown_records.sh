#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source _teardown_common.sh

# Layer 1 teardown: deletes the seeded records and demo content created by the
# bootstrap post-deploy scripts (seed_test_data.sh, populate_orders.sh,
# seed_journal_articles.sh, create_publisher_details.sh) and by the site
# initializer (commerce products). It leaves the object definitions,
# relationships, commerce configuration, roles, taxonomies, and list type
# definitions in place, so the data can be re-seeded without an environment
# rebuild. For a full teardown of the structural scaffolding too, run
# teardown.sh --full (or teardown_structure.sh after this).
#
# Deletion runs leaf-to-root so the object relationships the batch marks
# "prevent" never block a parent delete. Two relationships are "prevent":
# accountEntryToContract and accountEntryToEntitlement. Their children
# (contracts and entitlements) are therefore deleted before the accounts they
# hang off of. Relationships marked "cascade" or "disassociate" do not block
# deletion, but the explicit leaf-to-root order keeps the teardown
# deterministic regardless.

function main {
	_delete_commerce_orders
	_delete_entitlements
	_delete_license_keys
	_delete_contracts
	_delete_project_memberships
	_delete_projects
	_delete_publisher_data
	_delete_commerce_products
	_delete_journal_articles
	_delete_accounts
	_delete_users

	_log "Records teardown complete."
}

function _delete_commerce_orders {
	_log "Deleting commerce orders..."

	local ercs

	ercs=$(_read_order_ercs)

	_delete_from_collection "commerce orders" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/orders" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/orders" \
		${ercs}
}

function _delete_entitlements {

	# Entitlements (accountEntryToEntitlement is "prevent") must be gone before
	# their accounts. Deleting an order item only disassociates its
	# entitlements (commerceOrderItemToEntitlement is "disassociate"), so the
	# orders above leave the entitlement entries behind for this step.

	_log "Deleting entitlements..."

	_delete_object_entries "C_ENTITLEMENT"
}

function _delete_license_keys {
	_log "Deleting license keys..."

	_delete_object_entries "C_LICENSE_KEY"
}

function _delete_contracts {

	# Contracts (accountEntryToContract is "prevent") must be gone before their
	# accounts.

	_log "Deleting contracts..."

	_delete_object_entries "C_CONTRACT"
}

function _delete_project_memberships {
	_log "Deleting project memberships..."

	_delete_object_entries "C_PROJECT_MEMBERSHIP"
}

function _delete_projects {
	_log "Deleting projects..."

	_delete_object_entries "C_PROJECT"
}

function _delete_publisher_data {
	_log "Deleting publisher data..."

	_delete_object_entries "C_PUBLISHER_DETAILS"
	_delete_object_entries "C_PUBLISHER_SALES_SUMMARY"
	_delete_object_entries "C_PUBLISHER_ACCOUNT_REQUEST"
	_delete_object_entries "C_PUBLISHER_ASSET_ATTACHMENT"
	_delete_object_entries "C_PUBLISHER_ASSET"
	_delete_object_entries "C_LIFERAY_BUNDLE"
}

function _delete_commerce_products {

	# Products list a DTO "id" (the CPDefinition id) but delete by "productId"
	# (the CProduct id) at /products/{productId}, so the delete is keyed off
	# productId rather than the default id.

	_log "Deleting commerce products..."

	local ercs

	ercs=$(_read_product_ercs)

	ID_FIELD="productId" \
		_delete_from_collection "commerce products" \
			"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/products" \
			"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/products" \
			${ercs}
}

function _delete_journal_articles {
	_log "Deleting journal articles..."

	local site_id

	site_id=$(_resolve_site_id)

	if [[ -z ${site_id} ]]
	then
		_warn "Unable to resolve site \"${SITE_FRIENDLY_URL_PATH}\"; skipping journal articles."

		return 0
	fi

	local article_ercs

	article_ercs=$(_read_article_ercs)

	_delete_from_collection "journal articles" \
		"${LIFERAY_URL}/o/headless-delivery/v1.0/sites/${site_id}/structured-contents" \
		"${LIFERAY_URL}/o/headless-delivery/v1.0/structured-contents" \
		${article_ercs}

	local folder_ercs

	folder_ercs=$(_read_article_folder_ercs)

	_delete_from_collection "structured content folders" \
		"${LIFERAY_URL}/o/headless-delivery/v1.0/sites/${site_id}/structured-content-folders" \
		"${LIFERAY_URL}/o/headless-delivery/v1.0/structured-content-folders" \
		${folder_ercs}
}

function _delete_accounts {

	# Accounts are deleted by their seeded external reference codes so the
	# teardown never removes accounts created outside the bootstrap. With the
	# "prevent" children (contracts, entitlements) already gone, the remaining
	# account children ("cascade" relationships: projects, memberships,
	# properties, notes, and so on) are removed automatically.

	_log "Deleting seeded accounts..."

	local ercs

	ercs=$(_read_ercs "data/01-account.batch-engine-data.json")

	_delete_from_collection "accounts" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/accounts" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/accounts" \
		${ercs}
}

function _delete_users {
	_log "Deleting seeded user accounts..."

	local ercs

	ercs=$(_read_ercs "data/02-user-account.batch-engine-data.json")

	_delete_from_collection "user accounts" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/user-accounts" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/user-accounts" \
		${ercs}
}

function _read_article_ercs {
	python3 -c "
import glob
import json

for path in sorted(glob.glob('data/journal-articles/*/*.json')):
	with open(path) as file:
		article_id = json.load(file).get('articleId')

	if article_id:
		print(article_id)
"
}

function _read_article_folder_ercs {
	python3 -c "
import glob
import json

for path in sorted(glob.glob('data/journal-articles/*.metadata.json')):
	with open(path) as file:
		external_reference_code = json.load(file).get('externalReferenceCode')

	if external_reference_code:
		print(external_reference_code)
"
}

function _read_order_ercs {
	python3 -c "
import glob
import json

for path in sorted(glob.glob('data/orders/*.json')):
	with open(path) as file:
		external_reference_code = json.load(file).get('order', {}).get('externalReferenceCode')

	if external_reference_code:
		print(external_reference_code)
"
}

function _read_product_ercs {
	python3 -c "
import glob
import json

for path in sorted(glob.glob('${SITE_INITIALIZER_DIR}/commerce-catalogs/*.products.json')):
	with open(path) as file:
		data = json.load(file)

	items = data if isinstance(data, list) else data.get('items', [])

	for item in items:
		external_reference_code = item.get('externalReferenceCode')

		if external_reference_code:
			print(external_reference_code)
"
}

main "${@}"