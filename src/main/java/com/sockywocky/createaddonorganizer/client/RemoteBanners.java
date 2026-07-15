package com.sockywocky.createaddonorganizer.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.neoforged.fml.loading.FMLPaths;

public final class RemoteBanners {
    private static final Gson GSON = new Gson();
    private static final Path REMOTE_DIR = FMLPaths.CONFIGDIR.get().resolve("createaddonorganizer/remote_banners");
    private static final Path MANIFEST_CACHE = REMOTE_DIR.resolve("manifest.json");
    private static final Path ETAG_FILE = REMOTE_DIR.resolve("manifest.etag");
    private static final String RAW_FALLBACK_URL =
            "https://raw.githubusercontent.com/SockyWocky7/createaddonorganizer/master/banners/index.json";

    private static final AtomicBoolean SYNC_STARTED = new AtomicBoolean(false);

    private static volatile List<RemoteContributor> contributors = List.of();
    private static volatile Set<String> availableFiles = Set.of();
    private static volatile boolean everCached = false;

    private RemoteBanners() {}

    public record RemoteBannerFile(String file, int v) {}

    public record RemoteContributor(String name, String color, List<RemoteBannerFile> banners) {}

    private record FetchResult(boolean notModified, byte[] body, String etag, String baseUrl) {}

    public static void loadCacheFromDisk() {
        try {
            if (!Files.exists(MANIFEST_CACHE)) {
                everCached = false;
                return;
            }
            List<RemoteContributor> parsed = parseManifest(Files.readAllBytes(MANIFEST_CACHE));
            if (parsed == null) {
                contributors = List.of();
                availableFiles = Set.of();
                everCached = false;
                return;
            }
            contributors = parsed;
            availableFiles = computeAvailable(parsed);
            everCached = true;
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to load cached remote banner manifest", e);
            contributors = List.of();
            availableFiles = Set.of();
            everCached = false;
        }
    }

    public static void syncAsync() {
        if (!SYNC_STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(RemoteBanners::sync, "createaddonorganizer-banner-sync");
        thread.setDaemon(true);
        thread.start();
    }

    public static List<RemoteContributor> contributors() {
        return contributors;
    }

    public static boolean isAvailable(String filename) {
        return availableFiles.contains(filename);
    }

    public static boolean isCachedOnDisk(String filename) {
        return Files.exists(REMOTE_DIR.resolve(filename));
    }

    public static Path fileFor(String filename) {
        return REMOTE_DIR.resolve(filename);
    }

    public static Set<String> availableFilenames() {
        return availableFiles;
    }

    public static boolean hasEverCached() {
        return everCached;
    }

    private static void sync() {
        try {
            if (!Config.fetchOnlineBanners()) {
                return;
            }
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            String etag = readEtag();

            FetchResult result = fetchManifest(client, Config.bannerManifestUrl(), etag);
            if (result == null) {
                result = fetchManifest(client, RAW_FALLBACK_URL, etag);
            }
            if (result == null) {
                createaddonorganizer.LOGGER.warn("[CAO] remote banner manifest fetch failed (primary and fallback)");
                return;
            }

            Map<String, Integer> previousVersions = versionsOf(contributors);

            if (result.notModified()) {
                downloadMissingOrChanged(client, result.baseUrl(), contributors, previousVersions);
                availableFiles = computeAvailable(contributors);
                return;
            }

            List<RemoteContributor> newContributors = parseManifest(result.body());
            if (newContributors == null) {
                return;
            }
            downloadMissingOrChanged(client, result.baseUrl(), newContributors, previousVersions);

            writeAtomic(MANIFEST_CACHE, result.body());
            if (result.etag() != null) {
                writeAtomic(ETAG_FILE, result.etag().getBytes(StandardCharsets.UTF_8));
            }

            contributors = newContributors;
            availableFiles = computeAvailable(newContributors);
            everCached = true;
        } catch (Exception e) {
            createaddonorganizer.LOGGER.warn("[CAO] remote banner sync failed", e);
        }
    }

    private static FetchResult fetchManifest(HttpClient client, String url, String etag) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET();
            if (etag != null && !etag.isBlank()) {
                builder.header("If-None-Match", etag);
            }
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            String baseUrl = url.substring(0, url.lastIndexOf('/') + 1);
            if (response.statusCode() == 304) {
                return new FetchResult(true, null, etag, baseUrl);
            }
            if (response.statusCode() != 200) {
                return null;
            }
            String newEtag = response.headers().firstValue("ETag").orElse(null);
            return new FetchResult(false, response.body(), newEtag, baseUrl);
        } catch (IOException | InterruptedException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to fetch remote banner manifest from {}", url, e);
            return null;
        }
    }

    private static void downloadMissingOrChanged(HttpClient client, String baseUrl,
            List<RemoteContributor> targetContributors, Map<String, Integer> previousVersions) {
        for (RemoteContributor contributor : targetContributors) {
            for (RemoteBannerFile file : contributor.banners()) {
                Path target = REMOTE_DIR.resolve(file.file());
                boolean missing = !Files.exists(target);
                boolean changed = previousVersions.getOrDefault(file.file(), -1) != file.v();
                if (!missing && !changed) {
                    continue;
                }
                downloadPng(client, baseUrl + file.file(), target);
            }
        }
    }

    private static void downloadPng(HttpClient client, String url, Path target) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                createaddonorganizer.LOGGER.warn("[CAO] remote banner download failed ({}): {}", response.statusCode(), url);
                return;
            }
            byte[] bytes = response.body();
            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                    NativeImage image = NativeImage.read(in)) {
            }
            writeAtomic(target, bytes);
        } catch (IOException | InterruptedException e) {
            createaddonorganizer.LOGGER.warn("[CAO] remote banner download failed: {}", url, e);
        }
    }

    private static List<RemoteContributor> parseManifest(byte[] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        RemoteContributor[] parsed;
        try {
            parsed = GSON.fromJson(json, RemoteContributor[].class);
        } catch (JsonSyntaxException e) {
            createaddonorganizer.LOGGER.warn("[CAO] remote banner manifest is not valid JSON; ignoring it", e);
            return null;
        }
        if (parsed == null) {
            return List.of();
        }
        List<RemoteContributor> sanitized = new ArrayList<>(parsed.length);
        for (RemoteContributor contributor : parsed) {
            if (contributor == null || contributor.name() == null || contributor.banners() == null) {
                createaddonorganizer.LOGGER.warn("[CAO] skipping malformed contributor entry in banner manifest: {}", contributor);
                continue;
            }
            List<RemoteBannerFile> files = new ArrayList<>(contributor.banners().size());
            for (RemoteBannerFile file : contributor.banners()) {
                if (file == null || file.file() == null || !isSafeFilename(file.file())) {
                    createaddonorganizer.LOGGER.warn("[CAO] skipping malformed banner entry {} for contributor \"{}\"",
                            file, contributor.name());
                    continue;
                }
                files.add(file);
            }
            sanitized.add(files.size() == contributor.banners().size() ? contributor
                    : new RemoteContributor(contributor.name(), contributor.color(), files));
        }
        return List.copyOf(sanitized);
    }

    private static boolean isSafeFilename(String name) {
        return name.matches("[A-Za-z0-9._-]+\\.png");
    }

    private static Set<String> computeAvailable(List<RemoteContributor> manifestContributors) {
        Set<String> listed = new HashSet<>();
        for (RemoteContributor contributor : manifestContributors) {
            for (RemoteBannerFile file : contributor.banners()) {
                listed.add(file.file());
            }
        }
        Set<String> present = new HashSet<>();
        if (Files.isDirectory(REMOTE_DIR)) {
            try (var files = Files.list(REMOTE_DIR)) {
                files.map(p -> p.getFileName().toString())
                        .filter(listed::contains)
                        .forEach(present::add);
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to list cached remote banners", e);
            }
        }
        return present;
    }

    private static Map<String, Integer> versionsOf(List<RemoteContributor> list) {
        Map<String, Integer> map = new HashMap<>();
        for (RemoteContributor contributor : list) {
            for (RemoteBannerFile file : contributor.banners()) {
                map.put(file.file(), file.v());
            }
        }
        return map;
    }

    private static String readEtag() {
        try {
            if (Files.exists(ETAG_FILE)) {
                return Files.readString(ETAG_FILE, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to read cached banner manifest etag", e);
        }
        return null;
    }

    private static void writeAtomic(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
