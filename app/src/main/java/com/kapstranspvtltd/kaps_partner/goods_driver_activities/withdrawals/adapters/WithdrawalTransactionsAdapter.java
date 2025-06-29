package com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals.models.WithdrawalTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WithdrawalTransactionsAdapter extends RecyclerView.Adapter<WithdrawalTransactionsAdapter.ViewHolder> {
    private List<WithdrawalTransaction> transactions;
    private Context context;

    public WithdrawalTransactionsAdapter(Context context) {
        this.context = context;
        this.transactions = new ArrayList<>();
    }

    public void setTransactions(List<WithdrawalTransaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdrawal_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WithdrawalTransaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView txtMsg;
        private TextView txtStatus;
        private TextView txtDate;
        private TextView txtAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Match these IDs with your layout file
            txtMsg = itemView.findViewById(R.id.txt_msg);
            txtStatus = itemView.findViewById(R.id.txt_status);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtAmount = itemView.findViewById(R.id.txt_amount);
        }

        public void bind(WithdrawalTransaction transaction) {
            try {
                txtAmount.setText(String.format("₹%.2f", transaction.getAmount()));

                // Set withdrawal description
                String msg;
                if (transaction.getPaymentMethod().equals("UPI")) {
                    msg = "UPI Withdrawal - " + transaction.getUpiId();
                } else {
                    msg = "Bank Transfer - A/C: " + transaction.getAccountNumber();
                }
                if (txtMsg != null) txtMsg.setText(msg);

                // Set status
                if (txtStatus != null) {
                    txtStatus.setText(transaction.getStatus());
                    int statusColor;
                    switch (transaction.getStatus()) {
                        case "COMPLETED":
                            statusColor = R.color.green;
                            break;
                        case "FAILED":
                            statusColor = R.color.colorerror;
                            break;
                        case "PENDING":
                            statusColor = R.color.orange;
                            break;
                        default:
                            statusColor = R.color.colorgrey3;
                    }
                    txtStatus.setTextColor(ContextCompat.getColor(context, statusColor));
                }

                // Set date
                if (txtDate != null) {
                    txtDate.setText(formatDate(transaction.getCreatedAt()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String formatDate(long epochSeconds) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                return sdf.format(new Date(epochSeconds * 1000));
            } catch (Exception e) {
                e.printStackTrace();
                return "Invalid Date";
            }
        }
    }
}