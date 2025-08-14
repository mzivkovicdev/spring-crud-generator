<#if generateJavaDoc?? && generateJavaDoc>/**
     * Get a {@link ${modelName}} by id.
     *
     * @param id ${idDescription}
     * @return Found ${modelName} {@link ${modelName}}
     */</#if>
    <#if cache>@Cacheable(value = "${strippedModelName}", key = "#${idField}")</#if>
    public ${modelName} getById(final ${idType} ${idField}) {

        return this.repository.findById(${idField})
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("${modelName} with id not found: %s", ${idField})
            ));
    }