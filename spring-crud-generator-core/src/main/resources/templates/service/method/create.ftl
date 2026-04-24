    <#assign notNullArgs = notNullArgs![]>
    <#assign notEmptyArgs = notEmptyArgs![]>
    <#assign notBlankArgs = notBlankArgs![]>
    <#if javadocFields?has_content>
    /**
     * Creates a new {@link ${modelName}}.
     *
     <#list javadocFields as docField>
     * ${docField}
     </#list>
     * @return the created {@link ${modelName}}
     */</#if>
    ${transactionalAnnotation}
    <#if cache>
    @CachePut(value = "${modelName?uncap_first}", key = "#result.${idField}")
    </#if><#t>
    public ${modelName} create(${inputArgs}) {
        <#if notNullArgs?has_content>
        ArgumentVerifier.verifyNotNull(${notNullArgs?join(", ")});
        </#if>
        <#if notBlankArgs?has_content>
        ArgumentVerifier.verifyNotBlank(${notBlankArgs?join(", ")});
        </#if>
        <#if notEmptyArgs?has_content>
        ArgumentVerifier.verifyNotEmpty(${notEmptyArgs?join(", ")});
        </#if>
        
        LOGGER.info("Creating new ${strippedModelName}");

        return this.repository.saveAndFlush(new ${modelName}(${fieldNames}));
    }
