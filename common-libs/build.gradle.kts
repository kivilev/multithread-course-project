plugins {
    id("java-library")
}

version = "1.0-SNAPSHOT"

dependencies {
    //  tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}