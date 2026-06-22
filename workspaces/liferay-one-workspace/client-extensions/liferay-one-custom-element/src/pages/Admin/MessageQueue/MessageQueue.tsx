/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayForm, {ClayInput, ClaySelect} from '@clayui/form';
import ClayTable from '@clayui/table';
import {useEffect, useState} from 'react';

import EmptyState from '../../../components/EmptyState';
import Page from '../../../components/Page';
import {translate} from '../../../i18n';
import {Liferay} from '../../../liferay/liferay';
import FetcherError from '../../../services/fetcher/FetcherError';
import useHasMessageQueuePermissions from './hooks/useHasMessageQueuePermissions';
import {
	RoutingKey,
	dispatchMessage,
	getRoutingKeys,
} from './services/MessageQueue';

export default function MessageQueue() {
	const [dispatching, setDispatching] = useState(false);
	const [message, setMessage] = useState('');
	const [properties, setProperties] = useState('');
	const [routingKey, setRoutingKey] = useState('');
	const [routingKeys, setRoutingKeys] = useState<RoutingKey[]>([]);

	const {hasMessageQueuePermissions, loading} =
		useHasMessageQueuePermissions();

	useEffect(() => {
		if (!hasMessageQueuePermissions) {
			return;
		}

		getRoutingKeys()
			.then((items) => {
				setRoutingKeys(items);

				if (items.length) {
					setRoutingKey(items[0].routingKey);
				}
			})
			.catch(() => setRoutingKeys([]));
	}, [hasMessageQueuePermissions]);

	const handleSubmit = async (event: {preventDefault: () => void}) => {
		event.preventDefault();

		setDispatching(true);

		try {
			await dispatchMessage({message, properties, routingKey});

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

	if (!loading && !hasMessageQueuePermissions) {
		return (
			<Page title={translate('message-queue')}>
				<EmptyState
					description={translate(
						'you-do-not-have-access-to-the-message-queue'
					)}
					title={translate('access-required')}
					type="NO_ACCESS"
				/>
			</Page>
		);
	}

	return (
		<Page
			pageRendererProps={{isLoading: loading}}
			title={translate('message-queue')}
		>
			<form onSubmit={handleSubmit}>
				<ClayForm.Group>
					<label htmlFor="messageQueueRoutingKey">
						{translate('routing-key')}
					</label>

					<ClaySelect
						id="messageQueueRoutingKey"
						onChange={(event: {target: {value: string}}) =>
							setRoutingKey(event.target.value)
						}
						required
						value={routingKey}
					>
						<ClaySelect.Option
							label={translate('select-a-routing-key')}
							value=""
						/>

						{routingKeys.map((item) => (
							<ClaySelect.Option
								key={`${item.routingKey}:${item.subscriber}`}
								label={item.routingKey}
								value={item.routingKey}
							/>
						))}
					</ClaySelect>
				</ClayForm.Group>

				<ClayForm.Group>
					<label htmlFor="messageQueueMessage">
						{translate('message')}
					</label>

					<ClayInput
						component="textarea"
						id="messageQueueMessage"
						onChange={(event: {target: {value: string}}) =>
							setMessage(event.target.value)
						}
						required
						value={message}
					/>
				</ClayForm.Group>

				<ClayForm.Group>
					<label htmlFor="messageQueueProperties">
						{translate('properties')}
					</label>

					<ClayInput
						component="textarea"
						id="messageQueueProperties"
						onChange={(event: {target: {value: string}}) =>
							setProperties(event.target.value)
						}
						value={properties}
					/>
				</ClayForm.Group>

				<ClayButton disabled={dispatching} type="submit">
					{translate('submit')}
				</ClayButton>
			</form>

			{!!routingKeys.length && (
				<div className="mt-4">
					<h4>{translate('routing-keys')}</h4>

					<ClayTable>
						<ClayTable.Head>
							<ClayTable.Row>
								<ClayTable.Cell headingCell>
									{translate('routing-key')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('subscriber')}
								</ClayTable.Cell>
							</ClayTable.Row>
						</ClayTable.Head>

						<ClayTable.Body>
							{routingKeys.map((item) => (
								<ClayTable.Row
									key={`${item.routingKey}:${item.subscriber}`}
								>
									<ClayTable.Cell>
										{item.routingKey}
									</ClayTable.Cell>

									<ClayTable.Cell>
										{item.subscriber}
									</ClayTable.Cell>
								</ClayTable.Row>
							))}
						</ClayTable.Body>
					</ClayTable>
				</div>
			)}
		</Page>
	);
}
