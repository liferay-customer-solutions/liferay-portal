#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# Journal articles are demo content surfaced through dynamic asset list
# collections (by web content structure and publish date), so the pages depend
# only on the collection definitions in the site initializer, not on any
# individual article. The articles themselves are therefore seeded here, as a
# bootstrap step that runs after Liferay is healthy and the client extensions
# (including the site initializer) have been deployed, once the site, the
# Announcement web content structure, and the Article Categories vocabulary
# exist. StructuredContent only accepts numeric references for its structure,
# folder, and categories, so those are resolved at runtime from their external
# reference codes here. Articles upsert by external reference code, so re-runs
# are idempotent.

SITE_FRIENDLY_URL_PATH="${SITE_FRIENDLY_URL_PATH:-one}"

CONTENT_STRUCTURE_NAME="Announcement"

TAXONOMY_VOCABULARY_EXTERNAL_REFERENCE_CODE="T_AC"

declare -A CATEGORY_IDS

function main {
	_acquire_oauth_token

	local site_id

	site_id=$(_get "${LIFERAY_URL}/o/headless-admin-user/v1.0/sites/by-friendly-url-path/${SITE_FRIENDLY_URL_PATH}" | _read_field "id")

	if [[ -z ${site_id} ]]
	then
		echo "Unable to resolve site \"${SITE_FRIENDLY_URL_PATH}\"." >&2

		return 1
	fi

	local content_structure_id

	content_structure_id=$(_get "${LIFERAY_URL}/o/headless-delivery/v1.0/sites/${site_id}/content-structures?pageSize=100" | _read_item_id_by_name "${CONTENT_STRUCTURE_NAME}")

	_read_category_ids "${site_id}"

	local folder_metadata

	for folder_metadata in data/journal-articles/*.metadata.json
	do
		_seed_folder "${site_id}" "${content_structure_id}" "${folder_metadata}"
	done
}

function _seed_folder {
	local site_id="${1}"
	local content_structure_id="${2}"
	local folder_metadata="${3}"

	local folder_external_reference_code
	folder_external_reference_code=$(_read_field_from_file "${folder_metadata}" "externalReferenceCode")

	local folder_name
	folder_name=$(_read_field_from_file "${folder_metadata}" "name")

	local folder_id
	folder_id=$(_ensure_folder "${site_id}" "${folder_external_reference_code}" "${folder_name}")

	if [[ -z ${folder_id} ]]
	then
		echo "Unable to ensure folder ${folder_external_reference_code}." >&2

		return 1
	fi

	local article_directory="${folder_metadata%.metadata.json}"

	local article_metadata

	for article_metadata in "${article_directory}"/*.json
	do
		_seed_article "${site_id}" "${content_structure_id}" "${folder_id}" "${article_metadata}"
	done
}

function _seed_article {
	local site_id="${1}"
	local content_structure_id="${2}"
	local folder_id="${3}"
	local article_metadata="${4}"

	local article_xml="${article_metadata%.json}.xml"

	local external_reference_code
	external_reference_code=$(_read_field_from_file "${article_metadata}" "articleId")

	local body
	body=$(_build_structured_content "${article_metadata}" "${article_xml}" "${content_structure_id}" "${folder_id}")

	local status

	status=$(_curl \
		--data "${body}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request PUT \
		--write-out "%{http_code}" \
		"${LIFERAY_URL}/o/headless-delivery/v1.0/sites/${site_id}/structured-contents/by-external-reference-code/${external_reference_code}")

	if [[ ${status} == 2* ]]
	then
		echo "Seeded journal article ${external_reference_code}."
	else
		echo "Unable to seed journal article ${external_reference_code} (HTTP ${status})." >&2

		return 1
	fi
}

function _ensure_folder {
	local site_id="${1}"
	local external_reference_code="${2}"
	local name="${3}"

	local folder_id

	folder_id=$(_get "${LIFERAY_URL}/o/headless-delivery/v1.0/sites/${site_id}/structured-content-folders?pageSize=100" | _read_item_id_by_erc "${external_reference_code}")

	if [[ -n ${folder_id} ]]
	then
		echo "${folder_id}"

		return 0
	fi

	_curl \
		--data "{\"externalReferenceCode\": \"${external_reference_code}\", \"name\": \"${name}\"}" \
		--header "Content-Type: application/json" \
		--request POST \
		"${LIFERAY_URL}/o/headless-delivery/v1.0/sites/${site_id}/structured-content-folders" | _read_field "id"
}

function _read_category_ids {
	local site_id="${1}"

	local taxonomy_vocabulary_id

	taxonomy_vocabulary_id=$(_get "${LIFERAY_URL}/o/headless-admin-taxonomy/v1.0/sites/${site_id}/taxonomy-vocabularies?pageSize=100" | _read_item_id_by_erc "${TAXONOMY_VOCABULARY_EXTERNAL_REFERENCE_CODE}")

	local line

	while IFS=$'\t' read -r erc id
	do
		CATEGORY_IDS["${erc}"]="${id}"
	done < <(_get "${LIFERAY_URL}/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies/${taxonomy_vocabulary_id}/taxonomy-categories?pageSize=100" | python3 -c "
import json
import sys

for item in json.load(sys.stdin).get('items', []):
	print('{}\t{}'.format(item.get('externalReferenceCode'), item.get('id')))
")
}

function _build_structured_content {
	local article_metadata="${1}"
	local article_xml="${2}"
	local content_structure_id="${3}"
	local folder_id="${4}"

	local taxonomy_category_ids="[]"

	local erc

	for erc in $(_read_asset_category_ercs "${article_metadata}")
	do
		local id="${CATEGORY_IDS[${erc}]:-}"

		if [[ -n ${id} ]]
		then
			taxonomy_category_ids=$(python3 -c "
import json
import sys

ids = json.loads(sys.argv[1])
ids.append(int(sys.argv[2]))
print(json.dumps(ids))
" "${taxonomy_category_ids}" "${id}")
		fi
	done

	python3 -c "
import json
import sys
import xml.etree.ElementTree as ElementTree

with open(sys.argv[1]) as file:
	metadata = json.load(file)

tree = ElementTree.parse(sys.argv[2])

content_fields = []

for dynamic_element in tree.getroot().findall('dynamic-element'):
	name = dynamic_element.get('field-reference') or dynamic_element.get('name')

	dynamic_content = dynamic_element.find('dynamic-content')

	content_fields.append({
		'contentFieldValue': {'data': (dynamic_content.text or '').strip()},
		'fieldReference': name,
		'name': name,
	})

print(json.dumps({
	'contentFields': content_fields,
	'contentStructureId': int(sys.argv[3]),
	'structuredContentFolderId': int(sys.argv[4]),
	'taxonomyCategoryIds': json.loads(sys.argv[5]),
	'title': metadata['name'],
}))
" "${article_metadata}" "${article_xml}" "${content_structure_id}" "${folder_id}" "${taxonomy_category_ids}"
}

function _get {
	_curl "${1}"
}

function _read_asset_category_ercs {
	python3 -c "
import json
import sys

with open(sys.argv[1]) as file:
	print('\n'.join(json.load(file).get('assetCategoryERCs', [])))
" "${1}"
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

function _read_field_from_file {
	python3 -c "
import json
import sys

with open(sys.argv[1]) as file:
	print(json.load(file).get(sys.argv[2], ''))
" "${1}" "${2}"
}

function _read_item_id_by_erc {
	local external_reference_code="${1}"

	python3 -c "
import json
import sys

for item in json.load(sys.stdin).get('items', []):
	if item.get('externalReferenceCode') == sys.argv[1]:
		print(item.get('id'))

		break
" "${external_reference_code}"
}

function _read_item_id_by_name {
	local name="${1}"

	python3 -c "
import json
import sys

for item in json.load(sys.stdin).get('items', []):
	if item.get('name') == sys.argv[1]:
		print(item.get('id'))

		break
" "${name}"
}

main "${@}"