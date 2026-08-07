package chire.idea.mindustry.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.system.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@State(name = "MindustrySettings", storages = @Storage("mindustry-settings.xml"))
public final class MindustrySettingsState implements PersistentStateComponent<MindustrySettingsState> {
    public static final String DEFAULT_MIRROR = "https://gh.noki.icu/";

    public String gameJarPath = defaultGameJarPath();
    public String modsDirectory = defaultModsDirectory();
    public String versionKind = "Stable";
    public String gameVersion = "";
    public String mirrorUrl = DEFAULT_MIRROR;
    public List<MirrorEntry> mirrors = defaultMirrors();

    public static MindustrySettingsState getInstance() {
        return ApplicationManager.getApplication().getService(MindustrySettingsState.class);
    }

    public static String defaultGameJarPath() {
        return Path.of(System.getProperty("user.home"), ".mindustry", "Mindustry.jar").toString();
    }

    public static String defaultModsDirectory() {
        if (OS.CURRENT == OS.Windows) {
            return Path.of(System.getProperty("user.home"), "AppData", "Roaming", "Mindustry", "mods").toString();
        }
        return Path.of(System.getProperty("user.home"), ".local", "share", "Mindustry", "mods").toString();
    }

    private static List<MirrorEntry> defaultMirrors() {
        List<MirrorEntry> list = new ArrayList<>();
        list.add(new MirrorEntry(DEFAULT_MIRROR, true));
        return list;
    }

    /**
     * Prefix mode: the mirror URL is prepended to the original URL (e.g.
     * {@code https://gh.noki.icu/https://api.github.com/...}). If disabled, the original URL is used directly.
     */
    public static String applyMirror(String originalUrl, @Nullable String mirrorUrl, boolean prefixMode) {
        if (mirrorUrl == null || mirrorUrl.isBlank()) {
            return originalUrl;
        }
        return prefixMode ? mirrorUrl + originalUrl : originalUrl;
    }

    /**
     * The currently effective mirror URL from the settings.
     */
    public static String currentMirrorUrl() {
        return getInstance().mirrorUrl;
    }

    /**
     * Whether the currently effective mirror uses prefix mode.
     */
    public static boolean currentMirrorPrefix() {
        return getInstance().isCurrentMirrorPrefix();
    }

    public String applyMirror(String originalUrl) {
        return applyMirror(originalUrl, mirrorUrl, currentMirrorPrefix());
    }

    private boolean isCurrentMirrorPrefix() {
        for (MirrorEntry entry : mirrors) {
            if (mirrorUrl != null && mirrorUrl.equals(entry.url)) {
                return entry.prefixMode;
            }
        }
        return true;
    }

    @Override
    public @Nullable MindustrySettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MindustrySettingsState state) {
        gameJarPath = state.gameJarPath;
        modsDirectory = state.modsDirectory;
        versionKind = state.versionKind;
        gameVersion = state.gameVersion;
        mirrorUrl = state.mirrorUrl;
        mirrors = new ArrayList<>();
        for (MirrorEntry entry : state.mirrors) {
            mirrors.add(new MirrorEntry(entry.url, entry.prefixMode));
        }
    }

    public static class MirrorEntry {
        public String url = DEFAULT_MIRROR;
        public boolean prefixMode = true;

        public MirrorEntry() {
        }

        public MirrorEntry(String url, boolean prefixMode) {
            this.url = url;
            this.prefixMode = prefixMode;
        }
    }
}
