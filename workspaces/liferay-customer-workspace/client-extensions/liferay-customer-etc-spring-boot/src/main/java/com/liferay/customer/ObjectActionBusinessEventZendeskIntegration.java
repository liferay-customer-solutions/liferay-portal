/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.customer.constants.ExternalLinkConstants;
import com.liferay.customer.service.KoroneikiService;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ExternalLink;
import com.liferay.osb.spring.boot.client.zendesk.model.ZendeskTicket;
import com.liferay.osb.spring.boot.client.zendesk.search.SearchHits;
import com.liferay.osb.spring.boot.client.zendesk.search.ZendeskTicketQuery;
import com.liferay.osb.spring.boot.client.zendesk.service.ZendeskService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jenny Chen
 */
@RestController
public class ObjectActionBusinessEventZendeskIntegration
	extends BaseRestController {

	@RequestMapping(
		method = RequestMethod.POST,
		path = "/object/action/business/event/zendesk/integration"
	)
	public ResponseEntity<String> post(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		JSONObject jsonObject = new JSONObject(json);

		JSONObject businessEventJSONObject = jsonObject.getJSONObject(
			"objectEntryDTOBusinessEvent");

		JSONObject businessEventPropertiesJSONObject =
			businessEventJSONObject.getJSONObject("properties");

		String accountEntryToBusinessEventsERC =
			businessEventPropertiesJSONObject.getString(
				"accountEntryToBusinessEventsERC");

		try {
			JSONArray businessEventsjsonArray = _fetchBusinessEvents(
				accountEntryToBusinessEventsERC, jwt);

			_updateZendesk(
				_fetchZendeskOrganizationId(accountEntryToBusinessEventsERC),
				_getBusinessEvents(businessEventsjsonArray),
				_getImpactedZendeskTicketIds(businessEventsjsonArray));

			return new ResponseEntity<>(HttpStatus.OK);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to update Zendesk business events for " +
					accountEntryToBusinessEventsERC,
				exception);

			return new ResponseEntity(
				exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private JSONArray _fetchBusinessEvents(
			String externalReferenceCode, Jwt jwt)
		throws Exception {

		StringBundler sb = new StringBundler(4);

		sb.append("/o/c/businessevents?filter=");
		sb.append("r_accountEntryToBusinessEvents_accountEntryERC eq '");
		sb.append(externalReferenceCode);
		sb.append("'");

		JSONObject jsonObject = new JSONObject(
			get("Bearer " + jwt.getTokenValue(), sb.toString()));

		return _parseBusinessEvents(jsonObject.getJSONArray("items"));
	}

	private long _fetchZendeskOrganizationId(String externalReferenceCode)
		throws Exception {

		List<ExternalLink> externalLinks = _koroneikiService.fetchExternalLinks(
			externalReferenceCode, 1, 1000);

		for (ExternalLink externalLink : externalLinks) {
			String domain = externalLink.getDomain();
			String entityName = externalLink.getEntityName();

			if (domain.equals(ExternalLinkConstants.DOMAIN_ZENDESK) &&
				entityName.equals(
					ExternalLinkConstants.ENTITY_NAME_ZENDESK_ORGANIZATION)) {

				return GetterUtil.getLong(externalLink.getEntityId());
			}
		}

		return 0;
	}

	private String _getBusinessEvents(JSONArray jsonArray) {
		List<String> businessEvents = new ArrayList<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);

			List<String> businessEventFieldValues = new ArrayList<>();

			Iterator<String> iterator = jsonObject.keys();

			while (iterator.hasNext()) {
				String key = iterator.next();

				if (key.equals("impactedZendeskTicketIds")) {
					continue;
				}

				if (Validator.isNotNull(jsonObject.optString(key))) {
					businessEventFieldValues.add(
						key + ": " + jsonObject.getString(key));
				}
			}

			if (!businessEventFieldValues.isEmpty()) {
				businessEvents.add(
					StringUtil.merge(businessEventFieldValues, ",\n"));
			}
		}

		return StringUtil.merge(businessEvents, "\n\n");
	}

	private Long[] _getImpactedZendeskTicketIds(JSONArray jsonArray) {
		Set<Long> zendeskTicketIds = new HashSet<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);

			JSONArray impactedZendeskTicketIdsJSONArray =
				jsonObject.getJSONArray("impactedZendeskTicketIds");

			for (int j = 0; j < impactedZendeskTicketIdsJSONArray.length();
				 j++) {

				zendeskTicketIds.add(
					impactedZendeskTicketIdsJSONArray.getLong(j));
			}
		}

		return zendeskTicketIds.toArray(new Long[0]);
	}

	private JSONArray _parseBusinessEvents(JSONArray businessEventsjsonArray) {
		JSONArray parsedBusinessEventsJSONArray = new JSONArray();

		for (Object businessEventObject : businessEventsjsonArray) {
			JSONObject businessEventJSONObject = new JSONObject(
				businessEventObject.toString());

			JSONObject parsedBusinessEventJSONObject = new JSONObject();

			JSONObject currentLiferayVersionJSONObject =
				businessEventJSONObject.optJSONObject("currentLiferayVersion");

			if (currentLiferayVersionJSONObject != null) {
				parsedBusinessEventJSONObject.put(
					"currentVersion",
					currentLiferayVersionJSONObject.optString("name", null));
			}

			parsedBusinessEventJSONObject.put(
				"description",
				businessEventJSONObject.optString("description", null)
			).put(
				"impactedZendeskTicketIds",
				businessEventJSONObject.optJSONArray(
					"impactedZendeskTicketIds", new JSONArray())
			).put(
				"name", businessEventJSONObject.optString("name", null)
			);

			JSONObject newLiferayVersionJSONObject =
				businessEventJSONObject.optJSONObject("newLiferayVersion");

			if (newLiferayVersionJSONObject != null) {
				parsedBusinessEventJSONObject.put(
					"newVersion",
					newLiferayVersionJSONObject.optString("name", null));
			}

			String targetGoLiveDateTime = businessEventJSONObject.optString(
				"targetGoLiveDateTime", null);

			if (Validator.isNotNull(targetGoLiveDateTime)) {
				parsedBusinessEventJSONObject.put(
					"targetGoLiveDate", targetGoLiveDateTime.split("T")[0]);
			}

			JSONObject eventTypeJSONObject =
				businessEventJSONObject.optJSONObject("eventType");

			if (eventTypeJSONObject != null) {
				parsedBusinessEventJSONObject.put(
					"type", eventTypeJSONObject.optString("name", null));
			}

			parsedBusinessEventsJSONArray.put(parsedBusinessEventJSONObject);
		}

		return parsedBusinessEventsJSONArray;
	}

	private void _updateZendesk(
			long zendeskOrganizationId, String businessEvents,
			Long[] impactedZendeskTicketIds)
		throws Exception {

		_zendeskService.updateZendeskOrganization(
			zendeskOrganizationId, businessEvents);

		ZendeskTicketQuery zendeskTicketQuery = new ZendeskTicketQuery();

		zendeskTicketQuery.addCriterion(
			"organization:" + zendeskOrganizationId);
		zendeskTicketQuery.addCriterion("status<closed");

		int page = 1;

		while (page > 0) {
			zendeskTicketQuery.setPage(page);

			SearchHits<ZendeskTicket> searchHits = _zendeskService.search(
				zendeskTicketQuery);

			for (ZendeskTicket zendeskTicket : searchHits.getResults()) {
				Map<Long, String> customFields =
					zendeskTicket.getCustomFields();

				customFields.put(
					_zendeskBusinessEventTicketFieldId, businessEvents);

				Set<String> tags = zendeskTicket.getTags();

				if (ArrayUtil.contains(
						impactedZendeskTicketIds,
						zendeskTicket.getZendeskTicketId())) {

					tags.add("impacting_business_event");
				}

				_zendeskService.updateZendeskTicket(
					zendeskTicket.getZendeskTicketId(), zendeskOrganizationId,
					zendeskTicket.getRequesterId(), zendeskTicket.getStatus(),
					customFields, tags);
			}

			page = searchHits.getNextPage();
		}
	}

	private static final Log _log = LogFactory.getLog(
		ObjectActionBusinessEventZendeskIntegration.class);

	@Autowired
	private KoroneikiService _koroneikiService;

	@Value("${liferay.customer.zendesk.business.event.ticket.field.id}")
	private long _zendeskBusinessEventTicketFieldId;

	@Autowired
	private ZendeskService _zendeskService;

}