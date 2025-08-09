FROM eclipse-temurin:17
EXPOSE 8080
ARG JAR_FILE="target/${artifactId}-${version}"
ADD <#noparse>${JAR_FILE}</#noparse> ${artifactId}.jar
ENTRYPOINT [ "java", "-jar", "/${artifactId}.jar" ]