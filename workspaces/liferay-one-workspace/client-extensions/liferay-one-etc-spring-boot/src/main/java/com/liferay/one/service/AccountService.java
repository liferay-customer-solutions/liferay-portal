/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.custom.field.CustomField;
import com.liferay.headless.admin.user.client.custom.field.CustomValue;
import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountContactInformation;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountRole;
import com.liferay.headless.admin.user.client.dto.v1_0.EmailAddress;
import com.liferay.headless.admin.user.client.dto.v1_0.Phone;
import com.liferay.headless.admin.user.client.dto.v1_0.PostalAddress;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.dto.v1_0.WebUrl;
import com.liferay.headless.admin.user.client.pagination.Page;
import com.liferay.headless.admin.user.client.pagination.Pagination;
import com.liferay.headless.admin.user.client.problem.Problem;
import com.liferay.headless.admin.user.client.resource.v1_0.AccountResource;
import com.liferay.headless.admin.user.client.resource.v1_0.AccountRoleResource;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Amos Fong
 */
@Component
public class AccountService extends OneBaseService {

	public void addAccountUserAccount(
			long accountId, long userId, Long accountRoleId)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		post(
			getAuthorization(), "",
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/user-accounts/by-email-address/{emailAddress}"
			).buildAndExpand(
				accountId, userAccount.getEmailAddress()
			).toUri());

		if (accountRoleId != null) {
			post(
				getAuthorization(), "",
				UriComponentsBuilder.fromPath(
					"/o/headless-admin-user/v1.0/accounts/{accountId}" +
						"/account-roles/{accountRoleId}/user-accounts/{userId}"
				).buildAndExpand(
					accountId, accountRoleId, userId
				).toUri());
		}
	}

	public void ensureAccountUserAccount(long accountId, long userId)
		throws Exception {

		if (_hasAccountUserAccount(accountId, userId)) {
			return;
		}

		addAccountUserAccount(accountId, userId, null);
	}

	public Account fetchAccount(long accountId) throws Exception {
		AccountResource accountResource = AccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		try {
			return accountResource.getAccount(accountId);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public Account getAccount(String externalReferenceCode, Jwt jwt)
		throws Exception {

		AccountResource accountResource = AccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).build();

		return accountResource.getAccountByExternalReferenceCode(
			externalReferenceCode);
	}

	public String getAccountRoleName(long accountId, long accountRoleId)
		throws Exception {

		AccountRoleResource accountRoleResource = AccountRoleResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		Page<AccountRole> accountRolesPage =
			accountRoleResource.getAccountAccountRolesPage(
				accountId, null, null, Pagination.of(1, _PAGE_SIZE), null);

		for (AccountRole accountRole : accountRolesPage.getItems()) {
			if (Objects.equals(accountRole.getId(), accountRoleId)) {
				return accountRole.getName();
			}
		}

		return null;
	}

	public boolean hasAccountUserAccount(long accountId, long userId)
		throws Exception {

		for (UserAccount userAccount :
				_userAccountService.getAccountUserAccounts(accountId)) {

			if (Objects.equals(userAccount.getId(), userId)) {
				return true;
			}
		}

		return false;
	}

	public void removeAccountUserAccount(long accountId, long userId)
		throws Exception {

		delete(
			getAuthorization(), "",
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/user-accounts/{userId}"
			).buildAndExpand(
				accountId, userId
			).toUri());
	}

	public void removeAccountUserAccountRole(
			long accountId, long userId, long accountRoleId)
		throws Exception {

		delete(
			getAuthorization(), "",
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/account-roles/{accountRoleId}/user-accounts/{userId}"
			).buildAndExpand(
				accountId, accountRoleId, userId
			).toUri());
	}

	public void upsertAccount(
			com.liferay.one.salesforce.model.Account salesforceAccount)
		throws Exception {

		AccountResource accountResource = AccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		Account account = new Account();

		account.setExternalReferenceCode(salesforceAccount::getId);
		account.setStatus(() -> WorkflowConstants.STATUS_APPROVED);
		account.setType(() -> Account.Type.BUSINESS);

		if (Validator.isNotNull(salesforceAccount.getName())) {
			account.setName(salesforceAccount::getName);
		}

		if (Validator.isNotNull(salesforceAccount.getDescription())) {
			account.setDescription(salesforceAccount::getDescription);
		}

		_setAccountContactInformation(account, salesforceAccount);

		_setCustomFields(account, salesforceAccount);

		_setPostalAddresses(account, salesforceAccount);

		try {
			_upsertAccount(accountResource, account, salesforceAccount.getId());
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem == null) ||
				!Objects.equals(problem.getTitle(), "The web URL is invalid")) {

				throw problemException;
			}

			AccountContactInformation accountContactInformation =
				account.getAccountContactInformation();

			accountContactInformation.setWebUrls(() -> null);

			_upsertAccount(accountResource, account, salesforceAccount.getId());
		}
	}

	private boolean _hasAccountUserAccount(long accountId, long userId)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		AccountBrief[] accountBriefs = userAccount.getAccountBriefs();

		if (accountBriefs == null) {
			return false;
		}

		for (AccountBrief accountBrief : accountBriefs) {
			if (Objects.equals(accountBrief.getId(), accountId)) {
				return true;
			}
		}

		return false;
	}

	private void _setAccountContactInformation(
		Account account,
		com.liferay.one.salesforce.model.Account salesforceAccount) {

		String id = salesforceAccount.getId();

		List<EmailAddress> emailAddresses = new ArrayList<>();
		List<Phone> phones = new ArrayList<>();
		List<WebUrl> webUrls = new ArrayList<>();

		if (Validator.isNotNull(salesforceAccount.getOwnerEmail())) {
			EmailAddress emailAddress = new EmailAddress();

			emailAddress.setEmailAddress(salesforceAccount::getOwnerEmail);
			emailAddress.setExternalReferenceCode(() -> id + "-owner-email");
			emailAddress.setPrimary(() -> Boolean.TRUE);

			emailAddresses.add(emailAddress);
		}

		if (Validator.isNotNull(salesforceAccount.getPhone())) {
			Phone phone = new Phone();

			phone.setExternalReferenceCode(() -> id + "-phone");
			phone.setPhoneNumber(salesforceAccount::getPhone);
			phone.setPrimary(() -> Boolean.TRUE);

			phones.add(phone);
		}

		if (Validator.isNotNull(salesforceAccount.getFax())) {
			Phone phone = new Phone();

			phone.setExternalReferenceCode(() -> id + "-fax");
			phone.setPhoneNumber(salesforceAccount::getFax);
			phone.setPhoneType(() -> "fax");
			phone.setPrimary(() -> Boolean.FALSE);

			phones.add(phone);
		}

		String url = _toURL(salesforceAccount.getWebsite());

		if (url != null) {
			WebUrl webUrl = new WebUrl();

			webUrl.setExternalReferenceCode(() -> id + "-website");
			webUrl.setPrimary(() -> Boolean.TRUE);
			webUrl.setUrl(() -> url);

			webUrls.add(webUrl);
		}

		if (emailAddresses.isEmpty() && phones.isEmpty() && webUrls.isEmpty()) {
			return;
		}

		AccountContactInformation accountContactInformation =
			new AccountContactInformation();

		if (!emailAddresses.isEmpty()) {
			accountContactInformation.setEmailAddresses(
				() -> emailAddresses.toArray(new EmailAddress[0]));
		}

		if (!phones.isEmpty()) {
			accountContactInformation.setTelephones(
				() -> phones.toArray(new Phone[0]));
		}

		if (!webUrls.isEmpty()) {
			accountContactInformation.setWebUrls(
				() -> webUrls.toArray(new WebUrl[0]));
		}

		account.setAccountContactInformation(() -> accountContactInformation);
	}

	private void _setCustomFields(
		Account account,
		com.liferay.one.salesforce.model.Account salesforceAccount) {

		if (Validator.isNull(salesforceAccount.getAccountTier())) {
			return;
		}

		CustomField customField = new CustomField();

		customField.setName(() -> "accountTier");

		CustomValue customValue = new CustomValue();

		customValue.setData(salesforceAccount::getAccountTier);

		customField.setCustomValue(() -> customValue);

		account.setCustomFields(() -> new CustomField[] {customField});
	}

	private void _setPostalAddresses(
		Account account,
		com.liferay.one.salesforce.model.Account salesforceAccount) {

		String id = salesforceAccount.getId();

		List<PostalAddress> postalAddresses = new ArrayList<>();

		PostalAddress billingPostalAddress = _toPostalAddress(
			"billing", salesforceAccount.getBillingCity(),
			salesforceAccount.getBillingCountry(), id + "-billing",
			"Primary Billing Address", salesforceAccount.getBillingPostalCode(),
			salesforceAccount.getBillingState(),
			salesforceAccount.getBillingStreet());

		if (billingPostalAddress != null) {
			postalAddresses.add(billingPostalAddress);

			account.setDefaultBillingAddressExternalReferenceCode(
				() -> id + "-billing");
		}

		PostalAddress shippingPostalAddress = _toPostalAddress(
			"shipping", salesforceAccount.getShippingCity(),
			salesforceAccount.getShippingCountry(), id + "-shipping",
			"Primary Shipping Address",
			salesforceAccount.getShippingPostalCode(),
			salesforceAccount.getShippingState(),
			salesforceAccount.getShippingStreet());

		if (shippingPostalAddress != null) {
			postalAddresses.add(shippingPostalAddress);

			account.setDefaultShippingAddressExternalReferenceCode(
				() -> id + "-shipping");
		}

		if (!postalAddresses.isEmpty()) {
			account.setPostalAddresses(
				() -> postalAddresses.toArray(new PostalAddress[0]));
		}
	}

	private PostalAddress _toPostalAddress(
		String addressType, String city, String country,
		String externalReferenceCode, String name, String postalCode,
		String region, String street) {

		if (Validator.isNull(street) || Validator.isNull(city) ||
			Validator.isNull(postalCode)) {

			return null;
		}

		PostalAddress postalAddress = new PostalAddress();

		postalAddress.setAddressCountry(() -> country);
		postalAddress.setAddressLocality(() -> city);
		postalAddress.setAddressRegion(() -> region);
		postalAddress.setAddressType(() -> addressType);
		postalAddress.setExternalReferenceCode(() -> externalReferenceCode);
		postalAddress.setName(() -> name);
		postalAddress.setPostalCode(() -> postalCode);
		postalAddress.setStreetAddressLine1(() -> street);

		return postalAddress;
	}

	private String _toURL(String website) {
		if (Validator.isNull(website) || website.contains(" ") ||
			!website.contains(".")) {

			return null;
		}

		String lowerCaseWebsite = StringUtil.toLowerCase(website);

		if (lowerCaseWebsite.startsWith("http://") ||
			lowerCaseWebsite.startsWith("https://")) {

			return website;
		}

		return "https://" + website;
	}

	private void _upsertAccount(
			AccountResource accountResource, Account account,
			String externalReferenceCode)
		throws Exception {

		try {
			accountResource.patchAccountByExternalReferenceCode(
				externalReferenceCode, account);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				accountResource.putAccountByExternalReferenceCode(
					externalReferenceCode, account);
			}
			else {
				throw problemException;
			}
		}
	}

	private static final int _PAGE_SIZE = 500;

	@Autowired
	private UserAccountService _userAccountService;

}