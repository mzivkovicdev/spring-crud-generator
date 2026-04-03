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
<#if softDeleteEnabled?? && softDeleteEnabled>

        LOGGER.info("Soft-deleting ${strippedModelName} with id {}", ${idField});

        final ${modelName} entity = this.repository.findByIdAndDeletedFalse(${idField})
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("${strippedModelName?cap_first} with id not found: %s", ${idField})
            ));
        entity.setDeleted(true);
        this.repository.save(entity);

        LOGGER.info("Soft-deleted ${strippedModelName} with id {}", ${idField});
<#else>

        LOGGER.info("Deleting ${strippedModelName} with id {}", ${idField});

        this.repository.deleteById(${idField});

        LOGGER.info("Deleted ${strippedModelName} with id {}", ${idField});
</#if>
    }