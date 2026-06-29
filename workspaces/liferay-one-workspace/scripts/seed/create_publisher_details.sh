#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# The PublisherDetails record powers the Publisher Dashboard's profile page and
# is keyed to a supplier's Commerce catalog through the numeric catalogId. That
# ID does not exist at site initializer import time, so the record cannot be
# seeded as an object entry. It is therefore created here, as a bootstrap step
# that runs after Liferay is healthy, the client extensions (including the site
# initializer) have been deployed, and the supplier catalogs have been linked to
# their accounts. The catalog is resolved through its external reference code,
# and the record is upserted by its own external reference code so re-runs are
# idempotent.
#
# Each entry below is "publisherDetailsERC catalogERC jsonFile". Keep sorted.

PUBLISHER_DETAILS=(
	"PUBDET-TEST-SUPPLIER TEST_SUPPLIER_CATALOG data/publisher-details/test-supplier.json"
)

function main {
	_acquire_oauth_token

	local publisher_details

	for publisher_details in "${PUBLISHER_DETAILS[@]}"
	do
		_create_publisher_details ${publisher_details}
	done
}

function _create_publisher_details {
	local external_reference_code="${1}"
	local catalog_external_reference_code="${2}"
	local json_file="${3}"

	local catalog_url="${LIFERAY_URL}/o/headless-commerce-admin-catalog/v1.0/catalog/by-externalReferenceCode/${catalog_external_reference_code}"

	local publisher_details_url="${LIFERAY_URL}/o/c/publisherdetailses/by-external-reference-code/${external_reference_code}"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		# Wait for the supplier catalog to be imported by the site initializer.

		local catalog_id

		catalog_id=$(_curl "${catalog_url}" | _read_field "id" || true)

		if [[ -z ${catalog_id} ]] || [[ ${catalog_id} == "0" ]]
		then
			sleep 5

			continue
		fi

		# Upsert the PublisherDetails record with the resolved catalog ID.

		local body

		body=$(_read_body "${json_file}" "${catalog_id}")

		local status

		status=$(_curl \
			--data "${body}" \
			--header "Content-Type: application/json" \
			--output /dev/null \
			--request PUT \
			--write-out "%{http_code}" \
			"${publisher_details_url}" || true)

		if [[ ${status} == 2* ]]
		then
			echo "Created publisher details ${external_reference_code} for catalog ${catalog_external_reference_code}."

			return 0
		fi

		sleep 3
	done

	echo "Unable to create publisher details ${external_reference_code} for catalog ${catalog_external_reference_code}." >&2

	return 1
}

function _read_body {
	local json_file="${1}"
	local catalog_id="${2}"

	python3 -c "
import json

with open('${json_file}') as file:
	data = json.load(file)

data['catalogId'] = ${catalog_id}

print(json.dumps(data))
"
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