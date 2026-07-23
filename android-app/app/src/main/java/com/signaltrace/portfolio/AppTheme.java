package com.signaltrace.portfolio;

import android.annotation.SuppressLint;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

final class AppTheme {
    static final int INK = Color.rgb(13, 17, 20);
    static final int INK_2 = Color.rgb(23, 29, 33);
    static final int INK_3 = Color.rgb(38, 45, 49);
    static final int PAPER = Color.rgb(238, 239, 233);
    static final int PAPER_2 = Color.rgb(218, 220, 214);
    static final int MUTED = Color.rgb(143, 151, 149);
    static final int AMBER = Color.rgb(246, 184, 46);
    static final int TEAL = Color.rgb(50, 205, 190);
    static final int RED = Color.rgb(238, 82, 68);
    static final int WHITE = Color.rgb(250, 250, 246);
    static final int LINE = Color.rgb(61, 70, 73);

    static final TimeInterpolator EASE_OUT = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    static final TimeInterpolator EASE_IN_OUT = new PathInterpolator(0.65f, 0f, 0.35f, 1f);

    private AppTheme() {
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context context, String value, float size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLetterSpacing(0f);
        view.setLineSpacing(0f, 1.08f);
        view.setFontFeatureSettings("kern");
        view.setTypeface(Typeface.create(
            bold ? "sans-serif-condensed" : "sans-serif",
            bold ? Typeface.BOLD : Typeface.NORMAL
        ));
        return view;
    }

    static TextView label(Context context, String value, int color) {
        TextView view = text(context, value, 10, color, true);
        view.setAllCaps(false);
        return view;
    }

    static Drawable panel(int fill, int stroke, float cutDp, Context context) {
        return new CutCornerDrawable(fill, stroke, dp(context, cutDp), dp(context, 1));
    }

    static Drawable solid(int fill, float cutDp, Context context) {
        return new CutCornerDrawable(fill, Color.TRANSPARENT, dp(context, cutDp), 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    static void pressable(View view) {
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnTouchListener((target, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                target.animate()
                    .scaleX(0.975f)
                    .scaleY(0.975f)
                    .translationZ(AppTheme.dp(target.getContext(), 2))
                    .setDuration(90)
                    .setInterpolator(EASE_OUT)
                    .start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
                target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0)
                    .setDuration(260)
                    .setInterpolator(EASE_OUT)
                    .start();
            }
            return false;
        });
    }

    static void reveal(View view, int index) {
        view.setAlpha(0f);
        view.setTranslationY(dp(view.getContext(), 22));
        view.setRotationX(5f);
        view.animate()
            .alpha(1f)
            .translationY(0)
            .rotationX(0)
            .setStartDelay(Math.min(index, 8) * 42L)
            .setDuration(520)
            .setInterpolator(EASE_OUT)
            .start();
    }

    static int withAlpha(int color, int alpha) {
        return Color.argb(
            Math.max(0, Math.min(255, alpha)),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        );
    }

    private static final class CutCornerDrawable extends Drawable {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float cut;
        private final float strokeWidth;

        CutCornerDrawable(int fill, int stroke, float cut, float strokeWidth) {
            fillPaint.setColor(fill);
            fillPaint.setStyle(Paint.Style.FILL);
            strokePaint.setColor(stroke);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(strokeWidth);
            strokePaint.setStrokeJoin(Paint.Join.MITER);
            this.cut = cut;
            this.strokeWidth = strokeWidth;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            float inset = strokeWidth * 0.5f;
            float left = bounds.left + inset;
            float top = bounds.top + inset;
            float right = bounds.right - inset;
            float bottom = bounds.bottom - inset;
            float resolvedCut = Math.min(cut, Math.min(right - left, bottom - top) * 0.32f);
            path.reset();
            path.moveTo(left, top);
            path.lineTo(right - resolvedCut, top);
            path.lineTo(right, top + resolvedCut);
            path.lineTo(right, bottom);
            path.lineTo(left + resolvedCut * 0.58f, bottom);
            path.lineTo(left, bottom - resolvedCut * 0.58f);
            path.close();
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawPath(path, fillPaint);
            if (strokeWidth > 0 && strokePaint.getColor() != Color.TRANSPARENT) {
                canvas.drawPath(path, strokePaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            fillPaint.setAlpha(alpha);
            strokePaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            fillPaint.setColorFilter(colorFilter);
            strokePaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
