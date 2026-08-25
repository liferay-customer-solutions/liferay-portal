#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# CommerceOrder and CommerceOrderItem are system commerce objects, so they
# cannot be imported as site initializer object entries -- they have to be
# placed through the commerce order headless API. Because an Entitlement is the
# child of a CommerceOrderItem (see the commerceOrderItemToEntitlement object
# relationship), the order item has to exist before the entitlement can
# reference it. The entitlements are therefore created here rather than in the
# site initializer, which keeps the full Account -> Project -> Contract ->
# Order -> Order Item -> Entitlement chain intact.
#
# Each file under data/orders/ describes one order: the order with its nested order
# items, the entitlements granted by those order items, the license keys those
# entitlements unlock, and the usage events recording what those entitlements have
# consumed. Entitlements carry the order's account, contract,
# and project links directly (see the projectToEntitlement object
# relationship), matching what the EntitlementGeneration object action produces
# at runtime. This runs as a bootstrap step after Liferay is healthy and the
# client extensions (including the site initializer) have been deployed, once
# the accounts, projects, contracts, products, and entitlement definitions the
# orders reference exist.
#
# Orders are placed through a bounded pool of concurrent workers, since each is
# independent of the others. The entitlements, license keys, and usage events are
# then applied in three batch engine imports, in that order, because each layer
# references the one before it: entitlements reference their order item, license
# keys and usage events reference their entitlement.

CHANNEL_EXTERNAL_REFERENCE_CODE="LIFERAY_ONE_CHANNEL"

COMMERCE_ORDER_ITEM_OBJECT_DEFINITION_EXTERNAL_REFERENCE_CODE="L_COMMERCE_ORDER_ITEM"

ENTITLEMENT_GENERATION_OBJECT_ACTION_EXTERNAL_REFERENCE_CODE="OA_COMMERCE_ORDER_ITEM_ENTITLEMENT_GENERATION"

ORDERS_DIR="data/orders"

# Resolved at runtime and read by the EXIT trap, so it has to be global.

ENTITLEMENT_GENERATION_OBJECT_ACTION_ID=""

# The EntitlementGeneration object action fires onAfterAdd for every
# CommerceOrderItem, posting the order item to the spring boot workload so it can
# generate the entitlements. This script seeds the exact entitlements directly
# (see _import_entitlements), so the action is redundant here, and because the
# add of a system object sends a null payload it also logs a PortalCatapult error
# for every order item. It is disabled while the orders are placed and restored
# afterward through the EXIT trap.

function main {
	_acquire_oauth_token

	local channel_id

	channel_id=$(_resolve_channel_id) || return 1

	ENTITLEMENT_GENERATION_OBJECT_ACTION_ID=$(_resolve_entitlement_generation_object_action_id)

	if [[ -n ${ENTITLEMENT_GENERATION_OBJECT_ACTION_ID} ]]
	then
		trap _restore_entitlement_generation_object_action EXIT

		_set_object_action_active "${ENTITLEMENT_GENERATION_OBJECT_ACTION_ID}" "false"

		echo "Disabled the EntitlementGeneration object action while placing orders."
	else
		echo "Unable to resolve the EntitlementGeneration object action; placing orders with it enabled." >&2
	fi

	_place_orders "${channel_id}" || return 1

	# A remote token can expire (15 minutes) during a long order phase, so refresh
	# it before the batch imports. This re-mints a fresh token in OAuth mode and is
	# a no-op in basic auth or when a token was supplied.

	_acquire_oauth_token

	_import_entitlements || return 1
	_import_license_keys || return 1
	_import_usage_events || return 1
}

# Places every order file through a bounded pool of concurrent workers. Orders
# are mutually independent -- each references accounts, projects, contracts, and
# products that already exist -- so they can be created in parallel. The pool
# width is capped so a slow remote environment is not overwhelmed; override it
# with SEED_ORDER_PARALLELISM. A single order failure is counted and the rest
# continue, and the count is surfaced at the end.

function _place_orders {
	local channel_id="${1}"

	local max_parallel="${SEED_ORDER_PARALLELISM:-6}"

	local failures=0
	local running=0

	local file

	for file in "${ORDERS_DIR}"/*.json
	do
		_populate_order "${file}" "${channel_id}" &

		running=$((running + 1))

		if ((running >= max_parallel))
		then
			wait -n || failures=$((failures + 1))

			running=$((running - 1))
		fi
	done

	while ((running > 0))
	do
		wait -n || failures=$((failures + 1))

		running=$((running - 1))
	done

	if ((failures > 0))
	then
		echo "Unable to populate ${failures} order(s)." >&2

		return 1
	fi
}

function _build_order_payload {
	local file="${1}"
	local channel_id="${2}"
	local contract_id="${3}"
	local project_id="${4}"
	local publisher_sales_summary_id="${5}"

	python3 -c "
import json

with open('${file}') as file:
	order = json.load(file)['order']

# channelExternalReferenceCode is not resolved on create, so the numeric
# channelId is required. The contract, project, and publisher sales summary are
# linked through the contractToCommerceOrder, projectToCommerceOrder, and
# publisherToCommerceOrder object relationships, whose foreign key fields on the
# order take the numeric object entry IDs. The order item name is denormalized
# from the SKU on create -- sending it rejects the nested mapping -- so it is
# kept in the file for readability and dropped here. The order item custom
# fields take a different shape from the plain object the file authors and are
# applied per item afterward (see _set_order_items), so they are dropped too.

order.pop('channelExternalReferenceCode', None)
order.pop('contractExternalReferenceCode', None)
order.pop('projectExternalReferenceCode', None)
order.pop('publisherSalesSummaryExternalReferenceCode', None)

order['channelId'] = ${channel_id}

contract_id = '${contract_id}'

if contract_id:
	order['r_contractToCommerceOrder_c_contractId'] = int(contract_id)

project_id = '${project_id}'

if project_id:
	order['r_projectToCommerceOrder_c_projectId'] = int(project_id)

publisher_sales_summary_id = '${publisher_sales_summary_id}'

if publisher_sales_summary_id:
	order['r_publisherToCommerceOrder_c_publisherSalesSummaryId'] = int(
		publisher_sales_summary_id)

for order_item in order.get('orderItems', []):
	order_item.pop('customFields', None)
	order_item.pop('name', None)

print(json.dumps(order))
"
}

# Collects a nested array (entitlements or licenseKeys) from every order file
# into a single JSON array, ready to POST to a scoped batch endpoint.

function _collect_items {
	local key="${1}"

	python3 -c "
import glob
import json
import sys

items = []

for path in sorted(glob.glob('${ORDERS_DIR}/*.json')):
	with open(path) as file:
		items.extend(json.load(file).get('${key}', []))

json.dump(items, sys.stdout)
"
}

function _complete_payment {
	local file="${1}"
	local order_id="${2}"

	local payment_status

	payment_status=$(_read_field "paymentStatus" < "${file}")

	[[ -z ${payment_status} ]] && return 0

	local status

	status=$(_curl \
		--data "{\"paymentStatus\": ${payment_status}}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request PATCH \
		--write-out "%{http_code}" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/orders/${order_id}" || true)

	if [[ ${status} == 2* ]]
	then
		echo "Completed payment for order ${order_id}."
	else
		echo "Unable to complete payment for order ${order_id}." >&2
	fi
}

# Imports every order's entitlements in one batch. An entitlement references its
# order item through the commerceOrderItemToEntitlement relationship (by external
# reference code), so this runs only after every order and its items exist. The
# import upserts (a full replace, matching the per-entry PUT it replaces).

function _import_entitlements {
	local items

	items=$(_collect_items "entitlements")

	_run_batch_import "entitlements" "/o/c/entitlements" "UPDATE" "${items}"
}

# Imports every order's license keys in one batch. A license key entry carries
# only the link to its entitlement, so it partial-updates (matching the per-entry
# PATCH it replaces) to preserve the rest of each existing license key, and it
# runs after the entitlements it references exist.

function _import_license_keys {
	local items

	items=$(_collect_items "licenseKeys")

	_run_batch_import "license keys" "/o/c/licensekeys" "PARTIAL_UPDATE" "${items}"
}

# Imports every order's usage events in one batch. An event names the entitlement
# it draws down through the entitlementToUsageEvent relationship, so this runs
# after the entitlements exist. The import upserts by external reference code, and
# each event also carries a dedupe key, so a re-run neither duplicates an event nor
# double counts consumption.

function _import_usage_events {
	local items

	items=$(_collect_items "usageEvents")

	_run_batch_import "usage events" "/o/c/usageevents" "UPDATE" "${items}"
}

function _populate_order {
	local file="${1}"
	local channel_id="${2}"

	local order_external_reference_code
	local contract_external_reference_code
	local project_external_reference_code
	local publisher_sales_summary_external_reference_code

	IFS='|' read -r \
		order_external_reference_code \
		contract_external_reference_code \
		project_external_reference_code \
		publisher_sales_summary_external_reference_code \
		< <(_read_order_meta "${file}")

	local contract_id=""

	if [[ -n ${contract_external_reference_code} ]]
	then
		contract_id=$(_resolve_contract_id "${contract_external_reference_code}") || return 1
	fi

	local project_id=""
	local project_name=""

	if [[ -n ${project_external_reference_code} ]]
	then
		local project

		project=$(_resolve_project "${project_external_reference_code}") || return 1

		IFS=$'\t' read -r project_id project_name <<< "${project}"
	fi

	local publisher_sales_summary_id=""

	if [[ -n ${publisher_sales_summary_external_reference_code} ]]
	then
		publisher_sales_summary_id=$(_resolve_publisher_sales_summary_id "${publisher_sales_summary_external_reference_code}") || return 1
	fi

	local payload

	payload=$(_build_order_payload "${file}" "${channel_id}" "${contract_id}" "${project_id}" "${publisher_sales_summary_id}")

	# The order placement upserts by external reference code, so re-running is
	# idempotent. A 4xx is a permanent rejection -- bad data such as an
	# unresolvable SKU -- that retrying cannot fix, so it stops immediately and
	# surfaces the response body rather than spinning the full retry budget on a
	# doomed request. Only a transient 5xx or connection failure is retried,
	# which also covers a product or SKU still settling right after it was
	# ensured.

	local attempt
	local response
	local status

	for ((attempt = 1; attempt <= 20; attempt++))
	do
		response=$(_curl \
			--data "${payload}" \
			--header "Content-Type: application/json" \
			--request POST \
			--write-out "\n%{http_code}" \
			"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/orders" || true)

		status=$(echo "${response}" | tail -n 1)

		if [[ ${status} == 2* || ${status} == 4* ]]
		then
			break
		fi

		sleep 3
	done

	if [[ ${status} != 2* ]]
	then
		echo "Unable to create order ${order_external_reference_code} (HTTP ${status}): $(echo "${response}" | sed '$d')" >&2

		return 1
	fi

	echo "Created order ${order_external_reference_code}."

	local order_id

	order_id=$(echo "${response}" | sed '$d' | _read_field "id")

	_set_order_fields "${file}" "${order_id}" "${project_name}"
	_set_order_items "${file}" "${order_id}"
	_complete_payment "${file}" "${order_id}"
}

function _read_field {
	local field="${1}"

	python3 -c "
import json
import sys

try:
	value = json.load(sys.stdin)

	for key in '${field}'.split('.'):
		value = value[key]

	print(value)
except Exception:
	print('')
"
}

# Reads the four order fields the placement needs -- the order, contract,
# project, and publisher sales summary external reference codes -- in a single
# parse, emitted as one pipe-separated line, so an order costs one Python process
# here rather than four. The separator is pipe rather than tab because every
# field but the first is optional: an absent field is empty, and adjacent tabs
# (both being IFS whitespace) would collapse on read, shifting the remaining
# fields into the wrong variables. Pipe is not IFS whitespace, so empty fields
# are preserved.

function _read_order_meta {
	local file="${1}"

	python3 -c "
import json

with open('${file}') as file:
	order = json.load(file)['order']

print('|'.join((
	order.get('externalReferenceCode', ''),
	order.get('contractExternalReferenceCode', ''),
	order.get('projectExternalReferenceCode', ''),
	order.get('publisherSalesSummaryExternalReferenceCode', ''),
)))
"
}

# Reads the per order item patch bodies an order file implies, emitted as the
# order total on the first line followed by one tab-separated order item external
# reference code and compact JSON body per line. The total is zero for an order
# whose items are all priced at zero, and the caller then skips only the order
# level price patch.
#
# Each body carries the item's external reference code, because a patch
# regenerates it otherwise and the entitlement import links an entitlement to its
# order item by that code (see _import_entitlements). It also carries the custom
# fields the file authors, reshaped from the plain object the file uses for
# readability into the {name, customValue: {data}} array the commerce order item
# API expects. No tax is configured on the seeded channel, so the price with tax
# is the price.

function _read_order_items {
	local file="${1}"

	python3 -c "
import json

with open('${file}') as file:
	order = json.load(file)['order']

lines = []
total = 0

for order_item in order.get('orderItems', []):
	final_price = order_item.get('unitPrice', 0) * order_item.get('quantity', 1)

	total += final_price

	body = {'externalReferenceCode': order_item['externalReferenceCode']}

	custom_fields = order_item.get('customFields') or {}

	if custom_fields:
		body['customFields'] = [
			{'customValue': {'data': data}, 'name': name}
			for name, data in sorted(custom_fields.items())
		]

	if final_price:
		body['finalPrice'] = final_price
		body['finalPriceWithTaxAmount'] = final_price

	lines.append(
		'{}\t{}'.format(
			order_item['externalReferenceCode'],
			json.dumps(body, separators=(',', ':'), sort_keys=True)))

print('{:.2f}'.format(total))
print('\n'.join(lines))
"
}

function _resolve_channel_id {
	local url="${LIFERAY_URL}/o/headless-commerce-admin-channel/v1.0/channels?pageSize=100"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		local channel_id

		channel_id=$(_curl "${url}" | python3 -c "
import json
import sys

try:
	for channel in json.load(sys.stdin).get('items', []):
		if channel.get('externalReferenceCode') == '${CHANNEL_EXTERNAL_REFERENCE_CODE}':
			print(channel.get('id'))

			break
except Exception:
	pass
" || true)

		if [[ -n ${channel_id} ]]
		then
			echo "${channel_id}"

			return 0
		fi

		sleep 5
	done

	echo "Unable to resolve channel ${CHANNEL_EXTERNAL_REFERENCE_CODE}." >&2

	return 1
}

function _resolve_contract_id {
	local external_reference_code="${1}"

	local url="${LIFERAY_URL}/o/c/contracts/by-external-reference-code/${external_reference_code}"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		local contract_id

		contract_id=$(_curl "${url}" | _read_field "id" || true)

		if [[ -n ${contract_id} ]]
		then
			echo "${contract_id}"

			return 0
		fi

		sleep 5
	done

	echo "Unable to resolve contract ${external_reference_code}." >&2

	return 1
}

# Resolves a project external reference code to its numeric ID and name,
# emitted as a single tab-separated line. The ID sets the projectToCommerceOrder
# relationship foreign key on the order; the name is the denormalized projectName
# custom field the UI reads (see _set_order_fields).

function _resolve_project {
	local external_reference_code="${1}"

	local url="${LIFERAY_URL}/o/c/projects/by-external-reference-code/${external_reference_code}"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		local project

		project=$(_curl "${url}" | python3 -c "
import json
import sys

try:
	project = json.load(sys.stdin)

	print('{}\t{}'.format(project['id'], project.get('name', '')))
except Exception:
	pass
" || true)

		if [[ -n ${project} ]]
		then
			echo "${project}"

			return 0
		fi

		sleep 5
	done

	echo "Unable to resolve project ${external_reference_code}." >&2

	return 1
}

# Resolves a publisher sales summary external reference code to its numeric ID,
# which sets the publisherToCommerceOrder relationship foreign key on the order.
# A marketplace sale is attributed to the publisher's payout for a quarter
# through that relationship, which is what the Marketplace Payments page totals.

function _resolve_publisher_sales_summary_id {
	local external_reference_code="${1}"

	local url="${LIFERAY_URL}/o/c/publishersalessummaries/by-external-reference-code/${external_reference_code}"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		local publisher_sales_summary_id

		publisher_sales_summary_id=$(_curl "${url}" | _read_field "id" || true)

		if [[ -n ${publisher_sales_summary_id} ]]
		then
			echo "${publisher_sales_summary_id}"

			return 0
		fi

		sleep 5
	done

	echo "Unable to resolve publisher sales summary ${external_reference_code}." >&2

	return 1
}

# The object action can only be addressed by its numeric ID, which differs per
# environment, so it is resolved through the stable external reference codes of
# the CommerceOrderItem object definition and the action itself.

function _resolve_entitlement_generation_object_action_id {
	local url="${LIFERAY_URL}/o/object-admin/v1.0/object-definitions/by-external-reference-code/${COMMERCE_ORDER_ITEM_OBJECT_DEFINITION_EXTERNAL_REFERENCE_CODE}/object-actions"

	_curl "${url}" | python3 -c "
import json
import sys

try:
	for object_action in json.load(sys.stdin).get('items', []):
		if object_action.get('externalReferenceCode') == '${ENTITLEMENT_GENERATION_OBJECT_ACTION_EXTERNAL_REFERENCE_CODE}':
			print(object_action.get('id'))

			break
except Exception:
	pass
"
}

function _restore_entitlement_generation_object_action {
	[[ -z ${ENTITLEMENT_GENERATION_OBJECT_ACTION_ID} ]] && return 0

	_set_object_action_active "${ENTITLEMENT_GENERATION_OBJECT_ACTION_ID}" "true"

	echo "Re-enabled the EntitlementGeneration object action."
}

# Imports a JSON array of custom object entries through the object's scoped batch
# endpoint and waits for the import task to finish. The entries upsert by
# external reference code, and ON_ERROR_CONTINUE keeps a single bad entry from
# aborting the rest, exactly as the per-entry loops these batches replace did.

function _run_batch_import {
	local label="${1}"
	local rest_context_path="${2}"
	local update_strategy="${3}"
	local items="${4}"

	local count

	count=$(echo "${items}" | python3 -c "import json, sys; print(len(json.load(sys.stdin)))")

	if [[ ${count} == "0" ]]
	then
		return 0
	fi

	local url="${LIFERAY_URL}${rest_context_path}/batch?createStrategy=UPSERT&importStrategy=ON_ERROR_CONTINUE&updateStrategy=${update_strategy}"

	local import_task_id

	import_task_id=$(echo "${items}" | _curl \
		--data @- \
		--header "Content-Type: application/json" \
		"${url}" \
		| _read_field "id")

	if [[ -z ${import_task_id} ]]
	then
		echo "Unable to start batch import of ${count} ${label}." >&2

		return 1
	fi

	_wait_for_batch_import "${import_task_id}" "${label}" "${count}"
}

function _set_object_action_active {
	local object_action_id="${1}"
	local active="${2}"

	_curl \
		--data "{\"active\": ${active}}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request PATCH \
		"${LIFERAY_URL}/o/object-admin/v1.0/object-actions/${object_action_id}" || true
}

# The My Projects UI reads order fields that the order placement payload does
# not accept: the projectName and cloudProjectName custom fields (used to scope
# the project's Orders and Environment tabs) and the standard purchaseOrderNumber
# (shown as the purchase number on the product details). They are applied with a
# follow-up PATCH from the customFields object and purchaseOrderNumber in the
# order file once the order exists.
#
# projectName is a denormalized read cache: the project is linked authoritatively
# through the projectToCommerceOrder relationship (see _build_order_payload), and
# projectName is derived here from that same project's name rather than authored
# in the order file, so the relationship stays the single source of truth. The
# commerce order read APIs surface expando custom fields but not object
# relationship foreign keys, so the UI reads projectName off the order directly.

function _set_order_fields {
	local file="${1}"
	local order_id="${2}"
	local project_name="${3}"

	local payload

	payload=$(python3 -c "
import json
import sys

with open(sys.argv[1]) as file:
	data = json.load(file)

patch = {}

custom_fields = dict(data.get('customFields') or {})

project_name = sys.argv[2]

if project_name:
	custom_fields['projectName'] = project_name

if custom_fields:
	patch['customFields'] = custom_fields

if data.get('purchaseOrderNumber'):
	patch['purchaseOrderNumber'] = data['purchaseOrderNumber']

print(json.dumps(patch))
" "${file}" "${project_name}")

	[[ ${payload} == "{}" ]] && return 0

	local status

	status=$(_curl \
		--data "${payload}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request PATCH \
		--write-out "%{http_code}" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/orders/${order_id}" || true)

	if [[ ${status} == 2* ]]
	then
		echo "Set fields for order ${order_id}."
	else
		echo "Unable to set fields for order ${order_id}." >&2
	fi
}

function _set_order_item {
	local external_reference_code="${1}"
	local payload="${2}"

	local status

	status=$(_curl \
		--data "${payload}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request PATCH \
		--write-out "%{http_code}" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/orderItems/by-externalReferenceCode/${external_reference_code}" || true)

	if [[ ${status} != 2* ]]
	then
		echo "Unable to set fields for order item ${external_reference_code}." >&2
	fi
}

# Applies everything an order item needs that the order placement payload does not
# accept, in one PATCH per item.
#
# The entitlement generator reads an order item's term from its startDate and
# endDate custom fields and gates on customStatus (see OrderItemUtil), so a
# seeded item has to carry them or the runtime and the seed would disagree about
# when a grant runs and every item would read as unapproved.
#
# Commerce also prices an order from the channel price list when it is placed, so
# the unitPrice an order item carries lands on the item but leaves the item's
# final price and the order's total at zero. A seeded marketplace sale needs a
# real total -- the Marketplace Finance Orders page lists only orders that have
# one, and the Marketplace Payments page totals them per publisher -- so the
# prices ride along in the same PATCH. Only the order level total is skipped for
# an order whose items are all priced at zero.

function _set_order_items {
	local file="${1}"
	local order_id="${2}"

	local lines

	mapfile -t lines < <(_read_order_items "${file}")

	((${#lines[@]} == 0)) && return 0

	local line

	for line in "${lines[@]:1}"
	do
		local external_reference_code
		local payload

		IFS=$'\t' read -r external_reference_code payload <<< "${line}"

		_set_order_item "${external_reference_code}" "${payload}"
	done

	[[ ${lines[0]} == "0.00" ]] && return 0

	local status

	status=$(_curl \
		--data "{\"subtotal\": ${lines[0]}, \"total\": ${lines[0]}}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request PATCH \
		--write-out "%{http_code}" \
		"${LIFERAY_URL}/o/headless-commerce-admin-order/v1.0/orders/${order_id}" || true)

	if [[ ${status} == 2* ]]
	then
		echo "Set prices for order ${order_id}."
	else
		echo "Unable to set prices for order ${order_id}." >&2
	fi
}

function _wait_for_batch_import {
	local import_task_id="${1}"
	local label="${2}"
	local count="${3}"

	local url="${LIFERAY_URL}/o/headless-batch-engine/v1.0/import-task/${import_task_id}"

	local attempt

	# A completed or failed task breaks early, so the poll granularity only bounds
	# how long a fast local import waits past completion (600 attempts x 0.5s =
	# 5 minutes, generous enough for a slow remote queue).

	for ((attempt = 1; attempt <= 600; attempt++))
	do
		local task

		task=$(_curl "${url}")

		local status

		status=$(echo "${task}" | _read_field "executeStatus")

		case "${status}" in
			COMPLETED)
				echo "Imported ${count} ${label}."

				return 0
				;;
			FAILED)
				echo "Unable to import ${label}." >&2

				_curl "${url}/failed-items/report" >&2

				return 1
				;;
		esac

		sleep 0.5
	done

	echo "Timed out waiting for import of ${label}." >&2

	return 1
}

main "${@}"