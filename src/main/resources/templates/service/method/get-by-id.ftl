<#if generateJavaDoc?? && generateJavaDoc>/**
     * Get a {@link ${modelName}} by id.
     *
     * @param id ${idDescription}
     * @return Found ${modelName} {@link ${modelName}}
     */</#if>
    <#if cache>
    @Cacheable(value = "${modelName?uncap_first}", key = "#${idField}")
    </#if><#t>
    public ${modelName} getById(final ${idType} ${idField}) {

        return this.repository.findById(${idField})
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("${strippedModelName?cap_first} with id not found: %s", ${idField})
            ));
    }