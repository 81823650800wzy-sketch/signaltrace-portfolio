package com.signaltrace.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

final class GeometricNavBar extends View {
    interface Listener {
        void onSelected(int index);
    }

    private static final String[] LABELS = {"我的", "现场", "数据", "轨迹", "更新"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] lift = {1f, 0f, 0f, 0f, 0f};
    private Listener listener;
    private int selected = 0;
    private int target = 0;
    private float flip = 1f;

    GeometricNavBar(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        paint.setStrokeCap(Paint.Cap.SQUARE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(AppTheme.dp(context, 1));
        linePaint.setStrokeCap(Paint.Cap.SQUARE);
        setContentDescription("主导航");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void select(int index, boolean animate) {
        if (index < 0 || index >= LABELS.length || index == target) return;
        int previous = selected;
        target = index;
        if (!animate) {
            selected = index;
            for (int i = 0; i < lift.length; i++) lift[i] = i == index ? 1f : 0f;
            flip = 1f;
            invalidate();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(420);
        animator.setInterpolator(AppTheme.EASE_IN_OUT);
        animator.addUpdateListener(value -> {
            float progress = (float) value.getAnimatedValue();
            lift[previous] = 1f - progress;
            lift[index] = progress;
            flip = progress;
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                selected = index;
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        canvas.drawColor(AppTheme.INK);

        paint.setColor(AppTheme.withAlpha(AppTheme.WHITE, 18));
        canvas.drawRect(0, 0, width, AppTheme.dp(getContext(), 1), paint);

        float slot = width / LABELS.length;
        for (int index = 0; index < LABELS.length; index++) {
            float centerX = slot * (index + 0.5f);
            float amount = lift[index];
            float baseTop = AppTheme.dp(getContext(), 24);
            float top = baseTop - AppTheme.dp(getContext(), 8) * amount;
            float bottom = height - AppTheme.dp(getContext(), 7);
            float half = slot * (0.38f + 0.08f * amount);
            float skew = AppTheme.dp(getContext(), 8) * (index % 2 == 0 ? 1 : -1);

            float phase = index == target ? flip : (index == selected ? 1f - flip : 1f);
            float squash = 0.18f + 0.82f * Math.abs((float) Math.cos(phase * Math.PI));
            canvas.save();
            canvas.scale(squash, 1f, centerX, (top + bottom) * 0.5f);

            path.reset();
            if (amount > 0.5f) {
                path.moveTo(centerX - half + skew, top);
                path.lineTo(centerX + half, top);
                path.lineTo(centerX + half - skew, bottom);
                path.lineTo(centerX - half, bottom);
            } else {
                path.moveTo(centerX - half, top);
                path.lineTo(centerX + half - skew, top);
                path.lineTo(centerX + half, bottom);
                path.lineTo(centerX - half + skew, bottom);
            }
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(blend(AppTheme.INK_2, AppTheme.AMBER, amount));
            canvas.drawPath(path, paint);
            linePaint.setColor(blend(AppTheme.LINE, AppTheme.AMBER, amount));
            canvas.drawPath(path, linePaint);
            canvas.restore();

            int iconColor = amount > 0.48f ? AppTheme.INK : AppTheme.MUTED;
            drawIcon(canvas, index, centerX, top + AppTheme.dp(getContext(), 22), iconColor, amount);

            paint.setTextSize(AppTheme.dp(getContext(), 10));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(blend(AppTheme.MUTED, AppTheme.WHITE, amount));
            paint.setAlpha(Math.round(255 * (0.72f + amount * 0.28f)));
            float labelY = bottom - AppTheme.dp(getContext(), 8) - amount * AppTheme.dp(getContext(), 5);
            canvas.drawText(LABELS[index], centerX, labelY, paint);
            paint.setAlpha(255);

            if (amount > 0.02f) {
                paint.setColor(AppTheme.TEAL);
                float markerY = AppTheme.dp(getContext(), 7) - amount * AppTheme.dp(getContext(), 3);
                canvas.drawRect(
                    centerX - AppTheme.dp(getContext(), 11) * amount,
                    markerY,
                    centerX + AppTheme.dp(getContext(), 11) * amount,
                    markerY + AppTheme.dp(getContext(), 2),
                    paint
                );
            }
        }
    }

    private void drawIcon(Canvas canvas, int index, float x, float y, int color, float amount) {
        float unit = AppTheme.dp(getContext(), 1);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(unit * 1.7f);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        path.reset();
        if (index == 0) {
            path.moveTo(x - 7 * unit, y + 2 * unit);
            path.lineTo(x, y - 5 * unit);
            path.lineTo(x + 7 * unit, y + 2 * unit);
            path.lineTo(x + 4 * unit, y + 7 * unit);
            path.lineTo(x - 4 * unit, y + 7 * unit);
            path.close();
        } else if (index == 1) {
            canvas.drawRect(x - 7 * unit, y - 6 * unit, x + 7 * unit, y + 7 * unit, paint);
            canvas.drawLine(x - 3 * unit, y - 2 * unit, x + 4 * unit, y - 2 * unit, paint);
            canvas.drawLine(x - 3 * unit, y + 2 * unit, x + 2 * unit, y + 2 * unit, paint);
        } else if (index == 2) {
            path.moveTo(x - 8 * unit, y + 4 * unit);
            path.lineTo(x - 3 * unit, y - 1 * unit);
            path.lineTo(x + 1 * unit, y + 2 * unit);
            path.lineTo(x + 8 * unit, y - 6 * unit);
        } else if (index == 3) {
            canvas.drawCircle(x - 6 * unit, y + 4 * unit, 2 * unit, paint);
            canvas.drawCircle(x, y - 5 * unit, 2 * unit, paint);
            canvas.drawCircle(x + 7 * unit, y + 2 * unit, 2 * unit, paint);
            path.moveTo(x - 4 * unit, y + 2 * unit);
            path.lineTo(x - 1 * unit, y - 3 * unit);
            path.moveTo(x + 2 * unit, y - 4 * unit);
            path.lineTo(x + 5 * unit, y);
        } else {
            RectF arc = new RectF(x - 7 * unit, y - 7 * unit, x + 7 * unit, y + 7 * unit);
            canvas.drawArc(arc, -60, 280, false, paint);
            path.moveTo(x + 5 * unit, y - 7 * unit);
            path.lineTo(x + 8 * unit, y - 2 * unit);
            path.lineTo(x + 2 * unit, y - 2 * unit);
        }
        canvas.save();
        canvas.scale(1f + amount * 0.08f, 1f + amount * 0.08f, x, y);
        canvas.drawPath(path, paint);
        canvas.restore();
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        int index = Math.max(0, Math.min(LABELS.length - 1, (int) (event.getX() / (getWidth() / LABELS.length))));
        if (index != target) {
            select(index, true);
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            if (listener != null) listener.onSelected(index);
        }
        performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static int blend(int from, int to, float amount) {
        float p = Math.max(0f, Math.min(1f, amount));
        return android.graphics.Color.rgb(
            Math.round(android.graphics.Color.red(from) + (android.graphics.Color.red(to) - android.graphics.Color.red(from)) * p),
            Math.round(android.graphics.Color.green(from) + (android.graphics.Color.green(to) - android.graphics.Color.green(from)) * p),
            Math.round(android.graphics.Color.blue(from) + (android.graphics.Color.blue(to) - android.graphics.Color.blue(from)) * p)
        );
    }
}
