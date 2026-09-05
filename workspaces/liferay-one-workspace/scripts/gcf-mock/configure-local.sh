#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"

# Points liferay-one-etc-spring-boot at the liferay-one-gcf-mock client
# extension.
#
# The mock is a client extension, so the normal workspace flow builds and
# deploys it. Two things still have to be arranged locally.
#
# The two overrides that redirect the DataOps calls at the mock go in
# .env.local. That is the channel the one-deploy skill documents: it is read
# after build/local.env, so it wins. docker-compose.yaml lists it as a second
# env_file, and the LEC staging appends it onto the staged <name>.env, so it
# applies in both environments.
#
# A service in docker-compose.override.yaml is needed only for docker compose,
# which does not run container client extensions at all -- no kong service
# exists there either. In k3s the recipe deploys any directory carrying an
# LCP.json, so nothing extra is required.
#
# Both files are gitignored and already honored by the workspace, so nothing
# tracked is modified. Both edits are idempotent and preserve existing content.
#
# Usage: ./configure-local.sh [--remove]

K3S_CONTAINER="${LIFERAY_ONE_K3S_CONTAINER:-lec-one-test-k3s-k3s}"
SERVICE_NAME="liferay-one-gcf-mock"
WORKSPACE_DIR="${LIFERAY_ONE_WORKSPACE_DIR:-../..}"

# The two environments reach the mock under different names. Docker compose uses
# the service key from docker-compose.override.yaml, while the workspace build
# rewrites LCP.json's __PROJECT_ID__ to the client extension name with the
# dashes stripped, and the k3s recipe names the Deployment and Service after
# that id -- the same reason the other pods are liferayoneetcspringboot rather
# than liferay-one-etc-spring-boot.

K3S_HOST="${SERVICE_NAME//-/}"

function main {
	local remove="false"

	if [ "${1}" == "--remove" ]
	then
		remove="true"
	fi

	_configure_env "${remove}"

	if [ -f "${WORKSPACE_DIR}/docker-compose.yaml" ]
	then
		_configure_compose "${remove}"
	fi
}

function _configure_compose {
	local remove="${1}"

	local path="${WORKSPACE_DIR}/docker-compose.override.yaml"

	# The file can already hold a developer's own overrides, so the service is
	# merged in rather than written over. That needs the document parsed rather
	# than pattern matched, and PyYAML is the only dependency here that is not
	# in the standard library, so say so plainly instead of failing on the
	# import.

	if ! python3 -c "import yaml" > /dev/null 2>&1
	then
		echo "Declaring the mock as a compose service needs PyYAML (pip install --user PyYAML), because docker compose does not run container client extensions. Everything else is configured; add this to ${path} by hand to finish:" >&2
		echo "" >&2
		echo "services:" >&2
		echo "    ${SERVICE_NAME}:" >&2
		echo "        depends_on:" >&2
		echo "            liferay:" >&2
		echo "                condition: service_healthy" >&2
		echo "        image: ${SERVICE_NAME}:latest" >&2

		return 1
	fi

	python3 - "${path}" "${SERVICE_NAME}" "${remove}" <<'EOF'
import os
import sys

import yaml

path, service_name, remove = sys.argv[1], sys.argv[2], sys.argv[3] == "true"

document = {}

if os.path.exists(path):
	with open(path) as file:
		document = yaml.safe_load(file) or {}

services = document.setdefault("services", {})

if remove:
	if services.pop(service_name, None) is None:
		print("No %s service to remove" % service_name)

		raise SystemExit

	if not services:
		del document["services"]
else:
	services[service_name] = {
		"depends_on": {"liferay": {"condition": "service_healthy"}},
		"image": service_name + ":latest",
	}

if not document:
	os.remove(path)

	print("Removed %s" % path)

	raise SystemExit

with open(path, "w") as file:
	yaml.safe_dump(document, file, default_flow_style=False, sort_keys=True)

print("%s %s in %s" % ("Removed" if remove else "Declared", service_name, path))
EOF
}

function _configure_env {
	local remove="${1}"

	local path="${WORKSPACE_DIR}/.env.local"

	local host="${SERVICE_NAME}"

	if docker inspect "${K3S_CONTAINER}" > /dev/null 2>&1
	then
		host="${K3S_HOST}"
	fi

	local kept

	kept=$(grep --invert-match --no-messages --regexp="^GCE_METADATA_HOST=" --regexp="^LIFERAY_ONE_GCF_BASE_URL=" "${path}")

	if [ "${remove}" == "false" ]
	then
		kept=$(printf '%s\nGCE_METADATA_HOST=%s:80\nLIFERAY_ONE_GCF_BASE_URL=http://%s' "${kept}" "${host}" "${host}")
	fi

	# Drop a file that held nothing but these two overrides, rather than leaving
	# an empty one behind.

	if [ -z "$(echo "${kept}" | tr --delete '[:space:]')" ]
	then
		if [ -f "${path}" ]
		then
			rm --force "${path}"

			echo "Removed ${path}"
		fi

		return 0
	fi

	echo "${kept}" | grep --invert-match "^$" > "${path}"

	if [ "${remove}" == "true" ]
	then
		echo "Removed GCE_METADATA_HOST, LIFERAY_ONE_GCF_BASE_URL from ${path}"

		return 0
	fi

	echo "Set GCE_METADATA_HOST, LIFERAY_ONE_GCF_BASE_URL to ${host} in ${path}"
}

main "${@}"