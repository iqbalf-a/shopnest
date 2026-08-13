# ===== STAGE 1: BUILD (bikin file .jar) =====
# Pakai image berisi JDK 21 + Maven. Diberi nama "builder" untuk dirujuk nanti.
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Maven wrapper + pom dulu, lalu download dependency.
# Dipisah supaya kalau kode berubah tapi pom tidak, download tidak diulang (cache).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copy sisa kode, lalu build jadi .jar (skip test biar cepat)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ===== STAGE 2: RUNTIME (jalankan .jar) =====
# JRE saja (tanpa tools build) → image jauh lebih kecil
FROM eclipse-temurin:21-jre
WORKDIR /app

# Ambil HANYA file .jar hasil stage builder
COPY --from=builder /app/target/*.jar app.jar

# Port yang dipakai Eureka (dokumentasi; tidak otomatis membuka)
EXPOSE 8761

# Perintah saat container dinyalakan
ENTRYPOINT ["java", "-jar", "app.jar"]
