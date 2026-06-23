/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Observer} from '@clayui/modal/lib/types';
import {zodResolver} from '@hookform/resolvers/zod';
import {FormProvider, useForm} from 'react-hook-form';
import {IBusinessEvent} from '~/pages/BusinessEvents/types';
import adminSchemas from '~/schema/adminSchemas';

import CancelEventPage from './CancelEventPage/CancelEventPage';
import RecordGoLiveEventPage from './RecordGoLiveEventPage/RecordGoLiveEventPage';

interface IProps {
	accountExternalReferenceCode: string;
	businessEvent: IBusinessEvent;
	closeFunction?: (value: boolean) => void;
	modalType: string;
	observer: Observer;
	onCancel: () => void;
	onCompleted: () => void;
}

const ManageEventModal: React.FC<IProps> = ({
	accountExternalReferenceCode,
	businessEvent,
	closeFunction,
	modalType,
	observer,
	onCancel,
	onCompleted,
}) => {
	const methods = useForm({
		defaultValues: {
			businessEvent: {
				actualEventDate: '',
				actualEventTime: {
					hours: '--',
					minutes: '--',
				},
				lastComment: '',
				timeZone: businessEvent.timeZone || {key: ''},
			},
		},
		mode: 'onChange',
		resolver: zodResolver(adminSchemas.businessEventActual),
	});

	return (
		<>
			{modalType === 'cancelEvent' ? (
				<CancelEventPage
					accountExternalReferenceCode={accountExternalReferenceCode}
					businessEvent={businessEvent}
					closeFunction={closeFunction}
					modalType={modalType}
					observer={observer}
					onCancel={onCancel}
				/>
			) : (
				<FormProvider {...methods}>
					<RecordGoLiveEventPage
						accountExternalReferenceCode={
							accountExternalReferenceCode
						}
						businessEvent={businessEvent}
						closeFunction={closeFunction}
						modalType={modalType}
						observer={observer}
						onCompleted={onCompleted}
					/>
				</FormProvider>
			)}
		</>
	);
};

export default ManageEventModal;
