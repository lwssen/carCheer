package com.carcheer.app.ui.record;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.util.DateUtils;
import com.carcheer.app.util.FuelCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.Holder> {

    public interface OnRecordClickListener {
        void onClick(RefuelRecord record);
    }

    private final List<RefuelRecord> items = new ArrayList<>();
    private Map<Long, Double> consumptions = java.util.Collections.emptyMap();
    private Map<Long, Integer> issues = java.util.Collections.emptyMap();
    private final OnRecordClickListener listener;

    public RecordAdapter(OnRecordClickListener listener) {
        this.listener = listener;
    }

    /** 更新油耗计算结果并刷新展示 */
    public void setCalculation(FuelCalculator.Result result) {
        this.consumptions = result.consumptions;
        this.issues = result.issues;
        notifyDataSetChanged();
    }

    public void submitList(List<RefuelRecord> records) {
        items.clear();
        if (records != null) {
            items.addAll(records);
        }
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
        holder.tvDate.setText(DateUtils.format(r.date));

        if (r.fullTank) {
            holder.tvFullBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvFullBadge.setVisibility(View.GONE);
        }

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

        Double consumption = consumptions.get(r.id);
        Integer issue = issues.get(r.id);
        if (consumption != null) {
            holder.tvConsumption.setVisibility(View.VISIBLE);
            holder.tvConsumption.setText(
                    holder.itemView.getContext()
                            .getString(R.string.consumption_fmt, consumption));
        } else if (issue != null) {
            holder.tvConsumption.setVisibility(View.VISIBLE);
            holder.tvConsumption.setText(issueText(holder, issue));
        } else {
            holder.tvConsumption.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String issueText(Holder holder, int issueCode) {
        switch (issueCode) {
            case FuelCalculator.ISSUE_FIRST_TANK:
                return holder.itemView.getContext().getString(R.string.issue_first_tank);
            case FuelCalculator.ISSUE_VOLUME_MISSING:
                return holder.itemView.getContext().getString(R.string.issue_volume_missing);
            case FuelCalculator.ISSUE_ODOMETER_BACKWARD:
                return holder.itemView.getContext().getString(R.string.issue_odometer_backward);
            case FuelCalculator.ISSUE_ZERO_DISTANCE:
                return holder.itemView.getContext().getString(R.string.issue_zero_distance);
            default:
                return holder.itemView.getContext().getString(R.string.issue_unknown);
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvDate;
        final TextView tvFullBadge;
        final TextView tvAmount;
        final TextView tvDetail;
        final TextView tvConsumption;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvFullBadge = itemView.findViewById(R.id.tv_full_badge);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDetail = itemView.findViewById(R.id.tv_detail);
            tvConsumption = itemView.findViewById(R.id.tv_consumption);
        }
    }
}
