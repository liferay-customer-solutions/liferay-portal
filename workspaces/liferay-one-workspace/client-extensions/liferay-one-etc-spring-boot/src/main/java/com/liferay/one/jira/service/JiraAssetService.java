/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.service;

import com.liferay.one.jira.converter.BaseJiraAssetObjectConverter;
import com.liferay.one.jira.exception.JiraAssetObjectException;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.util.AQLUtil;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class JiraAssetService {

	public void delete(
		BaseJiraAssetObjectConverter converter, String externalKey) {

		if (Validator.isNull(externalKey)) {
			return;
		}

		_keyedLock.withLock(
			_getLockKey(converter, externalKey),
			() -> _delete(converter, externalKey));
	}

	/**
	 * Resolves a single external key to its existing asset object ID.
	 *
	 * @param  converter the converter describing the asset object type and its
	 *         external key attribute
	 * @param  externalKey the external key of the reference asset object
	 *
	 * @return the ID of the existing asset object, or <code>null</code> if the
	 *         external key is <code>null</code> or no matching asset object
	 *         exists
	 */
	public String fetchReferenceObjectId(
		BaseJiraAssetObjectConverter converter, String externalKey) {

		if (Validator.isNull(externalKey)) {
			return null;
		}

		Map<String, String> externalKeyToObjectIdMap =
			_getExternalKeyToObjectIdMap(
				converter, Collections.singletonList(externalKey));

		return externalKeyToObjectIdMap.get(externalKey);
	}

	/**
	 * Resolves a collection of entities to the IDs of their existing asset
	 * objects. Entities whose external key is <code>null</code> or does not
	 * resolve to an existing asset object are skipped.
	 *
	 * @param  converter the converter describing the asset object type and its
	 *         external key attribute
	 * @param  entities the entities to resolve to asset objects
	 * @param  externalKeyFunction the function that extracts an external key
	 *         from an entity
	 *
	 * @return the IDs of the resolved asset objects, or <code>null</code> if
	 *         <code>entities</code> is <code>null</code>
	 */
	public <T> List<String> fetchReferenceObjectIds(
		BaseJiraAssetObjectConverter converter, Collection<T> entities,
		Function<T, String> externalKeyFunction) {

		if (entities == null) {
			return null;
		}

		Set<String> externalKeys = new LinkedHashSet<>();

		for (T entity : entities) {
			if (entity == null) {
				continue;
			}

			String externalKey = externalKeyFunction.apply(entity);

			if (Validator.isNotNull(externalKey)) {
				externalKeys.add(externalKey);
			}
		}

		return _resolveToObjectIds(converter, externalKeys, null);
	}

	/**
	 * Resolves a collection of external keys to the IDs of their existing
	 * asset objects. External keys that do not resolve to an existing asset
	 * object are absent from the returned map.
	 *
	 * @param  converter the converter describing the asset object type and its
	 *         external key attribute
	 * @param  externalKeys the external keys of the reference asset objects
	 *
	 * @return the map from external key to existing asset object ID
	 */
	public Map<String, String> getExternalKeyToObjectIdMap(
		BaseJiraAssetObjectConverter converter,
		Collection<String> externalKeys) {

		return _getExternalKeyToObjectIdMap(
			converter, new ArrayList<>(externalKeys));
	}

	/**
	 * Returns the asset objects of the converter's type that match the AQL
	 * criteria appended by the consumer.
	 *
	 * @param  converter the converter describing the asset object type
	 * @param  consumer the consumer that appends criteria to the type's base
	 *         AQL
	 *
	 * @return the matching asset objects
	 */
	public List<JiraAssetObject> getJiraAssetObjects(
		BaseJiraAssetObjectConverter converter,
		Consumer<AQLUtil.Builder> consumer) {

		return _jiraAssetPersistence.searchObjects(
			converter.getAQLWithBuilder(consumer),
			converter::toJiraAssetObject);
	}

	/**
	 * Resolves a collection of entities to the IDs of their asset objects,
	 * creating an asset object for any external key that does not already
	 * resolve to one.
	 *
	 * @param  converter the converter describing the asset object type and its
	 *         external key attribute
	 * @param  entities the entities to resolve to asset objects
	 * @param  externalKeyFunction the function that extracts an external key
	 *         from an entity
	 * @param  createAssetObjectFunction the function that builds the asset
	 *         object to create for an entity whose external key is unresolved;
	 *         must not be <code>null</code>
	 *
	 * @return the IDs of the resolved and newly created asset objects, or
	 *         <code>null</code> if <code>entities</code> is <code>null</code>
	 */
	public <T> List<String> getOrCreateReferenceObjectIds(
		BaseJiraAssetObjectConverter converter, Collection<T> entities,
		Function<T, String> externalKeyFunction,
		Function<T, JiraAssetObject> createAssetObjectFunction) {

		Objects.requireNonNull(createAssetObjectFunction);

		if (entities == null) {
			return null;
		}

		Map<String, T> entitiesMap = new LinkedHashMap<>();

		for (T entity : entities) {
			if (entity == null) {
				continue;
			}

			String externalKey = externalKeyFunction.apply(entity);

			if (Validator.isNotNull(externalKey)) {
				entitiesMap.putIfAbsent(externalKey, entity);
			}
		}

		return _resolveToObjectIds(
			converter, entitiesMap.keySet(),
			externalKey -> createAssetObjectFunction.apply(
				entitiesMap.get(externalKey)));
	}

	/**
	 * Resolves a single external key to its existing asset object ID, throwing
	 * an exception when no matching asset object exists.
	 *
	 * @param  converter the converter describing the asset object type and its
	 *         external key attribute
	 * @param  externalKey the external key of the reference asset object
	 *
	 * @return the ID of the existing asset object
	 * @throws JiraAssetObjectException if no asset object exists for the given
	 *         external key
	 */
	public String getReferenceObjectId(
		BaseJiraAssetObjectConverter converter, String externalKey) {

		String objectId = fetchReferenceObjectId(converter, externalKey);

		if (objectId == null) {
			throw new JiraAssetObjectException(
				StringBundler.concat(
					"No \"", converter.getObjectTypeName(),
					"\" asset object exists for external key ", externalKey));
		}

		return objectId;
	}

	public boolean isUnchangedByExternalUpdatedAt(
		BaseJiraAssetObjectConverter converter,
		JiraAssetObject existingJiraAssetObject,
		JiraAssetObject jiraAssetObject) {

		String externalUpdatedAtAttributeName =
			converter.getExternalUpdatedAtAttributeName();

		String externalUpdatedAt = jiraAssetObject.getAttributeValue(
			externalUpdatedAtAttributeName);

		if (Validator.isNull(externalUpdatedAt)) {
			return false;
		}

		return Objects.equals(
			externalUpdatedAt,
			existingJiraAssetObject.getAttributeValue(
				externalUpdatedAtAttributeName));
	}

	/**
	 * Returns <code>true</code> if the asset object's external updated at
	 * attribute value is on or after the given date, meaning another sync has
	 * written the asset object since the date was captured.
	 *
	 * @param  converter the converter describing the asset object type
	 * @param  date the date to compare against
	 * @param  jiraAssetObject the asset object to check
	 *
	 * @return <code>true</code> if the asset object was updated on or after
	 *         the given date
	 */
	public boolean isUpdatedSince(
		BaseJiraAssetObjectConverter converter, Date date,
		JiraAssetObject jiraAssetObject) {

		String externalUpdatedAt = jiraAssetObject.getAttributeValue(
			converter.getExternalUpdatedAtAttributeName());

		if (Validator.isNull(externalUpdatedAt)) {
			return false;
		}

		String formattedDate = converter.formatDate(date);

		if (externalUpdatedAt.compareTo(formattedDate) >= 0) {
			return true;
		}

		return false;
	}

	/**
	 * Soft deletes an existing asset object by setting its deleted attribute
	 * to <code>true</code> instead of removing it. Unlike an upsert, no
	 * reference attribute is resolved, so the asset object can be soft deleted
	 * after the asset objects it references are gone.
	 *
	 * @param converter the converter describing the asset object type; it must
	 *        support soft deletion
	 * @param jiraAssetObject the existing asset object to soft delete
	 */
	public void softDelete(
		BaseJiraAssetObjectConverter converter,
		JiraAssetObject jiraAssetObject) {

		softDelete(converter, jiraAssetObject, null);
	}

	/**
	 * Soft deletes an existing asset object. A non-null predicate receives
	 * the asset object's freshly fetched state inside the asset-level lock
	 * and returns <code>true</code> to skip, so a decision made from an
	 * earlier snapshot cannot undo a concurrent write.
	 */
	public void softDelete(
		BaseJiraAssetObjectConverter converter, JiraAssetObject jiraAssetObject,
		Predicate<JiraAssetObject> shouldSkipUpdatePredicate) {

		String externalKey = jiraAssetObject.getAttributeValue(
			converter.getExternalKeyAttributeName());

		_keyedLock.withLock(
			_getLockKey(converter, externalKey),
			() -> _softDelete(
				converter, converter.getDeletedAttributeName(), externalKey,
				jiraAssetObject, shouldSkipUpdatePredicate));
	}

	/**
	 * Soft deletes every live asset object whose named attribute matches the
	 * given value, unconditionally and continuing past per-object failures.
	 * Only for delete cascades, where the matches reference a doomed entity
	 * and no concurrent write can make them worth keeping.
	 */
	public void softDeleteByAttribute(
		BaseJiraAssetObjectConverter converter, String attributeName,
		String attributeValue) {

		if (Validator.isNull(attributeValue)) {
			return;
		}

		List<JiraAssetObject> jiraAssetObjects = getJiraAssetObjects(
			converter,
			aqlBuilder -> aqlBuilder.andEquals(
				attributeValue, attributeName
			).andEquals(
				false, converter.getDeletedAttributeName()
			));

		// JSM has no batch update endpoint, so each matching asset object is
		// patched individually

		for (JiraAssetObject jiraAssetObject : jiraAssetObjects) {
			try {
				softDelete(converter, jiraAssetObject);
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to soft delete ", converter.getObjectTypeName(),
						" asset object ", jiraAssetObject.getObjectId()),
					exception);
			}
		}
	}

	public void upsert(
		BaseJiraAssetObjectConverter converter,
		JiraAssetObject jiraAssetObject) {

		upsert(converter, jiraAssetObject, null);
	}

	public void upsert(
		BaseJiraAssetObjectConverter converter, JiraAssetObject jiraAssetObject,
		BiPredicate<JiraAssetObject, JiraAssetObject>
			shouldSkipUpdateBiPredicate) {

		String externalKeyAttributeName =
			converter.getExternalKeyAttributeName();

		String externalKey = jiraAssetObject.getAttributeValue(
			externalKeyAttributeName);

		if (Validator.isNull(externalKey)) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Unable to upsert a ", converter.getObjectTypeName(),
						" asset object with no \"", externalKeyAttributeName,
						"\" value"));
			}

			return;
		}

		_keyedLock.withLock(
			_getLockKey(converter, externalKey),
			() -> _upsert(
				converter, externalKey, jiraAssetObject,
				shouldSkipUpdateBiPredicate));
	}

	private String _createObject(
		BaseJiraAssetObjectConverter converter, String externalKey,
		Function<String, JiraAssetObject> createAssetObjectFunction) {

		return _keyedLock.withLock(
			_getLockKey(converter, externalKey),
			() -> {
				String objectId = fetchReferenceObjectId(
					converter, externalKey);

				if (objectId != null) {
					return objectId;
				}

				JiraAssetObject jiraAssetObject =
					createAssetObjectFunction.apply(externalKey);

				if (jiraAssetObject == null) {
					if (_log.isWarnEnabled()) {
						_log.warn(
							StringBundler.concat(
								"Unable to create ",
								converter.getObjectTypeName(),
								" asset object for external key ",
								externalKey));
					}

					return null;
				}

				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Creating ", converter.getObjectTypeName(),
							" asset object for unresolved external key ",
							externalKey));
				}

				try {
					JSONObject jsonObject = _jiraAssetPersistence.createObject(
						converter.getObjectTypeId(), jiraAssetObject);

					return jsonObject.optString("id", null);
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to create ", converter.getObjectTypeName(),
							" asset object for external key ", externalKey),
						exception);

					return null;
				}
			});
	}

	private void _delete(
		BaseJiraAssetObjectConverter converter, String externalKey) {

		List<JiraAssetObject> jiraAssetObjects = getJiraAssetObjects(
			converter,
			aqlBuilder -> aqlBuilder.andEquals(
				externalKey, converter.getExternalKeyAttributeName()));

		if (jiraAssetObjects.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Skipping delete of ", converter.getObjectTypeName(),
						" asset object for external key ", externalKey,
						" because it does not exist"));
			}

			return;
		}

		JiraAssetObject jiraAssetObject = jiraAssetObjects.get(0);

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Deleting ", converter.getObjectTypeName(),
					" asset object for external key ", externalKey));
		}

		_jiraAssetPersistence.deleteObject(jiraAssetObject.getObjectId());
	}

	private Map<String, String> _getExternalKeyToObjectIdMap(
		BaseJiraAssetObjectConverter converter, List<String> externalKeys) {

		Map<String, String> externalKeyToObjectIdMap = new HashMap<>();

		if (ListUtil.isEmpty(externalKeys)) {
			return externalKeyToObjectIdMap;
		}

		Set<String> externalKeySet = new HashSet<>(externalKeys);

		externalKeys = new ArrayList<>(externalKeySet);

		for (int i = 0; i < externalKeys.size(); i += _CHUNK_SIZE) {
			List<String> externalKeys1 = externalKeys.subList(
				i, Math.min(i + _CHUNK_SIZE, externalKeys.size()));

			String externalKeyAttributeName =
				converter.getExternalKeyAttributeName();

			List<JiraAssetObject> jiraAssetObjects = getJiraAssetObjects(
				converter,
				aqlBuilder -> aqlBuilder.andIn(
					externalKeys1, externalKeyAttributeName));

			for (JiraAssetObject jiraAssetObject : jiraAssetObjects) {
				String externalKey = jiraAssetObject.getAttributeValue(
					externalKeyAttributeName);
				String objectId = jiraAssetObject.getObjectId();

				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Resolved external key ", externalKey,
							" to existing ", converter.getObjectTypeName(),
							" asset object ", objectId));
				}

				String previousObjectId = externalKeyToObjectIdMap.putIfAbsent(
					externalKey, objectId);

				if ((previousObjectId != null) &&
					!Objects.equals(previousObjectId, objectId) &&
					_log.isWarnEnabled()) {

					_log.warn(
						StringBundler.concat(
							"Multiple asset objects share external key ",
							externalKey, ": ", previousObjectId, " and ",
							objectId,
							"; the reference will resolve to the first match ",
							previousObjectId));
				}
			}
		}

		return externalKeyToObjectIdMap;
	}

	private String _getLockKey(
		BaseJiraAssetObjectConverter converter, String externalKey) {

		return converter.getObjectTypeName() + "#" + externalKey;
	}

	private List<String> _resolveToObjectIds(
		BaseJiraAssetObjectConverter converter, Collection<String> externalKeys,
		Function<String, JiraAssetObject> createAssetObjectFunction) {

		List<String> resolvedObjectIds = new ArrayList<>();

		Set<String> uniqueExternalKeys = new LinkedHashSet<>();

		for (String externalKey : externalKeys) {
			if (Validator.isNotNull(externalKey)) {
				uniqueExternalKeys.add(externalKey);
			}
		}

		if (uniqueExternalKeys.isEmpty()) {
			return resolvedObjectIds;
		}

		List<String> uniqueExternalKeysList = new ArrayList<>(
			uniqueExternalKeys);

		Map<String, String> externalKeyToObjectIdMap =
			_getExternalKeyToObjectIdMap(converter, uniqueExternalKeysList);

		for (String externalKey : uniqueExternalKeysList) {
			String objectId = externalKeyToObjectIdMap.get(externalKey);

			if ((objectId == null) && (createAssetObjectFunction != null)) {
				objectId = _createObject(
					converter, externalKey, createAssetObjectFunction);
			}

			if (objectId == null) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Unable to resolve external key ", externalKey,
							" to a ", converter.getObjectTypeName(),
							" asset object"));
				}

				continue;
			}

			resolvedObjectIds.add(objectId);
		}

		return resolvedObjectIds;
	}

	private void _softDelete(
		BaseJiraAssetObjectConverter converter, String deletedAttributeName,
		String externalKey, JiraAssetObject jiraAssetObject,
		Predicate<JiraAssetObject> shouldSkipUpdatePredicate) {

		JiraAssetObject existingJiraAssetObject = jiraAssetObject;

		if (shouldSkipUpdatePredicate != null) {
			List<JiraAssetObject> jiraAssetObjects = getJiraAssetObjects(
				converter,
				aqlBuilder -> aqlBuilder.andEquals(
					externalKey, converter.getExternalKeyAttributeName()));

			if (jiraAssetObjects.isEmpty()) {
				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Skipping soft delete of ",
							converter.getObjectTypeName(),
							" asset object for external key ", externalKey,
							" because it does not exist"));
				}

				return;
			}

			existingJiraAssetObject = jiraAssetObjects.get(0);

			if (shouldSkipUpdatePredicate.test(existingJiraAssetObject)) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						StringBundler.concat(
							"Skipping soft delete of recently updated ",
							converter.getObjectTypeName(),
							" asset object for external key ", externalKey));
				}

				return;
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Soft deleting ", converter.getObjectTypeName(),
					" asset object ", existingJiraAssetObject.getObjectId()));
		}

		JiraAssetObject patchJiraAssetObject =
			converter.createJiraAssetObject();

		patchJiraAssetObject.setAttributeValue(deletedAttributeName, true);
		patchJiraAssetObject.setAttributeValue(
			converter.getExternalUpdatedAtAttributeName(),
			converter.formatDate(new Date()));

		_jiraAssetPersistence.updateObject(
			existingJiraAssetObject.getObjectId(), patchJiraAssetObject);
	}

	private void _upsert(
		BaseJiraAssetObjectConverter converter, String externalKey,
		JiraAssetObject jiraAssetObject,
		BiPredicate<JiraAssetObject, JiraAssetObject>
			shouldSkipUpdateBiPredicate) {

		List<JiraAssetObject> jiraAssetObjects = getJiraAssetObjects(
			converter,
			aqlBuilder -> aqlBuilder.andEquals(
				externalKey, converter.getExternalKeyAttributeName()));

		if (jiraAssetObjects.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Creating ", converter.getObjectTypeName(),
						" asset object for external key ", externalKey));
			}

			_jiraAssetPersistence.createObject(
				converter.getObjectTypeId(), jiraAssetObject);

			return;
		}

		JiraAssetObject existingJiraAssetObject = jiraAssetObjects.get(0);

		if ((shouldSkipUpdateBiPredicate != null) &&
			shouldSkipUpdateBiPredicate.test(
				existingJiraAssetObject, jiraAssetObject)) {

			if (_log.isDebugEnabled()) {
				_log.debug(
					StringBundler.concat(
						"Skipping unchanged ", converter.getObjectTypeName(),
						" asset object for external key ", externalKey));
			}

			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Updating ", converter.getObjectTypeName(),
					" asset object for external key ", externalKey));
		}

		_jiraAssetPersistence.updateObject(
			existingJiraAssetObject.getObjectId(), jiraAssetObject);
	}

	private static final int _CHUNK_SIZE = 50;

	private static final Log _log = LogFactory.getLog(JiraAssetService.class);

	@Autowired
	private JiraAssetPersistence _jiraAssetPersistence;

	// Deliberately not the shared KeyedLock component: the synchronizers
	// hold entity-level locks from that component around calls into this
	// class, and nesting two lock levels on one striped pool can deadlock
	// when unrelated keys hash to each other's stripes.

	private final KeyedLock _keyedLock = new KeyedLock();

}