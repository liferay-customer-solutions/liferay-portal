/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import TicketOption from '~/pages/BusinessEvents/components/AssociatedTicketsContainer/TicketOption/TicketOption';
import {ITicket} from '~/types/ticket';

interface IProps {
	primaryAction?: (value: ITicket) => void;
	secondaryAction?: (value: ITicket) => void;
	tickets: ITicket[];
	type?: 'exhibit' | 'option' | 'selected';
}

const TicketList: React.FC<IProps> = ({
	primaryAction = () => {},
	secondaryAction = () => {},
	tickets,
	type = 'exhibit',
}) => {
	return (
		<>
			{tickets.map((ticket, index) => (
				<div className="mb-3" key={`${index}-${ticket.ticketId}`}>
					<TicketOption
						primaryAction={primaryAction}
						secondaryAction={secondaryAction}
						ticket={ticket}
						type={type}
					/>
				</div>
			))}
		</>
	);
};

export default TicketList;
