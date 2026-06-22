/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.AccountRoleConstants;
import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.SupportRegionConstants;
import com.liferay.one.model.Project;
import com.liferay.one.model.ProjectMembership;
import com.liferay.one.util.LocaleUtil;
import com.liferay.one.util.UserAccountUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.Year;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class ProvisioningEmailService extends OneBaseService {

	public void sendAutoProvisionedWelcomeEmail(Account account)
		throws Exception {

		List<UserAccount> userAccounts =
			_userAccountService.getAccountUserAccounts(account.getId());

		for (UserAccount userAccount : userAccounts) {
			if (UserAccountUtil.hasAccountRole(
					userAccount, account.getId(),
					AccountRoleConstants.NAMES_CUSTOMER_ACCOUNT_ROLES) &&
				UserAccountUtil.isVerified(userAccount)) {

				_sendContactWelcomeEmail(userAccount, List.of(account));
			}
		}
	}

	public void sendAutoProvisionedWelcomeEmail(
			ProjectMembership projectMembership)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(
			projectMembership.getUserId());

		if (!UserAccountUtil.isVerified(userAccount) ||
			!_isNewCustomerOrPartnerMembership(projectMembership)) {

			return;
		}

		_sendContactWelcomeEmail(
			userAccount,
			List.of(
				_accountService.getAccount(projectMembership.getAccountId())));
	}

	public void sendContactAssignedWelcomeEmail(
			ProjectMembership projectMembership)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(
			projectMembership.getUserId());

		Account account = _accountService.getAccount(
			projectMembership.getAccountId());

		if (!UserAccountUtil.isVerified(userAccount) || !_isEnabled(account) ||
			!_isNewCustomerOrPartnerMembership(projectMembership)) {

			return;
		}

		_sendContactWelcomeEmail(userAccount, List.of(account));
	}

	public void sendContactVerifiedWelcomeEmail(UserAccount userAccount)
		throws Exception {

		if (!_isPartnerEnabled() && _getRegions().isEmpty()) {
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

	private String _getLanguageId(UserAccount userAccount) {
		String languageId = userAccount.getLanguageId();

		if (Validator.isNull(languageId)) {
			return _DEFAULT_LANGUAGE_ID;
		}

		return languageId;
	}

	private String _getProjectInvitationMessage(
		List<Project> projects, Locale locale) {

		if (projects.size() == 1) {
			Project project = projects.get(0);

			return _messageSource.getMessage(
				"you-have-been-invited-to-the-liferay-project-x",
				new Object[] {
					StringBundler.concat(
						"<br /><a href=\"", _portalURL,
						"\" style=\"text-decoration: none\">",
						HtmlUtil.escape(project.getName()), "</a>")
				},
				locale);
		}

		StringBundler sb = new StringBundler();

		sb.append(
			_messageSource.getMessage(
				"you-have-been-invited-to-the-following-liferay-projects", null,
				locale));
		sb.append("<br />");

		for (Project project : projects) {
			sb.append("<a href=\"");
			sb.append(_portalURL);
			sb.append("\" style=\"text-decoration: none\">");
			sb.append(HtmlUtil.escape(project.getName()));
			sb.append("</a><br />");
		}

		return sb.toString();
	}

	private List<Project> _getProjects(long accountId, long userId)
		throws Exception {

		List<Project> projects = new ArrayList<>();

		List<ProjectMembership> projectMemberships =
			_projectMembershipService.getProjectMemberships(accountId, userId);

		for (ProjectMembership projectMembership : projectMemberships) {
			Project project = _projectService.getProject(
				projectMembership.getProjectExternalReferenceCode());

			if (project != null) {
				projects.add(project);
			}
		}

		return projects;
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

	private String _getRegionEmailAddress(String supportRegion) {
		if (supportRegion.equals(SupportRegionConstants.AUSTRALIA)) {
			return _emailAddressAustralia;
		}
		else if (supportRegion.equals(SupportRegionConstants.BRAZIL)) {
			return _emailAddressBrazil;
		}
		else if (supportRegion.equals(SupportRegionConstants.CHINA)) {
			return _emailAddressChina;
		}
		else if (supportRegion.equals(SupportRegionConstants.HUNGARY)) {
			return _emailAddressHungary;
		}
		else if (supportRegion.equals(SupportRegionConstants.INDIA)) {
			return _emailAddressIndia;
		}
		else if (supportRegion.equals(SupportRegionConstants.JAPAN)) {
			return _emailAddressJapan;
		}
		else if (supportRegion.equals(SupportRegionConstants.SPAIN)) {
			return _emailAddressSpain;
		}
		else if (supportRegion.equals(SupportRegionConstants.UNITED_STATES)) {
			return _emailAddressUS;
		}

		return _emailAddressGlobal;
	}

	private List<String> _getRegions() {
		if (Validator.isNull(_regions) || _regions.equals("unused")) {
			return List.of();
		}

		return List.of(_regions.split(StringPool.COMMA));
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
				AccountRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_MANAGER) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"manage-team-members-and-roles", null, locale));
			sb.append("</li>");
		}

		if (contactRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"activate-your-liferay-products", null, locale));
			sb.append("</li>");
		}

		if (contactRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_MEMBER) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_REQUESTER) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_MARKETING_USER) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_MEMBER) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_SALES_USER) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_TECHNICAL_USER)) {

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
				AccountRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_REQUESTER) ||
			contactRoleNames.contains(
				AccountRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

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

			if (!_getRegions().isEmpty() &&
				UserAccountUtil.hasAccountRole(
					userAccount, accountId,
					AccountRoleConstants.NAMES_CUSTOMER_ACCOUNT_ROLES) &&
				_entitlementService.hasEntitlement(
					accountId, EntitlementConstants.NAMES_SLAS) &&
				_getRegions().contains(
					_commerceOrderService.getSupportRegion(account))) {

				eligible = true;
			}

			if (_isPartnerEnabled() &&
				UserAccountUtil.hasAccountRole(
					userAccount, accountId,
					AccountRoleConstants.NAMES_PARTNER_ACCOUNT_ROLES) &&
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

	private boolean _isCustomerOrPartnerRole(String roleExternalReferenceCode) {
		return ArrayUtil.contains(
			AccountRoleConstants.ERCS_CUSTOMER_AND_PARTNER_ACCOUNT_ROLES,
			roleExternalReferenceCode);
	}

	private boolean _isEnabled(Account account) throws Exception {
		long accountId = account.getId();

		if (_entitlementService.hasEntitlement(
				accountId, EntitlementConstants.NAMES_SLAS) &&
			_getRegions().contains(
				_commerceOrderService.getSupportRegion(account))) {

			return true;
		}

		if (_isPartnerEnabled() &&
			_entitlementService.hasEntitlement(
				accountId, EntitlementConstants.NAME_PARTNER)) {

			return true;
		}

		return false;
	}

	private boolean _isNewCustomerOrPartnerMembership(
			ProjectMembership projectMembership)
		throws Exception {

		if (!_isCustomerOrPartnerRole(
				projectMembership.getRoleExternalReferenceCode())) {

			return false;
		}

		List<ProjectMembership> projectMemberships =
			_projectMembershipService.getProjectMemberships(
				projectMembership.getAccountId(),
				projectMembership.getUserId());

		for (ProjectMembership otherProjectMembership : projectMemberships) {
			if (Objects.equals(
					projectMembership.getExternalReferenceCode(),
					otherProjectMembership.getExternalReferenceCode())) {

				continue;
			}

			if (_isCustomerOrPartnerRole(
					otherProjectMembership.getRoleExternalReferenceCode())) {

				return false;
			}
		}

		return true;
	}

	private boolean _isPartnerEnabled() {
		return Boolean.parseBoolean(_partnerEnabled);
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
		List<Project> projects = new ArrayList<>();

		for (Account account : accounts) {
			contactRoleNames.addAll(
				UserAccountUtil.getAccountRoleNames(
					userAccount, account.getId()));
			projects.addAll(_getProjects(account.getId(), userAccount.getId()));
		}

		String projectKey = "";
		String projectNameSuffix = "";

		if (projects.size() == 1) {
			Project project = projects.get(0);

			projectKey = project.getExternalReferenceCode();
			projectNameSuffix = " - " + project.getName();
		}

		JSONObject processedTemplateJSONObject =
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				"PROVISIONING-WELCOME", languageId,
				HashMapBuilder.put(
					"CONTACT_ROLE_ACTIONS_LIST",
					_getRoleActionsList(contactRoleNames, locale)
				).put(
					"PROJECT_INVITATION_MESSAGE",
					_getProjectInvitationMessage(projects, locale)
				).put(
					"PROJECT_KEY", projectKey
				).put(
					"PROJECT_NAME_SUFFIX", projectNameSuffix
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
	private String _partnerEnabled;

	@Value("${liferay.one.portal.url}")
	private String _portalURL;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProjectService _projectService;

	@Value("${liferay.one.provisioning.regions}")
	private String _regions;

	@Autowired
	private UserAccountService _userAccountService;

}