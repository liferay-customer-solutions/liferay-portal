<#attempt>
	<#assign
		activeLayout = themeDisplay.getLayout()

		activePlid = activeLayout.getPlid()
	/>

<#recover>
	<#assign activePlid = 0 />
</#attempt>

<#assign
	navigationIcons = {
		"Activation": "cog",
		"Administration": "users",
		"Getting Started": "analytics",
		"Team Members": "list",
		"Using One Liferay": "document-text"
	}
/>

<nav aria-label="One Liferay Help" class="lo-support-nav">
	<ul class="list-unstyled mb-0">
		<#attempt>
			<#list entries![] as navigationMenuItem>
				<#assign
					navigationMenuItemName = navigationMenuItem.getName()
					navigationMenuItemPlid = (navigationMenuItem.getLayout().getPlid())!0
					navigationMenuItemURL = navigationMenuItem.getRegularURL()

					navigationIconSymbol = (navigationIcons[navigationMenuItemName])!"circle"
					navigationMenuItemActive = (activePlid > 0) && (navigationMenuItemPlid == activePlid)
				/>

				<#if navigationMenuItemURL?has_content>
					<li>
						<a
							class="align-items-center d-flex lo-support-nav-link<#if navigationMenuItemActive> active</#if>"
							href="${navigationMenuItemURL}"
							${navigationMenuItem.getTarget()}
						>
							<span class="align-items-center d-flex lo-support-nav-icon">
								<svg class="lexicon-icon" role="presentation" viewBox="0 0 512 512"><use xlink:href="/o/admin-theme/images/clay/icons.svg#${navigationIconSymbol}" /></svg>
							</span>
							<span class="lo-support-nav-label">${navigationMenuItemName}</span>
						</a>
					</li>
				</#if>
			</#list>
		<#recover>
		</#attempt>
	</ul>
</nav>