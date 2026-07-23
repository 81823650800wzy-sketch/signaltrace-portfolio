package com.signaltrace.portfolio;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

final class SwipePageHost extends FrameLayout {
    interface Listener {
        void onSwipe(int direction);
    }

    private final int touchSlop;
    private final int minimumVelocity;
    private Listener listener;
    private float downX;
    private float downY;
    private boolean dragging;
    private VelocityTracker velocityTracker;

    SwipePageHost(Context context) {
        super(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = configuration.getScaledMinimumFlingVelocity() * 3;
        setClipChildren(false);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        track(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            dragging = false;
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (Math.abs(dx) > touchSlop * 1.6f && Math.abs(dx) > Math.abs(dy) * 1.25f) {
                dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        track(event);
        if (!dragging && event.getActionMasked() != MotionEvent.ACTION_DOWN) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            View current = getChildCount() == 0 ? null : getChildAt(getChildCount() - 1);
            if (current != null) {
                float dx = event.getX() - downX;
                current.setTranslationX(dx * 0.34f);
                current.setRotationY(-dx / Math.max(1f, getWidth()) * 7f);
                current.setAlpha(Math.max(0.76f, 1f - Math.abs(dx) / Math.max(1f, getWidth()) * 0.25f));
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP
            || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            float dx = event.getX() - downX;
            float velocity = velocityTracker == null ? 0f : velocityTracker.getXVelocity();
            boolean commit = Math.abs(dx) > getWidth() * 0.18f || Math.abs(velocity) > minimumVelocity;
            int direction = dx < 0 ? 1 : -1;
            resetCurrent(commit ? 120 : 300);
            if (commit && listener != null) {
                performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                post(() -> listener.onSwipe(direction));
            }
            performClick();
            recycleVelocityTracker();
            dragging = false;
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void resetCurrent(long duration) {
        if (getChildCount() == 0) return;
        View current = getChildAt(getChildCount() - 1);
        current.animate()
            .translationX(0)
            .rotationY(0)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(AppTheme.EASE_OUT)
            .start();
    }

    private void track(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            recycleVelocityTracker();
            velocityTracker = VelocityTracker.obtain();
        }
        if (velocityTracker != null) {
            velocityTracker.addMovement(event);
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                velocityTracker.computeCurrentVelocity(1000);
            }
        }
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) velocityTracker.recycle();
        velocityTracker = null;
    }
}
