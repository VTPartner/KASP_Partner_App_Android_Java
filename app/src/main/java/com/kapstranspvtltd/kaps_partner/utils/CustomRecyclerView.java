package com.kapstranspvtltd.kaps_partner.utils;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomRecyclerView extends RecyclerView {

    private static final int DEFAULT_SCROLL_DELAY = 3000; // Time delay between each scroll (in milliseconds)
    private int mScrollDelay = DEFAULT_SCROLL_DELAY;
    private Handler mHandler;
    private Runnable mRunnable;
    private boolean mAutoRotationEnabled = false;

    public CustomRecyclerView(Context context) {
        super(context);
        init();
    }

    public CustomRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mHandler = new Handler();
        mRunnable = () -> {
            if (mAutoRotationEnabled) {
                smoothScrollToNextPosition();
                startAutoRotation();
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAutoRotation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoRotation();
    }

    public void setScrollDelay(int scrollDelay) {
        this.mScrollDelay = scrollDelay;
    }

    public void startAutoRotation() {
        mAutoRotationEnabled = true;
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, mScrollDelay);
    }

    public void stopAutoRotation() {
        mAutoRotationEnabled = false;
        mHandler.removeCallbacks(mRunnable);
    }

    private void smoothScrollToNextPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
        int currentPosition = layoutManager.findFirstCompletelyVisibleItemPosition();

        if (currentPosition != RecyclerView.NO_POSITION) {
            int nextPosition = (currentPosition + 1) % layoutManager.getItemCount();
            smoothScrollToPosition(nextPosition);
        }
    }
}