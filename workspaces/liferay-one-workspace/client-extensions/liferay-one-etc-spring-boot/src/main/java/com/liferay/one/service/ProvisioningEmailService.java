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
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class ProvisioningEmailService extends OneBaseService {

	public void sendAssignedWelcomeEmail(long userId, Account account)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		if (!UserAccountUtil.isVerified(userAccount) || !_isEnabled(account)) {
			return;
		}

		_sendWelcomeEmail(
			userAccount, List.of(account),
			_getProjects(account.getId(), userId));
	}

	public void sendAutoProvisionedWelcomeEmail(Account account)
		throws Exception {

		List<UserAccount> userAccounts =
			_userAccountService.getAccountUserAccounts(account.getId());

		for (UserAccount userAccount : userAccounts) {
			if (UserAccountUtil.hasAccountRole(
					userAccount, account.getId(),
					AccountRoleConstants.NAMES_CUSTOMER_ACCOUNT_ROLES) &&
				UserAccountUtil.isVerified(userAccount)) {

				_sendWelcomeEmail(
					userAccount, List.of(account),
					_getProjects(account.getId(), userAccount.getId()));
			}
		}
	}

	public void sendPartnerUserUpdateEmail(
			Account account, UserAccount userAccount, String accountRoleName,
			String accountRoleAction)
		throws Exception {

		JSONObject processedTemplateJSONObject =
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				"PROVISIONING-PARTNER-USER-UPDATE", _DEFAULT_LANGUAGE_ID,
				HashMapBuilder.put(
					"ACCOUNT_NAME", account.getName()
				).put(
					"ACCOUNT_ROLE", accountRoleName
				).put(
					"ACCOUNT_ROLE_ACTION", accountRoleAction
				).put(
					"USER_EMAIL_ADDRESS", userAccount.getEmailAddress()
				).put(
					"USER_FIRST_NAME", userAccount.getGivenName()
				).put(
					"USER_LAST_NAME", userAccount.getFamilyName()
				).build());

		_notificationQueueEntryService.addNotificationQueueEntry(
			_emailAddressGlobal, "Liferay Provisioning",
			_partnerUserUpdateRecipient,
			processedTemplateJSONObject.getString("subject"),
			processedTemplateJSONObject.getString("body"));
	}

	public void sendVerifiedWelcomeEmail(UserAccount userAccount)
		throws Exception {

		List<Account> accounts = _getWelcomeEligibleAccounts(userAccount);

		if (accounts.isEmpty()) {
			return;
		}

		List<Project> projects = new ArrayList<>();

		for (Account account : accounts) {
			projects.addAll(_getProjects(account.getId(), userAccount.getId()));
		}

		_sendWelcomeEmail(userAccount, accounts, projects);
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
			Project project = _projectService.fetchProject(
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
				_commerceOrderService.getSupportRegion(
					account.getId(), account.getDefaultBillingAddressId()));

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

	private String _getRoleActionsList(
		Set<String> accountRoleNames, Locale locale) {

		StringBundler sb = new StringBundler(21);

		sb.append("<ul><li>");
		sb.append(
			_messageSource.getMessage(
				"view-your-project's-subscriptions", null, locale));
		sb.append("</li>");

		if (accountRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_MANAGER) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"manage-team-members-and-roles", null, locale));
			sb.append("</li>");
		}

		if (accountRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			sb.append("<li>");
			sb.append(
				_messageSource.getMessage(
					"activate-your-liferay-products", null, locale));
			sb.append("</li>");
		}

		if (accountRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_MEMBER) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_REQUESTER) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_MARKETING_USER) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_MEMBER) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_PARTNER_SALES_USER) ||
			accountRoleNames.contains(
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

		if (accountRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_ADMINISTRATOR) ||
			accountRoleNames.contains(
				AccountRoleConstants.NAME_ACCOUNT_REQUESTER) ||
			accountRoleNames.contains(
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

			Account account = _accountService.fetchAccount(accountId);

			boolean eligible = false;

			if (UserAccountUtil.hasAccountRole(
					userAccount, accountId,
					AccountRoleConstants.NAMES_CUSTOMER_ACCOUNT_ROLES) &&
				_entitlementService.hasEntitlement(
					accountId, EntitlementConstants.NAMES_SLAS)) {

				eligible = true;
			}

			if (UserAccountUtil.hasAccountRole(
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

	private boolean _isEnabled(Account account) throws Exception {
		long accountId = account.getId();

		if (_entitlementService.hasEntitlement(
				accountId, EntitlementConstants.NAMES_SLAS) ||
			_entitlementService.hasEntitlement(
				accountId, EntitlementConstants.NAME_PARTNER)) {

			return true;
		}

		return false;
	}

	private void _sendWelcomeEmail(
			UserAccount userAccount, List<Account> accounts,
			List<Project> projects)
		throws Exception {

		if (accounts.isEmpty()) {
			return;
		}

		String languageId = _getLanguageId(userAccount);

		Locale locale = LocaleUtil.fromLanguageId(languageId);

		Set<String> accountRoleNames = new HashSet<>();

		for (Account account : accounts) {
			accountRoleNames.addAll(
				UserAccountUtil.getAccountRoleNames(
					userAccount, account.getId()));
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
					"ACCOUNT_ROLE_ACTIONS_LIST",
					_getRoleActionsList(accountRoleNames, locale)
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

	@Value("${liferay.one.provisioning.partner.user.update.recipient}")
	private String _partnerUserUpdateRecipient;

	@Value("${liferay.one.portal.url}")
	private String _portalURL;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private UserAccountService _userAccountService;

}