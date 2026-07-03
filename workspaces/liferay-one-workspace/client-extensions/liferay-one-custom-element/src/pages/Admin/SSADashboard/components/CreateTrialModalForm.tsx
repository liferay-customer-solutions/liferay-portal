/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import ClayForm, {ClayInput} from '@clayui/form';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {Observer, Size} from '@clayui/modal/lib/types';
import MultiSelect from '@clayui/multi-select';
import {zodResolver} from '@hookform/resolvers/zod';
import classNames from 'classnames';
import {useCallback, useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';
import {Input} from '~/components/Input/Input';
import Loading from '~/components/Loading/Loading';
import Modal from '~/components/Modal/Modal';
import Select from '~/components/Select/Select';
import {useOneContext} from '~/context/OneContextProvider';
import {useDeliveryProductByExternalReferenceCode} from '~/hooks/useDeliveryProductByExternalReferenceCode';
import i18n from '~/i18n';
import {useSSADashboardOutlet} from '~/pages/Admin/SSADashboard/hooks/useSSADashboardOutlet';
import {
	defaultSiteInitializer,
	siteInitializers,
	trialObjectives,
} from '~/pages/Admin/SSADashboard/utils/constants';
import adminSchemas from '~/schema/adminSchemas';
import ProductPurchaseSSATrial from '~/services/commerce/ProductPurchaseSSATrial';
import {Liferay} from '~/services/liferay/liferay';
import trialOAuth2 from '~/services/spring-boot/Trial';
import {OrderCustomFields, OrderWorkflowStatusCode} from '~/utils/orderUtils';

import Form from './MarketplaceForm/MarketplaceForm';

import type {APIResponse} from '~/types/api';
import type {Cart, PlacedOrder} from '~/types/orders';

const SectionTitle = ({title}: {title: string}) => (
	<>
		<h4>{title}</h4>
		<hr className="mb-3" />
	</>
);

type CreateTrialModalFormProps = {
	modal: {
		observer: Observer;
		onClose: () => void;
		open: boolean;
	};
	mutate: (
		fn: (data: APIResponse<PlacedOrder>) => unknown,
		options?: {revalidate: boolean}
	) => Promise<unknown>;
};

type FormFields = z.infer<typeof adminSchemas.ssaTrialForm>;

type Item = {
	key: string;
	label: string;
	value: string;
};

const CreateTrialModalForm: React.FC<CreateTrialModalFormProps> = ({
	modal,
	mutate,
}) => {
	const {ssaAccount} = useSSADashboardOutlet();
	const {properties} = useOneContext();
	const {data: product} = useDeliveryProductByExternalReferenceCode(
		properties.productExternalReferenceCode
	);

	const productPurchase = useMemo(() => {
		if (!ssaAccount || !product) {
			return null;
		}

		return new ProductPurchaseSSATrial(ssaAccount, product);
	}, [ssaAccount, product]);

	const {
		formState: {errors, isSubmitting},
		handleSubmit,
		register,
		setError,
		setValue,
		watch,
	} = useForm<FormFields>({
		defaultValues: {
			duration: 1,
			emailAddress: [],
			objective: '',
			projectId: '',
			siteInitializerKey: defaultSiteInitializer,
		},
		resolver: zodResolver(adminSchemas.ssaTrialForm),
	});

	const emails = watch('emailAddress');
	const objective = watch('objective');
	const projectId = watch('projectId');

	useEffect(() => {
		const suggestedDuration = trialObjectives.find(
			(trialObjective) => trialObjective.key === objective
		);

		setValue('duration', suggestedDuration?.days || 1);
	}, [objective, setValue]);

	const onSubmit = useCallback(
		async (data: FormFields) => {
			const projectId = data.projectId.toLowerCase();

			try {
				await trialOAuth2.checkDomainAvailability(projectId);
			}
			catch (error: unknown) {
				console.error((error as {message: string}).message);

				if ((error as {status: number}).status === 409) {
					return setError('projectId', {
						message: 'Project ID already exists',
					});
				}

				return Liferay.Util.openToast({
					message: i18n.translate('an-unexpected-error-occurred'),
					type: 'danger',
				});
			}

			const consoleInviteEmailAddresses = [
				...new Set([
					Liferay.ThemeDisplay.getUserEmailAddress(),
					...(data.emailAddress as {value: string}[]).map(
						({value}) => value
					),
				]),
			];

			try {
				const order = await (
					productPurchase as ProductPurchaseSSATrial
				).createOrder({
					customFields: {
						[OrderCustomFields.TRIAL_SETTINGS]: JSON.stringify({
							consoleInviteEmailAddresses,
							duration: data.duration,
							projectId,
							siteInitializerKey: data.siteInitializerKey,
						}),
					},
				} as unknown as Cart);

				mutate(
					(orders: APIResponse<PlacedOrder>) => ({
						...orders,
						items: [
							{
								...order,
								orderStatusInfo: {
									code: OrderWorkflowStatusCode.PROCESSING,
									label: 'processing',
									label_i18n: 'processing',
								},
							},
							...orders.items,
						],
					}),
					{revalidate: false}
				);

				if (!order) {
					return;
				}

				await trialOAuth2.provisioningTrial(order.id);

				mutate((response: APIResponse<PlacedOrder>) => response, {
					revalidate: true,
				});

				Liferay.Util.openToast({
					message: 'Trial successfully provisioned.',
					title: i18n.translate('success'),
					type: 'success',
				});
			}
			catch (error) {
				console.error(error);

				Liferay.Util.openToast({
					message: i18n.translate('an-unexpected-error-occurred'),
					type: 'danger',
				});
			}

			modal.onClose();
		},
		[modal, mutate, productPurchase, setError]
	);

	if (!modal.open) {
		return null;
	}

	if (isSubmitting) {
		return (
			<Modal
				observer={modal.observer}
				size={'md' as Size}
				title={i18n.translate('ssa-trial-installation-in-progress')}
				visible={modal.open}
			>
				<div className="m-8">
					<Loading className="mb-3" />

					<p className="mt-8 text-center">
						{i18n.translate(
							'the-installation-process-is-ongoing-and-may-take-some-time-navigating-to-other-sections-will-not-cancel-the-process'
						)}
					</p>
				</div>

				<hr className="mt-4" />

				<div className="d-flex justify-content-end">
					<Button displayType="secondary" onClick={modal.onClose}>
						{i18n.translate('go-to-ssa-trial-listing')}
					</Button>
				</div>
			</Modal>
		);
	}

	return (
		<Modal
			observer={modal.observer}
			size={'md' as Size}
			title={i18n.translate('add-new-trial')}
			visible={modal.open}
		>
			<ClayForm.Group className="mb-3 pr-2 w-100">
				<Form.Label className="mb-2">
					{i18n.translate('project-id')}
				</Form.Label>

				<ClayInput.Group
					className={classNames({
						'has-error': errors.projectId,
					})}
				>
					<ClayInput.GroupItem prepend>
						<ClayInput
							{...register('projectId')}
							className="custom-input mb-0"
							maxLength={25}
							required
							type="text"
						/>
					</ClayInput.GroupItem>

					<ClayInput.GroupItem append shrink>
						<ClayInput.GroupText>
							{properties.ssaProjectPrefix || '.lxc.liferay.com'}
						</ClayInput.GroupText>
					</ClayInput.GroupItem>
				</ClayInput.Group>

				{errors.projectId && (
					<p className="field-base-feedback text-danger">
						{errors.projectId?.message}
					</p>
				)}

				<small className="mt-0 text-black-50">
					{`${projectId?.length}/25`}
				</small>
			</ClayForm.Group>

			<ClayForm.Group className="mb-3 mt-2 pr-2 w-100">
				<Form.Label className="mb-2">
					{i18n.translate('solution')}
				</Form.Label>

				<Select
					{...register('siteInitializerKey')}
					name="siteInitializerKey"
					options={siteInitializers}
				/>
			</ClayForm.Group>

			<ClayForm.Group className="mb-3">
				<SectionTitle title={i18n.translate('usage')} />

				<div className="d-flex">
					<div className="pr-2 w-100">
						<Form.Label className="mb-2">
							{i18n.translate('objective')}
						</Form.Label>

						<Select
							{...register('objective')}
							defaultOptionLabel="Select an option"
							errors={errors}
							name="objective"
							options={trialObjectives}
						/>
					</div>

					<div className="pr-2 w-100">
						<Form.Label className="mb-2">
							{i18n.translate('duration-days')}
						</Form.Label>
						<Input
							{...register('duration')}
							disabled={!objective}
							errorMessage={errors.duration?.message}
							max={60}
							min={1}
							type="number"
						/>
					</div>
				</div>
			</ClayForm.Group>

			<div className="mb-3 pr-2 w-100">
				<SectionTitle title={i18n.translate('additional-admin')} />

				<Form.Label className="mb-2">
					{i18n.translate('email-address')}
				</Form.Label>

				<MultiSelect
					className="bg-white marketplace-form-select"
					id="allowed-email-domains"
					items={emails}
					onItemsChange={(values: Item[]) => {
						setValue('emailAddress', values);
					}}
				/>
				{errors.emailAddress && (
					<p className="text-danger">
						{errors.emailAddress?.message}
					</p>
				)}
			</div>

			<hr />

			<div className="d-flex justify-content-end">
				<Button
					className="mr-2"
					disabled={isSubmitting}
					displayType="secondary"
					onClick={modal.onClose}
				>
					{i18n.translate('cancel')}
				</Button>

				<Button
					disabled={isSubmitting}
					displayType="primary"
					onClick={handleSubmit(onSubmit)}
				>
					<div className="align-items-center d-flex">
						{isSubmitting && (
							<ClayLoadingIndicator className="mr-3 my-0" />
						)}
						{i18n.translate('create')}
					</div>
				</Button>
			</div>
		</Modal>
	);
};

export default CreateTrialModalForm;
