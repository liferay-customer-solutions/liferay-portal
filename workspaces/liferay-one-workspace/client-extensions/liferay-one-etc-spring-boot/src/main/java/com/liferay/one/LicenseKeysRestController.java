/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.ClassNameConstants;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.exception.NoSuchLicenseKeyException;
import com.liferay.one.license.LicenseKeyCSVExporter;
import com.liferay.one.model.LicenseKey;
import com.liferay.one.model.SubscriptionEntry;
import com.liferay.one.permission.AdminPermission;
import com.liferay.one.permission.LicenseKeyPermission;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.LicenseKeyService;
import com.liferay.one.service.SubscriptionEntryService;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Amos Fong
 */
@RequestMapping("/license-keys")
@RestController
public class LicenseKeysRestController extends OneBaseRestController {

	@DeleteMapping("/subscriptions")
	public void deleteSubscriptions(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyIds") long[] licenseKeyIds)
		throws Exception {

		UserAccount userAccount = getMyUserAccount(jwt);

		for (long licenseKeyId : licenseKeyIds) {
			_subscriptionEntryService.deleteSubscriptionEntry(
				jwt, ClassNameConstants.LICENSE_KEY, licenseKeyId,
				userAccount.getId());
		}
	}

	@GetMapping("/{licenseKeyId}")
	public LicenseKey getLicenseKey(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("licenseKeyId") long licenseKeyId)
		throws Exception {

		LicenseKey licenseKey = _licenseKeyService.getLicenseKey(
			jwt, licenseKeyId);

		_licenseKeyPermission.check(
			licenseKey.getAccountEntryId(), ActionKeys.VIEW, jwt);

		return licenseKey;
	}

	@GetMapping
	public ResponseEntity<String> getLicenseKeys(
			@AuthenticationPrincipal Jwt jwt, @RequestParam("page") int page,
			@RequestParam("pageSize") int pageSize)
		throws Exception {

		_adminPermission.check(jwt);

		if ((pageSize < 1) || (pageSize > _MAX_PAGE_SIZE)) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"The page size must be between 1 and " + _MAX_PAGE_SIZE);
		}

		return ResponseEntity.ok(
		).contentType(
			MediaType.APPLICATION_JSON
		).body(
			_licenseKeyService.getLicenseKeysPage(
				page, pageSize
			).toString()
		);
	}

	@GetMapping("/{licenseKeyId}/download")
	public ResponseEntity<String> getLicenseKeysDownload(
			@AuthenticationPrincipal Jwt jwt, @PathVariable long licenseKeyId)
		throws Exception {

		LicenseKey licenseKey = _licenseKeyService.getLicenseKey(
			jwt, licenseKeyId);

		_licenseKeyPermission.check(
			licenseKey.getAccountEntryId(), ActionKeys.VIEW, jwt);

		if (licenseKey.getLicenseVersion() < 2) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		String fileName = _licenseKeyService.getLicenseKeyDownloadFileName(
			licenseKey);

		return ResponseEntity.ok(
		).contentType(
			MediaType.TEXT_XML
		).header(
			HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=\"" + fileName + "\""
		).body(
			_licenseKeyService.getLicenseKeyDownloadXML(licenseKey)
		);
	}

	@GetMapping("/download")
	public ResponseEntity<String> getLicenseKeysDownload(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyIds") long[] licenseKeyIds)
		throws Exception {

		List<LicenseKey> licenseKeys = _getActiveLicenseKeys(
			jwt, licenseKeyIds);

		if (licenseKeys.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		return ResponseEntity.ok(
		).contentType(
			MediaType.TEXT_XML
		).header(
			HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=\"" +
				_licenseKeyService.getLicenseKeysDownloadFileName(licenseKeys) +
					"\""
		).body(
			_licenseKeyService.getLicenseKeysDownloadXML(licenseKeys)
		);
	}

	@GetMapping("/download-zip")
	public ResponseEntity<byte[]> getLicenseKeysDownloadZip(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyIds") long[] licenseKeyIds)
		throws Exception {

		List<LicenseKey> licenseKeys = _getActiveLicenseKeys(
			jwt, licenseKeyIds);

		if (licenseKeys.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		return ResponseEntity.ok(
		).contentType(
			MediaType.parseMediaType("application/zip")
		).header(
			HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=\"activation-keys.zip\""
		).body(
			_licenseKeyService.getLicenseKeysDownloadZip(licenseKeys)
		);
	}

	@GetMapping("/export")
	public ResponseEntity<String> getLicenseKeysExport(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyIds") long[] licenseKeyIds)
		throws Exception {

		if (licenseKeyIds.length == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		List<LicenseKey> licenseKeys = _licenseKeyService.getLicenseKeysByIds(
			licenseKeyIds);

		if (licenseKeys.size() < licenseKeyIds.length) {
			throw new NoSuchLicenseKeyException(
				"Unable to find every license key in " +
					Arrays.toString(licenseKeyIds));
		}

		UserAccount userAccount = getMyUserAccount(jwt);

		for (LicenseKey licenseKey : licenseKeys) {
			_licenseKeyPermission.check(
				userAccount, licenseKey.getAccountEntryId(), ActionKeys.VIEW);
		}

		return ResponseEntity.ok(
		).contentType(
			_CONTENT_TYPE_CSV
		).header(
			HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=\"" + _licenseKeyCSVExporter.getFileName() +
				"\""
		).body(
			_licenseKeyCSVExporter.toCSV(licenseKeys)
		);
	}

	@GetMapping("/subscriptions")
	public boolean getSubscriptions(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyId") long licenseKeyId)
		throws Exception {

		UserAccount userAccount = getMyUserAccount(jwt);

		SubscriptionEntry subscriptionEntry =
			_subscriptionEntryService.fetchSubscriptionEntry(
				jwt, ClassNameConstants.LICENSE_KEY, licenseKeyId,
				userAccount.getId());

		if (subscriptionEntry != null) {
			return true;
		}

		return false;
	}

	@PostMapping("/type-free")
	public LicenseKey postLicenseKeysTypeFree(@RequestBody String json)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		String domains = jsonObject.optString("domains");
		long orderId = jsonObject.optLong("orderId");
		String owner = jsonObject.optString("owner");

		if (_licenseKeyService.hasValidLicenseKeyTypeFree(domains, owner)) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"A license key was already provisioned for the owner with " +
					"this domain");
		}

		Order order = _commerceOrderService.getCommerceOrder(orderId);

		Account account = order.getAccount();

		LicenseKey licenseKey = _licenseKeyService.addLicenseKeyTypeFree(
			account.getId(), domains, String.valueOf(orderId), owner);

		Integer orderStatus = order.getOrderStatus();

		if ((orderStatus == null) ||
			(orderStatus != CommerceOrderConstants.ORDER_STATUS_COMPLETED)) {

			_commerceOrderService.completeOrder(
				order.getId(),
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED);
		}

		return licenseKey;
	}

	@PostMapping("/type-free-domains-check")
	public void postLicenseKeysTypeFreeDomainsCheck(@RequestBody String json)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		if (_licenseKeyService.hasValidLicenseKeyTypeFree(
				jsonObject.optString("domains"),
				jsonObject.optString("owner"))) {

			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"A license key was already provisioned for the owner with " +
					"this domain");
		}
	}

	@PutMapping("/subscriptions")
	public void putSubscriptions(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyIds") long[] licenseKeyIds)
		throws Exception {

		UserAccount userAccount = getMyUserAccount(jwt);

		for (long licenseKeyId : licenseKeyIds) {
			LicenseKey licenseKey = _licenseKeyService.getLicenseKey(
				jwt, licenseKeyId);

			_licenseKeyPermission.check(
				userAccount, licenseKey.getAccountEntryId(), ActionKeys.VIEW);
		}

		for (long licenseKeyId : licenseKeyIds) {
			_subscriptionEntryService.addSubscriptionEntry(
				jwt, ClassNameConstants.LICENSE_KEY, licenseKeyId,
				userAccount.getId());
		}
	}

	private List<LicenseKey> _getActiveLicenseKeys(
			Jwt jwt, long[] licenseKeyIds)
		throws Exception {

		List<LicenseKey> licenseKeys = new ArrayList<>();

		UserAccount userAccount = getMyUserAccount(jwt);

		for (LicenseKey licenseKey : _getLicenseKeys(jwt, licenseKeyIds)) {
			if (!licenseKey.isActive()) {
				continue;
			}

			_licenseKeyPermission.check(
				userAccount, licenseKey.getAccountEntryId(), ActionKeys.VIEW);

			licenseKeys.add(licenseKey);
		}

		return licenseKeys;
	}

	private List<LicenseKey> _getLicenseKeys(Jwt jwt, long[] licenseKeyIds)
		throws Exception {

		List<LicenseKey> licenseKeys = _licenseKeyService.getLicenseKeysByIds(
			jwt, licenseKeyIds);

		if (licenseKeys.size() < licenseKeyIds.length) {
			throw new NoSuchLicenseKeyException(
				"Unable to find every license key in " +
					Arrays.toString(licenseKeyIds));
		}

		return licenseKeys;
	}

	private static final MediaType _CONTENT_TYPE_CSV = MediaType.parseMediaType(
		"text/csv");

	private static final int _MAX_PAGE_SIZE = 100;

	@Autowired
	private AdminPermission _adminPermission;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private LicenseKeyCSVExporter _licenseKeyCSVExporter;

	@Autowired
	private LicenseKeyPermission _licenseKeyPermission;

	@Autowired
	private LicenseKeyService _licenseKeyService;

	@Autowired
	private SubscriptionEntryService _subscriptionEntryService;

}