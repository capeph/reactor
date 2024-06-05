plugins {
    java
}

group = "org.capeph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":processor"))
    annotationProcessor(project(":processor"))
    implementation("io.aeron:aeron-all:1.44.1")
}
