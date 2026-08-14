/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {useRef, useState} from 'react';
import {translate} from '~/i18n';
import {
	PROJECT_ROLE_ERCS,
	getProjectRoleLabel,
} from '~/pages/MyAccount/ProjectMembers/projectRoles';

import '~/pages/MyAccount/ProjectMembers/ProjectMembers.css';

type PermissionsSelectProps = {
	availableDesignations: string[];
	designations: string[];
	onRoleChange: (roleExternalReferenceCode: string) => void;
	onToggleDesignation: (designation: string) => void;
	roleExternalReferenceCode: string;
};

const PermissionsSelect = ({
	availableDesignations,
	designations,
	onRoleChange,
	onToggleDesignation,
	roleExternalReferenceCode,
}: PermissionsSelectProps) => {
	const [active, setActive] = useState(false);
	const [menuWidth, setMenuWidth] = useState<number>();
	const containerRef = useRef<HTMLDivElement>(null);

	const triggerLabel = [
		roleExternalReferenceCode
			? getProjectRoleLabel(roleExternalReferenceCode)
			: null,
		...designations,
	]
		.filter(Boolean)
		.join(', ');

	return (
		<div ref={containerRef}>
			<ClayDropDown
				active={active}
				className="project-permissions-role-dropdown"
				closeOnClick={false}
				menuElementAttrs={{
					className: 'project-permissions-role-menu',
					style: {width: menuWidth},
				}}
				onActiveChange={(nextActive) => {
					if (nextActive) {
						setMenuWidth(containerRef.current?.offsetWidth);
					}

					setActive(nextActive);
				}}
				trigger={
					<button
						className="align-items-center d-flex form-control justify-content-between project-permissions-role-trigger"
						type="button"
					>
						<span>
							{triggerLabel || translate('select-a-role')}
						</span>

						<ClayIcon symbol="caret-bottom" />
					</button>
				}
			>
				<ClayDropDown.ItemList>
					<li className="dropdown-subheader">
						{translate('project-roles')}
					</li>

					{PROJECT_ROLE_ERCS.map(
						(projectRoleExternalReferenceCode) => (
							<ClayDropDown.Item
								key={projectRoleExternalReferenceCode}
							>
								<ClayCheckbox
									checked={
										roleExternalReferenceCode ===
										projectRoleExternalReferenceCode
									}
									label={getProjectRoleLabel(
										projectRoleExternalReferenceCode
									)}
									onChange={() =>
										onRoleChange(
											projectRoleExternalReferenceCode
										)
									}
								/>
							</ClayDropDown.Item>
						)
					)}

					{!!availableDesignations.length && (
						<>
							<ClayDropDown.Divider />

							<li className="dropdown-subheader">
								{translate('cloud-contacts')}
							</li>

							{availableDesignations.map((designation) => (
								<ClayDropDown.Item key={designation}>
									<ClayCheckbox
										checked={designations.includes(
											designation
										)}
										label={designation}
										onChange={() =>
											onToggleDesignation(designation)
										}
									/>
								</ClayDropDown.Item>
							))}
						</>
					)}
				</ClayDropDown.ItemList>
			</ClayDropDown>
		</div>
	);
};

export default PermissionsSelect;
