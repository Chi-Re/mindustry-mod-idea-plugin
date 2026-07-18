package com.example.generators;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MindustryVersion {

    public enum MindustryVersionKind {
        Stable{
            @Override
            String[] urls() {
                return new String[]{"https://gh.noki.icu/https://api.github.com/repos/Anuken/Mindustry/tags"};
            }
        },

        Build{
            @Override
            String[] urls() {
//                return new String[]{"https://gh.noki.icu/https://api.github.com/repos/Anuken/MindustryBuilds/tags"};
                return new String[]{};
            }

            @Override
            public List<String> getVersions(int page) {
                return List.of("be");
            }
        };

        private HashMap<Integer, List<String>> versions = new HashMap<>();

        abstract String[] urls();

        public List<String> getVersions(int page) {
            if (!versions.containsKey(page)) versions.put(page, new ArrayList<>());

            if (versions.get(page).isEmpty()) {
                versions.put(page, getMiraiVersionList(this, page));
            }

            return versions.get(page).stream().toList();
        }

        public List<String> getVersions() {
            return getVersions(1);
        }
    }

    public static List<String> getMiraiVersionList(MindustryVersionKind kind, int page) {
        String[] urls = kind.urls();

        for (String url : urls) {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpget = new HttpGet(url);

            try(CloseableHttpResponse response = httpclient.execute(httpget)) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                EntityUtils.consume(entity);

                try {
                    response.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return extractVersions(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Connection failed!");
    }
    private static List<String> extractVersions(String context) {
        Pattern pattern = Pattern.compile("name\":\"\\s*(.*?)\\s*\"");
        Matcher matcher = pattern.matcher(context);
        List<String> versionStrings = new ArrayList<>();

        while (matcher.find()) {
            String version = matcher.group(1);
            if (version != null && !version.isEmpty()) {
                versionStrings.add(version);
            }
        }

        return versionStrings;
    }
}
