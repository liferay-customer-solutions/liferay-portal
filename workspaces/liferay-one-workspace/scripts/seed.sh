#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"

# Orchestrates seeding of a running Liferay environment: the batch test data and
# journal articles, then the post-import fix-ups (account activation, user group
# and role memberships, omni account assignment, commerce catalog linking,
# publisher details, and orders).
# It is the data-seeding half of bootstrap.sh, runnable on its own against an
# already-provisioned environment.
#
# By default the seed scripts authenticate with OAuth against the environment
# described by the workspace .env. bootstrap.sh pins them to localhost basic auth
# by exporting the relevant LIFERAY_* variables before invoking this script.

FAILED_STEPS=()

function main {

	# Every import below runs with ON_ERROR_CONTINUE, and a scoped object batch
	# endpoint silently drops an entry whose relationship points at something
	# that does not exist, so a broken reference costs nothing at import time and
	# surfaces much later as an empty tab. Validating first turns that into an
	# immediate failure.

	echo "Validating seed data."

	if ! ./seed/validate_seed_data.py
	then
		echo "Refusing to seed invalid data." >&2

		return 1
	fi

	_run "Seeding test data." ./seed/seed_test_data.sh
	_run "Ensuring product SKUs." ./seed/ensure_product_skus.sh
	_run "Seeding journal articles." ./seed/seed_journal_articles.sh
	_run "Activating seeded user accounts." ./seed/activate_user_accounts.sh
	_run "Assigning user group and role memberships." ./seed/assign_user_memberships.sh
	_run "Assigning users to their accounts." ./seed/assign_account_users.sh
	_run "Linking supplier accounts to commerce catalogs." ./seed/link_commerce_catalogs.sh
	_run "Creating publisher details." ./seed/create_publisher_details.sh
	_run "Populating orders, order items, and entitlements." ./seed/populate_orders.sh
	_run "Populating overage usage reports and orders." ./seed/populate_overages.sh

	_report
}

function _report {
	if ((${#FAILED_STEPS[@]} == 0))
	then
		echo "Seeding finished."

		return 0
	fi

	echo "Seeding finished with ${#FAILED_STEPS[@]} failed step(s):" >&2

	local failed_step

	for failed_step in "${FAILED_STEPS[@]}"
	do
		echo "  ${failed_step}" >&2
	done

	return 1
}

# Runs one seed step and remembers whether it failed. Every step upserts and each
# one is independently useful, so a failure does not stop the run -- a step that
# needs an earlier one will fail on its own. What a failure must not do is pass
# unnoticed, which is what happened while these were called bare: the exit status
# went nowhere, the run reported success, and the missing rows were found days
# later in the UI.

function _run {
	local message="${1}"

	shift

	echo "${message}"

	if "${@}"
	then
		return 0
	fi

	FAILED_STEPS+=("${message} (${*})")
}

main "${@}"