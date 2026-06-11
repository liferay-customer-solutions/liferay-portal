/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.resource.v1_0.UserAccountResource;
import com.liferay.one.constants.ContactRoleConstants;
import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.SupportRegion;
import com.liferay.one.util.LocaleUtil;
import com.liferay.one.util.UserAccountUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.Year;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class ProvisioningEmailService extends OneBaseService {

	public void sendAutoProvisionedWelcomeEmail(Account account)
		throws Exception {

		List<UserAccount> userAccounts = _accountService.getAccountUserAccounts(
			account.getId());

		for (UserAccount userAccount : userAccounts) {
			if (UserAccountUtil.hasAccountRole(
					userAccount, account.getId(),
					ContactRoleConstants.NAMES_CUSTOMER_CONTACT_ROLES) &&
				UserAccountUtil.isVerified(userAccount)) {

				_sendContactWelcomeEmail(userAccount, List.of(account));
			}
		}
	}

	public void sendAutoProvisionedWelcomeEmail(
			String emailAddress, Account account, List<String> currentRoleNames,
			List<String> addedRoleNames)
		throws Exception {

		UserAccount userAccount = _fetchUserAccountByEmailAddress(emailAddress);

		if ((userAccount == null) || !UserAccountUtil.isVerified(userAccount) ||
			!_isAssignedNewCustomerOrPartnerContactRole(
				currentRoleNames, addedRoleNames)) {

			return;
		}

		_sendContactWelcomeEmail(userAccount, List.of(account));
	}

	public void sendContactAssignedWelcomeEmail(
			UserAccount userAccount, Account account,
			List<String> currentRoleNames, List<String> addedRoleNames)
		throws Exception {

		if (!UserAccountUtil.isVerified(userAccount) || !_isEnabled(account) ||
			!_isAssignedNewCustomerOrPartnerContactRole(
				currentRoleNames, addedRoleNames)) {

			return;
		}

		_sendContactWelcomeEmail(userAccount, List.of(account));
	}

	public void sendContactVerifiedWelcomeEmail(UserAccount userAccount)
		throws Exception {

		if (!_partnerEnabled && _regions.isEmpty()) {
			return;
		}

		List<Account> accounts = _getWelcomeEligibleAccounts(userAccount);

		if (accounts.isEmpty()) {
			return;
		}

		_sendContactWelcomeEmail(userAccount, accounts);
	}

	public void sendPartnerContactUpdateEmail(
			Account account, UserAccount contact, String contactRoleName,
			String contactRoleAction)
		throws Exception {

		JSONObject processedTemplateJSONObject =
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				"PROVISIONING-PARTNER-CONTACT-UPDATE", _DEFAULT_LANGUAGE_ID,
				HashMapBuilder.put(
					"ACCOUNT_NAME", account.getName()
				).put(
					"CONTACT_EMAIL_ADDRESS", contact.getEmailAddress()
				).put(
					"CONTACT_FIRST_NAME", contact.getGivenName()
				).put(
					"CONTACT_LAST_NAME", contact.getFamilyName()
				).put(
					"CONTACT_ROLE", contactRoleName
				).put(
					"CONTACT_ROLE_ACTION", contactRoleAction
				).build());

		_notificationQueueEntryService.addNotificationQueueEntry(
			_emailAddressGlobal, "Liferay Provisioning",
			_partnerContactUpdateRecipient,
			processedTemplateJSONObject.getString("subject"),
			processedTemplateJSONObject.getString("body"));
	}

	private UserAccount _fetchUserAccountByEmailAddress(String emailAddress)
		throws Exception {

		UserAccountResource userAccountResource = UserAccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameter(
			"nestedFields", "customFields"
		).build();

		return userAccountResource.getUserAccountByEmailAddress(emailAddress);
	}

	private String _getAccountInvitationMessage(
		List<Account> accounts, Locale locale) {

		if (accounts.size() == 1) {
			Account account = accounts.get(0);

			return _messageSource.getMessage(
				"you-have-been-invited-to-the-liferay-project-x",
				new Object[] {
					StringBundler.concat(
						"<br /><a href=\"", _portalURL,
						"\" style=\"text-decoration: none\">",
						HtmlUtil.escape(account.getName()), "</a>")
				},
				locale);
		}

		StringBundler sb = new StringBundler();

		sb.append(
			_messageSource.getMessage(
				"you-have-been-invited-to-the-following-liferay-projects", null,
				locale));
		sb.append("<br />");

		for (Account account : accounts) {
			sb.append("<a href=\"");
			sb.append(_portalURL);
			sb.append("\" style=\"text-decoration: none\">");
			sb.append(HtmlUtil.escape(account.getName()));
			sb.append("</a><br />");
		}

		return sb.toString();
	}

	private String _getLanguageId(UserAccount userAccount) {
		String languageId = userAccount.getLanguageId();

		if (Validator.isNull(languageId)) {
			return _DEFAULT_LANGUAGE_ID;
		}

		return languageId;
	}

	private String _getProvisioningEmailAddress(List<Account> accounts)
		throws Exception {

		String provisioningEmailAddress = null;

		for (Account account : accounts) {
			String curProvisioningEmailAddress = _getRegionEmailAddress(
				_commerceOrderService.getSupportRegion(account));

			if ((provisioningEmailAddress != null) &&
				!provisioningEmailAddress.equals(curProvisioningEmailAddress)) {

				return _emailAddressGlobal;
			}

			provisioningEmailAddress = curProvisioningEmailAddress;
		}

		return provisioningEmailAddress;
	}

	private String _getRegionEmailAddress(SupportRegion supportRegion) {
		if (supportRegion == SupportRegion.AUSTRALIA) {
			return _emailAddressAustralia;
		}
		else if (supportRegion == SupportRegion.BRAZIL) {
			return _emailAddressBrazil;
		}
		else if (supportRegion == SupportRegion.CHINA) {
			return _emailAddressChina;
		}
		else if (supportRegion == SupportRegion.HUNGARY) {
			return _emailAddressHungary;
		}
		else if (supportRegion == SupportRegion.INDIA) {
			return _emailAddressIndia;
		}
		else if (supportRegion == SupportRegion.JAPAN) {
			return _emailAddressJapan;
		}
		else if (supportRegion == SupportRegion.SPAIN) {
			return _emailAddressSpain;
		}
		else if (supportRegion == SupportRegion.UNITED_STATES) {
			return _emailAddressUS;
		}

		return _emailAddressGlobal;
	}

	private String _getRoleActionsList(
		Set<String> contactRoleNames, Locale locale) {

		StringBundler sb = new StringBundler(21);

		sb.append("<ul><li>");
		sb.append(
			_messageSource.getMessage(
				"view-your-project's-subscriptions", null, locale));
		sb.append("</li>");

		if (contactRoleNames.contains(
				ContactRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_PARTNER_MANAGER) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"manage-team-members-and-roles", null, locale));
			sb.append("</li>");
		}

		if (contactRoleNames.contains(
				ContactRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"activate-your-liferay-products", null, locale));
			sb.append("</li>");
		}

		if (contactRoleNames.contains(
				ContactRoleConstants.NAME_ACCOUNT_MEMBER) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_ACCOUNT_REQUESTER) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_PARTNER_MARKETING_USER) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_PARTNER_MEMBER) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_PARTNER_SALES_USER) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_PARTNER_TECHNICAL_USER)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"view-the-activation-status-of-your-liferay-products", null,
					locale));
			sb.append("</li>");
		}

		sb.append("<li>");
		sb.append(
			_messageSource.getMessage(
				"learn-how-to-succeed-with-each-of-our-products", null,
				locale));
		sb.append("</li><li>");
		sb.append(
			_messageSource.getMessage(
				"search-our-extensive-knowledge-base", null, locale));
		sb.append("</li>");

		if (contactRoleNames.contains(
				ContactRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_ACCOUNT_REQUESTER) ||
			contactRoleNames.contains(
				ContactRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"request-help-from-our-support-team", null, locale));
			sb.append("</li>");
		}

		sb.append("</ul>");

		return sb.toString();
	}

	private List<Account> _getWelcomeEligibleAccounts(UserAccount userAccount)
		throws Exception {

		List<Account> accounts = new ArrayList<>();

		AccountBrief[] accountBriefs = userAccount.getAccountBriefs();

		if (accountBriefs == null) {
			return accounts;
		}

		for (AccountBrief accountBrief : accountBriefs) {
			Long accountId = accountBrief.getId();

			if (accountId == null) {
				continue;
			}

			Account account = _accountService.getAccount(accountId);

			boolean eligible = false;

			if (!_regions.isEmpty() &&
				UserAccountUtil.hasAccountRole(
					userAccount, accountId,
					ContactRoleConstants.NAMES_CUSTOMER_CONTACT_ROLES) &&
				_entitlementService.hasEntitlement(
					accountId, EntitlementConstants.NAMES_SLAS) &&
				_regions.contains(
					String.valueOf(
						_commerceOrderService.getSupportRegion(account)))) {

				eligible = true;
			}

			if (_partnerEnabled &&
				UserAccountUtil.hasAccountRole(
					userAccount, accountId,
					ContactRoleConstants.NAMES_PARTNER_CONTACT_ROLES) &&
				_entitlementService.hasEntitlement(
					accountId, EntitlementConstants.NAME_PARTNER)) {

				eligible = true;
			}

			if (eligible) {
				accounts.add(account);
			}
		}

		return accounts;
	}

	private boolean _isAssignedNewCustomerOrPartnerContactRole(
		List<String> currentRoleNames, List<String> addedRoleNames) {

		if (currentRoleNames != null) {
			for (String roleName : currentRoleNames) {
				if (_isCustomerOrPartnerContactRole(roleName)) {
					return false;
				}
			}
		}

		if (addedRoleNames != null) {
			for (String roleName : addedRoleNames) {
				if (_isCustomerOrPartnerContactRole(roleName)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean _isCustomerOrPartnerContactRole(String roleName) {
		if (ArrayUtil.contains(
				ContactRoleConstants.NAMES_CUSTOMER_CONTACT_ROLES, roleName) ||
			ArrayUtil.contains(
				ContactRoleConstants.NAMES_PARTNER_CONTACT_ROLES, roleName)) {

			return true;
		}

		return false;
	}

	private boolean _isEnabled(Account account) throws Exception {
		long accountId = account.getId();

		if (_entitlementService.hasEntitlement(
				accountId, EntitlementConstants.NAMES_SLAS) &&
			_regions.contains(
				String.valueOf(
					_commerceOrderService.getSupportRegion(account)))) {

			return true;
		}

		if (_partnerEnabled &&
			_entitlementService.hasEntitlement(
				accountId, EntitlementConstants.NAME_PARTNER)) {

			return true;
		}

		return false;
	}

	private void _sendContactWelcomeEmail(
			UserAccount userAccount, List<Account> accounts)
		throws Exception {

		if (accounts.isEmpty()) {
			return;
		}

		String languageId = _getLanguageId(userAccount);

		Locale locale = LocaleUtil.fromLanguageId(languageId);

		Set<String> contactRoleNames = new HashSet<>();

		for (Account account : accounts) {
			contactRoleNames.addAll(
				UserAccountUtil.getAccountRoleNames(
					userAccount, account.getId()));
		}

		String accountKey = "";
		String accountNameSuffix = "";

		if (accounts.size() == 1) {
			Account account = accounts.get(0);

			accountKey = account.getExternalReferenceCode();
			accountNameSuffix = " - " + account.getName();
		}

		JSONObject processedTemplateJSONObject =
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				"PROVISIONING-WELCOME", languageId,
				HashMapBuilder.put(
					"ACCOUNT_INVITATION_MESSAGE",
					_getAccountInvitationMessage(accounts, locale)
				).put(
					"ACCOUNT_KEY", accountKey
				).put(
					"ACCOUNT_NAME_SUFFIX", accountNameSuffix
				).put(
					"CONTACT_ROLE_ACTIONS_LIST",
					_getRoleActionsList(contactRoleNames, locale)
				).put(
					"YEAR",
					Year.now(
					).toString()
				).build());

		_notificationQueueEntryService.addNotificationQueueEntry(
			_getProvisioningEmailAddress(accounts), "Liferay Provisioning",
			userAccount.getEmailAddress(),
			processedTemplateJSONObject.getString("subject"),
			processedTemplateJSONObject.getString("body"));
	}

	private static final String _DEFAULT_LANGUAGE_ID = "en_US";

	@Autowired
	private AccountService _accountService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Value("${liferay.one.provisioning.email.address.australia}")
	private String _emailAddressAustralia;

	@Value("${liferay.one.provisioning.email.address.brazil}")
	private String _emailAddressBrazil;

	@Value("${liferay.one.provisioning.email.address.china}")
	private String _emailAddressChina;

	@Value("${liferay.one.provisioning.email.address.global}")
	private String _emailAddressGlobal;

	@Value("${liferay.one.provisioning.email.address.hungary}")
	private String _emailAddressHungary;

	@Value("${liferay.one.provisioning.email.address.india}")
	private String _emailAddressIndia;

	@Value("${liferay.one.provisioning.email.address.japan}")
	private String _emailAddressJapan;

	@Value("${liferay.one.provisioning.email.address.spain}")
	private String _emailAddressSpain;

	@Value("${liferay.one.provisioning.email.address.us}")
	private String _emailAddressUS;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private MessageSource _messageSource;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Autowired
	private NotificationTemplateService _notificationTemplateService;

	@Value("${liferay.one.provisioning.partner.contact.update.recipient}")
	private String _partnerContactUpdateRecipient;

	@Value("${liferay.one.provisioning.partner.enabled}")
	private boolean _partnerEnabled;

	@Value("${liferay.one.portal.url}")
	private String _portalURL;

	@Value("${liferay.one.provisioning.regions}")
	private List<String> _regions;

}