#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source _common.sh

function main {
	local reset="false"

	for arg in "${@}"
	do
		if [ "${arg}" == "--reset" ]
		then
			reset="true"
		fi
	done

	local product

	product="$(get_gradle_property liferay.workspace.product)"

	local version_tag="${product#dxp-}"

	cd ..

	# Bootstrap is the local orchestrator: it always seeds the localhost bundle
	# with admin credentials. The seed scripts now default to OAuth and read the
	# workspace .env (which may target a remote environment), so pin them to
	# basic auth against localhost and bypass the .env so a local bootstrap is
	# never redirected at a remote URL.

	export LIFERAY_ADMIN_EMAIL=test@liferay.com
	export LIFERAY_ADMIN_PASSWORD=test
	export LIFERAY_AUTH_MODE=basic
	export LIFERAY_ENV_FILE=/dev/null
	export LIFERAY_URL=http://localhost:8080

	if [ "${reset}" == "true" ]
	then
		echo "Tearing down containers and volumes."
		docker compose --file docker-compose.yaml down --volumes
	fi

	./gradlew clean

	bash scripts/bootstrap/extract_hotfix.sh
	bash scripts/bootstrap/extract_license.sh

	echo "Building Docker image."
	./gradlew buildDockerImage

	local workspace_name

	workspace_name="$(basename "$(pwd)")"

	echo "Tagging ${workspace_name}-liferay:${version_tag} as liferay:local."
	docker tag "${workspace_name}-liferay:${version_tag}" "liferay:local"

	echo "Starting containers."
	docker compose --file docker-compose.yaml up --detach

	echo "Waiting for Liferay to be healthy."
	until curl --fail --max-time 5 --output /dev/null --silent "http://localhost:8080/c/portal/status"
	do
		printf '.'
		sleep 10
	done

	echo "Deploying artifacts to Liferay container."
	bash scripts/bootstrap/deploy_client_extensions.sh

	echo "Seeding data."
	bash scripts/seed.sh

	echo "Done. Liferay is running at http://localhost."
}

main "${@}"