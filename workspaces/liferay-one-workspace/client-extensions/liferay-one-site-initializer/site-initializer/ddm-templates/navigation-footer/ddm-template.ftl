<#attempt>
	<#list entries![] as footerNavSection>
		<#assign footerNavItems = footerNavSection.getChildren() />

		<#if footerNavItems?has_content>
			<section class="c-mb-10 c-mb-lg-0 c-mb-md-10 c-px-0 col-lg-2 col-md-4 section-title">
				<h3 class="c-mb-4 font-weight-semi-bold">${footerNavSection.getName()}</h3>

				<ul class="c-p-0 d-flex flex-column list-unstyled text-decoration-none">
					<#list footerNavItems as footerNavItem>
						<li<#if !footerNavItem?is_last> class="c-mb-3"</#if>>
							<a class="c-p-0" href="${footerNavItem.getRegularURL()}" ${footerNavItem.getTarget()}>${footerNavItem.getName()}</a>
						</li>
					</#list>
				</ul>
			</section>
		</#if>
	</#list>
<#recover>
</#attempt>
