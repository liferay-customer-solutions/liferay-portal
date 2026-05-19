/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {FormEvent, useEffect, useState} from 'react';

declare global {
	interface Window {
		Liferay?: {
			OAuth2Client: {
				FromUserAgentApplication(name: string): {
					fetch(path: string, init?: RequestInit): Promise<Response>;
				};
			};
		};
	}
}

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

function getClient() {
	return window.Liferay?.OAuth2Client.FromUserAgentApplication(OAUTH_APP);
}

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
		const client = getClient();

		if (!client) {
			return;
		}

		client
			.fetch('/admin/debug-message-queue/routing-keys')
			.then((r) => r.json())
			.then(setRoutingKeys)
			.catch((error) =>
				console.error('Failed to load routing keys', error)
			);
	}, []);

	async function handleSubmit(event: FormEvent) {
		event.preventDefault();

		setSubmitState({status: 'loading'});

		try {
			const client = getClient();

			if (!client) {
				throw new Error('OAuth2 client unavailable');
			}

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
		<div style={{maxWidth: '720px', padding: '2rem'}}>
			<h2>Message Queue</h2>

			<p>
				Send a test message to a Pub/Sub topic to replay or debug
				distributed-messaging flows without waiting for an upstream
				producer.
			</p>

			{!!routingKeys.length && (
				<details style={{marginBottom: '1.5rem'}}>
					<summary style={{cursor: 'pointer', fontWeight: 'bold'}}>
						Valid routing keys
					</summary>

					<table
						style={{
							borderCollapse: 'collapse',
							marginTop: '0.5rem',
							width: '100%',
						}}
					>
						<thead>
							<tr>
								<th
									style={{
										borderBottom: '1px solid #ccc',
										padding: '0.4rem 0.8rem',
										textAlign: 'left',
									}}
								>
									Routing Key
								</th>

								<th
									style={{
										borderBottom: '1px solid #ccc',
										padding: '0.4rem 0.8rem',
										textAlign: 'left',
									}}
								>
									Pub/Sub Topic
								</th>
							</tr>
						</thead>

						<tbody>
							{routingKeys.map(({routingKey, topic}) => (
								<tr key={routingKey}>
									<td
										style={{
											padding: '0.4rem 0.8rem',
										}}
									>
										<code>{routingKey}</code>
									</td>

									<td
										style={{
											padding: '0.4rem 0.8rem',
										}}
									>
										{topic}
									</td>
								</tr>
							))}
						</tbody>
					</table>
				</details>
			)}

			<form onSubmit={handleSubmit}>
				<div style={{marginBottom: '1rem'}}>
					<label
						htmlFor="routingKey"
						style={{display: 'block', fontWeight: 'bold'}}
					>
						Routing Key
					</label>

					<input
						id="routingKey"
						onChange={(event) =>
							setForm((prev) => ({
								...prev,
								routingKey: event.target.value,
							}))
						}
						required
						style={{
							boxSizing: 'border-box',
							padding: '0.4rem',
							width: '100%',
						}}
						type="text"
						value={form.routingKey}
					/>
				</div>

				<div style={{marginBottom: '1rem'}}>
					<label
						htmlFor="message"
						style={{display: 'block', fontWeight: 'bold'}}
					>
						Message
					</label>

					<small style={{color: '#666'}}>
						Newlines are stripped before publishing to preserve
						legacy semantics.
					</small>

					<textarea
						id="message"
						onChange={(event) =>
							setForm((prev) => ({
								...prev,
								message: event.target.value,
							}))
						}
						rows={6}
						style={{
							boxSizing: 'border-box',
							display: 'block',
							padding: '0.4rem',
							width: '100%',
						}}
						value={form.message}
					/>
				</div>

				<div style={{marginBottom: '1rem'}}>
					<label
						htmlFor="properties"
						style={{display: 'block', fontWeight: 'bold'}}
					>
						Properties
					</label>

					<small style={{color: '#666'}}>
						One <code>key=value</code> per line. Insertion order is
						preserved.
					</small>

					<textarea
						id="properties"
						onChange={(event) =>
							setForm((prev) => ({
								...prev,
								properties: event.target.value,
							}))
						}
						placeholder={'key1=value1\nkey2=value2'}
						rows={4}
						style={{
							boxSizing: 'border-box',
							display: 'block',
							padding: '0.4rem',
							width: '100%',
						}}
						value={form.properties}
					/>
				</div>

				<button
					disabled={submitState.status === 'loading'}
					style={{padding: '0.5rem 1.5rem'}}
					type="submit"
				>
					{submitState.status === 'loading' ? 'Sending…' : 'Submit'}
				</button>
			</form>

			{submitState.status === 'success' && (
				<div
					role="alert"
					style={{
						backgroundColor: '#e6f4ea',
						border: '1px solid #34a853',
						borderRadius: '4px',
						color: '#137333',
						marginTop: '1rem',
						padding: '0.75rem 1rem',
					}}
				>
					Message published successfully.
				</div>
			)}

			{submitState.status === 'error' && (
				<div
					role="alert"
					style={{
						backgroundColor: '#fce8e6',
						border: '1px solid #ea4335',
						borderRadius: '4px',
						color: '#c5221f',
						marginTop: '1rem',
						padding: '0.75rem 1rem',
					}}
				>
					Error: {submitState.message}
				</div>
			)}
		</div>
	);
}
