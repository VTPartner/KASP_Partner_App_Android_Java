package com.kapstranspvtltd.kaps_partner.common_activities.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.common_activities.models.CancelReason;

import java.util.List;

public class CancelReasonAdapter extends RecyclerView.Adapter<CancelReasonAdapter.ViewHolder> {
    private List<CancelReason> reasons;
    private CancelReason selectedReason = null;
    private OnReasonSelectedListener listener;

    public interface OnReasonSelectedListener {
        void onReasonSelected(CancelReason reason);
    }

    public CancelReasonAdapter(List<CancelReason> reasons, OnReasonSelectedListener listener) {
        this.reasons = reasons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cancel_reason, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CancelReason reason = reasons.get(position);
        holder.reasonText.setText(reason.getReason());

        boolean isSelected = reason.equals(selectedReason);
        holder.itemView.setSelected(isSelected);
        holder.checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            selectedReason = reason;
            listener.onReasonSelected(reason);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return reasons.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView reasonText;
        ImageView checkIcon;

        ViewHolder(View view) {
            super(view);
            reasonText = view.findViewById(R.id.reasonText);
            checkIcon = view.findViewById(R.id.checkIcon);
        }
    }
}