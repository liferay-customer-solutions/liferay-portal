/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import {ClayCheckbox, ClaySelect} from '@clayui/form';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {zodResolver} from '@hookform/resolvers/zod';
import {useEffect, useMemo, useState} from 'react';
import {useFieldArray, useForm} from 'react-hook-form';
import Button from '~/components/Button/Button';
import {Input} from '~/components/Input/Input';
import useDXPProductVersions from '~/hooks/useDXPProductVersions';
import useListTypeDefinition from '~/hooks/useListTypeDefinition';
import {Word, translate} from '~/i18n';
import projectSchemas, {requiredSelectSchema} from '~/schema/projectSchemas';
import FetcherError from '~/services/fetcher/FetcherError';
import Cloud from '~/services/spring-boot/Cloud';

import useHasDisasterRecoveryEntitlement from '../../hooks/useHasDisasterRecoveryEntitlement';
import {
	CloudActivationAdminFieldName,
	CloudActivationField,
	CloudActivationProfile,
	getCloudActivationAdminFields,
	getCloudActivationFields,
} from '../../utils/cloudActivationFields';

type FormAdmin = {
	emailAddress: string;
	firstName: string;
	githubUsername: string;
	lastName: string;
	name: string;
};

type FormFields = {
	admins: FormAdmin[];
	allowedEmailDomains: string;
	analyticsCloudOwnerEmailAddress: string;
	disasterRecoveryRegion: string;
	dxpVersion: string;
	friendlyURL: string;
	ownerEmailAddress: string;
	projectId: string;
	region: string;
	timeZone: string;
	workspaceName: string;
};

const ANALYTICS_CLOUD_TERMS_AND_CONDITIONS_URL =
	'https://www.liferay.com/documents/d/guest/1012410';

const DISASTER_RECOVERY_DEFAULT_INDEX_BY_PROFILE: Partial<
	Record<CloudActivationProfile, number>
> = {
	'analytics-cloud': 0,
	'paas': 1,
};

const PRIMARY_REGION_DEFAULT_INDEX_BY_PROFILE: Partial<
	Record<CloudActivationProfile, number>
> = {
	'analytics-cloud': 0,
	'paas': 0,
	'saas': 0,
};

const SAAS_PRIMARY_REGION_LIST_TYPE_DEFINITION_ERC =
	'LT_ENVIRONMENT_PRIMARY_REGION';

const SCHEMA_BY_PROFILE = {
	'analytics-cloud': projectSchemas.cloudActivationAnalyticsCloud,
	'paas': projectSchemas.cloudActivationPaaS,
	'saas': projectSchemas.cloudActivationSaaS,
};

const SUBMIT_ERROR_MESSAGE_KEYS: Record<number, Word> = {
	403: 'you-need-administrator-role-on-this-project-to-submit-this-form',
	422: 'this-project-does-not-have-an-active-subscription-for-this-product-contact-your-liferay-sales-representative',
};

function getSchema(
	profile: CloudActivationProfile,
	hasDisasterRecoveryEntitlement: boolean
) {
	const schema = SCHEMA_BY_PROFILE[profile];

	if (profile === 'saas' || !hasDisasterRecoveryEntitlement) {
		return schema;
	}

	return schema.extend({disasterRecoveryRegion: requiredSelectSchema});
}

function buildEmptyAdmin(): FormAdmin {
	return {
		emailAddress: '',
		firstName: '',
		githubUsername: '',
		lastName: '',
		name: '',
	};
}

function buildDefaultValues(
	profile: CloudActivationProfile
): Partial<FormFields> {
	const defaultValues: Record<string, FormAdmin[] | string> = {};

	getCloudActivationFields(profile).forEach((field) => {
		if (field.kind === 'admins') {
			defaultValues[field.environmentField] = [buildEmptyAdmin()];
		}
		else {
			defaultValues[field.environmentField] = '';
		}
	});

	return defaultValues as Partial<FormFields>;
}

function toAdmins(
	profile: CloudActivationProfile,
	admins: FormAdmin[]
): Record<string, string>[] {
	if (profile === 'paas') {
		return admins.map((admin) => ({
			emailAddress: admin.emailAddress,
			firstName: admin.firstName,
			githubUsername: admin.githubUsername,
			lastName: admin.lastName,
		}));
	}

	return admins.map((admin) => {
		const [firstName, ...lastNameParts] = admin.name.trim().split(' ');

		return {
			emailAddress: admin.emailAddress,
			firstName,
			lastName: lastNameParts.join(' '),
		};
	});
}

function toFields(
	profile: CloudActivationProfile,
	values: FormFields
): Record<string, unknown> {
	if (profile === 'analytics-cloud') {
		return {
			allowedEmailDomains: values.allowedEmailDomains,
			disasterRecoveryRegion: values.disasterRecoveryRegion,
			friendlyURL: values.friendlyURL,
			ownerEmailAddress: values.ownerEmailAddress,
			region: values.region,
			timeZone: values.timeZone,
			workspaceName: values.workspaceName,
		};
	}

	if (profile === 'paas') {
		return {
			admins: toAdmins(profile, values.admins),
			disasterRecoveryRegion: values.disasterRecoveryRegion,
			dxpVersion: values.dxpVersion,
			projectId: values.projectId,
			region: values.region,
		};
	}

	return {
		admins: toAdmins(profile, values.admins),
		analyticsCloudOwnerEmailAddress: values.analyticsCloudOwnerEmailAddress,
		projectId: values.projectId,
		region: values.region,
	};
}

function toErrorMessageKey(error: unknown): Word {
	if (error instanceof FetcherError && error.status) {
		return (
			SUBMIT_ERROR_MESSAGE_KEYS[error.status] ??
			'an-unexpected-error-occurred'
		);
	}

	return 'an-unexpected-error-occurred';
}

type CloudActivationFormProps = {
	onAlreadySubmitted: () => void;
	onSuccess: () => void;
	profile: CloudActivationProfile;
	projectExternalReferenceCode: string;
	projectName: string;
};

export default function CloudActivationForm({
	onAlreadySubmitted,
	onSuccess,
	profile,
	projectExternalReferenceCode,
	projectName,
}: CloudActivationFormProps) {
	const [termsAccepted, setTermsAccepted] = useState(false);

	const allFields = useMemo(
		() => getCloudActivationFields(profile),
		[profile]
	);

	const {hasDisasterRecoveryEntitlement, loading: entitlementLoading} =
		useHasDisasterRecoveryEntitlement(projectExternalReferenceCode);

	const {loading: dxpVersionsLoading, productVersions} =
		useDXPProductVersions(profile === 'paas');

	const {data: saasPrimaryRegionListTypeDefinition} = useListTypeDefinition(
		profile === 'saas' ? SAAS_PRIMARY_REGION_LIST_TYPE_DEFINITION_ERC : null
	);

	const saasRegionOptions = useMemo(
		() =>
			(saasPrimaryRegionListTypeDefinition?.listTypeEntries ?? [])
				.map((listTypeEntry) => ({
					label: listTypeEntry.name,
					value: listTypeEntry.name,
				}))
				.sort((option1, option2) =>
					option1.label.localeCompare(option2.label)
				),
		[saasPrimaryRegionListTypeDefinition]
	);

	const fields = useMemo(
		() =>
			hasDisasterRecoveryEntitlement
				? allFields
				: allFields.filter(
						(field) =>
							field.environmentField !== 'disasterRecoveryRegion'
					),
		[allFields, hasDisasterRecoveryEntitlement]
	);

	const disasterRecoveryRegionOptions =
		allFields.find(
			(field) => field.environmentField === 'disasterRecoveryRegion'
		)?.options ?? [];

	const regionOptions =
		profile === 'saas'
			? saasRegionOptions
			: allFields.find((field) => field.environmentField === 'region')
					?.options ?? [];

	const {
		control,
		formState: {errors, isSubmitting, isValid},
		getValues,
		handleSubmit,
		register,
		setError,
		setValue,
		watch,
	} = useForm<FormFields>({
		defaultValues: buildDefaultValues(profile),
		mode: 'onChange',
		resolver: zodResolver(
			getSchema(profile, hasDisasterRecoveryEntitlement)
		),
	});

	const {
		append,
		fields: adminFields,
		remove,
	} = useFieldArray({control, name: 'admins'});

	const adminInputFields = useMemo(
		() => getCloudActivationAdminFields(profile),
		[profile]
	);

	const watchedRegion = watch('region');
	const watchedDisasterRecoveryRegion = watch('disasterRecoveryRegion');

	useEffect(() => {
		if (
			profile !== 'paas' ||
			dxpVersionsLoading ||
			!productVersions.length
		) {
			return;
		}

		if (!getValues('dxpVersion')) {
			setValue('dxpVersion', productVersions[0], {shouldValidate: true});
		}
	}, [dxpVersionsLoading, productVersions, profile, getValues, setValue]);

	useEffect(() => {
		const defaultIndex = PRIMARY_REGION_DEFAULT_INDEX_BY_PROFILE[profile];

		if (defaultIndex === undefined || getValues('region')) {
			return;
		}

		const defaultOption = regionOptions[defaultIndex];

		if (defaultOption) {
			setValue('region', defaultOption.value, {shouldValidate: true});
		}
	}, [getValues, profile, regionOptions, setValue]);

	useEffect(() => {
		if (entitlementLoading || !hasDisasterRecoveryEntitlement) {
			return;
		}

		if (getValues('disasterRecoveryRegion')) {
			return;
		}

		const defaultIndex =
			DISASTER_RECOVERY_DEFAULT_INDEX_BY_PROFILE[profile];

		if (defaultIndex === undefined) {
			return;
		}

		const indexOption = disasterRecoveryRegionOptions[defaultIndex];

		const defaultOption =
			profile === 'paas' && indexOption?.value === watchedRegion
				? disasterRecoveryRegionOptions.find(
						(option) => option.value !== watchedRegion
					)
				: indexOption;

		if (defaultOption) {
			setValue('disasterRecoveryRegion', defaultOption.value, {
				shouldValidate: true,
			});
		}
	}, [
		disasterRecoveryRegionOptions,
		entitlementLoading,
		getValues,
		hasDisasterRecoveryEntitlement,
		profile,
		setValue,
		watchedRegion,
	]);

	const onSubmit = async (values: FormFields) => {
		try {
			await Cloud.postEnvironmentsActivationRequest(
				profile,
				toFields(profile, values),
				projectExternalReferenceCode
			);

			onSuccess();
		}
		catch (error) {
			if (error instanceof FetcherError && error.status === 409) {
				onAlreadySubmitted();

				return;
			}

			setError('root', {message: toErrorMessageKey(error)});
		}
	};

	const rootErrorMessageKey = errors.root?.message as Word | undefined;

	const termsAcceptanceLabel = translate(
		'by-checking-this-box-and-clicking-next-below-i-as-an-authorized-representative-of-x-acknowledge-that-x-accepts-the-x-terms-and-conditions-and-privacy-policy-x-these-terms-will-govern-x-s-use-of-liferay-analytics-cloud-unless-x-has-entered-into-a-separate-agreement-with-liferay-that-governs-x-s-use-of-liferay-analytics-cloud'
	)
		.split('{0}')
		.join(projectName);

	const termsAcceptanceLabelParts = termsAcceptanceLabel.split(/\{1\}|\{2\}/);

	const hasTermsAcceptanceLinkPlacement =
		termsAcceptanceLabelParts.length === 3 &&
		!!termsAcceptanceLabelParts[1].trim();

	const termsAcceptanceFallbackText = termsAcceptanceLabel
		.replace(/\{1\}|\{2\}/g, ' ')
		.replace(/\s+/g, ' ')
		.trim();

	const getAdminErrorMessage = (
		index: number,
		name: CloudActivationAdminFieldName
	) => errors.admins?.[index]?.[name]?.message as string | undefined;

	const renderAdmins = (field: CloudActivationField) => (
		<div className="form-group" key={field.environmentField}>
			<label className="ml-0">{translate(field.label)}</label>

			{adminFields.map((adminField, index) => (
				<div
					className={index ? 'border-top mt-3 pt-3' : ''}
					key={adminField.id}
				>
					{adminInputFields.map((adminInputField) => (
						<Input
							{...register(
								`admins.${index}.${adminInputField.name}`
							)}
							errorMessage={getAdminErrorMessage(
								index,
								adminInputField.name
							)}
							key={adminInputField.name}
							label={translate(adminInputField.label)}
							required={adminInputField.required}
							type={
								adminInputField.kind === 'email'
									? 'email'
									: 'text'
							}
						/>
					))}
				</div>
			))}

			{adminFields.length > 1 && (
				<Button
					className="ml-0 my-2"
					displayType="secondary"
					onClick={() => remove(adminFields.length - 1)}
					prependIcon="hr"
					small
					type="button"
				>
					{translate('remove-project-admin')}
				</Button>
			)}

			<Button
				className="ml-0 my-2"
				displayType="secondary"
				onClick={() => append(buildEmptyAdmin())}
				prependIcon="plus"
				small
				type="button"
			>
				{translate('add-another-admin')}
			</Button>
		</div>
	);

	const renderField = (field: CloudActivationField) => {
		if (field.kind === 'admins') {
			return renderAdmins(field);
		}

		const errorMessage = errors[field.environmentField as keyof FormFields]
			?.message as string | undefined;

		if (field.kind === 'select') {
			let disabledOptionValue: string | undefined;
			let options = field.options ?? [];

			if (field.environmentField === 'dxpVersion') {
				options = productVersions.map((version) => ({
					label: version,
					value: version,
				}));
			}
			else if (
				profile === 'saas' &&
				field.environmentField === 'region'
			) {
				options = saasRegionOptions;
			}
			else if (
				profile === 'paas' &&
				field.environmentField === 'region'
			) {
				disabledOptionValue = watchedDisasterRecoveryRegion;
			}
			else if (
				profile === 'paas' &&
				field.environmentField === 'disasterRecoveryRegion'
			) {
				disabledOptionValue = watchedRegion;
			}

			const translateOptionLabels =
				field.environmentField !== 'dxpVersion' && profile !== 'saas';

			return (
				<div
					className={`form-group${errorMessage ? ' has-error' : ''}`}
					key={field.environmentField}
				>
					<label className="ml-0" htmlFor={field.environmentField}>
						{translate(field.label)}
					</label>

					<ClaySelect
						id={field.environmentField}
						{...register(
							field.environmentField as keyof FormFields
						)}
					>
						<ClaySelect.Option label="" value="" />

						{options.map((option) => (
							<ClaySelect.Option
								disabled={option.value === disabledOptionValue}
								key={option.value}
								label={
									translateOptionLabels
										? translate(option.label as Word)
										: option.label
								}
								value={option.value}
							/>
						))}
					</ClaySelect>

					{!!errorMessage && (
						<div className="field-base-feedback text-danger">
							{errorMessage}
						</div>
					)}
				</div>
			);
		}

		return (
			<Input
				{...register(field.environmentField as keyof FormFields)}
				errorMessage={errorMessage}
				helpMessage={
					field.helpMessage ? translate(field.helpMessage) : undefined
				}
				key={field.environmentField}
				label={translate(field.label)}
				placeholder={field.placeholder}
				required={field.required}
				type={field.kind === 'email' ? 'email' : 'text'}
			/>
		);
	};

	return (
		<form onSubmit={handleSubmit(onSubmit)}>
			{fields.map(renderField)}

			{profile === 'analytics-cloud' && (
				<div className="align-items-start d-flex mt-4">
					<ClayCheckbox
						checked={termsAccepted}
						className="mr-2"
						id="analytics-cloud-terms-acceptance"
						onChange={() =>
							setTermsAccepted((accepted) => !accepted)
						}
					/>

					<label
						className="font-weight-normal ml-1 text-4"
						htmlFor="analytics-cloud-terms-acceptance"
					>
						{hasTermsAcceptanceLinkPlacement ? (
							<>
								{termsAcceptanceLabelParts[0]}

								<a
									href={
										ANALYTICS_CLOUD_TERMS_AND_CONDITIONS_URL
									}
									rel="noreferrer noopener"
									target="_blank"
								>
									{termsAcceptanceLabelParts[1]}
								</a>

								{termsAcceptanceLabelParts[2]}
							</>
						) : (
							<>
								{termsAcceptanceFallbackText}{' '}
								<a
									href={
										ANALYTICS_CLOUD_TERMS_AND_CONDITIONS_URL
									}
									rel="noreferrer noopener"
									target="_blank"
								>
									{translate('terms')}
								</a>
							</>
						)}
					</label>
				</div>
			)}

			{!!rootErrorMessageKey && (
				<ClayAlert className="mt-3" displayType="danger" role={null}>
					{translate(rootErrorMessageKey)}
				</ClayAlert>
			)}

			<div className="d-flex justify-content-end mt-4">
				<Button
					disabled={
						!isValid ||
						isSubmitting ||
						(profile === 'analytics-cloud' && !termsAccepted)
					}
					type="submit"
				>
					<div className="align-items-center d-flex">
						{isSubmitting && (
							<ClayLoadingIndicator className="mr-3 my-0" />
						)}

						{translate('submit')}
					</div>
				</Button>
			</div>
		</form>
	);
}
