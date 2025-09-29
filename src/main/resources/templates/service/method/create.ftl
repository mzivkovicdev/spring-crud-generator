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
    <#if cache>@CachePut(value = "${strippedModelName}", key = "#result.${idField}")</#if>
    public ${modelName} create(${inputArgs}) {

        LOGGER.info("Creating new ${strippedModelName}");

        return this.repository.saveAndFlush(new ${modelName}(${fieldNames}));
    }