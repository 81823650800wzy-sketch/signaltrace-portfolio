package com.signaltrace.portfolio;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

final class ContentStore {
    private static final String PREFS = "signaltrace_state";
    private static final String LEGACY_PREFS = "portfolio_showcase";
    private static final String LEGACY_CONTENT_KEY = "portfolio_content";
    private static final String ACTIVE_VERSION_KEY = "active_content_version";
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private final Context context;
    private final SharedPreferences preferences;
    private JSONObject content;

    ContentStore(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.content = loadBestAvailable();
    }

    JSONObject get() {
        return content;
    }

    JSONObject object(String key) {
        JSONObject value = content.optJSONObject(key);
        return value == null ? new JSONObject() : value;
    }

    List<JSONObject> objects(String key) {
        List<JSONObject> result = new ArrayList<>();
        JSONArray array = content.optJSONArray(key);
        if (array == null) return result;
        for (int index = 0; index < array.length(); index++) {
            JSONObject value = array.optJSONObject(index);
            if (value != null) result.add(value);
        }
        return result;
    }

    String version() {
        JSONObject meta = object("meta");
        return meta.optString("version", "embedded");
    }

    synchronized void install(byte[] bytes) throws Exception {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("内容包大小不合法");
        }
        JSONObject candidate = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        validate(candidate);

        File directory = new File(context.getFilesDir(), "content");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建内容目录");
        }
        File staging = new File(directory, "staging.json");
        File active = new File(directory, "active.json");
        try (FileOutputStream output = new FileOutputStream(staging)) {
            output.write(bytes);
            output.getFD().sync();
        }
        Files.move(
            staging.toPath(),
            active.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        );
        content = candidate;
        preferences.edit().putString(ACTIVE_VERSION_KEY, version()).apply();
    }

    synchronized void importUri(Uri uri) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalArgumentException("无法读取文件");
            install(readAll(input, MAX_BYTES));
        }
    }

    synchronized void restoreEmbedded() {
        File active = activeFile();
        if (active.exists()) {
            //noinspection ResultOfMethodCallIgnored
            active.delete();
        }
        preferences.edit().remove(ACTIVE_VERSION_KEY).apply();
        content = readEmbedded();
    }

    SharedPreferences preferences() {
        return preferences;
    }

    private JSONObject loadBestAvailable() {
        try {
            File active = activeFile();
            if (active.exists()) {
                try (InputStream input = new FileInputStream(active)) {
                    JSONObject value = new JSONObject(new String(readAll(input, MAX_BYTES), StandardCharsets.UTF_8));
                    validate(value);
                    return value;
                }
            }
        } catch (Exception ignored) {
            // Fall through to the prior app cache, then the embedded package.
        }

        try {
            String legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .getString(LEGACY_CONTENT_KEY, "");
            if (!legacy.isEmpty()) {
                JSONObject value = new JSONObject(legacy);
                validate(value);
                return value;
            }
        } catch (Exception ignored) {
            // Fall through to embedded content.
        }
        return readEmbedded();
    }

    private JSONObject readEmbedded() {
        try (InputStream input = context.getAssets().open("portfolio.json")) {
            JSONObject value = new JSONObject(new String(readAll(input, MAX_BYTES), StandardCharsets.UTF_8));
            validate(value);
            return value;
        } catch (Exception error) {
            throw new IllegalStateException("内置内容包损坏", error);
        }
    }

    private File activeFile() {
        return new File(new File(context.getFilesDir(), "content"), "active.json");
    }

    static void validate(JSONObject value) {
        if (value.optJSONObject("meta") == null
            || value.optJSONObject("profile") == null
            || value.optJSONArray("works") == null
            || value.optJSONArray("journal") == null
            || value.optJSONArray("capabilities") == null) {
            throw new IllegalArgumentException("内容包缺少必要模块");
        }
        if (value.optJSONArray("works").length() == 0 || value.optJSONArray("journal").length() == 0) {
            throw new IllegalArgumentException("内容包不能是空档案");
        }
    }

    static byte[] readAll(InputStream input, int maxBytes) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) throw new IllegalArgumentException("文件超过大小限制");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
