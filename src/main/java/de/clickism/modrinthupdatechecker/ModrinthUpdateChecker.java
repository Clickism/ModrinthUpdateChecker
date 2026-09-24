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
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class to check for newer versions of a project hosted on Modrinth.
 */
public class ModrinthUpdateChecker {

    private static final String API_URL = "https://api.modrinth.com/v2/project/{id}/version";

    // Parameters for the request
    private final String projectId;
    private final String loader;
    @Nullable
    private String minecraftVersion;
    @Nullable
    private Boolean featured = null;
    private boolean includeChangelog = false;

    @Nullable
    public Consumer<Exception> onError = null;
    @Nullable
    public Function<String, String> getRawVersion = ModrinthUpdateChecker::getRawVersion;

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
     * i.E: "fabric-1.2+1.17.1" -> "1.2"
     *
     * @param version the version string
     * @return the raw version string
     */
    public static String getRawVersion(String version) {
        if (version.isEmpty()) return version;
        version = version.replaceAll("^\\D+", "");
        String[] split = version.split("\\+");
        return split[0];
    }

    /**
     * Check the latest version of the project for the given loader and minecraft version
     * and call the consumer with it.
     *
     * @param consumer the consumer
     */
    public void checkVersion(Consumer<String> consumer) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(prepareURI())
                .GET()
                .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() != 200) {
                        handleError(new RuntimeException("wrong response status code: " + response.statusCode()));
                        return;
                    }
                    JsonArray versionsArray = JsonParser.parseString(response.body()).getAsJsonArray();
                    String latestVersion = getLatestVersion(versionsArray);
                    if (latestVersion == null) {
                        handleError(new RuntimeException("latest version is null"));
                        return;
                    }
                    consumer.accept(latestVersion);
                });
        } catch (Exception e) {
            handleError(e);
        }
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
     * Get the latest compatible version from the versions array.
     *
     * @param versions the versions array
     * @return the latest compatible version
     */
    @Nullable
    protected String getLatestVersion(JsonArray versions) {
        return versions.asList().stream().findFirst()
            .map(JsonElement::getAsJsonObject)
            .map(version -> version.get("version_number").getAsString())
            .map(getRawVersion != null
                ? getRawVersion
                : (v -> v))
            .orElse(null);
    }

    /**
     * Prepare this request uri based on current parameters.
     *
     * @return the request uri
     */
    private URI prepareURI() {
        var query = prepareParameters().entrySet().stream()
            .map(entry -> entry.getKey() + '=' + entry.getValue())
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
     * Function called on error calling the api.
     *
     * @param onError What should happen on error
     * @return this
     */
    public ModrinthUpdateChecker onError(@Nullable Consumer<Exception> onError) {
        this.onError = onError;
        return this;
    }

    public ModrinthUpdateChecker minecraftVersion(@Nullable String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
        return this;
    }

    public ModrinthUpdateChecker includeChangelog(boolean includeChangelog) {
        this.includeChangelog = includeChangelog;
        return this;
    }

    public static ModrinthUpdateChecker create(String projectId, String loader) {
        return new ModrinthUpdateChecker(projectId, loader);
    }

    public static ModrinthUpdateChecker fabric(String projectId) {
        return new ModrinthUpdateChecker(projectId, "fabric");
    }

    public static ModrinthUpdateChecker forge(String projectId) {
        return new ModrinthUpdateChecker(projectId, "forge");
    }

    public static ModrinthUpdateChecker neoforge(String projectId) {
        return new ModrinthUpdateChecker(projectId, "neoforge");
    }

    /**
     * Set the function to get raw version from the modrinth version.
     * If null provided raw version will act as in the identity function.
     *
     * @param getRawVersion The function transforming modrinth version to raw version
     * @return this
     */
    public ModrinthUpdateChecker setGetRawVersion(@Nullable Function<String, String> getRawVersion) {
        this.getRawVersion = getRawVersion;
        return this;
    }
}
