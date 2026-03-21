public interface ${className} extends MongoRepository<${modelName}, ${idType}> {

    default ${modelName} saveAndFlush(final ${modelName} entity) {
        return this.save(entity);
    }
}
