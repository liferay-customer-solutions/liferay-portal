#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# Test data records (accounts, users, projects, contracts, license keys,
# project memberships, and publisher records) are environment-specific seed
# data, so they are not shipped in the liferay-one-batch client extension,
# which deploys to every environment. They are instead imported here, as a
# bootstrap step that runs after Liferay is healthy and the client extensions
# (including the batch object definitions) have been deployed. Each
# batch-engine-data file under data is a batch engine document; the files are
# imported in filename order so dependencies resolve (accounts before projects,
# projects before memberships, and so on). Imports upsert by external reference
# code, so re-runs are idempotent.

BATCH_ENGINE_URL="${LIFERAY_URL}/o/headless-batch-engine/v1.0/import-task"

function main {
	_acquire_oauth_token

	_wait_for_object_definitions

	local file

	for file in data/*.batch-engine-data.json
	do
		_import "${file}"
	done
}

function _import {
	local file="${1}"

	local class_name
	class_name=$(_read_config "${file}" "className")

	local create_strategy
	create_strategy=$(_read_config "${file}" "createStrategy")

	local import_strategy="ON_ERROR_CONTINUE"

	if [[ $(_read_config "${file}" "onErrorFail") == "true" ]]
	then
		import_strategy="ON_ERROR_FAIL"
	fi

	local url

	if [[ ${class_name} == "com.liferay.object.model.ObjectDefinition#"* ]]
	then

		# The generic batch engine import task does not recognize dynamic custom
		# object classes, so custom object entries are imported through the
		# object's own scoped batch endpoint, resolved from the object
		# definition's REST context path.

		local object_external_reference_code="${class_name#*#}"

		local rest_context_path

		rest_context_path=$(_curl \
			"${LIFERAY_URL}/o/object-admin/v1.0/object-definitions/by-external-reference-code/${object_external_reference_code}" \
			| _read_field "restContextPath")

		if [[ -z ${rest_context_path} ]]
		then
			echo "Unable to resolve REST context path for ${object_external_reference_code}." >&2

			return 1
		fi

		url="${LIFERAY_URL}${rest_context_path}/batch?createStrategy=${create_strategy}&importStrategy=${import_strategy}"
	else
		local task_item_delegate_name
		task_item_delegate_name=$(_read_config "${file}" "taskItemDelegateName")

		local encoded_class_name
		encoded_class_name=$(python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=''))" "${class_name}")

		url="${BATCH_ENGINE_URL}/${encoded_class_name}?createStrategy=${create_strategy}&importStrategy=${import_strategy}&taskItemDelegateName=${task_item_delegate_name}"
	fi

	local import_task_id

	import_task_id=$(_read_items "${file}" | _curl \
		--data @- \
		--header "Content-Type: application/json" \
		"${url}" \
		| _read_field "id")

	if [[ -z ${import_task_id} ]]
	then
		echo "Unable to start import for ${file}." >&2

		return 1
	fi

	_wait_for_import "${import_task_id}" "${file}"
}

function _wait_for_import {
	local import_task_id="${1}"
	local file="${2}"

	local attempt

	# A remote environment's batch engine can take several minutes to drain a
	# queued import, far longer than a fresh local bundle, so wait generously
	# (300 attempts x 2s = 10 minutes). A completed or failed task breaks early,
	# so this never slows down the common, fast case.

	for ((attempt = 1; attempt <= 300; attempt++))
	do
		local execute_status

		execute_status=$(_curl \
			"${BATCH_ENGINE_URL}/${import_task_id}" \
			| _read_field "executeStatus")

		case "${execute_status}" in
			COMPLETED)
				echo "Imported ${file}."

				return 0
				;;
			FAILED)
				echo "Unable to import ${file}." >&2

				_curl \
					"${BATCH_ENGINE_URL}/${import_task_id}/failed-items/report" >&2

				return 1
				;;
		esac

		sleep 2
	done

	echo "Timed out waiting for import of ${file}." >&2

	return 1
}

function _wait_for_object_definitions {
	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		local status

		status=$(_curl \
			--output /dev/null \
			--write-out "%{http_code}" \
			"${LIFERAY_URL}/o/c/projects")

		if [[ ${status} == "200" ]]
		then
			return 0
		fi

		sleep 5
	done

	echo "Object definitions were not available." >&2

	return 1
}

function _read_config {
	local file="${1}"
	local key="${2}"

	python3 -c "
import json

with open('${file}') as file:
	configuration = json.load(file)['configuration']

print(configuration.get('${key}') or configuration.get('parameters', {}).get('${key}', ''))
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

function _read_items {
	local file="${1}"

	python3 -c "
import json
import sys

with open('${file}') as file:
	json.dump(json.load(file)['items'], sys.stdout)
"
}

main "${@}"