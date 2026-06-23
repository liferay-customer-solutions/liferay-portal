/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {zodResolver} from '@hookform/resolvers/zod';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {KeyedMutator} from 'swr';
import {z} from 'zod';
import FormInput from '~/components/FormInput/FormInput';
import i18n from '~/i18n';
import {
	EXTEND_OPTIONS,
	EXTEND_TYPES,
} from '~/pages/Admin/SSADashboard/utils/constants';
import adminSchemas from '~/schema/adminSchemas';
import {Liferay} from '~/services/liferay/liferay';
import TrialExtensionRequests from '~/services/objects/TrialExtensionRequests';
import trialOAuth2 from '~/services/spring-boot/Trial';
import {OrderCustomFields} from '~/utils/orderUtils';

import type {APIResponse} from '~/types/api';
import type {PlacedOrder} from '~/types/orders';
import type {TrialExtend} from '~/types/trial';

type ExtendSSATrialModalProps = {
	accountId: number;
	firstExtendRequest: boolean;
	mutatePlacedOrder?: KeyedMutator<APIResponse<PlacedOrder>>;
	onClose: () => void;
	order: PlacedOrder;
	orderMutate: KeyedMutator<APIResponse<PlacedOrder>>;
	ssaTrialExtendMutate: KeyedMutator<APIResponse<TrialExtend>>;
};

const ExtendSSATrialModal: React.FC<ExtendSSATrialModalProps> = ({
	accountId,
	firstExtendRequest,
	mutatePlacedOrder,
	onClose,
	order,
	orderMutate,
	ssaTrialExtendMutate,
}) => {
	const [submitting, setSubmitting] = useState<boolean>(false);

	const {
		formState: {errors, isLoading},
		handleSubmit,
		register,
	} = useForm({
		defaultValues: {
			duration: '' as unknown as number,
			reason: '',
		},
		mode: 'onSubmit',
		resolver: zodResolver(adminSchemas.extendSSATrial),
	});

	const inputProps = {
		errors,
		register: register as (
			name: string,
			options?: Record<string, unknown>
		) => Record<string, unknown> | void,
		required: true,
	};

	const extendType = firstExtendRequest
		? EXTEND_TYPES.AUTO_EXTEND
		: EXTEND_TYPES.ADMIN_REQUEST;

	const extendOptions = EXTEND_OPTIONS.find(
		(option) => option.extendType === extendType
	);

	const onSubmit = async (
		form: z.infer<typeof adminSchemas.extendSSATrial>
	) => {
		setSubmitting(true);

		const trialSettings =
			order.customFields?.[OrderCustomFields.TRIAL_SETTINGS];
		const projectId = JSON.parse(trialSettings)?.projectId;

		try {
			const extendTrial = {
				dueStatus: {
					key:
						extendType === EXTEND_TYPES.AUTO_EXTEND
							? 'AutoApproved'
							: 'Pending',
				},
				duration: form.duration,
				projectId,
				r_accountEntryToTrialExtensionRequest_accountEntryId: accountId,
				r_orderToTrialExtensionRequest_commerceOrderId: order.id,
				reason: form.reason,
			};

			const newExtensionRequest: TrialExtend =
				await TrialExtensionRequests.createTrialExtensionRequest(
					extendTrial
				);

			if (extendType === EXTEND_TYPES.AUTO_EXTEND) {
				await trialOAuth2.extendTrial(newExtensionRequest.id);
			}

			ssaTrialExtendMutate(
				(data: APIResponse<TrialExtend> | undefined) => {
					return {
						...(data ?? ({} as APIResponse<TrialExtend>)),
						items: [newExtensionRequest, ...(data?.items ?? [])],
					};
				},
				{revalidate: false}
			);

			if (extendType === EXTEND_TYPES.AUTO_EXTEND) {
				if (mutatePlacedOrder) {
					mutatePlacedOrder(
						(response: APIResponse<PlacedOrder> | undefined) =>
							response,
						{
							revalidate: true,
						}
					);
				}

				orderMutate(
					(response: APIResponse<PlacedOrder> | undefined) =>
						response,
					{
						revalidate: true,
					}
				);
			}

			Liferay.Util.openToast({
				message: i18n.translate('trial-extended-successfully'),
				title: i18n.translate('success'),
				type: 'success',
			});

			setSubmitting(false);

			onClose();
		}
		catch (error) {
			console.error(error);

			Liferay.Util.openToast({
				message: i18n.translate('failed-to-extend-trial'),
				title: i18n.translate('failure'),
				type: 'danger',
			});
			setSubmitting(false);
		}
	};

	return (
		<div>
			<ClayAlert displayType={extendOptions?.alertType}>
				{extendOptions?.alertText}
			</ClayAlert>

			<FormInput
				{...inputProps}
				boldLabel
				label="Duration"
				name="duration"
				placeholder="Value between 1 and 60"
				required={true}
				type="number"
			/>
			<FormInput
				{...inputProps}
				boldLabel
				label={i18n.translate('reason')}
				name="reason"
				placeholder="Tell why you need to extend the trial"
				required={true}
				type="textarea"
			/>
			<div className="d-flex justify-content-end">
				<ClayButton
					className="mr-4"
					disabled={!!submitting}
					displayType="secondary"
					onClick={onClose}
				>
					{i18n.translate('cancel')}
				</ClayButton>
				<ClayButton
					disabled={isLoading || submitting}
					onClick={handleSubmit(onSubmit)}
				>
					<div className="align-items-center d-flex">
						{submitting && (
							<ClayLoadingIndicator className="mr-3 my-0" />
						)}
						{extendOptions?.actionText}
					</div>
				</ClayButton>
			</div>
		</div>
	);
};

export default ExtendSSATrialModal;
