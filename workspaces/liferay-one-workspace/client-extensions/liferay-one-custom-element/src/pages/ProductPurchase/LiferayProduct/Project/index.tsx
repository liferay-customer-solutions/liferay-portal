/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import ProductPurchase from '~/components/ProductPurchase/ProductPurchase';
import RadioCardList, {
	RadioOption,
} from '~/components/RadioCardList/RadioCardList';
import {useProperties} from '~/context/PropertiesContext';
import {useFetch} from '~/hooks/useFetch';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import {useAppPurchaseContext} from '~/pages/ProductPurchase/context/AppPurchaseContext';
import {Liferay} from '~/services/liferay/liferay';

import NoProjectAvailable from './NoProjectAvailable/NoProjectAvailable';

import type {APIResponse} from '~/types/api';
import type {SalesforceProject} from '~/types/salesforceProject';

type ProjectAPIItem = {
	externalReferenceCode: string;
	id: number;
	name: string;
};

const ProjectSelection = () => {
	const {contactSupportURL} = useProperties();

	const {salesforceProject, setSalesforceProject} = useAppPurchaseContext();

	const {
		actions: {nextStep, previousStep},
		selectedAccount,
	} = useProductPurchaseLayoutContext();

	const {data: projectsData, isLoading} = useFetch<
		APIResponse<ProjectAPIItem>
	>(selectedAccount?.id ? '/o/c/projects' : null, {
		params: {
			filter: `r_accountEntryToProject_accountEntryId eq '${selectedAccount.id}'`,
			pageSize: 200,
			sort: 'name:asc',
		},
	});

	if (isLoading) {
		return <ClayLoadingIndicator />;
	}

	const projects = projectsData?.items ?? [];

	if (!projects.length) {
		return <NoProjectAvailable />;
	}

	const continueButtonProps = {
		children: i18n.translate('continue'),
		disabled: isLoading || !salesforceProject,
		onClick: () => {
			nextStep();
		},
	};

	return (
		<ProductPurchase.Shell
			className="d-flex flex-column"
			footerProps={{
				backButtonProps: {onClick: previousStep},
				continueButtonProps,
			}}
			title={i18n.translate('project-selection')}
		>
			<span
				className="mb-4 secondary-text"
				dangerouslySetInnerHTML={{
					__html: i18n.sub('x-available-for-you', [
						'projects-and-resources',
						Liferay.ThemeDisplay.getUserEmailAddress(),
					]),
				}}
			/>
			<RadioCardList<ProjectAPIItem>
				contentList={projects.map((proj, index) => ({
					fullTitle: true,
					id: index,
					selected:
						proj.externalReferenceCode ===
						salesforceProject?.externalReferenceCode,
					title: (
						<span className="font-weight-semi-bold">
							{proj.name}
						</span>
					),
					value: proj,
				}))}
				leftRadio
				onSelect={(radioOption: RadioOption<ProjectAPIItem>) =>
					setSalesforceProject({
						externalReferenceCode:
							radioOption.value.externalReferenceCode,
						id: radioOption.value.id,
						name: radioOption.value.name,
					} as SalesforceProject)
				}
			/>

			<p className="secondary-text">
				{i18n.translate('not-seeing-a-specific-project')}

				<a
					className="font-weight-semi-bold ml-1"
					href={contactSupportURL}
					target="_blank"
				>
					{i18n.translate('contact-support')}
				</a>
			</p>
		</ProductPurchase.Shell>
	);
};

export default ProjectSelection;
