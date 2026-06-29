#!/bin/bash

# Shared helpers for the teardown scripts (teardown.sh, teardown_records.sh,
# teardown_structure.sh). These mirror the bootstrap seed scripts in reverse:
# where bootstrap.sh creates data through the headless and object REST APIs,
# these helpers delete it.
#
# Connection and authentication (the .env loader, the LIFERAY_* defaults, the
# OAuth token minting, and the authenticated _curl wrapper) come from _common.sh,
# which the seed scripts share. Only the delete-specific helpers live here.
#
# Deletions are resolved to numeric IDs and issued by ID. They deliberately do
# not use each API's "by external reference code" delete path, because its
# spelling is inconsistent across Liferay headless APIs (some use
# by-externalReferenceCode, others by-external-reference-code, and a few expose
# no such path at all). Listing a collection and deleting by ID works the same
# way everywhere.
#
# errexit is intentionally not set: a delete that returns 404 (already gone) is
# a normal, idempotent outcome, and one failure should not abort the rest of
# the teardown. _common.sh enables errexit, so it is turned back off right after
# sourcing.

source "$(dirname "${BASH_SOURCE[0]}")/../_common.sh"

set +o errexit

SITE_FRIENDLY_URL_PATH="${SITE_FRIENDLY_URL_PATH:-one}"

SITE_INITIALIZER_DIR="${SITE_INITIALIZER_DIR:-${WORKSPACE_ROOT}/client-extensions/liferay-one-site-initializer/site-initializer}"

# Deletes every item of a paged collection by numeric ID. With one or more
# external reference codes, only items whose externalReferenceCode matches are
# deleted; with none, every item is deleted. All pages are collected before any
# delete, so deletions never shift items between pages mid-pass.
#
# Args: label list_url delete_url [erc...]

function _delete_from_collection {
	local label="${1}"
	local list_url="${2}"
	local delete_url="${3}"

	shift 3

	local codes="${*}"

	local separator="?"

	[[ ${list_url} == *"?"* ]] && separator="&"

	local all_pairs=""
	local page=1

	while :
	do
		local response

		response=$(_curl "${list_url}${separator}page=${page}&pageSize=100")

		local output

		output=$(echo "${response}" | _matching_ids "${codes}")

		local count

		count=$(echo "${output}" | sed -n 's/^COUNT //p')

		local body

		body=$(echo "${output}" | grep -v '^COUNT ' || true)

		[[ -n ${body} ]] && all_pairs+="${body}"$'\n'

		[[ -z ${count} || ${count} -lt 100 ]] && break

		page=$((page + 1))

		[[ ${page} -gt 1000 ]] && break
	done

	local deleted=0
	local id erc

	while IFS=$'\t' read -r id erc
	do
		[[ -z ${id} ]] && continue

		if _delete_by_id "${delete_url}" "${id}" "${erc:-${label}}"
		then
			deleted=$((deleted + 1))
		fi
	done < <(echo "${all_pairs}")

	_log "Deleted ${deleted} ${label}."
}

# Deletes a single resource by ID. A 404 is treated as success: the resource is
# already gone, which is the desired end state.
#
# Args: delete_url id label

function _delete_by_id {
	local delete_url="${1}"
	local id="${2}"
	local label="${3}"

	local status

	status=$(_curl \
		--output /dev/null \
		--request DELETE \
		--write-out "%{http_code}" \
		"${delete_url}/${id}" || true)

	if [[ ${status} == 2* || ${status} == 404 ]]
	then
		_log "  Deleted ${label} (${id})."

		return 0
	fi

	_warn "  Unable to delete ${label} (${id}): HTTP ${status}."

	return 1
}

# Deletes every entry of a custom object, resolving the object's scoped REST
# context path (e.g. /o/c/contracts) from its object definition.
#
# Args: object_definition_external_reference_code

function _delete_object_entries {
	local object_external_reference_code="${1}"

	local rest_context_path

	rest_context_path=$(_resolve_rest_context_path "${object_external_reference_code}")

	if [[ -z ${rest_context_path} ]]
	then
		_warn "Unable to resolve REST context path for ${object_external_reference_code}; skipping its entries."

		return 0
	fi

	local url="${LIFERAY_URL}${rest_context_path}"

	_delete_from_collection "${object_external_reference_code} entries" "${url}" "${url}"
}

function _resolve_object_definition_id {
	local object_external_reference_code="${1}"

	_curl "${LIFERAY_URL}/o/object-admin/v1.0/object-definitions/by-external-reference-code/${object_external_reference_code}" | _json_field "id"
}

function _resolve_rest_context_path {
	local object_external_reference_code="${1}"

	_curl "${LIFERAY_URL}/o/object-admin/v1.0/object-definitions/by-external-reference-code/${object_external_reference_code}" | _json_field "restContextPath"
}

function _resolve_site_id {
	_curl "${LIFERAY_URL}/o/headless-admin-user/v1.0/sites/by-friendly-url-path/${SITE_FRIENDLY_URL_PATH}" | _json_field "id"
}

# Reads one page of a collection from stdin. Prints "id<TAB>externalReferenceCode"
# for each matching item, then a final "COUNT <items on page>" line that the
# caller uses to detect the last page. With no codes, every item matches.
#
# The id printed is taken from the ID_FIELD field, which defaults to "id". A few
# collections delete by a different field than they list by: commerce products,
# for example, list an "id" (the CPDefinition id) but delete by "productId" (the
# CProduct id). Set ID_FIELD before calling _delete_from_collection in that case.
#
# Args: space-separated external reference codes (may be empty)

function _matching_ids {
	python3 -c "
import json
import os
import sys

codes = set(filter(None, sys.argv[1].split())) if len(sys.argv) > 1 else set()

id_field = os.environ.get('ID_FIELD') or 'id'

try:
	items = json.load(sys.stdin).get('items', [])
except Exception:
	items = []

for item in items:
	external_reference_code = item.get('externalReferenceCode') or ''

	if not codes or external_reference_code in codes:
		print('{}\t{}'.format(item.get(id_field), external_reference_code))

print('COUNT {}'.format(len(items)))
" "${1:-}"
}

# Prints "id<TAB>name" for each item of a collection read from stdin.

function _id_name_pairs {
	python3 -c "
import json
import sys

try:
	items = json.load(sys.stdin).get('items', [])
except Exception:
	items = []

for item in items:
	print('{}\t{}'.format(item.get('id'), item.get('name')))
"
}

# Prints "id<TAB>externalReferenceCode" for each item of a collection read from
# stdin.

function _id_erc_pairs {
	python3 -c "
import json
import sys

try:
	items = json.load(sys.stdin).get('items', [])
except Exception:
	items = []

for item in items:
	print('{}\t{}'.format(item.get('id'), item.get('externalReferenceCode') or ''))
"
}

# Prints the externalReferenceCode of every item in a batch engine document, or
# of every top-level item when the file is a bare array.
#
# Args: file

function _read_ercs {
	python3 -c "
import json
import sys

with open(sys.argv[1]) as file:
	data = json.load(file)

items = data.get('items', []) if isinstance(data, dict) else data

for item in items:
	external_reference_code = item.get('externalReferenceCode')

	if external_reference_code:
		print(external_reference_code)
" "${1}"
}

# Minting happens once, at source time and at top level, so a failed token
# request aborts the sourcing script. Acquiring inside _curl would not: _curl
# runs in command-substitution subshells throughout the teardown, where an exit
# would terminate only the subshell.

_acquire_oauth_token