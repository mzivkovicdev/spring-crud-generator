/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.constants;

/**
 * Constants representing keys used in the template context for code generation.
 */
public final class TemplateContextConstants {

    private TemplateContextConstants() {}

    public static final String STORAGE_NAME = "storageName";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String FIELD = "field";
    public static final String LENGTH = "length";
    public static final String FIELDS = "fields";
    public static final String FIELDS_WITH_LENGTH = "fieldsWithLength";
    public static final String ID_TYPE = "idType";
    public static final String ID_FIELD = "idField";
    public static final String ID_DESCRIPTION = "idDescription";
    public static final String FIELD_TYPE = "fieldType";
    public static final String FIELD_NAMES = "fieldNames";
    public static final String CLASS_NAME = "className";
    public static final String INPUT_ARGS = "inputArgs";
    public static final String NON_ID_FIELD_NAMES = "nonIdFieldNames";
    public static final String AUDIT_ENABLED = "auditEnabled";
    public static final String AUDIT_TYPE = "auditType";
    public static final String SERVICE_CLASSES = "serviceClasses";
    public static final String MODEL_NAME = "modelName";
    public static final String STRIPPED_MODEL_NAME = "strippedModelName";
    public static final String TRANSACTIONAL_ANNOTATION = "transactionalAnnotation";
    public static final String RETRYABLE_ANNOTATION = "retryableAnnotation";
    public static final String TEST_INPUT_ARGS = "testInputArgs";
    public static final String MODEL_SERVICE = "modelService";
    public static final String RELATIONS = "relations";
    public static final String MODEL = "model";
    public static final String RELATION_ID_FIELD = "relationIdField";
    public static final String RELATION_ID_TYPE = "relationIdType";
    public static final String RELATION_CLASS_NAME = "relationClassName";
    public static final String STRIPPED_RELATION_CLASS_NAME = "strippedRelationClassName";
    public static final String ELEMENT_PARAM = "elementParam";
    public static final String RELATION_FIELD_MODEL = "relationFieldModel";
    public static final String RELATION_FIELD = "relationField";
    public static final String IS_COLLECTION = "isCollection";
    public static final String IS_BASE_ENTITY = "isBaseEntity";
    public static final String JAVADOC_FIELDS = "javadocFields";
    public static final String METHOD_NAME = "methodName";
    public static final String RELATION_TYPE = "relationType";
    public static final String RELATED_ID = "relatedId";
    public static final String RELATED_ID_PARAM = "relatedIdParam";
    public static final String GENERATE_JAVA_DOC = "generateJavaDoc";
    public static final String INPUT_FIELDS = "inputFields";
    public static final String FIELD_NAMES_WITHOUT_ID = "fieldNamesWithoutId";
    public static final String ENTITY_GRAPH_NAME = "entityGraphName";
    public static final String OPEN_IN_VIEW_ENABLED = "openInViewEnabled";
    public static final String BASE_IMPORTS = "baseImports";
    public static final String PROJECT_IMPORTS = "projectImports";
    public static final String LAZY_FIELDS = "lazyFields";
    public static final String EAGER_FIELDS = "eagerFields";
    public static final String BASE_COLLECTION_FIELDS = "baseCollectionFields";
    public static final String HAS_LAZY_FIELDS = "hasLazyFields";
    
    public static final String IS_RELATION = "isRelation";
    public static final String IS_JSON_FIELD = "isJsonField";
    public static final String IS_ENUM = "isEnum";

    public static final String JSON_FIELDS = "jsonFields";
    public static final String JSON_MODELS = "jsonModels";

    public static final String INPUT_FIELDS_WITHOUT_RELATIONS = "inputFieldsWithoutRelations";
    public static final String INPUT_FIELDS_WITH_RELATIONS = "inputFieldsWithRelations";

    public static final String ENUM_NAME = "enumName";
    public static final String VALUES = "values";

    public static final String MODEL_IMPORT = "modelImport";
    public static final String TRANSFER_OBJECT_IMPORT = "transferObjectImport";
    public static final String MAPPER_NAME = "mapperName";
    public static final String TRANSFER_OBJECT_NAME = "transferObjectName";
    public static final String SWAGGER = "swagger";
    public static final String SWAGGER_MODEL = "swaggerModel";
    public static final String GENERATED_MODEL_IMPORT = "generatedModelImport";
    public static final String GENERATE_ALL_HELPER_METHODS = "generateAllHelperMethods";
    public static final String HELPER_MAPPER_IMPORTS = "helperMapperImports";
    public static final String PARAMETERS = "parameters";

    public static final String DATA_GENERATOR = "dataGenerator";
    public static final String DATA_GENERATOR_FIELD_NAME = "generatorFieldName";
    public static final String DATA_GENERATOR_SINGLE_OBJ = "singleObjectMethodName";
    public static final String DATA_GENERATOR_LIST_METHOD = "multipleObjectsMethodName";
}
