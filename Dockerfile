FROM maven:3.9.9-amazoncorretto-21 AS build

COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests

FROM amazoncorretto:21
COPY --from=build /home/app/target/transfer-scheduler-0.0.1-SNAPSHOT.jar /usr/local/lib/transfer-scheduler-0.0.1-SNAPSHOT.jar

EXPOSE 8083
ENTRYPOINT ["java","-jar","/usr/local/lib/transfer-scheduler-0.0.1-SNAPSHOT.jar"]

