#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# Overage orders are standalone CommerceOrders generated when metered usage
# exceeds an entitlement cap. Each is billed as its own order (the
# marketplaceOrderType custom field is "overage") carrying one order item for the
# overaged product, so it surfaces in that product's Orders tab on the dashboard
# alongside the original purchase order for the same product.
#
# Each file under data/overages/ describes one overage: the UsageReport that
# aggregated the usage (the audit trail) and the standalone order it generated.
# The order is created first, then the report records the order's numeric ID in
# its plain commerceOrderId field -- a plain field rather than a navigable
# relationship, because an edge into the system CommerceOrder object puts a cycle
# through the object entry OData entity model and 500s every /o/c/... endpoint.
# Like CommerceOrder itself, these cannot be site initializer entries -- the order
# goes through the commerce order headless API -- so they are seeded here, after
# populate_orders has placed the base orders the products already carry.
#
# The report is a plain custom object entry upserted by external reference code
# through its scoped /o/c/usagereports endpoint; the order is upserted by external
# reference code through the commerce order API, mirroring populate_orders. Both
# upsert, so re-running is idempotent.

CHANNEL_EXTERNAL_REFERENCE_CODE="LIFERAY_ONE_CHANNEL"

OVERAGES_DIR="data/overages"

function main {
	_acquire_oauth_token

	local channel_id

	channel_id=$(_resolve_channel_id) || return 1

	local failures=0

	local file

	for file in "${OVERAGES_DIR}"/*.json
	do
		[[ -e ${file} ]] || continue

		_populate_overage "${file}" "${channel_id}" || failures=$((failures + 1))
	done

	if ((failures > 0))
	then
		echo "Unable to populate ${failures} overage(s)." >&2

		return 1
	fi
}

# Strips the fields the commerce order create does not accept -- the external
# reference codes resolved to numeric IDs, the customFields applied by a later
# PATCH, and the order item name denormalized from the SKU -- and sets the
# channel, contract, and project foreign keys. The audit link back to the usage
# report that generated this overage is not set here: it lives on the report as
# its plain commerceOrderId field, set once this order exists (see
# _create_usage_report).

function _build_order_payload {
	local file="${1}"
	local channel_id="${2}"
	local contract_id="${3}"
	local project_id="${4}"

	python3 -c "
import json

with open('${file}') as file:
	order = json.load(file)['order']

order.pop('channelExternalReferenceCode', None)
order.pop('contractExternalReferenceCode', None)
order.pop('customFields', None)
order.pop('projectExternalReferenceCode', None)

order['channelId'] = ${channel_id}
order['r_contractToCommerceOrder_c_contractId'] = ${contract_id}

project_id = '${project_id}'

if project_id:
	order['r_projectToCommerceOrder_c_projectId'] = int(project_id)

for order_item in order.get('orderItems', []):
	order_item.pop('name', None)

print(json.dumps(order))
"
}

# Upserts the UsageReport custom object entry by external reference code and
# echoes its numeric ID. The report is the child of its project through the
# projectToUsageReport relationship (its foreign key). commerceOrderId is a
# denormalized plain field -- the audit link to the standalone overage order this
# report generated -- rather than a navigable relationship, because an edge into
# the system CommerceOrder object puts a cycle through the object entry OData
# entity model and 500s every /o/c/... endpoint.

function _create_usage_report {
	local file="${1}"
	local project_id="${2}"
	local commerce_order_id="${3}"

	local external_reference_code

	external_reference_code=$(python3 -c "
import json

with open('${file}') as file:
	print(json.load(file)['usageReport']['externalReferenceCode'])
")

	local payload

	payload=$(python3 -c "
import json

with open('${file}') as file:
	report = json.load(file)['usageReport']

# projectExternalReferenceCode is not a report field -- the project is linked
# through the projectToUsageReport relationship foreign key set below -- so it is
# stripped. contractExternalReferenceCode is a denormalized field the approved
# object action reads to build the overage order, so it is kept.

report.pop('projectExternalReferenceCode', None)

report['commerceOrderId'] = ${commerce_order_id}

project_id = '${project_id}'

if project_id:
	report['r_projectToUsageReport_c_projectId'] = int(project_id)

print(json.dumps(report))
")

	local response

	response=$(_curl \
		--data "${payload}" \
		--header "Content-Type: application/json" \
		--request PUT \
		"${LIFERAY_URL}/o/c/usagereports/by-external-reference-code/${external_reference_code}")

	echo "${response}" | _read_field "id"
}

function _populate_overage {
	local file="${1}"
	local channel_id="${2}"

	local order_external_reference_code
	local contract_external_reference_code
	local project_external_reference_code

	IFS=$'\t' read -r \
		order_external_reference_code \
		contract_external_reference_code \
		project_external_reference_code \
		< <(_read_overage_meta "${file}")

	local contract_id

	contract_id=$(_resolve_contract_id "${contract_external_reference_code}") || return 1

	local project_id=""
	local project_name=""

	if [[ -n ${project_external_reference_code} ]]
	then
		local project

		project=$(_resolve_project "${project_external_reference_code}") || return 1

		IFS=$'\t' read -r project_id project_name <<< "${project}"
	fi

	local payload

	payload=$(_build_order_payload "${file}" "${channel_id}" "${contract_id}" "${project_id}")

	# The order placement upserts by external reference code, so re-running is
	# idempotent. A 4xx is a permanent rejection that retrying cannot fix, so it
	# stops immediately; only a transient 5xx or connection failure is retried.

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
		echo "Unable to create overage order ${order_external_reference_code} (HTTP ${status}): $(echo "${response}" | sed '$d')" >&2

		return 1
	fi

	echo "Created overage order ${order_external_reference_code}."

	local order_id

	order_id=$(echo "${response}" | sed '$d' | _read_field "id")

	_set_order_fields "${file}" "${order_id}" "${project_name}"

	# The usage report is the audit trail behind this overage order; it records the
	# order's numeric ID in its commerceOrderId field, so it is created once the
	# order exists.

	local usage_report_id

	usage_report_id=$(_create_usage_report "${file}" "${project_id}" "${order_id}")

	if [[ -z ${usage_report_id} ]]
	then
		echo "Unable to create usage report for overage ${order_external_reference_code}." >&2

		return 1
	fi

	echo "Created usage report for overage ${order_external_reference_code}."
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

# Reads the three order fields the placement needs -- the order, contract, and
# project external reference codes -- in a single parse, emitted as one
# tab-separated line.

function _read_overage_meta {
	local file="${1}"

	python3 -c "
import json

with open('${file}') as file:
	order = json.load(file)['order']

print('\t'.join((
	order.get('externalReferenceCode', ''),
	order.get('contractExternalReferenceCode', ''),
	order.get('projectExternalReferenceCode', ''),
)))
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

# Resolves a project external reference code to its numeric ID and name, emitted
# as a single tab-separated line. The ID sets the projectToCommerceOrder and
# projectToUsageReport relationship foreign keys; the name is the denormalized
# projectName custom field the UI reads (see _set_order_fields).

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

# The UI reads order fields the placement payload does not accept: the
# marketplaceOrderType and projectName custom fields (used to badge the overage
# and scope it to the project's Orders tab) and the standard purchaseOrderNumber.
# They are applied with a follow-up PATCH from the customFields object and
# purchaseOrderNumber in the overage file once the order exists. projectName is a
# denormalized read cache derived from the project linked authoritatively through
# the projectToCommerceOrder relationship.

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

order = data.get('order') or {}

patch = {}

custom_fields = dict(order.get('customFields') or {})

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
		echo "Set fields for overage order ${order_id}."
	else
		echo "Unable to set fields for overage order ${order_id}." >&2
	fi
}

main "${@}"