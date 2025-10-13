
    @Test
    void create() {

        final ${modelName} ${strippedModelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);

        when(this.${strippedModelName?uncap_first}Repository.saveAndFlush(any()))
                .thenReturn(${strippedModelName?uncap_first});

        final ${modelName} result = this.${strippedModelName?uncap_first}Service.create(
            <#list fieldNamesList as fieldName>${strippedModelName?uncap_first}.get${fieldName?cap_first}()<#if fieldName_has_next>, </#if></#list>
        );

        verify${strippedModelName?cap_first}(result, ${strippedModelName?uncap_first});

        verify(this.${strippedModelName?uncap_first}Repository).saveAndFlush(any());
    }