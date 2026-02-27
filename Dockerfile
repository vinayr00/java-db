FROM openjdk:21

WORKDIR /app

COPY . .

RUN javac -cp ".:sqlite-jdbc-3.36.0.3.jar" StudentBackend.java

CMD ["java", "-cp", ".:sqlite-jdbc-3.36.0.3.jar", "StudentBackend"]