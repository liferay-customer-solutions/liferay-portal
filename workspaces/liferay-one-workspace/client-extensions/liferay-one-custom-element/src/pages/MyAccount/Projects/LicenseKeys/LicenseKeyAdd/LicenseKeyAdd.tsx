/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {zodResolver} from '@hookform/resolvers/zod';
import {useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {Input} from '~/components/Input/Input';
import Select from '~/components/Select/Select';
import {ProjectProduct} from '~/hooks/useProjectCommerce';
import {translate} from '~/i18n';
import commerceSchemas from '~/schema/commerceSchemas';
import LicenseKeys from '~/services/objects/LicenseKeys';

const CLUSTER_KEY_TYPES = ['cluster', 'developer-cluster', 'virtual-cluster'];

const HOST_KEY_TYPES = ['enterprise', 'oem', 'production'];

const KEY_TYPE_OPTIONS = [
	{key: 'production', name: 'Production'},
	{key: 'cluster', name: 'Cluster'},
	{key: 'virtual-cluster', name: 'Virtual Cluster'},
	{key: 'developer', name: 'Developer'},
	{key: 'developer-cluster', name: 'Developer Cluster'},
	{key: 'enterprise', name: 'Enterprise'},
	{key: 'oem', name: 'OEM'},
];

const SIZING_OPTIONS = [
	{key: 'small', name: 'Small'},
	{key: 'medium', name: 'Medium'},
	{key: 'large', name: 'Large'},
];

type NewKeyFormData = {
	clusterSize?: string;
	description?: string;
	domains: string;
	environmentName: string;
	expirationDate?: string;
	hostName?: string;
	keyType: string;
	startDate: string;
};

type LicenseKeyAddProps = {
	accountId?: number | string;
	initialIncludedExternalReferenceCodes?: string[];
	initialSizing?: Record<string, string>;
	initialValues?: Partial<NewKeyFormData>;
	onClose: () => void;
	onGenerated: () => void;
	products: ProjectProduct[];
	projectExternalReferenceCode?: string;
};

function toISODate(value?: string): string | undefined {
	return value ? new Date(value).toISOString() : undefined;
}

export default function LicenseKeyAdd({
	accountId,
	initialIncludedExternalReferenceCodes,
	initialSizing,
	initialValues,
	onClose,
	onGenerated,
	products,
	projectExternalReferenceCode,
}: LicenseKeyAddProps) {
	const {
		formState: {errors},
		getValues,
		register,
		setValue,
		trigger,
		watch,
	} = useForm<NewKeyFormData>({
		defaultValues: {
			clusterSize: '',
			description: '',
			domains: '',
			environmentName: '',
			expirationDate: '',
			hostName: '',
			keyType: '',
			startDate: '',
			...initialValues,
		},
		mode: 'onChange',
		resolver: zodResolver(commerceSchemas.newLicenseKey),
	});

	const [includedExternalReferenceCodes, setIncludedExternalReferenceCodes] =
		useState<string[]>(
			() =>
				initialIncludedExternalReferenceCodes ??
				products.map((product) => product.externalReferenceCode)
		);
	const [sizingByExternalReferenceCode, setSizingByExternalReferenceCode] =
		useState<Record<string, string>>(() => initialSizing ?? {});
	const [stepIndex, setStepIndex] = useState(0);
	const [submitError, setSubmitError] = useState('');
	const [submitting, setSubmitting] = useState(false);

	const includedProducts = useMemo(
		() =>
			products.filter((product) =>
				includedExternalReferenceCodes.includes(
					product.externalReferenceCode
				)
			),
		[includedExternalReferenceCodes, products]
	);

	const steps = useMemo(() => {
		const productSteps = includedProducts.map(
			(product) => `product:${product.externalReferenceCode}`
		);

		return products.length
			? ['environment', 'products', ...productSteps, 'review']
			: ['environment', 'review'];
	}, [includedProducts, products.length]);

	const currentStep = steps[Math.min(stepIndex, steps.length - 1)];

	const keyType = watch('keyType');

	function toggleProduct(externalReferenceCode: string) {
		setIncludedExternalReferenceCodes((previous) =>
			previous.includes(externalReferenceCode)
				? previous.filter((code) => code !== externalReferenceCode)
				: [...previous, externalReferenceCode]
		);
	}

	async function handleNext() {
		if (currentStep === 'environment') {
			const valid = await trigger([
				'domains',
				'environmentName',
				'keyType',
				'startDate',
			]);

			if (!valid) {
				return;
			}
		}

		if (currentStep.startsWith('product:')) {
			const externalReferenceCode = currentStep.slice('product:'.length);

			if (!sizingByExternalReferenceCode[externalReferenceCode]) {
				setSubmitError(translate('this-field-is-required'));

				return;
			}
		}

		setSubmitError('');
		setStepIndex((previous) => Math.min(previous + 1, steps.length - 1));
	}

	function handleBack() {
		setSubmitError('');
		setStepIndex((previous) => Math.max(previous - 1, 0));
	}

	async function handleGenerate() {
		const values = getValues();

		setSubmitError('');
		setSubmitting(true);

		try {
			await LicenseKeys.createLicenseKey({
				active: true,
				additionalInfo: JSON.stringify({
					products: includedProducts.map((product) => ({
						externalReferenceCode: product.externalReferenceCode,
						name: product.name,
						sizing:
							sizingByExternalReferenceCode[
								product.externalReferenceCode
							] ?? '',
					})),
				}),
				customExpirationDate: toISODate(values.expirationDate),
				description: values.description || undefined,
				domains: values.domains,
				hostName: values.hostName || undefined,
				licenseName: 'Aggregate License',
				licenseType: values.keyType,
				maxClusterNodes: values.clusterSize
					? Number(values.clusterSize)
					: undefined,
				name: values.environmentName,
				productName:
					includedProducts
						.map((product) => product.name)
						.join(', ') || undefined,
				r_accountEntryToLicenseKey_accountEntryId: accountId
					? Number(accountId)
					: undefined,
				r_projectToLicenseKey_c_projectERC:
					projectExternalReferenceCode || undefined,
				startDate: toISODate(values.startDate),
			});

			onGenerated();
			onClose();
		}
		catch {
			setSubmitError(translate('an-unexpected-error-occurred'));
		}
		finally {
			setSubmitting(false);
		}
	}

	function renderStep() {
		if (currentStep === 'environment') {
			return (
				<>
					<Input
						{...register('environmentName')}
						errorMessage={errors.environmentName?.message}
						label={translate('environment-name')}
						required
					/>

					<Select
						defaultOptionLabel={translate('select-an-option')}
						errors={errors as Record<string, {message?: string}>}
						label={translate('key-type')}
						name="keyType"
						onChange={({target: {value}}) =>
							setValue('keyType', value, {shouldValidate: true})
						}
						options={KEY_TYPE_OPTIONS}
						required
						value={keyType}
					/>

					{HOST_KEY_TYPES.includes(keyType) ? (
						<Input
							{...register('hostName')}
							label={translate('host-name')}
						/>
					) : null}

					{CLUSTER_KEY_TYPES.includes(keyType) ? (
						<Input
							{...register('clusterSize')}
							label={translate('cluster-size')}
							type="number"
						/>
					) : null}

					<Input
						{...register('domains')}
						errorMessage={errors.domains?.message}
						label={translate('domains')}
						required
					/>

					<Input
						{...register('startDate')}
						errorMessage={errors.startDate?.message}
						label={translate('start-date')}
						required
						type="date"
					/>

					<Input
						{...register('expirationDate')}
						label={translate('expiration-date')}
						type="date"
					/>

					<Input
						{...register('description')}
						component="textarea"
						label={translate('description')}
					/>
				</>
			);
		}

		if (currentStep === 'products') {
			return (
				<div className="d-flex flex-column gap-2">
					<p className="text-neutral-8">
						{translate('select-products')}
					</p>

					{products.map((product) => (
						<label
							className="align-items-center d-flex gap-2"
							key={product.externalReferenceCode}
						>
							<input
								checked={includedExternalReferenceCodes.includes(
									product.externalReferenceCode
								)}
								onChange={() =>
									toggleProduct(product.externalReferenceCode)
								}
								type="checkbox"
							/>

							<span>{product.name}</span>
						</label>
					))}
				</div>
			);
		}

		if (currentStep.startsWith('product:')) {
			const externalReferenceCode = currentStep.slice('product:'.length);
			const product = includedProducts.find(
				(current) =>
					current.externalReferenceCode === externalReferenceCode
			);

			return (
				<>
					<h4 className="mb-3">{product?.name}</h4>

					<Select
						defaultOptionLabel={translate('select-an-option')}
						label={translate('sizing')}
						name="sizing"
						onChange={({target: {value}}) =>
							setSizingByExternalReferenceCode((previous) => ({
								...previous,
								[externalReferenceCode]: value,
							}))
						}
						options={SIZING_OPTIONS}
						required
						value={
							sizingByExternalReferenceCode[
								externalReferenceCode
							] ?? ''
						}
					/>
				</>
			);
		}

		const values = getValues();

		return (
			<div className="d-flex flex-column gap-2">
				<h4>{translate('review')}</h4>

				<div>
					<strong>{translate('environment-name')}:</strong>{' '}
					{values.environmentName}
				</div>

				<div>
					<strong>{translate('key-type')}:</strong> {values.keyType}
				</div>

				<div>
					<strong>{translate('domains')}:</strong> {values.domains}
				</div>

				<div>
					<strong>{translate('products')}:</strong>{' '}
					{includedProducts
						.map((product) => product.name)
						.join(', ') || '-'}
				</div>
			</div>
		);
	}

	const isReview = currentStep === 'review';

	return (
		<div className="d-flex flex-column">
			<div className="mb-3 text-neutral-7">
				{translate('step-x-of-x')
					.replace('{0}', String(stepIndex + 1))
					.replace('{1}', String(steps.length))}
			</div>

			{renderStep()}

			{submitError ? (
				<div className="mt-3 text-danger">{submitError}</div>
			) : null}

			<div className="d-flex justify-content-between mt-4">
				<ClayButton
					disabled={submitting}
					displayType="secondary"
					onClick={stepIndex === 0 ? onClose : handleBack}
				>
					{stepIndex === 0 ? translate('cancel') : translate('back')}
				</ClayButton>

				{isReview ? (
					<ClayButton
						disabled={submitting || !accountId}
						onClick={handleGenerate}
					>
						{translate('generate')}
					</ClayButton>
				) : (
					<ClayButton onClick={handleNext}>
						{translate('next')}
					</ClayButton>
				)}
			</div>
		</div>
	);
}
