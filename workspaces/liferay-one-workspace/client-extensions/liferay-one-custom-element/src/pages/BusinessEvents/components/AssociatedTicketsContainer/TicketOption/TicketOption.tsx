/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import ClayIcon from '@clayui/icon';
import {Word, translate} from '~/i18n';
import {ITicket} from '~/pages/BusinessEvents/types';

import './TicketOption.css';

interface IProps {
	primaryAction?: (value: ITicket) => void;
	secondaryAction?: (value: ITicket) => void;
	ticket: ITicket;
	type?: 'exhibit' | 'option' | 'selected';
}

const TicketOption: React.FC<IProps> = ({
	primaryAction = () => {},
	secondaryAction = () => {},
	ticket,
	type = 'exhibit',
}) => {
	return (
		<div className={`d-flex ticket-container ticket-container-${type}`}>
			<div
				className={`align-items-center d-flex justify-content-between overflow-hidden ticket-label ticket-label-${type} w-100`}
			>
				<Button
					borderless
					className="d-flex w-100"
					onClick={() => primaryAction(ticket)}
				>
					<div
						className="d-flex flex-grow-1 justify-content-between overflow-hidden ticket-text-container"
						title={translate(ticket.status as Word)}
					>
						<div className="overflow-hidden ticket-text">
							{`${ticket.ticketId} - ${ticket.subject}`}
						</div>

						<div
							className={`d-none ticket-link-icon ticket-link-icon-${type}`}
						>
							<ClayIcon symbol="shortcut" />
						</div>
					</div>

					<div className="align-items-center d-flex flex-shrink-0 justify-content-center px-3 text-capitalize ticket-status">
						<div className="overflow-hidden ticket-status-text">
							{translate(ticket.status as Word)}
						</div>
					</div>
				</Button>
			</div>

			<div
				className={`align-items-center d-none ticket-button ticket-button-${type}`}
			>
				<Button
					borderless
					className="ticket-button-icon"
					onClick={() => {
						secondaryAction(ticket);
					}}
				>
					<span>
						<ClayIcon
							symbol={type === 'option' ? 'shortcut' : 'times'}
						/>
					</span>
				</Button>
			</div>
		</div>
	);
};

export default TicketOption;
