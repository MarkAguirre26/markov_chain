FROM openjdk:17
EXPOSE 8081
ADD target/PlayerCompanion.jar PlayerCompanion.jar
ENTRYPOINT ["java","-jar","PlayerCompanion.jar"]