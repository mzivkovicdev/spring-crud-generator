public interface ${className} extends MongoRepository<${modelName}, ${idType}> {
<#if softDeleteEnabled?? && softDeleteEnabled>

    Optional<${modelName}> findByIdAndDeletedFalse(${idType} id);

    Page<${modelName}> findAllByDeletedFalse(Pageable pageable);

</#if>
    default ${modelName} saveAndFlush(final ${modelName} entity) {
        return this.save(entity);
    }
}
