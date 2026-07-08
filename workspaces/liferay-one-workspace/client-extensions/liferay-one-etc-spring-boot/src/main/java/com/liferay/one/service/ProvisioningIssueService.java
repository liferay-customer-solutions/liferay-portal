/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.jira.service.JiraIssueService;
import com.liferay.one.jira.util.JiraDocumentUtil;
import com.liferay.one.pubsub.Message;
import com.liferay.one.salesforce.model.Account;
import com.liferay.one.salesforce.model.Opportunity;
import com.liferay.one.salesforce.model.OpportunityLineItem;
import com.liferay.one.util.SupportRegionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StackTraceUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class ProvisioningIssueService {

	public void addErrorIssue(Message message, Exception exception) {
		_addErrorIssue(message, null, message.getPayload(), exception);
	}

	public void addErrorIssue(
		Message message, JSONObject recordJSONObject, Exception exception) {

		String opportunityId = null;

		JSONObject opportunityJSONObject = recordJSONObject.optJSONObject(
			"opportunity");

		if (opportunityJSONObject != null) {
			opportunityId = opportunityJSONObject.optString("Id");
		}

		_addErrorIssue(
			message, opportunityId, recordJSONObject.toString(), exception);
	}

	public void addOpportunityInvoicedIssue(
		com.liferay.headless.admin.user.client.dto.v1_0.Account account,
		Account salesforceAccount, Opportunity opportunity,
		List<OpportunityLineItem> opportunityLineItems,
		List<String> warningMessages) {

		try {
			JSONArray contentJSONArray = new JSONArray(
			).put(
				JiraDocumentUtil.createParagraph(
					"Opportunity Information", true)
			).put(
				JiraDocumentUtil.createParagraph(
					"Account Name: " + account.getName(), false)
			).put(
				JiraDocumentUtil.createParagraph(
					"Account ID: " + account.getExternalReferenceCode(), false)
			).put(
				JiraDocumentUtil.createParagraph(
					"Opportunity Type: " + opportunity.getType(), false)
			).put(
				JiraDocumentUtil.createLinkParagraph(
					"One Liferay Account Link",
					StringBundler.concat(
						_portalURL, "/web/one/my-account#/",
						account.getExternalReferenceCode(), "/account-details"))
			).put(
				JiraDocumentUtil.createLinkParagraph(
					"Salesforce Opportunity Link",
					"https://liferay.my.salesforce.com/" + opportunity.getId())
			);

			if (!warningMessages.isEmpty()) {
				contentJSONArray.put(
					JiraDocumentUtil.createParagraph("Warnings", true));

				for (String warningMessage : warningMessages) {
					contentJSONArray.put(
						JiraDocumentUtil.createParagraph(
							"- " + warningMessage, false));
				}
			}

			contentJSONArray.put(
				JiraDocumentUtil.createParagraph(
					"Products Purchased in this Opportunity", true));

			for (OpportunityLineItem opportunityLineItem :
					opportunityLineItems) {

				contentJSONArray.put(
					JiraDocumentUtil.createParagraph(
						StringBundler.concat(
							opportunityLineItem.getProductName(),
							" (Quantity: ", opportunityLineItem.getQuantity(),
							")"),
						false));

				if (Validator.isNotNull(opportunityLineItem.getProductType())) {
					contentJSONArray.put(
						JiraDocumentUtil.createParagraph(
							"Product Type: " +
								opportunityLineItem.getProductType(),
							false));
				}

				contentJSONArray.put(
					JiraDocumentUtil.createParagraph(
						StringBundler.concat(
							"Date Range: ",
							opportunityLineItem.getServiceDate(), " - ",
							opportunityLineItem.getEndDate()),
						false)
				).put(
					JiraDocumentUtil.createHorizontalRule()
				);
			}

			Map<String, Object> customFields = new HashMap<>();

			if (Validator.isNotNull(salesforceAccount.getBillingCountry())) {
				customFields.put(
					_jiraProvisioningFieldCountry,
					salesforceAccount.getBillingCountry());
			}

			if (Validator.isNotNull(opportunity.getOwnerEmailAddress())) {
				customFields.put(
					_jiraProvisioningFieldOwner,
					opportunity.getOwnerEmailAddress());
			}

			customFields.put(
				_jiraProvisioningFieldProvisioningComponent,
				new JSONObject(
				).put(
					"value", "Opportunity Invoiced"
				));
			customFields.put(
				_jiraProvisioningFieldSupportRegion,
				new JSONObject(
				).put(
					"value",
					SupportRegionUtil.getSupportRegion(
						opportunity.getSoldBy(),
						salesforceAccount.getBillingCountry())
				));

			_putJiraOrganizationCustomFields(customFields);

			Set<String> productTypes = new LinkedHashSet<>();

			for (OpportunityLineItem opportunityLineItem :
					opportunityLineItems) {

				if (Validator.isNotNull(opportunityLineItem.getProductType())) {
					productTypes.add(opportunityLineItem.getProductType());
				}
			}

			StringBundler sb = new StringBundler(7);

			if (!warningMessages.isEmpty()) {
				sb.append("[Warning] ");
			}

			sb.append(opportunity.getType());
			sb.append(": ");

			if (!productTypes.isEmpty()) {
				sb.append(StringUtil.merge(productTypes, ", "));
				sb.append(" ");
			}

			sb.append("Subscription(s) for ");
			sb.append(account.getName());

			_jiraIssueService.addIssue(
				customFields,
				new JSONObject(
				).put(
					"content", contentJSONArray
				).put(
					"type", "doc"
				).put(
					"version", 1
				),
				_jiraProvisioningIssueTypeId, _jiraSupportHCProject,
				sb.toString());
		}
		catch (Exception exception) {
			_log.error(
				"Unable to add Jira issue for opportunity " +
					opportunity.getId(),
				exception);
		}
	}

	private void _addErrorIssue(
		Message message, String opportunityId, String payload,
		Exception exception1) {

		try {
			JSONArray contentJSONArray = new JSONArray(
			).put(
				JiraDocumentUtil.createParagraph(
					"An unexpected auto provisioning error occurred.", true)
			).put(
				JiraDocumentUtil.createParagraph(
					"Topic: " + message.getTopic(), false)
			);

			if (Validator.isNotNull(opportunityId)) {
				contentJSONArray.put(
					JiraDocumentUtil.createParagraph(
						"Opportunity ID: " + opportunityId, false));
			}

			contentJSONArray.put(
				JiraDocumentUtil.createParagraph("Message:", false)
			).put(
				JiraDocumentUtil.createCodeBlock(payload)
			).put(
				JiraDocumentUtil.createParagraph("Error(s):", false)
			).put(
				JiraDocumentUtil.createCodeBlock(
					StackTraceUtil.getStackTrace(exception1))
			);

			Map<String, Object> customFields =
				HashMapBuilder.<String, Object>put(
					"labels",
					new JSONArray(
					).put(
						"auto-generated"
					).put(
						"provisioning-error"
					)
				).build();

			_putJiraOrganizationCustomFields(customFields);

			String summary = "Auto-Provisioning Error";

			if (Validator.isNotNull(opportunityId)) {
				summary =
					"Auto-Provisioning Error for opportunity " + opportunityId;
			}

			_jiraIssueService.addIssue(
				customFields,
				new JSONObject(
				).put(
					"content", contentJSONArray
				).put(
					"type", "doc"
				).put(
					"version", 1
				),
				_jiraProvisioningIssueTypeId, _jiraSupportHCProject, summary);
		}
		catch (Exception exception2) {
			_log.error(
				"Unable to add Jira issue for the provisioning error",
				exception2);
		}
	}

	private void _putJiraOrganizationCustomFields(
		Map<String, Object> customFields) {

		customFields.put(
			_jiraProvisioningFieldOffering,
			new JSONArray(
			).put(
				new JSONObject(
				).put(
					"id", _jiraWorkspaceId + ":" + _jiraProvisioningOfferingId
				)
			));
		customFields.put(
			_jiraProvisioningFieldOrganization,
			new JSONArray(
			).put(
				new JSONObject(
				).put(
					"id",
					_jiraWorkspaceId + ":" + _jiraProvisioningOrganizationId
				)
			));
		customFields.put(
			_jiraSupportHCFieldRequestType, _jiraProvisioningRequestTypeId);
	}

	private static final Log _log = LogFactory.getLog(
		ProvisioningIssueService.class);

	@Autowired
	private JiraIssueService _jiraIssueService;

	@Value("${liferay.one.jira.provisioning.field.country}")
	private String _jiraProvisioningFieldCountry;

	@Value("${liferay.one.jira.provisioning.field.offering}")
	private String _jiraProvisioningFieldOffering;

	@Value("${liferay.one.jira.provisioning.field.organization}")
	private String _jiraProvisioningFieldOrganization;

	@Value("${liferay.one.jira.provisioning.field.owner}")
	private String _jiraProvisioningFieldOwner;

	@Value("${liferay.one.jira.provisioning.field.provisioning.component}")
	private String _jiraProvisioningFieldProvisioningComponent;

	@Value("${liferay.one.jira.provisioning.field.support.region}")
	private String _jiraProvisioningFieldSupportRegion;

	@Value("${liferay.one.jira.provisioning.issue.type.id}")
	private String _jiraProvisioningIssueTypeId;

	@Value("${liferay.one.jira.provisioning.offering.id}")
	private String _jiraProvisioningOfferingId;

	@Value("${liferay.one.jira.provisioning.organization.id}")
	private String _jiraProvisioningOrganizationId;

	@Value("${liferay.one.jira.provisioning.request.type.id}")
	private String _jiraProvisioningRequestTypeId;

	@Value("${liferay.one.jira.support.hc.field.request.type}")
	private String _jiraSupportHCFieldRequestType;

	@Value("${liferay.one.jira.support.hc.project}")
	private String _jiraSupportHCProject;

	@Value("${liferay.one.jira.workspace.id}")
	private String _jiraWorkspaceId;

	@Value("${liferay.one.portal.url}")
	private String _portalURL;

}