/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayForm, {ClayInput, ClaySelect} from '@clayui/form';
import ClayTable from '@clayui/table';
import {useEffect, useState} from 'react';
import EmptyState from '~/components/EmptyState/EmptyState';
import Page from '~/components/Page/Page';
import {translate} from '~/i18n';
import FetcherError from '~/services/fetcher/FetcherError';
import {Liferay} from '~/services/liferay/liferay';
import DispatchMessage, {
	Subscriber,
} from '~/services/spring-boot/DispatchMessage';

import useHasAdminPermissions from './hooks/useHasAdminPermissions';

export default function PubSub() {
	const [attributes, setAttributes] = useState('');
	const [dispatching, setDispatching] = useState(false);
	const [payload, setPayload] = useState('');
	const [topic, setTopic] = useState('');
	const [topics, setTopics] = useState<Subscriber[]>([]);

	const {hasAdminPermissions, loading} = useHasAdminPermissions();

	useEffect(() => {
		if (!hasAdminPermissions) {
			return;
		}

		DispatchMessage.getTopics()
			.then((items) => {
				setTopics(items);

				if (items.length) {
					setTopic(items[0].topic);
				}
			})
			.catch(() => setTopics([]));
	}, [hasAdminPermissions]);

	const handleSubmit = async (event: {preventDefault: () => void}) => {
		event.preventDefault();

		setDispatching(true);

		try {
			await DispatchMessage.dispatchMessage({attributes, payload, topic});

			Liferay.Util.openToast({
				message: translate('success'),
				type: 'success',
			});
		}
		catch (error) {
			const info = (error as FetcherError)?.info;

			Liferay.Util.openToast({
				message:
					info?.detail ??
					info?.title ??
					translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}
		finally {
			setDispatching(false);
		}
	};

	if (!loading && !hasAdminPermissions) {
		return (
			<Page title={translate('pub-sub')}>
				<EmptyState
					description={translate('you-do-not-have-access-to-pub-sub')}
					title={translate('access-required')}
					type="NO_ACCESS"
				/>
			</Page>
		);
	}

	return (
		<Page
			pageRendererProps={{isLoading: loading}}
			title={translate('pub-sub')}
		>
			<form onSubmit={handleSubmit}>
				<ClayForm.Group>
					<label htmlFor="pubSubTopic">{translate('topic')}</label>

					<ClaySelect
						id="pubSubTopic"
						onChange={(event: {target: {value: string}}) =>
							setTopic(event.target.value)
						}
						required
						value={topic}
					>
						<ClaySelect.Option
							label={translate('select-a-topic')}
							value=""
						/>

						{topics.map((item) => (
							<ClaySelect.Option
								key={`${item.topic}:${item.name}`}
								label={item.topic}
								value={item.topic}
							/>
						))}
					</ClaySelect>
				</ClayForm.Group>

				<ClayForm.Group>
					<label htmlFor="pubSubPayload">
						{translate('payload')}
					</label>

					<ClayInput
						component="textarea"
						id="pubSubPayload"
						onChange={(event: {target: {value: string}}) =>
							setPayload(event.target.value)
						}
						required
						value={payload}
					/>
				</ClayForm.Group>

				<ClayForm.Group>
					<label htmlFor="pubSubAttributes">
						{translate('attributes')}
					</label>

					<ClayInput
						component="textarea"
						id="pubSubAttributes"
						onChange={(event: {target: {value: string}}) =>
							setAttributes(event.target.value)
						}
						value={attributes}
					/>
				</ClayForm.Group>

				<ClayButton disabled={dispatching} type="submit">
					{translate('submit')}
				</ClayButton>
			</form>

			{!!topics.length && (
				<div className="mt-4">
					<h4>{translate('topics')}</h4>

					<ClayTable>
						<ClayTable.Head>
							<ClayTable.Row>
								<ClayTable.Cell headingCell>
									{translate('topic')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('name')}
								</ClayTable.Cell>
							</ClayTable.Row>
						</ClayTable.Head>

						<ClayTable.Body>
							{topics.map((item) => (
								<ClayTable.Row
									key={`${item.topic}:${item.name}`}
								>
									<ClayTable.Cell>
										{item.topic}
									</ClayTable.Cell>

									<ClayTable.Cell>{item.name}</ClayTable.Cell>
								</ClayTable.Row>
							))}
						</ClayTable.Body>
					</ClayTable>
				</div>
			)}
		</Page>
	);
}
