package com.kapstranspvtltd.kaps_partner.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.models.OrderRatingModel;
import com.kapstranspvtltd.kaps_partner.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RatingsAdapter extends RecyclerView.Adapter<RatingsAdapter.RatingViewHolder> {
    private List<OrderRatingModel> ratings;

    public RatingsAdapter(List<OrderRatingModel> ratings) {
        this.ratings = ratings;
    }

    @NonNull
    @Override
    public RatingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_rating, parent, false);
        return new RatingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RatingViewHolder holder, int position) {
        OrderRatingModel rating = ratings.get(position);
        holder.bind(rating);
    }

    @Override
    public int getItemCount() {
        return ratings.size();
    }

    static class RatingViewHolder extends RecyclerView.ViewHolder {
        private TextView customerNameText;
        private TextView bookingDateText;
        private TextView ratingBar;

        private TextView bookingIDText;
        private TextView commentText;

        public RatingViewHolder(@NonNull View itemView) {
            super(itemView);
            customerNameText = itemView.findViewById(R.id.customerName);
            bookingDateText = itemView.findViewById(R.id.bookingTime);
            ratingBar = itemView.findViewById(R.id.ratingValue);
            commentText = itemView.findViewById(R.id.ratingDescription);
            bookingIDText = itemView.findViewById(R.id.booking_id);
        }

        public void bind(OrderRatingModel rating) {
            bookingIDText.setText("# CRN"+rating.getBookingId());
            customerNameText.setText(rating.getCustomerName());
            String formattedDate = formatDate(rating.getBookingDate());
            bookingDateText.setText(formattedDate);
            try {
//                float ratingValue = Float.parseFloat(rating.getRating());
                ratingBar.setText(rating.getRating());
            } catch (NumberFormatException e) {
//                ratingBar.setRating(0);
            }
            commentText.setText(rating.getComment());
        }

        private String formatDate(String dateStr) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                Date date = inputFormat.parse(dateStr);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return dateStr;
            }
        }
    }
}