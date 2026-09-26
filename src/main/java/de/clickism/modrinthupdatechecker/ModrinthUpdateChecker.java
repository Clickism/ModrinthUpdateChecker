/*
 * MIT License
 *
 * Copyright (c) 2025 Clickism
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.clickism.modrinthupdatechecker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility class to check for newer versions of a project hosted on Modrinth.
 */
public class ModrinthUpdateChecker {

    private static final String API_URL = "https://api.modrinth.com/v2/project/{id}/version";

    // Parameters for the request
    private final String projectId;
    private final String loader;
    private @Nullable String minecraftVersion;
    private @Nullable Boolean featured = null;
    private boolean includeChangelog = false;

    private @Nullable Consumer<Exception> onError = null;
    private @NotNull Consumer<ModrinthVersion> onVersion = version -> {};

    /**
     * Create a new update checker for the given project.
     * This will check the latest version for the given loader and any minecraft version.
     *
     * @param projectId the project ID
     * @param loader    the loader
     */
    protected ModrinthUpdateChecker(String projectId, String loader) {
        this.projectId = projectId;
        this.loader = loader;
    }

    /**
     * Gets the raw version from a version string.
     * <p>
     * Strips any non-numeric characters and the minecraft version (must be after "+")
     * from the version string.
     * <p>
     * Example:
     * <ul>
     *     <li>
     *         <code>stripVersion("fabric-1.2+1.17.1")</code> returns <code>"1.2"</code>
     *     </li>
     *     <li>
     *         <code>stripVersion("2.2.1+1.20.1-fabric")</code> returns <code>"2.2.1"</code>
     *     </li>
     * </ul>
     *
     * @param version the version string
     * @return the raw version string
     */
    public static String stripVersion(String version) {
        if (version.isEmpty()) return version;
        version = version.replaceAll("^\\D+", "");
        String[] split = version.split("\\+");
        if (split.length == 0) return version;
        return split[0];
    }

    /**
     * Creates a new update checker for the given project and loader.
     *
     * @param projectId The project ID
     * @param loader    The loader
     * @return A new update checker instance
     */
    public static ModrinthUpdateChecker loader(String projectId, String loader) {
        return new ModrinthUpdateChecker(projectId, loader);
    }

    /**
     * Creates a new update checker for the given project and the Fabric loader.
     *
     * @param projectId The project ID
     * @return A new update checker instance for Fabric
     */
    public static ModrinthUpdateChecker fabric(String projectId) {
        return new ModrinthUpdateChecker(projectId, "fabric");
    }

    /**
     * Creates a new update checker for the given project and the Forge loader.
     *
     * @param projectId The project ID
     * @return A new update checker instance for Forge
     */
    public static ModrinthUpdateChecker forge(String projectId) {
        return new ModrinthUpdateChecker(projectId, "forge");
    }

    /**
     * Creates a new update checker for the given project and the NeoForge loader.
     *
     * @param projectId The project ID
     * @return A new update checker instance for NeoForge
     */
    public static ModrinthUpdateChecker neoforge(String projectId) {
        return new ModrinthUpdateChecker(projectId, "neoforge");
    }

    /**
     * Creates a new update checker for the given project and the Paper loader.
     *
     * @param projectId The project ID
     * @return A new update checker instance for Paper
     */
    public static ModrinthUpdateChecker paper(String projectId) {
        return new ModrinthUpdateChecker(projectId, "paper");
    }

    /**
     * Creates a new update checker for the given project and the Spigot loader.
     *
     * @param projectId The project ID
     * @return A new update checker instance for Spigot
     */
    public static ModrinthUpdateChecker spigot(String projectId) {
        return new ModrinthUpdateChecker(projectId, "spigot");
    }

    /**
     * Checks for the latest version of the project and calls the onVersion callback with it.
     *
     * @return This update checker instance for method chaining
     */
    private ModrinthUpdateChecker check(boolean async) {
        try {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                .uri(prepareURI())
                .GET()
                .build();

            if (async) {
                // Send async
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAcceptAsync(this::handleResponse);
            } else {
                // Send sync
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                handleResponse(response);
            }
        } catch (Exception e) {
            handleError(e);
        }
        return this;
    }

    /**
     * Handles the response from the Modrinth API.
     *
     * @param response The HTTP response
     */
    private void handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            handleError(new RuntimeException("wrong response status code: " + response.statusCode()));
            return;
        }
        JsonArray versionsArray = JsonParser.parseString(response.body()).getAsJsonArray();
        ModrinthVersion latestVersion = parseLatestVersionIn(versionsArray);
        if (latestVersion == null) {
            handleError(new RuntimeException("latest version is null"));
            return;
        }
        // Call callback
        onVersion.accept(latestVersion);
    }

    /**
     * Checks for the latest version of the project and calls the onVersion callback with it.
     * This method is asynchronous and the onVersion callback will be called when the response is received.
     *
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker check() {
        return check(true);
    }

    /**
     * Checks for the latest version of the project and calls the onVersion callback with it.
     * This method is synchronous and will block until the response is received.
     *
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker checkAndWait() {
        return check(false);
    }

    /**
     * Handle an error by calling the onError consumer if it is set.
     *
     * @param exception the exception
     */
    private void handleError(Exception exception) {
        if (onError != null) {
            onError.accept(exception);
        }
    }

    /**
     * Finds and parses the latest compatible version from the versions array.
     *
     * @param versions The versions array
     * @return The latest compatible version
     */
    private @Nullable ModrinthVersion parseLatestVersionIn(JsonArray versions) {
        return versions.asList().stream()
            .findFirst()
            .map(JsonElement::getAsJsonObject)
            .map(this::parseVersion)
            .orElse(null);
    }

    /**
     * Parse a version from the JSON object.
     *
     * @param version the JSON object
     * @return the parsed version
     */
    private ModrinthVersion parseVersion(JsonObject version) {
        var name = version.get("name").getAsString();
        var versionNumber = version.get("version_number").getAsString();
        var changelog = version.has("changelog") && !version.get("changelog").isJsonNull()
            ? version.get("changelog").getAsString()
            : "";
        var versionType = version.get("version_type").getAsString();
        var featured = version.get("featured").getAsBoolean();
        var dateString = version.get("date_published").getAsString();
        var date = LocalDateTime.parse(dateString.substring(0, 19));
        var downloads = version.get("downloads").getAsInt();
        // Create object
        return new ModrinthVersion(
            name,
            versionNumber,
            changelog,
            versionType,
            featured,
            date,
            downloads
        );
    }

    /**
     * Prepare the request URI with the project ID and parameters.
     *
     * @return the request URI
     */
    private URI prepareURI() {
        var query = prepareParameters().entrySet().stream()
            .map(entry -> {
                try {
                    return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                           + '='
                           + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    throw new RuntimeException("Error encoding query parameter: " + e.getMessage(), e);
                }
            })
            .collect(Collectors.joining("&"));
        var url = API_URL.replace("{id}", projectId) + '?' + query;
        return URI.create(url);
    }

    /**
     * Get the parameters for the version request.
     *
     * @return a map of key-value map of the request parameters
     */
    private Map<String, String> prepareParameters() {
        var parameters = new HashMap<String, String>();

        parameters.put("loaders", formatAsArray(loader));
        if (minecraftVersion != null) {
            parameters.put("game_versions", formatAsArray(minecraftVersion));
        }
        if (featured != null) {
            parameters.put("featured", featured.toString());
        }
        parameters.put("include_changelog", String.valueOf(includeChangelog));

        return parameters;
    }

    /**
     * Format a value as an array for the request parameters.
     * Modrinth API expects arrays to be formatted as ["value"].
     *
     * @param value the value
     * @return the formatted value
     */
    private String formatAsArray(String value) {
        return "[\"" + value + "\"]";
    }

    /**
     * Only get featured or non-featured versions, or null for all versions.
     *
     * @param featured Whether to only get featured versions, non-featured versions, or null for all versions
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker onlyFeatured(@Nullable Boolean featured) {
        this.featured = featured;
        return this;
    }

    /**
     * Callback for when an error occurs during the version check. If not set, errors will be ignored.
     *
     * @param onError The callback to call when an error occurs
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker onError(@Nullable Consumer<Exception> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * Set the minecraft version to check for.
     *
     * @param minecraftVersion The minecraft version to check for
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker minecraftVersion(@Nullable String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
        return this;
    }

    /**
     * Whether to include the changelog in the response. Default is false.
     *
     * @param includeChangelog Whether to include the changelog in the response
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker includeChangelog(boolean includeChangelog) {
        this.includeChangelog = includeChangelog;
        return this;
    }

    /**
     * Callback for when a version is found. If not set, the version will be ignored.
     *
     * @param onVersion The callback to call when a version is found
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker onVersion(@NotNull Consumer<ModrinthVersion> onVersion) {
        this.onVersion = onVersion;
        return this;
    }

    /**
     * Callback for when a version is found. If not set, the version will be ignored.
     *
     * @param onVersion The callback to call when a version is found
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker onVersionString(@NotNull Consumer<String> onVersion) {
        this.onVersion = version -> onVersion.accept(version.versionNumber());
        return this;
    }

    /**
     * Callback for when a version is found. If not set, the version will be ignored.
     * The version string will be stripped of any non-numeric characters and the minecraft version.
     *
     * @param onVersion The callback to call when a version is found
     * @return This update checker instance for method chaining
     */
    public ModrinthUpdateChecker onStrippedVersionString(@NotNull Consumer<String> onVersion) {
        this.onVersion = version -> onVersion.accept(stripVersion(version.versionNumber()));
        return this;
    }
}
