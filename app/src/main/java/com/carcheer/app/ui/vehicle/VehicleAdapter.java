package com.carcheer.app.ui.vehicle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.Holder> {

    private final List<Vehicle> items = new ArrayList<>();
    private Map<Long, Integer> recordCounts = java.util.Collections.emptyMap();
    private final Consumer<Vehicle> onEdit;
    private final Consumer<Vehicle> onDelete;

    public VehicleAdapter(Consumer<Vehicle> onEdit, Consumer<Vehicle> onDelete) {
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    public void submitList(List<Vehicle> vehicles, Map<Long, Integer> counts) {
        items.clear();
        if (vehicles != null) {
            items.addAll(vehicles);
        }
        recordCounts = counts;
        notifyDataSetChanged();
    }

    public void refreshCounts() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Vehicle vehicle = items.get(position);
        holder.tvName.setText(vehicle.name);

        holder.tvDefaultBadge.setVisibility(vehicle.isDefault ? View.VISIBLE : View.GONE);

        String plate = vehicle.plateNo;
        holder.tvPlate.setVisibility(
                plate != null && !plate.trim().isEmpty() ? View.VISIBLE : View.GONE);
        holder.tvPlate.setText(plate);

        Integer count = recordCounts.get(vehicle.id);
        String summary = holder.itemView.getContext().getString(
                R.string.vehicle_records_fmt, count == null ? 0 : count);
        holder.tvSummary.setText(summary);

        holder.itemView.setOnClickListener(v -> onEdit.accept(vehicle));
        holder.itemView.setOnLongClickListener(v -> {
            onDelete.accept(vehicle);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDefaultBadge;
        final TextView tvPlate;
        final TextView tvSummary;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDefaultBadge = itemView.findViewById(R.id.tv_default_badge);
            tvPlate = itemView.findViewById(R.id.tv_plate);
            tvSummary = itemView.findViewById(R.id.tv_summary);
        }
    }
}
