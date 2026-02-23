FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY nexusbot.jar /app/nexusbot.jar

RUN chown -R app:app /app

USER app

CMD ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/nexusbot.jar"]
