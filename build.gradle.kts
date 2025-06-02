plugins {
    id("java")
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp").version("0.1.4")
}

group = "de.clickism"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10")
    compileOnly("org.jetbrains:annotations:24.0.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
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

nmcp {
    centralPortal {
        username = findProperty("ossrhUsername") as String?
        password = findProperty("ossrhPassword") as String?
        publishingType = "USER_MANAGED"
    }
}

tasks.named("publish") {
    dependsOn(tasks["sourcesJar"], tasks["javadocJar"])
}