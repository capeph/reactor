plugins {
    id("java")
}

group = "org.capeph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":processor"))
    annotationProcessor(project(":processor"))
    implementation(project(":config"))
    implementation("io.aeron:aeron-all:1.44.1")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
}