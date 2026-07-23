package com.signaltrace.portfolio;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int IMPORT_CONTENT_REQUEST = 4001;
    private static final int ATTACH_MEDIA_REQUEST = 4002;
    private static final String VIDEO_URI_KEY = "hydrogen_response_video";
    private static final String DISPLAY_NAME_KEY = "display_name";
    private static final String DIAGNOSTIC_PREFS = "signaltrace_diagnostics";
    private static final String CRASH_PENDING_KEY = "crash_pending";
    private static final String CRASH_TRACE_KEY = "crash_trace";

    private ContentStore contentStore;
    private UpdateManager updateManager;
    private SwipePageHost pageHost;
    private View appHeader;
    private GeometricNavBar navigation;
    private TextView headerStatus;
    private int activeDestination;
    private int internshipMode;
    private boolean response95 = true;
    private UpdateManager.Manifest remoteManifest;
    private int updateProgress;
    private String updateMessage = "等待检查";
    private boolean updateBusy;
    private boolean immersive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installCrashRecorder();
        String previousCrash = getSharedPreferences(DIAGNOSTIC_PREFS, MODE_PRIVATE)
            .getString(CRASH_TRACE_KEY, "");
        boolean recoveryRequired = getSharedPreferences(DIAGNOSTIC_PREFS, MODE_PRIVATE)
            .getBoolean(CRASH_PENDING_KEY, false);
        if (recoveryRequired) {
            showRecoveryScreen("检测到上次启动异常", previousCrash);
            return;
        }
        try {
            startFullExperience();
        } catch (Throwable error) {
            String trace = recordCrash(error);
            showRecoveryScreen("完整界面启动失败", trace);
        }
    }

    private void startFullExperience() {
        configureWindow();
        contentStore = new ContentStore(this);
        updateManager = new UpdateManager(this, contentStore);
        View shell = buildShell();
        setContentView(shell);
        showDestination(0, false);
        shell.postDelayed(this::markStartupHealthy, 1800);
        try {
            PortfolioSyncJobService.schedule(this);
        } catch (RuntimeException error) {
            android.util.Log.w("SignalTrace", "Background sync scheduling unavailable", error);
        }
        checkForUpdates(false);
    }

    private void installCrashRecorder() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            recordCrash(error);
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    @android.annotation.SuppressLint("ApplySharedPref")
    private String recordCrash(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        String trace = writer.toString();
        getSharedPreferences(DIAGNOSTIC_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(CRASH_PENDING_KEY, true)
            .putString(CRASH_TRACE_KEY, trace)
            .commit();
        android.util.Log.e("SignalTrace", "Runtime failure", error);
        return trace;
    }

    private void markStartupHealthy() {
        getSharedPreferences(DIAGNOSTIC_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(CRASH_PENDING_KEY, false)
            .remove(CRASH_TRACE_KEY)
            .apply();
    }

    private void showRecoveryScreen(String reason, String trace) {
        getWindow().setStatusBarColor(Color.rgb(13, 17, 20));
        getWindow().setNavigationBarColor(Color.rgb(13, 17, 20));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(rawDp(22), rawDp(28), rawDp(22), rawDp(28));
        body.setBackgroundColor(Color.rgb(238, 239, 233));

        TextView label = recoveryText("安全启动", 12, Color.rgb(238, 82, 68), true);
        body.addView(label);
        TextView title = recoveryText("App 已进入恢复模式", 27, Color.rgb(13, 17, 20), true);
        body.addView(title, recoveryTop(10));
        TextView detail = recoveryText(
            reason + "。你的本地视频和个性称呼仍会保留；下面可以先重试，或只清理可重新下载的内容缓存。",
            14,
            Color.rgb(38, 45, 49),
            false
        );
        body.addView(detail, recoveryTop(10));

        TextView version = recoveryText(
            "APP " + BuildConfig.VERSION_NAME + " / Android " + android.os.Build.VERSION.RELEASE
                + " / API " + android.os.Build.VERSION.SDK_INT,
            11,
            Color.rgb(91, 103, 103),
            true
        );
        body.addView(version, recoveryTop(18));

        TextView retry = recoveryAction("重试完整体验", Color.rgb(246, 184, 46), Color.rgb(13, 17, 20));
        retry.setOnClickListener(view -> {
            clearCrashMarker();
            recreate();
        });
        body.addView(retry, recoveryTop(18));

        TextView clearCache = recoveryAction("清理成长包缓存并重试", Color.rgb(23, 29, 33), Color.WHITE);
        clearCache.setOnClickListener(view -> {
            clearDownloadableContent();
            clearCrashMarker();
            recreate();
        });
        body.addView(clearCache, recoveryTop(9));

        TextView copy = recoveryAction("复制诊断信息", Color.rgb(50, 205, 190), Color.rgb(13, 17, 20));
        copy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("SignalTrace crash", trace));
            Toast.makeText(this, "诊断信息已复制", Toast.LENGTH_SHORT).show();
        });
        body.addView(copy, recoveryTop(9));

        TextView diagnostic = recoveryText(
            trace == null || trace.trim().isEmpty()
                ? "尚未记录到Java异常。若仍无法进入，请截取系统提示并连接USB调试。"
                : abbreviateTrace(trace),
            10,
            Color.rgb(218, 220, 214),
            false
        );
        diagnostic.setPadding(rawDp(14), rawDp(14), rawDp(14), rawDp(14));
        diagnostic.setTextIsSelectable(true);
        diagnostic.setBackgroundColor(Color.rgb(23, 29, 33));
        body.addView(diagnostic, recoveryTop(18));
        scroll.addView(body);
        setContentView(scroll);
    }

    private TextView recoveryText(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(android.graphics.Typeface.create(
            bold ? "sans-serif-condensed" : "sans-serif",
            bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL
        ));
        view.setLineSpacing(0f, 1.12f);
        return view;
    }

    private TextView recoveryAction(String value, int fill, int textColor) {
        TextView view = recoveryText(value, 13, textColor, true);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(rawDp(48));
        view.setPadding(rawDp(14), rawDp(10), rawDp(14), rawDp(10));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(rawDp(3));
        view.setBackground(background);
        view.setClickable(true);
        return view;
    }

    private LinearLayout.LayoutParams recoveryTop(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, rawDp(top), 0, 0);
        return params;
    }

    private int rawDp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String abbreviateTrace(String trace) {
        int limit = Math.min(trace.length(), 5000);
        return trace.substring(0, limit);
    }

    @android.annotation.SuppressLint("ApplySharedPref")
    private void clearCrashMarker() {
        getSharedPreferences(DIAGNOSTIC_PREFS, MODE_PRIVATE).edit().clear().commit();
    }

    private void clearDownloadableContent() {
        File directory = new File(getFilesDir(), "content");
        File active = new File(directory, "active.json");
        File staging = new File(directory, "staging.json");
        if (active.exists() && !active.delete()) {
            android.util.Log.w("SignalTrace", "Could not delete active content cache");
        }
        if (staging.exists() && !staging.delete()) {
            android.util.Log.w("SignalTrace", "Could not delete staging content cache");
        }
        getSharedPreferences("portfolio_showcase", MODE_PRIVATE)
            .edit()
            .remove("portfolio_content")
            .apply();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(AppTheme.INK);
        window.setNavigationBarColor(AppTheme.INK);
    }

    private View buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(AppTheme.INK);
        appHeader = buildHeader();
        shell.addView(appHeader, new LinearLayout.LayoutParams(-1, dp(64)));

        pageHost = new SwipePageHost(this);
        pageHost.setBackgroundColor(AppTheme.PAPER);
        pageHost.setListener(direction -> {
            int next = Math.max(0, Math.min(4, activeDestination + direction));
            if (next != activeDestination) {
                navigation.select(next, true);
                showDestination(next, true);
            }
        });
        shell.addView(pageHost, new LinearLayout.LayoutParams(-1, 0, 1f));

        navigation = new GeometricNavBar(this);
        navigation.setListener(index -> showDestination(index, true));
        shell.addView(navigation, new LinearLayout.LayoutParams(-1, dp(86)));
        return shell;
    }

    private View buildHeader() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));
        bar.setBackgroundColor(AppTheme.INK);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.app_icon_art);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setBackground(AppTheme.panel(AppTheme.INK_2, AppTheme.LINE, 8, this));
        icon.setPadding(dp(3), dp(3), dp(3), dp(3));
        icon.setOnClickListener(view -> view.animate()
            .rotationYBy(360)
            .setDuration(720)
            .setInterpolator(AppTheme.EASE_IN_OUT)
            .start());
        bar.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        TextView title = AppTheme.text(this, displayName(), 14, AppTheme.WHITE, true);
        TextView subtitle = AppTheme.text(this, "我的电厂实习 · 信号正在生长", 10, AppTheme.MUTED, false);
        copy.addView(title);
        copy.addView(subtitle, topParams(2));
        bar.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        headerStatus = AppTheme.label(this, "在线 · " + BuildConfig.VERSION_NAME, AppTheme.TEAL);
        headerStatus.setGravity(Gravity.CENTER);
        headerStatus.setPadding(dp(9), dp(6), dp(9), dp(6));
        headerStatus.setBackground(AppTheme.panel(AppTheme.INK_2, AppTheme.TEAL, 6, this));
        AppTheme.pressable(headerStatus);
        headerStatus.setOnClickListener(view -> {
            navigation.select(4, true);
            showDestination(4, true);
        });
        bar.addView(headerStatus);
        return bar;
    }

    private void showDestination(int destination, boolean animate) {
        activeDestination = Math.max(0, Math.min(4, destination));
        View next = buildDestination(activeDestination);
        if (!animate || pageHost.getChildCount() == 0) {
            pageHost.removeAllViews();
            pageHost.addView(next, new FrameLayout.LayoutParams(-1, -1));
            navigation.select(activeDestination, false);
            return;
        }

        View previous = pageHost.getChildAt(pageHost.getChildCount() - 1);
        next.setAlpha(0f);
        next.setTranslationX(dp(24));
        next.setScaleX(0.985f);
        next.setScaleY(0.985f);
        pageHost.addView(next, new FrameLayout.LayoutParams(-1, -1));
        next.animate()
            .alpha(1f)
            .translationX(0)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(420)
            .setInterpolator(AppTheme.EASE_OUT)
            .start();
        previous.animate()
            .alpha(0f)
            .translationX(-dp(18))
            .setDuration(240)
            .setInterpolator(AppTheme.EASE_IN_OUT)
            .withEndAction(() -> pageHost.removeView(previous))
            .start();
    }

    private View buildDestination(int destination) {
        if (destination == 1) return buildInternshipPage();
        if (destination == 2) return buildLabPage();
        if (destination == 3) return buildTrajectoryPage();
        if (destination == 4) return buildUpdatePage();
        return buildIdentityPage();
    }

    private View buildIdentityPage() {
        LinearLayout body = pageBody(false);
        SignalSceneView scene = new SignalSceneView(this);
        body.addView(scene, new LinearLayout.LayoutParams(-1, dp(236)));

        LinearLayout identity = panel(AppTheme.PAPER, AppTheme.INK);
        addReveal(identity, AppTheme.label(this, "我的现场 · 2026", AppTheme.RED), 0);
        addReveal(identity, AppTheme.text(this, displayName(), 29, AppTheme.INK, true), 1, 8);
        addReveal(identity, AppTheme.text(this, "仪控班实习 · 一边观察，一边建立自己的判断", 13, AppTheme.INK_3, true), 2, 8);

        LinearLayout idFooter = new LinearLayout(this);
        idFooter.setGravity(Gravity.CENTER_VERTICAL);
        FloatingActionView edit = new FloatingActionView(this, "✦", "换个称呼", AppTheme.AMBER, AppTheme.INK, 0f);
        edit.setOnClickListener(view -> showDisplayNameDialog());
        idFooter.addView(edit, new LinearLayout.LayoutParams(0, -2, 1f));
        FloatingActionView focus = new FloatingActionView(this, "⛶", "沉浸", AppTheme.INK_2, AppTheme.WHITE, 1.7f);
        focus.setOnClickListener(view -> toggleImmersive());
        LinearLayout.LayoutParams focusParams = new LinearLayout.LayoutParams(0, -2, 1f);
        focusParams.setMargins(dp(9), 0, 0, 0);
        idFooter.addView(focus, focusParams);
        addReveal(identity, idFooter, 3, 18);
        body.addView(identity, fullTop(0));

        LinearLayout stats = new LinearLayout(this);
        stats.setWeightSum(3);
        stats.addView(metric(String.valueOf(contentStore.objects("journal").size()), "现场记录", AppTheme.TEAL), weighted(1, 0));
        stats.addView(metric(String.valueOf(contentStore.objects("works").size()), "作品模块", AppTheme.AMBER), weighted(1, 8));
        stats.addView(metric("01", "产品案例", AppTheme.RED), weighted(1, 8));
        body.addView(stats, fullTop(14));

        addSection(body, "此刻", "最近一次现场观察");
        List<JSONObject> journal = contentStore.objects("journal");
        if (!journal.isEmpty()) {
            JSONObject latest = journal.get(journal.size() - 1);
            LinearLayout now = panel(AppTheme.INK_2, AppTheme.LINE);
            now.addView(AppTheme.label(this,
                latest.optString("date", "") + " / " + latest.optString("category", ""),
                AppTheme.TEAL));
            now.addView(AppTheme.text(this, latest.optString("title", ""), 19, AppTheme.WHITE, true), topParams(8));
            now.addView(AppTheme.text(this, latest.optString("learning", ""), 12, AppTheme.PAPER_2, false), topParams(10));
            FloatingActionView enter = new FloatingActionView(this, "→", "展开现场", AppTheme.TEAL, AppTheme.INK, 0.8f);
            enter.setOnClickListener(view -> {
                navigation.select(1, true);
                showDestination(1, true);
            });
            now.addView(enter, topParams(16));
            body.addView(now, fullTop(10));
        }

        addSection(body, "向里走", "现场、数据与证据轨迹");
        body.addView(moduleLink("01", "现场", "日志、作品与能力从内部展开", AppTheme.AMBER, 1), fullTop(10));
        body.addView(moduleLink("02", "三维数据", "同时旋转两组响应曲线并点选读数", AppTheme.TEAL, 2), fullTop(9));
        body.addView(moduleLink("03", "证据轨迹", "把16条现场事件放进空间关系中", AppTheme.RED, 3), fullTop(9));
        body.setPadding(0, 0, 0, dp(20));
        return wrap(body);
    }

    private View buildInternshipPage() {
        LinearLayout body = pageBody(true);
        body.addView(kicker("我的现场", "从设备、信号和一次次判断向里展开"));
        body.addView(segment(
            new String[]{"总览", "现场日志", "作品", "能力图谱"},
            internshipMode,
            index -> {
                internshipMode = index;
                showDestination(1, true);
            }
        ), fullTop(16));

        if (internshipMode == 1) buildJournalSubPage(body);
        else if (internshipMode == 2) buildWorksSubPage(body);
        else if (internshipMode == 3) buildCapabilitySubPage(body);
        else buildInternshipOverview(body);
        return wrap(body);
    }

    private void buildInternshipOverview(LinearLayout body) {
        List<JSONObject> journal = contentStore.objects("journal");
        LinearLayout phase = panel(AppTheme.INK, AppTheme.INK);
        phase.addView(AppTheme.label(this, "这一阶段", AppTheme.AMBER));
        phase.addView(AppTheme.text(this, "从看见设备，到理解信号", 23, AppTheme.WHITE, true), topParams(8));
        phase.addView(AppTheme.text(this,
            journal.size() + " 次观察，正在变成自己的经验地图。",
            12, AppTheme.PAPER_2, false), topParams(10));
        phase.addView(progressRail(Math.min(1f, journal.size() / 24f), "当前阶段", journal.size() + " / 24"), topParams(18));
        body.addView(phase, fullTop(16));

        addSection(body, "现场分层", "从记录、作品走到能力证据");
        body.addView(subspace("A", "现场日志", journal.size() + " 条事实、参与、学习与产品信号", AppTheme.TEAL, 1), fullTop(10));
        body.addView(subspace("B", "作品与项目", contentStore.objects("works").size() + " 项成果 + FieldTrace 产品案例", AppTheme.AMBER, 2), fullTop(9));
        body.addView(subspace("C", "能力图谱", contentStore.objects("capabilities").size() + " 条能力证据 + 流程模型入口", AppTheme.RED, 3), fullTop(9));

        addSection(body, "判断方式", "事实、推断、待确认");
        body.addView(trustGrid(), fullTop(10));
    }

    private void buildJournalSubPage(LinearLayout body) {
        List<JSONObject> journal = contentStore.objects("journal");
        addSection(body, journal.size() + " 条现场记录", "当时的观察、参与和判断");
        for (int index = journal.size() - 1; index >= 0; index--) {
            JSONObject item = journal.get(index);
            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(detailLine("现场观察", item.optString("observation", ""), AppTheme.TEAL));
            detail.addView(detailLine("本人参与", item.optString("participation", ""), AppTheme.AMBER), topParams(10));
            detail.addView(detailLine("学习结论", item.optString("learning", ""), AppTheme.WHITE), topParams(10));
            detail.addView(detailLine("产品信号", item.optString("productSignal", ""), AppTheme.RED), topParams(10));
            body.addView(expandable(
                item.optString("date", "--.--"),
                item.optString("title", "现场记录"),
                item.optString("category", "未分类"),
                detail
            ), fullTop(index == journal.size() - 1 ? 10 : 9));
        }
    }

    private void buildWorksSubPage(LinearLayout body) {
        addSection(body, "我的作品", "从现场观察长出来的结果");
        int index = 1;
        for (JSONObject work : contentStore.objects("works")) {
            LinearLayout card = panel(index % 2 == 0 ? AppTheme.INK_2 : AppTheme.INK, AppTheme.LINE);
            card.addView(AppTheme.label(this, String.format(Locale.US, "%02d / %s", index, work.optString("type", "成果")), index % 2 == 0 ? AppTheme.TEAL : AppTheme.AMBER));
            card.addView(AppTheme.text(this, work.optString("title", ""), 21, AppTheme.WHITE, true), topParams(8));
            card.addView(AppTheme.text(this, work.optString("summary", ""), 12, AppTheme.PAPER_2, false), topParams(10));
            card.addView(AppTheme.text(this, work.optString("tags", ""), 10, AppTheme.MUTED, true), topParams(14));
            body.addView(card, fullTop(index == 1 ? 10 : 9));
            AppTheme.reveal(card, index);
            index++;
        }

        addSection(body, "FieldTrace", "真实痛点如何变成产品判断");
        for (JSONObject product : contentStore.objects("productCases")) {
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.addView(detailLine("用户", product.optString("users", ""), AppTheme.TEAL));
            details.addView(detailLine("洞察", product.optString("insight", ""), AppTheme.AMBER), topParams(10));
            details.addView(detailLine("方案", product.optString("solution", ""), AppTheme.WHITE), topParams(10));
            details.addView(detailLine("来源", product.optString("evidence", ""), AppTheme.RED), topParams(10));
            body.addView(expandable(
                product.optString("stage", "概念验证"),
                product.optString("title", "FieldTrace"),
                product.optString("problem", ""),
                details
            ), fullTop(10));
        }
    }

    private void buildCapabilitySubPage(LinearLayout body) {
        addSection(body, "能力从哪里来", "每一项都能回到具体证据");
        int index = 0;
        for (JSONObject capability : contentStore.objects("capabilities")) {
            LinearLayout card = panel(AppTheme.PAPER, index == 1 ? AppTheme.TEAL : AppTheme.INK);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.TOP);
            TextView number = AppTheme.text(this, "0" + (index + 1), 23, index == 1 ? AppTheme.TEAL : AppTheme.RED, true);
            row.addView(number, new LinearLayout.LayoutParams(dp(42), -2));
            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(AppTheme.text(this, capability.optString("title", ""), 19, AppTheme.INK, true));
            copy.addView(AppTheme.text(this, capability.optString("detail", ""), 12, AppTheme.INK_3, false), topParams(7));
            copy.addView(AppTheme.label(this, "证据 · " + capability.optString("proof", ""), AppTheme.RED), topParams(12));
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
            card.addView(row);
            body.addView(card, fullTop(index == 0 ? 10 : 9));
            index++;
        }

        addSection(body, "流程模型", "继续补充的空间入口");
        for (JSONObject model : contentStore.objects("models")) {
            body.addView(keyValuePanel(
                model.optString("phase", "规划中"),
                model.optString("title", ""),
                model.optString("summary", "")
            ), fullTop(9));
        }
    }

    private View buildLabPage() {
        LinearLayout body = pageBody(true);
        body.addView(kicker("三维响应数据", "两组采样在同一个空间里"));

        TextView pointTitle = AppTheme.label(this, "95% 标气 · 起点", AppTheme.AMBER);
        TextView pointValue = AppTheme.text(this, "93.200% H₂", 25, AppTheme.WHITE, true);
        TextView pointRate = AppTheme.text(this, "T + 0s · 等待变化", 11, AppTheme.MUTED, true);
        TextView cursorValue = AppTheme.text(this, response95 ? "95% 数据切片" : "90% 数据切片", 12, AppTheme.WHITE, true);
        ResponseCurveView curve = new ResponseCurveView(this);
        curve.setDataset(response95);
        curve.setCursorListener((seconds, value) -> cursorValue.setText(
            String.format(Locale.US, "T + %.0fs · %.3f%% H₂", seconds, value)
        ));

        SpatialDataView space = new SpatialDataView(this);
        space.setListener(point -> {
            response95 = point.dataset == 0;
            curve.setDataset(response95);
            cursorValue.setText(response95 ? "95% 数据切片" : "90% 数据切片");
            pointTitle.setText(response95 ? "95% 标气 · 上升" : "90% 标气 · 下降");
            pointTitle.setTextColor(response95 ? AppTheme.AMBER : AppTheme.TEAL);
            pointValue.setText(String.format(Locale.US, "%.3f%% H₂", point.value));
            pointRate.setText(String.format(
                Locale.US,
                "T + %.0fs · 变化率 %+.4f%%/s",
                point.seconds,
                point.rate
            ));
        });
        body.addView(space, new LinearLayout.LayoutParams(-1, dp(330)));

        LinearLayout focusRow = new LinearLayout(this);
        FloatingActionView focus95 = new FloatingActionView(this, "↗", "95% 上升", AppTheme.AMBER, AppTheme.INK, 0f);
        FloatingActionView focus90 = new FloatingActionView(this, "↘", "90% 下降", AppTheme.TEAL, AppTheme.INK, 1.8f);
        focus95.setOnClickListener(view -> {
            response95 = true;
            curve.setDataset(true);
            space.focusDataset(0);
        });
        focus90.setOnClickListener(view -> {
            response95 = false;
            curve.setDataset(false);
            space.focusDataset(1);
        });
        focusRow.addView(focus95, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams focus90Params = new LinearLayout.LayoutParams(0, -2, 1f);
        focus90Params.setMargins(dp(9), 0, 0, 0);
        focusRow.addView(focus90, focus90Params);
        body.addView(focusRow, fullTop(10));

        LinearLayout selectedPoint = panel(AppTheme.INK_2, AppTheme.LINE);
        selectedPoint.addView(pointTitle);
        selectedPoint.addView(pointValue, topParams(6));
        selectedPoint.addView(pointRate, topParams(5));
        body.addView(selectedPoint, fullTop(12));
        space.focusDataset(response95 ? 0 : 1);

        addSection(body, "二维切片", "当前数据的平面投影");
        LinearLayout chartPanel = panel(AppTheme.INK_2, AppTheme.LINE);
        chartPanel.addView(curve, new LinearLayout.LayoutParams(-1, dp(220)));
        cursorValue.setGravity(Gravity.END);
        chartPanel.addView(cursorValue, topParams(8));
        body.addView(chartPanel, fullTop(10));

        body.addView(videoEvidencePanel(), fullTop(14));

        addSection(body, "什么时候停止等待", "两个条件同时成立");
        LinearLayout decision = panel(AppTheme.PAPER, AppTheme.INK);
        decision.addView(numberedRule("A", "预测值收敛", "连续 3 次拟合的 Pmax 变化 < 0.02%", AppTheme.AMBER));
        decision.addView(numberedRule("B", "变化速率足够小", "最近两次读数变化率 < 0.002%/s", AppTheme.TEAL), topParams(12));
        decision.addView(AppTheme.text(this, "A + B → 取最后一次预测稳定值", 12, AppTheme.INK, true), topParams(16));
        body.addView(decision, fullTop(10));

        addSection(body, "这次实测", "T90 约 73–90 秒");
        body.addView(keyValuePanel(
            "需要继续确认",
            "比标称响应慢 3–4 倍",
            "相比厂家标称 ≤23 秒偏慢 3–4 倍，建议复核样气流量 400–600 ml/min、管路死体积、泄漏与传感器老化。"
        ), fullTop(10));
        return wrap(body);
    }

    private View buildTrajectoryPage() {
        LinearLayout body = pageBody(true);
        List<JSONObject> journal = contentStore.objects("journal");
        java.util.Set<String> categories = new java.util.HashSet<>();
        for (JSONObject item : journal) categories.add(item.optString("category", "未分类"));
        body.addView(kicker("我的轨迹", journal.size() + "次现场观察，在空间里彼此相连"));

        TextView eventMeta = AppTheme.label(this, "最新节点", AppTheme.TEAL);
        TextView eventTitle = AppTheme.text(this, "等待选择", 21, AppTheme.WHITE, true);
        TextView eventDetail = AppTheme.text(this, "", 12, AppTheme.PAPER_2, false);

        EvidenceSpaceView space = new EvidenceSpaceView(this);
        space.setListener((index, item) -> {
            eventMeta.setText(String.format(
                Locale.CHINA,
                "%s · %s",
                item.optString("date", ""),
                item.optString("category", "")
            ));
            eventTitle.setText(item.optString("title", ""));
            eventDetail.setText(item.optString("learning", ""));
        });
        space.setItems(journal);
        body.addView(space, new LinearLayout.LayoutParams(-1, dp(340)));

        LinearLayout selectedEvent = panel(AppTheme.INK_2, AppTheme.LINE);
        selectedEvent.addView(eventMeta);
        selectedEvent.addView(eventTitle, topParams(7));
        selectedEvent.addView(eventDetail, topParams(8));
        body.addView(selectedEvent, fullTop(10));

        LinearLayout stats = new LinearLayout(this);
        stats.addView(metric(String.valueOf(journal.size()), "观察节点", AppTheme.TEAL), weighted(1, 0));
        stats.addView(metric(String.valueOf(categories.size()), "现场主题", AppTheme.AMBER), weighted(1, 8));
        stats.addView(metric(String.valueOf(contentStore.objects("works").size()), "作品落点", AppTheme.RED), weighted(1, 8));
        body.addView(stats, fullTop(12));

        addSection(body, "连接方式", "不是按日期堆叠，而是按信号生长");
        body.addView(methodStep("01", "看见", "记录现场发生了什么"), fullTop(8));
        body.addView(methodStep("02", "求证", "区分事实、推断和待确认"), fullTop(8));
        body.addView(methodStep("03", "转化", "把重复问题变成产品线索"), fullTop(8));

        FloatingActionView openJournal = new FloatingActionView(this, "↗", "进入现场日志", AppTheme.AMBER, AppTheme.INK, 0.7f);
        openJournal.setOnClickListener(view -> {
            internshipMode = 1;
            navigation.select(1, true);
            showDestination(1, true);
        });
        body.addView(openJournal, fullTop(16));
        return wrap(body);
    }

    private View buildUpdatePage() {
        LinearLayout body = pageBody(true);
        body.addView(kicker("我的更新", "内容和能力各自生长，失败也不影响当前版本"));

        LinearLayout state = panel(AppTheme.INK, AppTheme.INK);
        state.addView(AppTheme.label(this,
            updateBusy ? "正在同步" : "当前通道 · " + (remoteManifest == null ? "稳定" : remoteManifest.channel),
            updateBusy ? AppTheme.AMBER : AppTheme.TEAL));
        state.addView(AppTheme.text(this, updateMessage, 22, AppTheme.WHITE, true), topParams(8));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(updateProgress);
        progress.setProgressTintList(ColorStateList.valueOf(AppTheme.AMBER));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(AppTheme.INK_3));
        state.addView(progress, new LinearLayout.LayoutParams(-1, dp(8)));
        state.addView(AppTheme.text(this,
            "App " + BuildConfig.VERSION_NAME + " / 内容 " + contentStore.version(),
            11, AppTheme.MUTED, true), topParams(10));
        body.addView(state, fullTop(16));

        TextView check = command(updateBusy ? "更新任务进行中" : "检查并同步成长包", "↻", AppTheme.INK, AppTheme.AMBER);
        check.setEnabled(!updateBusy);
        check.setAlpha(updateBusy ? 0.55f : 1f);
        check.setOnClickListener(view -> checkForUpdates(true));
        body.addView(check, fullTop(12));

        if (remoteManifest != null && remoteManifest.hasAppUpdate()) {
            TextView apk = command("下载并安装 " + remoteManifest.versionName, "↓", AppTheme.WHITE, AppTheme.RED);
            apk.setOnClickListener(view -> downloadAppUpdate());
            body.addView(apk, fullTop(9));
        } else if (remoteManifest != null) {
            body.addView(statusBand("能力包", "当前 APK 已是最新版本", AppTheme.TEAL), fullTop(9));
        }

        addSection(body, "更新保护", "新内容经过四步确认后才会生效");
        body.addView(updateGate("01", "获取版本清单", "HTTPS 清单声明版本、渠道、大小和下载地址", AppTheme.TEAL), fullTop(10));
        body.addView(updateGate("02", "暂存更新包", "新包先单独保存，不直接覆盖当前内容", AppTheme.AMBER), fullTop(8));
        body.addView(updateGate("03", "SHA-256 校验", "下载结果必须与发布端摘要完全一致", AppTheme.RED), fullTop(8));
        body.addView(updateGate("04", "确认切换", "验证通过才替换；失败继续使用当前版本", AppTheme.WHITE), fullTop(8));

        addSection(body, "三层生长", "不同内容用不同方式进入 App");
        body.addView(layerPanel("内容包", "日志、作品、实验数据与界面内容", "无需重装", AppTheme.TEAL), fullTop(10));
        body.addView(layerPanel("能力包", "新交互、系统权限、渲染与更新能力", "安装确认", AppTheme.AMBER), fullTop(8));
        body.addView(layerPanel("本地证据", "原始视频、个性称呼与本地附件", "只在本机", AppTheme.RED), fullTop(8));

        if (remoteManifest != null) {
            addSection(body, "本次变化 · " + remoteManifest.versionName, remoteManifest.publishedAt);
            body.addView(AppTheme.text(this, remoteManifest.releaseNotes, 12, AppTheme.INK_3, false), fullTop(8));
            TextView release = command("查看完整发布页", "↗", AppTheme.WHITE, AppTheme.INK_2);
            release.setOnClickListener(view -> updateManager.openReleasePage(remoteManifest));
            body.addView(release, fullTop(10));
        }

        addSection(body, "本机维护", "手动导入与故障恢复");
        TextView importButton = command("导入 portfolio.json", "＋", AppTheme.INK, AppTheme.PAPER_2);
        importButton.setOnClickListener(view -> importContent());
        body.addView(importButton, fullTop(10));
        TextView restore = command("恢复内置安全版本", "↺", AppTheme.WHITE, AppTheme.INK_3);
        restore.setOnClickListener(view -> confirmRestore());
        body.addView(restore, fullTop(8));
        return wrap(body);
    }

    private void checkForUpdates(boolean userInitiated) {
        if (updateBusy) return;
        updateBusy = true;
        updateProgress = 6;
        updateMessage = "正在连接更新仓库";
        if (activeDestination == 4) showDestination(4, false);
        if (userInitiated) Toast.makeText(this, "开始检查更新", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                UpdateManager.Manifest manifest = updateManager.check();
                remoteManifest = manifest;
                runOnUiThread(() -> setUpdateState(24, "版本清单已验证", true));
                boolean changed = updateManager.installContentIfNeeded(manifest,
                    (percent, message) -> runOnUiThread(() -> setUpdateState(percent, message, true)));
                runOnUiThread(() -> {
                    updateBusy = false;
                    updateProgress = 100;
                    if (manifest.hasAppUpdate()) {
                        updateMessage = "发现能力包 " + manifest.versionName;
                        headerStatus.setText(String.format(Locale.US, "有更新 · %s", manifest.versionName));
                        headerStatus.setTextColor(AppTheme.AMBER);
                    } else if (changed) {
                        updateMessage = "成长资源包已更新";
                    } else {
                        updateMessage = "所有模块均为最新";
                    }
                    if (activeDestination == 4 || changed) showDestination(activeDestination, false);
                    if (userInitiated) Toast.makeText(this, updateMessage, Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    updateBusy = false;
                    updateProgress = 0;
                    updateMessage = "更新检查失败，继续使用当前安全版本";
                    if (activeDestination == 4) showDestination(4, false);
                    if (userInitiated) Toast.makeText(this,
                        updateMessage + "：" + friendlyError(error),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void downloadAppUpdate() {
        if (remoteManifest == null || updateBusy) return;
        if (!getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(this, "请先允许 SignalTrace 安装更新包，返回后再次点击下载", Toast.LENGTH_LONG).show();
            startActivity(new Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName())
            ));
            return;
        }
        updateBusy = true;
        updateProgress = 1;
        updateMessage = "准备下载能力包";
        showDestination(4, false);
        new Thread(() -> {
            try {
                updateManager.downloadAndOpenApk(remoteManifest,
                    (percent, message) -> runOnUiThread(() -> setUpdateState(percent, message, true)));
                runOnUiThread(() -> {
                    updateBusy = false;
                    updateMessage = "等待系统确认安装";
                    showDestination(4, false);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    updateBusy = false;
                    updateProgress = 0;
                    updateMessage = "能力包下载失败";
                    showDestination(4, false);
                    Toast.makeText(this, friendlyError(error), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setUpdateState(int progress, String message, boolean refresh) {
        updateProgress = progress;
        updateMessage = message;
        if (refresh && activeDestination == 4) showDestination(4, false);
    }

    private View videoEvidencePanel() {
        String rawUri = contentStore.preferences().getString(VIDEO_URI_KEY, "");
        LinearLayout card = panel(AppTheme.INK, rawUri.isEmpty() ? AppTheme.LINE : AppTheme.TEAL);
        card.addView(AppTheme.label(this, "本地录像", rawUri.isEmpty() ? AppTheme.MUTED : AppTheme.TEAL));
        card.addView(AppTheme.text(this,
            rawUri.isEmpty() ? "原始录像尚未关联" : "原始录像已在本机形成证据入口",
            18, AppTheme.WHITE, true), topParams(8));
        card.addView(AppTheme.text(this,
            "文件只通过系统选择器授权给本 App，不进入公开内容包，也不会自动上传。",
            11, AppTheme.MUTED, false), topParams(8));
        TextView action = command(rawUri.isEmpty() ? "关联原始录像" : "打开原始录像", rawUri.isEmpty() ? "+" : "▶", AppTheme.INK, AppTheme.TEAL);
        action.setOnClickListener(view -> {
            if (rawUri.isEmpty()) attachVideo();
            else openLocalUri(Uri.parse(rawUri), "video/*");
        });
        card.addView(action, topParams(14));
        if (!rawUri.isEmpty()) {
            TextView replace = command("重新选择", "↻", AppTheme.WHITE, AppTheme.INK_3);
            replace.setOnClickListener(view -> attachVideo());
            card.addView(replace, topParams(8));
        }
        return card;
    }

    private View trustGrid() {
        LinearLayout row = new LinearLayout(this);
        row.addView(metric("事实", "可直接记录", AppTheme.TEAL), weighted(1, 0));
        row.addView(metric("推断", "标注依据", AppTheme.AMBER), weighted(1, 8));
        row.addView(metric("待确认", "保留开放", AppTheme.RED), weighted(1, 8));
        return row;
    }

    private View moduleLink(String index, String title, String detail, int accent, int destination) {
        LinearLayout card = panel(AppTheme.PAPER, AppTheme.INK);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        TextView number = AppTheme.text(this, index, 17, accent, true);
        number.setGravity(Gravity.CENTER);
        card.addView(number, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(AppTheme.text(this, title, 17, AppTheme.INK, true));
        copy.addView(AppTheme.text(this, detail, 11, AppTheme.INK_3, false), topParams(4));
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(AppTheme.text(this, "→", 19, AppTheme.INK, true));
        AppTheme.pressable(card);
        card.setOnClickListener(view -> {
            navigation.select(destination, true);
            showDestination(destination, true);
        });
        return card;
    }

    private View subspace(String code, String title, String detail, int accent, int mode) {
        LinearLayout card = panel(AppTheme.INK_2, AppTheme.LINE);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = AppTheme.text(this, code, 24, accent, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(AppTheme.panel(AppTheme.INK, accent, 6, this));
        card.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(AppTheme.text(this, title, 17, AppTheme.WHITE, true));
        copy.addView(AppTheme.text(this, detail, 11, AppTheme.MUTED, false), topParams(4));
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(AppTheme.text(this, "↗", 17, AppTheme.WHITE, true));
        AppTheme.pressable(card);
        card.setOnClickListener(view -> {
            internshipMode = mode;
            showDestination(1, true);
        });
        return card;
    }

    private View expandable(String eyebrow, String title, String summary, View detail) {
        LinearLayout card = panel(AppTheme.PAPER, AppTheme.INK);
        card.setLayoutTransition(smoothLayoutTransition());
        TextView meta = AppTheme.label(this, eyebrow, AppTheme.RED);
        card.addView(meta);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = AppTheme.text(this, title, 17, AppTheme.INK, true);
        heading.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = AppTheme.text(this, "＋", 22, AppTheme.INK, false);
        arrow.setGravity(Gravity.CENTER);
        heading.addView(arrow, new LinearLayout.LayoutParams(dp(40), dp(40)));
        card.addView(heading, topParams(5));
        TextView summaryView = AppTheme.text(this, summary, 11, AppTheme.INK_3, false);
        summaryView.setMaxLines(3);
        card.addView(summaryView, topParams(4));
        detail.setVisibility(View.GONE);
        card.addView(detail, topParams(14));
        AppTheme.pressable(card);
        card.setOnClickListener(view -> {
            boolean open = detail.getVisibility() == View.VISIBLE;
            detail.setVisibility(open ? View.GONE : View.VISIBLE);
            summaryView.setVisibility(open ? View.VISIBLE : View.GONE);
            arrow.animate().rotation(open ? 0 : 45).setDuration(360).setInterpolator(AppTheme.EASE_OUT).start();
        });
        return card;
    }

    private LayoutTransition smoothLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(360);
        transition.setInterpolator(LayoutTransition.APPEARING, AppTheme.EASE_OUT);
        transition.setInterpolator(LayoutTransition.DISAPPEARING, AppTheme.EASE_IN_OUT);
        transition.setInterpolator(LayoutTransition.CHANGING, AppTheme.EASE_OUT);
        transition.enableTransitionType(LayoutTransition.CHANGING);
        return transition;
    }

    private View detailLine(String label, String value, int accent) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(12), dp(10), dp(12), dp(10));
        block.setBackground(AppTheme.panel(AppTheme.INK_2, AppTheme.LINE, 7, this));
        block.addView(AppTheme.label(this, label, accent));
        block.addView(AppTheme.text(this, value, 11, AppTheme.PAPER_2, false), topParams(5));
        return block;
    }

    private View keyValuePanel(String meta, String title, String detail) {
        LinearLayout card = panel(AppTheme.PAPER, AppTheme.INK);
        card.addView(AppTheme.label(this, meta, AppTheme.RED));
        card.addView(AppTheme.text(this, title, 17, AppTheme.INK, true), topParams(5));
        card.addView(AppTheme.text(this, detail, 11, AppTheme.INK_3, false), topParams(7));
        return card;
    }

    private View numberedRule(String number, String title, String detail, int accent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = AppTheme.text(this, number, 19, AppTheme.INK, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(AppTheme.solid(accent, 6, this));
        row.addView(mark, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(AppTheme.text(this, title, 15, AppTheme.INK, true));
        copy.addView(AppTheme.text(this, detail, 11, AppTheme.INK_3, false), topParams(3));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private View methodStep(String index, String title, String detail) {
        LinearLayout card = panel(AppTheme.INK_2, AppTheme.LINE);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(AppTheme.text(this, index, 19, AppTheme.AMBER, true), new LinearLayout.LayoutParams(dp(42), -2));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(AppTheme.text(this, title, 15, AppTheme.WHITE, true));
        copy.addView(AppTheme.text(this, detail, 11, AppTheme.MUTED, false), topParams(3));
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        return card;
    }

    private View updateGate(String index, String title, String detail, int accent) {
        LinearLayout row = panel(AppTheme.PAPER, AppTheme.INK);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView number = AppTheme.text(this, index, 16, accent, true);
        row.addView(number, new LinearLayout.LayoutParams(dp(44), -2));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(AppTheme.text(this, title, 15, AppTheme.INK, true));
        copy.addView(AppTheme.text(this, detail, 11, AppTheme.INK_3, false), topParams(4));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private View layerPanel(String title, String detail, String status, int accent) {
        LinearLayout card = panel(AppTheme.INK_2, AppTheme.LINE);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = AppTheme.text(this, title, 14, AppTheme.WHITE, true);
        top.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView chip = AppTheme.label(this, status, accent);
        top.addView(chip);
        card.addView(top);
        card.addView(AppTheme.text(this, detail, 11, AppTheme.MUTED, false), topParams(7));
        return card;
    }

    private View statusBand(String label, String value, int accent) {
        LinearLayout row = panel(AppTheme.INK_2, AppTheme.LINE);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(AppTheme.label(this, label, accent), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(AppTheme.text(this, value, 12, AppTheme.WHITE, true));
        return row;
    }

    private View progressRail(float value, String label, String numeric) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout labels = new LinearLayout(this);
        labels.addView(AppTheme.label(this, label, AppTheme.MUTED), new LinearLayout.LayoutParams(0, -2, 1f));
        labels.addView(AppTheme.label(this, numeric, AppTheme.WHITE));
        block.addView(labels);
        FrameLayout track = new FrameLayout(this);
        track.setBackground(AppTheme.solid(AppTheme.INK_3, 1, this));
        View fill = new View(this);
        fill.setBackgroundColor(AppTheme.AMBER);
        track.addView(fill, new FrameLayout.LayoutParams(0, -1));
        track.post(() -> {
            ViewGroup.LayoutParams params = fill.getLayoutParams();
            params.width = Math.round(track.getWidth() * Math.max(0f, Math.min(1f, value)));
            fill.setLayoutParams(params);
            fill.setScaleX(0f);
            fill.setPivotX(0);
            fill.animate().scaleX(1f).setDuration(800).setInterpolator(AppTheme.EASE_OUT).start();
        });
        block.addView(track, new LinearLayout.LayoutParams(-1, dp(7)));
        return block;
    }

    private View segment(String[] labels, int selected, SegmentListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.setBackground(AppTheme.panel(AppTheme.INK, AppTheme.LINE, 7, this));
        for (int index = 0; index < labels.length; index++) {
            final int item = index;
            TextView tab = AppTheme.text(this, labels[index], 11, index == selected ? AppTheme.INK : AppTheme.MUTED, true);
            tab.setGravity(Gravity.CENTER);
            tab.setMinHeight(dp(40));
            if (index == selected) {
                tab.setBackground(AppTheme.solid(AppTheme.AMBER, 5, this));
            } else {
                tab.setBackgroundColor(Color.TRANSPARENT);
            }
            AppTheme.pressable(tab);
            tab.setOnClickListener(view -> {
                if (item != selected) listener.onSelected(item);
            });
            row.addView(tab, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return row;
    }

    private View kicker(String label, String title) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(AppTheme.label(this, label, AppTheme.RED));
        block.addView(AppTheme.text(this, title, 24, AppTheme.INK, true), topParams(7));
        return block;
    }

    private void addSection(LinearLayout body, String label, String title) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(AppTheme.label(this, label, AppTheme.RED));
        block.addView(AppTheme.text(this, title, 17, AppTheme.INK, true), topParams(5));
        body.addView(block, fullTop(24));
    }

    private View metric(String value, String label, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(12), dp(10), dp(12));
        card.setBackground(AppTheme.panel(AppTheme.INK_2, AppTheme.LINE, 7, this));
        card.addView(AppTheme.text(this, value, 20, accent, true));
        card.addView(AppTheme.label(this, label, AppTheme.MUTED), topParams(4));
        return card;
    }

    private TextView command(String label, String icon, int textColor, int fill) {
        TextView view = AppTheme.text(this, icon + "  " + label, 12, textColor, true);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(46));
        view.setPadding(dp(14), dp(10), dp(14), dp(10));
        view.setBackground(AppTheme.panel(fill, fill, 7, this));
        AppTheme.pressable(view);
        return view;
    }

    private LinearLayout panel(int fill, int stroke) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(15), dp(15), dp(15), dp(15));
        panel.setBackground(AppTheme.panel(fill, stroke, 10, this));
        return panel;
    }

    private LinearLayout pageBody(boolean paddedTop) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), paddedTop ? dp(18) : 0, dp(14), dp(28));
        body.setBackgroundColor(AppTheme.PAPER);
        return body;
    }

    private View wrap(LinearLayout body) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private void addReveal(LinearLayout parent, View child, int index) {
        parent.addView(child);
        AppTheme.reveal(child, index);
    }

    private void addReveal(LinearLayout parent, View child, int index, int topMargin) {
        parent.addView(child, topParams(topMargin));
        AppTheme.reveal(child, index);
    }

    private String displayName() {
        if (contentStore == null) return "我的信号轨迹";
        String value = contentStore.preferences().getString(DISPLAY_NAME_KEY, "我的信号轨迹");
        return value == null || value.trim().isEmpty() ? "我的信号轨迹" : value.trim();
    }

    private void showDisplayNameDialog() {
        EditText input = new EditText(this);
        input.setText(displayName());
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(18)});
        input.setPadding(dp(16), dp(13), dp(16), dp(13));
        input.setTextColor(AppTheme.INK);
        input.setHintTextColor(AppTheme.MUTED);
        input.setBackground(AppTheme.panel(AppTheme.PAPER, AppTheme.INK, 7, this));

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(dp(18), dp(8), dp(18), 0);
        wrapper.addView(input, new FrameLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this)
            .setTitle("这里怎么称呼你")
            .setView(wrapper)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", (dialog, which) -> {
                String value = input.getText().toString().trim();
                contentStore.preferences().edit()
                    .putString(DISPLAY_NAME_KEY, value.isEmpty() ? "我的信号轨迹" : value)
                    .apply();
                View shell = buildShell();
                setContentView(shell);
                showDestination(0, false);
            })
            .show();
    }

    private void toggleImmersive() {
        immersive = !immersive;
        if (appHeader != null) appHeader.setVisibility(immersive ? View.GONE : View.VISIBLE);
        if (navigation != null) navigation.setVisibility(immersive ? View.GONE : View.VISIBLE);
        int flags = immersive
            ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            : View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        Toast.makeText(this, immersive ? "已进入沉浸模式，返回键退出" : "已退出沉浸模式", Toast.LENGTH_SHORT).show();
    }

    private void attachVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("video/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, ATTACH_MEDIA_REQUEST);
    }

    private void importContent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, IMPORT_CONTENT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Some providers grant only session access.
        }
        if (requestCode == ATTACH_MEDIA_REQUEST) {
            contentStore.preferences().edit().putString(VIDEO_URI_KEY, uri.toString()).apply();
            showDestination(2, false);
            Toast.makeText(this, "原始录像已关联，仅保存在本机", Toast.LENGTH_LONG).show();
        } else if (requestCode == IMPORT_CONTENT_REQUEST) {
            try {
                contentStore.importUri(uri);
                showDestination(4, false);
                Toast.makeText(this, "本地内容包已验证并切换", Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Toast.makeText(this, "导入失败：" + friendlyError(error), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openLocalUri(Uri uri, String mime) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception error) {
            Toast.makeText(this, "无法打开本地文件，请重新选择", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRestore() {
        new AlertDialog.Builder(this)
            .setTitle("恢复内置内容？")
            .setMessage("在线或本地导入的成长资源包将被移除；个性称呼和本地视频关联会保留。")
            .setNegativeButton("取消", null)
            .setPositiveButton("恢复", (dialog, which) -> {
                contentStore.restoreEmbedded();
                updateMessage = "已恢复内置安全版本";
                showDestination(4, false);
            })
            .show();
    }

    @Override
    public void onBackPressed() {
        if (immersive) {
            toggleImmersive();
            return;
        }
        if (activeDestination == 1 && internshipMode != 0) {
            internshipMode = 0;
            showDestination(1, true);
            return;
        }
        if (activeDestination != 0) {
            navigation.select(0, true);
            showDestination(0, true);
            return;
        }
        super.onBackPressed();
    }

    private String friendlyError(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private int dp(float value) {
        return AppTheme.dp(this, value);
    }

    private LinearLayout.LayoutParams fullTop(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(top), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams topParams(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(top), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams weighted(float weight, int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, weight);
        params.setMargins(dp(leftMargin), 0, 0, 0);
        return params;
    }

    private interface SegmentListener {
        void onSelected(int index);
    }
}
