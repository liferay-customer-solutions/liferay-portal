/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.jira.converter.BusinessEventConverter;
import com.liferay.one.jira.model.AssetObject;
import com.liferay.one.jira.model.AssetObjectFieldOption;
import com.liferay.one.jira.model.BusinessEvent;
import com.liferay.one.jira.model.BusinessEventVersion;
import com.liferay.one.jira.model.SupportIssue;
import com.liferay.one.jira.service.JiraService;
import com.liferay.one.permission.BusinessEventPermission;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jenny Chen
 */
@RequestMapping("/jira")
@RestController
public class JiraRestController extends OneBaseRestController {

	@DeleteMapping("/accounts/{externalReferenceCode}/business-events/{id}")
	public ResponseEntity<String> deleteAccountsBusinessEvents(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("id") String id)
		throws Exception {

		_businessEventPermission.check(
			externalReferenceCode, ActionKeys.UPDATE, jwt);

		_jiraService.deleteBusinessEvent(id);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/accounts/{externalReferenceCode}/business-events")
	public ResponseEntity<String> getAccountsBusinessEvents(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		_businessEventPermission.check(
			externalReferenceCode, ActionKeys.VIEW, jwt);

		return _getResponseEntity(
			_jiraService.getBusinessEvents(externalReferenceCode),
			BusinessEvent::toJSONObject);
	}

	@GetMapping("/accounts/{externalReferenceCode}/business-events/{id}")
	public ResponseEntity<String> getAccountsBusinessEvents(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("id") String id)
		throws Exception {

		_businessEventPermission.check(
			externalReferenceCode, ActionKeys.VIEW, jwt);

		BusinessEvent businessEvent = _jiraService.getBusinessEvent(id);

		return new ResponseEntity<>(
			businessEvent.toJSONObject(
			).toString(),
			HttpStatus.OK);
	}

	@GetMapping(
		"/accounts/{externalReferenceCode}/business-events/{id}/versions"
	)
	public ResponseEntity<String> getAccountsBusinessEventsVersions(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("id") String id)
		throws Exception {

		_businessEventPermission.check(
			externalReferenceCode, ActionKeys.VIEW, jwt);

		return _getResponseEntity(
			_jiraService.getBusinessEventVersions(id),
			BusinessEventVersion::toJSONObject);
	}

	@GetMapping("/accounts/{externalReferenceCode}/tickets")
	public ResponseEntity<String> getAccountsTickets(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@RequestParam(defaultValue = StringPool.BLANK, required = false)
				String[] ticketIds)
		throws Exception {

		_businessEventPermission.check(
			externalReferenceCode, ActionKeys.VIEW, jwt);

		return _getResponseEntity(
			_jiraService.getSupportIssues(externalReferenceCode, ticketIds),
			SupportIssue::toJSONObject);
	}

	@GetMapping("/business-events/fields/{fieldName}/options")
	public ResponseEntity<String> getBusinessEventsFieldsOptions(
			@PathVariable("fieldName") String fieldName)
		throws Exception {

		return _getResponseEntity(
			_jiraService.getAssetObjectFieldOptions(
				fieldName, _jiraBusinessEventAssetObjectTypeId),
			AssetObjectFieldOption::toJSONObject);
	}

	@GetMapping("/product-versions")
	public ResponseEntity<String> getProductVersions() throws Exception {
		return _getResponseEntity(
			_jiraService.getAssetObjects(
				_OBJECT_SCHEMA_BUSINESS_EVENTS, _OBJECT_TYPE_PRODUCT_VERSION),
			AssetObject::toJSONObject);
	}

	@PostMapping("/accounts/{externalReferenceCode}/business-events")
	public ResponseEntity<String> postAccountsBusinessEvents(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@RequestBody String json)
		throws Exception {

		_businessEventPermission.check(
			externalReferenceCode, ActionKeys.UPDATE, jwt);

		UserAccount userAccount = _getMyUserAccount(jwt);

		_jiraService.createBusinessEvent(
			_businessEventConverter.toBusinessEvent(
				externalReferenceCode, json, userAccount.getEmailAddress()));

		return _getResponseEntity(
			_jiraService.getBusinessEvents(externalReferenceCode),
			BusinessEvent::toJSONObject);
	}

	@PutMapping("/accounts/{externalReferenceCode}/business-events/{id}")
	public ResponseEntity<String> putAccountsBusinessEvents(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("id") String id, @RequestBody String json)
		throws Exception {

		if (_log.isInfoEnabled()) {
			_log.info("PUT business event " + id);
		}

		_businessEventPermission.check(
			externalReferenceCode, ActionKeys.UPDATE, jwt);

		UserAccount userAccount = _getMyUserAccount(jwt);

		BusinessEvent businessEvent = _businessEventConverter.toBusinessEvent(
			externalReferenceCode, json, userAccount.getEmailAddress());

		businessEvent = _jiraService.updateBusinessEvent(businessEvent, id);

		return new ResponseEntity<>(
			businessEvent.toJSONObject(
			).toString(),
			HttpStatus.OK);
	}

	private UserAccount _getMyUserAccount(Jwt jwt) throws Exception {
		try {
			return _userAccountService.getMyUserAccount(jwt);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to get user account", exception);
			}

			throw new PrincipalException();
		}
	}

	private <T> ResponseEntity<String> _getResponseEntity(
		List<T> items, Function<T, JSONObject> transformFunction) {

		JSONObject responseJSONObject = new JSONObject();

		JSONArray itemsJSONArray = new JSONArray();

		for (T item : items) {
			itemsJSONArray.put(transformFunction.apply(item));
		}

		responseJSONObject.put("items", itemsJSONArray);

		return new ResponseEntity<>(
			responseJSONObject.toString(), HttpStatus.OK);
	}

	private static final String _OBJECT_SCHEMA_BUSINESS_EVENTS =
		"Business Events";

	private static final String _OBJECT_TYPE_PRODUCT_VERSION =
		"Product Version";

	private static final Log _log = LogFactory.getLog(JiraRestController.class);

	@Autowired
	private BusinessEventConverter _businessEventConverter;

	@Autowired
	private BusinessEventPermission _businessEventPermission;

	@Value("${liferay.one.jira.business.event.asset.object.type.id}")
	private String _jiraBusinessEventAssetObjectTypeId;

	@Autowired
	private JiraService _jiraService;

	@Autowired
	private UserAccountService _userAccountService;

}