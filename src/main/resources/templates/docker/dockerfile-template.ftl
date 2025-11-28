<#setting number_format="computer">
FROM ${baseImage}:${javaVersion}<#if tag?? && tag?has_content>-${tag}</#if>
EXPOSE ${port}
ARG JAR_FILE="target/${artifactId}-${version}.jar"
ADD <#noparse>${JAR_FILE}</#noparse> ${artifactId}.jar
ENTRYPOINT [ "java", "-jar", "/${artifactId}.jar" ]