/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {ReactNode, useEffect, useRef, useState} from 'react';

type LDPEventsPeriodPickerProps = {
	alignment?: 'left' | 'right';
	children: (close: () => void) => ReactNode;
	label: string;
	onStepBack: () => void;
	onStepForward: () => void;
	title: string;
};

export default function LDPEventsPeriodPicker({
	alignment = 'right',
	children,
	label,
	onStepBack,
	onStepForward,
	title,
}: LDPEventsPeriodPickerProps) {
	const [expanded, setExpanded] = useState(false);
	const containerRef = useRef<HTMLDivElement>(null);
	const triggerRef = useRef<HTMLButtonElement>(null);

	useEffect(() => {
		if (!expanded) {
			return;
		}

		const close = () => {
			setExpanded(false);

			triggerRef.current?.focus();
		};

		const handleKeyDown = (event: KeyboardEvent) => {
			if (event.key === 'Escape') {
				close();
			}
		};

		const handlePointerDown = (event: MouseEvent) => {
			if (!containerRef.current?.contains(event.target as Node)) {
				setExpanded(false);
			}
		};

		document.addEventListener('keydown', handleKeyDown);
		document.addEventListener('mousedown', handlePointerDown);

		return () => {
			document.removeEventListener('keydown', handleKeyDown);
			document.removeEventListener('mousedown', handlePointerDown);
		};
	}, [expanded]);

	return (
		<div className="ldp-events-period-picker" ref={containerRef}>
			<button
				aria-expanded={expanded}
				aria-haspopup="dialog"
				className="ldp-events-period-trigger"
				onClick={() => setExpanded(!expanded)}
				ref={triggerRef}
				type="button"
			>
				<ClayIcon symbol="date" />

				<span className="ldp-events-period-label">{label}</span>

				<ClayIcon symbol="caret-bottom" />
			</button>

			{expanded && (
				<div
					aria-label={title}
					className={classNames('ldp-events-period-panel', {
						'ldp-events-period-panel-left': alignment === 'left',
					})}
					role="dialog"
				>
					<div className="ldp-events-period-panel-header">
						<span className="ldp-events-period-panel-title">
							{title}
						</span>

						<div className="ldp-events-period-panel-steppers">
							<button onClick={onStepBack} type="button">
								<ClayIcon symbol="angle-left" />
							</button>

							<button onClick={onStepForward} type="button">
								<ClayIcon symbol="angle-right" />
							</button>
						</div>
					</div>

					{children(() => setExpanded(false))}
				</div>
			)}
		</div>
	);
}
