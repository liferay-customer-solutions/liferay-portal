/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.model.Property;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.salesforce.model.SalesforceModelTestUtil;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;

import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class ProvisioningSubdomainServiceTest {

	@BeforeEach
	public void setUp() {
		_provisioningSubdomainService = new ProvisioningSubdomainService();

		_oktaService = Mockito.mock(OktaService.class);
		_propertyService = Mockito.mock(PropertyService.class);

		_account = new Account();

		_account.setExternalReferenceCode(_ACCOUNT_ERC);
		_account.setId(_ACCOUNT_ID);

		ReflectionTestUtils.setField(
			_provisioningSubdomainService, "_oktaService", _oktaService);
		ReflectionTestUtils.setField(
			_provisioningSubdomainService, "_propertyService",
			_propertyService);
	}

	@Test
	public void testProvisionSubdomainGeneratesAndAssignsSubdomain()
		throws Exception {

		Mockito.when(
			_propertyService.getProperties(Mockito.anyString())
		).thenReturn(
			Collections.emptyList()
		);

		_provisioningSubdomainService.provisionSubdomain(
			_account, List.of(_createPaasExperienceLineItem()));

		ArgumentCaptor<String> subdomainArgumentCaptor =
			ArgumentCaptor.forClass(String.class);

		Mockito.verify(
			_propertyService
		).addProperty(
			Mockito.eq(_ACCOUNT_ID),
			Mockito.eq(PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN),
			subdomainArgumentCaptor.capture()
		);

		String subdomain = subdomainArgumentCaptor.getValue();

		Assertions.assertEquals(8, subdomain.length());
		Assertions.assertTrue(subdomain.matches("[a-z]{8}"));

		ArgumentCaptor<String> filterArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_propertyService
		).getProperties(
			filterArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			filterArgumentCaptor.getValue(
			).contains(
				"value eq '" + subdomain + "'"
			));
		Assertions.assertTrue(
			filterArgumentCaptor.getValue(
			).contains(
				"name eq '" + PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN +
					"'"
			));

		Mockito.verify(
			_oktaService
		).createApplication(
			_ACCOUNT_ERC, subdomain
		);
	}

	@Test
	public void testProvisionSubdomainRetriesOnCollisionThenSucceeds()
		throws Exception {

		Property existingProperty = new Property(
			new JSONObject(
			).put(
				"id", 1L
			));

		Mockito.when(
			_propertyService.getProperties(Mockito.anyString())
		).thenReturn(
			List.of(existingProperty)
		).thenReturn(
			Collections.emptyList()
		);

		_provisioningSubdomainService.provisionSubdomain(
			_account, List.of(_createPaasExperienceLineItem()));

		ArgumentCaptor<String> filterArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_propertyService, Mockito.times(2)
		).getProperties(
			filterArgumentCaptor.capture()
		);

		List<String> filters = filterArgumentCaptor.getAllValues();

		Assertions.assertNotEquals(filters.get(0), filters.get(1));

		ArgumentCaptor<String> subdomainArgumentCaptor =
			ArgumentCaptor.forClass(String.class);

		Mockito.verify(
			_propertyService
		).addProperty(
			Mockito.eq(_ACCOUNT_ID),
			Mockito.eq(PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN),
			subdomainArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			filters.get(
				1
			).contains(
				"value eq '" + subdomainArgumentCaptor.getValue() + "'"
			));
	}

	@Test
	public void testProvisionSubdomainSkipsWhenNoPaasExperienceLine()
		throws Exception {

		SalesforceOpportunityLineItem lineItem =
			new SalesforceOpportunityLineItem(
				SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
					"USD", null, "LINE-1", "PROD-1", "Other Product",
					"Subscription", 5, null));

		_provisioningSubdomainService.provisionSubdomain(
			_account, List.of(lineItem));

		Mockito.verifyNoInteractions(_propertyService);
		Mockito.verifyNoInteractions(_oktaService);
	}

	@Test
	public void testProvisionSubdomainSkipsWhenSubdomainAlreadyExists()
		throws Exception {

		Mockito.when(
			_propertyService.getPropertyValue(
				_ACCOUNT_ID, PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN)
		).thenReturn(
			"existing-subdomain"
		);

		_provisioningSubdomainService.provisionSubdomain(
			_account, List.of(_createPaasExperienceLineItem()));

		Mockito.verify(
			_propertyService, Mockito.never()
		).addProperty(
			Mockito.anyLong(), Mockito.any(), Mockito.any()
		);

		Mockito.verifyNoInteractions(_oktaService);
	}

	@Test
	public void testProvisionSubdomainStopsAfterExhaustingAttempts()
		throws Exception {

		Property existingProperty = new Property(
			new JSONObject(
			).put(
				"id", 1L
			));

		Mockito.when(
			_propertyService.getProperties(Mockito.anyString())
		).thenReturn(
			List.of(existingProperty)
		);

		_provisioningSubdomainService.provisionSubdomain(
			_account, List.of(_createPaasExperienceLineItem()));

		Mockito.verify(
			_propertyService, Mockito.times(20)
		).getProperties(
			Mockito.anyString()
		);

		Mockito.verify(
			_propertyService, Mockito.never()
		).addProperty(
			Mockito.anyLong(), Mockito.any(), Mockito.any()
		);

		Mockito.verifyNoInteractions(_oktaService);
	}

	@Test
	public void testProvisionSubdomainSwallowsOktaFailure() throws Exception {
		Mockito.when(
			_propertyService.getProperties(Mockito.anyString())
		).thenReturn(
			Collections.emptyList()
		);

		Mockito.doThrow(
			new RuntimeException("Unable to create Okta application")
		).when(
			_oktaService
		).createApplication(
			Mockito.any(), Mockito.any()
		);

		Assertions.assertDoesNotThrow(
			() -> _provisioningSubdomainService.provisionSubdomain(
				_account, List.of(_createPaasExperienceLineItem())));

		Mockito.verify(
			_propertyService
		).addProperty(
			Mockito.eq(_ACCOUNT_ID),
			Mockito.eq(PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN),
			Mockito.any()
		);
	}

	private SalesforceOpportunityLineItem _createPaasExperienceLineItem() {
		return new SalesforceOpportunityLineItem(
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "PaaS Experience",
				"Subscription", 1, null));
	}

	private static final String _ACCOUNT_ERC = "ACCOUNT-1";

	private static final long _ACCOUNT_ID = 1000L;

	private Account _account;
	private OktaService _oktaService;
	private PropertyService _propertyService;
	private ProvisioningSubdomainService _provisioningSubdomainService;

}