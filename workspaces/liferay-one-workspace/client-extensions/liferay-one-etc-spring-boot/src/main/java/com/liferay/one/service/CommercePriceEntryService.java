/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.pricing.client.dto.v2_0.PriceEntry;
import com.liferay.headless.commerce.admin.pricing.client.problem.Problem;
import com.liferay.headless.commerce.admin.pricing.client.resource.v2_0.PriceEntryResource;

import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class CommercePriceEntryService extends OneBaseService {

	public void addOrUpdatePriceEntry(
			boolean active, String externalReferenceCode, double price,
			long priceListId, long skuId)
		throws Exception {

		PriceEntry existingPriceEntry = _fetchPriceEntry(externalReferenceCode);

		PriceEntryResource priceEntryResource = _buildPriceEntryResource();

		if ((existingPriceEntry != null) &&
			!Objects.equals(existingPriceEntry.getPriceListId(), priceListId)) {

			priceEntryResource.deletePriceEntryByExternalReferenceCode(
				externalReferenceCode);

			existingPriceEntry = null;
		}

		PriceEntry priceEntry = new PriceEntry();

		priceEntry.setActive(() -> active);
		priceEntry.setExternalReferenceCode(() -> externalReferenceCode);
		priceEntry.setPrice(() -> price);
		priceEntry.setPriceListId(() -> priceListId);
		priceEntry.setSkuId(() -> skuId);

		if (existingPriceEntry != null) {
			priceEntryResource.patchPriceEntryByExternalReferenceCode(
				externalReferenceCode, priceEntry);
		}
		else {
			priceEntryResource.postPriceListIdPriceEntry(
				priceListId, priceEntry);
		}
	}

	public void deletePriceEntry(String externalReferenceCode)
		throws Exception {

		PriceEntry priceEntry = _fetchPriceEntry(externalReferenceCode);

		if (priceEntry == null) {
			return;
		}

		PriceEntryResource priceEntryResource = _buildPriceEntryResource();

		priceEntryResource.deletePriceEntryByExternalReferenceCode(
			externalReferenceCode);
	}

	private PriceEntryResource _buildPriceEntryResource() {
		return PriceEntryResource.builder(
		).endpoint(
			getDXPEndpointAddress(), lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
	}

	private PriceEntry _fetchPriceEntry(String externalReferenceCode)
		throws Exception {

		PriceEntryResource priceEntryResource = _buildPriceEntryResource();

		try {
			return priceEntryResource.getPriceEntryByExternalReferenceCode(
				externalReferenceCode);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

}