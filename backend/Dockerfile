# Build e runtime separados: a imagem final leva so o JRE e o jar, sem Maven
# nem o codigo-fonte junto.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Os testes rodam no CI (backend-test.yml) e precisam baixar binarios do
# Postgres embarcado; repeti-los aqui so deixaria a imagem lenta.
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/bank-parser-*.jar app.jar
RUN useradd --system --uid 1001 bankparser
USER 1001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
