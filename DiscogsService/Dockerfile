FROM eclipse-temurin:21.0.3_9-jre-jammy as builder
WORKDIR extracted
ADD ./target/discogsservice-1.0.0.jar discogsservice-1.0.0.jar
RUN java -Djarmode=layertools -jar discogsservice-1.0.0.jar extract

FROM eclipse-temurin:21.0.3_9-jre-jammy
WORKDIR application
COPY --from=builder extracted/dependencies/ ./
COPY --from=builder extracted/spring-boot-loader/ ./
COPY --from=builder extracted/snapshot-dependencies/ ./
COPY --from=builder extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]