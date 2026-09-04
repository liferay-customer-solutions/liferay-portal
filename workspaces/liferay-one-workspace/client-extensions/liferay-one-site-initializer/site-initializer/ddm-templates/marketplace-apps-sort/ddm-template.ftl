<div>
	<#assign
		sortTermLabel = (sortDisplayContext.getSelectedSortTermDisplayContext().getLabel())!"relevance"
	/>

	<div class="form-group-item">
		<@clay["dropdown-menu"]
			cssClass="form-control form-control-select form-control-select-secondary"
			displayType="secondary"
			dropdownItems=sortDisplayContext.getActionDropdownItems()
			id="${namespace + 'sortSelectionDropdown'}"
			label=sortTermLabel
		/>
	</div>
</div>