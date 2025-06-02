## ⬆️ Modrinth Update Checker ⬆️

This is a single class Java library to check for newer versions of projects on Modrinth using the Modrinth API.

Licensed under the **MIT License**.

### Adding to Your Project 📦
Modrinth Update Checker is available on **Maven Central**. You can add it to your project using Maven or Gradle:
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("de.clickism:modrinth-update-checker:1.0")
}
```

### How to Use 🛠️

`ModrinthUpdateChecker#checkVersion(...)` will call the given callback with the latest version available on Modrinth.
In case of an error while fetching the version, the callback won't be called and the checker will fail silently.

Simple usage:
```java
public class ExampleMod implements ModInitializer {
    
    public static final Logger LOGGER = LoggerFactory.getLogger("example-mod");
    
    @Override
    public void onInitialize() {
        new ModrinthUpdateChecker("my-modrinth-slug", "fabric", "1.21.4")
                .checkVersion(version -> {
                    LOGGER.warn("Latest version available: {}", version);
                });
    }
}
```

Some extra logic is needed to check if a newer version is available.

**For Fabric:**

```java
public class ExampleMod implements ModInitializer {

    public static final String MOD_ID = "example-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        String modVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
        String minecraftVersion = MinecraftVersion.CURRENT.getName();
        new ModrinthUpdateChecker("my-modrinth-slug", "fabric", minecraftVersion)
                .checkVersion(version -> {
                    if (modVersion == null || UpdateChecker.getRawVersion(modVersion).equals(version)) {
                        LOGGER.info("Example Mod is up to date.");
                        return;
                    }
                    LOGGER.info("Newer version available: {}", version);
                });
    }
}
```

**For Spigot:**

```java
public class ExamplePlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        new ModrinthUpdateChecker("my-modrinth-slug", "spigot", null)
                .checkVersion(version -> {
                    if (getDescription().getVersion().equals(version)) {
                        getLogger().info("Example Plugin is up to date.");
                        return;
                    }
                    getLogger().warning("Newer version available: " + version);
                });
    }
}
```
