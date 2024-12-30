FROM openjdk:21-jdk-slim
WORKDIR /app
COPY build/libs/order-service-0.0.1-SNAPSHOT.jar order-service.jar
ENTRYPOINT ["java", "-jar", "order-service.jar"]
