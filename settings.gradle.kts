plugins {
    id("com.gradleup.nmcp.settings").version("1.6.2")
}

rootProject.name = "ModrinthUpdateChecker"

nmcpSettings {
    centralPortal {
        username = providers.gradleProperty("ossrhUsername").orNull
        password = providers.gradleProperty("ossrhPassword").orNull
        publishingType = "USER_MANAGED"
    }
}