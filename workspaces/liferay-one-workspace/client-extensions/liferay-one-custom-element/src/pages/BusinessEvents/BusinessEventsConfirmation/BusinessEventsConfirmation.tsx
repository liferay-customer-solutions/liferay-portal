/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import {Observer} from '@clayui/modal/lib/types';
import {translate} from '~/i18n';
import BusinessEventsModal from '~/pages/BusinessEvents/components/BusinessEventsModal/BusinessEventsModal';

interface IBusinessEventsConfirmationProps {
	handleSubmit: () => void;
	headerTitle: string;
	isLoadingSubmitButton?: boolean;
	message: string;
	observer: Observer;
	onClose: () => void;
	reason: string;
	setReason: React.Dispatch<React.SetStateAction<string>>;
}

const BusinessEventsConfirmation = ({
	handleSubmit,
	headerTitle,
	isLoadingSubmitButton,
	message,
	observer,
	onClose,
	reason,
	setReason,
}: IBusinessEventsConfirmationProps) => {
	const handleInputChange = (event: {target: {value: string}}) => {
		setReason(event.target.value);
	};

	return (
		<BusinessEventsModal
			handleSubmit={handleSubmit}
			headerTitle={headerTitle}
			isLoadingSubmitButton={isLoadingSubmitButton}
			modalType="editEvent"
			observer={observer}
			onClose={onClose}
			reason={reason}
			submitButton={translate('save-changes')}
			title={translate('change-planned-event-date')}
		>
			<p className="mb-3">{message}</p>

			<div>
				<div className="font-weight-bold pb-2">
					{translate('reason-for-change')}

					<span className="edit-modal-asterisk"> *</span>
				</div>

				<ClayInput
					component="textarea"
					onChange={handleInputChange}
					required
					type="text"
					value={reason}
				/>
			</div>
		</BusinessEventsModal>
	);
};

export default BusinessEventsConfirmation;
