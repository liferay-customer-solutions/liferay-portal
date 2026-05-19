/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayForm, {ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import ClayTable from '@clayui/table';
import * as OAuth2 from '@liferay/oauth2-provider-web/client';
import {FormEvent, useEffect, useState} from 'react';

import Page from '../../../components/Page';

type RoutingKey = {
	routingKey: string;
	topic: string;
};

type SubmitState =
	| {status: 'idle'}
	| {status: 'loading'}
	| {status: 'success'}
	| {message: string; status: 'error'};

const OAUTH_APP = 'liferay-one-etc-spring-boot-oaua';

export default function MessageQueue() {
	const [routingKeys, setRoutingKeys] = useState<RoutingKey[]>([]);

	const [form, setForm] = useState({
		message: '',
		properties: '',
		routingKey: 'ebenezer-support-opportunity-entries',
	});

	const [submitState, setSubmitState] = useState<SubmitState>({
		status: 'idle',
	});

	useEffect(() => {
		OAuth2.FromUserAgentApplication(OAUTH_APP)
			.then((client) =>
				client.fetch('/admin/debug-message-queue/routing-keys')
			)
			.then((response) => response.json())
			.then(setRoutingKeys)
			.catch((error) =>
				console.error('Failed to load routing keys', error)
			);
	}, []);

	async function handleSubmit(event: FormEvent) {
		event.preventDefault();

		setSubmitState({status: 'loading'});

		try {
			const client = await OAuth2.FromUserAgentApplication(OAUTH_APP);

			const response = await client.fetch('/admin/debug-message-queue', {
				body: JSON.stringify(form),
				headers: {'Content-Type': 'application/json'},
				method: 'POST',
			});

			if (!response.ok) {
				const data = await response.json().catch(() => ({}));

				throw new Error(
					data?.error?.message ?? `HTTP ${response.status}`
				);
			}

			setSubmitState({status: 'success'});
		}
		catch (error) {
			setSubmitState({
				message:
					error instanceof Error ? error.message : 'Unknown error',
				status: 'error',
			});
		}
	}

	return (
		<Page
			description="Send a test message to a Pub/Sub topic to replay or debug distributed-messaging flows without waiting for an upstream producer."
			title="Message Queue"
		>
			<div className="border p-4 rounded-lg" style={{maxWidth: '720px'}}>
				{!!routingKeys.length && (
					<details className="mb-4">
						<summary className="font-weight-bold">
							Valid routing keys
						</summary>

						<ClayTable className="mt-2">
							<ClayTable.Head>
								<ClayTable.Row>
									<ClayTable.Cell headingTitle>
										Routing Key
									</ClayTable.Cell>

									<ClayTable.Cell headingTitle>
										Pub/Sub Topic
									</ClayTable.Cell>
								</ClayTable.Row>
							</ClayTable.Head>

							<ClayTable.Body>
								{routingKeys.map(({routingKey, topic}) => (
									<ClayTable.Row key={routingKey}>
										<ClayTable.Cell>
											<code>{routingKey}</code>
										</ClayTable.Cell>

										<ClayTable.Cell>{topic}</ClayTable.Cell>
									</ClayTable.Row>
								))}
							</ClayTable.Body>
						</ClayTable>
					</details>
				)}

				<ClayForm onSubmit={handleSubmit}>
					<ClayForm.Group>
						<label htmlFor="routingKey">Routing Key</label>

						<ClayInput
							id="routingKey"
							onChange={(event) =>
								setForm((prev) => ({
									...prev,
									routingKey: event.target.value,
								}))
							}
							required
							type="text"
							value={form.routingKey}
						/>
					</ClayForm.Group>

					<ClayForm.Group>
						<label htmlFor="message">Message</label>

						<ClayForm.Text>
							Newlines are stripped before publishing to preserve
							legacy semantics.
						</ClayForm.Text>

						<ClayInput
							component="textarea"
							id="message"
							onChange={(event) =>
								setForm((prev) => ({
									...prev,
									message: event.target.value,
								}))
							}
							style={{minHeight: '9rem'}}
							value={form.message}
						/>
					</ClayForm.Group>

					<ClayForm.Group>
						<label htmlFor="properties">Properties</label>

						<ClayForm.Text>
							One <code>key=value</code> per line. Insertion order
							is preserved.
						</ClayForm.Text>

						<ClayInput
							component="textarea"
							id="properties"
							onChange={(event) =>
								setForm((prev) => ({
									...prev,
									properties: event.target.value,
								}))
							}
							placeholder={'key1=value1\nkey2=value2'}
							style={{minHeight: '6rem'}}
							value={form.properties}
						/>
					</ClayForm.Group>

					<ClayButton
						disabled={submitState.status === 'loading'}
						type="submit"
					>
						{submitState.status === 'loading'
							? 'Sending…'
							: 'Submit'}
					</ClayButton>
				</ClayForm>

				{submitState.status === 'success' && (
					<div className="alert alert-success mb-0 mt-3" role="alert">
						<span className="alert-indicator">
							<ClayIcon symbol="check-circle-full" />
						</span>
						Message published successfully.
					</div>
				)}

				{submitState.status === 'error' && (
					<div className="alert alert-danger mb-0 mt-3" role="alert">
						<span className="alert-indicator">
							<ClayIcon symbol="exclamation-full" />
						</span>

						{submitState.message}
					</div>
				)}
			</div>
		</Page>
	);
}
