plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.capeph"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")
    repositories(RepositoryHandler::mavenCentral)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lookup"))
    implementation(project(":messages"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.4")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("io.aeron:aeron-all:1.44.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.12.0")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
}