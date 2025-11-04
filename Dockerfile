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

# Download certificate in order to allow DB connection
RUN apt-get update && apt-get install -y curl && \
    curl -o DigiCertGlobalRootG2.crt.pem https://www.digicert.com/CACerts/DigiCertGlobalRootG2.crt.pem && \
    curl -o MicrosoftRSA2017.crt.pem "https://www.microsoft.com/pkiops/certs/Microsoft%20RSA%20Root%20Certificate%20Authority%202017.pem" && \
    cat DigiCertGlobalRootG2.crt.pem MicrosoftRSA2017.crt.pem > azure-mysql-ca.pem


# Indicate the port that exposed by the container
EXPOSE 8080

#COPY wait-for-it.sh /usr/app/wait-for-it.sh
#RUN chmod +x ./wait-for-it.sh
#
## Command that is executed when running docker run
#use this for running in local CMD [ "/usr/app/wait-for-it.sh", "db:3306", "--", "java", "-jar", "/usr/app/AtraApplication.jar" ]
CMD [ "java", "-jar", "/usr/app/AtraApplication.jar" ]