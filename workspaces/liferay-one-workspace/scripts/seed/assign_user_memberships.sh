#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# Applies the seeded persona users' regular-role memberships. Most personas
# inherit a regular role from a user group (Customers grants Customer, Partners
# grants Partner, Employees grants Liferay Staff), and every persona joins
# Everyone; the user groups and the roles they carry ship in the
# liferay-one-batch client extension. A few roles are assigned to a user
# directly rather than through a group -- the Publisher persona and the default
# administrator carry the Marketplace Publisher role.
#
# Neither membership is expressible through the UserAccount batch document: the
# headless UserAccount import path ignores both userGroupBriefs and (for regular
# roles) roleBriefs, so both are applied here, after the users are seeded in
# 02-user-account.batch-engine-data.json. The headless user-group-users and
# role-association endpoints are idempotent, so re-runs are safe.

ROLE_URL="${LIFERAY_URL}/o/headless-admin-user/v1.0/roles"

USER_ACCOUNT_URL="${LIFERAY_URL}/o/headless-admin-user/v1.0/user-accounts"

USER_GROUP_URL="${LIFERAY_URL}/o/headless-admin-user/v1.0/user-groups"

# Each entry is "<user group external reference code>=<space-separated user
# account external reference codes>". Sorted by user group external reference
# code.

GROUP_MEMBERSHIPS=(
	"C_CUSTOMERS=USER_CUSTOMER_ADMIN USER_CUSTOMER_MEMBER"
	"C_EMPLOYEES=USER_STAFF"
	"C_EVERYONE=USER_CUSTOMER_ADMIN USER_CUSTOMER_MEMBER USER_PARTNER_ADMIN USER_PARTNER_MEMBER USER_PUBLISHER_ADMIN USER_STAFF"
	"C_PARTNERS=USER_PARTNER_ADMIN USER_PARTNER_MEMBER"
)

# Each entry is "<regular role external reference code>=<space-separated user
# identifiers>" for roles assigned directly to a user rather than through a
# group. A user identifier is a user account external reference code, or an
# email address when the user has no seeded external reference code (the default
# administrator). Sorted by role external reference code.

ROLE_MEMBERSHIPS=(
	"C_MARKETPLACE_PUBLISHER=USER_PUBLISHER_ADMIN test@liferay.com"
)

function main {
	_acquire_oauth_token

	local membership

	for membership in "${GROUP_MEMBERSHIPS[@]}"
	do
		_assign_user_group "${membership%%=*}" "${membership#*=}"
	done

	for membership in "${ROLE_MEMBERSHIPS[@]}"
	do
		_assign_role "${membership%%=*}" "${membership#*=}"
	done
}

function _assign_role {
	local role_external_reference_code="${1}"
	local user_account_external_reference_codes="${2}"

	local user_account_external_reference_code

	for user_account_external_reference_code in ${user_account_external_reference_codes}
	do
		local user_id

		user_id=$(_resolve_user_id "${user_account_external_reference_code}")

		if [[ -z ${user_id} ]]
		then
			continue
		fi

		local status

		status=$(_curl \
			--output /dev/null \
			--request POST \
			--write-out "%{http_code}" \
			"${ROLE_URL}/by-external-reference-code/${role_external_reference_code}/association/user-account/${user_id}")

		if [[ ${status} == 2* ]]
		then
			echo "Assigned ${user_account_external_reference_code} the ${role_external_reference_code} role."
		else
			_warn "Unable to assign ${role_external_reference_code} to ${user_account_external_reference_code} (HTTP ${status})."
		fi
	done
}

function _assign_user_group {
	local user_group_external_reference_code="${1}"
	local user_account_external_reference_codes="${2}"

	local user_ids=()

	local user_account_external_reference_code

	for user_account_external_reference_code in ${user_account_external_reference_codes}
	do
		local user_id

		user_id=$(_resolve_user_id "${user_account_external_reference_code}")

		if [[ -n ${user_id} ]]
		then
			user_ids+=("${user_id}")
		fi
	done

	if [[ ${#user_ids[@]} -eq 0 ]]
	then
		_warn "No users resolved for ${user_group_external_reference_code}."

		return 1
	fi

	local body

	body=$(printf '%s\n' "${user_ids[@]}" | python3 -c "import json, sys; print(json.dumps([int(line) for line in sys.stdin.read().split()]))")

	local status

	status=$(_curl \
		--data "${body}" \
		--header "Content-Type: application/json" \
		--output /dev/null \
		--request POST \
		--write-out "%{http_code}" \
		"${USER_GROUP_URL}/by-external-reference-code/${user_group_external_reference_code}/user-group-users")

	if [[ ${status} == 2* ]]
	then
		echo "Assigned ${#user_ids[@]} user(s) to ${user_group_external_reference_code}."

		return 0
	fi

	_warn "Unable to assign users to ${user_group_external_reference_code} (HTTP ${status})."

	return 1
}

function _resolve_user_id {
	local identifier="${1}"

	local user_id

	if [[ ${identifier} == *"@"* ]]
	then
		user_id=$(_curl \
			--get \
			--data-urlencode "filter=emailAddress eq '${identifier}'" \
			"${USER_ACCOUNT_URL}" \
			| python3 -c "
import json
import sys

try:
	items = json.load(sys.stdin).get('items', [])

	print(items[0]['id'] if items else '')
except Exception:
	print('')
")
	else
		user_id=$(_curl \
			"${USER_ACCOUNT_URL}/by-external-reference-code/${identifier}" \
			| _json_field "id")
	fi

	if [[ -z ${user_id} ]]
	then
		_warn "Unable to resolve user \"${identifier}\"."
	fi

	echo "${user_id}"
}

main "${@}"