    <#if generateJavaDoc?? && generateJavaDoc>
    /**
    * Deletes a {@link ${modelName}} by its ID.
    *
    * @param ${idField} ${idDescription}
    */</#if>
    ${transactionalAnnotation}
    <#if cache>
    @CacheEvict(value = "${modelName?uncap_first}", key = "#${idField}")
    </#if><#t>
    public void deleteById(final ${idType} ${idField}) {

        LOGGER.info("Deleting ${strippedModelName} with id {}", ${idField});

        this.repository.deleteById(${idField});

        LOGGER.info("Deleted ${strippedModelName} with id {}", ${idField});
    }