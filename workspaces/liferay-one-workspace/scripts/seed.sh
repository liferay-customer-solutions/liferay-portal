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

	echo "Seeding test data."
	./seed/seed_test_data.sh

	echo "Ensuring product SKUs."
	./seed/ensure_product_skus.sh

	echo "Seeding journal articles."
	./seed/seed_journal_articles.sh

	echo "Activating seeded user accounts."
	./seed/activate_user_accounts.sh

	echo "Assigning user group and role memberships."
	./seed/assign_user_memberships.sh

	echo "Assigning users to their accounts."
	./seed/assign_account_users.sh

	echo "Linking supplier accounts to commerce catalogs."
	./seed/link_commerce_catalogs.sh

	echo "Creating publisher details."
	./seed/create_publisher_details.sh

	echo "Populating orders, order items, and entitlements."
	./seed/populate_orders.sh

	echo "Populating overage usage reports and orders."
	./seed/populate_overages.sh
}

main "${@}"