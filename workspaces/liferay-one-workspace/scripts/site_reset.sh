#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source _common.sh

# Resets the "One" site in place, without touching Docker, the database, or the
# other client extensions. It tears down the seeded records, deletes the site
# group, redeploys just the site initializer client extension (which recreates
# and re-initializes the site), then reseeds the data and rebinds the virtual
# host. This is the lightest of the three resets: because it leaves the database
# and every other client extension alone, it is far faster than an instance or
# environment reset and keeps the local-dev OAuth2 application intact. Use it
# when only the site content or the seeded records are stale.
#
# Like bootstrap.sh, it pins the seed and teardown scripts to admin basic auth
# against localhost, so it never depends on the local-dev OAuth2 application and
# is never redirected at the remote environment the workspace .env may describe.

CONTAINER_NAME="liferay"

SITE_FRIENDLY_URL_PATH="one"

function main {
	export LIFERAY_ADMIN_EMAIL=test@liferay.com
	export LIFERAY_ADMIN_PASSWORD=test
	export LIFERAY_AUTH_MODE=basic
	export LIFERAY_ENV_FILE=/dev/null
	export LIFERAY_URL=http://localhost:8080

	local container_id

	container_id="$(docker ps --quiet --filter "name=^${CONTAINER_NAME}$")"

	if [[ -z ${container_id} ]]
	then
		echo "Unable to find a running \"${CONTAINER_NAME}\" container." >&2

		return 1
	fi

	cd ..

	echo "Tearing down seeded records."
	bash scripts/seed/teardown_records.sh

	echo "Deleting the One site."
	_delete_site

	echo "Redeploying the site initializer."
	SINCE="$(date +%s)"

	./gradlew ":client-extensions:liferay-one-site-initializer:deploy" \
		-Ddeploy.docker.container.id="${container_id}"

	_wait_for_site_initializer

	echo "Setting virtual hosts."
	bash scripts/bootstrap/set_virtual_hosts.sh

	echo "Seeding data."
	bash scripts/seed.sh

	echo "Done. The One site has been reset."
}

# Deletes the One site group through the JSONWS bridge, resolving the group ID
# from its friendly URL path first. JSONWS is used, rather than the headless
# site API, because deleting a group is an admin operation that the seed's
# scoped token does not cover; basic auth is already pinned above. A missing
# site is fine -- the redeploy below recreates it either way.

function _delete_site {
	local group_id

	group_id=$(_curl "${LIFERAY_URL}/o/headless-admin-user/v1.0/sites/by-friendly-url-path/${SITE_FRIENDLY_URL_PATH}" | _json_field "id")

	if [[ -z ${group_id} ]] || [[ ${group_id} == "0" ]]
	then
		echo "  No existing One site found; skipping the delete."

		return 0
	fi

	local status

	status=$(_curl \
		--data-urlencode "groupId=${group_id}" \
		--output /dev/null \
		--request POST \
		--write-out "%{http_code}" \
		"${LIFERAY_URL}/api/jsonws/group/delete-group" || true)

	if [[ ${status} == 2* ]]
	then
		echo "  Deleted the One site (${group_id})."

		return 0
	fi

	echo "  Unable to delete the One site (${group_id}): HTTP ${status}." >&2

	return 1
}

# The site initializer logs a clear completion marker once it finishes seeding
# the site named "One". Watch the container log from the moment the redeploy
# started (SINCE) until the marker appears.

function _wait_for_site_initializer {
	local timeout=600

	local elapsed=0

	echo "Waiting for the site initializer to finish."

	until docker logs --since "${SINCE}" "${CONTAINER_NAME}" 2>&1 |
		grep --quiet --extended-regexp "BundleSiteInitializer.*Initialized One for group"
	do
		if [ "${elapsed}" -ge "${timeout}" ]
		then
			echo "Timed out after ${timeout}s waiting for the site initializer." >&2

			return 1
		fi

		printf '.'

		sleep 5

		elapsed=$((elapsed + 5))
	done

	echo " Done waiting for the site initializer."
}

main "${@}"