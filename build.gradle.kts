plugins {
    id("java")
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp.aggregation").version("1.6.2")
}

group = "de.clickism"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10")
    compileOnly("org.jetbrains:annotations:24.0.0")

    // Testing
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

tasks.register<Jar>("javadocJar") {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            groupId = group.toString()
            artifactId = "modrinth-update-checker"
            version = version.toString()
            pom {
                name.set("Modrinth Update Checker")
                description.set("Single class Java library to check for newer versions of projects on Modrinth using the Modrinth API.")
                url.set("https://github.com/Clickism/ModrinthUpdateChecker")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://mit-license.org")
                    }
                }
                developers {
                    developer {
                        id.set("Clickism")
                        name.set("Clickism")
                        email.set("dev@clickism.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Clickism/ModrinthUpdateChecker.git")
                    developerConnection.set("scm:git:ssh://github.com/Clickism/ModrinthUpdateChecker.git")
                    url.set("https://github.com/Clickism/ModrinthUpdateChecker")
                }
            }
        }
    }
    signing {
        sign(publishing.publications["mavenJava"])
    }
}

nmcpAggregation {
    centralPortal {
        username = findProperty("ossrhUsername") as String?
        password = findProperty("ossrhPassword") as String?
        publishingType = "USER_MANAGED"
    }
}

tasks.named("publish") {
    dependsOn(tasks["sourcesJar"], tasks["javadocJar"])
}