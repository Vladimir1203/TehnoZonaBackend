# 1. Bazirana slika (OpenJDK 17)
FROM eclipse-temurin:17-jdk-alpine

# 2. Postavi radni direktorijum
WORKDIR /app

# 3. Kopiraj JAR fajl (ovo ćeš zameniti stvarnim imenom)
COPY target/TehnoZonaSpring-0.0.1-SNAPSHOT.jar

# 4. Otvori port ako želiš (opciono)
EXPOSE 8080

# 5. Start komanda
ENTRYPOINT ["java", "-jar", "app.jar"]
