package chire.idea.mindustry.settings;

import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class MindustryGameDownloader {
    private MindustryGameDownloader() {
    }

    public static boolean isBuildKind(@NotNull String kind) {
        return "Build".equals(kind);
    }

    public static @NotNull String buildDownloadUrl(@NotNull String kind, @NotNull String version) {
        return MindustrySettingsState.getInstance().applyMirror(baseDownloadUrl(kind, version));
    }

    public static @NotNull String buildDownloadUrl(@NotNull String kind, @NotNull String version,
                                                   @NotNull String mirrorUrl, boolean prefixMode) {
        String url = baseDownloadUrl(kind, version);
        return MindustrySettingsState.applyMirror(url, mirrorUrl, prefixMode);
    }

    private static @NotNull String baseDownloadUrl(@NotNull String kind, @NotNull String version) {
        if (isBuildKind(kind)) {
            return "https://github.com/Anuken/MindustryBuilds/releases/download/" + version
                    + "/Mindustry-BE-Desktop-" + version + ".jar";
        }
        return "https://github.com/Anuken/Mindustry/releases/download/" + version + "/Mindustry.jar";
    }

    public static @NotNull String fileNameFor(@NotNull String kind, @NotNull String version) {
        return (isBuildKind(kind) ? "Mindustry-BE-" : "Mindustry-") + version + ".jar";
    }

    public static void download(@NotNull String url, @NotNull Path target) throws IOException {
        Path absolute = target.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.deleteIfExists(tmp);

        HttpRequests.request(url)
                .productNameAsUserAgent()
                .redirectLimit(10)
                .connect(request -> {
                    try (InputStream in = request.getInputStream();
                         OutputStream out = Files.newOutputStream(tmp)) {
                        in.transferTo(out);
                    }
                    return null;
                });

        Files.move(tmp, absolute, StandardCopyOption.REPLACE_EXISTING);
    }
}
