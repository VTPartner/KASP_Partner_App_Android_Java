package com.kapstranspvtltd.kaps_partner.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.models.OrderModel;

import java.util.List;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.ViewHolder> {
    private List<OrderModel> trips;

    public TripsAdapter(List<OrderModel> trips) {
        this.trips = trips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderModel trip = trips.get(position);
        holder.bind(trip);
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, amountText,tripIDText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
//            tripIDText = itemView.findViewById(R.id.tripIDText);
            timeText = itemView.findViewById(R.id.timeText);
            amountText = itemView.findViewById(R.id.amountText);
        }

        void bind(OrderModel trip) {
//            tripIDText.setText(trip.get);
            timeText.setText(trip.getFormattedBookingTime());
            amountText.setText(trip.getFormattedPrice());
        }
    }
}