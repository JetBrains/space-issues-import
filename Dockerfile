# build
FROM openjdk:11.0.3-jdk

MAINTAINER Anton Spilnyy <anton.spilnyy@jetbrains.com>

COPY . /home/space
WORKDIR /home/space

RUN ["./gradlew", "--no-daemon", "jar"]

ENTRYPOINT ["java", "-jar", "./build/libs/space-issues-import-1.0-SNAPSHOT.jar"]
