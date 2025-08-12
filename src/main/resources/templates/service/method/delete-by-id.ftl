    <#if generateJavaDoc?? && generateJavaDoc>
    /**
    * Deletes a {@link ${modelName}} by its ID.
    *
    * @param ${idField} ${idDescription}
    */</#if>
    ${transactionalAnnotation}
    <#if cache>@CacheEvict(value = "${strippedModelName}", key = "#${idField}")</#if>
    public void deleteById(final ${idType} ${idField}) {

        LOGGER.info("Deleting ${modelName} with id {}", ${idField});

        this.repository.deleteById(${idField});

        LOGGER.info("Deleted ${modelName} with id {}", ${idField});
    }