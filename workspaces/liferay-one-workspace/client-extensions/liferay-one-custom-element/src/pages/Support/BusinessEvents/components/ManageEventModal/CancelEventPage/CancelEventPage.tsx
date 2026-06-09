/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import {Observer} from '@clayui/modal/lib/types';
import {useState} from 'react';

import Badge from '../../../../../../components/Badge/Badge';
import {translate} from '../../../../../../i18n';
import {Liferay} from '../../../../../../liferay/liferay';
import {updateBusinessEvent} from '../../../services/jira/Jira';
import {IBusinessEvent} from '../../../types';
import BusinessEventsModal from '../../BusinessEventsModal/BusinessEventsModal';

interface IProps {
	accountExternalReferenceCode: string;
	businessEvent: IBusinessEvent;
	closeFunction?: (value: boolean) => void;
	modalType: string;
	observer: Observer;
	onCancel: () => void;
}

const CancelEventPage: React.FC<IProps> = ({
	accountExternalReferenceCode,
	businessEvent,
	closeFunction = () => {},
	modalType,
	observer,
	onCancel,
}) => {
	const [isLoadingSubmitButton, setIsLoadingSubmitButton] =
		useState<boolean>(false);
	const [reason, setReason] = useState('');

	const handleInputChange = (event: {target: {value: string}}) => {
		setReason(event.target.value);
	};

	const handleSubmit = async () => {
		const updatedBusinessEvent = {...businessEvent};

		const businessEventId = updatedBusinessEvent.id;

		if (!businessEventId) {
			return;
		}

		const formattedBusinessEvent = {
			...updatedBusinessEvent,
			currentLiferayVersion:
				updatedBusinessEvent.currentLiferayVersion?.key,
			eventStatus: 'Canceled',
			eventType: updatedBusinessEvent.eventType?.key,
			lastComment: reason,
			newLiferayVersion: updatedBusinessEvent.newLiferayVersion?.key,
			timeZone: updatedBusinessEvent.timeZone?.key,
		};

		try {
			setIsLoadingSubmitButton(true);

			await updateBusinessEvent(
				accountExternalReferenceCode,
				businessEventId,
				formattedBusinessEvent
			);

			closeFunction(false);
			onCancel();
		}
		catch (error) {
			setIsLoadingSubmitButton(false);

			Liferay.Util.openToast({
				message: translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}
	};

	return (
		<BusinessEventsModal
			handleSubmit={handleSubmit}
			headerTitle={translate('cancel-business-event')}
			isLoadingSubmitButton={isLoadingSubmitButton}
			modalType={modalType}
			observer={observer}
			onClose={() => closeFunction(false)}
			reason={reason}
			submitButton={translate('cancel-business-event')}
			title={`${translate('cancel')} ${businessEvent.name}`}
		>
			<div>
				<div className="font-weight-bold mb-3">
					{translate(
						'please-let-us-know-the-reason-you-are-canceling-this-event'
					)}

					<span className="edit-modal-asterisk"> *</span>
				</div>

				<ClayInput
					component="textarea"
					onChange={handleInputChange}
					required
					type="text"
					value={reason}
				/>

				<Badge alertType="info" badgeClassName="mt-3">
					<span className="pl-1 text-paragraph">
						{translate(
							'once-canceled-no-further-edits-can-be-made-to-this-event'
						)}
					</span>
				</Badge>
			</div>
		</BusinessEventsModal>
	);
};

export default CancelEventPage;
