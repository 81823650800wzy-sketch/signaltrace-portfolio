package com.signaltrace.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class EvidenceSpaceView extends View {
    interface Listener {
        void onSelected(int index, JSONObject item);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final List<JSONObject> items = new ArrayList<>();
    private final List<Node> nodes = new ArrayList<>();
    private final ScaleGestureDetector scaleDetector;
    private final ValueAnimator ambient = ValueAnimator.ofFloat(0f, 1f);
    private Listener listener;
    private float yaw = 18f;
    private float pitch = -8f;
    private float zoom = 1f;
    private float lastX;
    private float lastY;
    private float downX;
    private float downY;
    private float pulse;
    private int selected = -1;

    EvidenceSpaceView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoom = clamp(zoom * detector.getScaleFactor(), 0.72f, 1.62f);
                invalidate();
                return true;
            }
        });
        ambient.setDuration(4200);
        ambient.setRepeatCount(ValueAnimator.INFINITE);
        ambient.setInterpolator(null);
        ambient.addUpdateListener(animation -> {
            pulse = (float) animation.getAnimatedValue();
            invalidate();
        });
        ambient.start();
        setContentDescription("三维现场证据轨迹");
    }

    void setItems(List<JSONObject> source) {
        items.clear();
        items.addAll(source);
        selected = items.isEmpty() ? -1 : items.size() - 1;
        invalidate();
        notifySelected();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        ambient.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(AppTheme.INK);
        nodes.clear();
        drawGrid(canvas);
        buildNodes();
        drawConnections(canvas);
        drawNodes(canvas);
        drawLabels(canvas);
    }

    private void drawGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 24));
        for (int ring = 1; ring <= 4; ring++) {
            path.reset();
            for (int step = 0; step <= 48; step++) {
                double angle = Math.PI * 2 * step / 48;
                float radius = ring * 0.24f;
                Point point = project((float) Math.cos(angle) * radius, -0.72f, (float) Math.sin(angle) * radius);
                if (step == 0) path.moveTo(point.x, point.y);
                else path.lineTo(point.x, point.y);
            }
            canvas.drawPath(path, paint);
        }
        for (int line = 0; line < 8; line++) {
            double angle = Math.PI * 2 * line / 8;
            Point center = project(0, -0.72f, 0);
            Point edge = project((float) Math.cos(angle), -0.72f, (float) Math.sin(angle));
            canvas.drawLine(center.x, center.y, edge.x, edge.y, paint);
        }
    }

    private void buildNodes() {
        int count = Math.max(1, items.size());
        for (int index = 0; index < items.size(); index++) {
            float progress = index / (float) Math.max(1, count - 1);
            double angle = index * 1.17 + progress * Math.PI;
            float radius = 0.38f + progress * 0.55f;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float y = 0.58f - progress * 1.18f;
            Point point = project(x, y, z);
            nodes.add(new Node(index, point.x, point.y, point.depth, colorFor(items.get(index).optString("category", ""))));
        }
    }

    private void drawConnections(Canvas canvas) {
        if (nodes.size() < 2) return;
        path.reset();
        for (int index = 0; index < nodes.size(); index++) {
            Node node = nodes.get(index);
            if (index == 0) path.moveTo(node.x, node.y);
            else path.lineTo(node.x, node.y);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 1.6f));
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 105));
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);

        paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
        for (Node node : nodes) {
            Point floor = projectForIndex(node.index, -0.72f);
            paint.setColor(AppTheme.withAlpha(node.color, node.index == selected ? 125 : 42));
            canvas.drawLine(node.x, node.y, floor.x, floor.y, paint);
        }
    }

    private void drawNodes(Canvas canvas) {
        nodes.sort((a, b) -> Float.compare(b.depth, a.depth));
        for (Node node : nodes) {
            boolean active = node.index == selected;
            float radius = AppTheme.dp(getContext(), active ? 6f : 3.5f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(AppTheme.withAlpha(node.color, active ? 255 : 190));
            canvas.drawCircle(node.x, node.y, radius, paint);
            if (active) {
                float orbit = AppTheme.dp(getContext(), 11f + (float) Math.sin(pulse * Math.PI * 2) * 2f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(AppTheme.dp(getContext(), 1.2f));
                paint.setColor(AppTheme.WHITE);
                canvas.drawCircle(node.x, node.y, orbit, paint);
            }
        }
    }

    private void drawLabels(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(AppTheme.dp(getContext(), 10));
        paint.setColor(AppTheme.MUTED);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("起点", AppTheme.dp(getContext(), 12), getHeight() - AppTheme.dp(getContext(), 12), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(AppTheme.TEAL);
        canvas.drawText("现在", getWidth() - AppTheme.dp(getContext(), 12), AppTheme.dp(getContext(), 22), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastX = downX = event.getX();
            lastY = downY = event.getY();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !scaleDetector.isInProgress()) {
            float dx = event.getX() - lastX;
            float dy = event.getY() - lastY;
            yaw += dx * 0.31f;
            pitch = clamp(pitch - dy * 0.24f, -50f, 50f);
            lastX = event.getX();
            lastY = event.getY();
            invalidate();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (Math.hypot(event.getX() - downX, event.getY() - downY) < AppTheme.dp(getContext(), 12)) {
                selectNearest(event.getX(), event.getY());
            }
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void selectNearest(float x, float y) {
        Node nearest = null;
        float best = AppTheme.dp(getContext(), 34);
        for (Node node : nodes) {
            float distance = (float) Math.hypot(node.x - x, node.y - y);
            if (distance < best) {
                best = distance;
                nearest = node;
            }
        }
        if (nearest == null) return;
        selected = nearest.index;
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        invalidate();
        notifySelected();
    }

    private void notifySelected() {
        if (listener != null && selected >= 0 && selected < items.size()) {
            listener.onSelected(selected, items.get(selected));
        }
    }

    private Point projectForIndex(int index, float y) {
        int count = Math.max(1, items.size());
        float progress = index / (float) Math.max(1, count - 1);
        double angle = index * 1.17 + progress * Math.PI;
        float radius = 0.38f + progress * 0.55f;
        return project((float) Math.cos(angle) * radius, y, (float) Math.sin(angle) * radius);
    }

    private Point project(float x, float y, float z) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        float rotatedX = (float) (x * Math.cos(yawRadians) - z * Math.sin(yawRadians));
        float rotatedZ = (float) (x * Math.sin(yawRadians) + z * Math.cos(yawRadians));
        float rotatedY = (float) (y * Math.cos(pitchRadians) - rotatedZ * Math.sin(pitchRadians));
        float depth = (float) (y * Math.sin(pitchRadians) + rotatedZ * Math.cos(pitchRadians));
        float perspective = 1f / (1.45f - depth * 0.22f);
        float scale = Math.min(getWidth(), getHeight()) * 0.39f * zoom * perspective;
        return new Point(
            getWidth() * 0.5f + rotatedX * scale,
            getHeight() * 0.52f - rotatedY * scale,
            depth
        );
    }

    private int colorFor(String category) {
        if (category.contains("响应") || category.contains("分析")) return AppTheme.AMBER;
        if (category.contains("异常") || category.contains("维护")) return AppTheme.RED;
        if (category.contains("巡检") || category.contains("定位")) return AppTheme.TEAL;
        return AppTheme.PAPER_2;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Point {
        final float x;
        final float y;
        final float depth;

        Point(float x, float y, float depth) {
            this.x = x;
            this.y = y;
            this.depth = depth;
        }
    }

    private static final class Node {
        final int index;
        final float x;
        final float y;
        final float depth;
        final int color;

        Node(int index, float x, float y, float depth, int color) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.depth = depth;
            this.color = color;
        }
    }
}
