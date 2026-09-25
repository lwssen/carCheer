package com.carcheer.app.ui.history;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    private final List<RefuelRecord> items = new ArrayList<>();
    private boolean highlightAll;

    public void submitList(List<RefuelRecord> records, boolean highlightAll) {
        items.clear();
        if (records != null) {
            items.addAll(records);
        }
        this.highlightAll = highlightAll;
        notifyDataSetChanged();
    }

    public List<RefuelRecord> getItems() {
        return items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RefuelRecord r = items.get(position);
        int primary = attrColor(holder.itemView, androidx.appcompat.R.attr.colorPrimary);
        int outline = attrColor(holder.itemView, com.google.android.material.R.attr.colorOutline);
        holder.card.setStrokeWidth(dp(holder.itemView, highlightAll ? 2 : 1));
        holder.card.setStrokeColor(highlightAll ? primary : outline);

        holder.tvDate.setText(DateUtils.format(r.date));
        holder.tvFullBadge.setVisibility(r.fullTank ? View.VISIBLE : View.GONE);
        holder.tvAmount.setText(r.amount != null
                ? String.format(Locale.CHINA, "¥%.2f", r.amount)
                : "—");

        StringBuilder detail = new StringBuilder();
        if (r.fuelType != null && !r.fuelType.isEmpty()) {
            detail.append(r.fuelType).append("  ");
        }
        if (r.volume != null) {
            detail.append(String.format(Locale.CHINA, "%.2fL", r.volume)).append("  ");
        }
        if (r.price != null) {
            detail.append(String.format(Locale.CHINA, "¥%.2f/L", r.price)).append("  ");
        }
        detail.append(String.format(Locale.CHINA, "%.0fkm", r.odometer));
        holder.tvDetail.setText(detail.toString().trim());

        if (highlightAll) {
            holder.tvConsumption.setVisibility(View.VISIBLE);
            holder.tvConsumption.setText(R.string.history_max_badge);
            holder.tvConsumption.setTextColor(primary);
        } else if (r.consumption != null) {
            holder.tvConsumption.setVisibility(View.VISIBLE);
            holder.tvConsumption.setText(
                    holder.itemView.getContext()
                            .getString(R.string.consumption_fmt, r.consumption));
            holder.tvConsumption.setTextColor(primary);
        } else {
            holder.tvConsumption.setVisibility(View.GONE);
        }
    }

    private static int attrColor(View v, int attr) {
        TypedValue tv = new TypedValue();
        v.getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private static int dp(View v, int dp) {
        return Math.round(dp * v.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvDate;
        final TextView tvFullBadge;
        final TextView tvAmount;
        final TextView tvDetail;
        final TextView tvConsumption;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvDate = itemView.findViewById(R.id.tv_date);
            tvFullBadge = itemView.findViewById(R.id.tv_full_badge);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDetail = itemView.findViewById(R.id.tv_detail);
            tvConsumption = itemView.findViewById(R.id.tv_consumption);
        }
    }
}
