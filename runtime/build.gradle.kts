plugins {
    java
    `java-library`
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    implementation(project(":scanner"))
    implementation(project(":agent"))
    implementation(project(":workflow"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")

    // SQLite
    runtimeOnly("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("org.hibernate.orm:hibernate-community-dialects")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.bootJar {
    archiveFileName.set("aipa-runtime.jar")
}
