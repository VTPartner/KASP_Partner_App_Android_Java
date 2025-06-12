package com.kapstranspvtltd.kaps_partner.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kapstranspvtltd.kaps_partner.models.OrderModel;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ItemOrdersBinding;

import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
    private final List<OrderModel> orders;

    public OrdersAdapter(List<OrderModel> orders) {
        this.orders = orders;
    }

    public class OrderViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrdersBinding binding;

        public OrderViewHolder(ItemOrdersBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OrderModel order) {
            // Set customer name
            binding.customerName.setText(order.getCustomerName());

            // Set date
//            String formattedDateTime = order.getDayFromDate() + ", " + order.getFormattedDate();
//            binding.orderDate.setText(formattedDateTime);
            String day = order.getDayFromDate();
            String formattedDateTime = !day.isEmpty() ?
                    day + ", " + order.getFormattedDate() :
                    order.getFormattedDate();
            binding.orderDate.setText(formattedDateTime);

            // Set price
            binding.orderAmount.setText(order.getFormattedPrice());
            binding.orderAmount.setTextColor(
                ContextCompat.getColor(binding.getRoot().getContext(), R.color.colorPrimary)
            );

            // Load customer image
            // Commented out as in original code
            /*
            String customerImage = order.getCustomerImage();
            if (customerImage != null) {
                Glide.with(binding.customerImage.getContext())
                    .load(customerImage)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.customerImage);
            } else {
                binding.customerImage.setImageResource(R.drawable.ic_person);
            }
            */

            // Add divider except for last item
            binding.divider.setVisibility(
                getAdapterPosition() < orders.size() - 1 ? ViewGroup.VISIBLE : ViewGroup.GONE
            );
        }
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrdersBinding binding = ItemOrdersBinding.inflate(
            LayoutInflater.from(parent.getContext()), 
            parent, 
            false
        );
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }
}