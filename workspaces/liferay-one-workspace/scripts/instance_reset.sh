#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source _common.sh

# Resets the Liferay virtual instance's data in place, entirely through the
# Liferay APIs and client-extension redeploys. It never touches Docker, the
# database, or the server process: it tears down everything the bootstrap seeds
# and provisions (both the seeded records and the structural scaffolding),
# deletes the "One" site, then reprovisions by redeploying the two data-bearing
# client extensions (the batch import and the site initializer) and reseeding.
#
# This is the middle reset tier, between /one-site-reset (site and records only)
# and /one-env-reset (a full Docker rebuild). Use it when the batch-owned
# scaffolding that /one-site-reset leaves in place -- object definitions,
# relationships, commerce configuration, roles, taxonomies -- needs to be
# rebuilt, but the image and bundle are still good.
#
# A literal "create a new company and delete the old one" is impossible for the
# default virtual instance: Liferay defines the default as the company whose
# webId matches the company.default.web.id property, refuses to delete it
# (RequiredCompanyException), and offers no runtime way to hand the default off
# to another company. So a clean instance is achieved by resetting the default
# company's data in place instead.
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

	echo "Tearing down seeded records and structural scaffolding."
	bash scripts/seed/teardown.sh --full

	echo "Deleting the One site."
	_delete_site

	echo "Redeploying the batch import."
	SINCE="$(date +%s)"

	./gradlew ":client-extensions:liferay-one-batch:deploy" \
		-Ddeploy.docker.container.id="${container_id}"

	_wait_for_batch_imports

	echo "Redeploying the site initializer."
	./gradlew ":client-extensions:liferay-one-site-initializer:deploy" \
		-Ddeploy.docker.container.id="${container_id}"

	_wait_for_site_initializer

	echo "Setting virtual hosts."
	bash scripts/bootstrap/set_virtual_hosts.sh

	echo "Seeding data."
	bash scripts/seed.sh

	echo "Done. The Liferay instance data has been reset."
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

# The batch client extension imports its data asynchronously on the file install
# watcher thread. Wait until the batch engine import activity starts and then
# stays quiet, which means every import task has finished.

function _wait_for_batch_imports {
	local idle_seconds=20
	local timeout=600

	local elapsed=0
	local idle=0
	local last_count=0
	local started="false"

	echo "Waiting for batch engine imports to finish."

	while true
	do
		local count

		count=$(docker logs --since "${SINCE}" "${CONTAINER_NAME}" 2>&1 |
			grep --count --extended-regexp "BatchEngineImportTaskExecutorImpl" || true)

		if [ "${count}" -gt 0 ]
		then
			started="true"
		fi

		if [ "${started}" == "true" ] && [ "${count}" -eq "${last_count}" ]
		then
			idle=$((idle + 5))

			if [ "${idle}" -ge "${idle_seconds}" ]
			then
				echo " Batch engine imports finished."

				return 0
			fi
		else
			idle=0
		fi

		if [ "${elapsed}" -ge "${timeout}" ]
		then
			echo "Timed out after ${timeout}s waiting for batch engine imports." >&2

			return 1
		fi

		last_count=${count}

		printf '.'

		sleep 5

		elapsed=$((elapsed + 5))
	done
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