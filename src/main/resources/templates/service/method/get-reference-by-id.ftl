
<#if generateJavaDoc?? && generateJavaDoc>
    /**
    * Get reference of the {@link ${modelName}}
    *
    * @param id ${idDescription}
    * @return Reference of {@link ${modelName}}
    */</#if>
    public ${modelName} getReferenceById(final ${idType} id) {

        return this.repository.getReferenceById(id);
    }