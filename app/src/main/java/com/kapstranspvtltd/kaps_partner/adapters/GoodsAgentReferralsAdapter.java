package com.kapstranspvtltd.kaps_partner.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.models.GoodsAgentReferralModel;

import java.util.List;

public class GoodsAgentReferralsAdapter extends RecyclerView.Adapter<GoodsAgentReferralsAdapter.ReferralViewHolder> {

    private List<GoodsAgentReferralModel> referralsList;
    private Context context;

    public GoodsAgentReferralsAdapter(List<GoodsAgentReferralModel> referralsList) {
        this.referralsList = referralsList;
    }

    @NonNull
    @Override
    public ReferralViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_goods_agent_referral, parent, false);
        return new ReferralViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReferralViewHolder holder, int position) {
        GoodsAgentReferralModel referral = referralsList.get(position);
        holder.bind(referral);
    }

    @Override
    public int getItemCount() {
        return referralsList.size();
    }

    public class ReferralViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPosition;
        private TextView tvReferralName;
        private TextView tvReferralDate;
        private TextView tvReferralAmount;
        private TextView tvReferralStatus;

        public ReferralViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tvPosition);
            tvReferralName = itemView.findViewById(R.id.tvReferralName);
            tvReferralDate = itemView.findViewById(R.id.tvReferralDate);
            tvReferralAmount = itemView.findViewById(R.id.tvReferralAmount);
            tvReferralStatus = itemView.findViewById(R.id.tvReferralStatus);
        }

        public void bind(GoodsAgentReferralModel referral) {
            tvPosition.setText(String.valueOf(referral.getPosition()));
            tvReferralName.setText(referral.getDriverName());
            tvReferralDate.setText("Joined " + referral.getRelativeTime());
            tvReferralAmount.setText(referral.getFormattedAmount());
            tvReferralStatus.setText(referral.getStatus());

            // Set status styling
            if (referral.isCompleted()) {
                tvReferralStatus.setTextColor(ContextCompat.getColor(context, R.color.green));
                tvReferralStatus.setBackgroundResource(R.drawable.bg_status_completed);
                tvReferralAmount.setTextColor(ContextCompat.getColor(context, R.color.green));
            } else if (referral.isPending()) {
                tvReferralStatus.setTextColor(ContextCompat.getColor(context, R.color.orange));
                tvReferralStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvReferralAmount.setTextColor(ContextCompat.getColor(context, R.color.grey));
                tvReferralAmount.setText("₹0");
            } else {
                tvReferralStatus.setTextColor(ContextCompat.getColor(context, R.color.grey));
                tvReferralStatus.setBackgroundResource(R.drawable.bg_status_default);
                tvReferralAmount.setTextColor(ContextCompat.getColor(context, R.color.grey));
            }
        }
    }

    public void updateReferrals(List<GoodsAgentReferralModel> newReferrals) {
        this.referralsList = newReferrals;
        notifyDataSetChanged();
    }

    public void addReferral(GoodsAgentReferralModel referral) {
        referralsList.add(referral);
        notifyItemInserted(referralsList.size() - 1);
    }

    public void updateReferral(int position, GoodsAgentReferralModel referral) {
        if (position >= 0 && position < referralsList.size()) {
            referralsList.set(position, referral);
            notifyItemChanged(position);
        }
    }

    public void clearReferrals() {
        referralsList.clear();
        notifyDataSetChanged();
    }
} 