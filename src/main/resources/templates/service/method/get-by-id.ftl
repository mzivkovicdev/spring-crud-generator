<#if generateJavaDoc?? && generateJavaDoc>/**
     * Get a {@link ${modelName}} by id.
     *
     * @param id ${idDescription}
     * @return Found ${modelName} {@link ${modelName}}
     */</#if>
    public ${modelName} getById(final ${idType} id) {

        return this.repository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "${modelName} with id not found: " + id
            ));
    }