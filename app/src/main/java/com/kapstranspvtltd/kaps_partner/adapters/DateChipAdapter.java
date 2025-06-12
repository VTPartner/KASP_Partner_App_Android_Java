package com.kapstranspvtltd.kaps_partner.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.kapstranspvtltd.kaps_partner.R;

import java.util.List;

public class DateChipAdapter extends RecyclerView.Adapter<DateChipAdapter.DateChipViewHolder> {
    private List<String> dates;
    private int selectedPosition = 0;
    private DateSelectedListener listener;

    public interface DateSelectedListener {
        void onDateSelected(String date);
    }

    public DateChipAdapter(List<String> dates, DateSelectedListener listener) {
        this.dates = dates;
        this.listener = listener;
    }

    public class DateChipViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        public DateChipViewHolder(Chip chip) {
            super(chip);
            this.chip = chip;
        }
    }

    @NonNull
    @Override
    public DateChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        Chip chip = new Chip(context);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setChipBackgroundColorResource(R.color.chip_background);
        chip.setTextColor(ContextCompat.getColorStateList(context, R.color.colorPrimary));

        // Set horizontal margin (e.g., 8dp)
        int marginHorizontal = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(marginHorizontal, 0, marginHorizontal, 0); // left, top, right, bottom
        chip.setLayoutParams(params);

        return new DateChipViewHolder(chip);
    }


    @Override
    public void onBindViewHolder(@NonNull DateChipViewHolder holder, int position) {
        holder.chip.setText(dates.get(position));
        holder.chip.setChecked(position == selectedPosition);
        holder.chip.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onDateSelected(dates.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }
}