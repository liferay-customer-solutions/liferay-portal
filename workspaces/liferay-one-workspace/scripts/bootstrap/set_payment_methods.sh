#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"

# Force admin basic auth before sourcing the common helpers. Activating a
# payment method is a JSONWS admin operation: JSONWS does not accept the seed's
# scoped OAuth token, and the seed scope list does not include the commerce
# channel API used to resolve the channel.

export LIFERAY_AUTH_MODE=basic

source ../_common.sh

# Activate the commerce payment methods the marketplace checkout offers. A
# freshly provisioned channel has no CommercePaymentMethodGroupRel rows at all
# -- the payment engines are registered as OSGi components, but the per channel
# row that marks one usable is only created when someone activates that method
# under Commerce, Channels, Payment Methods. Until then every checkout that
# sets a payment method fails: the delivery cart PATCH carrying "paymentMethod"
# cannot resolve the engine and answers 404, which surfaces in the purchase
# flow as "An unexpected error occurred" on Purchase App.
#
# There is no client extension type, headless endpoint, or batch engine
# delegate for CommercePaymentMethodGroupRel, so the activation is applied here
# as a bootstrap step through the JSONWS bridge, the same way the virtual host
# binding is. Adding a row is not idempotent on its own -- a second add would
# create a duplicate -- so each engine is skipped when the channel already
# carries a row for it, which makes the script safe to re-run on every
# bootstrap and env reset.
#
# Each entry below is "channelExternalReferenceCode paymentIntegrationKey name".
# The keys are the ones the custom element sends from its Payment Method step:
# money-order backs "Pay with Bank Transfer" and paypal-integration backs "Pay
# with Card". Keep sorted.

PAYMENT_METHODS=(
	"LIFERAY_ONE_CHANNEL money-order Money Order"
	"LIFERAY_ONE_CHANNEL paypal-integration PayPal"
)

function main {
	_acquire_oauth_token

	local payment_method

	for payment_method in "${PAYMENT_METHODS[@]}"
	do
		_set_payment_method ${payment_method}
	done
}

function _read_json_value {
	local key="${1}"

	python3 -c "
import json
import sys

try:
	print(json.load(sys.stdin).get('${key}', ''))
except Exception:
	print('')
"
}

function _read_payment_integration_keys {
	python3 -c "
import json
import sys

try:
	items = json.load(sys.stdin)
except Exception:
	items = []

if not isinstance(items, list):
	items = []

for item in items:
	print(item.get('paymentIntegrationKey', ''))
"
}

# The channel's payment methods hang off the channel's own group, not the group
# of the site the channel is attached to, and neither the headless channel DTO
# nor the JSONWS channel model exposes it -- the column does not exist on
# CommerceChannel, the group is found by class name and class PK. What is
# reachable is the group whose group key is the channel ID, so the channel's
# site group supplies the company ID and that key resolves the rest.

function _resolve_channel_group_id {
	local commerce_channel_id="${1}"
	local site_group_id="${2}"

	local company_id

	company_id=$(_curl \
		--get \
		--data-urlencode "groupId=${site_group_id}" \
		"${LIFERAY_URL}/api/jsonws/group/get-group" |
		_read_json_value "companyId" || true)

	if [[ -z ${company_id} ]]
	then
		return 0
	fi

	_curl \
		--get \
		--data-urlencode "companyId=${company_id}" \
		--data-urlencode "groupKey=${commerce_channel_id}" \
		"${LIFERAY_URL}/api/jsonws/group/get-group" |
		_read_json_value "groupId" || true
}

function _set_payment_method {
	local channel_external_reference_code="${1}"
	local payment_integration_key="${2}"

	shift 2

	local name="${*}"

	local channel_url="${LIFERAY_URL}/o/headless-commerce-admin-channel/v1.0/channels/by-externalReferenceCode/${channel_external_reference_code}"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		# Wait for the site initializer to create the channel.

		local channel

		channel=$(_curl "${channel_url}" || true)

		local commerce_channel_id
		local site_group_id

		commerce_channel_id=$(_read_json_value "id" <<< "${channel}")
		site_group_id=$(_read_json_value "siteGroupId" <<< "${channel}")

		local group_id

		if [[ -n ${commerce_channel_id} ]] && [[ -n ${site_group_id} ]]
		then
			group_id=$(_resolve_channel_group_id "${commerce_channel_id}" "${site_group_id}")
		fi

		if [[ -z ${group_id} ]] || [[ ${group_id} == "0" ]]
		then
			sleep 5

			continue
		fi

		local payment_integration_keys

		payment_integration_keys=$(_curl \
			--get \
			--data-urlencode "groupId=${group_id}" \
			"${LIFERAY_URL}/api/jsonws/commerce.commercepaymentmethodgrouprel/get-commerce-payment-method-group-rels" |
			_read_payment_integration_keys || true)

		if grep --line-regexp --quiet "${payment_integration_key}" <<< "${payment_integration_keys}"
		then
			echo "Payment method ${payment_integration_key} is already activated on channel ${channel_external_reference_code}."

			return 0
		fi

		# CommercePaymentMethodGroupRelService.addCommercePaymentMethodGroupRel
		# takes the channel's group ID, localized name and description maps, an
		# active flag, an icon file, the payment integration key, a priority,
		# and engine type settings. The icon is nulled through the JSONWS "-"
		# prefix because there is no file to upload.

		local status

		status=$(_curl \
			--data-urlencode "active=true" \
			--data-urlencode "descriptionMap={\"en_US\":\"\"}" \
			--data-urlencode "groupId=${group_id}" \
			--data-urlencode "-imageFile=" \
			--data-urlencode "nameMap={\"en_US\":\"${name}\"}" \
			--data-urlencode "paymentIntegrationKey=${payment_integration_key}" \
			--data-urlencode "priority=0" \
			--data-urlencode "typeSettings=" \
			--output /dev/null \
			--request POST \
			--write-out "%{http_code}" \
			"${LIFERAY_URL}/api/jsonws/commerce.commercepaymentmethodgrouprel/add-commerce-payment-method-group-rel" || true)

		if [[ ${status} == 2* ]]
		then
			echo "Activated payment method ${payment_integration_key} on channel ${channel_external_reference_code}."

			return 0
		fi

		sleep 3
	done

	echo "Unable to activate payment method ${payment_integration_key} on channel ${channel_external_reference_code}." >&2

	return 1
}

main "${@}"