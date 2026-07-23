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
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int IMPORT_CONTENT_REQUEST = 4001;
    private static final int ATTACH_MEDIA_REQUEST = 4002;
    private static final String VIDEO_URI_KEY = "hydrogen_response_video";
    private static final String CUSTOM_WISHES_KEY = "custom_wishes";

    private ContentStore contentStore;
    private UpdateManager updateManager;
    private FrameLayout pageHost;
    private GeometricNavBar navigation;
    private TextView headerStatus;
    private int activeDestination;
    private int internshipMode;
    private boolean response95 = true;
    private UpdateManager.Manifest remoteManifest;
    private int updateProgress;
    private String updateMessage = "等待检查";
    private boolean updateBusy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        contentStore = new ContentStore(this);
        updateManager = new UpdateManager(this, contentStore);
        setContentView(buildShell());
        showDestination(0, false);
        PortfolioSyncJobService.schedule(this);
        checkForUpdates(false);
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(AppTheme.INK);
        window.setNavigationBarColor(AppTheme.INK);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
    }

    private View buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(AppTheme.INK);
        shell.addView(buildHeader(), new LinearLayout.LayoutParams(-1, dp(64)));

        pageHost = new FrameLayout(this);
        pageHost.setBackgroundColor(AppTheme.PAPER);
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
        TextView title = AppTheme.text(this, "SIGNALTRACE / GROWTH OS", 14, AppTheme.WHITE, true);
        TextView subtitle = AppTheme.text(this, "现场信号 · 证据档案 · 人生轨迹", 10, AppTheme.MUTED, false);
        copy.addView(title);
        copy.addView(subtitle, topParams(2));
        bar.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        headerStatus = AppTheme.label(this, "LIVE / " + BuildConfig.VERSION_NAME, AppTheme.TEAL);
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
        activeDestination = destination;
        View next = buildDestination(destination);
        if (!animate || pageHost.getChildCount() == 0) {
            pageHost.removeAllViews();
            pageHost.addView(next, new FrameLayout.LayoutParams(-1, -1));
            navigation.select(destination, false);
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
        if (destination == 3) return buildWishlistPage();
        if (destination == 4) return buildUpdatePage();
        return buildIdentityPage();
    }

    private View buildIdentityPage() {
        LinearLayout body = pageBody(false);
        SignalSceneView scene = new SignalSceneView(this);
        body.addView(scene, new LinearLayout.LayoutParams(-1, dp(248)));

        JSONObject profile = contentStore.object("profile");
        LinearLayout identity = panel(AppTheme.PAPER, AppTheme.INK);
        addReveal(identity, AppTheme.label(this, "ID / FIELD OBSERVER / 2026", AppTheme.RED), 0);
        addReveal(identity, AppTheme.text(this, profile.optString("name", "我的实习成长档案"), 28, AppTheme.INK, true), 1, 8);
        addReveal(identity, AppTheme.text(this, profile.optString("role", "仪控班实习 · 现场学习进行中"), 13, AppTheme.INK_3, true), 2, 8);
        addReveal(identity, AppTheme.text(this, profile.optString("headline", ""), 16, AppTheme.INK, false), 3, 18);
        addReveal(identity, AppTheme.text(this, profile.optString("summary", ""), 12, AppTheme.INK_3, false), 4, 10);

        LinearLayout idFooter = new LinearLayout(this);
        idFooter.setGravity(Gravity.CENTER_VERTICAL);
        TextView serial = AppTheme.label(this, "TRACE-" + contentStore.version(), AppTheme.INK);
        idFooter.addView(serial, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView copy = command("复制名片", "↗", AppTheme.INK, AppTheme.AMBER);
        copy.setOnClickListener(view -> copyIdentity(profile));
        idFooter.addView(copy);
        addReveal(identity, idFooter, 5, 18);
        body.addView(identity, fullTop(0));

        LinearLayout stats = new LinearLayout(this);
        stats.setWeightSum(3);
        stats.addView(metric(String.valueOf(contentStore.objects("journal").size()), "现场记录", AppTheme.TEAL), weighted(1, 0));
        stats.addView(metric(String.valueOf(contentStore.objects("works").size()), "作品模块", AppTheme.AMBER), weighted(1, 8));
        stats.addView(metric("01", "产品案例", AppTheme.RED), weighted(1, 8));
        body.addView(stats, fullTop(14));

        addSection(body, "NOW / CURRENT SIGNAL", "今天正在形成的证据");
        List<JSONObject> journal = contentStore.objects("journal");
        if (!journal.isEmpty()) {
            JSONObject latest = journal.get(journal.size() - 1);
            LinearLayout now = panel(AppTheme.INK_2, AppTheme.LINE);
            now.addView(AppTheme.label(this,
                latest.optString("date", "") + " / " + latest.optString("category", ""),
                AppTheme.TEAL));
            now.addView(AppTheme.text(this, latest.optString("title", ""), 19, AppTheme.WHITE, true), topParams(8));
            now.addView(AppTheme.text(this, latest.optString("learning", ""), 12, AppTheme.PAPER_2, false), topParams(10));
            TextView enter = command("进入实习档案", "→", AppTheme.WHITE, AppTheme.TEAL);
            enter.setOnClickListener(view -> {
                navigation.select(1, true);
                showDestination(1, true);
            });
            now.addView(enter, topParams(16));
            body.addView(now, fullTop(10));
        }

        addSection(body, "SYSTEM / MODULES", "不是展示页，而是会继续生长的个人系统");
        body.addView(moduleLink("01", "实习档案", "按总览、日志、作品、能力图谱进入详细子界面", AppTheme.AMBER, 1), fullTop(10));
        body.addView(moduleLink("02", "响应实验室", "用原始采样点触摸查看曲线、模型和停止条件", AppTheme.TEAL, 2), fullTop(9));
        body.addView(moduleLink("03", "人生愿望", "可完成、可追加、只保存在自己的设备中", AppTheme.RED, 3), fullTop(9));
        body.setPadding(0, 0, 0, dp(20));
        return wrap(body);
    }

    private View buildInternshipPage() {
        LinearLayout body = pageBody(true);
        body.addView(kicker("INTERNSHIP / POWER PLANT I&C", "实习不是一列文本，而是一组可以深入的现场空间"));
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
        phase.addView(AppTheme.label(this, "PHASE / FIELD LEARNING", AppTheme.AMBER));
        phase.addView(AppTheme.text(this, "从看见设备，到理解信号为什么可信", 23, AppTheme.WHITE, true), topParams(8));
        phase.addView(AppTheme.text(this,
            journal.size() + " 条现场事件已经形成证据链。所有公开内容保留参与边界，不替代师傅判断，也不连接生产系统。",
            12, AppTheme.PAPER_2, false), topParams(10));
        phase.addView(progressRail(Math.min(1f, journal.size() / 24f), "当前学习周期", journal.size() + " / 24 EVENTS"), topParams(18));
        body.addView(phase, fullTop(16));

        addSection(body, "SUBSPACE / ENTER", "详细信息分布在内部，不再堆成一张长页面");
        body.addView(subspace("A", "现场日志", journal.size() + " 条事实、参与、学习与产品信号", AppTheme.TEAL, 1), fullTop(10));
        body.addView(subspace("B", "作品与项目", contentStore.objects("works").size() + " 项成果 + FieldTrace 产品案例", AppTheme.AMBER, 2), fullTop(9));
        body.addView(subspace("C", "能力图谱", contentStore.objects("capabilities").size() + " 条能力证据 + 流程模型入口", AppTheme.RED, 3), fullTop(9));

        addSection(body, "BOUNDARY / TRUST", "事实、推断、待确认必须保持距离");
        body.addView(trustGrid(), fullTop(10));
    }

    private void buildJournalSubPage(LinearLayout body) {
        List<JSONObject> journal = contentStore.objects("journal");
        addSection(body, "FIELD LOG / " + journal.size(), "点击事件展开四层证据");
        for (int index = journal.size() - 1; index >= 0; index--) {
            JSONObject item = journal.get(index);
            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(detailLine("OBS / 现场观察", item.optString("observation", ""), AppTheme.TEAL));
            detail.addView(detailLine("ACT / 本人参与", item.optString("participation", ""), AppTheme.AMBER), topParams(10));
            detail.addView(detailLine("LEARN / 学习结论", item.optString("learning", ""), AppTheme.WHITE), topParams(10));
            detail.addView(detailLine("PRODUCT / 产品信号", item.optString("productSignal", ""), AppTheme.RED), topParams(10));
            body.addView(expandable(
                item.optString("date", "--.--"),
                item.optString("title", "现场记录"),
                item.optString("category", "未分类"),
                detail
            ), fullTop(index == journal.size() - 1 ? 10 : 9));
        }
    }

    private void buildWorksSubPage(LinearLayout body) {
        addSection(body, "OUTPUT / SELECTED WORK", "从现场观察到可验证作品");
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

        addSection(body, "PRODUCT CASE / FIELDTRACE", "真实痛点如何转成产品判断");
        for (JSONObject product : contentStore.objects("productCases")) {
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.addView(detailLine("USERS / 用户", product.optString("users", ""), AppTheme.TEAL));
            details.addView(detailLine("INSIGHT / 洞察", product.optString("insight", ""), AppTheme.AMBER), topParams(10));
            details.addView(detailLine("SOLUTION / 方案", product.optString("solution", ""), AppTheme.WHITE), topParams(10));
            details.addView(detailLine("EVIDENCE / 来源", product.optString("evidence", ""), AppTheme.RED), topParams(10));
            body.addView(expandable(
                product.optString("stage", "概念验证"),
                product.optString("title", "FieldTrace"),
                product.optString("problem", ""),
                details
            ), fullTop(10));
        }
    }

    private void buildCapabilitySubPage(LinearLayout body) {
        addSection(body, "CAPABILITY / EVIDENCE", "能力必须能指出证据落在哪里");
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
            copy.addView(AppTheme.label(this, "PROOF / " + capability.optString("proof", ""), AppTheme.RED), topParams(12));
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
            card.addView(row);
            body.addView(card, fullTop(index == 0 ? 10 : 9));
            index++;
        }

        addSection(body, "MODEL / PROCESS LIBRARY", "后续模型和实景素材有独立成长入口");
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
        body.addView(kicker("H₂ RESPONSE / ANALYSIS LAB", "原始采样、函数拟合、停止条件和本地视频在同一条证据链"));

        TextView cursorValue = AppTheme.text(this, "拖动曲线查看任意时刻", 12, AppTheme.WHITE, true);
        LinearLayout chartPanel = panel(AppTheme.INK_2, AppTheme.LINE);
        chartPanel.addView(segment(
            new String[]{"95% 标气 / 上升", "90% 标气 / 下降"},
            response95 ? 0 : 1,
            index -> {
                response95 = index == 0;
                showDestination(2, true);
            }
        ));
        ResponseCurveView curve = new ResponseCurveView(this);
        curve.setDataset(response95);
        curve.setCursorListener((seconds, value) -> cursorValue.setText(
            String.format(Locale.US, "T + %.0fs  /  %.3f%% H₂", seconds, value)
        ));
        chartPanel.addView(curve, new LinearLayout.LayoutParams(-1, dp(230)));
        cursorValue.setGravity(Gravity.END);
        chartPanel.addView(cursorValue, topParams(8));
        body.addView(chartPanel, fullTop(16));

        LinearLayout metrics = new LinearLayout(this);
        if (response95) {
            metrics.addView(metric("94.755%", "预测稳定值", AppTheme.AMBER), weighted(1, 0));
            metrics.addView(metric("0.9995", "双指数 R²", AppTheme.TEAL), weighted(1, 8));
            metrics.addView(metric("80s", "进入精度带", AppTheme.RED), weighted(1, 8));
        } else {
            metrics.addView(metric("90.003%", "预测稳定值", AppTheme.TEAL), weighted(1, 0));
            metrics.addView(metric("0.9978", "单指数 R²", AppTheme.AMBER), weighted(1, 8));
            metrics.addView(metric("30s", "进入精度带", AppTheme.RED), weighted(1, 8));
        }
        body.addView(metrics, fullTop(12));

        body.addView(videoEvidencePanel(), fullTop(14));

        addSection(body, "DECISION / STOP WAITING", "两个条件必须同时成立");
        LinearLayout decision = panel(AppTheme.PAPER, AppTheme.INK);
        decision.addView(numberedRule("A", "预测值收敛", "连续 3 次拟合的 Pmax 变化 < 0.02%", AppTheme.AMBER));
        decision.addView(numberedRule("B", "变化速率足够小", "最近两次读数变化率 < 0.002%/s", AppTheme.TEAL), topParams(12));
        decision.addView(AppTheme.text(this, "A + B 同时满足 → 停止等待，取最后一次预测稳定值。首次应用前仍需用标气验证当前设备。", 12, AppTheme.INK, true), topParams(16));
        body.addView(decision, fullTop(10));

        addSection(body, "METHOD / FIVE STEPS", "从录像读数到可以复核的结论");
        String[][] steps = {
            {"01", "开始计时", "通气后每 5 秒记录一次读数"},
            {"02", "形成样本", "采集 5–8 个点后开始指数拟合"},
            {"03", "持续重算", "每新增一个点，重新观察 Pmax 收敛"},
            {"04", "检查速率", "同步计算最近两点的读数变化率"},
            {"05", "双重判定", "收敛和速率同时满足才停止"}
        };
        for (String[] step : steps) {
            body.addView(methodStep(step[0], step[1], step[2]), fullTop(8));
        }

        addSection(body, "BOUNDARY / REPORT", "模型是判断辅助，不是现场校验的替代品");
        body.addView(keyValuePanel(
            "实测发现",
            "T90 约 73–90 秒",
            "相比厂家标称 ≤23 秒偏慢 3–4 倍，建议复核样气流量 400–600 ml/min、管路死体积、泄漏与传感器老化。"
        ), fullTop(10));
        return wrap(body);
    }

    private View buildWishlistPage() {
        LinearLayout body = pageBody(true);
        List<JSONObject> wishes = allWishes();
        int completed = 0;
        for (JSONObject wish : wishes) {
            if (isWishDone(wish.optString("id", ""))) completed++;
        }

        body.addView(kicker("LIFE LIST / OPEN HORIZON", "愿望不是完成率装饰，而是给未来行动留下坐标"));
        WishOrbitView orbit = new WishOrbitView(this);
        orbit.setProgress(completed, wishes.size());
        body.addView(orbit, new LinearLayout.LayoutParams(-1, dp(220)));

        TextView add = command("添加一个愿望", "+", AppTheme.INK, AppTheme.AMBER);
        add.setOnClickListener(view -> showAddWishDialog());
        body.addView(add, fullTop(10));

        String[] horizons = {"NOW", "NEXT", "LIFETIME"};
        String[] labels = {"现在", "下一程", "这一生"};
        for (int group = 0; group < horizons.length; group++) {
            addSection(body, horizons[group] + " / " + labels[group], wishGroupSummary(wishes, horizons[group]));
            for (JSONObject wish : wishes) {
                if (horizons[group].equalsIgnoreCase(wish.optString("horizon", "NEXT"))) {
                    body.addView(wishCard(wish), fullTop(8));
                }
            }
        }
        body.addView(AppTheme.text(this,
            "愿望的完成状态与自定义条目只保存在本机。在线成长包可以增加新的默认方向，但不会覆盖你的个人选择。",
            11, AppTheme.MUTED, false), fullTop(20));
        return wrap(body);
    }

    private View buildUpdatePage() {
        LinearLayout body = pageBody(true);
        body.addView(kicker("UPDATE DEPOT / STAGED DELIVERY", "像大型游戏一样分层：清单、资源包、完整性验证、能力包"));

        LinearLayout state = panel(AppTheme.INK, AppTheme.INK);
        state.addView(AppTheme.label(this,
            updateBusy ? "PIPELINE / RUNNING" : "CHANNEL / " + (remoteManifest == null ? "STABLE" : remoteManifest.channel.toUpperCase(Locale.ROOT)),
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

        addSection(body, "PIPELINE / FOUR GATES", "任何资源进入正式档案前都要经过四道门");
        body.addView(updateGate("01", "获取版本清单", "HTTPS 清单声明版本、渠道、大小和下载地址", AppTheme.TEAL), fullTop(10));
        body.addView(updateGate("02", "暂存更新包", "新包先进入 staging，不直接覆盖可用内容", AppTheme.AMBER), fullTop(8));
        body.addView(updateGate("03", "SHA-256 校验", "下载结果必须与发布端摘要完全一致", AppTheme.RED), fullTop(8));
        body.addView(updateGate("04", "原子切换", "验证通过才替换 active；失败继续使用旧版本", AppTheme.WHITE), fullTop(8));

        addSection(body, "RELEASE / LAYERS", "后续成长可以走不同更新层");
        body.addView(layerPanel("RESOURCE PACK", "日记、作品、愿望模板、实验数据与主题配置", "无需重装", AppTheme.TEAL), fullTop(10));
        body.addView(layerPanel("APK CAPABILITY", "新原生交互、系统权限、渲染能力与更新引擎", "需要系统确认安装", AppTheme.AMBER), fullTop(8));
        body.addView(layerPanel("PRIVATE EVIDENCE", "原始视频、个人愿望完成状态与本地附件", "永不公开推送", AppTheme.RED), fullTop(8));

        if (remoteManifest != null) {
            addSection(body, "RELEASE NOTES / " + remoteManifest.versionName, remoteManifest.publishedAt);
            body.addView(AppTheme.text(this, remoteManifest.releaseNotes, 12, AppTheme.INK_3, false), fullTop(8));
            TextView release = command("查看完整发布页", "↗", AppTheme.WHITE, AppTheme.INK_2);
            release.setOnClickListener(view -> updateManager.openReleasePage(remoteManifest));
            body.addView(release, fullTop(10));
        }

        addSection(body, "LOCAL / MAINTENANCE", "本地导入与故障恢复");
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
                        headerStatus.setText(String.format(Locale.US, "UPDATE / %s", manifest.versionName));
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
        card.addView(AppTheme.label(this, "LOCAL EVIDENCE / VIDEO", rawUri.isEmpty() ? AppTheme.MUTED : AppTheme.TEAL));
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

    private List<JSONObject> allWishes() {
        List<JSONObject> result = new ArrayList<>(contentStore.objects("wishlist"));
        try {
            JSONArray custom = new JSONArray(contentStore.preferences().getString(CUSTOM_WISHES_KEY, "[]"));
            for (int index = 0; index < custom.length(); index++) {
                JSONObject item = custom.optJSONObject(index);
                if (item != null) result.add(item);
            }
        } catch (Exception ignored) {
            // Invalid local custom entries are ignored without affecting the published list.
        }
        return result;
    }

    private View wishCard(JSONObject wish) {
        String id = wish.optString("id", "wish-" + wish.optString("title", "").hashCode());
        boolean done = isWishDone(id);
        LinearLayout card = panel(done ? AppTheme.INK_2 : AppTheme.PAPER, done ? AppTheme.TEAL : AppTheme.INK);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView check = AppTheme.text(this, done ? "✓" : "◇", 22, done ? AppTheme.TEAL : AppTheme.RED, true);
        check.setGravity(Gravity.CENTER);
        card.addView(check, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(AppTheme.text(this, wish.optString("title", "未命名愿望"), 16, done ? AppTheme.WHITE : AppTheme.INK, true));
        String note = wish.optString("note", "");
        if (!note.isEmpty()) copy.addView(AppTheme.text(this, note, 11, done ? AppTheme.MUTED : AppTheme.INK_3, false), topParams(4));
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1f);
        copyParams.setMargins(dp(8), 0, 0, 0);
        card.addView(copy, copyParams);
        AppTheme.pressable(card);
        card.setOnClickListener(view -> {
            contentStore.preferences().edit().putBoolean("wish_done_" + id, !done).apply();
            showDestination(3, false);
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        });
        if (wish.optBoolean("custom", false)) {
            card.setOnLongClickListener(view -> {
                confirmDeleteWish(id);
                return true;
            });
        }
        return card;
    }

    private void showAddWishDialog() {
        EditText input = new EditText(this);
        input.setHint("例如：完成一个真正帮助现场学习的产品");
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setPadding(dp(18), dp(14), dp(18), dp(14));
        input.setTextColor(AppTheme.INK);
        input.setHintTextColor(AppTheme.MUTED);
        input.setBackground(AppTheme.panel(AppTheme.PAPER, AppTheme.INK, 8, this));
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(dp(18), dp(8), dp(18), 0);
        wrapper.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("添加一个愿望")
            .setView(wrapper)
            .setNegativeButton("取消", null)
            .setPositiveButton("加入 NEXT", (dialog, which) -> {
                String title = input.getText().toString().trim();
                if (title.isEmpty()) return;
                try {
                    JSONArray custom = new JSONArray(contentStore.preferences().getString(CUSTOM_WISHES_KEY, "[]"));
                    custom.put(new JSONObject()
                        .put("id", "local-" + System.currentTimeMillis())
                        .put("horizon", "NEXT")
                        .put("title", title)
                        .put("note", "由我在本机添加")
                        .put("custom", true));
                    contentStore.preferences().edit().putString(CUSTOM_WISHES_KEY, custom.toString()).apply();
                    showDestination(3, false);
                } catch (Exception error) {
                    Toast.makeText(this, "无法保存愿望", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void confirmDeleteWish(String id) {
        new AlertDialog.Builder(this)
            .setTitle("删除这条本地愿望？")
            .setMessage("长按仅可删除你在本机添加的条目。")
            .setNegativeButton("保留", null)
            .setPositiveButton("删除", (dialog, which) -> {
                try {
                    JSONArray source = new JSONArray(contentStore.preferences().getString(CUSTOM_WISHES_KEY, "[]"));
                    JSONArray next = new JSONArray();
                    for (int index = 0; index < source.length(); index++) {
                        JSONObject item = source.optJSONObject(index);
                        if (item != null && !id.equals(item.optString("id"))) next.put(item);
                    }
                    contentStore.preferences().edit()
                        .putString(CUSTOM_WISHES_KEY, next.toString())
                        .remove("wish_done_" + id)
                        .apply();
                    showDestination(3, false);
                } catch (Exception ignored) {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private boolean isWishDone(String id) {
        return contentStore.preferences().getBoolean("wish_done_" + id, false);
    }

    private String wishGroupSummary(List<JSONObject> wishes, String horizon) {
        int count = 0;
        int completed = 0;
        for (JSONObject wish : wishes) {
            if (horizon.equalsIgnoreCase(wish.optString("horizon", "NEXT"))) {
                count++;
                if (isWishDone(wish.optString("id", ""))) completed++;
            }
        }
        return completed + " / " + count + " 已完成";
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

    private void copyIdentity(JSONObject profile) {
        String value = profile.optString("name", "我的仪控实习作品集")
            + "\n" + profile.optString("role", "")
            + "\n" + profile.optString("headline", "");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("SignalTrace 名片", value));
        Toast.makeText(this, "名片文字已复制", Toast.LENGTH_SHORT).show();
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
            .setMessage("在线或本地导入的成长资源包将被移除；愿望完成状态和本地视频关联会保留。")
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
