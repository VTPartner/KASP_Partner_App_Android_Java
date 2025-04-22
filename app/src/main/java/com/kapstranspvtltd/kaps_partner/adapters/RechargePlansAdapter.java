package com.kapstranspvtltd.kaps_partner.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.kapstranspvtltd.kaps_partner.models.RechargePlan;
import com.kapstranspvtltd.kaps_partner.R;

import java.util.List;

public class RechargePlansAdapter extends RecyclerView.Adapter<RechargePlansAdapter.PlanViewHolder> {
    private List<RechargePlan> plans;
    private OnPlanSelectedListener listener;

    public interface OnPlanSelectedListener {
        void onPlanSelected(RechargePlan plan);
    }

    public RechargePlansAdapter(List<RechargePlan> plans, OnPlanSelectedListener listener) {
        this.plans = plans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recharge_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        holder.bind(plans.get(position));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    class PlanViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText, descriptionText, priceText, validityText;
        private MaterialButton buyButton;

        PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.planTitle);
            descriptionText = itemView.findViewById(R.id.planDescription);
            priceText = itemView.findViewById(R.id.planPrice);
            validityText = itemView.findViewById(R.id.planValidity);
            buyButton = itemView.findViewById(R.id.buyButton);
        }

        void bind(RechargePlan plan) {
            try {
                // Format price with ₹ symbol
                priceText.setText("₹"+plan.getPrice()+"");

                // Format validity days
                validityText.setText("Valid for "+plan.getValidityDays()+" days");

                // Set other fields
                titleText.setText(plan.getTitle());
                descriptionText.setText(plan.getDescription());

            } catch (Exception e) {
                Log.e("RechargePlansAdapter", "Error binding plan data: " + e.getMessage());
                // Set default values in case of error
                priceText.setText("₹0.00");
                validityText.setText("Valid for 0 days");
            }
//            titleText.setText(plan.getTitle());
//            descriptionText.setText(plan.getDescription());
//            priceText.setText(String.format("₹%.2f", plan.getPrice()));
//            validityText.setText(String.format("Valid for %d days", plan.getValidityDays()));

            buyButton.setOnClickListener(v -> listener.onPlanSelected(plan));
        }
    }
}