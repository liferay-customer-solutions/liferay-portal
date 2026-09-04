<#assign
	commerceContext = renderRequest.getAttribute("COMMERCE_CONTEXT")
	scopeGroupId = themeDisplay.getScopeGroupId()
	specificationGroups = cpContentHelper.getCPOptionCategories(themeDisplay.getCompanyId())
/>

<#function langKey label>
	<#return label?lower_case?replace(" ", "-", "r")?replace("&", "and", "r")?replace(",", "", "r")?replace("/", "-", "r") />
</#function>

<#function getSpecificationValue specificationGroupKey specificationKey productId defaultValue="">
	<#local specificationGroup=specificationGroups?filter(specificationGroup -> specificationGroup.getKey() == specificationGroupKey) />
		<#if specificationGroup?has_content>
			<#local specifications=cpContentHelper.getCategorizedCPDefinitionSpecificationOptionValues(productId, specificationGroup?first.getCPOptionCategoryId()) />
			<#local specification=specifications?filter(productSpecification -> stringUtil.equals(productSpecification.getCPSpecificationOption().getKey(), specificationKey)) />

			<#return (specification?first.getValue(locale))!defaultValue />
		</#if>

		<#return defaultValue />
</#function>

<#function getSpecificationValues specificationGroupKey specificationKey productId>
	<#local specificationGroup=specificationGroups?filter(specificationGroup -> specificationGroup.getKey() == specificationGroupKey) />
		<#if specificationGroup?has_content>
			<#local specifications=cpContentHelper.getCategorizedCPDefinitionSpecificationOptionValues( productId, specificationGroup?first.getCPOptionCategoryId() ) />
			<#local specificationsFiltered=specifications?filter(productSpecification -> stringUtil.equals(productSpecification.getCPSpecificationOption().getKey(), specificationKey)) />

			<#if specificationsFiltered?has_content>
				<#return specificationsFiltered?map(item -> item.getValue(locale)) />
			</#if>
		</#if>
	<#return [] />
</#function>

<div class="card-grid">
	<div class="cards-container">
		<#if entries?has_content>
			<#list entries as entry>
				<#if entry?has_content>
					<#assign
						capabilities = getSpecificationValues("product-metadata", "liferay-products-capabilities", entry.getCPDefinitionId())
						categories = getSpecificationValues("product-metadata", "liferay-products-categories", entry.getCPDefinitionId())
						developerName = getSpecificationValue("product-metadata", "developer-name", entry.getCPDefinitionId())
						productDescription = stringUtil.shorten(htmlUtil.stripHtml(entry.getDescription()!""), 160, "...")
						productImage = cpContentHelper.getDefaultImageFileURL(-1, entry.getCPDefinitionId())
					/>

					<a class="card d-flex flex-column overflow-hidden text-dark text-decoration-none" href="${cpContentHelper.getFriendlyURL(entry, themeDisplay)}">
						<div class="align-items-center card-image-wrapper card-image-wrapper-liferay-product d-flex justify-content-center w-100">
							<img alt="${entry.getName()}" class="card-product-image" draggable="false" loading="lazy" src="${productImage}" />
						</div>

						<div class="card-body d-flex flex-column">
							<div class="d-flex flex-column">
								<h3 class="card-title">${entry.getName()}</h3>

								<#if developerName?has_content>
									<p class="card-subtitle">By ${developerName}</p>
								</#if>

								<#if categories?has_content>
									<div class="card-labels d-flex flex-wrap my-2">
										<#list categories as category>
											<span class="lo-product-label">${languageUtil.get(locale, langKey(category), category)}</span>
										</#list>
									</div>
								</#if>
							</div>

							<#if productDescription?has_content>
								<p class="card-description">${productDescription}</p>
							</#if>

							<#if capabilities?has_content>
								<ul class="card-bullet-list list-unstyled mb-0">
									<#list capabilities as tag>
										<#if tag?trim?has_content>
											<li class="align-items-start card-bullet-item d-flex small">
												<span class="align-items-center card-bullet-icon d-flex flex-shrink-0 justify-content-center text-primary">
													<svg fill="none" height="14" viewBox="0 0 14 14" width="14" xmlns="http://www.w3.org/2000/svg">
														<path d="M2.5 7L5.5 10L11.5 4" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" />
													</svg>
												</span>
												<span>${tag}</span>
											</li>
										</#if>
									</#list>
								</ul>
							</#if>
						</div>
					</a>
				</#if>
			</#list>
		</#if>
	</div>
</div>