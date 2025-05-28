/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {LearnHoverIcon} from '~/assets/LearnHoverIcon';
import {LearnIcon} from '~/assets/LearnIcon';
import {MyTicketsHoverIcon} from '~/assets/MyTicketsHoverIcon';
import {MyTicketsIcon} from '~/assets/MyTicketsIcon';
import {ReleaseNotesHoverIcon} from '~/assets/ReleaseNotesHoverIcon';
import {ReleaseNotesIcon} from '~/assets/ReleaseNotesIcon';
import {SecurityReportsHoverIcon} from '~/assets/SecurityReportsHoverIcon';
import {SecurityReportsIcon} from '~/assets/SecurityReportsIcon';
import {SubmitATicketHoverIcon} from '~/assets/SubmitATicketHoverIcon';
import {SubmitATicketIcon} from '~/assets/SubmitATicketIcon';
import i18n from '~/utils/I18n';
import routerPath from '~/utils/routerPath';

import TileCard from './components/TileCard';

import './TilesCard.css';

interface TilesCardProps {
	atLeastOneProject: boolean;
	isLogged: boolean;
}

const TilesCard = ({atLeastOneProject, isLogged}: TilesCardProps) => {
	const pageRoutes = routerPath();

	const tileCards = [
		{
			hoverSvgIcon: <LearnHoverIcon />,
			link: 'https://learn.liferay.com/',
			show: true,
			subtitle: i18n.translate('find-documentation-tutorials-and-guides'),
			svgIcon: <LearnIcon />,
			title: i18n.translate('liferay-learn'),
		},
		{
			hoverSvgIcon: <SecurityReportsHoverIcon />,
			link: pageRoutes.securityVulnerabilities(),
			show: isLogged,
			subtitle: i18n.translate(
				'latest-vulnerability-alerts-and-incident-updates'
			),
			svgIcon: <SecurityReportsIcon />,
			title: i18n.translate('security-reports'),
		},
		{
			hoverSvgIcon: <ReleaseNotesHoverIcon />,
			link: pageRoutes.releaseNotes(),
			show: true,
			subtitle: i18n.translate(
				'review-version-highlights-and-technical-details'
			),
			svgIcon: <ReleaseNotesIcon />,
			title: i18n.translate('release-notes'),
		},
		{
			hoverSvgIcon: <MyTicketsHoverIcon />,
			link: '',
			show: atLeastOneProject && isLogged,
			subtitle: i18n.translate('track-open-and-closed-support-requests'),
			svgIcon: <MyTicketsIcon />,
			title: i18n.translate('my-tickets'),
		},
		{
			hoverSvgIcon: <SubmitATicketHoverIcon />,
			link: '',
			show: atLeastOneProject && isLogged,
			subtitle: i18n.translate(
				'need-help-log-an-issue-or-ask-a-question'
			),
			svgIcon: <SubmitATicketIcon />,
			title: i18n.translate('submit-a-ticket'),
		},
	];

	return (
		<div className="mt-4 tiles-grid-container">
			{tileCards
				.filter((card) => card.show)
				.map((card, index) => (
					<TileCard
						hoverSvgIcon={card.hoverSvgIcon}
						key={index}
						link={card.link}
						subtitle={card.subtitle}
						svgIcon={card.svgIcon}
						title={card.title}
					/>
				))}
		</div>
	);
};

export default TilesCard;
