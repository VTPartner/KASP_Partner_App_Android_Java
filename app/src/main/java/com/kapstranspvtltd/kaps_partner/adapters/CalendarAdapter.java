package com.kapstranspvtltd.kaps_partner.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.common_activities.models.CalendarDay;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
    private List<CalendarDay> days;
    private OnDayClickListener listener;
    private int selectedPosition = 0;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day, int position);
    }

    public CalendarAdapter(List<CalendarDay> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
        days.get(0).setSelected(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day);
        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition != position) {
                days.get(selectedPosition).setSelected(false);
                notifyItemChanged(selectedPosition);
                selectedPosition = position;
                day.setSelected(true);
                notifyItemChanged(position);
                listener.onDayClick(day, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dayNameText, dayNumberText;
        CardView cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNameText = itemView.findViewById(R.id.dayNameText);
            dayNumberText = itemView.findViewById(R.id.dayNumberText);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(CalendarDay day) {
            dayNameText.setText(day.getDayName());
            dayNumberText.setText(String.valueOf(day.getDayNumber()));
            cardView.setCardBackgroundColor(day.isSelected() ?
                    itemView.getContext().getColor(R.color.colorPrimary) :
                    itemView.getContext().getColor(R.color.white));
            dayNameText.setTextColor(day.isSelected() ? Color.WHITE : Color.BLACK);
            dayNumberText.setTextColor(day.isSelected() ? Color.WHITE : Color.BLACK);
        }
    }
}