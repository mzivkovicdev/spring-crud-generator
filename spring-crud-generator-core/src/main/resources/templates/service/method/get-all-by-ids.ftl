
<#if generateJavaDoc?? && generateJavaDoc>
    /**
    * Get all ${modelName} by provided IDs
    *
    * @param ids {@link List} of ${idDescription}
    * @return A {@link List} of found {@link ${modelName}}.
    */</#if>
    public List<${modelName}> getAllByIds(final List<${idType}> ids) {

        return this.repository.findAllById(ids);
    }
    