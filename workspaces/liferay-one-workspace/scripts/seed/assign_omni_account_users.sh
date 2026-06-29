#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# Attaches a fixed set of users to the Omni Test Account (ACCNT-001) and grants
# each the requested account role. These users already exist -- the persona
# users are seeded in 02-user-account.batch-engine-data.json and
# test@liferay.com is the default portal administrator -- so this step only
# associates them with the account and assigns roles rather than creating
# anything. The headless association endpoints are idempotent (they also add the
# user to the account when missing), so re-runs are safe. Account role IDs are
# generated per environment, so they are resolved by name at runtime.

ACCOUNT_EXTERNAL_REFERENCE_CODE="ACCNT-001"

ACCOUNT_URL="${LIFERAY_URL}/o/headless-admin-user/v1.0/accounts/by-external-reference-code/${ACCOUNT_EXTERNAL_REFERENCE_CODE}"

# Each entry is "<email address>=<account role name>". An empty role name grants
# account membership with no account role. Sorted by email address.

ASSIGNMENTS=(
	"admin@customer.com=Account Administrator"
	"admin@partner.com=Partner Manager"
	"member@customer.com="
	"member@partner.com=Partner Member"
	"test@liferay.com=Account Administrator"
)

declare -A ACCOUNT_ROLE_IDS

function main {
	_acquire_oauth_token

	_load_account_role_ids

	local assignment

	for assignment in "${ASSIGNMENTS[@]}"
	do
		_assign_user "${assignment%%=*}" "${assignment#*=}"
	done
}

function _assign_user {
	local email_address="${1}"
	local role_name="${2}"

	local url

	if [[ -z ${role_name} ]]
	then
		url="${ACCOUNT_URL}/user-accounts/by-email-address/${email_address}"
	else
		local account_role_id="${ACCOUNT_ROLE_IDS[${role_name}]:-}"

		if [[ -z ${account_role_id} ]]
		then
			_warn "Unable to resolve account role \"${role_name}\" for ${ACCOUNT_EXTERNAL_REFERENCE_CODE}."

			return 1
		fi

		url="${ACCOUNT_URL}/account-roles/${account_role_id}/user-accounts/by-email-address/${email_address}"
	fi

	local status

	status=$(_curl \
		--output /dev/null \
		--request POST \
		--write-out "%{http_code}" \
		"${url}")

	if [[ ${status} == 2* ]]
	then
		if [[ -z ${role_name} ]]
		then
			echo "Assigned ${email_address} to ${ACCOUNT_EXTERNAL_REFERENCE_CODE}."
		else
			echo "Assigned ${email_address} to ${ACCOUNT_EXTERNAL_REFERENCE_CODE} as ${role_name}."
		fi

		return 0
	fi

	_warn "Unable to assign ${email_address} to ${ACCOUNT_EXTERNAL_REFERENCE_CODE} (HTTP ${status})."

	return 1
}

function _load_account_role_ids {
	local id
	local name

	while IFS=$'\t' read -r id name
	do
		ACCOUNT_ROLE_IDS["${name}"]="${id}"
	done < <(_curl "${ACCOUNT_URL}/account-roles?pageSize=200" | python3 -c "
import json
import sys

for account_role in json.load(sys.stdin).get('items', []):
	print(str(account_role.get('id')) + '\t' + account_role.get('name'))
")
}

main "${@}"
