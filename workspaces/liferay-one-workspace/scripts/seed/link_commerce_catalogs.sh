#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# A commerce catalog binds to its owning supplier account only through the
# numeric account ID, and the Catalog DTO accepts no external reference code for
# that binding. The site initializer therefore creates the supplier catalogs
# without an owner, since the account ID does not exist at import time. Those
# bindings are established here, as a bootstrap step that runs after Liferay is
# healthy and the client extensions (including the site initializer) have been
# deployed, by resolving each supplier account through its external reference
# code and assigning it to the matching catalog.
#
# Each entry below is "catalogERC accountERC". Keep sorted.

CATALOG_ACCOUNTS=(
	"LIFERAY_INC_CATALOG ACCNT-020"
	"SALESFORCE_CATALOG ACCNT-020"
	"TEST_SUPPLIER_CATALOG ACCNT-021"
)

function main {
	_acquire_oauth_token

	local catalog_account

	for catalog_account in "${CATALOG_ACCOUNTS[@]}"
	do
		_link_commerce_catalog ${catalog_account}
	done
}

function _link_commerce_catalog {
	local catalog_external_reference_code="${1}"
	local account_external_reference_code="${2}"

	local account_url="${LIFERAY_URL}/o/headless-commerce-admin-account/v1.0/accounts/by-externalReferenceCode/${account_external_reference_code}"
	local catalog_url="${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/catalog/by-externalReferenceCode/${catalog_external_reference_code}"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		# Wait for the supplier account to be imported by the site initializer.

		local account_id

		account_id=$(_curl "${account_url}" | _read_field "id" || true)

		if [[ -z ${account_id} ]] || [[ ${account_id} == "0" ]]
		then
			sleep 5

			continue
		fi

		# Assign the account to the catalog. This fails until the catalog exists,
		# so retry until the PATCH succeeds.

		local status

		status=$(_curl \
			--data "{\"accountId\": ${account_id}}" \
			--header "Content-Type: application/json" \
			--output /dev/null \
			--request PATCH \
			--write-out "%{http_code}" \
			"${catalog_url}" || true)

		if [[ ${status} == 2* ]]
		then
			echo "Linked catalog ${catalog_external_reference_code} to account ${account_external_reference_code}."

			return 0
		fi

		sleep 3
	done

	echo "Unable to link catalog ${catalog_external_reference_code} to account ${account_external_reference_code}." >&2

	return 1
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

main "${@}"