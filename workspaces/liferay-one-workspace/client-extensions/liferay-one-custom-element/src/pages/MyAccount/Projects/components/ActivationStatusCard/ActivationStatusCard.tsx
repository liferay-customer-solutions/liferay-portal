/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import {useModal} from '@clayui/modal';
import {useState} from 'react';
import Button from '~/components/Button/Button';
import Modal from '~/components/Modal/Modal';
import {useProject} from '~/context/ProjectContext';
import {useProjectEnvironments} from '~/hooks/useProjectEnvironments';
import {Word, translate} from '~/i18n';
import {filterEnvironmentsByProject} from '~/pages/MyAccount/Projects/utils/filterEnvironmentsByProject';
import {formatDate} from '~/utils/dateUtils';

import useHasActivationPermission from '../../hooks/useHasActivationPermission';
import {getCloudConsoleURL} from '../../utils/cloudActivationFields';
import CloudActivationModal from '../CloudActivationModal/CloudActivationModal';

import './ActivationStatusCard.css';

import type {ProjectContract} from '~/hooks/useProjectCommerce';

import type {CloudActivationProfile} from '../../utils/cloudActivationFields';
import type {EnvironmentProfile} from '../../utils/resolveEnvironmentProfile';

type StatusConfig = {
	consoleLinkLabel: Word;
	icon: string;
	offering: string;
	subtitle: Word;
	title: Word;
};

const CONFIG_BY_ENVIRONMENT_PROFILE: Record<
	CloudActivationProfile,
	StatusConfig
> = {
	'analytics-cloud': {
		consoleLinkLabel: 'go-to-analytics-cloud',
		icon: 'analytics',
		offering: 'Analytics Cloud',
		subtitle:
			'almost-there-setup-analytics-cloud-by-finishing-the-activation-form',
		title: 'analytics-cloud-activation',
	},
	'paas': {
		consoleLinkLabel: 'go-to-product-console',
		icon: 'cloud',
		offering: 'PaaS',
		subtitle:
			'almost-there-setup-liferay-paas-by-finishing-the-activation-form',
		title: 'liferay-paas-activation',
	},
	'saas': {
		consoleLinkLabel: 'go-to-liferay-saas',
		icon: 'cloud',
		offering: 'SaaS',
		subtitle:
			'almost-there-setup-liferay-saas-by-finishing-the-activation-form',
		title: 'liferay-saas-activation',
	},
};

const FALLBACK_CLOUD_ACTIVATION_PROFILE: CloudActivationProfile =
	'analytics-cloud';

const MODAL_SUBTITLE_BY_PROFILE: Record<CloudActivationProfile, Word> = {
	'analytics-cloud':
		'we-ll-need-a-few-details-to-finish-creating-your-analytics-cloud-workspace',
	'paas': 'we-ll-need-a-few-details-to-finish-building-your-liferay-paas-environment',
	'saas': 'we-ll-need-a-few-details-to-finish-creating-your-liferay-saas-workspace',
};

const MODAL_TITLE_BY_PROFILE: Record<CloudActivationProfile, Word> = {
	'analytics-cloud': 'set-up-analytics-cloud',
	'paas': 'set-up-liferay-paas',
	'saas': 'set-up-liferay-saas',
};

const STATUS_LABEL: Record<
	string,
	{displayType: 'secondary' | 'success' | 'warning'; label: Word}
> = {
	active: {displayType: 'success', label: 'active'},
	deactivated: {displayType: 'secondary', label: 'deactivated'},
	expired: {displayType: 'warning', label: 'expired'},
	pending: {displayType: 'warning', label: 'in-progress'},
};

function isCloudActivationProfile(
	environmentProfile: EnvironmentProfile | undefined
): environmentProfile is CloudActivationProfile {
	return (
		environmentProfile === 'analytics-cloud' ||
		environmentProfile === 'paas' ||
		environmentProfile === 'saas'
	);
}

type ActivationStatusCardProps = {
	contract?: ProjectContract;
	environmentProfile?: EnvironmentProfile;
	productName: string;
};

export default function ActivationStatusCard({
	contract,
	environmentProfile,
	productName,
}: ActivationStatusCardProps) {
	const {projectId, projects} = useProject();
	const {
		environments,
		loading: environmentsLoading,
		mutate,
	} = useProjectEnvironments();
	const {hasActivationPermission, loading: activationPermissionLoading} =
		useHasActivationPermission(projectId);

	const [isModalOpen, setIsModalOpen] = useState(false);

	const {observer, onClose} = useModal({
		onClose: () => {
			setIsModalOpen(false);
			mutate();
		},
	});

	const cloudActivationProfile = isCloudActivationProfile(environmentProfile)
		? environmentProfile
		: undefined;

	const copyProfile =
		cloudActivationProfile ?? FALLBACK_CLOUD_ACTIVATION_PROFILE;

	const config = CONFIG_BY_ENVIRONMENT_PROFILE[copyProfile];

	const environmentOffering = cloudActivationProfile
		? CONFIG_BY_ENVIRONMENT_PROFILE[cloudActivationProfile].offering
		: undefined;

	const [environment] = environmentOffering
		? filterEnvironmentsByProject(
				projectId,
				environments.filter(
					(current) => current.offering === environmentOffering
				)
			)
		: [];

	const statusLoading = activationPermissionLoading || environmentsLoading;

	const status = STATUS_LABEL[environment?.status ?? ''];

	const consoleURL =
		cloudActivationProfile && environment
			? getCloudConsoleURL(cloudActivationProfile, environment)
			: '';

	const projectName =
		projects.find((project) => project.externalReferenceCode === projectId)
			?.name ?? '';

	return (
		<div className="mt-3">
			<h2>{translate(config.title)}</h2>

			<p className="text-neutral-7">{translate(config.subtitle)}</p>

			<div className="activation-status-card">
				<span className="activation-status-card-icon">
					<ClayIcon symbol={config.icon} />
				</span>

				<div className="flex-grow-1">
					<span className="fw-bold">{productName}</span>

					{!!(contract?.startDate || contract?.endDate) && (
						<p className="list-card-subtext m-0">
							{formatDate(contract?.startDate)} -{' '}
							{formatDate(contract?.endDate)}
						</p>
					)}

					{status?.label === 'active' && consoleURL && (
						<a
							className="link-primary"
							href={consoleURL}
							rel="noopener"
							target="_blank"
						>
							{translate(config.consoleLinkLabel)}
						</a>
					)}
				</div>

				{(!!status || !statusLoading) && (
					<ClayLabel displayType={status?.displayType ?? 'secondary'}>
						{translate(status?.label ?? 'not-activated')}
					</ClayLabel>
				)}
			</div>

			{!status && statusLoading && (
				<div className="py-2 text-neutral-7">
					{translate('loading')}
				</div>
			)}

			{!status &&
				!statusLoading &&
				hasActivationPermission &&
				!!cloudActivationProfile && (
					<Button
						displayType="link"
						onClick={() => setIsModalOpen(true)}
					>
						{translate('finish-activation')}
					</Button>
				)}

			<Modal
				observer={observer}
				subtitle={translate(MODAL_SUBTITLE_BY_PROFILE[copyProfile])}
				title={translate(MODAL_TITLE_BY_PROFILE[copyProfile])}
				visible={isModalOpen}
			>
				<CloudActivationModal
					onClose={onClose}
					profile={copyProfile}
					projectExternalReferenceCode={projectId}
					projectName={projectName}
				/>
			</Modal>
		</div>
	);
}
