package com.kapstranspvtltd.kaps_partner.goods_driver_activities.helper;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import java.util.Locale;

public class UnloadingTimerManager {
    private static final String TAG = "UnloadingTimerManager";

    // Preference keys
    private static final String PREF_TIMER_START = "unloading_timer_start_";
    private static final String PREF_TIMER_PAUSED = "unloading_timer_paused_";
    private static final String PREF_REMAINING_TIME = "unloading_remaining_time_";
    private static final String PREF_PENALTY_START = "penalty_timer_start_";
    private static final String PREF_ACCUMULATED_PENALTY = "accumulated_penalty_";
    private static final String PREF_LAST_PENALTY_UPDATE = "last_penalty_update_";
    private static final String PREF_IS_PENALTY_RUNNING = "is_penalty_running_";
    private static final String PREF_PENALTY_AMOUNT = "penalty_amount_";
    private static final String PREF_INITIAL_WAITING_MINUTES = "initial_waiting_minutes_";
    private static final String PREF_PENALTY_PER_MINUTE = "penalty_per_minute_";
    private static final String PREF_TIMER_ACTIVE = "timer_active_";

    private final Context context;
    private final Handler handler;
    private final PreferenceManager prefs;
    private CountDownTimer countDownTimer;
    private Runnable penaltyRunnable;
    private TextView timeDisplay;
    private TextView penaltyDisplay;
    private long startTimeMillis;
    private int bookingId;
    private int minimumWaitingMinutes;
    private double penaltyPerMinute;
    private UnloadingTimerListener listener;
    private double accumulatedPenalty;
    private boolean isPenaltyRunning;
    private boolean isTimerActive;

    public interface UnloadingTimerListener {
        void onPenaltyUpdated(double totalPenalty, long penaltyMinutes);
        void onTimerFinished();
    }

    public UnloadingTimerManager(Context context, int bookingId, TextView timeDisplay,
                                 TextView penaltyDisplay, UnloadingTimerListener listener) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.bookingId = bookingId;
        this.timeDisplay = timeDisplay;
        this.penaltyDisplay = penaltyDisplay;
        this.listener = listener;
        this.prefs = new PreferenceManager(context);

        // Load saved state
        this.accumulatedPenalty = prefs.getFloatValue(PREF_ACCUMULATED_PENALTY + bookingId, 0.0f);
        this.isPenaltyRunning = prefs.getBooleanValue(PREF_IS_PENALTY_RUNNING + bookingId, false);
        this.isTimerActive = prefs.getBooleanValue(PREF_TIMER_ACTIVE + bookingId, false);
        this.minimumWaitingMinutes = prefs.getIntValue(PREF_INITIAL_WAITING_MINUTES + bookingId, 0);
        this.penaltyPerMinute = prefs.getFloatValue(PREF_PENALTY_PER_MINUTE + bookingId, 0.0f);
    }

    public void startTimer(int minimumWaitingMinutes, double penaltyPerMinute) {
        Log.d(TAG, "Starting timer for booking " + bookingId);
        this.minimumWaitingMinutes = minimumWaitingMinutes;
        this.penaltyPerMinute = penaltyPerMinute;

        // Save initial parameters
        prefs.saveIntValue(PREF_INITIAL_WAITING_MINUTES + bookingId, minimumWaitingMinutes);
        prefs.saveFloatValue(PREF_PENALTY_PER_MINUTE + bookingId, (float) penaltyPerMinute);
        prefs.saveBooleanValue(PREF_TIMER_ACTIVE + bookingId, true);

        long savedStartTime = prefs.getLongValue(PREF_TIMER_START + bookingId, 0);
        long savedPausedTime = prefs.getLongValue(PREF_TIMER_PAUSED + bookingId, 0);
        long savedRemainingTime = prefs.getLongValue(PREF_REMAINING_TIME + bookingId, 0);
        long lastPenaltyUpdate = prefs.getLongValue(PREF_LAST_PENALTY_UPDATE + bookingId, 0);

        // Handle missed penalty time if applicable
        if (isPenaltyRunning && lastPenaltyUpdate > 0) {
            calculateMissedPenalty(lastPenaltyUpdate);
        }

        long remainingTimeMillis;

        if (savedStartTime > 0 && isTimerActive) {
            if (savedPausedTime > 0) {
                // Resume from paused state
                remainingTimeMillis = savedRemainingTime;
                // Clear paused state
                prefs.removeValue(PREF_TIMER_PAUSED + bookingId);
            } else {
                // Calculate remaining time from saved start
                long elapsedTime = System.currentTimeMillis() - savedStartTime;
                remainingTimeMillis = (minimumWaitingMinutes * 60 * 1000L) - elapsedTime;

                if (remainingTimeMillis <= 0) {
                    Log.d(TAG, "Timer expired, starting penalty");
                    remainingTimeMillis = 0;
                    startPenaltyTimer();
                }
            }
        } else {
            // Fresh start
            remainingTimeMillis = minimumWaitingMinutes * 60 * 1000L;
            startTimeMillis = System.currentTimeMillis();
            prefs.saveLongValue(PREF_TIMER_START + bookingId, startTimeMillis);
        }

        startCountdown(remainingTimeMillis);
    }

    private void calculateMissedPenalty(long lastUpdate) {
        long now = System.currentTimeMillis();
        long missedMillis = now - lastUpdate;

        if (missedMillis > 0) {
            long missedMinutes = missedMillis / (60 * 1000);
            if (missedMinutes > 0) {
                double missedPenalty = missedMinutes * penaltyPerMinute;
                accumulatedPenalty += missedPenalty;

                Log.d(TAG, String.format(Locale.getDefault(),
                        "Calculated missed penalty: ₹%.2f for %d minutes", missedPenalty, missedMinutes));

                saveCurrentPenaltyState(now);
                updatePenaltyDisplay();
            }
        }
    }

    private void saveCurrentPenaltyState(long currentTime) {
        prefs.saveFloatValue(PREF_ACCUMULATED_PENALTY + bookingId, (float) accumulatedPenalty);
        prefs.saveLongValue(PREF_LAST_PENALTY_UPDATE + bookingId, currentTime);
        prefs.saveBooleanValue(PREF_IS_PENALTY_RUNNING + bookingId, isPenaltyRunning);
    }

    private void startPenaltyTimer() {
        isPenaltyRunning = true;
        long penaltyStartTime = System.currentTimeMillis();

        if (prefs.getLongValue(PREF_PENALTY_START + bookingId, 0) == 0) {
            prefs.saveLongValue(PREF_PENALTY_START + bookingId, penaltyStartTime);
        }

        saveCurrentPenaltyState(penaltyStartTime);

        penaltyRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPenaltyRunning) return;

                long currentTime = System.currentTimeMillis();
                long lastUpdate = prefs.getLongValue(PREF_LAST_PENALTY_UPDATE + bookingId, penaltyStartTime);

                long newPenaltyMinutes = (currentTime - lastUpdate) / (60 * 1000);
                if (newPenaltyMinutes > 0) {
                    double newPenalty = newPenaltyMinutes * penaltyPerMinute;
                    accumulatedPenalty += newPenalty;

                    saveCurrentPenaltyState(currentTime);
                    updatePenaltyDisplay();

                    if (listener != null) {
                        long totalMinutes = (currentTime - penaltyStartTime) / (60 * 1000);
                        listener.onPenaltyUpdated(accumulatedPenalty, totalMinutes);
                    }
                }

                handler.postDelayed(this, 60000); // Update every minute
            }
        };
        handler.post(penaltyRunnable);
        updatePenaltyDisplay();
    }

    private void updatePenaltyDisplay() {
        if (penaltyDisplay != null) {
            long currentTime = System.currentTimeMillis();
            long penaltyStart = prefs.getLongValue(PREF_PENALTY_START + bookingId, 0);
            long totalPenaltyMinutes = (currentTime - penaltyStart) / (60 * 1000);

            prefs.saveFloatValue(PREF_PENALTY_AMOUNT + bookingId, (float) accumulatedPenalty);

            String penaltyText = String.format(Locale.getDefault(),
                    "Penalty: ₹%.2f (%d mins)", accumulatedPenalty, totalPenaltyMinutes);
            handler.post(() -> {
                penaltyDisplay.setText(penaltyText);
                penaltyDisplay.setVisibility(View.VISIBLE);
            });
        }
    }

    private void startCountdown(long remainingMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (remainingMillis <= 0) {
            timeDisplay.setText("00:00:00");
            startPenaltyTimer();
            if (listener != null) {
                listener.onTimerFinished();
            }
            return;
        }

        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerDisplay(millisUntilFinished);
                prefs.saveLongValue(PREF_REMAINING_TIME + bookingId, millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeDisplay.setText("00:00:00");
                startPenaltyTimer();
                if (listener != null) {
                    listener.onTimerFinished();
                }
            }
        }.start();
    }

    private void updateTimerDisplay(long millisUntilFinished) {
        int hours = (int) (millisUntilFinished / (1000 * 60 * 60));
        int minutes = (int) ((millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) ((millisUntilFinished % (1000 * 60)) / 1000);

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                hours, minutes, seconds);
        handler.post(() -> timeDisplay.setText(timeStr));
    }

    public void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            prefs.saveLongValue(PREF_TIMER_PAUSED + bookingId, System.currentTimeMillis());
        }
        if (penaltyRunnable != null) {
            handler.removeCallbacks(penaltyRunnable);
            saveCurrentPenaltyState(System.currentTimeMillis());
        }
    }

    public void stopTimer() {
        isPenaltyRunning = false;
        isTimerActive = false;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (penaltyRunnable != null) {
            handler.removeCallbacks(penaltyRunnable);
        }
        prefs.saveBooleanValue(PREF_TIMER_ACTIVE + bookingId, false);
        clearTimerPrefs();
    }

    public double getCurrentPenalty() {
        if (isPenaltyRunning) {
            long lastUpdate = prefs.getLongValue(PREF_LAST_PENALTY_UPDATE + bookingId, 0);
            if (lastUpdate > 0) {
                calculateMissedPenalty(lastUpdate);
            }
        }
        return accumulatedPenalty;
    }

    private void clearTimerPrefs() {
        String[] keys = {
                PREF_TIMER_START, PREF_TIMER_PAUSED, PREF_REMAINING_TIME,
                PREF_PENALTY_START, PREF_ACCUMULATED_PENALTY,
                PREF_LAST_PENALTY_UPDATE, PREF_IS_PENALTY_RUNNING,
                PREF_INITIAL_WAITING_MINUTES, PREF_PENALTY_PER_MINUTE,
                PREF_TIMER_ACTIVE
        };

        for (String key : keys) {
            prefs.removeValue(key + bookingId);
        }
    }
}