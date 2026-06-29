#!/bin/bash

# Sends a single liferay-one-batch *.batch-engine-data.json file to a Liferay
# environment through the headless batch engine import-task endpoint, then polls
# the created import task until it reaches a terminal state and reports the
# outcome (processed / failed counts and any error message).
#
# This is the manual, one-file-at-a-time equivalent of what the liferay-one-batch
# client extension does on deploy. Run it per file, in numeric order, so an
# import that depends on an earlier one (relationships after definitions,
# products after catalogs) sees its prerequisites in place.
#
# Connection and OAuth config are read from the workspace .env, exactly like the
# teardown scripts. The token is minted with the batch engine scope only.
#
# Usage: scripts/seed/send_batch.sh <path-to-batch-file>

set -o nounset
set -o pipefail

LIFERAY_ENV_FILE="${LIFERAY_ENV_FILE:-../../.env}"

if [[ -f ${LIFERAY_ENV_FILE} ]]
then
	for _env_key in \
		LIFERAY_OAUTH_CLIENT_ID \
		LIFERAY_OAUTH_CLIENT_SECRET \
		LIFERAY_OAUTH_TOKEN_URL \
		LIFERAY_URL
	do
		[[ -n ${!_env_key:-} ]] && continue

		_env_line=$(grep -E "^${_env_key}=" "${LIFERAY_ENV_FILE}" | tail -n 1)

		[[ -n ${_env_line} ]] && export "${_env_key}=${_env_line#*=}"
	done

	unset _env_key _env_line
fi

LIFERAY_URL="${LIFERAY_URL:-http://localhost:8080}"
LIFERAY_OAUTH_TOKEN_URL="${LIFERAY_OAUTH_TOKEN_URL:-${LIFERAY_URL}/o/oauth2/token}"

BATCH_FILE="${1:?Usage: send_batch.sh <path-to-batch-file>}"

if [[ ! -f ${BATCH_FILE} ]]
then
	echo "File not found: ${BATCH_FILE}" >&2

	exit 1
fi

# Mint the token once.

_TOKEN=$(curl --silent \
	--data-urlencode "client_id=${LIFERAY_OAUTH_CLIENT_ID}" \
	--data-urlencode "client_secret=${LIFERAY_OAUTH_CLIENT_SECRET}" \
	--data-urlencode "grant_type=client_credentials" \
	--data-urlencode "scope=Liferay.Headless.Batch.Engine.everything" \
	"${LIFERAY_OAUTH_TOKEN_URL}" | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")

if [[ -z ${_TOKEN} ]]
then
	echo "Unable to mint OAuth token from ${LIFERAY_OAUTH_TOKEN_URL}" >&2

	exit 1
fi

# Pull className, the query parameters, the item count, and the items body out
# of the batch file. taskItemDelegateName may sit inside "parameters" or beside
# it; createStrategy/updateStrategy live in "parameters"; onErrorFail maps to
# importStrategy. containsHeaders is CSV-only and ignored for JSON items.

read -r CLASS_NAME ITEM_COUNT QUERY < <(python3 -c "
import json, sys, urllib.parse

with open('${BATCH_FILE}') as f:
	data = json.load(f)

config = data.get('configuration', {})
params = dict(config.get('parameters', {}))

class_name = config.get('className', '')

delegate = config.get('taskItemDelegateName') or params.get('taskItemDelegateName') or 'DEFAULT'

query = {'taskItemDelegateName': delegate}

if params.get('createStrategy'):
	query['createStrategy'] = params['createStrategy']
if params.get('updateStrategy'):
	query['updateStrategy'] = params['updateStrategy']

on_error_fail = str(params.get('onErrorFail', 'false')).lower()
query['importStrategy'] = 'ON_ERROR_FAIL' if on_error_fail == 'true' else 'ON_ERROR_CONTINUE'

items = data.get('items', [])

print(class_name, len(items), urllib.parse.urlencode(query))
")

if [[ -z ${CLASS_NAME} ]]
then
	echo "No className in ${BATCH_FILE}" >&2

	exit 1
fi

# Body is just the items array.

ITEMS_BODY=$(python3 -c "import json; print(json.dumps(json.load(open('${BATCH_FILE}')).get('items', [])))")

echo "── $(basename "${BATCH_FILE}")"
echo "   className: ${CLASS_NAME}"
echo "   items:     ${ITEM_COUNT}"
echo "   params:    ${QUERY}"

IMPORT_URL="${LIFERAY_URL}/o/headless-batch-engine/v1.0/import-task/${CLASS_NAME}?${QUERY}"

RESPONSE=$(curl --silent \
	--header "Authorization: Bearer ${_TOKEN}" \
	--header "Content-Type: application/json" \
	--request POST \
	--data "${ITEMS_BODY}" \
	"${IMPORT_URL}")

TASK_ID=$(echo "${RESPONSE}" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)

if [[ -z ${TASK_ID} ]]
then
	echo "   ✗ FAILED to create import task."
	echo "   Response: ${RESPONSE}"

	exit 1
fi

echo "   task id:   ${TASK_ID}"

# Poll the import task until terminal.

STATUS_URL="${LIFERAY_URL}/o/headless-batch-engine/v1.0/import-task/${TASK_ID}"

for _attempt in $(seq 1 120)
do
	TASK=$(curl --silent --header "Authorization: Bearer ${_TOKEN}" "${STATUS_URL}")

	STATUS=$(echo "${TASK}" | python3 -c "import json,sys; print(json.load(sys.stdin).get('executeStatus',''))" 2>/dev/null)

	if [[ ${STATUS} == "COMPLETED" || ${STATUS} == "FAILED" ]]
	then
		break
	fi

	sleep 2
done

echo "${TASK}" | python3 -c "
import json, sys

t = json.load(sys.stdin)

status = t.get('executeStatus', '?')
processed = t.get('processedItemsCount', '?')
total = t.get('totalItemsCount', '?')
failed = t.get('failedItemsCount', 0)
error = t.get('errorMessage', '')

mark = '✓' if status == 'COMPLETED' and not failed else '✗'

print('   {} status:    {}  (processed {}/{}, failed {})'.format(mark, status, processed, total, failed))

if error:
	print('   error:     ' + error)
"

# Report per-item failures when any.

FAILED_COUNT=$(echo "${TASK}" | python3 -c "import json,sys; print(json.load(sys.stdin).get('failedItemsCount',0) or 0)" 2>/dev/null)

if [[ ${FAILED_COUNT} != "0" && -n ${FAILED_COUNT} ]]
then
	echo "   ── failed items ──"
	curl --silent --header "Authorization: Bearer ${_TOKEN}" \
		"${STATUS_URL}/import-task-errors?pageSize=50" | python3 -c "
import json, sys

try:
	items = json.load(sys.stdin).get('items', [])
except Exception:
	items = []

for it in items[:50]:
	print('     - item {}: {}'.format(it.get('itemIndex', '?'), it.get('message', '')))
" 2>/dev/null || true
fi

# Exit nonzero if the task did not fully succeed, so the caller can stop.

FINAL_STATUS=$(echo "${TASK}" | python3 -c "import json,sys; print(json.load(sys.stdin).get('executeStatus',''))" 2>/dev/null)

[[ ${FINAL_STATUS} == "COMPLETED" && ${FAILED_COUNT} == "0" ]]