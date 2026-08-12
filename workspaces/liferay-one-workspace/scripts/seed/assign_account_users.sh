#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../_common.sh

# Attaches the seeded persona users to their accounts and grants the account
# roles a few of them need. The memberships come from the accountBriefs already
# declared in 02-user-account.batch-engine-data.json, which the headless
# UserAccount import ignores in the same way it ignores userGroupBriefs and
# roleBriefs -- the users are created, but none of the declared accounts are
# attached -- so that document stays the single source of truth for who belongs
# where and this step applies it.
#
# Account roles cannot be expressed in that document at all, so the handful that
# personas need are listed here. test@liferay.com is the default portal
# administrator rather than a seeded persona, so its memberships are listed here
# too. Membership is granted before the role because the account-role endpoint
# does not enrol a user that is not already a member. Every endpoint used is
# idempotent, so re-runs are safe. Account role IDs are generated per
# environment, so they are resolved by name at runtime.

USER_ACCOUNT_DATA="data/02-user-account.batch-engine-data.json"

# Each entry is "<account external reference code>=<email address>=<account role
# name>". An empty role name grants account membership with no account role.
# Sorted by account external reference code, then email address.

ROLE_ASSIGNMENTS=(
	"ACCNT-001=admin@customer.com=Account Administrator"
	"ACCNT-001=admin@partner.com=Partner Manager"
	"ACCNT-001=member@customer.com="
	"ACCNT-001=member@partner.com=Partner Member"
	"ACCNT-001=test@liferay.com=Account Administrator"
	"ACCNT-021=admin@publisher.com=Account Administrator"
	"ACCNT-021=test@liferay.com=Account Administrator"
)

declare -A ACCOUNT_ROLE_IDS
declare -A MEMBERSHIPS_GRANTED

function main {
	_acquire_oauth_token

	local assignment
	local membership

	while IFS=$'\t' read -r account_external_reference_code email_address
	do
		_assign_membership "${account_external_reference_code}" "${email_address}"
	done < <(_read_declared_memberships)

	for assignment in "${ROLE_ASSIGNMENTS[@]}"
	do
		membership="${assignment%=*}"

		_assign_membership "${membership%%=*}" "${membership#*=}"

		_assign_account_role \
			"${membership%%=*}" "${membership#*=}" "${assignment##*=}"
	done
}

function _account_url {
	echo "${LIFERAY_URL}/o/headless-admin-user/v1.0/accounts/by-external-reference-code/${1}"
}

function _assign_account_role {
	local account_external_reference_code="${1}"
	local email_address="${2}"
	local role_name="${3}"

	[[ -z ${role_name} ]] && return 0

	_load_account_role_ids "${account_external_reference_code}"

	local account_role_id="${ACCOUNT_ROLE_IDS[${account_external_reference_code}|${role_name}]:-}"

	if [[ -z ${account_role_id} ]]
	then
		_warn "Unable to resolve account role \"${role_name}\" for ${account_external_reference_code}."

		return 1
	fi

	local status

	status=$(_curl \
		--output /dev/null \
		--request POST \
		--write-out "%{http_code}" \
		"$(_account_url "${account_external_reference_code}")/account-roles/${account_role_id}/user-accounts/by-email-address/${email_address}")

	if [[ ${status} == 2* ]]
	then
		echo "Assigned ${email_address} to ${account_external_reference_code} as ${role_name}."

		return 0
	fi

	_warn "Unable to assign ${email_address} to ${account_external_reference_code} as ${role_name} (HTTP ${status})."

	return 1
}

function _assign_membership {
	local account_external_reference_code="${1}"
	local email_address="${2}"

	local key="${account_external_reference_code}|${email_address}"

	[[ -n ${MEMBERSHIPS_GRANTED[${key}]:-} ]] && return 0

	MEMBERSHIPS_GRANTED["${key}"]=1

	local status

	status=$(_curl \
		--output /dev/null \
		--request POST \
		--write-out "%{http_code}" \
		"$(_account_url "${account_external_reference_code}")/user-accounts/by-email-address/${email_address}")

	if [[ ${status} == 2* ]]
	then
		echo "Assigned ${email_address} to ${account_external_reference_code}."

		return 0
	fi

	_warn "Unable to assign ${email_address} to ${account_external_reference_code} (HTTP ${status})."

	return 1
}

function _load_account_role_ids {
	local account_external_reference_code="${1}"

	[[ -n ${ACCOUNT_ROLE_IDS[${account_external_reference_code}|loaded]:-} ]] && return 0

	ACCOUNT_ROLE_IDS["${account_external_reference_code}|loaded"]=1

	local id
	local name

	while IFS=$'\t' read -r id name
	do
		ACCOUNT_ROLE_IDS["${account_external_reference_code}|${name}"]="${id}"
	done < <(_curl "$(_account_url "${account_external_reference_code}")/account-roles?pageSize=200" | python3 -c "
import json
import sys

for account_role in json.load(sys.stdin).get('items', []):
	print(str(account_role.get('id')) + '\t' + account_role.get('name'))
")
}

function _read_declared_memberships {
	python3 -c "
import json

with open('${USER_ACCOUNT_DATA}') as user_account_file:
	user_accounts = json.load(user_account_file)

if isinstance(user_accounts, dict):
	user_accounts = user_accounts.get('items', [])

memberships = set()

for user_account in user_accounts:
	email_address = user_account.get('emailAddress')

	for account_brief in user_account.get('accountBriefs') or []:
		external_reference_code = account_brief.get('externalReferenceCode')

		if email_address and external_reference_code:
			memberships.add((external_reference_code, email_address))

for external_reference_code, email_address in sorted(memberships):
	print(external_reference_code + '\t' + email_address)
"
}

main "${@}"