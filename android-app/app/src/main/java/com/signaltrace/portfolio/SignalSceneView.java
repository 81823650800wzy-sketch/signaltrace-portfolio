package com.signaltrace.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

final class SignalSceneView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final ValueAnimator clock = ValueAnimator.ofFloat(0f, 1f);
    private LinearGradient prismShader;
    private float phase;
    private float tiltX;
    private float tiltY;

    SignalSceneView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        clock.setDuration(7000);
        clock.setRepeatCount(ValueAnimator.INFINITE);
        clock.setInterpolator(null);
        clock.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
        clock.start();
        setContentDescription("动态信号轨迹与工业棱镜");
    }

    @Override
    protected void onDetachedFromWindow() {
        clock.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float cx = width * 0.5f;
        float prismY = height * 0.47f;
        float size = Math.min(width, height) * 0.24f;
        prismShader = new LinearGradient(
            cx - size,
            prismY - size,
            cx + size,
            prismY + size,
            new int[]{AppTheme.INK_3, AppTheme.PAPER, AppTheme.INK_2},
            null,
            Shader.TileMode.CLAMP
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        canvas.drawColor(AppTheme.INK);

        float cx = width * 0.5f + tiltX * AppTheme.dp(getContext(), 10);
        float horizon = height * 0.37f + tiltY * AppTheme.dp(getContext(), 6);
        float bottom = height * 1.05f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
        paint.setColor(AppTheme.withAlpha(AppTheme.TEAL, 45));
        for (int index = -6; index <= 6; index++) {
            float bottomX = cx + index * width * 0.13f;
            canvas.drawLine(cx + index * width * 0.015f, horizon, bottomX, bottom, paint);
        }
        for (int index = 0; index < 8; index++) {
            float p = index / 8f;
            float eased = p * p;
            float y = horizon + eased * (bottom - horizon);
            canvas.drawLine(0, y, width, y, paint);
        }

        float prismY = height * 0.47f + tiltY * AppTheme.dp(getContext(), 9);
        float size = Math.min(width, height) * 0.24f;
        path.reset();
        path.moveTo(cx, prismY - size);
        path.lineTo(cx + size * 0.86f, prismY + size * 0.46f);
        path.lineTo(cx, prismY + size);
        path.lineTo(cx - size * 0.86f, prismY + size * 0.46f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(prismShader);
        canvas.drawPath(path, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 2));
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 200));
        canvas.drawPath(path, paint);
        canvas.drawLine(cx, prismY - size, cx, prismY + size, paint);
        canvas.drawLine(cx - size * 0.86f, prismY + size * 0.46f, cx, prismY + size * 0.12f, paint);
        canvas.drawLine(cx + size * 0.86f, prismY + size * 0.46f, cx, prismY + size * 0.12f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 4));
        paint.setColor(AppTheme.AMBER);
        path.reset();
        float signalY = prismY + size * 0.28f;
        path.moveTo(-20, signalY + size * 0.32f);
        path.lineTo(cx - size * 0.62f, signalY + size * 0.08f);
        path.lineTo(cx - size * 0.12f, signalY - size * 0.08f);
        path.lineTo(cx + size * 0.2f, signalY + size * 0.11f);
        path.lineTo(width + 20, signalY - size * 0.72f);
        canvas.drawPath(path, paint);

        paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
        paint.setColor(AppTheme.withAlpha(AppTheme.TEAL, 170));
        float travel = (phase * 1.2f) % 1f;
        float pulseX = width * travel;
        float pulseY = signalY + size * (0.30f - travel * 0.92f);
        canvas.drawCircle(pulseX, pulseY, AppTheme.dp(getContext(), 4), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AppTheme.TEAL);
        canvas.drawCircle(pulseX, pulseY, AppTheme.dp(getContext(), 2), paint);

        paint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        paint.setTextSize(AppTheme.dp(getContext(), 10));
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 170));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("SIGNAL / EVIDENCE / GROWTH", AppTheme.dp(getContext(), 16), AppTheme.dp(getContext(), 24), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(AppTheme.TEAL);
        canvas.drawText(String.format(java.util.Locale.US, "%03d", Math.round(phase * 999)), width - AppTheme.dp(getContext(), 16), AppTheme.dp(getContext(), 24), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            tiltX = (event.getX() / Math.max(1f, getWidth()) - 0.5f) * 2f;
            tiltY = (event.getY() / Math.max(1f, getHeight()) - 0.5f) * 2f;
            animate().rotationY(tiltX * 3f).rotationX(-tiltY * 2f).setDuration(80).start();
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            tiltX = 0f;
            tiltY = 0f;
            animate().rotationY(0f).rotationX(0f).setDuration(520).setInterpolator(AppTheme.EASE_OUT).start();
            invalidate();
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
}
