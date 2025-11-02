#################################################
# Base image for the build container
################################################# 3.8.5
FROM maven:3.9.11-eclipse-temurin-17 as builder

# Define the working directory where commands will be executed
WORKDIR /project

# Copy the project dependencies
COPY pom.xml /project/

# Copy the project code
COPY src /project/src

# Download the project dependencies
RUN mvn clean verify -DskipTests=true

# Build the project
RUN mvn package -o -DskipTests=true

#################################################
# Base image for the application container
#################################################
FROM eclipse-temurin:17-jdk

# Define the working directory where the JAR is located
WORKDIR /usr/app/

# Copy the JAR from the build container
COPY --from=builder /project/target/*.jar /usr/app/AtraApplication.jar

# Indicate the port that exposed by the container
EXPOSE 8080

#COPY wait-for-it.sh /usr/app/wait-for-it.sh
#RUN chmod +x ./wait-for-it.sh
#
## Command that is executed when running docker run
#use this for running in local CMD [ "/usr/app/wait-for-it.sh", "db:3306", "--", "java", "-jar", "/usr/app/AtraApplication.jar" ]
CMD [ "java", "-jar", "/usr/app/AtraApplication.jar" ]