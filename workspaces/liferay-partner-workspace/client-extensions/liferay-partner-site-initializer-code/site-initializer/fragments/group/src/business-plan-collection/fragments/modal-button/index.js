const modalButton = fragmentElement.querySelector('#modalButton_' + fragmentEntryLinkNamespace);
const url = configuration.url;

const objectDefinitionId = modalButton.getAttribute('data-object-definition-id');
const objectEntryId = modalButton.getAttribute('data-object-entry-id');

const parseAndReplaceVariables = (string, objectEntry) => {
	const regex = /\$\{([\w.]+)\}/g;

	return string.replace(regex, (match, variableName) => {
		const value = objectEntry[variableName];

		return value !== undefined ? value : match;
	});
}

modalButton.onclick = async () => {
	const objectDefinitionResponse = await fetch(
		'/o/object-admin/v1.0/object-definitions/' + objectDefinitionId,
		{
			headers: {
				'content-type': 'application/json',
				'x-csrf-token': Liferay.authToken,
			},
			method: 'GET',
		}
	);

	const objectDefinition = await objectDefinitionResponse.json();

	const objectEntryResponse = await fetch(
		objectDefinition.restContextPath + '/' + objectEntryId,
		{
			headers: {
				'content-type': 'application/json',
				'x-csrf-token': Liferay.authToken,
			},
			method: 'GET',
		}
	);

	const objectEntry = await objectEntryResponse.json();

	Liferay.Util.openWindow({
		dialog: {
			destroyOnHide: true,
			modal: true,
			width: 800,
		},
		dialogIframe: {
			bodyCssClass: 'dialog-with-footer',
		},
		toolbars: {
			footer: [
				{
					cssClass: 'btn btn-secondary',
					discardDefaultButtonCssClasses: true,
					label: Liferay.Language.get('cancel'),
					on: {
						click() {
							const form = document.querySelector('form')[0];

							if (form) {
								form.reset();
							}

							dialog.destroy();
						},
					},
				},
				{
					cssClass: 'btn btn-primary',
					discardDefaultButtonCssClasses: true,
					label: Liferay.Language.get('save'),
					on: {
						click() {
							const form = document.querySelector('form')[0];

							if (form) {
								submitForm(form);
							}

							dialog.hide();
						},
					},
				},
			],
			header: [],
		},
		uri: window.location.origin + parseAndReplaceVariables(url, objectEntry),
	});
}