package ${packageName};

import io.mongock.api.annotations.BeforeExecution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
<#if indexes?has_content>
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
</#if>
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "${changeUnitId}", order = "${order}", author = "spring-crud-generator")
public class ${className} {

    private final MongoTemplate mongoTemplate;

    public ${className}(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @BeforeExecution
    public void beforeExecution() {
        if (!this.mongoTemplate.collectionExists("${collectionName}")) {
            this.mongoTemplate.createCollection("${collectionName}");
        }
    }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {
        this.mongoTemplate.dropCollection("${collectionName}");
    }

    @Execution
    public void execution() {
<#list indexes as index>
        this.mongoTemplate.indexOps("${collectionName}")
                .ensureIndex(new Index().on("${index.field}", Sort.Direction.ASC)<#if index.unique>.unique()</#if>);
</#list>
    }

    @RollbackExecution
    public void rollbackExecution() {
<#list indexes as index>
        this.mongoTemplate.indexOps("${collectionName}").dropIndex("${index.field}_1");
</#list>
    }
}
