#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

# Orchestrates the teardown of the data created by the bootstrap: the batch
# client extension, the site initializer, and the post-deploy scripts. It is
# the inverse of bootstrap.sh's seeding steps.
#
# By default it runs the records-only teardown (teardown_records.sh): the
# seeded object entries and demo content, leaving the structural scaffolding in
# place so the data can be re-seeded without an environment rebuild.
#
# With --full it additionally runs the structural teardown
# (teardown_structure.sh): object relationships, object definitions, commerce
# configuration, roles, taxonomies, and list type definitions. Records are torn
# down first so that, by the time the object definitions are deleted, the
# "prevent" relationships no longer hold any entries.
#
# Requests authenticate with an OAuth2 client-credentials bearer token, minted
# by default from the "local-dev" application created by the one-oauth-app
# skill. Connection settings are read from the environment:
# LIFERAY_URL (default http://localhost:8080)
# LIFERAY_OAUTH_CLIENT_ID (default local-dev)
# LIFERAY_OAUTH_CLIENT_SECRET (default local-dev-secret)
# SITE_FRIENDLY_URL_PATH (default one)
#
# To fall back to admin basic auth, set LIFERAY_AUTH_MODE=basic (then
# LIFERAY_ADMIN_EMAIL / LIFERAY_ADMIN_PASSWORD apply, defaulting to
# test@liferay.com / test). See _teardown_common.sh for the full list of
# OAuth overrides.
#
# Usage:
# teardown.sh Delete seeded records and demo content.
# teardown.sh --full Also delete the structural scaffolding.

function main {
	local full="false"

	for arg in "${@}"
	do
		case "${arg}" in
			--full)
				full="true"
				;;
			-h | --help)
				_usage

				return 0
				;;
			*)
				echo "Unknown argument: ${arg}" >&2

				_usage >&2

				return 1
				;;
		esac
	done

	bash teardown_records.sh

	if [ "${full}" == "true" ]
	then
		bash teardown_structure.sh
	fi

	echo "Teardown complete."
}

function _usage {
	echo "Usage: teardown.sh [--full]"
	echo
	echo "  (no args)  Delete the seeded records and demo content."
	echo "  --full     Also delete the structural scaffolding (object"
	echo "             definitions, relationships, commerce config, roles,"
	echo "             taxonomies, list type definitions)."
}

main "${@}"