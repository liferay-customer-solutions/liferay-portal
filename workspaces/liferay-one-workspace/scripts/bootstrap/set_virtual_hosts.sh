#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

# Force admin basic auth before sourcing the common helpers. Binding a virtual
# host is a JSONWS admin operation: JSONWS does not accept the seed's scoped
# OAuth token, and the seed scope list does not include the site API used to
# resolve the group ID, so the OAuth path would fail on both calls.

export LIFERAY_AUTH_MODE=basic

source ../_common.sh

# Bind the local development virtual host to the One site's public layout set so
# the site is served at its own domain root (http://one.localhost) instead of
# only at the /web/one friendly URL path. Serving the site at a domain root is
# what makes site-level redirects behave as they do in a real deployment, which
# localhost cannot exercise. The site initializer format has no virtual host
# field, so the binding is applied here as a bootstrap step that runs after the
# site initializer has created the site. The call is idempotent -- re-running it
# just re-asserts the same hostname -- so it is safe on every bootstrap and env
# reset.
#
# Each entry below is "siteExternalReferenceCode virtualHostname". Keep sorted.

VIRTUAL_HOSTS=(
	"LIFERAY_ONE one.localhost"
)

function main {
	_acquire_oauth_token

	local virtual_host

	for virtual_host in "${VIRTUAL_HOSTS[@]}"
	do
		_set_virtual_host ${virtual_host}
	done
}

function _set_virtual_host {
	local site_external_reference_code="${1}"
	local virtual_hostname="${2}"

	# The sites list is filtered by external reference code rather than fetched
	# through the by-external-reference-code path because that path (and the
	# by-id path) return 404 for site groups on this release; the list endpoint
	# resolves them reliably.

	local sites_url="${LIFERAY_URL}/o/headless-admin-site/v1.0/sites?pageSize=200"

	local attempt

	for ((attempt = 1; attempt <= 60; attempt++))
	do
		# Wait for the site initializer to create the site.

		local group_id

		group_id=$(_curl "${sites_url}" | _read_group_id "${site_external_reference_code}" || true)

		if [[ -z ${group_id} ]] || [[ ${group_id} == "0" ]]
		then
			sleep 5

			continue
		fi

		# Bind the virtual host to the public layout set through the JSONWS
		# bridge. LayoutSetService.updateVirtualHosts takes the group ID, the
		# private-layout flag, and a hostname -> language ID map; an empty
		# language ID means the site default.

		local status

		status=$(_curl \
			--data-urlencode "groupId=${group_id}" \
			--data-urlencode "privateLayout=false" \
			--data-urlencode "virtualHostnames={\"${virtual_hostname}\":\"\"}" \
			--output /dev/null \
			--request POST \
			--write-out "%{http_code}" \
			"${LIFERAY_URL}/api/jsonws/layoutset/update-virtual-hosts" || true)

		if [[ ${status} == 2* ]]
		then
			echo "Bound virtual host ${virtual_hostname} to site ${site_external_reference_code}."

			return 0
		fi

		sleep 3
	done

	echo "Unable to bind virtual host ${virtual_hostname} to site ${site_external_reference_code}." >&2

	return 1
}

function _read_group_id {
	local external_reference_code="${1}"

	python3 -c "
import json
import sys

try:
	items = json.load(sys.stdin).get('items', [])
except Exception:
	items = []

for item in items:
	if item.get('externalReferenceCode') == '${external_reference_code}':
		print(item.get('id', ''))

		break
else:
	print('')
"
}

main "${@}"