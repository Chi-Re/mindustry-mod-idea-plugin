package com.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        int a = 10;

        if (a > 10) {
            System.out.println(a);
        } else if (a < 10) {
            System.out.println(a);
        }
    }


//    public static Set<String> getMiraiVersionList() throws IOException {
//        // 从阿里云镜像下载，失败则回退 Maven Central
//        String[] urls = {
//                "https://gh.noki.icu/https://api.github.com/repos/Anuken/Mindustry/tags"
//        };
//
//        for (String url : urls) {
//            CloseableHttpClient httpclient = HttpClients.createDefault();
//            HttpGet httpget = new HttpGet(url);
//            CloseableHttpResponse response = httpclient.execute(httpget);
//
//            try {
//                HttpEntity entity = response.getEntity();
//                String result = EntityUtils.toString(entity);
//                EntityUtils.consume(entity);
//
//                return extractVersions(result);
//            } finally {
//                response.close();
//            }
//        }
//
//        return Collections.singleton("null");
//    }
//    private static Set<String> extractVersions(String context) {
//        Pattern pattern = Pattern.compile("name\":\"\\s*(.*?)\\s*\"");
//        Matcher matcher = pattern.matcher(context);
//        List<String> versionStrings = new ArrayList<>();
//
//        while (matcher.find()) {
//            String version = matcher.group(1);
//            if (version != null && !version.isEmpty()) {
//                versionStrings.add(version);
//            }
//        }
//
//        return versionStrings.stream()
//                .sorted(Comparator.reverseOrder())
//                .map(String::new)
//                .collect(Collectors.toSet());
//    }
}
