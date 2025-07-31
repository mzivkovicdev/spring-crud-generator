    <#if generateJavaDoc?? && generateJavaDoc>
    /**
    * Deletes a {@link ${modelName}} by its ID.
    *
    * @param id ${idDescription}
    */</#if>
    ${transactionalAnnotation}
    public void deleteById(final ${idType} id) {

        LOGGER.info("Deleting ${modelName} with id {}", id);

        this.repository.deleteById(id);

        LOGGER.info("Deleted ${modelName} with id {}", id);
    }