#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source _teardown_common.sh

# Layer 2 teardown: removes the structural scaffolding the liferay-one-batch
# client extension and the site initializer create -- object relationships,
# the custom fields added to system objects, object definitions, object
# folders, commerce configuration (channel, catalogs, price lists,
# specifications, options, option categories, currencies, order types), roles,
# taxonomy vocabularies, and list type definitions. It mirrors the batch import
# order in reverse.
#
# Object relationships are deleted before the object definitions they connect:
# Liferay refuses to delete an object definition that still participates in a
# relationship ("object relationships must be deleted before objects can be
# deleted"). Object definitions are deleted before list type definitions,
# because object fields may reference a list type as a picklist. Deleting an
# object definition cascades any entries that remain, but this script assumes
# the seeded records were already removed -- run teardown_records.sh first, or
# run teardown.sh --full, which chains the two.
#
# This layer does not delete site-initializer layouts, fragments, navigation
# menus, style books, or expando columns: those are bound to the site and the
# reliable way to reset them is to rebuild the environment (/one-env-reset) or
# re-run the site initializer.
#
# IMPORTANT -- the deletions here are not durable while the liferay-one-batch
# client extension is deployed. Deleting an object definition unregisters the
# object's dynamically generated REST services, which triggers an OSGi bundle
# refresh; that refresh re-activates the "type: batch" client extension, which
# re-imports every object definition, list type, role, and commerce
# configuration it owns (observed as "Refresh Thread: Equinox Container"
# batch engine import tasks in the portal log). The deletes below succeed, but
# the batch extension restores its data seconds later. For a durable full
# teardown, first stop or undeploy the liferay-one-batch (and
# liferay-one-site-initializer) client extensions, or just rebuild the
# environment with /one-env-reset. The records layer (teardown_records.sh)
# deletes no definitions, triggers no refresh, and is durable on its own.

function main {
	_delete_object_relationships
	_delete_commerce_channel
	_delete_commerce_catalogs
	_delete_commerce_price_lists
	_delete_commerce_specifications
	_delete_commerce_options
	_delete_commerce_option_categories
	_delete_commerce_currencies
	_delete_commerce_order_types
	_delete_user_groups
	_delete_roles
	_delete_taxonomy_vocabularies
	_delete_system_object_fields
	_delete_object_definitions
	_delete_object_folders
	_delete_list_type_definitions

	_log "Structure teardown complete."
}

function _delete_object_relationships {
	_log "Deleting object relationships..."

	local source_external_reference_code relationship_names

	while IFS=$'\t' read -r source_external_reference_code relationship_names
	do
		[[ -z ${source_external_reference_code} ]] && continue

		local definition_id

		definition_id=$(_resolve_object_definition_id "${source_external_reference_code}")

		if [[ -z ${definition_id} || ${definition_id} == "0" ]]
		then
			_warn "  No object definition ${source_external_reference_code}; skipping its relationships."

			continue
		fi

		local list_url="${LIFERAY_URL}/o/object-admin/v1.0/object-definitions/${definition_id}/object-relationships"
		local delete_url="${LIFERAY_URL}/o/object-admin/v1.0/object-relationships"

		# Object relationships are matched by name, not by external reference
		# code.

		local id name

		while IFS=$'\t' read -r id name
		do
			[[ -z ${id} ]] && continue

			if [[ " ${relationship_names} " == *" ${name} "* ]]
			then
				_delete_by_id "${delete_url}" "${id}" "relationship ${name}"
			fi
		done < <(_curl "${list_url}?page=1&pageSize=200" | _id_name_pairs)
	done < <(_read_relationships_by_source)
}

function _delete_commerce_channel {
	_log "Deleting commerce channel..."

	_delete_from_collection "commerce channel" \
		"${LIFERAY_URL}/o/headless-commerce-admin-channel/v1.0/channels" \
		"${LIFERAY_URL}/o/headless-commerce-admin-channel/v1.0/channels" \
		"LIFERAY_ONE_CHANNEL"
}

function _delete_commerce_catalogs {

	# The catalog delete-by-id path is singular (/catalog/{id}), while the list
	# path is plural (/catalogs).

	_log "Deleting commerce catalogs..."

	_delete_from_collection "commerce catalogs" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/catalogs" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/catalog" \
		"LIFERAY_INC_CATALOG" "SALESFORCE_CATALOG" "TEST_SUPPLIER_CATALOG"
}

function _delete_commerce_price_lists {
	_log "Deleting commerce price lists..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/11-commerce-price-list.batch-engine-data.json")

	_delete_from_collection "commerce price lists" \
		"${LIFERAY_URL}/o/headless-commerce-admin-pricing/v1.0/priceLists" \
		"${LIFERAY_URL}/o/headless-commerce-admin-pricing/v1.0/priceLists" \
		${ercs}
}

function _delete_commerce_specifications {
	_log "Deleting commerce specifications..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/10-commerce-specification.batch-engine-data.json")

	_delete_from_collection "commerce specifications" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/specifications" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/specifications" \
		${ercs}
}

function _delete_commerce_options {
	_log "Deleting commerce options..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/09-commerce-option.batch-engine-data.json")

	_delete_from_collection "commerce options" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/options" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/options" \
		${ercs}
}

function _delete_commerce_option_categories {
	_log "Deleting commerce option categories..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/08-commerce-option-category.batch-engine-data.json")

	_delete_from_collection "commerce option categories" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/optionCategories" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/optionCategories" \
		${ercs}
}

function _delete_commerce_currencies {

	# Only the seeded currencies are deleted, so the portal's default
	# currencies are left untouched.

	_log "Deleting commerce currencies..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/07-commerce-currency.batch-engine-data.json")

	_delete_from_collection "commerce currencies" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/currencies" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/currencies" \
		${ercs}
}

function _delete_commerce_order_types {
	_log "Deleting commerce order types..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/12-commerce-order-type.batch-engine-data.json")

	_delete_from_collection "commerce order types" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/order-types" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/order-types" \
		${ercs}
}

function _delete_user_groups {

	# User groups are deleted before the roles they carry, mirroring the batch
	# import order (roles, then user groups) in reverse.

	_log "Deleting user groups..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/16-user-group.batch-engine-data.json")

	_delete_from_collection "user groups" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/user-groups" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/user-groups" \
		${ercs}
}

function _delete_roles {

	# Only the custom (C_) roles from the batch are deleted. Built-in roles use
	# Liferay system (L_) external reference codes and must not be removed.

	_log "Deleting custom roles..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/13-role.batch-engine-data.json" | grep -v '^L_' || true)

	_delete_from_collection "custom roles" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/roles" \
		"${LIFERAY_URL}/o/headless-admin-user/v1.0/roles" \
		${ercs}
}

function _delete_taxonomy_vocabularies {

	# Deleting a vocabulary cascades its taxonomy categories.

	_log "Deleting taxonomy vocabularies..."

	local site_id

	site_id=$(_resolve_site_id)

	if [[ -z ${site_id} ]]
	then
		_warn "Unable to resolve site \"${SITE_FRIENDLY_URL_PATH}\"; skipping taxonomy vocabularies."

		return 0
	fi

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/15-taxonomy-vocabulary.batch-engine-data.json")

	_delete_from_collection "taxonomy vocabularies" \
		"${LIFERAY_URL}/o/headless-admin-taxonomy/v1.0/sites/${site_id}/taxonomy-vocabularies" \
		"${LIFERAY_URL}/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies" \
		${ercs}
}

function _delete_system_object_fields {

	# The batch adds custom (non-system) fields to the L_COMMERCE_ORDER_ITEM and
	# L_COMMERCE_PRODUCT_DEFINITION system objects. The system objects
	# themselves are not deleted; only the custom fields added to them are.

	_log "Deleting custom fields added to system objects..."

	local object_external_reference_code field_ercs

	while IFS=$'\t' read -r object_external_reference_code field_ercs
	do
		[[ -z ${object_external_reference_code} ]] && continue

		local definition_id

		definition_id=$(_resolve_object_definition_id "${object_external_reference_code}")

		if [[ -z ${definition_id} || ${definition_id} == "0" ]]
		then
			_warn "  No object definition ${object_external_reference_code}; skipping its custom fields."

			continue
		fi

		local list_url="${LIFERAY_URL}/o/object-admin/v1.0/object-definitions/${definition_id}/object-fields"
		local delete_url="${LIFERAY_URL}/o/object-admin/v1.0/object-fields"

		local id erc

		while IFS=$'\t' read -r id erc
		do
			[[ -z ${id} ]] && continue

			if [[ " ${field_ercs} " == *" ${erc} "* ]]
			then
				_delete_by_id "${delete_url}" "${id}" "object field ${erc}"
			fi
		done < <(_curl "${list_url}?page=1&pageSize=200" | _id_erc_pairs)
	done < <(_read_system_object_fields)
}

function _delete_object_definitions {
	_log "Deleting object definitions..."

	local external_reference_code

	for external_reference_code in $(_read_object_definition_ercs)
	do
		local definition_id

		definition_id=$(_resolve_object_definition_id "${external_reference_code}")

		if [[ -z ${definition_id} || ${definition_id} == "0" ]]
		then
			continue
		fi

		_delete_by_id "${LIFERAY_URL}/o/object-admin/v1.0/object-definitions" "${definition_id}" "object definition ${external_reference_code}"
	done
}

function _delete_object_folders {
	_log "Deleting object folders..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/01-object-folder.batch-engine-data.json")

	_delete_from_collection "object folders" \
		"${LIFERAY_URL}/o/object-admin/v1.0/object-folders" \
		"${LIFERAY_URL}/o/object-admin/v1.0/object-folders" \
		${ercs}
}

function _delete_list_type_definitions {
	_log "Deleting list type definitions..."

	local ercs

	ercs=$(_read_ercs "${BATCH_DIR}/00-list-type-definition.batch-engine-data.json")

	_delete_from_collection "list type definitions" \
		"${LIFERAY_URL}/o/headless-admin-list-type/v1.0/list-type-definitions" \
		"${LIFERAY_URL}/o/headless-admin-list-type/v1.0/list-type-definitions" \
		${ercs}
}

function _read_object_definition_ercs {
	python3 -c "
import json

for path in (
	'${BATCH_DIR}/03-object-definition.batch-engine-data.json',
	'${BATCH_DIR}/05-object-definition-account-entry-restricted.batch-engine-data.json',
):
	with open(path) as file:
		for item in json.load(file).get('items', []):
			external_reference_code = item.get('externalReferenceCode')

			if external_reference_code:
				print(external_reference_code)
" | sort -u
}

function _read_relationships_by_source {
	python3 -c "
import collections
import json

with open('${BATCH_DIR}/04-object-relationship.batch-engine-data.json') as file:
	items = json.load(file)['items']

groups = collections.OrderedDict()

for item in items:
	source = item.get('objectDefinitionExternalReferenceCode1')
	name = item.get('name')

	if source and name:
		groups.setdefault(source, []).append(name)

for source, names in groups.items():
	print('{}\t{}'.format(source, ' '.join(names)))
"
}

function _read_system_object_fields {
	python3 -c "
import json

with open('${BATCH_DIR}/02-system-object-field.batch-engine-data.json') as file:
	items = json.load(file)['items']

for item in items:
	object_external_reference_code = item.get('externalReferenceCode')

	field_ercs = [
		object_field.get('externalReferenceCode')
		for object_field in item.get('objectFields', [])
		if not object_field.get('system') and object_field.get('externalReferenceCode')
	]

	if object_external_reference_code and field_ercs:
		print('{}\t{}'.format(object_external_reference_code, ' '.join(field_ercs)))
"
}

main "${@}"