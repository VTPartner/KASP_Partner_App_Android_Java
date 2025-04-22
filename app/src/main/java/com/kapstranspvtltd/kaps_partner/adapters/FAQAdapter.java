package com.kapstranspvtltd.kaps_partner.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.models.FAQ;

import java.util.ArrayList;
import java.util.List;

public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.FAQViewHolder> {
    private List<FAQ> faqs;
    private Context context;

    public FAQAdapter(Context context) {
        this.context = context;
        this.faqs = new ArrayList<>();
    }

    @NonNull
    @Override
    public FAQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_faq, parent, false);
        return new FAQViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FAQViewHolder holder, int position) {
        FAQ faq = faqs.get(position);
        holder.textQuestion.setText(faq.getQuestion());
        holder.textAnswer.setText(faq.getAnswer());
    }

    @Override
    public int getItemCount() {
        return faqs.size();
    }

    public void setFaqs(List<FAQ> faqs) {
        this.faqs = faqs;
        notifyDataSetChanged();
    }

    static class FAQViewHolder extends RecyclerView.ViewHolder {
        TextView textQuestion;
        TextView textAnswer;

        FAQViewHolder(@NonNull View itemView) {
            super(itemView);
            textQuestion = itemView.findViewById(R.id.textQuestion);
            textAnswer = itemView.findViewById(R.id.textAnswer);
        }
    }
}