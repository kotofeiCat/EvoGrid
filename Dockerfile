FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]