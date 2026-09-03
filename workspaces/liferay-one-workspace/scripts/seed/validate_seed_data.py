#!/usr/bin/env python3

"""Validates the seed data before it is imported.

Every seed import runs with ON_ERROR_CONTINUE, and the scoped object batch
endpoints silently drop an entry whose relationship points at something that does
not exist. A dangling reference therefore costs nothing at import time and shows
up much later as an empty tab or a missing row, so the whole data set is checked
here instead, against the model it is supposed to describe:

    Account > Project > Contract > Order > Order Item > Entitlement > License Key

with entitlement definitions attached to products to say which entitlements an
order generates, and usage events recording consumption of an entitlement against
a usage definition.

Errors are defects that make the seed data describe something impossible, and
they fail the run. Warnings are coverage gaps -- a product nothing orders, a
definition nothing grants -- which are worth reporting but are a legitimate state
for a data set to be in.
"""

import datetime
import glob
import json
import os
import sys

from collections import Counter, \
	defaultdict

SEED_DIR = os.path.dirname(os.path.abspath(__file__))

DATA = os.path.join(SEED_DIR, "data")

WORKSPACE_ROOT = os.path.dirname(os.path.dirname(SEED_DIR))

GRANT_TYPE_UNLIMITED = "unlimited"

# Custom fields the entitlement generator reads off an order item. Without them a
# regeneration would produce dateless entitlements and treat the item as
# unapproved, so seeded items that lack them are a defect rather than a gap.

REQUIRED_ORDER_ITEM_CUSTOM_FIELDS = ("customStatus", "endDate", "startDate")

# An order deliberately authored to grant nothing, so it is exempt from the check
# that an order item whose product defines entitlements grants them.

NO_ENTITLEMENT_ORDERS = {"C_ORDER_SAAS_EXPERIENCE_NO_ENTITLEMENTS.json"}

# The overage orders are placed after the EntitlementGeneration object action is
# re-enabled, so their entitlements are produced by the runtime rather than seeded
# alongside the order. They are exempt from the same check, but everything else --
# their references, their chain, their order item custom fields -- is checked like
# any other order, because the generator reads those fields.

RUNTIME_ENTITLEMENT_ORDERS = set()


class Report:

	def __init__(self):
		self._errors = defaultdict(list)
		self._warnings = defaultdict(list)

	def error(self, category, message):
		self._errors[category].append(message)

	def print(self):
		for label, findings in (
			("ERROR", self._errors),
			("WARNING", self._warnings),
		):
			for category in sorted(findings):
				messages = findings[category]

				print(
					"\n%s: %s (%d)" % (label, category, len(messages)),
					file=sys.stderr if label == "ERROR" else sys.stdout,
				)

				for message in messages:
					print("  - %s" % message)

		return sum(len(messages) for messages in self._errors.values())

	def warning(self, category, message):
		self._warnings[category].append(message)

def _check_chain(report, scope):
	projects = scope["projects"]
	contracts = scope["contracts"]
	orders = scope["orders"]
	entitlements = scope["entitlements"]
	memberships = scope["memberships"]
	environments = scope["environments"]
	order_items = scope["order_items"]
	definitions = scope["definitions"]

	project_accounts = {
		project["externalReferenceCode"]: project.get(
			"r_accountEntryToProject_accountEntryERC"
		)
		for project in projects
	}
	contract_accounts = {
		contract["externalReferenceCode"]: contract.get(
			"r_accountEntryToContract_accountEntryERC"
		)
		for contract in contracts
	}
	contract_projects = {
		contract["externalReferenceCode"]: contract.get(
			"r_projectToContract_c_projectERC"
		)
		for contract in contracts
	}

	category = "Broken Account > Project > Contract > Order chain"

	for contract in contracts:
		erc = contract["externalReferenceCode"]
		project = contract_projects.get(erc)

		if project and project_accounts.get(project) != contract_accounts.get(erc):
			report.error(
				category,
				"contract %s is on account %s but its project %s is on account %s"
				% (
					erc,
					contract_accounts.get(erc),
					project,
					project_accounts.get(project),
				),
			)

	for order in orders:
		erc = order["externalReferenceCode"]
		account = order.get("accountExternalReferenceCode")
		contract = order.get("contractExternalReferenceCode")
		project = order.get("projectExternalReferenceCode")

		if contract and account and contract_accounts.get(contract) != account:
			report.error(
				category,
				"order %s is on account %s but its contract %s is on account %s"
				% (erc, account, contract, contract_accounts.get(contract)),
			)

		if project and account and project_accounts.get(project) != account:
			report.error(
				category,
				"order %s is on account %s but its project %s is on account %s"
				% (erc, account, project, project_accounts.get(project)),
			)

		if contract and project and contract_projects.get(contract) not in (
			None,
			project,
		):
			report.error(
				category,
				"order %s is on project %s but its contract %s belongs to project %s"
				% (erc, project, contract, contract_projects.get(contract)),
			)

	for membership in memberships:
		project = membership.get("r_projectToProjectMembership_c_projectERC")
		account = membership.get("r_accountEntryToProjectMembership_accountEntryERC")

		if project and account and project_accounts.get(project) != account:
			report.error(
				category,
				"project membership %s is on account %s but its project %s is on "
				"account %s"
				% (
					membership["externalReferenceCode"],
					account,
					project,
					project_accounts.get(project),
				),
			)

	for environment in environments:
		erc = environment["externalReferenceCode"]
		account = environment.get("r_accountEntryToEnvironment_accountEntryERC")
		contract = environment.get("r_contractToEnvironment_c_contractERC")
		project = environment.get("r_projectToEnvironment_c_projectERC")

		if project and account and project_accounts.get(project) != account:
			report.error(
				category,
				"environment %s is on account %s but its project %s is on account %s"
				% (erc, account, project, project_accounts.get(project)),
			)

		if contract and account and contract_accounts.get(contract) != account:
			report.error(
				category,
				"environment %s is on account %s but its contract %s is on account %s"
				% (erc, account, contract, contract_accounts.get(contract)),
			)

	order_by_erc = {order["externalReferenceCode"]: order for order in orders}
	order_item_orders = {
		order_item["externalReferenceCode"]: order_item["_order"]
		for order_item in order_items
	}
	order_item_skus = {
		order_item["externalReferenceCode"]: order_item.get("skuExternalReferenceCode")
		for order_item in order_items
	}
	definition_skus = {
		definition["externalReferenceCode"]: definition.get(
			"skuExternalReferenceCode"
		)
		for definition in definitions
	}

	pairs = Counter()

	for entitlement in entitlements:
		erc = entitlement["externalReferenceCode"]
		order_item = entitlement.get(
			"r_commerceOrderItemToEntitlement_commerceOrderItemERC"
		)

		if not order_item:
			report.error(
				"Entitlement detached from the order chain",
				"entitlement %s (%s) has no order item" % (erc, entitlement["_file"]),
			)

			continue

		definition = entitlement.get(
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionERC"
		)

		pairs[(order_item, definition)] += 1

		order_erc = order_item_orders.get(order_item)

		if order_erc is None:
			continue

		order = order_by_erc[order_erc]

		for label, entitlement_field, order_field in (
			(
				"account",
				"r_accountEntryToEntitlement_accountEntryERC",
				"accountExternalReferenceCode",
			),
			(
				"contract",
				"r_contractToEntitlement_c_contractERC",
				"contractExternalReferenceCode",
			),
			(
				"project",
				"r_projectToEntitlement_c_projectERC",
				"projectExternalReferenceCode",
			),
		):
			if (entitlement.get(entitlement_field) or None) != (
				order.get(order_field) or None
			):
				report.error(
					"Entitlement disagrees with its order",
					"entitlement %s %s=%r but its order %s has %s=%r"
					% (
						erc,
						label,
						entitlement.get(entitlement_field),
						order_erc,
						label,
						order.get(order_field),
					),
				)

		sku = order_item_skus.get(order_item)

		if (
			definition
			and sku
			and definition_skus.get(definition)
			and definition_skus[definition] != sku
		):
			report.error(
				"Entitlement definition is not on the ordered SKU",
				"entitlement %s uses definition %s from SKU %s but its order item "
				"%s sells SKU %s"
				% (erc, definition, definition_skus[definition], order_item, sku),
			)

	for (order_item, definition), count in sorted(pairs.items()):
		if count > 1:
			report.error(
				"Duplicate entitlement",
				"order item %s grants %d entitlements from definition %s, but the "
				"generator treats that pair as unique"
				% (order_item, count, definition),
			)


def _check_coverage(report, scope):
	definitions = scope["definitions"]
	entitlements = scope["entitlements"]
	license_keys = scope["license_keys"]
	order_items = scope["order_items"]
	order_license_keys = scope["order_license_keys"]
	products = scope["products"]
	usage_definitions = scope["usage_definitions"]
	usage_events = scope["usage_events"]

	definitions_by_sku = defaultdict(list)

	for definition in definitions:
		if definition.get("active"):
			definitions_by_sku[definition.get("skuExternalReferenceCode")].append(
				definition["externalReferenceCode"]
			)

	entitled = {
		entitlement.get("r_commerceOrderItemToEntitlement_commerceOrderItemERC")
		for entitlement in entitlements
	}

	for order_item in order_items:
		erc = order_item["externalReferenceCode"]

		if (
			erc in entitled
			or order_item["_file"] in NO_ENTITLEMENT_ORDERS
			or order_item["_file"] in RUNTIME_ENTITLEMENT_ORDERS
		):
			continue

		sku = order_item.get("skuExternalReferenceCode")

		if definitions_by_sku.get(sku):
			report.error(
				"Order item grants no entitlement",
				"order item %s (%s) sells SKU %s, which defines %d entitlement "
				"definition(s), but grants nothing"
				% (erc, order_item["_file"], sku, len(definitions_by_sku[sku])),
			)
		else:
			report.warning(
				"SKU defines no entitlements",
				"order item %s (%s) sells SKU %s, which has no entitlement "
				"definitions" % (erc, order_item["_file"], sku),
			)

	linked = {
		license_key["externalReferenceCode"] for license_key in order_license_keys
	}

	for license_key in license_keys:
		erc = license_key["externalReferenceCode"]

		if erc not in linked:
			report.error(
				"License key detached from the entitlement chain",
				"license key %s is never linked to an entitlement" % erc,
			)

	ordered = {
		order_item.get("skuExternalReferenceCode") for order_item in order_items
	}

	unsold = sorted(
		product["externalReferenceCode"]
		for product in products
		if not any(
			sku["externalReferenceCode"] in ordered for sku in product.get("skus", [])
		)
	)

	if unsold:
		report.warning(
			"Product is never ordered",
			"%d of %d products: %s" % (len(unsold), len(products), ", ".join(unsold)),
		)

	used = {
		entitlement.get(
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionERC"
		)
		for entitlement in entitlements
	}

	unused = sorted(
		definition["externalReferenceCode"]
		for definition in definitions
		if definition["externalReferenceCode"] not in used
	)

	if unused:
		report.warning(
			"Entitlement definition grants nothing",
			"%d of %d definitions: %s"
			% (len(unused), len(definitions), ", ".join(unused)),
		)

	metered = {
		event.get("r_usageDefinitionToUsageEvent_c_usageDefinitionERC")
		for event in usage_events
	}

	for usage_definition in usage_definitions:
		erc = usage_definition["externalReferenceCode"]

		if erc not in metered:
			report.warning(
				"Usage definition has no usage events",
				"usage definition %s" % erc,
			)


def _check_dates(report, contracts, entitlements):
	category = "Date problem"

	contract_windows = {}

	for contract in contracts:
		erc = contract["externalReferenceCode"]
		start = parse_date(contract.get("startDate"))
		end = parse_date(contract.get("endDate"))

		contract_windows[erc] = (start, end)

		if start is None:
			report.error(category, "contract %s has no start date" % erc)

		if end is None:
			report.error(category, "contract %s has no end date" % erc)

		if start and end and end <= start:
			report.error(
				category,
				"contract %s ends %s at or before it starts %s"
				% (erc, contract.get("endDate"), contract.get("startDate")),
			)

	for entitlement in entitlements:
		erc = entitlement["externalReferenceCode"]
		start = parse_date(entitlement.get("startDate"))
		end = parse_date(entitlement.get("endDate"))

		if start is None:
			report.error(category, "entitlement %s has no start date" % erc)

		if end is None:
			report.error(category, "entitlement %s has no end date" % erc)

		if start and end and end <= start:
			report.error(
				category,
				"entitlement %s ends %s at or before it starts %s"
				% (erc, entitlement.get("endDate"), entitlement.get("startDate")),
			)

		window = contract_windows.get(
			entitlement.get("r_contractToEntitlement_c_contractERC")
		)

		if not window:
			continue

		contract_start, contract_end = window

		if start and contract_start and start < contract_start:
			report.error(
				category,
				"entitlement %s starts %s before its contract starts %s"
				% (erc, entitlement.get("startDate"), contract_start.isoformat()),
			)

		if end and contract_end and end > contract_end:
			report.error(
				category,
				"entitlement %s ends %s after its contract ends %s"
				% (erc, entitlement.get("endDate"), contract_end.isoformat()),
			)


def _check_fields(report, definitions, entitlements, order_items):
	by_erc = {
		definition["externalReferenceCode"]: definition for definition in definitions
	}
	order_item_quantities = {
		order_item["externalReferenceCode"]: order_item.get("quantity")
		for order_item in order_items
	}

	for entitlement in entitlements:
		erc = entitlement["externalReferenceCode"]

		if not entitlement.get("name"):
			report.error(
				"Missing required field", "entitlement %s has no name" % erc
			)

		definition = by_erc.get(
			entitlement.get(
				"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionERC"
			)
		)

		if not definition:
			continue

		# EntitlementService.generateEntitlements copies the definition's name onto
		# the entitlement, so seeded and generated entitlements only agree when the
		# two match. They deliberately do not always match here, because the runtime
		# itself is of two minds about what this field holds: the SaaS, LDP, and
		# Experience usage strategies match a capacity entitlement against machine
		# keys (apv, document-library-size, vcpu), while CloudRestController and the
		# SLA checks in AccountsRestController and ProvisioningEmailService match
		# against display labels ("Up to 5 Production Pods", "Gold Support").
		#
		# No single spelling satisfies both, so this is reported rather than failed.
		# Resolving it means settling on one convention in EntitlementConstants and
		# renaming the definitions to match, which is a runtime change.

		if entitlement["name"] != definition.get("name"):
			report.warning(
				"Entitlement name differs from what the generator would write",
				"entitlement %s is named %r but its definition %s is named %r"
				% (
					erc,
					entitlement["name"],
					definition["externalReferenceCode"],
					definition.get("name"),
				),
			)

		if (
			definition.get("grantType")
			and entitlement.get("grantType")
			and definition["grantType"] != entitlement["grantType"]
		):
			report.error(
				"Entitlement disagrees with its definition",
				"entitlement %s has grant type %r but its definition %s has %r"
				% (
					erc,
					entitlement.get("grantType"),
					definition["externalReferenceCode"],
					definition.get("grantType"),
				),
			)

		# EntitlementService.generateEntitlements copies the definition's ceiling
		# onto the entitlement untouched, so a seeded ceiling the definition does
		# not carry is one a regeneration would drop.

		if entitlement.get("maxQuantity") != definition.get("maxQuantity"):
			report.error(
				"Entitlement disagrees with its definition",
				"entitlement %s has maximum quantity %r but its definition %s has %r"
				% (
					erc,
					entitlement.get("maxQuantity"),
					definition["externalReferenceCode"],
					definition.get("maxQuantity"),
				),
			)

		# LPD-100375 scales the granted quantity by how many of the SKU were
		# ordered, so the only quantity a regeneration can produce is the order
		# item's quantity times the definition's default.

		order_item_quantity = order_item_quantities.get(
			entitlement.get("r_commerceOrderItemToEntitlement_commerceOrderItemERC")
		)

		if (
			definition.get("defaultQuantity") is not None
			and order_item_quantity is not None
		):
			expected = order_item_quantity * definition["defaultQuantity"]

			if entitlement.get("quantity") != expected:
				report.error(
					"Entitlement disagrees with its definition",
					"entitlement %s grants %r but its order item sells %r of a "
					"definition granting %r each, so the generator would grant %r"
					% (
						erc,
						entitlement.get("quantity"),
						order_item_quantity,
						definition["defaultQuantity"],
						expected,
					),
				)

		# An unlimited grant is measured by its grant type, so a quantity would be
		# meaningless; anything else needs one.

		if entitlement.get("grantType") != GRANT_TYPE_UNLIMITED and (
			entitlement.get("quantity") is None
		):
			report.error(
				"Missing required field",
				"entitlement %s has no quantity and is not an unlimited grant"
				% erc,
			)

	for order_item in order_items:
		custom_fields = order_item.get("customFields") or {}

		missing = [
			field
			for field in REQUIRED_ORDER_ITEM_CUSTOM_FIELDS
			if not custom_fields.get(field)
		]

		if missing:
			report.error(
				"Order item is missing the fields the generator reads",
				"order item %s (%s) has no %s"
				% (
					order_item["externalReferenceCode"],
					order_item["_file"],
					", ".join(missing),
				),
			)


def _check_references(report, universes, scope):
	category = "Dangling reference"

	def check(collection, field, universe, label=None):
		for entry in collection:
			value = entry.get(field)

			if not value:
				continue

			if value not in universes[universe]:
				report.error(
					category,
					"%s %s points at %s %r, which does not exist"
					% (
						label or "entry",
						entry.get("externalReferenceCode"),
						universe,
						value,
					),
				)

	check(
		scope["projects"],
		"r_accountEntryToProject_accountEntryERC",
		"account",
		"project",
	)
	check(
		scope["contracts"],
		"r_accountEntryToContract_accountEntryERC",
		"account",
		"contract",
	)
	check(
		scope["contracts"], "r_projectToContract_c_projectERC", "project", "contract"
	)
	check(
		scope["contracts"],
		"r_originalContractToContract_c_contractERC",
		"contract",
		"contract",
	)
	check(
		scope["memberships"],
		"r_accountEntryToProjectMembership_accountEntryERC",
		"account",
		"project membership",
	)
	check(
		scope["memberships"],
		"r_projectToProjectMembership_c_projectERC",
		"project",
		"project membership",
	)
	check(
		scope["memberships"],
		"r_userToProjectMembership_userERC",
		"user",
		"project membership",
	)
	check(
		scope["memberships"],
		"roleExternalReferenceCode",
		"role",
		"project membership",
	)
	check(
		scope["license_keys"],
		"r_accountEntryToLicenseKey_accountEntryERC",
		"account",
		"license key",
	)
	check(
		scope["license_keys"],
		"r_projectToLicenseKey_c_projectERC",
		"project",
		"license key",
	)
	check(
		scope["license_keys"],
		"r_commerceProductToLicenseKey_CProductERC",
		"product",
		"license key",
	)
	check(
		scope["definitions"],
		"skuExternalReferenceCode",
		"SKU",
		"entitlement definition",
	)
	check(
		scope["definitions"],
		"r_usageDefinitionToEntitlementDefinition_c_usageDefinitionERC",
		"usage definition",
		"entitlement definition",
	)
	check(
		scope["environments"],
		"r_accountEntryToEnvironment_accountEntryERC",
		"account",
		"environment",
	)
	check(
		scope["environments"],
		"r_contractToEnvironment_c_contractERC",
		"contract",
		"environment",
	)
	check(
		scope["environments"],
		"r_projectToEnvironment_c_projectERC",
		"project",
		"environment",
	)
	check(
		scope["usage_events"],
		"r_environmentToUsageEvent_c_environmentERC",
		"environment",
		"usage event",
	)
	check(
		scope["usage_events"],
		"r_usageDefinitionToUsageEvent_c_usageDefinitionERC",
		"usage definition",
		"usage event",
	)
	check(
		scope["usage_events"],
		"r_entitlementToUsageEvent_c_entitlementERC",
		"entitlement",
		"usage event",
	)
	check(
		scope["sales_summaries"],
		"r_accountEntryToPublisherSalesSummary_accountEntryERC",
		"account",
		"publisher sales summary",
	)
	check(scope["orders"], "accountExternalReferenceCode", "account", "order")
	check(scope["orders"], "contractExternalReferenceCode", "contract", "order")
	check(scope["orders"], "projectExternalReferenceCode", "project", "order")
	check(scope["orders"], "orderTypeExternalReferenceCode", "order type", "order")
	check(
		scope["orders"],
		"publisherSalesSummaryExternalReferenceCode",
		"publisher sales summary",
		"order",
	)
	check(scope["order_items"], "skuExternalReferenceCode", "SKU", "order item")
	check(
		scope["entitlements"],
		"r_accountEntryToEntitlement_accountEntryERC",
		"account",
		"entitlement",
	)
	check(
		scope["entitlements"],
		"r_contractToEntitlement_c_contractERC",
		"contract",
		"entitlement",
	)
	check(
		scope["entitlements"],
		"r_projectToEntitlement_c_projectERC",
		"project",
		"entitlement",
	)
	check(
		scope["entitlements"],
		"r_commerceOrderItemToEntitlement_commerceOrderItemERC",
		"order item",
		"entitlement",
	)
	check(
		scope["entitlements"],
		"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionERC",
		"entitlement definition",
		"entitlement",
	)
	check(
		scope["usage_reports"],
		"accountExternalReferenceCode",
		"account",
		"usage report",
	)
	check(
		scope["usage_reports"],
		"contractExternalReferenceCode",
		"contract",
		"usage report",
	)
	check(
		scope["usage_reports"],
		"projectExternalReferenceCode",
		"project",
		"usage report",
	)
	check(scope["usage_reports"], "skuExternalReferenceCode", "SKU", "usage report")
	check(
		scope["usage_reports"],
		"r_usageDefinitionToUsageReport_c_usageDefinitionERC",
		"usage definition",
		"usage report",
	)
	check(
		scope["order_license_keys"],
		"externalReferenceCode",
		"license key",
		"license key link",
	)
	check(
		scope["order_license_keys"],
		"r_entitlementToLicenseKey_c_entitlementERC",
		"entitlement",
		"license key link",
	)


def _check_unique(report, scope):
	for label, key in (
		("account", "accounts"),
		("contract", "contracts"),
		("entitlement", "entitlements"),
		("entitlement definition", "definitions"),
		("environment", "environments"),
		("license key", "license_keys"),
		("order", "orders"),
		("order item", "order_items"),
		("product", "products"),
		("project", "projects"),
		("project membership", "memberships"),
		("publisher sales summary", "sales_summaries"),
		("usage definition", "usage_definitions"),
		("usage event", "usage_events"),
		("usage report", "usage_reports"),
		("user", "users"),
	):
		counts = Counter(item.get("externalReferenceCode") for item in scope[key])

		for erc, count in sorted(counts.items()):
			if count > 1:
				report.error(
					"Duplicate external reference code",
					"%s %r is defined %d times, so the import would upsert one over "
					"the other" % (label, erc, count),
				)

	sku_counts = Counter()

	for product in scope["products"]:
		for sku in product.get("skus", []):
			sku_counts[sku["externalReferenceCode"]] += 1

	for erc, count in sorted(sku_counts.items()):
		if count > 1:
			report.error(
				"Duplicate external reference code",
				"SKU %r is defined %d times" % (erc, count),
			)


def items(name):
	return load(os.path.join(DATA, name)).get("items", [])


def load(path):
	with open(path, encoding="utf-8") as file:
		return json.load(file)


def main():
	report = Report()

	accounts = items("01-account.batch-engine-data.json")
	users = items("02-user-account.batch-engine-data.json")
	projects = items("03-project.batch-engine-data.json")
	contracts = items("04-contract.batch-engine-data.json")
	products = items("06-commerce-product.batch-engine-data.json")
	license_keys = items("07-license-key.batch-engine-data.json")
	memberships = items("08-project-membership.batch-engine-data.json")
	sales_summaries = items("10-publisher-sales-summary.batch-engine-data.json")
	usage_definitions = items("13-usage-definition.batch-engine-data.json")
	environments = items("14-environment.batch-engine-data.json")
	definitions = items("15-entitlement-definition.batch-engine-data.json")

	batch = os.path.join(
		WORKSPACE_ROOT, "client-extensions", "liferay-one-batch", "batch"
	)

	order_types = load(
		os.path.join(batch, "12-commerce-order-type.batch-engine-data.json")
	)["items"]
	roles = load(os.path.join(batch, "13-role.batch-engine-data.json"))["items"]

	orders = []
	order_items = []
	entitlements = []
	order_license_keys = []
	usage_events = []

	usage_reports = []

	overage_paths = sorted(glob.glob(os.path.join(DATA, "overages", "*.json")))

	for path in sorted(glob.glob(os.path.join(DATA, "orders", "*.json"))) + (
		overage_paths
	):
		name = os.path.basename(path)
		data = load(path)

		if path in overage_paths:
			RUNTIME_ENTITLEMENT_ORDERS.add(name)

		usage_report = data.get("usageReport")

		if usage_report:
			usage_report["_file"] = name
			usage_reports.append(usage_report)

		order = data["order"]
		order["_file"] = name
		orders.append(order)

		for order_item in order.get("orderItems", []):
			order_item["_file"] = name
			order_item["_order"] = order["externalReferenceCode"]
			order_items.append(order_item)

		for entitlement in data.get("entitlements", []):
			entitlement["_file"] = name
			entitlements.append(entitlement)

		for license_key in data.get("licenseKeys", []):
			license_key["_file"] = name
			order_license_keys.append(license_key)

		for usage_event in data.get("usageEvents", []):
			usage_event["_file"] = name
			usage_events.append(usage_event)

	def ercs(collection):
		return {item["externalReferenceCode"] for item in collection}

	skus = {}

	for product in products:
		for sku in product.get("skus", []):
			skus[sku["externalReferenceCode"]] = product["externalReferenceCode"]

	universes = {
		"account": ercs(accounts),
		"contract": ercs(contracts),
		"entitlement": ercs(entitlements),
		"entitlement definition": ercs(definitions),
		"environment": ercs(environments),
		"license key": ercs(license_keys),
		"order": ercs(orders),
		"order item": ercs(order_items),
		"order type": ercs(order_types),
		"product": ercs(products),
		"project": ercs(projects),
		"publisher sales summary": ercs(sales_summaries),
		"role": ercs(roles),
		"SKU": set(skus),
		"usage definition": ercs(usage_definitions),
		"user": ercs(users),
	}

	_check_unique(report, locals())
	_check_references(report, universes, locals())
	_check_chain(report, locals())
	_check_dates(report, contracts, entitlements)
	_check_fields(report, definitions, entitlements, order_items)
	_check_coverage(report, locals())

	errors = report.print()

	if errors:
		print(
			"\nSeed data validation failed with %d error(s)." % errors,
			file=sys.stderr,
		)

		return 1

	print("\nSeed data validation passed.")

	return 0


def parse_date(value):
	if not value:
		return None

	try:
		return datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
	except ValueError:
		return None


if __name__ == "__main__":
	sys.exit(main())