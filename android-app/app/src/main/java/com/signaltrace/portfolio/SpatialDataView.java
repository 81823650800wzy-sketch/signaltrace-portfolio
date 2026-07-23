package com.signaltrace.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SpatialDataView extends View {
    interface Listener {
        void onPointSelected(DataPoint point);
    }

    static final class DataPoint {
        final int dataset;
        final int index;
        final double seconds;
        final double value;
        final double rate;

        DataPoint(int dataset, int index, double seconds, double value, double rate) {
            this.dataset = dataset;
            this.index = index;
            this.seconds = seconds;
            this.value = value;
            this.rate = rate;
        }
    }

    private static final double[] T95 = {0, 5, 10, 15, 20, 25, 30, 35, 50, 70, 90, 115, 140, 170, 194, 215, 244};
    private static final double[] V95 = {93.20, 93.45, 93.63, 93.79, 93.91, 93.99, 94.06, 94.11, 94.20, 94.27, 94.32, 94.38, 94.43, 94.48, 94.51, 94.53, 94.55};
    private static final double[] T90 = {0, 5, 10, 15, 20, 25, 35, 45, 52, 69, 82, 102, 132, 160, 189, 282};
    private static final double[] V90 = {90.42, 90.36, 90.32, 90.28, 90.24, 90.21, 90.17, 90.13, 90.11, 90.08, 90.06, 90.04, 90.02, 90.01, 90.00, 89.99};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final List<ProjectedPoint> projected = new ArrayList<>();
    private final ScaleGestureDetector scaleDetector;
    private final ValueAnimator ambient = ValueAnimator.ofFloat(0f, 1f);
    private Listener listener;
    private float yaw = -24f;
    private float pitch = 17f;
    private float zoom = 1f;
    private float lastX;
    private float lastY;
    private float downX;
    private float downY;
    private float ambientPhase;
    private int selectedDataset = 0;
    private int selectedIndex = 0;
    private VelocityTracker velocityTracker;

    SpatialDataView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoom = clamp(zoom * detector.getScaleFactor(), 0.72f, 1.65f);
                invalidate();
                return true;
            }
        });
        ambient.setDuration(5400);
        ambient.setRepeatCount(ValueAnimator.INFINITE);
        ambient.setInterpolator(null);
        ambient.addUpdateListener(animation -> {
            ambientPhase = (float) animation.getAnimatedValue();
            invalidate();
        });
        ambient.start();
        setContentDescription("三维响应数据");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void focusDataset(int dataset) {
        selectedDataset = dataset == 0 ? 0 : 1;
        selectedIndex = 0;
        invalidate();
        notifySelected();
    }

    @Override
    protected void onDetachedFromWindow() {
        ambient.cancel();
        recycleVelocity();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(AppTheme.INK);
        projected.clear();
        drawSpatialGrid(canvas);
        drawCurve(canvas, 1, T90, V90, AppTheme.TEAL);
        drawCurve(canvas, 0, T95, V95, AppTheme.AMBER);
        drawAxisLabels(canvas);
        drawSelection(canvas);
    }

    private void drawSpatialGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 28));
        for (int z = -3; z <= 3; z++) {
            Point a = project(-1.05f, -0.78f, z / 3f);
            Point b = project(1.05f, -0.78f, z / 3f);
            canvas.drawLine(a.x, a.y, b.x, b.y, paint);
        }
        for (int x = -5; x <= 5; x++) {
            Point a = project(x / 5f, -0.78f, -1f);
            Point b = project(x / 5f, -0.78f, 1f);
            canvas.drawLine(a.x, a.y, b.x, b.y, paint);
        }
        paint.setColor(AppTheme.withAlpha(AppTheme.RED, 60));
        Point zeroA = project(-1.05f, 0f, 0f);
        Point zeroB = project(1.05f, 0f, 0f);
        canvas.drawLine(zeroA.x, zeroA.y, zeroB.x, zeroB.y, paint);
    }

    private void drawCurve(Canvas canvas, int dataset, double[] times, double[] values, int color) {
        List<ProjectedPoint> points = new ArrayList<>();
        double min = dataset == 0 ? 93.2 : 89.99;
        double max = dataset == 0 ? 94.75 : 90.42;
        double maxTime = dataset == 0 ? 244 : 282;
        float lane = dataset == 0 ? -0.42f : 0.42f;
        for (int index = 0; index < times.length; index++) {
            float x = (float) (times[index] / maxTime * 2 - 1);
            float y = (float) ((values[index] - min) / Math.max(0.001, max - min) * 1.28 - 0.64);
            Point screen = project(x, y, lane);
            double rate = index == 0 ? 0 : (values[index] - values[index - 1]) / (times[index] - times[index - 1]);
            points.add(new ProjectedPoint(dataset, index, times[index], values[index], rate, screen.x, screen.y, screen.depth));
        }

        path.reset();
        for (int index = 0; index < points.size(); index++) {
            ProjectedPoint point = points.get(index);
            if (index == 0) path.moveTo(point.x, point.y);
            else path.lineTo(point.x, point.y);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), dataset == selectedDataset ? 3f : 1.7f));
        paint.setColor(AppTheme.withAlpha(color, dataset == selectedDataset ? 255 : 145));
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);

        points.sort(Comparator.comparingDouble(point -> -point.depth));
        paint.setStyle(Paint.Style.FILL);
        for (ProjectedPoint point : points) {
            float radius = AppTheme.dp(getContext(), point.dataset == selectedDataset ? 3.2f : 2.3f);
            paint.setColor(AppTheme.withAlpha(color, point.dataset == selectedDataset ? 240 : 145));
            canvas.drawCircle(point.x, point.y, radius, paint);
            projected.add(point);
        }
    }

    private void drawSelection(Canvas canvas) {
        ProjectedPoint selected = null;
        for (ProjectedPoint point : projected) {
            if (point.dataset == selectedDataset && point.index == selectedIndex) selected = point;
        }
        if (selected == null) return;
        float pulse = 1f + (float) Math.sin(ambientPhase * Math.PI * 2) * 0.18f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 1.4f));
        paint.setColor(AppTheme.WHITE);
        canvas.drawCircle(selected.x, selected.y, AppTheme.dp(getContext(), 8) * pulse, paint);
        Point floor = project(
            selected.dataset == 0 ? (float) (selected.seconds / 244 * 2 - 1) : (float) (selected.seconds / 282 * 2 - 1),
            -0.78f,
            selected.dataset == 0 ? -0.42f : 0.42f
        );
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 80));
        canvas.drawLine(selected.x, selected.y, floor.x, floor.y, paint);
    }

    private void drawAxisLabels(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(AppTheme.dp(getContext(), 10));
        paint.setColor(AppTheme.MUTED);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("时间", AppTheme.dp(getContext(), 12), getHeight() - AppTheme.dp(getContext(), 12), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(AppTheme.AMBER);
        canvas.drawText("95% 上升", getWidth() - AppTheme.dp(getContext(), 12), AppTheme.dp(getContext(), 21), paint);
        paint.setColor(AppTheme.TEAL);
        canvas.drawText("90% 下降", getWidth() - AppTheme.dp(getContext(), 12), AppTheme.dp(getContext(), 37), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);
        trackVelocity(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastX = downX = event.getX();
            lastY = downY = event.getY();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !scaleDetector.isInProgress()) {
            float dx = event.getX() - lastX;
            float dy = event.getY() - lastY;
            yaw += dx * 0.32f;
            pitch = clamp(pitch - dy * 0.24f, -52f, 52f);
            lastX = event.getX();
            lastY = event.getY();
            invalidate();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float distance = Math.abs(event.getX() - downX) + Math.abs(event.getY() - downY);
            if (distance < AppTheme.dp(getContext(), 12)) selectNearest(event.getX(), event.getY());
            startInertia();
            recycleVelocity();
            performClick();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) recycleVelocity();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void selectNearest(float x, float y) {
        ProjectedPoint nearest = null;
        float best = AppTheme.dp(getContext(), 30);
        for (ProjectedPoint point : projected) {
            float distance = (float) Math.hypot(point.x - x, point.y - y);
            if (distance < best) {
                best = distance;
                nearest = point;
            }
        }
        if (nearest == null) return;
        selectedDataset = nearest.dataset;
        selectedIndex = nearest.index;
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        invalidate();
        notifySelected();
    }

    private void notifySelected() {
        double[] times = selectedDataset == 0 ? T95 : T90;
        double[] values = selectedDataset == 0 ? V95 : V90;
        int index = Math.max(0, Math.min(selectedIndex, times.length - 1));
        double rate = index == 0 ? 0 : (values[index] - values[index - 1]) / (times[index] - times[index - 1]);
        if (listener != null) listener.onPointSelected(new DataPoint(selectedDataset, index, times[index], values[index], rate));
    }

    private Point project(float x, float y, float z) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        float rotatedX = (float) (x * Math.cos(yawRadians) - z * Math.sin(yawRadians));
        float rotatedZ = (float) (x * Math.sin(yawRadians) + z * Math.cos(yawRadians));
        float rotatedY = (float) (y * Math.cos(pitchRadians) - rotatedZ * Math.sin(pitchRadians));
        float depth = (float) (y * Math.sin(pitchRadians) + rotatedZ * Math.cos(pitchRadians));
        float perspective = 1f / (1.42f - depth * 0.22f);
        float scale = Math.min(getWidth(), getHeight()) * 0.34f * zoom * perspective;
        return new Point(
            getWidth() * 0.5f + rotatedX * scale,
            getHeight() * 0.53f - rotatedY * scale,
            depth
        );
    }

    private void trackVelocity(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            recycleVelocity();
            velocityTracker = VelocityTracker.obtain();
        }
        if (velocityTracker != null) velocityTracker.addMovement(event);
    }

    private void startInertia() {
        if (velocityTracker == null) return;
        velocityTracker.computeCurrentVelocity(1000);
        final float vx = velocityTracker.getXVelocity() * 0.012f;
        final float vy = velocityTracker.getYVelocity() * 0.008f;
        ValueAnimator inertia = ValueAnimator.ofFloat(0f, 1f);
        inertia.setDuration(720);
        inertia.setInterpolator(AppTheme.EASE_OUT);
        final float startYaw = yaw;
        final float startPitch = pitch;
        inertia.addUpdateListener(animation -> {
            float p = (float) animation.getAnimatedValue();
            yaw = startYaw + vx * p;
            pitch = clamp(startPitch - vy * p, -52f, 52f);
            invalidate();
        });
        inertia.start();
    }

    private void recycleVelocity() {
        if (velocityTracker != null) velocityTracker.recycle();
        velocityTracker = null;
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

    private static final class ProjectedPoint {
        final int dataset;
        final int index;
        final double seconds;
        final double value;
        final double rate;
        final float x;
        final float y;
        final float depth;

        ProjectedPoint(int dataset, int index, double seconds, double value, double rate, float x, float y, float depth) {
            this.dataset = dataset;
            this.index = index;
            this.seconds = seconds;
            this.value = value;
            this.rate = rate;
            this.x = x;
            this.y = y;
            this.depth = depth;
        }
    }
}
