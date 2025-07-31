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
    public ${modelName} create(${inputArgs}) {

        LOGGER.info("Creating new ${modelName}");

        return this.repository.saveAndFlush(new ${modelName}(${fieldNames}));
    }