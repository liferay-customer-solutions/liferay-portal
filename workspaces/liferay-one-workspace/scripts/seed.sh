#!/bin/bash

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
	echo "Seeding test data."
	bash seed/seed_test_data.sh

	echo "Ensuring product SKUs."
	bash seed/ensure_product_skus.sh

	echo "Seeding journal articles."
	bash seed/seed_journal_articles.sh

	echo "Activating seeded user accounts."
	bash seed/activate_user_accounts.sh

	echo "Assigning user group and role memberships."
	bash seed/assign_user_memberships.sh

	echo "Assigning users to the Omni Test Account."
	bash seed/assign_omni_account_users.sh

	echo "Linking supplier accounts to commerce catalogs."
	bash seed/link_commerce_catalogs.sh

	echo "Creating publisher details."
	bash seed/create_publisher_details.sh

	echo "Populating orders, order items, and entitlements."
	bash seed/populate_orders.sh
}

main "${@}"