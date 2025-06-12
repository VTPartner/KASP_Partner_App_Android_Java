package com.kapstranspvtltd.kaps_partner.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.kapstranspvtltd.kaps_partner.R;

public class SwipeButton extends FrameLayout {
    private ImageView slideImageView;
    private TextView slideTextView;
    private float initialX;
    private boolean isSlided = false;
    private int viewWidth;
    private float buttonWidth;
    private static final float SLIDE_THRESHOLD = 0.8f;
    private static final long ANIMATION_DURATION = 300;
    private float currentProgress = 0f;

    public interface OnSlideCompleteListener {
        void onSlideComplete();
    }

    public interface OnSlideProgressListener {
        void onSlideProgress(float progress);
        void onSlideStart();
        void onSlideReset();
    }

    private OnSlideCompleteListener completeListener;
    private OnSlideProgressListener progressListener;

    public SwipeButton(Context context) {
        super(context);
        init(context);
    }

    public SwipeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.swipe_button_layout, this, true);
        slideImageView = findViewById(R.id.slide_image);
        slideTextView = findViewById(R.id.slide_text);

        post(() -> {
            buttonWidth = slideImageView.getWidth();
            viewWidth = getWidth();
        });

        setupTouchListener();
    }

    private void setupTouchListener() {
        slideImageView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getX();
                    slideImageView.setPressed(true);
                    if (progressListener != null) {
                        progressListener.onSlideStart();
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!isSlided) {
                        float moved = event.getRawX() - getLeft() - initialX;
                        float progress = Math.max(0, Math.min(1, moved / (viewWidth - buttonWidth)));
                        updateProgress(progress, false);

                        if (progressListener != null) {
                            progressListener.onSlideProgress(progress);
                        }

                        if (progress > SLIDE_THRESHOLD && !isSlided) {
                            completeSlide();
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    slideImageView.setPressed(false);
                    if (!isSlided) {
                        resetSlide();
                        if (progressListener != null) {
                            progressListener.onSlideReset();
                        }
                    }
                    return true;
            }
            return false;
        });
    }

    private void updateProgress(float progress, boolean animate) {
        if (animate) {
            ValueAnimator progressAnimator = ValueAnimator.ofFloat(currentProgress, progress);
            progressAnimator.setDuration(ANIMATION_DURATION);
            progressAnimator.setInterpolator(progress == 1f ?
                    new AccelerateDecelerateInterpolator() :
                    new OvershootInterpolator());

            progressAnimator.addUpdateListener(animation -> {
                currentProgress = (float) animation.getAnimatedValue();
                updateUI(currentProgress);
            });
            progressAnimator.start();
        } else {
            currentProgress = progress;
            updateUI(progress);
        }
    }

    private void updateUI(float progress) {
        float translationX = progress * (viewWidth - buttonWidth);
        slideImageView.setTranslationX(translationX);

        // Animate text alpha and translation
        float textProgress = Math.min(1, progress * 2);
        slideTextView.setAlpha(1 - textProgress);
        slideTextView.setTranslationX(translationX * 0.25f);

        // Add scale animation to the slider
        float scale = 1 + (progress * 0.2f);
        slideImageView.setScaleX(scale);
        slideImageView.setScaleY(scale);
    }

    private void completeSlide() {
        isSlided = true;
        updateProgress(1f, true);
        postDelayed(() -> {
            if (completeListener != null) {
                completeListener.onSlideComplete();
            }
        }, ANIMATION_DURATION);
    }

    private void resetSlide() {
        isSlided = false;
        updateProgress(0f, true);
    }

    public void reset() {
        isSlided = false;
        resetSlide();
    }

    public void setText(String text) {
        slideTextView.setText(text);
        slideTextView.setAlpha(1f);
        slideTextView.setTranslationX(0f);
    }

    public void setColors(int backgroundColor, int textColor) {
        setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        slideTextView.setTextColor(textColor);
    }

    public void setOnSlideCompleteListener(OnSlideCompleteListener listener) {
        this.completeListener = listener;
    }

    public void setOnSlideProgressListener(OnSlideProgressListener listener) {
        this.progressListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        buttonWidth = slideImageView.getWidth();
    }
}