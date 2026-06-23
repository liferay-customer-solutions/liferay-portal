/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import i18n, {Word} from '~/i18n';

const STATUS_COLORS: {[key: string]: string} = {
	active: 'var(--color-success)',
	completed: 'var(--color-success)',
};

type DetailHeaderProps = {
	description?: string;
	icon?: string;
	logoColor: string;
	name: string;
	publisher: string;
	showByPrefix?: boolean;
	status: string;
};

export default function DetailHeader({
	description,
	icon,
	logoColor,
	name,
	publisher,
	showByPrefix,
	status,
}: DetailHeaderProps) {
	return (
		<div className="mb-4">
			<div
				className="align-items-center d-flex"
				style={{gap: 'var(--spacer-3)'}}
			>
				<span
					className="align-items-center d-flex justify-content-center"
					style={{
						background: logoColor,
						borderRadius: 'var(--border-radius-lg, 0.75rem)',
						color: 'var(--color-white)',
						flexShrink: 0,
						fontSize: '1.25rem',
						fontWeight: 600,
						height: '3.5rem',
						width: '3.5rem',
					}}
				>
					{icon ? (
						<ClayIcon
							style={{height: '1.75rem', width: '1.75rem'}}
							symbol={icon}
						/>
					) : (
						name.charAt(0)
					)}
				</span>

				<div className="d-flex flex-column" style={{gap: '0.25rem'}}>
					<h1
						className="m-0"
						style={{fontSize: '1.5rem', fontWeight: 700}}
					>
						{name}
					</h1>

					<span
						className="align-items-center d-flex"
						style={{
							color: 'var(--color-neutral-7)',
							gap: 'var(--spacer-2)',
						}}
					>
						<span>
							{showByPrefix
								? i18n.sub('by-x', publisher)
								: publisher}
						</span>

						<span
							className="align-items-center d-flex"
							style={{gap: 'var(--spacer-1)'}}
						>
							<span
								style={{
									background:
										STATUS_COLORS[status] ??
										'var(--color-neutral-6)',
									borderRadius: '50%',
									display: 'inline-block',
									height: '0.5rem',
									marginRight: '0.25rem',
									width: '0.5rem',
								}}
							/>

							{i18n.translate(status as Word)}
						</span>
					</span>
				</div>
			</div>

			{description && (
				<p
					className="mb-0 mt-3"
					style={{
						color: 'var(--color-neutral-8)',
						maxWidth: '52rem',
					}}
				>
					{description}
				</p>
			)}
		</div>
	);
}
