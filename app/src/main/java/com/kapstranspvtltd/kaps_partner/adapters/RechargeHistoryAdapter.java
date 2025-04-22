package com.kapstranspvtltd.kaps_partner.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.models.RechargeHistory;
import com.kapstranspvtltd.kaps_partner.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RechargeHistoryAdapter extends RecyclerView.Adapter<RechargeHistoryAdapter.ViewHolder> {
    private List<RechargeHistory> historyList;
    private Context context;

    public RechargeHistoryAdapter(Context context) {
        this.context = context;
        this.historyList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recharge_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RechargeHistory history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistoryList(List<RechargeHistory> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPlanTitle, tvPlanDescription, tvPrice, tvValidity;
        private TextView tvPurchaseDate, tvStatus, tvExpiryTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvPlanTitle = itemView.findViewById(R.id.tvPlanTitle);
            tvPlanDescription = itemView.findViewById(R.id.tvPlanDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvValidity = itemView.findViewById(R.id.tvValidity);
            tvPurchaseDate = itemView.findViewById(R.id.tvPurchaseDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvExpiryTime = itemView.findViewById(R.id.tvExpiryTime);
        }

        void bind(RechargeHistory history) {
            tvPlanTitle.setText(history.getPlanTitle());
            tvPlanDescription.setText(history.getPlanDescription());
            tvPrice.setText(String.format(Locale.US, "₹%.2f", history.getPlanPrice()));
            tvValidity.setText(history.getPlanDays()+" Days");

            try {
                // Define date formats
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);

                // Parse expiry time
                Date expiryDate = inputFormat.parse(history.getPlanExpiryTime());

                // Get current date/time
                Date currentDate = new Date();

                // Compare dates for status
                boolean isExpired = currentDate.after(expiryDate);

                // Set status with proper background
                tvStatus.setText(isExpired ? "EXPIRED" : "ACTIVE");
                tvStatus.setBackgroundResource(isExpired ?
                        R.drawable.bg_status_expiry : R.drawable.bg_status_active);

                // Format dates for display
                tvPurchaseDate.setText(getFormattedBookingTiming(history.getPurchasedDateTime()));
                tvExpiryTime.setText(outputFormat.format(expiryDate));

                // Optionally add remaining time for active plans
                if (!isExpired) {
                    long remainingTime = expiryDate.getTime() - currentDate.getTime();
                    String remainingDays = formatRemainingTime(remainingTime);
                    tvExpiryTime.setText(String.format("%s (%s)",
                            outputFormat.format(expiryDate),
                            remainingDays));
                }

            } catch (ParseException e) {
                Log.e("RechargeHistory", "Error parsing dates: " + e.getMessage());
                // Fallback to raw values
                tvPurchaseDate.setText(getFormattedBookingTiming(history.getPurchasedDateTime()));
                tvExpiryTime.setText(history.getPlanExpiryTime());
                tvStatus.setText("UNKNOWN");
                tvStatus.setBackgroundResource(R.drawable.bg_status_expiry);
            }
        }

        // Helper method to format remaining time
        private String formatRemainingTime(long milliseconds) {
            long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
            long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24;

            if (days > 0) {
                return String.format(Locale.US, "%d days %d hrs left", days, hours);
            } else if (hours > 0) {
                return String.format(Locale.US, "%d hours left", hours);
            } else {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
                return String.format(Locale.US, "%d minutes left", minutes);
            }
        }
        public String getFormattedBookingTiming(String bookingTiming) {
            try {
                double epochTime = Double.parseDouble(bookingTiming);
                long milliseconds = (long) (epochTime * 1000);
                Date date = new Date(milliseconds);
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, hh:mm a", Locale.getDefault());
                return sdf.format(date);
            } catch (Exception e) {
                return bookingTiming;
            }
        }
    }
}