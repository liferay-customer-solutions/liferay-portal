/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.headless.admin.user.client.custom.field.CustomField;
import com.liferay.headless.admin.user.client.custom.field.CustomValue;
import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountContactInformation;
import com.liferay.headless.admin.user.client.dto.v1_0.EmailAddress;
import com.liferay.headless.admin.user.client.dto.v1_0.Phone;
import com.liferay.headless.admin.user.client.dto.v1_0.WebUrl;
import com.liferay.one.jira.constants.AccountConstants;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.portal.kernel.util.ArrayUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class AccountConverter extends BaseAssetObjectConverter {

	@Override
	public String getObjectTypeName() {
		return AccountConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(Account account) {
		return toAssetObject(
			account, account.getExternalReferenceCode(), account.getName());
	}

	public JiraAssetObject toAssetObject(
		Account account, String externalKey, String name) {

		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_EXTERNAL_KEY, externalKey);
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_NAME, name);
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_DESCRIPTION,
			account.getDescription());
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_EXTERNAL_CREATED_AT,
			formatDate(account.getDateCreated()));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_EXTERNAL_UPDATED_AT,
			formatDate(account.getDateModified()));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_STATUS,
			_status(account.getStatus()));

		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_CODE,
			_customFieldData(account, _CUSTOM_FIELD_NAME_ACCOUNT_CODE));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_TIER,
			_customFieldData(account, _CUSTOM_FIELD_NAME_ACCOUNT_TIER));

		AccountContactInformation accountContactInformation =
			account.getAccountContactInformation();

		if (accountContactInformation != null) {
			jiraAssetObject.setAttributeValue(
				AccountConstants.ATTRIBUTE_NAME_CONTACT_EMAIL_ADDRESS,
				_getEmailAddress(accountContactInformation));
			jiraAssetObject.setAttributeValue(
				AccountConstants.ATTRIBUTE_NAME_FAX_NUMBER,
				_getFaxNumber(accountContactInformation));
			jiraAssetObject.setAttributeValue(
				AccountConstants.ATTRIBUTE_NAME_PHONE_NUMBER,
				_getPhoneNumber(accountContactInformation));
			jiraAssetObject.setAttributeValue(
				AccountConstants.ATTRIBUTE_NAME_WEBSITE,
				_getWebsite(accountContactInformation));
		}

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	private Object _customFieldData(Account account, String name) {
		CustomField[] customFields = account.getCustomFields();

		if (customFields == null) {
			return null;
		}

		for (CustomField customField : customFields) {
			if (!name.equals(customField.getName())) {
				continue;
			}

			CustomValue customValue = customField.getCustomValue();

			if (customValue == null) {
				return null;
			}

			return customValue.getData();
		}

		return null;
	}

	private String _getEmailAddress(
		AccountContactInformation accountContactInformation) {

		EmailAddress[] emailAddresses =
			accountContactInformation.getEmailAddresses();

		if (ArrayUtil.isEmpty(emailAddresses)) {
			return null;
		}

		for (EmailAddress emailAddress : emailAddresses) {
			if (Boolean.TRUE.equals(emailAddress.getPrimary())) {
				return emailAddress.getEmailAddress();
			}
		}

		EmailAddress emailAddress = emailAddresses[0];

		return emailAddress.getEmailAddress();
	}

	private String _getFaxNumber(
		AccountContactInformation accountContactInformation) {

		Phone[] telephones = ArrayUtil.filter(
			accountContactInformation.getTelephones(),
			telephone -> "fax".equalsIgnoreCase(telephone.getPhoneType()));

		if (ArrayUtil.isEmpty(telephones)) {
			return null;
		}

		Phone telephone = telephones[0];

		return telephone.getPhoneNumber();
	}

	private String _getPhoneNumber(
		AccountContactInformation accountContactInformation) {

		Phone[] telephones = ArrayUtil.filter(
			accountContactInformation.getTelephones(),
			telephone -> !"fax".equalsIgnoreCase(telephone.getPhoneType()));

		if (ArrayUtil.isEmpty(telephones)) {
			return null;
		}

		for (Phone telephone : telephones) {
			if (Boolean.TRUE.equals(telephone.getPrimary())) {
				return telephone.getPhoneNumber();
			}
		}

		Phone telephone = telephones[0];

		return telephone.getPhoneNumber();
	}

	private String _getWebsite(
		AccountContactInformation accountContactInformation) {

		WebUrl[] webUrls = accountContactInformation.getWebUrls();

		if (ArrayUtil.isEmpty(webUrls)) {
			return null;
		}

		for (WebUrl webUrl : webUrls) {
			if (Boolean.TRUE.equals(webUrl.getPrimary())) {
				return webUrl.getUrl();
			}
		}

		WebUrl webUrl = webUrls[0];

		return webUrl.getUrl();
	}

	private String _status(Integer status) {
		if (status == null) {
			return null;
		}

		if (status == 0) {
			return "Active";
		}

		return "Closed";
	}

	private static final String _CUSTOM_FIELD_NAME_ACCOUNT_CODE = "accountCode";

	private static final String _CUSTOM_FIELD_NAME_ACCOUNT_TIER = "accountTier";

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}