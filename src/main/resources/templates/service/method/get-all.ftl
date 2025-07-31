    
    /**
     * Get all {@link ${modelName}} with pagination by page number and page size.
     *
     * @param pageNumber The page number.
     * @param pageSize The page size.
     * @return A page of {@link ${modelName}}.
     */
    public Page<${modelName}> getAll(final Integer pageNumber, final Integer pageSize) {

        return repository.findAll(PageRequest.of(pageNumber, pageSize));
    }