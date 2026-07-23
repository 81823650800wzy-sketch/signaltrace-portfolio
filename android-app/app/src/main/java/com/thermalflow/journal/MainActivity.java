package com.signaltrace.portfolio;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int IMPORT_CONTENT_REQUEST = 1001;
    private static final int ATTACH_MODEL_REQUEST = 1002;
    private static final int ATTACH_MEDIA_REQUEST = 1003;
    private static final String PREFS_NAME = "portfolio_showcase";
    private static final String CONTENT_KEY = "portfolio_content";
    private static final String MODEL_URI_PREFIX = "model_uri_";
    private static final String EVIDENCE_VIDEO_URI_KEY = "evidence_video_hydrogen_response";
    private static final int INK = Color.rgb(16, 23, 29);
    private static final int INK_SOFT = Color.rgb(30, 40, 48);
    private static final int MUTED = Color.rgb(91, 103, 103);
    private static final int TEAL = Color.rgb(59, 193, 183);
    private static final int AMBER = Color.rgb(244, 200, 74);
    private static final int RED = Color.rgb(232, 91, 79);
    private static final int SURFACE = Color.rgb(223, 227, 225);
    private static final int BORDER = Color.rgb(174, 182, 181);

    private SharedPreferences preferences;
    private JSONObject contentPack;
    private LinearLayout body;
    private String activePage = "profile";
    private String pendingModelId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        contentPack = loadContent();
        setContentView(buildScreen());
        PortfolioSyncJobService.schedule(this);
        syncNow(false);
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SURFACE);

        root.addView(buildTopBar());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(28));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        root.addView(buildNavigation());
        renderPage();
        return root;
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(10), dp(14), dp(10));
        bar.setBackgroundColor(INK);

        TextView mark = new TextView(this);
        mark.setText("ST");
        mark.setTextColor(INK);
        mark.setTextSize(12);
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(null, 1);
        mark.setBackground(tacticalShape(AMBER, AMBER, 8));
        bar.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        TextView title = text("SIGNALTRACE / FIELD ARCHIVE", 13, Color.WHITE, true);
        TextView subtitle = text("仪控实习证据 · 产品案例 · 离线可用", 10, Color.rgb(155, 167, 164), false);
        copy.addView(title);
        copy.addView(subtitle);
        bar.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        TextView offline = text("OFFLINE", 10, TEAL, true);
        offline.setPadding(dp(9), dp(5), dp(9), dp(5));
        offline.setBackground(tacticalShape(INK_SOFT, TEAL, 6));
        bar.addView(offline);
        return bar;
    }

    private View buildNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(6), dp(7), dp(6), dp(8));
        nav.setBackgroundColor(INK);
        String[][] tabs = {{"名片", "profile"}, {"看板", "dashboard"}, {"日志", "journal"}, {"复盘", "evidence"}, {"成果", "work"}, {"更新", "update"}};
        for (String[] tab : tabs) {
            Button button = new Button(this);
            button.setText(tab[0]);
            button.setAllCaps(false);
            button.setTextSize(12);
            button.setTextColor(tab[1].equals(activePage) ? INK : Color.rgb(167, 179, 176));
            button.setBackground(tab[1].equals(activePage) ? tacticalShape(AMBER, AMBER, 6) : tacticalShape(INK, INK, 6));
            button.setOnClickListener(view -> {
                activePage = tab[1];
                renderPage();
                nav.invalidate();
                setContentView(buildScreen());
            });
            nav.addView(button, new LinearLayout.LayoutParams(0, dp(44), 1));
        }
        return nav;
    }

    private void renderPage() {
        body.removeAllViews();
        if ("profile".equals(activePage)) renderProfile();
        if ("dashboard".equals(activePage)) renderDashboard();
        if ("journal".equals(activePage)) renderJournal();
        if ("evidence".equals(activePage)) renderEvidence();
        if ("work".equals(activePage)) renderWork();
        if ("models".equals(activePage)) renderModels();
        if ("update".equals(activePage)) renderUpdate();
    }

    private void renderProfile() {
        JSONObject profile = contentPack.optJSONObject("profile");
        body.addView(hero(profile));
        addGap(20);
        body.addView(eyebrow("CURRENT FOCUS"));
        body.addView(heading(profile.optString("headline", "把现场学习转化为可阅读的技术作品")));
        addGap(10);
        body.addView(paragraph(profile.optString("summary", "围绕流程理解、仪控信号和证据化复盘，持续构建可验证的学习档案。")));
        addGap(22);
        body.addView(eyebrow("CAPABILITY EVIDENCE"));
        for (JSONObject capability : objects("capabilities")) {
            body.addView(capabilityCard(capability), fullWidthTop(10));
        }
        addGap(22);
        body.addView(eyebrow("PORTFOLIO STATUS"));
        body.addView(statusStrip());
        addGap(18);
        body.addView(quickActionPanel());
    }

    private void renderDashboard() {
        body.addView(eyebrow("FIELD INTELLIGENCE DASHBOARD"));
        body.addView(heading("用数据、流程和证据看见实习进展"));
        body.addView(paragraph("这里把日志、成果、产品案例和响应特性分析放进同一张看板。它不是生产监控，而是公开安全的学习证据总览。"), fullWidthTop(10));
        body.addView(statusStrip(), fullWidthTop(18));
        body.addView(categoryMatrix(), fullWidthTop(16));
        body.addView(responseChartCard(), fullWidthTop(16));
        body.addView(responseMetricsCard(), fullWidthTop(16));
        body.addView(methodRail(), fullWidthTop(16));
    }

    private void renderJournal() {
        body.addView(eyebrow("FIELD JOURNAL / " + objects("journal").size() + " EVENTS"));
        body.addView(heading("真实日志与学习证据"));
        body.addView(paragraph("原始记录经过工作内容筛选和逐条脱敏。每条日志同时说明现场观察、本人参与、学习结论与产品信号。"), fullWidthTop(10));
        body.addView(categoryMatrix(), fullWidthTop(16));
        int index = 0;
        for (JSONObject entry : objects("journal")) {
            body.addView(journalCard(entry, index), fullWidthTop(12));
            index++;
        }
    }

    private void renderEvidence() {
        body.addView(eyebrow("EVIDENCE ROOM / RESPONSE + MEDIA"));
        body.addView(heading("氢气纯度仪响应等待复盘"));
        body.addView(paragraph("新增原始视频的本地证据入口、响应曲线和五步复盘流程。公开视频包只保存脱敏结论；原始视频通过系统文件选择器保留在本机。"), fullWidthTop(10));
        body.addView(videoEvidenceCard(), fullWidthTop(18));
        body.addView(responseChartCard(), fullWidthTop(16));
        body.addView(responseDecisionCard(), fullWidthTop(16));
        body.addView(evidenceFlowCard(), fullWidthTop(16));
    }

    private View hero(JSONObject profile) {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(tacticalShape(INK, INK, 14));
        ImageView image = new ImageView(this);
        image.setImageResource(getResources().getIdentifier("portfolio_cover", "drawable", getPackageName()));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setAlpha(0.62f);
        hero.addView(image, new FrameLayout.LayoutParams(-1, dp(230)));

        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.BOTTOM);
        overlay.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView label = text(profile.optString("role", "仪控学习者 / 工业数字化产品方向"), 12, Color.rgb(199, 231, 227), true);
        TextView name = text(profile.optString("name", "个人学习作品集"), 29, Color.WHITE, true);
        TextView note = text(objects("journal").size() + "条实际日志 · " + objects("works").size() + "项成果 · " + objects("productCases").size() + "个产品经理案例", 12, Color.rgb(224, 232, 231), false);
        overlay.addView(label);
        overlay.addView(name, fullWidthTop(5));
        overlay.addView(note, fullWidthTop(8));
        hero.addView(overlay, new FrameLayout.LayoutParams(-1, dp(230)));
        return hero;
    }

    private void renderWork() {
        body.addView(eyebrow("SELECTED WORK / PRODUCT THINKING"));
        body.addView(heading("把现场学习整理为可追溯的作品"));
        body.addView(paragraph("内容来自公开资料、合成演示与经过复核的脱敏学习结论；不包含生产系统、现场参数或未授权信息。"), fullWidthTop(10));
        for (JSONObject item : objects("works")) body.addView(workCard(item), fullWidthTop(14));
        addGap(24);
        body.addView(eyebrow("PRODUCT CASE"));
        for (JSONObject item : objects("productCases")) body.addView(productCaseCard(item), fullWidthTop(12));
        addGap(24);
        body.addView(eyebrow("MODEL + MEDIA"));
        renderModels();
    }

    private void renderModels() {
        body.addView(eyebrow("PROCESS LIBRARY"));
        body.addView(heading("流程与模型资产"));
        body.addView(paragraph("这里是后续流程模型、实景结合素材和互动演示的统一入口。模型资产可以独立关联到本机，内容索引则由离线内容包更新。"), fullWidthTop(10));
        for (JSONObject model : objects("models")) body.addView(modelCard(model), fullWidthTop(14));
    }

    private void renderUpdate() {
        body.addView(eyebrow("NETWORK UPDATE + OFFLINE CACHE"));
        body.addView(heading("自动获取公开更新，断网继续访问"));
        body.addView(paragraph("App 启动时和系统定时任务会检查公开 HTTPS 内容地址。验证通过后保存为新的离线缓存；网络异常不会清空最后一次成功内容。"), fullWidthTop(10));

        Button syncButton = primaryButton("立即检查网络更新");
        syncButton.setOnClickListener(view -> syncNow(true));
        body.addView(syncButton, fullWidthTop(20));

        Button importButton = secondaryButton("从本机导入 portfolio.json");
        importButton.setOnClickListener(view -> importContent());
        body.addView(importButton, fullWidthTop(10));

        Button copyButton = secondaryButton("复制内容包模板说明");
        copyButton.setOnClickListener(view -> copyTemplate());
        body.addView(copyButton, fullWidthTop(10));

        Button resetButton = secondaryButton("恢复内置作品集");
        resetButton.setOnClickListener(view -> {
            preferences.edit().remove(CONTENT_KEY).apply();
            contentPack = loadContent();
            renderPage();
            Toast.makeText(this, "已恢复内置作品集", Toast.LENGTH_SHORT).show();
        });
        body.addView(resetButton, fullWidthTop(10));

        JSONObject meta = contentPack.optJSONObject("meta");
        body.addView(metaCard("当前内容版本", meta.optString("version", "内置版本"), meta.optString("updatedAt", "未标注日期")), fullWidthTop(22));
        body.addView(metaCard("同步状态", preferences.getString(PortfolioSync.LAST_SYNC_MESSAGE_KEY, "尚未检查网络更新"), formatSyncTime()), fullWidthTop(10));
        body.addView(metaCard("离线状态", "最后成功内容已保存在设备", "无网络时自动使用缓存与内置兜底内容"), fullWidthTop(10));
        body.addView(metaCard("模型策略", "GLB 等文件保持在本机", "可为每个流程卡关联本地模型文件"), fullWidthTop(10));

        String updateUrl = preferences.getString(PortfolioSync.UPDATE_URL_KEY, "");
        int updateVersionCode = preferences.getInt(PortfolioSync.UPDATE_VERSION_CODE_KEY, 0);
        if (!updateUrl.isEmpty() && updateVersionCode > BuildConfig.VERSION_CODE) {
            Button versionButton = primaryButton("查看 App 新版本 " + preferences.getString(PortfolioSync.UPDATE_VERSION_NAME_KEY, ""));
            versionButton.setOnClickListener(view -> openWebUrl(updateUrl));
            body.addView(versionButton, fullWidthTop(16));
            body.addView(paragraph(preferences.getString(PortfolioSync.UPDATE_NOTES_KEY, "发布页提供更新说明与安装包。")), fullWidthTop(8));
        }
    }

    private void syncNow(boolean showFeedback) {
        if (showFeedback) Toast.makeText(this, "正在检查公开更新", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            PortfolioSync.Result result = PortfolioSync.sync(getApplicationContext());
            runOnUiThread(() -> {
                if (result.contentChanged) contentPack = loadContent();
                if ("update".equals(activePage)) setContentView(buildScreen());
                else if (result.contentChanged) renderPage();
                if (showFeedback) Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private String formatSyncTime() {
        long timestamp = preferences.getLong(PortfolioSync.LAST_SYNC_AT_KEY, 0L);
        if (timestamp == 0L) return "联网成功后会自动缓存";
        return "上次检查 " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(timestamp));
    }

    private void openWebUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception error) {
            Toast.makeText(this, "无法打开更新页面", Toast.LENGTH_LONG).show();
        }
    }

    private View capabilityCard(JSONObject capability) {
        LinearLayout card = card();
        TextView title = text(capability.optString("title"), 16, INK, true);
        TextView detail = paragraph(capability.optString("detail"));
        TextView proof = text(capability.optString("proof"), 12, TEAL, true);
        card.addView(title);
        card.addView(detail, fullWidthTop(7));
        card.addView(proof, fullWidthTop(12));
        return card;
    }

    private View journalCard(JSONObject entry, int index) {
        LinearLayout card = card();
        card.setBackground(tacticalShape(index % 3 == 1 ? Color.rgb(246, 238, 197) : Color.WHITE, index % 3 == 0 ? TEAL : index % 3 == 1 ? AMBER : RED, 12));

        LinearLayout meta = new LinearLayout(this);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        TextView number = text(String.format(java.util.Locale.US, "%02d", index + 1), 11, INK, true);
        number.setPadding(dp(8), dp(5), dp(8), dp(5));
        number.setBackground(tacticalShape(index % 3 == 0 ? TEAL : index % 3 == 1 ? AMBER : RED, Color.TRANSPARENT, 5));
        meta.addView(number);
        TextView date = text(entry.optString("date") + " / " + entry.optString("category"), 11, MUTED, true);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(0, -2, 1);
        dateParams.setMargins(dp(9), 0, 0, 0);
        meta.addView(date, dateParams);
        card.addView(meta);

        card.addView(text(entry.optString("title"), 18, INK, true), fullWidthTop(13));
        card.addView(paragraph(entry.optString("observation")), fullWidthTop(8));
        card.addView(journalLine("我的参与", entry.optString("participation")), fullWidthTop(13));
        card.addView(journalLine("学到什么", entry.optString("learning")), fullWidthTop(8));

        LinearLayout signal = new LinearLayout(this);
        signal.setOrientation(LinearLayout.VERTICAL);
        signal.setPadding(dp(11), dp(9), dp(11), dp(9));
        signal.setBackground(tacticalShape(INK, INK, 8));
        signal.addView(text("PRODUCT SIGNAL", 9, TEAL, true));
        signal.addView(text(entry.optString("productSignal"), 12, Color.WHITE, false), fullWidthTop(4));
        card.addView(signal, fullWidthTop(13));
        return card;
    }

    private View journalLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView key = text(label, 10, MUTED, true);
        row.addView(key, new LinearLayout.LayoutParams(dp(62), -2));
        row.addView(text(value, 12, INK, false), new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View productCaseCard(JSONObject item) {
        LinearLayout card = card();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(tacticalShape(AMBER, INK, 16));
        card.addView(text(item.optString("stage"), 10, INK, true));
        card.addView(text(item.optString("title"), 23, INK, true), fullWidthTop(10));
        card.addView(text(item.optString("problem"), 13, Color.rgb(46, 54, 55), false), fullWidthTop(10));
        card.addView(journalLine("目标用户", item.optString("users")), fullWidthTop(16));
        card.addView(journalLine("核心洞察", item.optString("insight")), fullWidthTop(9));
        JSONArray features = item.optJSONArray("features");
        if (features != null) {
            TextView featureTitle = text("MVP FEATURE SET", 9, INK, true);
            card.addView(featureTitle, fullWidthTop(18));
            for (int index = 0; index < features.length(); index++) {
                card.addView(text("0" + (index + 1) + "  " + features.optString(index), 12, INK, true), fullWidthTop(7));
            }
        }
        TextView boundary = text("非生产系统 · 不读取控制指令 · 不输出自动根因", 10, Color.WHITE, true);
        boundary.setPadding(dp(10), dp(8), dp(10), dp(8));
        boundary.setBackground(tacticalShape(INK, INK, 7));
        card.addView(boundary, fullWidthTop(18));
        return card;
    }

    private View workCard(JSONObject item) {
        LinearLayout card = card();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(item.optString("title"), 17, INK, true);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView type = pill(item.optString("type", "学习成果"), AMBER);
        row.addView(type);
        card.addView(row);
        card.addView(paragraph(item.optString("summary")), fullWidthTop(8));
        TextView tags = text(item.optString("tags"), 12, TEAL, true);
        card.addView(tags, fullWidthTop(13));
        return card;
    }

    private View modelCard(JSONObject model) {
        LinearLayout card = card();
        TextView phase = pill(model.optString("phase", "规划中"), TEAL);
        card.addView(phase);
        card.addView(text(model.optString("title"), 18, INK, true), fullWidthTop(10));
        card.addView(paragraph(model.optString("summary")), fullWidthTop(7));
        String id = model.optString("id");
        String storedUri = preferences.getString(MODEL_URI_PREFIX + id, "");
        TextView location = text(storedUri.isEmpty() ? "尚未关联本地模型文件" : "已关联离线模型文件", 12, storedUri.isEmpty() ? MUTED : TEAL, true);
        card.addView(location, fullWidthTop(14));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button attach = secondaryButton(storedUri.isEmpty() ? "关联 GLB / 模型文件" : "更换模型文件");
        attach.setOnClickListener(view -> attachModel(id));
        actions.addView(attach, new LinearLayout.LayoutParams(0, dp(42), 1));
        if (!storedUri.isEmpty()) {
            Button open = primaryButton("打开");
            open.setOnClickListener(view -> openModel(storedUri));
            LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(dp(82), dp(42));
            openParams.setMargins(dp(8), 0, 0, 0);
            actions.addView(open, openParams);
        }
        card.addView(actions, fullWidthTop(14));
        return card;
    }

    private View statusStrip() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(dp(14), dp(12), dp(14), dp(12));
        strip.setBackground(tacticalShape(INK, INK, 10));
        String[] values = {String.valueOf(objects("journal").size()), String.valueOf(objects("works").size()), String.valueOf(objects("productCases").size()), "离线"};
        String[] labels = {"实际日志", "成果", "产品案例", "访问"};
        for (int index = 0; index < labels.length; index++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            TextView value = text(values[index], 19, index == 2 ? AMBER : TEAL, true);
            TextView label = text(labels[index], 10, Color.rgb(174, 184, 181), false);
            item.addView(value);
            item.addView(label);
            strip.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return strip;
    }

    private View metaCard(String title, String value, String detail) {
        LinearLayout card = card();
        card.addView(text(title, 12, MUTED, true));
        card.addView(text(value, 16, INK, true), fullWidthTop(5));
        card.addView(paragraph(detail), fullWidthTop(5));
        return card;
    }

    private void importContent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, IMPORT_CONTENT_REQUEST);
    }

    private void attachModel(String modelId) {
        pendingModelId = modelId;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, ATTACH_MODEL_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { }
        if (requestCode == IMPORT_CONTENT_REQUEST) importFromUri(uri);
        if (requestCode == ATTACH_MODEL_REQUEST && pendingModelId != null) {
            preferences.edit().putString(MODEL_URI_PREFIX + pendingModelId, uri.toString()).apply();
            renderPage();
            Toast.makeText(this, "已关联离线模型文件", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == ATTACH_MEDIA_REQUEST) {
            preferences.edit().putString(EVIDENCE_VIDEO_URI_KEY, uri.toString()).apply();
            renderPage();
            Toast.makeText(this, "已关联原始视频证据", Toast.LENGTH_SHORT).show();
        }
    }

    private void importFromUri(Uri uri) {
        try {
            String raw = readStream(getContentResolver().openInputStream(uri));
            JSONObject incoming = new JSONObject(raw);
            if (!incoming.has("profile") || !incoming.has("works") || !incoming.has("models")) {
                throw new IllegalArgumentException("内容包缺少 profile、works 或 models");
            }
            preferences.edit().putString(CONTENT_KEY, incoming.toString()).apply();
            contentPack = incoming;
            activePage = "profile";
            setContentView(buildScreen());
            Toast.makeText(this, "作品集已更新", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "无法导入：请使用合法的 portfolio.json", Toast.LENGTH_LONG).show();
        }
    }

    private void openModel(String rawUri) {
        try {
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setDataAndType(Uri.parse(rawUri), "application/octet-stream");
            open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(open, "选择模型查看器"));
        } catch (Exception error) {
            Toast.makeText(this, "设备上没有可打开该模型的应用", Toast.LENGTH_LONG).show();
        }
    }

    private void attachEvidenceVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, ATTACH_MEDIA_REQUEST);
    }

    private void openEvidenceVideo(String rawUri) {
        try {
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setDataAndType(Uri.parse(rawUri), "video/*");
            open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(open, "打开原始视频证据"));
        } catch (Exception error) {
            Toast.makeText(this, "设备上没有可打开该视频的应用", Toast.LENGTH_LONG).show();
        }
    }

    private View quickActionPanel() {
        LinearLayout card = card();
        card.setBackground(tacticalShape(Color.rgb(246, 238, 197), INK, 14));
        card.addView(text("QUICK ACTIONS", 10, INK, true));
        card.addView(text("不只是阅读，还可以看曲线、进复盘、关联本地素材。", 15, INK, true), fullWidthTop(8));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button dashboard = primaryButton("打开看板");
        dashboard.setOnClickListener(view -> {
            activePage = "dashboard";
            setContentView(buildScreen());
        });
        row.addView(dashboard, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button evidence = secondaryButton("进入复盘");
        evidence.setOnClickListener(view -> {
            activePage = "evidence";
            setContentView(buildScreen());
        });
        LinearLayout.LayoutParams evidenceParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        evidenceParams.setMargins(dp(8), 0, 0, 0);
        row.addView(evidence, evidenceParams);
        card.addView(row, fullWidthTop(14));
        return card;
    }

    private View categoryMatrix() {
        LinearLayout card = card();
        card.addView(text("LEARNING TOPICS", 10, TEAL, true));
        card.addView(text("学习主题分布", 18, INK, true), fullWidthTop(7));
        String[][] groups = {
                {"仪表/测量", "温度测量|压力测量|测量原理|分析仪表|响应特性"},
                {"巡检/定位", "巡检|设备定位"},
                {"趋势/系统", "DCS学习|异常排查|岗位认知"},
                {"维护/规范", "维护约束|规范学习|液位测量|安全与入班"}
        };
        int total = Math.max(1, objects("journal").size());
        for (String[] group : groups) {
            int count = 0;
            for (JSONObject entry : objects("journal")) {
                if (group[1].contains(entry.optString("category"))) count++;
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.addView(text(group[0] + "  " + count + "/" + total, 12, INK, true));
            row.addView(progressBar(count, total, group[0].contains("仪表") ? TEAL : group[0].contains("巡检") ? AMBER : group[0].contains("趋势") ? RED : INK), fullWidthTop(6));
            card.addView(row, fullWidthTop(12));
        }
        return card;
    }

    private View progressBar(int value, int total, int color) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setBackground(tacticalShape(Color.rgb(232, 237, 235), BORDER, 5));
        View fill = new View(this);
        fill.setBackground(tacticalShape(color, color, 5));
        int width = Math.max(dp(28), (int) (getResources().getDisplayMetrics().widthPixels * 0.72f * value / Math.max(1, total)));
        wrap.addView(fill, new FrameLayout.LayoutParams(width, dp(12)));
        return wrap;
    }

    private View responseChartCard() {
        LinearLayout card = card();
        card.setBackground(tacticalShape(INK, TEAL, 16));
        card.addView(text("RESPONSE CURVE", 10, TEAL, true));
        card.addView(text("等待稳定不是玄学，是一条可记录的曲线", 19, Color.WHITE, true), fullWidthTop(7));
        TextView note = text("95%标气由低向高趋稳；90%标气变化量小，所以体感更快。曲线为脱敏学习表达，不替代正式校验。", 12, Color.rgb(200, 213, 211), false);
        note.setLineSpacing(dp(4), 1f);
        card.addView(note, fullWidthTop(8));
        ResponseCurveView chart = new ResponseCurveView(this);
        card.addView(chart, new LinearLayout.LayoutParams(-1, dp(220)));
        return card;
    }

    private View responseMetricsCard() {
        LinearLayout card = card();
        card.setBackground(tacticalShape(Color.WHITE, RED, 14));
        card.addView(text("KEY RESPONSE FINDINGS", 10, RED, true));
        String[][] metrics = {
                {"95%组", "双指数拟合 R² 0.9995", "快/慢过程并存，需更长观察"},
                {"90%组", "单指数拟合 R² 0.9978", "变化量小，体感更快稳定"},
                {"等待策略", "30-150秒场景化判断", "先验证设备条件，再缩短等待"}
        };
        for (String[] item : metrics) {
            LinearLayout row = new LinearLayout(this);
            row.setPadding(0, dp(10), 0, dp(10));
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView key = pill(item[0], item[0].contains("90") ? AMBER : item[0].contains("策略") ? TEAL : RED);
            row.addView(key, new LinearLayout.LayoutParams(dp(78), -2));
            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(text(item[1], 14, INK, true));
            copy.addView(text(item[2], 12, MUTED, false), fullWidthTop(3));
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
            copyParams.setMargins(dp(10), 0, 0, 0);
            row.addView(copy, copyParams);
            card.addView(row);
        }
        return card;
    }

    private View videoEvidenceCard() {
        LinearLayout card = card();
        card.setBackground(tacticalShape(Color.rgb(246, 238, 197), INK, 16));
        card.addView(text("LOCAL MEDIA EVIDENCE", 10, INK, true));
        card.addView(text("原始视频只在本机保留", 20, INK, true), fullWidthTop(8));
        card.addView(paragraph("通过系统文件选择器关联录像。App只保存本机文件URI，不上传视频、不写入公开内容包。"), fullWidthTop(8));
        String uri = preferences.getString(EVIDENCE_VIDEO_URI_KEY, "");
        TextView status = text(uri.isEmpty() ? "尚未关联原始视频" : "已关联原始视频证据", 12, uri.isEmpty() ? MUTED : TEAL, true);
        card.addView(status, fullWidthTop(14));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button attach = primaryButton(uri.isEmpty() ? "关联原始视频" : "更换视频");
        attach.setOnClickListener(view -> attachEvidenceVideo());
        row.addView(attach, new LinearLayout.LayoutParams(0, dp(42), 1));
        if (!uri.isEmpty()) {
            Button open = secondaryButton("打开");
            open.setOnClickListener(view -> openEvidenceVideo(uri));
            LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(dp(82), dp(42));
            openParams.setMargins(dp(8), 0, 0, 0);
            row.addView(open, openParams);
        }
        card.addView(row, fullWidthTop(14));
        return card;
    }

    private View responseDecisionCard() {
        LinearLayout card = card();
        card.addView(text("STOP-WAIT DECISION", 10, TEAL, true));
        card.addView(text("停止等待必须同时看两个条件", 18, INK, true), fullWidthTop(7));
        String[][] rules = {
                {"A", "预测值连续收敛", "连续拟合的稳定值变化足够小"},
                {"B", "变化速率足够小", "最近读数变化已接近平衡"},
                {"边界", "仍需现场复核", "模型只辅助判断，不替代规程"}
        };
        for (String[] rule : rules) {
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            TextView badge = text(rule[0], 16, Color.WHITE, true);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(tacticalShape(rule[0].equals("边界") ? RED : TEAL, Color.TRANSPARENT, 8));
            item.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));
            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(text(rule[1], 15, INK, true));
            copy.addView(text(rule[2], 12, MUTED, false), fullWidthTop(3));
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
            copyParams.setMargins(dp(12), 0, 0, 0);
            item.addView(copy, copyParams);
            card.addView(item, fullWidthTop(12));
        }
        return card;
    }

    private View evidenceFlowCard() {
        LinearLayout card = card();
        card.setBackground(tacticalShape(Color.WHITE, AMBER, 16));
        card.addView(text("EVIDENCE FLOW", 10, AMBER, true));
        card.addView(text("从录像到公开复盘的五步链路", 18, INK, true), fullWidthTop(7));
        String[] steps = {"现场发现等待偏长", "录像保留读数变化", "整理时间-读数数据", "指数拟合与残差检查", "只发布脱敏结论"};
        for (int index = 0; index < steps.length; index++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView number = text(String.format(java.util.Locale.US, "%02d", index + 1), 12, INK, true);
            number.setGravity(Gravity.CENTER);
            number.setBackground(tacticalShape(index == steps.length - 1 ? TEAL : AMBER, Color.TRANSPARENT, 5));
            row.addView(number, new LinearLayout.LayoutParams(dp(42), dp(34)));
            TextView label = text(steps[index], 13, INK, true);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
            labelParams.setMargins(dp(10), 0, 0, 0);
            row.addView(label, labelParams);
            card.addView(row, fullWidthTop(9));
        }
        return card;
    }

    private View methodRail() {
        LinearLayout card = card();
        card.addView(text("METHOD RAIL", 10, TEAL, true));
        String[] methods = {"观察现场", "追踪链路", "校验证据", "转成产品"};
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.HORIZONTAL);
        for (int index = 0; index < methods.length; index++) {
            TextView item = text(methods[index], 12, index == 1 ? Color.WHITE : INK, true);
            item.setGravity(Gravity.CENTER);
            item.setBackground(tacticalShape(index == 1 ? INK : index == 2 ? AMBER : TEAL, Color.TRANSPARENT, 8));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1);
            if (index > 0) params.setMargins(dp(6), 0, 0, 0);
            rail.addView(item, params);
        }
        card.addView(rail, fullWidthTop(12));
        return card;
    }

    private void copyTemplate() {
        String template = "{\n  \"meta\": {\"version\": \"2026.07\", \"updatedAt\": \"2026-07-22\"},\n  \"profile\": {\"name\": \"你的名字\", \"role\": \"你的方向\", \"headline\": \"一句定位\", \"summary\": \"公开安全的简介\"},\n  \"capabilities\": [{\"title\": \"能力\", \"detail\": \"说明\", \"proof\": \"证据\"}],\n  \"works\": [{\"title\": \"作品\", \"type\": \"案例\", \"summary\": \"说明\", \"tags\": \"标签\"}],\n  \"models\": [{\"id\": \"process-01\", \"title\": \"流程模型\", \"phase\": \"规划中\", \"summary\": \"模型说明\"}]\n}";
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("portfolio.json", template));
        Toast.makeText(this, "模板已复制，可保存为 portfolio.json 后导入", Toast.LENGTH_LONG).show();
    }

    private JSONObject loadContent() {
        try {
            String override = preferences.getString(CONTENT_KEY, "");
            if (!override.isEmpty()) return new JSONObject(override);
            AssetManager assets = getAssets();
            return new JSONObject(readStream(assets.open("portfolio.json")));
        } catch (Exception error) {
            return new JSONObject();
        }
    }

    private List<JSONObject> objects(String key) {
        List<JSONObject> result = new ArrayList<>();
        JSONArray array = contentPack.optJSONArray(key);
        if (array == null) return result;
        for (int index = 0; index < array.length(); index++) {
            JSONObject object = array.optJSONObject(index);
            if (object != null) result.add(object);
        }
        return result;
    }

    private String readStream(InputStream stream) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) bytes.write(buffer, 0, read);
        stream.close();
        return bytes.toString("UTF-8");
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(15), dp(15), dp(15));
        card.setBackground(tacticalShape(Color.WHITE, BORDER, 10));
        return card;
    }

    private TextView eyebrow(String value) {
        return text(value, 11, TEAL, true);
    }

    private TextView heading(String value) {
        TextView view = text(value, 25, INK, true);
        view.setLineSpacing(dp(2), 1f);
        return view;
    }

    private TextView paragraph(String value) {
        TextView view = text(value, 14, MUTED, false);
        view.setLineSpacing(dp(5), 1f);
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(null, bold ? 1 : 0);
        return view;
    }

    private TextView pill(String value, int color) {
        TextView pill = text(value, 11, color, true);
        pill.setPadding(dp(8), dp(4), dp(8), dp(4));
        pill.setBackground(tacticalShape(Color.rgb(244, 238, 207), color, 6));
        return pill;
    }

    private Button primaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(INK);
        button.setTypeface(null, 1);
        button.setBackground(tacticalShape(AMBER, INK, 9));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(Color.WHITE);
        button.setBackground(tacticalShape(INK_SOFT, BORDER, 9));
        return button;
    }

    private Drawable tacticalShape(int fill, int stroke, int cut) {
        return new TacticalDrawable(fill, stroke, dp(cut));
    }

    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setStroke(dp(1), stroke);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidthTop(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(margin);
        return params;
    }

    private void addGap(int value) {
        View gap = new View(this);
        body.addView(gap, new LinearLayout.LayoutParams(1, dp(value)));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ResponseCurveView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] highTimes = {0, 10, 20, 30, 50, 80, 115, 150, 194, 244};
        private final float[] highValues = {93.20f, 93.63f, 93.91f, 94.06f, 94.20f, 94.30f, 94.38f, 94.45f, 94.51f, 94.55f};
        private final float[] lowTimes = {0, 10, 20, 30, 45, 62, 82, 117, 160, 282};
        private final float[] lowValues = {90.42f, 90.32f, 90.24f, 90.19f, 90.13f, 90.09f, 90.06f, 90.03f, 90.01f, 89.99f};

        ResponseCurveView(Context context) {
            super(context);
            setMinimumHeight(220);
            textPaint.setTextSize(24f);
            textPaint.setFakeBoldText(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float left = 34f;
            float top = 24f;
            float right = width - 18f;
            float bottom = height - 38f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(30, 40, 48));
            canvas.drawRect(0, 0, width, height, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.rgb(79, 94, 101));
            for (int i = 0; i <= 4; i++) {
                float y = top + (bottom - top) * i / 4f;
                canvas.drawLine(left, y, right, y, paint);
            }
            canvas.drawLine(left, bottom, right, bottom, paint);
            canvas.drawLine(left, top, left, bottom, paint);

            drawCurve(canvas, highTimes, highValues, left, top, right, bottom, 93.1f, 94.7f, Color.rgb(59, 193, 183));
            drawCurve(canvas, lowTimes, lowValues, left, top, right, bottom, 89.9f, 90.5f, Color.rgb(244, 200, 74));

            textPaint.setColor(Color.rgb(59, 193, 183));
            canvas.drawText("95% response", left + 6f, top + 24f, textPaint);
            textPaint.setColor(Color.rgb(244, 200, 74));
            canvas.drawText("90% response", left + 6f, top + 54f, textPaint);
            textPaint.setColor(Color.rgb(190, 203, 201));
            textPaint.setTextSize(20f);
            canvas.drawText("time -> stable reading", left + 6f, height - 12f, textPaint);
            textPaint.setTextSize(24f);
        }

        private void drawCurve(Canvas canvas, float[] times, float[] values, float left, float top, float right, float bottom, float minValue, float maxValue, int color) {
            Path path = new Path();
            for (int i = 0; i < times.length; i++) {
                float x = left + (right - left) * times[i] / 300f;
                float normalized = (values[i] - minValue) / Math.max(0.01f, maxValue - minValue);
                float y = bottom - (bottom - top) * normalized;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(color);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < times.length; i++) {
                float x = left + (right - left) * times[i] / 300f;
                float normalized = (values[i] - minValue) / Math.max(0.01f, maxValue - minValue);
                float y = bottom - (bottom - top) * normalized;
                canvas.drawCircle(x, y, 5f, paint);
            }
        }
    }

    private static class TacticalDrawable extends Drawable {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float cut;

        TacticalDrawable(int fill, int stroke, float cut) {
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(fill);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(1.5f);
            strokePaint.setColor(stroke);
            this.cut = cut;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float left = bounds.left;
            float top = bounds.top;
            float right = bounds.right;
            float bottom = bounds.bottom;
            Path path = new Path();
            path.moveTo(left + cut, top);
            path.lineTo(right, top);
            path.lineTo(right, bottom - cut);
            path.lineTo(right - cut, bottom);
            path.lineTo(left, bottom);
            path.lineTo(left, top + cut);
            path.close();
            canvas.drawPath(path, fillPaint);
            canvas.drawPath(path, strokePaint);
        }

        @Override public void setAlpha(int alpha) { fillPaint.setAlpha(alpha); strokePaint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { fillPaint.setColorFilter(filter); strokePaint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
