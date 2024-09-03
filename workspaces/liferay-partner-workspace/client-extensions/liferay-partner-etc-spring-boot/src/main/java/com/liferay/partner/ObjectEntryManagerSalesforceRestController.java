/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.partner;

import com.liferay.partner.service.CloudFunctionsWebService;
import com.liferay.partner.service.UserAccountWebService;
import com.liferay.partner.utils.Constants;
import com.liferay.petra.string.StringBundler;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Felipe Franca
 */
@RequestMapping("/object/entry/manager/salesforce")
@RestController
public class ObjectEntryManagerSalesforceRestController
	extends BaseRestController {

	@GetMapping("/{objectDefinitionExternalReferenceCode}")
	public ResponseEntity<String> get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String objectDefinitionExternalReferenceCode,
			@RequestParam Map<String, String> parameters)
		throws Exception {

		JSONObject userAccountJSONObject =
			_userAccountWebService.getUserAccount(jwt);

		String scope = _getScope(userAccountJSONObject);

		if (scope.equals(Constants.UNAUTHORIZED)) {
			return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
		}

		String stringParameters = _getStringParameters(
			parameters,
			_getScopeString(
				objectDefinitionExternalReferenceCode, scope,
				userAccountJSONObject));

		String endpoint = _getEndpoint(objectDefinitionExternalReferenceCode);

		JSONObject itemsJSONObject = _cloudFunctionsWebService.getItems(
			endpoint + stringParameters);

		return new ResponseEntity<>(
			new JSONObject(
			).put(
				"items", itemsJSONObject.getJSONArray("items")
			).put(
				"totalCount", itemsJSONObject.getInt("totalCount")
			).toString(),
			HttpStatus.OK);
	}

	@GetMapping(
		"/{objectDefinitionExternalReferenceCode}/{externalReferenceCode}"
	)
	public ResponseEntity<String> get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String objectDefinitionExternalReferenceCode,
			@PathVariable String externalReferenceCode)
		throws Exception {

		JSONObject userAccountJSONObject =
			_userAccountWebService.getUserAccount(jwt);

		String scope = _getScope(userAccountJSONObject);

		if (scope.equals(Constants.UNAUTHORIZED)) {
			return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
		}

		String endpoint = _getEndpoint(objectDefinitionExternalReferenceCode);

		JSONObject itemJSONObject = _cloudFunctionsWebService.getItems(
			endpoint + "/" + externalReferenceCode);

		if (_hasSingleEntryPermission(
				itemJSONObject, objectDefinitionExternalReferenceCode, scope,
				userAccountJSONObject)) {

			return new ResponseEntity<>(
				itemJSONObject.toString(), HttpStatus.OK);
		}

		return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
	}

	private String _getEndpoint(String objectDefinitionExternalReferenceCode) {
		if (objectDefinitionExternalReferenceCode.equals(
				Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE)) {

			return _leadProxyEndpoint;
		}

		if (objectDefinitionExternalReferenceCode.equals(
				Constants.
					OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE)) {

			return _opportunityPartnerRoleProxyEndpoint;
		}

		return null;
	}

	private String _getScope(JSONObject userAccountJSONObject) {
		boolean channel = false;
		boolean partner = false;

		String roleName;

		for (Object roleBriefObject :
				userAccountJSONObject.getJSONArray("roleBriefs")) {

			JSONObject roleBriefJSONObject = new JSONObject(
				roleBriefObject.toString());

			roleName = roleBriefJSONObject.getString("name");

			if (roleName.equals(Constants.ADMINISTRATOR)) {
				return Constants.ADMINISTRATOR;
			}

			if (Constants.getChannelRoles(
				).contains(
					roleName
				)) {

				channel = true;

				continue;
			}

			if (Constants.getPartnerRoles(
				).contains(
					roleName
				)) {

				partner = true;
			}
		}

		if (channel) {
			return Constants.CHANNEL;
		}

		JSONArray accountBriefsJSONArray = userAccountJSONObject.getJSONArray(
			"accountBriefs");

		if (partner && (accountBriefsJSONArray.length() > 0)) {
			return Constants.PARTNER;
		}

		return Constants.UNAUTHORIZED;
	}

	private String _getScopeString(
		String objectDefinitionExternalReferenceCode, String scope,
		JSONObject userAccountJSONObject) {

		StringBundler sb = new StringBundler();

		if (scope.equals(Constants.ADMINISTRATOR)) {
			return sb.toString();
		}

		if (scope.equals(Constants.CHANNEL)) {
			if (objectDefinitionExternalReferenceCode.equals(
					Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE)) {

				sb.append("partnerAccountOwnerEmail eq \'");
				sb.append(userAccountJSONObject.getString("emailAddress"));
				sb.append("\'");

				return sb.toString();
			}

			if (objectDefinitionExternalReferenceCode.equals(
					Constants.
						OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE)) {

				sb.append("channelOwnerEmail eq \'");
				sb.append(userAccountJSONObject.getString("emailAddress"));
				sb.append("\'");

				return sb.toString();
			}
		}

		int accountBriefsCounter = 1;
		int accountBriefsSize = userAccountJSONObject.getJSONArray(
			"accountBriefs"
		).length();

		for (Object accountBriefObject :
				userAccountJSONObject.getJSONArray("accountBriefs")) {

			JSONObject accountBriefJSONObject = new JSONObject(
				accountBriefObject.toString());

			if (objectDefinitionExternalReferenceCode.equals(
					Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE)) {

				sb.append("partnerAccountId eq \'");
			}

			if (objectDefinitionExternalReferenceCode.equals(
					Constants.
						OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE)) {

				sb.append("accountExternalReferenceCode eq \'");
			}

			sb.append(
				accountBriefJSONObject.getString("externalReferenceCode"));
			sb.append("\'");

			if (accountBriefsCounter < accountBriefsSize) {
				sb.append(" or ");
			}

			accountBriefsCounter++;
		}

		return sb.toString();
	}

	private String _getStringParameters(
		Map<String, String> parameters, String scopeString) {

		StringBundler sb = new StringBundler("?");

		boolean addAmpersand = false;

		if (parameters.containsKey("filter")) {
			sb.append("filter=");

			if (!scopeString.isBlank()) {
				sb.append("(");
				sb.append(parameters.get("filter"));
				sb.append(") and ");
				sb.append(scopeString);
			}
			else {
				sb.append(parameters.get("filter"));
			}

			addAmpersand = true;
		}
		else if (!scopeString.isBlank()) {
			sb.append("filter=");
			sb.append(scopeString);

			addAmpersand = true;
		}

		if (parameters.containsKey("page")) {
			if (addAmpersand) {
				sb.append("&");
			}

			sb.append("page=");
			sb.append(parameters.get("page"));

			addAmpersand = true;
		}

		if (parameters.containsKey("pageSize")) {
			if (addAmpersand) {
				sb.append("&");
			}

			sb.append("pageSize=");
			sb.append(parameters.get("pageSize"));

			addAmpersand = true;
		}

		if (parameters.containsKey("search")) {
			if (addAmpersand) {
				sb.append("&");
			}

			sb.append("search=");
			sb.append(parameters.get("search"));

			addAmpersand = true;
		}

		if (parameters.containsKey("sort")) {
			if (addAmpersand) {
				sb.append("&");
			}

			sb.append("sort=");
			sb.append(parameters.get("sort"));
		}

		return sb.toString();
	}

	private boolean _hasSingleEntryPermission(
		JSONObject itemJSONObject, String objectDefinitionExternalReferenceCode,
		String scope, JSONObject userAccountJSONObject) {

		if (scope.equals(Constants.ADMINISTRATOR)) {
			return true;
		}

		if (scope.equals(Constants.CHANNEL)) {
			String userEmailAddress = userAccountJSONObject.getString(
				"emailAddress");

			if (objectDefinitionExternalReferenceCode.equals(
					Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE)) {

				return userEmailAddress.equals(
					itemJSONObject.getString("partnerAccountOwnerEmail"));
			}

			if (objectDefinitionExternalReferenceCode.equals(
					Constants.
						OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE)) {

				return userEmailAddress.equals(
					itemJSONObject.getString("opportunityOwnerEmail"));
			}
		}

		String accountBriefExternalReferenceCode;

		for (Object accountBriefObject :
				userAccountJSONObject.getJSONArray("accountBriefs")) {

			JSONObject accountBriefJSONObject = new JSONObject(
				accountBriefObject.toString());

			accountBriefExternalReferenceCode =
				accountBriefJSONObject.getString("externalReferenceCode");

			if (objectDefinitionExternalReferenceCode.equals(
					Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE) &&
				accountBriefExternalReferenceCode.equals(
					itemJSONObject.getString("partnerAccountId"))) {

				return true;
			}

			if (objectDefinitionExternalReferenceCode.equals(
					Constants.
						OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE) &&
				accountBriefExternalReferenceCode.equals(
					itemJSONObject.getString("accountExternalReferenceCode"))) {

				return true;
			}
		}

		return false;
	}

	@Autowired
	private CloudFunctionsWebService _cloudFunctionsWebService;

	@Value("${liferay.partner.cloud.functions.leadproxy.endpoint}")
	private String _leadProxyEndpoint;

	@Value(
		"${liferay.partner.cloud.functions.opportunitypartnerroleproxy.endpoint}"
	)
	private String _opportunityPartnerRoleProxyEndpoint;

	@Autowired
	private UserAccountWebService _userAccountWebService;

}