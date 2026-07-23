package com.signaltrace.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class WishOrbitView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator clock = ValueAnimator.ofFloat(0f, 360f);
    private final RectF orbit = new RectF();
    private int completed;
    private int total = 1;
    private float angle;

    WishOrbitView(Context context) {
        super(context);
        clock.setDuration(9000);
        clock.setRepeatCount(ValueAnimator.INFINITE);
        clock.setInterpolator(null);
        clock.addUpdateListener(animation -> {
            angle = (float) animation.getAnimatedValue();
            invalidate();
        });
        clock.start();
        setContentDescription("人生愿望完成进度");
    }

    void setProgress(int completed, int total) {
        this.completed = Math.max(0, completed);
        this.total = Math.max(1, total);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        clock.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(AppTheme.INK);
        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.5f;
        float radius = Math.min(getWidth(), getHeight()) * 0.31f;
        orbit.set(cx - radius, cy - radius, cx + radius, cy + radius);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 1));
        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 35));
        canvas.drawOval(orbit, paint);
        paint.setStrokeWidth(AppTheme.dp(getContext(), 7));
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setColor(AppTheme.AMBER);
        canvas.drawArc(orbit, -90, 360f * completed / total, false, paint);

        paint.setStyle(Paint.Style.FILL);
        for (int index = 0; index < 3; index++) {
            double radians = Math.toRadians(angle + index * 120);
            float x = cx + (float) Math.cos(radians) * radius;
            float y = cy + (float) Math.sin(radians) * radius;
            paint.setColor(index == 0 ? AppTheme.TEAL : AppTheme.withAlpha(AppTheme.WHITE, 150));
            canvas.save();
            canvas.rotate(angle + index * 120, x, y);
            canvas.drawRect(
                x - AppTheme.dp(getContext(), 4),
                y - AppTheme.dp(getContext(), 4),
                x + AppTheme.dp(getContext(), 4),
                y + AppTheme.dp(getContext(), 4),
                paint
            );
            canvas.restore();
        }

        paint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(AppTheme.dp(getContext(), 31));
        paint.setColor(AppTheme.WHITE);
        canvas.drawText(completed + "/" + total, cx, cy + AppTheme.dp(getContext(), 7), paint);
        paint.setTextSize(AppTheme.dp(getContext(), 9));
        paint.setColor(AppTheme.MUTED);
        canvas.drawText("LIFE LIST / IN MOTION", cx, cy + AppTheme.dp(getContext(), 27), paint);
    }
}
