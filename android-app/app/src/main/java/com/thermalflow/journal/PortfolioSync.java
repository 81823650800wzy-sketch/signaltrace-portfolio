package com.signaltrace.portfolio;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class PortfolioSync {
    static final String PREFS_NAME = "portfolio_showcase";
    static final String CONTENT_KEY = "portfolio_content";
    static final String LAST_SYNC_AT_KEY = "last_sync_at";
    static final String LAST_SYNC_MESSAGE_KEY = "last_sync_message";
    static final String UPDATE_VERSION_CODE_KEY = "update_version_code";
    static final String UPDATE_VERSION_NAME_KEY = "update_version_name";
    static final String UPDATE_NOTES_KEY = "update_notes";
    static final String UPDATE_URL_KEY = "update_url";
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private PortfolioSync() { }

    static Result sync(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean contentChanged = false;
        String contentMessage;

        try {
            String contentUrl = context.getString(R.string.portfolio_content_url).trim();
            if (contentUrl.isEmpty()) throw new IllegalStateException("未配置公开内容地址");
            JSONObject content = new JSONObject(fetchHttps(contentUrl));
            validateContent(content);
            String normalized = content.toString();
            String current = preferences.getString(CONTENT_KEY, "");
            if (!normalized.equals(current)) {
                preferences.edit().putString(CONTENT_KEY, normalized).apply();
                contentChanged = true;
                contentMessage = "已获取内容版本 " + content.optJSONObject("meta").optString("version", "new");
            } else {
                contentMessage = "内容已是最新版本";
            }
        } catch (Exception error) {
            contentMessage = "联网更新失败，继续使用离线缓存";
        }

        try {
            String manifestUrl = context.getString(R.string.app_update_manifest_url).trim();
            if (!manifestUrl.isEmpty()) {
                JSONObject update = new JSONObject(fetchHttps(manifestUrl));
                int remoteVersionCode = update.optInt("versionCode", 0);
                String openUrl = update.optString("releasePageUrl", update.optString("apkUrl", ""));
                if (remoteVersionCode > BuildConfig.VERSION_CODE && openUrl.startsWith("https://")) {
                    preferences.edit()
                        .putInt(UPDATE_VERSION_CODE_KEY, remoteVersionCode)
                        .putString(UPDATE_VERSION_NAME_KEY, update.optString("versionName", "新版本"))
                        .putString(UPDATE_NOTES_KEY, update.optString("releaseNotes", ""))
                        .putString(UPDATE_URL_KEY, openUrl)
                        .apply();
                }
            }
        } catch (Exception ignored) {
            // Content updates and binary version checks fail independently.
        }

        preferences.edit()
            .putLong(LAST_SYNC_AT_KEY, System.currentTimeMillis())
            .putString(LAST_SYNC_MESSAGE_KEY, contentMessage)
            .apply();
        return new Result(contentChanged, contentMessage);
    }

    private static void validateContent(JSONObject content) {
        if (!content.has("meta") || !content.has("profile") || !content.has("capabilities") || !content.has("works") || !content.has("models")) {
            throw new IllegalArgumentException("作品集内容结构不完整");
        }
        if (content.optJSONArray("works").length() == 0 || content.optJSONArray("models").length() == 0) {
            throw new IllegalArgumentException("作品集至少需要一个成果和模型入口");
        }
    }

    private static String fetchHttps(String rawUrl) throws Exception {
        if (!rawUrl.startsWith("https://")) throw new SecurityException("只允许 HTTPS 更新地址");
        HttpURLConnection connection = (HttpURLConnection) new URL(rawUrl).openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(9000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SignalTrace-Portfolio/" + BuildConfig.VERSION_NAME);
        connection.setInstanceFollowRedirects(true);
        int statusCode = connection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IllegalStateException("HTTP " + statusCode);
        }
        try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = stream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_RESPONSE_BYTES) throw new IllegalArgumentException("更新内容超过大小限制");
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } finally {
            connection.disconnect();
        }
    }

    static final class Result {
        final boolean contentChanged;
        final String message;

        Result(boolean contentChanged, String message) {
            this.contentChanged = contentChanged;
            this.message = message;
        }
    }
}
