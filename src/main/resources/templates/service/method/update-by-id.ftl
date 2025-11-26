    <#if javadocFields?has_content>
    /**
     * Updates an existing {@link ${modelName}}
     *
    <#list javadocFields as field>
     * ${field}
    </#list>
     * @return updated {@link ${modelName}}
     */</#if>
    ${transactionalAnnotation}
    <#if cache>
    @CachePut(value = "${strippedModelName}", key = "#${idField}")
    </#if><#t>
    public ${modelName} updateById(<#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>) {

        final ${modelName} existing = this.getById(${idField});

    <#if fieldNamesWithoutId?has_content>
        existing.set${fieldNamesWithoutId[0]?cap_first}(${fieldNamesWithoutId[0]})<#list fieldNamesWithoutId[1..] as field>
            .set${field?cap_first}(${field})</#list>;</#if>

        LOGGER.info("Updating ${strippedModelName} with id {}", ${idField});

        return this.repository.saveAndFlush(existing);
    }