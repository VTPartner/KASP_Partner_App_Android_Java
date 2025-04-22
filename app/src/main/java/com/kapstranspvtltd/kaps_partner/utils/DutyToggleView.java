package com.kapstranspvtltd.kaps_partner.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kapstranspvtltd.kaps_partner.R;

public class DutyToggleView extends FrameLayout {
    private float startX;
    private boolean isOnline = false;
    private OnDutyStatusChangeListener listener;
    private TextView statusText;
    private View toggleIndicator;
    private LinearLayout container;

    public interface OnDutyStatusChangeListener {
        void onDutyStatusChanged(boolean isOnline);
    }

    public DutyToggleView(Context context) {
        super(context);
        init(context);
    }

    public DutyToggleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_duty_toggle, this, true);

        container = findViewById(R.id.toggle_container);
        statusText = findViewById(R.id.status_text);
        toggleIndicator = findViewById(R.id.toggle_indicator);

        setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float endX = event.getX();
                    float deltaX = endX - startX;

                    if (Math.abs(deltaX) > 100) { // Minimum swipe distance
                        toggleDutyStatus();
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Optional: Add visual feedback during swipe
                    return true;
            }
            return false;
        });

        updateToggleState();
    }

    private void toggleDutyStatus() {
        isOnline = !isOnline;
        updateToggleState();
        if (listener != null) {
            listener.onDutyStatusChanged(isOnline);
        }
    }

    private void updateToggleState() {
        if (isOnline) {
            container.setBackgroundResource(R.drawable.bg_toggle_online);
            statusText.setText("On Duty");
            statusText.setTextColor(getResources().getColor(R.color.white));
            toggleIndicator.setBackgroundResource(R.drawable.ic_online_indicator_animated);
        } else {
            container.setBackgroundResource(R.drawable.bg_toggle_offline);
            statusText.setText("Off Duty");
            statusText.setTextColor(getResources().getColor(R.color.black));
            toggleIndicator.setBackgroundResource(R.drawable.ic_offline_indicator_animated);
        }

        // Animate the indicator
        Animation pulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse_animation);
        toggleIndicator.startAnimation(pulseAnimation);
    }

    public void setOnDutyStatusChangeListener(OnDutyStatusChangeListener listener) {
        this.listener = listener;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        if (isOnline != online) {
            isOnline = online;
            updateToggleState();
        }
    }
}
