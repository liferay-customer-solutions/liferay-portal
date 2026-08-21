#!/bin/sh

# The Mailpit web UI and REST API expose every message the environment has
# sent, and the load balancer publishes them on a public host name. Refuse to
# start unless basic authentication is configured so an unset secret fails the
# deployment instead of silently opening the mailbox.

set -e

if [ -z "${MP_UI_AUTH}" ] && [ -z "${MP_UI_AUTH_FILE}" ]; then
	echo "Unable to start Mailpit because neither MP_UI_AUTH nor MP_UI_AUTH_FILE is set"

	exit 1
fi

exec /mailpit "$@"