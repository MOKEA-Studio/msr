package kr.mokea.msr.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.mokea.msr.MsrMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = MsrMod.MOD_ID, value = Dist.CLIENT)
public final class ReleaseUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseUpdater.class);
    private static final URI RELEASE_URI = URI.create("https://api.github.com/repos/MOKEA-Studio/msr/releases/latest");
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final long MAX_JAR_SIZE = 100L * 1024 * 1024;

    private ReleaseUpdater() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        Thread.ofVirtual().name("msr-release-updater").start(ReleaseUpdater::check);
    }

    private static void check() {
        try {
            Path currentJar = ModList.get().getModFileById(MsrMod.MOD_ID).getFile().getFilePath().toRealPath();
            if (!Files.isRegularFile(currentJar) || !currentJar.getFileName().toString().endsWith(".jar")) {
                LOGGER.info("Skipping automatic updates in a development environment");
                return;
            }
            Path modsDir = FMLPaths.MODSDIR.get().toRealPath();
            if (!currentJar.getParent().equals(modsDir)) {
                LOGGER.info("Skipping automatic updates: mod JAR is outside the mods directory");
                return;
            }
            String currentVersion = ModList.get().getModContainerById(MsrMod.MOD_ID).orElseThrow()
                    .getModInfo().getVersion().toString();
            try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10)).build()) {
                HttpRequest request = HttpRequest.newBuilder(RELEASE_URI).timeout(Duration.ofSeconds(20))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "msr-updater").build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    LOGGER.warn("Release check returned HTTP {}", response.statusCode());
                    return;
                }
                JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
                String latestVersion = release.get("tag_name").getAsString().replaceFirst("^v", "");
                if (compareVersions(latestVersion, currentVersion) <= 0) return;
                String assetName = "msr-" + latestVersion + ".jar";
                JsonObject asset = findAsset(release.getAsJsonArray("assets"), assetName);
                if (asset == null || !asset.has("digest")) {
                    LOGGER.warn("Release {} has no matching JAR with SHA-256 digest", latestVersion);
                    return;
                }
                String digest = asset.get("digest").getAsString();
                if (!digest.matches("sha256:[0-9a-fA-F]{64}")) {
                    LOGGER.warn("Release {} has no valid SHA-256 digest", latestVersion);
                    return;
                }
                long size = asset.get("size").getAsLong();
                if (size <= 0 || size > MAX_JAR_SIZE) {
                    LOGGER.warn("Release asset has invalid size: {}", size);
                    return;
                }
                URI downloadUri = URI.create(asset.get("browser_download_url").getAsString());
                if (!"https".equals(downloadUri.getScheme()) || !"github.com".equals(downloadUri.getHost())) {
                    LOGGER.warn("Unexpected release download URL: {}", downloadUri);
                    return;
                }
                Path stagingDir = FMLPaths.GAMEDIR.get().resolve(".msr-updates");
                Files.createDirectories(stagingDir);
                Path staged = stagingDir.resolve(assetName + ".pending");
                Path temp = stagingDir.resolve(assetName + ".part");
                try {
                    HttpRequest download = HttpRequest.newBuilder(downloadUri).timeout(Duration.ofMinutes(3))
                            .header("User-Agent", "msr-updater").build();
                    HttpResponse<InputStream> jarResponse = client.send(download, HttpResponse.BodyHandlers.ofInputStream());
                    if (jarResponse.statusCode() != 200) throw new IllegalStateException("Download HTTP " + jarResponse.statusCode());
                    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    long count = 0;
                    try (InputStream input = jarResponse.body(); var output = Files.newOutputStream(temp)) {
                        byte[] buffer = new byte[8192];
                        int n;
                        while ((n = input.read(buffer)) != -1) {
                            count += n;
                            if (count > MAX_JAR_SIZE) throw new IllegalStateException("Release JAR exceeds size limit");
                            sha256.update(buffer, 0, n);
                            output.write(buffer, 0, n);
                        }
                    }
                    if (count != size || !HexFormat.of().formatHex(sha256.digest()).equalsIgnoreCase(digest.substring(7))) {
                        throw new IllegalStateException("Release JAR size or SHA-256 mismatch");
                    }
                    Files.move(temp, staged, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temp);
                }
                Path target = modsDir.resolve(assetName);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> launchSwapper(currentJar, staged, target, stagingDir), "msr-update-swap"));
                LOGGER.info("MSR {} downloaded and verified. It will be installed when Minecraft closes.", latestVersion);
            }
        } catch (Exception e) {
            LOGGER.warn("Automatic update check failed: {}", e.toString());
        }
    }

    private static JsonObject findAsset(JsonArray assets, String name) {
        for (var element : assets) {
            JsonObject asset = element.getAsJsonObject();
            if (name.equals(asset.get("name").getAsString())) return asset;
        }
        return null;
    }

    private static int compareVersions(String left, String right) {
        Matcher a = VERSION.matcher(left);
        Matcher b = VERSION.matcher(right);
        if (!a.matches() || !b.matches()) return -1;
        for (int i = 1; i <= 3; i++) {
            int compared = Integer.compare(Integer.parseInt(a.group(i)), Integer.parseInt(b.group(i)));
            if (compared != 0) return compared;
        }
        return 0;
    }

    private static void launchSwapper(Path current, Path staged, Path target, Path stagingDir) {
        try {
            Path classFile = stagingDir.resolve("kr/mokea/msr/update/ReleaseSwapHelper.class");
            Files.createDirectories(classFile.getParent());
            try (InputStream source = ReleaseUpdater.class.getResourceAsStream("ReleaseSwapHelper.class")) {
                if (source == null) throw new IllegalStateException("Updater helper is missing");
                Files.copy(source, classFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Path java = Path.of(System.getProperty("java.home"), "bin", "java");
            new ProcessBuilder(java.toString(), "-cp", stagingDir.toString(),
                    ReleaseSwapHelper.class.getName(), Long.toString(ProcessHandle.current().pid()),
                    current.toString(), staged.toString(), target.toString()).start();
        } catch (Exception e) {
            LOGGER.warn("Could not start update installer: {}", e.toString());
        }
    }
}
