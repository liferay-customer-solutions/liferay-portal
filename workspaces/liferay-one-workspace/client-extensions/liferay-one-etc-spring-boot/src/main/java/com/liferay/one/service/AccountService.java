/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.custom.field.CustomField;
import com.liferay.headless.admin.user.client.custom.field.CustomValue;
import com.liferay.headless.admin.user.client.dto.v1_0.Account;
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
import com.liferay.petra.string.StringPool;
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

	public void addAccountUserAccount(long accountId, Jwt jwt, long userId)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		post(
			getAuthorization(jwt), StringPool.BLANK,
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/user-accounts/by-email-address/{emailAddress}"
			).buildAndExpand(
				accountId, userAccount.getEmailAddress()
			).toUri());
	}

	public void addAccountUserAccount(long accountId, long userId)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		post(
			getAuthorization(), StringPool.BLANK,
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/user-accounts/by-email-address/{emailAddress}"
			).buildAndExpand(
				accountId, userAccount.getEmailAddress()
			).toUri());
	}

	public void addAccountUserAccount(
			long accountId, long accountRoleId, long userId)
		throws Exception {

		addAccountUserAccount(accountId, userId);

		addAccountUserAccountRole(accountId, accountRoleId, userId);
	}

	public void addAccountUserAccount(
			String externalReferenceCode, Jwt jwt, long userId)
		throws Exception {

		Account account = getAccount(externalReferenceCode, jwt);

		addAccountUserAccount(account.getId(), jwt, userId);
	}

	public void addAccountUserAccountRole(
			long accountId, long accountRoleId, long userId)
		throws Exception {

		post(
			getAuthorization(), StringPool.BLANK,
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/account-roles/{accountRoleId}/user-accounts/{userId}"
			).buildAndExpand(
				accountId, accountRoleId, userId
			).toUri());
	}

	public void addAccountUserAccountRole(
			long accountRoleId, String externalReferenceCode, Jwt jwt,
			long userId)
		throws Exception {

		Account account = getAccount(externalReferenceCode, jwt);

		post(
			getAuthorization(jwt), StringPool.BLANK,
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/account-roles/{accountRoleId}/user-accounts/{userId}"
			).buildAndExpand(
				account.getId(), accountRoleId, userId
			).toUri());
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

	public Account fetchAccountByExternalReferenceCode(
			String externalReferenceCode)
		throws Exception {

		AccountResource accountResource = AccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		try {
			return accountResource.getAccountByExternalReferenceCode(
				externalReferenceCode);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public Long fetchAccountRoleId(long accountId, String name)
		throws Exception {

		List<Long> accountRoleIds = getAllItems(
			"/o/headless-admin-user/v1.0/accounts/" + accountId +
				"/account-roles",
			"name eq '" + name + "'", jsonObject -> jsonObject.getLong("id"));

		if (accountRoleIds.isEmpty()) {
			return null;
		}

		return accountRoleIds.get(0);
	}

	public Account getAccount(long accountEntryId, Jwt jwt) throws Exception {
		AccountResource accountResource = AccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).build();

		return accountResource.getAccount(accountEntryId);
	}

	public Account getAccount(String externalReferenceCode) throws Exception {
		AccountResource accountResource = AccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		return accountResource.getAccountByExternalReferenceCode(
			externalReferenceCode);
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

		AccountRoleResource accountRoleResource = _buildAccountRoleResource();

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

	public boolean hasDuplicateAccountName(
			String name, String externalReferenceCode)
		throws Exception {

		if (Validator.isNull(name)) {
			return false;
		}

		List<String> externalReferenceCodes = getAllItems(
			"/o/headless-admin-user/v1.0/accounts",
			"name eq '" + StringUtil.replace(name, '\'', "''") + "'",
			jsonObject -> jsonObject.optString("externalReferenceCode"));

		for (String otherExternalReferenceCode : externalReferenceCodes) {
			if (!Objects.equals(
					otherExternalReferenceCode, externalReferenceCode)) {

				return true;
			}
		}

		return false;
	}

	public void removeAccountUserAccount(
			String externalReferenceCode, Jwt jwt, long userId)
		throws Exception {

		Account account = getAccount(externalReferenceCode, jwt);

		delete(
			getAuthorization(jwt), StringPool.BLANK,
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/user-accounts/{userId}"
			).buildAndExpand(
				account.getId(), userId
			).toUri());
	}

	public void removeAccountUserAccountRole(
			long accountRoleId, String externalReferenceCode, Jwt jwt,
			long userId)
		throws Exception {

		Account account = getAccount(externalReferenceCode, jwt);

		delete(
			getAuthorization(jwt), StringPool.BLANK,
			UriComponentsBuilder.fromPath(
				"/o/headless-admin-user/v1.0/accounts/{accountId}" +
					"/account-roles/{accountRoleId}/user-accounts/{userId}"
			).buildAndExpand(
				account.getId(), accountRoleId, userId
			).toUri());
	}

	public void upsertAccount(
			com.liferay.one.salesforce.model.Account salesforceAccount)
		throws Exception {

		_upsertAccount(salesforceAccount);
	}

	public void upsertAccount(
			com.liferay.one.salesforce.model.Account salesforceAccount,
			String soldBy)
		throws Exception {

		Account account = _upsertAccount(salesforceAccount);

		if (account == null) {
			return;
		}

		_setDefaultLicensingCustomFields(
			account, salesforceAccount.getBillingCountry(), soldBy);
	}

	private AccountRoleResource _buildAccountRoleResource() {
		return AccountRoleResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
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

		List<CustomField> customFields = new ArrayList<>();

		if (Validator.isNotNull(salesforceAccount.getAccountTier())) {
			customFields.add(
				_toCustomField(
					"accountTier", salesforceAccount.getAccountTier()));
		}

		if (customFields.isEmpty()) {
			return;
		}

		account.setCustomFields(() -> customFields.toArray(new CustomField[0]));
	}

	private void _setDefaultLicensingCustomFields(
			Account account, String billingCountry, String soldBy)
		throws Exception {

		List<CustomField> customFields = new ArrayList<>();

		if (Validator.isNull(billingCountry) ||
			(!billingCountry.equals("Ireland") &&
			 !billingCountry.equals("United Kingdom"))) {

			customFields.add(_toCustomField("allowComplimentary", true));
		}

		if (Objects.equals(soldBy, "Liferay Brazil") ||
			Objects.equals(soldBy, "Liferay China") ||
			Objects.equals(soldBy, "Liferay India")) {

			customFields.add(_toCustomField("allowPermanentLicenses", false));
		}

		if (customFields.isEmpty()) {
			return;
		}

		AccountResource accountResource = AccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		Account patchAccount = new Account();

		patchAccount.setCustomFields(
			() -> customFields.toArray(new CustomField[0]));

		accountResource.patchAccount(account.getId(), patchAccount);
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

	private CustomField _toCustomField(String name, Object value) {
		CustomField customField = new CustomField();

		customField.setName(() -> name);

		CustomValue customValue = new CustomValue();

		customValue.setData(() -> value);

		customField.setCustomValue(() -> customValue);

		return customField;
	}

	private PostalAddress _toPostalAddress(
		String addressType, String city, String country,
		String externalReferenceCode, String name, String postalCode,
		String region, String street) {

		if (Validator.isNull(street) || Validator.isNull(city) ||
			Validator.isNull(postalCode)) {

			return null;
		}

		String truncatedCity = _truncate(city, _MAX_CITY_LENGTH);
		String truncatedStreet = _truncate(street, _MAX_STREET_LENGTH);

		PostalAddress postalAddress = new PostalAddress();

		postalAddress.setAddressCountry(() -> country);
		postalAddress.setAddressLocality(() -> truncatedCity);
		postalAddress.setAddressRegion(() -> region);
		postalAddress.setAddressType(() -> addressType);
		postalAddress.setExternalReferenceCode(() -> externalReferenceCode);
		postalAddress.setName(() -> name);
		postalAddress.setPostalCode(() -> postalCode);
		postalAddress.setStreetAddressLine1(() -> truncatedStreet);

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

	private String _truncate(String value, int maxLength) {
		if ((value != null) && (value.length() > maxLength)) {
			return value.substring(0, maxLength);
		}

		return value;
	}

	private Account _upsertAccount(
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
			return _upsertAccount(
				accountResource, account, salesforceAccount.getId());
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

			return _upsertAccount(
				accountResource, account, salesforceAccount.getId());
		}
	}

	private Account _upsertAccount(
			AccountResource accountResource, Account account,
			String externalReferenceCode)
		throws Exception {

		try {
			accountResource.patchAccountByExternalReferenceCode(
				externalReferenceCode, account);

			return null;
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return accountResource.putAccountByExternalReferenceCode(
					externalReferenceCode, account);
			}

			throw problemException;
		}
	}

	private static final int _MAX_CITY_LENGTH = 75;

	private static final int _MAX_STREET_LENGTH = 255;

	private static final int _PAGE_SIZE = 500;

	@Autowired
	private UserAccountService _userAccountService;

}