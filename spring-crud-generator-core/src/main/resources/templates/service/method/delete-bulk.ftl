    /**
     * Bulk deletes {@link ${modelName}} by IDs.
     *
     * @param ids list of ${idType} identifiers
     */
    ${transactionalAnnotation}
    <#if cache>
    @CacheEvict(value = "${modelName?uncap_first}", allEntries = true)
    </#if><#t>
    public void bulkDelete(final List<${idType}> ids) {
        ArgumentVerifier.verifyNotEmpty(ids);

        LOGGER.info("Bulk deleting {} ${strippedModelName} records", ids.size());

        ids.forEach(this::deleteById);
    }