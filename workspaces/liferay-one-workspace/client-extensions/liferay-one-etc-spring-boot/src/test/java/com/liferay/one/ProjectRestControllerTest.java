/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.exception.GoogleCloudFunctionUnavailableException;
import com.liferay.one.exception.InvalidUsageProductException;
import com.liferay.one.permission.BusinessEventPermission;
import com.liferay.one.service.ProjectService;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Veloso
 */
public class ProjectRestControllerTest {

	@Test
	public void testGetUsageChecksPermissionBeforeReadingUsage()
		throws Exception {

		ProjectRestController projectRestController = _createController();

		Mockito.when(
			_projectService.getProjectUsage(
				_PRODUCT_EXTERNAL_REFERENCE_CODE, _EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new JSONObject()
		);

		projectRestController.getUsage(
			null, _EXTERNAL_REFERENCE_CODE, _PRODUCT_EXTERNAL_REFERENCE_CODE);

		InOrder inOrder = Mockito.inOrder(
			_businessEventPermission, _projectService);

		inOrder.verify(
			_businessEventPermission
		).check(
			ActionKeys.VIEW, null, _EXTERNAL_REFERENCE_CODE
		);

		inOrder.verify(
			_projectService
		).getProjectUsage(
			_PRODUCT_EXTERNAL_REFERENCE_CODE, _EXTERNAL_REFERENCE_CODE
		);
	}

	@Test
	public void testGetUsageDoesNotReadUsageWhenPermissionIsDenied()
		throws Exception {

		ProjectRestController projectRestController = _createController();

		Mockito.doThrow(
			new PrincipalException()
		).when(
			_businessEventPermission
		).check(
			ActionKeys.VIEW, null, _EXTERNAL_REFERENCE_CODE
		);

		Assertions.assertThrows(
			PrincipalException.class,
			() -> projectRestController.getUsage(
				null, _EXTERNAL_REFERENCE_CODE,
				_PRODUCT_EXTERNAL_REFERENCE_CODE));

		Mockito.verifyNoInteractions(_projectService);
	}

	@Test
	public void testGetUsageReturnsMetrics() throws Exception {
		ProjectRestController projectRestController = _createController();

		Mockito.when(
			_projectService.getProjectUsage(
				_PRODUCT_EXTERNAL_REFERENCE_CODE, _EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new JSONObject(
			).put(
				"metrics", new JSONObject()
			)
		);

		ResponseEntity<String> responseEntity = projectRestController.getUsage(
			null, _EXTERNAL_REFERENCE_CODE, _PRODUCT_EXTERNAL_REFERENCE_CODE);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertFalse(jsonObject.has("variant"));
		Assertions.assertTrue(jsonObject.has("metrics"));
	}

	@Test
	public void testHandleExceptionMapsGoogleCloudFunctionUnavailableToBadGateway() {
		ProjectRestController projectRestController = _createController();

		ResponseEntity<ProblemDetail> responseEntity =
			projectRestController.handleException(
				new GoogleCloudFunctionUnavailableException());

		Assertions.assertEquals(
			HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());

		ProblemDetail problemDetail = responseEntity.getBody();

		Assertions.assertEquals(
			HttpStatus.BAD_GATEWAY.value(), problemDetail.getStatus());
	}

	@Test
	public void testHandleExceptionMapsInvalidProductToBadRequest() {
		ProjectRestController projectRestController = _createController();

		ResponseEntity<ProblemDetail> responseEntity =
			projectRestController.handleException(
				new InvalidUsageProductException("Product is required"));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

		ProblemDetail problemDetail = responseEntity.getBody();

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
		Assertions.assertEquals(
			"Product is required", problemDetail.getDetail());
	}

	private ProjectRestController _createController() {
		ProjectRestController projectRestController =
			new ProjectRestController();

		ReflectionTestUtils.setField(
			projectRestController, "_businessEventPermission",
			_businessEventPermission);
		ReflectionTestUtils.setField(
			projectRestController, "_projectService", _projectService);

		return projectRestController;
	}

	private static final String _EXTERNAL_REFERENCE_CODE = "PRJCT-004";

	private static final String _PRODUCT_EXTERNAL_REFERENCE_CODE = "PRDCT-PAAS";

	private final BusinessEventPermission _businessEventPermission =
		Mockito.mock(BusinessEventPermission.class);
	private final ProjectService _projectService = Mockito.mock(
		ProjectService.class);

}