#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source _common.sh

CONTAINER_NAME="liferay"

function main {
	cd ..

	CONTAINER_ID="$(docker ps --quiet --filter "name=^${CONTAINER_NAME}$")"

	if [[ -z ${CONTAINER_ID} ]]
	then
		echo "Unable to find a running \"${CONTAINER_NAME}\" container." >&2

		return 1
	fi

	# Deploy each client extension in dependency order, waiting for each to
	# finish before deploying the next. The site initializer must run last
	# because it references the objects, custom element, and styling that the
	# preceding client extensions register.

	deploy_client_extension "liferay-one-global-css"

	settle "liferay-one-global-css"

	deploy_client_extension "liferay-one-instance-settings"

	settle "liferay-one-instance-settings"

	deploy_client_extension "liferay-one-custom-element"

	settle "liferay-one-custom-element"

	deploy_client_extension "liferay-one-batch"

	wait_for_batch_imports

	deploy_client_extension "liferay-one-etc-spring-boot"

	settle "liferay-one-etc-spring-boot"

	deploy_client_extension "liferay-one-site-initializer"

	wait_for_site_initializer

	echo "Rebuilding Spring Boot client extension image."
	./gradlew :client-extensions:liferay-one-etc-spring-boot:buildDockerImage

	if [ ! -f client-extensions/liferay-one-etc-spring-boot/build/local.env ]
	then
		echo "Regenerating the missing Compose environment file."
		./gradlew :client-extensions:liferay-one-etc-spring-boot:buildDockerImage --rerun-tasks
	fi

	echo "Recreating Spring Boot client extension container."
	docker compose up --detach liferay-one-etc-spring-boot
}

function deploy_client_extension {
	local name=${1}

	echo "Deploying ${name}."

	./gradlew ":client-extensions:${name}:deploy" \
		-Ddeploy.docker.container.id="${CONTAINER_ID}"
}

# Lightweight client extensions (globalCSS, instanceSettings, customElement) do
# not emit an install marker when the file install watcher picks them up, so
# fall back to a short settle for the watcher to process the dropped artifact.

function settle {
	local name=${1}

	echo "Waiting for ${name} to settle."

	sleep 10
}

# The batch client extension imports its data asynchronously on the file
# install watcher thread. Wait until the batch engine import activity starts
# and then stays quiet, which means every import task has finished.

function wait_for_batch_imports {
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
# the site named "One".

function wait_for_site_initializer {
	wait_for_log_marker \
		"BundleSiteInitializer.*Initialized One for group" \
		"the site initializer to finish" \
		600
}

function wait_for_log_marker {
	local pattern=${1}
	local description=${2}
	local timeout=${3:-300}

	local elapsed=0

	echo "Waiting for ${description}."

	until docker logs --since "${SINCE}" "${CONTAINER_NAME}" 2>&1 |
		grep --quiet --extended-regexp "${pattern}"
	do
		if [ "${elapsed}" -ge "${timeout}" ]
		then
			echo "Timed out after ${timeout}s waiting for ${description}." >&2

			return 1
		fi

		printf '.'

		sleep 5

		elapsed=$((elapsed + 5))
	done

	echo " Done waiting for ${description}."
}

SINCE="$(date +%s)"

main "${@}"