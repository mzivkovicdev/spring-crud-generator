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
    public ${modelName} updateById(<#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>) {

        final ${modelName} existing = this.getById(id);

    <#if fieldNamesWithoutId?has_content>
        existing.set${fieldNamesWithoutId[0]?cap_first}(${fieldNamesWithoutId[0]})<#list fieldNamesWithoutId[1..] as field>
            .set${field?cap_first}(${field})</#list>;</#if>

        LOGGER.info("Updating ${modelName} with id {}", id);

        return this.repository.saveAndFlush(existing);
    }