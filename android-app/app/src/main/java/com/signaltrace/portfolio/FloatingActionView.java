package com.signaltrace.portfolio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

@android.annotation.SuppressLint("ViewConstructor")
final class FloatingActionView extends TextView {
    private final ValueAnimator floatAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
    private final float phase;

    FloatingActionView(Context context, String icon, String label, int fill, int textColor, float phase) {
        super(context);
        this.phase = phase;
        setText(String.format(java.util.Locale.CHINA, "%s  %s", icon, label));
        setTextSize(12);
        setTextColor(textColor);
        setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        setGravity(Gravity.CENTER);
        setMinHeight(AppTheme.dp(context, 48));
        setPadding(
            AppTheme.dp(context, 15),
            AppTheme.dp(context, 10),
            AppTheme.dp(context, 15),
            AppTheme.dp(context, 10)
        );
        setBackground(AppTheme.panel(fill, fill, 8, context));
        AppTheme.pressable(this);
        floatAnimator.setDuration(3200);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setInterpolator(new LinearInterpolator());
        floatAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue() + this.phase;
            setTranslationY((float) Math.sin(value) * AppTheme.dp(getContext(), 2.4f));
            setRotation((float) Math.sin(value * 0.5f) * 0.35f);
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!floatAnimator.isStarted()) floatAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        floatAnimator.cancel();
        setTranslationY(0f);
        setRotation(0f);
        super.onDetachedFromWindow();
    }
}
