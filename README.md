# Modrinth Update Checker

This is a single class Java library to check for newer versions of projects on Modrinth using the Modrinth API.

Licensed under the **MIT License**.

## Adding to Your Project
Modrinth Update Checker is available on **Maven Central**. You can add it to your project using Maven or Gradle:
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("de.clickism:modrinth-update-checker:1.1")
}
```

## Usage

The library provides a simple API to check for updates on Modrinth.
You can use one of the predefined factory methods for your platform to create a `ModrinthUpdateChecker` instance:

```java
ModrinthUpdateChecker.fabric("my-modrinth-slug"); // for Fabric mods
```

And you can define a callback to handle the latest version found on Modrinth:

```java
.onVersion(version -> { /* ... */ }
```

And send an API request to Modrinth and fetch the latest version asynchronously using:

```java
.check();
```

The callback and check methods are separated, so you can define the checker once, and call `check()` multiple times
or regularly to check for updates if you need to.

In case of an error while fetching the version, the callback won't be called and the checker will fail silently.
You can set the `onError` callback to handle errors if you want to.

### Simple Usage

You can use the following code to check for updates in your mod/plugin:

```java
public class ExampleMod implements ModInitializer {
    // ...
    @Override
    public void onInitialize() {
        ModrinthUpdateChecker.fabric("my-modrinth-slug")
            .minecraftVersion("1.21.4")
            .includeChangelog(true)
            .onVersion(version -> {
                LOGGER.warn("Latest version available: {}", version.versionNumber());
                LOGGER.warn("Changelog: {}", version.changelog());
            })
            .check();
    }
}
```

### All Options

Here is an example of all available options/methods:

```java
ModrinthUpdateChecker.fabric("my-modrinth-slug")
    .minecraftVersion("1.21.4") // will check all versions if not specified
    .includeChangelog(true) // default is false, if false changelog will be an empty string
    .onlyFeatured(false) // default is false, if true only the featured versions will be checked
    .onError(error -> { // by default errors are ignored
        LOGGER.error("Failed to check for updates: {}", error.getMessage());
    })
    .onVersion(version -> { // called when the latest version is found
        // "version" is an object containing various metadata
        // such as version number, changelog, release date, downloads, etc.
        LOGGER.warn("Latest version available: {}", version.versionNumber());
        LOGGER.warn("Changelog: {}", version.changelog());
    })
    .onVersionString(version -> { // called when the latest version is found
        // "version" is the version number string
        LOGGER.warn("Latest version available: {}", version); // e.g.: 2.4+1.20.1-fabric
    })
    .onStrippedVersionString(version -> { // called when the latest version is found
        // "version" is the version number string without the minecraft/loader version
        LOGGER.warn("Latest version available: {}", version); // e.g.: 2.4
    })
    .check(); // sends a request to the Modrinth API asynchronously
    .checkAndWait(); // sends a request to the Modrinth API synchronously (blocking the current thread)
```

### Checking if Version is Newer

Some extra platform-specific logic is needed to check if the latest version is newer than the current
version of your mod/plugin. Here are some examples for Fabric, Forge and Paper:

#### Fabric

```java
public class ExampleMod implements ModInitializer {
    // ...
    @Override
    public void onInitialize() {
        var modVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElseThrow();
        var minecraftVersion = DetectedVersion.BUILT_IN.getName(); // for vanilla mappings 1.20.1
        
        ModrinthUpdateChecker.fabric("my-modrinth-slug")
            .minecraftVersion(minecraftVersion)
            .onVersion(version -> {
                // We need to strip the version number, as it likely includes the minecraft version and/or loader version.
                // For example, "2.4+1.20.1-fabric" would be stripped to "2.4".
                // This method might not work for all formats, so make sure to test it with your versioning scheme.
                if (version.strippedVersionNumber().equals(modVersion)) {
                    LOGGER.info("Example Mod is up to date.");
                    return;
                }
                LOGGER.info("Newer version available: {}", version);
            })
            .check();
    }
}
```

#### Forge

```java
@Mod("examplemod")
public class ExampleMod {
    // ...
    public ExampleMod(FMLJavaModLoadingContext context) {
        var modVersion = ModList.get().getModContainer("examplemod")
                .map(container -> container.getModInfo().getVersion().toString())
                .orElseThrow();
        var minecraftVersion = DetectedVersion.BUILT_IN.getName(); // for vanilla mappings 1.20.1
        
        ModrinthUpdateChecker.forge("my-modrinth-slug")
            .minecraftVersion(minecraftVersion)
            .onVersion(version -> {
                if (version.strippedVersionNumber().equals(modVersion)) {
                    LOGGER.info("Example Mod is up to date.");
                    return;
                }
                LOGGER.info("Newer version available: {}", version);
            })
            .check();
    }
}
```

#### Paper

```java
public class ExamplePlugin extends JavaPlugin {
    // ...
    @Override
    public void onEnable() {
        ModrinthUpdateChecker.paper("my-modrinth-slug")
            // For paper, Minecraft version is likely not needed
            .onVersion(version -> {
                if (getDescription().getVersion().equals(version)) {
                    getLogger().info("Example Plugin is up to date.");
                    return;
                }
                getLogger().warning("Newer version available: " + version);
            })
            .check();
    }
}
```
