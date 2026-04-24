    
    /**
     * Bulk creates new {@link ${modelName}}.
     *
     * @param ${strippedModelName}s list of ${strippedModelName?uncap_first} 
     * @return the created {@link List} of {@link ${modelName}}
     */
    ${transactionalAnnotation}
    <#if cache>
    @CacheEvict(value = "${modelName?uncap_first}", allEntries = true)
    </#if><#t>
    public List<${modelName}> bulkCreate(final List<${modelName}> ${strippedModelName}s) {
        ArgumentVerifier.verifyNotEmpty(${strippedModelName}s);

        LOGGER.info("Creating {} ${strippedModelName} records", ${strippedModelName}s.size());

        return this.repository.saveAllAndFlush(${strippedModelName}s);
    }
