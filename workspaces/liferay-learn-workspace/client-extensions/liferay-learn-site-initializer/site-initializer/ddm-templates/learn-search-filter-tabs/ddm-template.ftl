<#if entries?has_content>
	<#assign totalCount = 0 />

	<#list assetCategoriesSearchFacetDisplayContext.getBucketDisplayContexts() as bucket>
		<#assign totalCount = totalCount + bucket.getCount() />
	</#list>

	<ul class="list-unstyled tab-list">
		<li class="facet-value">
			<@clay.button
				cssClass="facet-clear btn-unstyled ${assetCategoriesSearchFacetDisplayContext.isNothingSelected()?then('selected-tab-btn', 'facet-term-unselected')}"
				displayType="link"
				onClick="Liferay.Search.FacetUtil.clearSelections(event);"
				value="clear"
			>
				<span class="term-text">${languageUtil.get(locale, "all-results", "All Results")}</span>

				<#if entry.isFrequencyVisible()>
					<span class="term-count">${totalCount}</span>
				</#if>
			</@clay.button>
		</li>

		<#list entries as entry>
			<li class="facet-value">
				<@clay.button
					cssClass="facet-term btn-unstyled term-name"
					data\-term\-id="${entry.getFilterValue()}"
					disabled="true"
					displayType="link"
					onClick="${namespace}updateSelection(event)"
				>
					<span class="term-text">${htmlUtil.escape(entry.getBucketText())}</span>

					<#if entry.isFrequencyVisible()>
						<span class="term-count">${entry.getFrequency()}</span>
					</#if>
				</@clay.button>
			</li>
		</#list>
	</ul>
</#if>

<script>
	document.addEventListener('DOMContentLoaded', function() {
		const urlParams = new URLSearchParams(window.location.search);
		const urlParamValue = urlParams.get('resource-type');

		const buttons = document.querySelectorAll('.facet-value .facet-term');

		buttons.forEach(function(button) {
			const dataTermId = button.getAttribute('data-term-id');

			if (dataTermId === urlParamValue) {
				button.classList.add('selected-tab-btn');
			}
		});
	});
	
	function ${namespace}updateSelection(event) {
		const form = event.currentTarget.form;

		if (!form) {
			return;
		}

		Liferay.Search.FacetUtil.selectTerms(form, []);
		Liferay.Search.FacetUtil.changeSelection(event);
	}	
</script>