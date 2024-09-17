#!/bin/bash

function check_health {
	docker inspect --format='{{.State.Health.Status}}' "${1}" | grep -q "healthy"

	if [[ $? -eq 0 ]]; then
		echo "Container '${1}' is healthy."

		return 0
	fi

	echo "Container '${1}' is not healthy."

	sleep 10

	check_health "${1}"
}

function check_logs {
	docker logs -f "${container}" | grep -q "${1}"

	if [[ $? -eq 0 ]]; then
		echo "Text '${1}' found in logs."

		return 0
	fi

	echo "Text '${1}' not found in logs."

	sleep 10

	check_logs "${1}"
}

function deploy {
	./gradlew :client-extensions:${1}:clean :client-extensions:${1}:deploy "-Ddeploy.docker.container.id=${container}"
}

function main {
	local container="liferay-customer-workspace-liferay"

	mkdir -p ../liferay/patching

	curl https://releases-cdn.liferay.com/tools/patching-tool/patching-tool-4.0.3.zip > ../liferay/patching/patching-tool-4.0.3.zip
	curl https://releases-cdn.liferay.com/dxp/hotfix/2024.q2.7/liferay-dxp-2024.q2.7-hotfix-4.zip > ../liferay/patching/liferay-dxp-2024.q2.7-hotfix-4.zip

	docker compose up -d

	check_health ${container}

	pushd .. > /dev/null

	deploy "liferay-customer-etc-cron"
	deploy "liferay-customer-etc-spring-boot"
	deploy "liferay-customer-global-css"

	check_logs "STARTED liferaycustomerglobalcss_7.4.13"

	deploy "liferay-customer-custom-element"

	check_logs "STARTED liferaycustomercustomelement_7.4.13"

	deploy "liferay-customer-site-initializer-code"

	check_logs "STARTED liferaycustomersiteinitializercode_7.4.13"

	deploy "liferay-partner-site-initializer-quickstart"

	popd > /dev/null

	docker compose up
}

main "${@}"