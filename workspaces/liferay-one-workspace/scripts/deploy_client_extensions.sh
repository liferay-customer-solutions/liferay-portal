#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source _common.sh

function main {
	cd ..

	./gradlew deploy \
		-Ddeploy.docker.container.id="$(docker ps --quiet --filter "name=^liferay$")"

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

main "${@}"