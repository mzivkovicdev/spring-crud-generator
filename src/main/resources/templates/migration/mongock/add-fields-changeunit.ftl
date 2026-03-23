package ${packageName};

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "${changeUnitId}", order = "${order}", author = "spring-crud-generator")
public class ${className} {

    @Execution
    public void execution(final MongoTemplate mongoTemplate) {
<#list fields as field>
        mongoTemplate.getCollection("${collectionName}")
                .updateMany(
                        new Document("${field}", new Document("$exists", false)),
                        new Document("$set", new Document("${field}", null))
                );
</#list>
    }

    @RollbackExecution
    public void rollbackExecution(final MongoTemplate mongoTemplate) {
<#list fields as field>
        mongoTemplate.getCollection("${collectionName}")
                .updateMany(new Document(), new Document("$unset", new Document("${field}", "")));
</#list>
    }
}
