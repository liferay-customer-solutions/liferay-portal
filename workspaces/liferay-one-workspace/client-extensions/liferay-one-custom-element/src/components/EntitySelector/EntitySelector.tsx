/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown, {Align} from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import {ReactNode, useCallback, useRef, useState} from 'react';
import i18n from '~/i18n';

import './EntitySelector.css';

const LABEL_COLOR = 'var(--color-neutral-7)';
const VALUE_COLOR = 'var(--color-neutral-10)';

export type SelectorItem = {
	icon?: ReactNode;
	id: string;
	name: string;
	subtitle?: string;
};

type EntitySelectorProps = {
	ariaLabel: string;
	badge?: string;
	emptyLabel?: string;
	items: SelectorItem[];
	label: string;
	loading?: boolean;
	name: string;
	onSearchChange: (value: string) => void;
	onSelect: (id: string) => void;
	readOnly?: boolean;
	searchValue: string;
	selectedId?: string;
	triggerIcon: ReactNode;
	variant?: 'compact' | 'rich';
};

export default function EntitySelector({
	ariaLabel,
	badge,
	emptyLabel,
	items,
	label,
	loading = false,
	name,
	onSearchChange,
	onSelect,
	readOnly = false,
	searchValue,
	selectedId,
	triggerIcon,
	variant = 'compact',
}: EntitySelectorProps) {
	const [active, setActive] = useState(false);
	const [menuWidth, setMenuWidth] = useState<number>();
	const triggerElementRef = useRef<HTMLButtonElement | null>(null);

	const setTriggerElement = useCallback((node: HTMLButtonElement | null) => {
		triggerElementRef.current = node;
	}, []);

	function handleActiveChange(next: boolean) {
		if (next && triggerElementRef.current) {
			setMenuWidth(
				triggerElementRef.current.getBoundingClientRect().width
			);
		}

		setActive(next);
	}

	function handleSelect(id: string) {
		setActive(false);

		onSelect(id);
	}

	const caretIcon = readOnly ? null : (
		<ClayIcon
			style={{color: LABEL_COLOR, flexShrink: 0, marginLeft: 'auto'}}
			symbol="caret-bottom"
		/>
	);

	const trigger =
		variant === 'rich' ? (
			<button
				aria-label={ariaLabel}
				className="align-items-center bg-transparent border-0 d-flex p-0 text-left w-100"
				disabled={readOnly}
				ref={setTriggerElement}
				style={{gap: '0.75rem'}}
				type="button"
			>
				{triggerIcon}

				<span
					className="d-flex flex-column flex-fill"
					style={{minWidth: 0}}
				>
					<span
						style={{
							color: LABEL_COLOR,
							fontSize: '0.6875rem',
							fontWeight: 600,
							letterSpacing: '0.06em',
							textTransform: 'uppercase',
						}}
					>
						{label}
					</span>

					<span
						className="align-items-center d-flex w-100"
						style={{gap: 'var(--spacer-1)'}}
					>
						<span
							className="text-truncate"
							style={{
								color: VALUE_COLOR,
								fontSize: '0.9375rem',
								fontWeight: 700,
								minWidth: 0,
							}}
							title={name}
						>
							{name}
						</span>

						{caretIcon}
					</span>

					{badge && (
						<span
							style={{
								alignSelf: 'flex-start',
								backgroundColor: 'var(--color-neutral-2)',
								borderRadius:
									'var(--border-radius-sm, 0.25rem)',
								color: 'var(--color-neutral-7)',
								fontSize: '0.6875rem',
								fontWeight: 700,
								marginTop: 'var(--spacer-1)',
								padding: '0.0625rem 0.5rem',
							}}
						>
							{badge}
						</span>
					)}
				</span>
			</button>
		) : (
			<button
				aria-label={ariaLabel}
				className="align-items-center border-0 d-flex entity-selector-trigger"
				disabled={readOnly}
				ref={setTriggerElement}
				style={{
					backgroundColor: 'var(--color-neutral-1)',
					borderRadius: 'var(--border-radius-lg, 0.625rem)',
					gap: 'var(--spacer-2)',
					padding: '0.375rem 0.75rem 0.375rem 0.375rem',
					width: '14rem',
				}}
				type="button"
			>
				{triggerIcon}

				<span
					className="text-truncate"
					style={{
						color: VALUE_COLOR,
						fontWeight: 600,
						minWidth: 0,
					}}
					title={name}
				>
					{name}
				</span>

				{caretIcon}
			</button>
		);

	if (readOnly) {
		return trigger;
	}

	return (
		<ClayDropDown
			active={active}
			alignmentPosition={Align.BottomLeft}
			menuElementAttrs={{
				className: 'entity-selector-menu',
				style: menuWidth
					? {maxWidth: 'none', width: menuWidth}
					: undefined,
			}}
			onActiveChange={handleActiveChange}
			trigger={trigger}
		>
			<ClayDropDown.Search
				onChange={(value) => onSearchChange(value)}
				placeholder={i18n.translate('search')}
				value={searchValue}
			/>

			<ClayDropDown.ItemList>
				{loading && (
					<ClayDropDown.Item disabled>
						{i18n.translate('loading')}
					</ClayDropDown.Item>
				)}

				{!loading && !items.length && (
					<ClayDropDown.Item disabled>
						{emptyLabel ?? i18n.translate('no-results-found')}
					</ClayDropDown.Item>
				)}

				{!loading &&
					items.map((item) => (
						<ClayDropDown.Item
							active={item.id === selectedId}
							key={item.id}
							onClick={() => handleSelect(item.id)}
						>
							<span
								className="align-items-center d-flex"
								style={{gap: 'var(--spacer-2)', minWidth: 0}}
							>
								{item.icon}

								<span
									className="d-flex flex-column"
									style={{minWidth: 0}}
								>
									<span
										className="text-truncate"
										style={{fontWeight: 600}}
										title={item.name}
									>
										{item.name}
									</span>

									{item.subtitle && (
										<span
											className="text-truncate"
											style={{
												color: LABEL_COLOR,
												fontSize: '0.75rem',
												textTransform: 'capitalize',
											}}
										>
											{item.subtitle}
										</span>
									)}
								</span>
							</span>
						</ClayDropDown.Item>
					))}
			</ClayDropDown.ItemList>
		</ClayDropDown>
	);
}
