/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.partner;

import java.util.Map;

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

import com.liferay.partner.service.CloudFunctionsWebService;
import com.liferay.partner.service.UserAccountWebService;
import com.liferay.partner.utils.Constants;
import com.liferay.petra.string.StringBundler;

import org.json.JSONObject;

/**
 * @author Felipe Franca
 */
@RequestMapping("/object/entry/manager/salesforce")
@RestController
public class ObjectEntryManagerSalesforceRestController extends BaseRestController {

	@GetMapping("/{objectDefinitionExternalReferenceCode}")
	public ResponseEntity<String> get(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String objectDefinitionExternalReferenceCode,
			@RequestParam Map<String, String> parameters) throws Exception {

		JSONObject userAccountJsonObject = _userAccountWebService.getUserAccount(jwt);

		String scope = _getScope(userAccountJsonObject);

		if (scope.equals(Constants.UNAUTHORIZED)) {
			return new ResponseEntity<String>(
					"Access Denied",
					HttpStatus.FORBIDDEN);
		}

		String scopeString = _getScopeString(objectDefinitionExternalReferenceCode, scope, userAccountJsonObject);

		String stringParameters = _getStringParameters(parameters, scopeString);

		String endpoint = _getEndpoint(objectDefinitionExternalReferenceCode);

		JSONObject itemsJsonObject = _cloudFunctionsWebService.getItems(endpoint + stringParameters);

		return new ResponseEntity<String>(
				new JSONObject().put(
						"items", itemsJsonObject.getJSONArray("items")).put(
								"totalCount", itemsJsonObject.getInt("totalCount"))
						.toString(),
				HttpStatus.OK);
	}

	@GetMapping("/{objectDefinitionExternalReferenceCode}/{externalReferenceCode}")
	public ResponseEntity<String> get(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String objectDefinitionExternalReferenceCode, @PathVariable String externalReferenceCode)
			throws Exception {

		JSONObject userAccountJsonObject = _userAccountWebService.getUserAccount(jwt);

		String scope = _getScope(userAccountJsonObject);

		if (scope.equals(Constants.UNAUTHORIZED)) {
			return new ResponseEntity<String>(
					"Access Denied",
					HttpStatus.FORBIDDEN);
		}

		String endpoint = _getEndpoint(objectDefinitionExternalReferenceCode);

		JSONObject itemJsonObject = _cloudFunctionsWebService.getItems(endpoint + "/" + externalReferenceCode);

		if (_getSingleEntryPermission(itemJsonObject, objectDefinitionExternalReferenceCode, scope,
				userAccountJsonObject)) {
			return new ResponseEntity<String>(
					itemJsonObject.toString(),
					HttpStatus.OK);
		}

		return new ResponseEntity<String>(
				"Access Denied",
				HttpStatus.FORBIDDEN);
	}

	private String _getEndpoint(String objectDefinitionExternalReferenceCode) {
		if (objectDefinitionExternalReferenceCode.equals(Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE))
			return _leadProxyEndpoint;

		if (objectDefinitionExternalReferenceCode
				.equals(Constants.OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE))
			return _opportunityPartnerRoleProxyEndpoint;

		return null;
	}

	private String _getScope(JSONObject userAccountJsonObject) {
		boolean isChannel = false;
		boolean isPartner = false;

		for (Object roleBriefObject : userAccountJsonObject.getJSONArray("roleBriefs")) {
			JSONObject roleBriefJsonObject = new JSONObject(roleBriefObject.toString());

			if (roleBriefJsonObject.getString("name").equals(Constants.ADMINISTRATOR)) {
				return Constants.ADMINISTRATOR;
			}

			if (Constants.getChannelRoles().contains(roleBriefJsonObject.getString("name"))) {
				isChannel = true;
				continue;
			}

			if (Constants.getPartnerRoles().contains(roleBriefJsonObject.getString("name"))) {
				isPartner = true;
			}
		}

		if (isChannel)
			return Constants.CHANNEL;

		if (isPartner && userAccountJsonObject.getJSONArray("accountBriefs").length() > 0)
			return Constants.PARTNER;

		return Constants.UNAUTHORIZED;
	}

	private String _getScopeString(String objectDefinitionExternalReferenceCode, String scope,
			JSONObject userAccountJsonObject) {
		StringBundler sb = new StringBundler();

		if (scope.equals(Constants.ADMINISTRATOR)) {
			return sb.toString();
		}

		if (scope.equals(Constants.CHANNEL)) {
			if (objectDefinitionExternalReferenceCode.equals(Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE)) {
				sb.append("partnerAccountOwnerEmail eq \'");
				sb.append(userAccountJsonObject.getString("emailAddress"));
				sb.append("\'");

				return sb.toString();
			}

			if (objectDefinitionExternalReferenceCode
					.equals(Constants.OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE)) {
				sb.append("channelOwnerEmail eq \'");
				sb.append(userAccountJsonObject.getString("emailAddress"));
				sb.append("\'");

				return sb.toString();
			}
		}

		int accountBriefsCounter = 1;
		int accountBriefsSize = userAccountJsonObject.getJSONArray("accountBriefs").length();

		for (Object accountBriefObject : userAccountJsonObject.getJSONArray("accountBriefs")) {
			JSONObject accountBriefJsonObject = new JSONObject(accountBriefObject.toString());

			if (objectDefinitionExternalReferenceCode.equals(Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE)){
				sb.append("partnerAccountId eq \'");
			}

			if (objectDefinitionExternalReferenceCode.equals(Constants.OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE)){
				sb.append("accountExternalReferenceCode eq \'");
			}
			sb.append(accountBriefJsonObject.getString("externalReferenceCode"));
			sb.append("\'");

			if (accountBriefsCounter < accountBriefsSize) {
				sb.append(" or ");
			}

			accountBriefsCounter++;
		}

		return sb.toString();
	}

	private boolean _getSingleEntryPermission(JSONObject itemJsonObject, String objectDefinitionExternalReferenceCode,
			String scope, JSONObject userAccountJsonObject) {
		if (scope.equals(Constants.ADMINISTRATOR)) {
			return true;
		}

		if (scope.equals(Constants.CHANNEL)) {
			if (objectDefinitionExternalReferenceCode.equals(Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE)) {
				return itemJsonObject.getString("partnerAccountOwnerEmail")
						.equals(userAccountJsonObject.getString("emailAddress"));
			}

			if (objectDefinitionExternalReferenceCode
					.equals(Constants.OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE)) {
				return itemJsonObject.getString("opportunityOwnerEmail")
						.equals(userAccountJsonObject.getString("emailAddress"));
			}
		}

		for (Object accountBriefObject : userAccountJsonObject.getJSONArray("accountBriefs")) {
			JSONObject accountBriefJsonObject = new JSONObject(accountBriefObject.toString());

			if (objectDefinitionExternalReferenceCode
			.equals(Constants.LEAD_PROXY_EXTERNAL_REFERENCE_CODE) && itemJsonObject.getString("partnerAccountId")
					.equals(accountBriefJsonObject.getString("externalReferenceCode"))) {
				return true;
			}

			if (objectDefinitionExternalReferenceCode
			.equals(Constants.OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE) && itemJsonObject.getString("accountExternalReferenceCode")
					.equals(accountBriefJsonObject.getString("externalReferenceCode"))) {
				return true;
			}
		}

		return false;
	}

	private String _getStringParameters(Map<String, String> parameters, String scopeString) {
		StringBundler sb = new StringBundler("?");

		boolean addAmpersand = false;

		if (parameters.containsKey("filter")) {
			sb.append("filter=");

			if (!scopeString.isBlank()) {
				sb.append("(");
				sb.append(parameters.get("filter"));
				sb.append(") and ");
				sb.append(scopeString);
			} else {
				sb.append(parameters.get("filter"));
			}

			addAmpersand = true;
		} else if (!scopeString.isBlank()) {
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

			addAmpersand = true;
		}

		return sb.toString();
	}

	@Autowired
	private CloudFunctionsWebService _cloudFunctionsWebService;

	@Autowired
	private UserAccountWebService _userAccountWebService;

	@Value("${liferay.partner.cloud.functions.leadproxy.endpoint}")
	private String _leadProxyEndpoint;

	@Value("${liferay.partner.cloud.functions.opportunitypartnerroleproxy.endpoint}")
	private String _opportunityPartnerRoleProxyEndpoint;

}
