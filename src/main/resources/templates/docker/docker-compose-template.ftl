<#setting number_format="computer">
services:
    ${artifactId}:
        build:
            context: .
            dockerfile: Dockerfile
        container_name: ${artifactId}-app
        restart: unless-stopped
        ports:
            - "${appPort}:${appPort}"
        environment:
            <#if dbType == "postgresql">
            SPRING_DATASOURCE_URL: jdbc:postgresql://database:${dbPort}/${artifactId}
            SPRING_DATASOURCE_USERNAME: app
            SPRING_DATASOURCE_PASSWORD: app
            <#elseif dbType == "mysql">
            SPRING_DATASOURCE_URL: jdbc:mysql://database:${dbPort}/${artifactId}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
            SPRING_DATASOURCE_USERNAME: app
            SPRING_DATASOURCE_PASSWORD: app
            <#elseif dbType == "mssql">
            SPRING_DATASOURCE_URL: jdbc:sqlserver://database:${dbPort};databaseName=${artifactId};encrypt=false
            SPRING_DATASOURCE_USERNAME: app
            SPRING_DATASOURCE_PASSWORD: App!Passw0rd
            </#if>
            <#if cacheType?? && cacheType?lower_case == "redis">
            SPRING_DATA_REDIS_HOST: redis
            SPRING_DATA_REDIS_PORT: 6379
            </#if><#t>
        depends_on:
            - database
            <#if cacheType?? && cacheType?lower_case == "redis">
            - redis
            </#if><#t>
        networks:
            - ${artifactId}-network
    
    database:
        <#if dbType == "postgresql">
        image: ${dbImage}<#if dbTag?? && dbTag?has_content>:${dbTag}</#if>
        container_name: ${artifactId}-postgre-db
        restart: unless-stopped
        environment:
            POSTGRES_DB: ${artifactId}
            POSTGRES_USER: app
            POSTGRES_PASSWORD: app
        ports:
            - "${dbPort}:${dbPort}"
        <#elseif dbType == "mysql">
        image: ${dbImage}<#if dbTag?? && dbTag?has_content>:${dbTag}</#if>
        container_name: ${artifactId}-mysql-db
        environment:
            MYSQL_DATABASE: ${artifactId}
            MYSQL_USER: app
            MYSQL_PASSWORD: app
            MYSQL_ROOT_PASSWORD: root
        command: ["--default-authentication-plugin=mysql_native_password"]
        ports:
            - "${dbPort}:${dbPort}"
        <#elseif dbType == "mssql">
        image: ${dbImage}<#if dbTag?? && dbTag?has_content>:${dbTag}</#if>
        container_name: ${artifactId}-mssql-db
        environment:
            ACCEPT_EULA: "Y"
            DB_NAME: ${artifactId}
            MSSQL_SA_PASSWORD: Strong!Passw0rd
            MSSQL_PID: "Express"
            APP_USER: app
            APP_PASSWORD: App!Passw0rd
        ports:
            - "${dbPort}:${dbPort}"
        command:
            - bash
            - -lc
            - |
                /opt/mssql/bin/sqlservr & pid=$$!;
                echo 'Waiting for SQL Server to be ready...';
                for i in {1..60}; do
                    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$$MSSQL_SA_PASSWORD" -C -Q 'SELECT 1' >/dev/null 2>&1 && break;
                    sleep 3;
                done;
                echo 'Creating database and user if needed...';
                /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$$MSSQL_SA_PASSWORD" -C -Q "IF DB_ID('$$DB_NAME') IS NULL BEGIN CREATE DATABASE [$$DB_NAME]; END";
                /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$$MSSQL_SA_PASSWORD" -C -Q "IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = '$$APP_USER') BEGIN CREATE LOGIN [$$APP_USER] WITH PASSWORD='$$APP_PASSWORD', CHECK_POLICY=OFF; END";
                /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$$MSSQL_SA_PASSWORD" -C -Q "USE [$$DB_NAME]; IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = '$$APP_USER') CREATE USER [$$APP_USER] FOR LOGIN [$$APP_USER]; EXEC sp_addrolemember 'db_owner', '$$APP_USER';";
                wait $$pid
        </#if>
        volumes:
            - db_data:/var/<#if dbType == "mssql">opt<#else>lib</#if>/<#if dbType == "postgresql">postgresql<#elseif dbType == "mysql">mysql<#elseif dbType == "mssql">mssql</#if>
        networks:
            - ${artifactId}-network
    <#if cacheType?? && cacheType?lower_case == "redis">

    redis:
        image: redis:7-alpine
        container_name: ${artifactId}-redis
        restart: unless-stopped
        command: ["redis-server", "--appendonly", "yes"]
        ports:
            - "6379:6379"
        volumes:
            - redis_data:/data
        networks:
            - ${artifactId}-network

    </#if><#t>
networks:
    ${artifactId}-network:

volumes:
    db_data:
    <#if cacheType?? && cacheType?lower_case == "redis">
    redis_data:
    </#if><#t>