package dev.markozivkovic.springcrudgenerator.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;

class MongoModelImportsTest {

    @Test
    @DisplayName("computeMongoModelImports includes document/id/dbref/audit/collection imports when needed")
    void computeMongoModelImports_includesAllRelevantImports() {

        final ModelDefinition model = new ModelDefinition()
                .setName("ProductModel")
                .setAudit(new AuditDefinition().setEnabled(true).setType(AuditTypeEnum.INSTANT))
                .setFields(List.of(
                        new FieldDefinition().setName("id").setType("String").setId(new IdDefinition()),
                        new FieldDefinition()
                                .setName("users")
                                .setType("UserEntity")
                                .setRelation(new RelationDefinition().setType("OneToMany")),
                        new FieldDefinition()
                                .setName("tags")
                                .setType("TagEntity")
                                .setRelation(new RelationDefinition().setType("ManyToMany").setUniqueItems(true))
                ));

        final String imports = MongoModelImports.computeMongoModelImports(model, true);

        assertTrue(imports.contains("import " + ImportConstants.SpringData.MONGO_DOCUMENT));
        assertTrue(imports.contains("import " + ImportConstants.SpringData.MONGO_ID));
        assertTrue(imports.contains("import " + ImportConstants.SpringData.MONGO_DB_REF));
        assertTrue(imports.contains("import " + ImportConstants.SpringData.CREATED_DATE));
        assertTrue(imports.contains("import " + ImportConstants.SpringData.LAST_MODIFIED_DATE));
        assertTrue(imports.contains("import " + ImportConstants.Java.ARRAY_LIST));
        assertTrue(imports.contains("import " + ImportConstants.Java.HASH_SET));
    }

    @Test
    @DisplayName("computeMongoModelImports omits @Document import for helper models")
    void computeMongoModelImports_withoutDocumentImport_excludesDocumentImport() {

        final ModelDefinition helperModel = new ModelDefinition()
                .setName("Address")
                .setFields(List.of(new FieldDefinition().setName("city").setType("String")));

        final String imports = MongoModelImports.computeMongoModelImports(helperModel, false);

        assertFalse(imports.contains("import " + ImportConstants.SpringData.MONGO_DOCUMENT));
    }
}
