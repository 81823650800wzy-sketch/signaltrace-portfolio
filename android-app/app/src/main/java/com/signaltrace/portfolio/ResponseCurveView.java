package com.signaltrace.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

final class ResponseCurveView extends View {
    interface CursorListener {
        void onCursor(double seconds, double value);
    }

    private static final double[] T95 = {0, 5, 10, 15, 20, 25, 30, 33, 35, 50, 60, 70, 80, 90, 100, 115, 140, 150, 170, 180, 183, 194, 203, 215, 227, 244};
    private static final double[] V95 = {93.20, 93.45, 93.63, 93.79, 93.91, 93.99, 94.06, 94.09, 94.11, 94.20, 94.24, 94.27, 94.30, 94.32, 94.34, 94.38, 94.43, 94.45, 94.48, 94.49, 94.50, 94.51, 94.52, 94.53, 94.54, 94.55};
    private static final double[] T90 = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 52, 62, 69, 82, 102, 117, 132, 160, 189, 282};
    private static final double[] V90 = {90.42, 90.36, 90.32, 90.28, 90.24, 90.21, 90.19, 90.17, 90.15, 90.13, 90.12, 90.11, 90.09, 90.08, 90.06, 90.04, 90.03, 90.02, 90.01, 90.00, 89.99};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final ValueAnimator reveal = ValueAnimator.ofFloat(0f, 1f);
    private boolean ninetyFive = true;
    private float revealProgress;
    private float cursor = -1f;
    private CursorListener listener;

    ResponseCurveView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL));
        reveal.setDuration(900);
        reveal.setInterpolator(AppTheme.EASE_OUT);
        reveal.addUpdateListener(animation -> {
            revealProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        reveal.start();
        setContentDescription("氢气纯度仪响应曲线，可左右拖动查看读数");
    }

    void setDataset(boolean useNinetyFive) {
        if (ninetyFive == useNinetyFive) return;
        ninetyFive = useNinetyFive;
        cursor = -1f;
        reveal.cancel();
        reveal.start();
    }

    void setCursorListener(CursorListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(AppTheme.INK_2);
        float left = AppTheme.dp(getContext(), 34);
        float top = AppTheme.dp(getContext(), 28);
        float right = getWidth() - AppTheme.dp(getContext(), 14);
        float bottom = getHeight() - AppTheme.dp(getContext(), 30);
        double[] times = ninetyFive ? T95 : T90;
        double[] values = ninetyFive ? V95 : V90;
        double minY = ninetyFive ? 93.1 : 89.9;
        double maxY = ninetyFive ? 94.85 : 90.5;
        double maxX = ninetyFive ? 250 : 285;

        paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 30));
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, paint);
        }
        for (int i = 0; i <= 5; i++) {
            float x = left + (right - left) * i / 5f;
            canvas.drawLine(x, top, x, bottom, paint);
        }

        paint.setTextSize(AppTheme.dp(getContext(), 9));
        paint.setColor(AppTheme.MUTED);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(String.format(java.util.Locale.US, "%.2f%%", maxY), AppTheme.dp(getContext(), 4), top + AppTheme.dp(getContext(), 3), paint);
        canvas.drawText(String.format(java.util.Locale.US, "%.2f%%", minY), AppTheme.dp(getContext(), 4), bottom, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(java.util.Locale.US, "%.0fs", maxX), right, getHeight() - AppTheme.dp(getContext(), 10), paint);

        path.reset();
        int visibleCount = Math.max(2, Math.round((values.length - 1) * revealProgress) + 1);
        for (int i = 0; i < visibleCount; i++) {
            float x = map(times[i], 0, maxX, left, right);
            float y = map(values[i], minY, maxY, bottom, top);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 2.4f));
        paint.setColor(ninetyFive ? AppTheme.AMBER : AppTheme.TEAL);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < visibleCount; i++) {
            float x = map(times[i], 0, maxX, left, right);
            float y = map(values[i], minY, maxY, bottom, top);
            canvas.drawRect(x - 1.8f, y - 1.8f, x + 1.8f, y + 1.8f, paint);
        }

        if (cursor >= 0f) {
            float x = Math.max(left, Math.min(right, cursor));
            paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 150));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
            canvas.drawLine(x, top, x, bottom, paint);
            double seconds = (x - left) / Math.max(1, right - left) * maxX;
            double value = interpolate(seconds, times, values);
            float y = map(value, minY, maxY, bottom, top);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(AppTheme.WHITE);
            canvas.drawCircle(x, y, AppTheme.dp(getContext(), 4), paint);
            paint.setColor(AppTheme.INK);
            canvas.drawCircle(x, y, AppTheme.dp(getContext(), 2), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            cursor = event.getX();
            notifyCursor();
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
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

    private void notifyCursor() {
        if (listener == null || cursor < 0f) return;
        float left = AppTheme.dp(getContext(), 34);
        float right = getWidth() - AppTheme.dp(getContext(), 14);
        double[] times = ninetyFive ? T95 : T90;
        double[] values = ninetyFive ? V95 : V90;
        double maxX = ninetyFive ? 250 : 285;
        double seconds = (Math.max(left, Math.min(right, cursor)) - left) / Math.max(1, right - left) * maxX;
        listener.onCursor(seconds, interpolate(seconds, times, values));
    }

    private static double interpolate(double time, double[] times, double[] values) {
        if (time <= times[0]) return values[0];
        for (int i = 1; i < times.length; i++) {
            if (time <= times[i]) {
                double p = (time - times[i - 1]) / (times[i] - times[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * p;
            }
        }
        return values[values.length - 1];
    }

    private static float map(double value, double fromMin, double fromMax, float toMin, float toMax) {
        return (float) (toMin + (value - fromMin) / (fromMax - fromMin) * (toMax - toMin));
    }
}
