/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {filesize} from 'filesize';
import {UploadedFile} from '~/components/FileList/FileList';
import Form from '~/components/MarketplaceForm/MarketplaceForm';
import MultiSelect from '~/components/MultiSelect/MultiSelect';
import Select from '~/components/Select/Select';
import UploadLogo from '~/components/UploadLogo/UploadLogo';
import {NewAppTypes, useNewAppContext} from '~/context/NewAppContextProvider';
import {ProductVocabulary, ProductWorkflowStatusCode} from '~/enums/Product';
import i18n from '~/i18n';
import {ProductTags} from '~/utils/productUtils';
import {getRandomID} from '~/utils/stringUtils';

import type {VocabularyCategoryOption} from '~/hooks/useGetVocabulariesAndCategories';

const tooltipInfo = {
	areas: i18n.translate(
		'select-the-areas-of-liferay-your-app-extends-such-as-analytics-content-management-or-commerce-areas-help-customers-browsing-the-marketplace-by-capability-find-your-app'
	),
	categories: i18n.translate(
		'choose-the-marketplace-category-that-most-accurately-describes-what-your-app-does-users-looking-for-specific-types-of-apps-will-often-browse-categories-by-searching-for-a-specific-category-name-on-the-main-marketplace-home-page-having-your-app-listed-under-the-appropriate-category-will-help-them-find-it'
	),
	description: i18n.translate(
		'you-can-put-anything-you-want-here-but-a-good-guideline-is-no-more-than-4-5-paragraphs-this-field-does-not-allow-any-markup-tags-its-just-text-please-do-not-use-misleading-names-information-or-icons-descriptions-should-be-as-concise-as-possible-ensure-your-icons-images-descriptions-and-tags-are-free-of-profanity-or-other-offensive-material'
	),
	name: i18n.translate(
		'customers-of-the-marketplace-will-see-this-as-the-name-of-the-app-please-use-a-title-no-longer-than-50-characters-titles-longer-than-18-characters-may-be-truncated-the-app-title-may-contain-the-word-liferay-to-describe-its-use-or-intent-as-long-as-the-name-does-not-imply-official-certification-or-validation-from-liferay-inc-examples-of-permissible-names-include-exchange-connector-for-liferay-or-integration-connector-kit-for-liferay-while-liferay-mail-solution-or-liferay-management-console-would-not-be-permitted-without-explicit-approval-please-refer-to-our-trademark-policy'
	),
	tags: i18n.translate(
		'tags-help-to-describe-your-app-in-the-marketplace-select-the-tags-most-relevant-to-your-app-they-can-be-changed-if-needed'
	),
};

const Profile = () => {
	const [
		{
			_product,
			profile: {areas, categories, description, file, name, tags},
			references: {
				flags: {canModifyProductProfileCategory},
				vocabulariesAndCategories,
			},
		},
		dispatch,
	] = useNewAppContext();

	const defaultSourceItems = {
		areas:
			vocabulariesAndCategories[ProductVocabulary.APP_AREA]?.categories ??
			vocabulariesAndCategories['App Area']?.categories ??
			[],
		categories:
			vocabulariesAndCategories[ProductVocabulary.APP_CATEGORY]
				?.categories ??
			vocabulariesAndCategories['App Category']?.categories ??
			[],
		tags:
			vocabulariesAndCategories[ProductVocabulary.APP_TAGS]?.categories ??
			vocabulariesAndCategories['App Tags']?.categories ??
			[],
	};

	const onChange = (event: {target: {name: string; value: unknown}}) => {
		dispatch({
			payload: {[event.target.name]: event.target.value},
			type: NewAppTypes.SET_PROFILE,
		});
	};

	const handleLogoUpload = (files: FileList) => {
		const _file = files[0];

		const newUploadedFile: UploadedFile = {
			changed: true,
			error: false,
			file: _file,
			fileName: _file.name,
			id: getRandomID(),
			preview: URL.createObjectURL(_file),
			progress: 0,
			readableSize: filesize(_file.size),
			tags: [ProductTags.APP_ICON],
			uploaded: true,
		};

		if (file?.id) {
			dispatch({
				payload: file.id,
				type: NewAppTypes.SET_DELETE_IMAGE,
			});
		}

		dispatch({
			payload: {
				file: newUploadedFile,
			},
			type: NewAppTypes.SET_PROFILE,
		});
	};

	const handleDelete = async (id: string) => {
		dispatch({
			payload: id,
			type: NewAppTypes.SET_DELETE_IMAGE,
		});

		dispatch({
			payload: {
				file: undefined,
			},
			type: NewAppTypes.SET_PROFILE,
		});
	};

	const getFilteredItems = (
		selectedItems: {[key: string]: string}[],
		defaultItems: {[key: string]: string}[]
	) =>
		defaultItems?.filter(
			(defaultCategory) =>
				!selectedItems?.some(
					(category) => defaultCategory.value === category.value
				)
		);

	return (
		<div className="new-app-form-profile">
			<h5>App Info</h5>
			<hr />
			<div className="align-items-center d-flex mt-5">
				<UploadLogo
					onDeleteFile={handleDelete}
					onUpload={handleLogoUpload}
					uploadedFile={file}
				/>
			</div>
			<Form.FormControl>
				<Form.Label
					className="mt-5"
					htmlFor="name"
					info={tooltipInfo.name}
					required
				>
					{i18n.translate('name')}
				</Form.Label>

				<Form.Input
					maxLength={50}
					name="name"
					onChange={onChange}
					placeholder="Enter app name"
					value={name}
				/>
			</Form.FormControl>
			<Form.FormControl>
				<Form.Label
					className="mt-5"
					htmlFor="description"
					info={tooltipInfo.description}
					required
				>
					{i18n.translate('description')}
				</Form.Label>

				<Form.Input
					component="textarea"
					maxLength={2000}
					name="description"
					onChange={onChange}
					placeholder="Enter app description"
					type="textarea"
					value={description}
				/>
			</Form.FormControl>

			<div className="form-multiselect">
				<Form.FormControl>
					<Form.Label
						className="mt-5"
						htmlFor="categories"
						info={tooltipInfo.categories}
						required
					>
						{i18n.translate('category')}
					</Form.Label>

					<Select
						aria-label={i18n.translate('categories')}
						className={categories?.value || 'select-empty-value'}
						defaultOption
						defaultOptionLabel={i18n.translate('select-category')}
						disabled={
							!canModifyProductProfileCategory &&
							!!_product?.productId &&
							_product.productStatus !==
								ProductWorkflowStatusCode.DRAFT
						}
						name="category"
						onChange={(event) => {
							const category = defaultSourceItems.categories.find(
								(defaultCategory: VocabularyCategoryOption) =>
									defaultCategory.value === event.target.value
							);
							onChange({
								target: {
									name: 'categories',
									value: {
										label: category?.label ?? '',
										value: event.target.value,
									},
								},
							});
						}}
						options={defaultSourceItems.categories.map(
							(category: {label: string; value: string}) => ({
								key: category.value,
								name: category.label,
							})
						)}
						required
						value={categories?.value || ''}
					/>
				</Form.FormControl>

				<Form.FormControl>
					<Form.Label
						htmlFor="areas"
						info={tooltipInfo.areas}
						required
					>
						{i18n.translate('area')}
					</Form.Label>

					<MultiSelect
						ariaLabel={i18n.translate('area')}
						inputName="area"
						key={`areas-${areas.length}`}
						multiselectKey={`area-${
							getFilteredItems(areas, defaultSourceItems?.areas)
								.length
						}`}
						onItemsChange={(items) => {
							const filteredValue = (
								items as {[key: string]: string}[]
							).filter((item) =>
								defaultSourceItems.areas.some(
									(defaultItem: VocabularyCategoryOption) =>
										defaultItem.value === item.value
								)
							);

							onChange({
								target: {name: 'areas', value: filteredValue},
							});
						}}
						placeholder={i18n.translate('select-areas')}
						required
						selectedItems={areas}
						sourceItems={getFilteredItems(
							areas,
							defaultSourceItems?.areas
						)}
					/>
				</Form.FormControl>

				<Form.FormControl>
					<Form.Label htmlFor="tags" info={tooltipInfo.tags} required>
						{i18n.translate('tags')}
					</Form.Label>

					<MultiSelect
						ariaLabel={i18n.translate('tags')}
						inputName="tags-selector"
						key={`tags-${tags.length}`}
						multiselectKey={`tag-${
							getFilteredItems(tags, defaultSourceItems?.tags)
								.length
						}`}
						onItemsChange={(items) => {
							const filteredValue = (
								items as {[key: string]: string}[]
							).filter((item) =>
								defaultSourceItems.tags.some(
									(defaultItem: VocabularyCategoryOption) =>
										defaultItem.value === item.value
								)
							);
							onChange({
								target: {name: 'tags', value: filteredValue},
							});
						}}
						placeholder={i18n.translate('select-tags')}
						required
						selectedItems={tags}
						sourceItems={getFilteredItems(
							tags,
							defaultSourceItems?.tags
						)}
					/>
				</Form.FormControl>
			</div>
		</div>
	);
};

export default Profile;
