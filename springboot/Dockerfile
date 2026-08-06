# 构建阶段：使用 Maven + JDK8 打包
FROM maven:3.6.3-jdk-8 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# 运行阶段：仅保留 JRE
FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=build /app/target/springboot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
