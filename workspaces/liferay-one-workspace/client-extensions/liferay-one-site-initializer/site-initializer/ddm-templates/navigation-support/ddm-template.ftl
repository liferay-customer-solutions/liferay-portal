<#--
Vertical sidebar for the One Liferay Help section. Renders the support
navigation menu the tag supplies as `entries`, already filtered by each menu
item's own permissions, so a topic whose page the current user may not view
never reaches this template.
-->

<#assign activePlid = 0 />

<#attempt>
	<#assign activePlid = themeDisplay.getLayout().getPlid() />
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
				/>

				<#assign
					navigationIconSymbol = (navigationIcons[navigationMenuItemName])!"circle"
					navigationMenuItemActive = (activePlid > 0) && (navigationMenuItemPlid == activePlid)
				/>

				<li>
					<a
						class="align-items-center d-flex lo-support-nav-link<#if navigationMenuItemActive> active</#if>"
						href="${navigationMenuItemURL?has_content?then(navigationMenuItemURL, "#")}"
						<#if ((navigationMenuItem.getTarget())!"")?contains("_blank")>target="_blank"</#if>
					>
						<span class="align-items-center d-flex lo-support-nav-icon">
							<svg class="lexicon-icon" role="presentation" viewBox="0 0 512 512"><use xlink:href="/o/admin-theme/images/clay/icons.svg#${navigationIconSymbol}" /></svg>
						</span>
						<span class="lo-support-nav-label">${navigationMenuItemName}</span>
					</a>
				</li>
			</#list>
		<#recover>
		</#attempt>
	</ul>
</nav>

<style>
	.lo-support-nav {
		background-color: var(--color-neutral-1, #f7f8f9);
		border-radius: 12px;
		padding: 0.75rem;
	}

	.lo-support-nav-link {
		border-radius: 8px;
		color: var(--color-neutral-9, #272833);
		gap: 0.75rem;
		padding: 0.625rem 0.875rem;
		text-decoration: none;
		transition: background-color 0.1s ease-in-out, color 0.1s ease-in-out;
	}

	.lo-support-nav-link + .lo-support-nav-link,
	.lo-support-nav li + li .lo-support-nav-link {
		margin-top: 0.25rem;
	}

	.lo-support-nav-link:focus,
	.lo-support-nav-link:hover {
		background-color: var(--color-neutral-2, #e7e7ed);
		color: var(--color-neutral-9, #272833);
		text-decoration: none;
	}

	.lo-support-nav-link.active {
		background-color: rgba(11, 95, 255, 0.1);
		color: var(--color-brand-primary, #0b5fff);
		font-weight: var(--font-weight-semi-bold, 600);
	}

	.lo-support-nav-icon {
		color: var(--color-neutral-6, #6b6c7e);
		flex-shrink: 0;
	}

	.lo-support-nav-link.active .lo-support-nav-icon {
		color: var(--color-brand-primary, #0b5fff);
	}

	.lo-support-nav-icon .lexicon-icon {
		height: 1rem;
		margin-top: 0;
		width: 1rem;
	}
</style>