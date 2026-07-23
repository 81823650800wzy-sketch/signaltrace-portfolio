package com.signaltrace.portfolio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

final class UpdateManager {
    interface ProgressListener {
        void onProgress(int percent, String message);
    }

    static final class Manifest {
        final int versionCode;
        final String versionName;
        final int minimumVersionCode;
        final String releaseNotes;
        final String publishedAt;
        final String channel;
        final String contentVersion;
        final String contentUrl;
        final String contentSha256;
        final long contentSize;
        final String apkUrl;
        final String apkSha256;
        final long apkSize;
        final String releasePageUrl;

        Manifest(JSONObject root) {
            versionCode = root.optInt("versionCode", 0);
            versionName = root.optString("versionName", "未知版本");
            minimumVersionCode = root.optInt("minimumSupportedVersionCode", 1);
            releaseNotes = root.optString("releaseNotes", "");
            publishedAt = root.optString("publishedAt", "");
            channel = root.optString("channel", "stable");

            JSONObject pack = root.optJSONObject("contentPack");
            contentVersion = pack == null ? "" : pack.optString("version", "");
            contentUrl = pack == null ? "" : pack.optString("url", "");
            contentSha256 = pack == null ? "" : pack.optString("sha256", "");
            contentSize = pack == null ? 0L : pack.optLong("sizeBytes", 0L);

            JSONObject apk = root.optJSONObject("apk");
            apkUrl = apk == null ? root.optString("apkUrl", "") : apk.optString("url", "");
            apkSha256 = apk == null ? root.optString("apkSha256", "") : apk.optString("sha256", "");
            apkSize = apk == null ? 0L : apk.optLong("sizeBytes", 0L);
            releasePageUrl = root.optString("releasePageUrl", "");
        }

        boolean hasAppUpdate() {
            return versionCode > BuildConfig.VERSION_CODE;
        }

        boolean requiresAppUpdate() {
            return BuildConfig.VERSION_CODE < minimumVersionCode;
        }
    }

    private static final int MANIFEST_LIMIT = 256 * 1024;
    private static final int CONTENT_LIMIT = 2 * 1024 * 1024;
    private static final int APK_LIMIT = 100 * 1024 * 1024;

    private final Context context;
    private final ContentStore contentStore;

    UpdateManager(Context context, ContentStore contentStore) {
        this.context = context.getApplicationContext();
        this.contentStore = contentStore;
    }

    Manifest check() throws Exception {
        String manifestUrl = context.getString(R.string.app_update_manifest_url).trim();
        byte[] bytes = download(manifestUrl, MANIFEST_LIMIT, null, "读取更新清单");
        return new Manifest(new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)));
    }

    boolean installContentIfNeeded(Manifest manifest, ProgressListener listener) throws Exception {
        if (manifest.contentUrl.isEmpty() || manifest.contentVersion.isEmpty()) return false;
        if (manifest.contentVersion.equals(contentStore.version())) return false;
        byte[] bytes = download(manifest.contentUrl, CONTENT_LIMIT, listener, "下载成长资源包");
        verify(bytes, manifest.contentSha256);
        if (listener != null) listener.onProgress(94, "验证内容结构");
        contentStore.install(bytes);
        if (listener != null) listener.onProgress(100, "资源包已原子切换");
        return true;
    }

    void downloadAndOpenApk(Manifest manifest, ProgressListener listener) throws Exception {
        if (!manifest.apkUrl.startsWith("https://")) {
            throw new IllegalStateException("当前版本未提供直接安装包");
        }
        byte[] bytes = download(manifest.apkUrl, APK_LIMIT, listener, "下载 APK 能力包");
        verify(bytes, manifest.apkSha256);

        File directory = new File(context.getCacheDir(), "updates");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建更新缓存");
        }
        File file = new File(directory, "latest.apk");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.getFD().sync();
        }
        if (listener != null) listener.onProgress(100, "安装包校验完成");

        Uri uri = Uri.parse("content://" + context.getPackageName() + ".updates/apk");
        Intent install = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(install);
    }

    void openReleasePage(Manifest manifest) {
        String url = manifest.releasePageUrl.isEmpty() ? manifest.apkUrl : manifest.releasePageUrl;
        if (!url.startsWith("https://")) return;
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private byte[] download(
        String rawUrl,
        int maxBytes,
        ProgressListener listener,
        String label
    ) throws Exception {
        if (!rawUrl.startsWith("https://")) throw new SecurityException("更新仅允许 HTTPS");
        HttpURLConnection connection = (HttpURLConnection) new URL(rawUrl).openConnection();
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json, application/octet-stream, application/vnd.android.package-archive");
        connection.setRequestProperty("User-Agent", "SignalTrace/" + BuildConfig.VERSION_NAME);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IllegalStateException("更新服务器返回 HTTP " + status);
        }
        int expected = connection.getContentLength();
        if (expected > maxBytes) {
            connection.disconnect();
            throw new IllegalArgumentException("更新包超过安全上限");
        }
        try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) throw new IllegalArgumentException("更新包超过安全上限");
                output.write(buffer, 0, read);
                if (listener != null) {
                    int percent = expected > 0 ? Math.min(90, Math.round(total * 90f / expected)) : 35;
                    listener.onProgress(percent, label);
                }
            }
            return output.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private static void verify(byte[] bytes, String expectedSha256) throws Exception {
        if (expectedSha256 == null || expectedSha256.trim().isEmpty()) return;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder actual = new StringBuilder();
        for (byte value : digest.digest(bytes)) {
            actual.append(String.format(Locale.US, "%02x", value & 0xff));
        }
        if (!actual.toString().equalsIgnoreCase(expectedSha256.trim())) {
            throw new SecurityException("更新包完整性校验失败");
        }
    }
}
