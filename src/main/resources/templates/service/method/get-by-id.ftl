<#if generateJavaDoc?? && generateJavaDoc>/**
     * Get a {@link ${modelName}} by id.
     *
     * @param id ${idDescription}
     * @return Found ${modelName} {@link ${modelName}}
     */</#if>
    public ${modelName} getById(final ${idType} ${idField}) {

        return this.repository.findById(${idField})
            .orElseThrow(() -> new RuntimeException(
                "${modelName} with id not found: " + ${idField}
            ));
    }