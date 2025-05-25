package com.kapstranspvtltd.kaps_partner.common_activities.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.common_activities.models.WalletTransaction;

import java.util.ArrayList;
import java.util.List;

public class WalletHistoryAdapter extends RecyclerView.Adapter<WalletHistoryAdapter.ViewHolder> {
    private final Context context;
    private List<WalletTransaction> transactions = new ArrayList<>();

    public WalletHistoryAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransaction transaction = transactions.get(position);
        holder.bind(transaction, context);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void setTransactions(List<WalletTransaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtAmount;
        private final TextView txtType;
        private final TextView txtDate;
        private final TextView txtStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAmount = itemView.findViewById(R.id.txt_amount);
            txtType = itemView.findViewById(R.id.txt_msg);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtStatus = itemView.findViewById(R.id.txt_status);
        }

        public void bind(WalletTransaction transaction, Context context) {
            String amountText = "₹"+transaction.getAmount();
            txtAmount.setText(amountText);
            txtAmount.setTextColor(ContextCompat.getColor(context,
                    transaction.getType().equalsIgnoreCase("DEPOSIT") ? R.color.green : R.color.colorerror));

            txtType.setText("#"+transaction.getRazorPayID());
            txtDate.setText(transaction.getDate());
            txtStatus.setText(transaction.getType());
            txtStatus.setTextColor(ContextCompat.getColor(context,
                    transaction.getType().equalsIgnoreCase("DEPOSIT") ? R.color.green : R.color.colorerror));
        }
    }
}