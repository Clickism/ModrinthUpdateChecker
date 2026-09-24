/*
 * MIT License
 *
 * Copyright (c) 2026 Clickism
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

import java.time.LocalDateTime;

/**
 * Represents a version of a mod on Modrinth.
 *
 * @param name          The name of the version
 * @param versionNumber The version number (e.g. <code>1.0.0</code>)
 * @param changelog     The changelog of the version, or an empty string if not in the request
 *                      ({@link ModrinthUpdateChecker#includeChangelog(boolean)})
 * @param versionType   The type of the version: <code>release</code>, <code>beta</code>, or <code>alpha</code>
 * @param featured      Whether the version is featured
 * @param datePublished The date the version was published
 * @param downloads     The number of downloads of the version
 */
public record ModrinthVersion(
    String name,
    String versionNumber,
    String changelog,
    String versionType,
    boolean featured,
    LocalDateTime datePublished,
    int downloads
) {
    /**
     * Returns the stripped version number using {@link ModrinthUpdateChecker#stripVersion(String)}.
     *
     * @return The stripped version number
     */
    public String strippedVersionNumber() {
        return ModrinthUpdateChecker.stripVersion(versionNumber);
    }
}
