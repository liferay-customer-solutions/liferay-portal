/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.ProductVersion;
import com.liferay.one.model.LicenseEntry;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * @author Allen Ziegenfus
 */
@Component
public class LicenseEntryService {

	public List<LicenseEntry> getLicenseEntries(String productKey) {
		List<LicenseEntry> licenseEntries = new ArrayList<>();

		for (LicenseEntry licenseEntry : _licenseEntries) {
			if (Objects.equals(licenseEntry.getProductKey(), productKey)) {
				licenseEntries.add(licenseEntry);
			}
		}

		return licenseEntries;
	}

	public List<LicenseEntry> getLicenseEntriesByNameVersion(
		String name, String version, boolean supportedVersions) {

		List<LicenseEntry> licenseEntries = new ArrayList<>();

		for (LicenseEntry licenseEntry : _licenseEntries) {
			String licenseEntryName = licenseEntry.getName();

			if ((licenseEntryName != null) && licenseEntryName.contains(name)) {
				licenseEntries.add(licenseEntry);
			}
		}

		return _filterByVersion(
			licenseEntries, name, version, supportedVersions);
	}

	public List<LicenseEntry> getLicenseEntriesByType(String type) {
		List<LicenseEntry> licenseEntries = new ArrayList<>();

		for (LicenseEntry licenseEntry : _licenseEntries) {
			if (Objects.equals(licenseEntry.getType(), type)) {
				licenseEntries.add(licenseEntry);
			}
		}

		return licenseEntries;
	}

	public List<LicenseEntry> getLicenseEntriesByVersion(
		String productKey, String version, boolean supportedVersions) {

		// TODO Resolve product name via CommerceProductService and apply
		// _filterByVersion (getOrder needs the name, not the productKey).

		return getLicenseEntries(productKey);
	}

	public LicenseEntry getLicenseEntry(String productKey, String type) {
		for (LicenseEntry licenseEntry : _licenseEntries) {
			if (Objects.equals(licenseEntry.getProductKey(), productKey) &&
				Objects.equals(licenseEntry.getType(), type)) {

				return licenseEntry;
			}
		}

		return null;
	}

	// Ported from LicenseEntryLocalServiceImpl.filterByVersion (LPD-89423).

	private List<LicenseEntry> _filterByVersion(
		List<LicenseEntry> licenseEntries, String name, String version,
		boolean supportedVersions) {

		List<LicenseEntry> curLicenseEntries = new ArrayList<>();

		for (LicenseEntry licenseEntry : licenseEntries) {
			int productVersionMinOrder = ProductVersion.getOrder(
				name, licenseEntry.getVersionMin(), supportedVersions);
			int productVersionMaxOrder = ProductVersion.getOrder(
				name, licenseEntry.getVersionMax(), supportedVersions);

			if ((Validator.isNull(licenseEntry.getVersionMin()) ||
				 (productVersionMinOrder <= ProductVersion.getOrder(
					 name, version, supportedVersions))) &&
				(Validator.isNull(licenseEntry.getVersionMax()) ||
				 (ProductVersion.getOrder(name, version, supportedVersions) <=
					 productVersionMaxOrder))) {

				curLicenseEntries.add(licenseEntry);
			}
		}

		return curLicenseEntries;
	}

	// TODO Inline production Provisioning_LicenseEntry rows (LPD-89423).

	private final List<LicenseEntry> _licenseEntries = Arrays.asList();

}