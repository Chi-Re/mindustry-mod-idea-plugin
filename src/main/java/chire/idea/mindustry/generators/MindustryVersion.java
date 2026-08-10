package chire.idea.mindustry.generators;

import chire.idea.mindustry.settings.MindustrySettingsState;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MindustryVersion {
    public static final int PAGE_SIZE = 100;

    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    public enum MindustryVersionKind {
        Stable {
            @Override
            String[] urls() {
                return new String[]{"https://api.github.com/repos/Anuken/Mindustry/tags"};
            }
        },

        Build {
            @Override
            String[] urls() {
                return new String[]{"https://api.github.com/repos/Anuken/MindustryBuilds/tags"};
            }
        };

        private final ConcurrentHashMap<Integer, List<String>> versions = new ConcurrentHashMap<>();

        abstract String[] urls();

        public List<String> getVersions() {
            return getVersions(1, false);
        }

        public List<String> getVersions(int page) {
            return getVersions(page, false);
        }

        public List<String> getVersions(int page, boolean force) {
            return getVersions(page, force, MindustrySettingsState.currentMirrorUrl(), MindustrySettingsState.currentMirrorPrefix());
        }

        public List<String> getVersions(int page, boolean force, String mirrorUrl, boolean prefixMode) {
            if (force) {
                versions.remove(page);
            }

            if (!versions.containsKey(page)) {
                versions.put(page, new ArrayList<>());
            }

            if (versions.get(page).isEmpty()) {
                versions.put(page, MindustryVersion.getMiraiVersionList(this, page, mirrorUrl, prefixMode));
            }

            return new ArrayList<>(versions.get(page));
        }

        public void clearCache() {
            versions.clear();
        }

        public static void clearAllCache() {
            for (MindustryVersionKind kind : values()) {
                kind.clearCache();
            }
        }
    }

    public static List<String> getMiraiVersionList(MindustryVersionKind kind, int page, String mirrorUrl, boolean prefixMode) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10_000)
                .setSocketTimeout(15_000)
                .build();

        IOException lastError = null;

        try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            for (String url : kind.urls()) {
                String mirrored = MindustrySettingsState.applyMirror(url, mirrorUrl, prefixMode);

                LinkedHashSet<String> candidates = new LinkedHashSet<>();
                candidates.add(mirrored);
                candidates.add(url);

                for (String candidate : candidates) {
                    String requestUrl = candidate + "?per_page=" + PAGE_SIZE + "&page=" + page;

                    try (CloseableHttpResponse response = httpclient.execute(new HttpGet(requestUrl))) {
                        HttpEntity entity = response.getEntity();
                        String result = EntityUtils.toString(entity);
                        EntityUtils.consume(entity);

                        List<String> versions = extractVersions(result);

                        if (!versions.isEmpty()) {
                            return versions;
                        }
                    } catch (IOException e) {
                        lastError = e;
                    }
                }
            }
        } catch (IOException e) {
            lastError = e;
        }

        throw new RuntimeException("Connection failed!" + (lastError == null ? "" : " " + lastError.getMessage()), lastError);
    }

    private static List<String> extractVersions(String context) {
        Matcher matcher = TAG_NAME_PATTERN.matcher(context);
        List<String> versionStrings = new ArrayList<>();

        while (matcher.find()) {
            String version = matcher.group(1);
            if (isVersionTag(version)) {
                versionStrings.add(version);
            }
        }

        return versionStrings;
    }

    private static boolean isVersionTag(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        return version.matches("[vV]?\\d+([.\\w-]+)?");
    }
}
