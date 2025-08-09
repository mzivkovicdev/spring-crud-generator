
<#if generateJavaDoc?? && generateJavaDoc>
    /**
    * Get reference of the {@link ${modelName}}
    *
    * @param ${idField} ${idDescription}
    * @return Reference of {@link ${modelName}}
    */</#if>
    public ${modelName} getReferenceById(final ${idType} ${idField}) {

        return this.repository.getReferenceById(${idField});
    }