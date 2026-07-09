#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# The commerce batch engine imports the Product entries in
# data/06-commerce-product.batch-engine-data.json but silently drops the nested
# "skus" on the Liferay product entries -- it keeps them only for the app
# entries -- so those products import with no SKU. The task reports COMPLETED
# with no failed items, so the loss is invisible, and re-running the batch does
# not recover the SKUs.
#
# Order placement resolves each order item by its SKU external reference code at
# the CPInstance level (company scoped, catalog independent), so a product
# without a SKU makes every order that references it fail with "Invalid order
# item sku". That stalled populate_orders on its retry loop and, under errexit,
# aborted the whole seed on the first order.
#
# This step recreates the SKUs declaratively from the same source file through
# the synchronous catalog endpoint, which -- unlike the batch import -- reliably
# creates the nested SKU. The endpoint upserts by the SKU external reference
# code, so it is idempotent: re-running is safe, and so is running it against the
# app products whose SKUs the batch already created. It runs after
# seed_test_data (which imports the products) and before populate_orders (which
# needs the SKUs).

PRODUCT_BATCH_FILE="data/06-commerce-product.batch-engine-data.json"

function main {
	_acquire_oauth_token

	local product_external_reference_code
	local sku_payload

	while IFS=$'\t' read -r product_external_reference_code sku_payload
	do
		[[ -z ${product_external_reference_code} ]] && continue

		_ensure_sku "${product_external_reference_code}" "${sku_payload}"
	done < <(_read_product_skus)
}

function _ensure_sku {
	local product_external_reference_code="${1}"
	local sku_payload="${2}"

	local sku_external_reference_code

	sku_external_reference_code=$(echo "${sku_payload}" | _read_field "externalReferenceCode")

	local status

	status=$(_curl \
		--data "${sku_payload}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request POST \
		--write-out "%{http_code}" \
		"${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/products/by-externalReferenceCode/${product_external_reference_code}/skus" || true)

	if [[ ${status} == 2* ]]
	then
		echo "Ensured SKU ${sku_external_reference_code} on product ${product_external_reference_code}."
	else
		echo "Unable to ensure SKU ${sku_external_reference_code} on product ${product_external_reference_code}." >&2
	fi
}

function _read_field {
	local field="${1}"

	python3 -c "
import json
import sys

try:
	print(json.load(sys.stdin).get('${field}', ''))
except Exception:
	print('')
"
}

# Emits one line per SKU: the owning product external reference code, a tab, and
# the SKU object as JSON, taken straight from the product batch file so the SKUs
# stay a single source of truth.

function _read_product_skus {
	python3 -c "
import json

with open('${PRODUCT_BATCH_FILE}') as file:
	items = json.load(file).get('items', [])

for item in items:
	product_external_reference_code = item.get('externalReferenceCode')

	for sku in item.get('skus', []):
		print(product_external_reference_code + '\t' + json.dumps(sku))
"
}

main "${@}"
