    
    /**
     * Get all {@link ${modelName}} with pagination by page number and page size.
     *
     * @param pageNumber The page number.
     * @param pageSize The page size.
     <#if sortEnabled?? && sortEnabled>
     * @param sortBy Optional sort field.
     * @param sortDirection Optional sort direction (ASC or DESC).
     </#if><#t>
     * @return A page of {@link ${modelName}}.
     */
    public Page<${modelName}> getAll(final Integer pageNumber, final Integer pageSize<#if sortEnabled?? && sortEnabled>,
            final String sortBy, final String sortDirection</#if>) {

        <#if sortEnabled?? && sortEnabled>
        if (sortBy == null || sortBy.isBlank()) {
            return repository.<#if mongoSoftDelete?? && mongoSoftDelete>findAllByDeletedFalse<#else>findAll</#if>(PageRequest.of(pageNumber, pageSize));
        }

        if (!isAllowedSortField(sortBy)) {
            throw new IllegalArgumentException(
                "Invalid sortBy '" + sortBy + "' for ${modelName}. Allowed values are: ${sortAllowedFieldsCsv}."
            );
        }

        final String resolvedSortDirection = (sortDirection == null || sortDirection.isBlank())
                ? "${sortDefaultDirection}" : sortDirection;
        final Direction direction;
        try {
            direction = Direction.fromString(resolvedSortDirection);
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Invalid sortDirection '" + resolvedSortDirection + "' for ${modelName}. Allowed values are: ASC, DESC."
            );
        }

        final Sort sort = Sort.by(direction, sortBy);
        return repository.<#if mongoSoftDelete?? && mongoSoftDelete>findAllByDeletedFalse<#else>findAll</#if>(PageRequest.of(pageNumber, pageSize, sort));
        <#else>
        return repository.<#if mongoSoftDelete?? && mongoSoftDelete>findAllByDeletedFalse<#else>findAll</#if>(PageRequest.of(pageNumber, pageSize));
        </#if>
    }
    <#if sortEnabled?? && sortEnabled>

    private boolean isAllowedSortField(final String sortField) {
        return <#list sortAllowedFields as allowedField>"${allowedField}".equals(sortField)<#if allowedField_has_next> || </#if></#list>;
    }
    </#if><#t>
