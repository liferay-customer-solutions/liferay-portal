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
	businessEvent: IBusinessEvent;
	closeFunction?: (value: boolean) => void;
	modalType: string;
	observer: Observer;
	onCancel: () => void;
	onCompleted: () => void;
	projectExternalReferenceCode: string;
}

const ManageEventModal: React.FC<IProps> = ({
	businessEvent,
	closeFunction,
	modalType,
	observer,
	onCancel,
	onCompleted,
	projectExternalReferenceCode,
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
					businessEvent={businessEvent}
					closeFunction={closeFunction}
					modalType={modalType}
					observer={observer}
					onCancel={onCancel}
					projectExternalReferenceCode={projectExternalReferenceCode}
				/>
			) : (
				<FormProvider {...methods}>
					<RecordGoLiveEventPage
						businessEvent={businessEvent}
						closeFunction={closeFunction}
						modalType={modalType}
						observer={observer}
						onCompleted={onCompleted}
						projectExternalReferenceCode={
							projectExternalReferenceCode
						}
					/>
				</FormProvider>
			)}
		</>
	);
};

export default ManageEventModal;
