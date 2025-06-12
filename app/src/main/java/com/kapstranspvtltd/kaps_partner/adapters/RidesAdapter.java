package com.kapstranspvtltd.kaps_partner.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kapstranspvtltd.kaps_partner.models.OrderRidesModel;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ItemRideBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.RideViewHolder> {
    
    private List<OrderRidesModel> ordersList;
    private OnRideClickListener listener;

    public interface OnRideClickListener {
        void onRideClick(OrderRidesModel order);
    }

    public RidesAdapter(List<OrderRidesModel> ordersList, OnRideClickListener listener) {
        this.ordersList = ordersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRideBinding binding = ItemRideBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent, 
            false
        );
        return new RideViewHolder(binding);
    }

    class RideViewHolder extends RecyclerView.ViewHolder {
        private final ItemRideBinding binding;

        RideViewHolder(ItemRideBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderRidesModel order) {
            // Load customer image using Glide
            Glide.with(binding.customerImage)
                    .load(order.getCustomerImage())
                    .placeholder(R.drawable.demo_user)
                    .error(R.drawable.demo_user)
                    .circleCrop()
                    .into(binding.customerImage);
            String dropAddress = order.getDropAddress();

            if(dropAddress != null && dropAddress.isEmpty() == false && dropAddress.equalsIgnoreCase("NA")){
                binding.dottedLineLyt.setVisibility(View.GONE);
                binding.dropLocationLayout.setVisibility(View.GONE);
            }
            long totalPriceRoundAmount = Math.round(order.getTotalPrice());
            long penaltyAmountRound = Math.round(order.getPenaltyAmount());
            long totalPrice = totalPriceRoundAmount+penaltyAmountRound;
            // Set texts
            binding.dateTime.setText(formatDateTime(order.getBookingTiming()));
            binding.customerName.setText(order.getCustomerName());
            binding.amount.setText("₹"+totalPrice);
            binding.pickupAddress.setText(order.getPickupAddress());
            binding.dropAddress.setText(order.getDropAddress());



            // Handle click
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRideClick(order);
                }
            });
        }

        private String formatDateTime(double timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return sdf.format(new Date((long) (timestamp * 1000)));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        holder.bind(ordersList.get(position));
    }

    @Override
    public int getItemCount() {
        return ordersList.size();
    }


}