package com.carcheer.app.ui.vehicle;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.Vehicle;
import com.carcheer.app.data.pojo.VehicleRecordCount;
import com.carcheer.app.databinding.ActivityVehicleManageBinding;
import com.carcheer.app.viewmodel.VehicleViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleManageActivity extends AppCompatActivity {

    private VehicleViewModel viewModel;
    private VehicleAdapter adapter;
    private ActivityVehicleManageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.carcheer.app.util.ThemeSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityVehicleManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setTitle(R.string.vehicle_manage_title);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(VehicleViewModel.class);
        adapter = new VehicleAdapter(this::showEditDialog, this::confirmDelete);
        binding.rvVehicles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVehicles.setAdapter(adapter);

        binding.fabAddVehicle.setOnClickListener(v -> showEditDialog(null));

        Map<Long, Integer> counts = new HashMap<>();
        viewModel.getVehicles().observe(this, vehicles -> {
            adapter.submitList(vehicles, counts);
            updateEmptyState(vehicles == null || vehicles.isEmpty());
        });
        viewModel.getRecordCounts().observe(this, list -> {
            counts.clear();
            if (list != null) {
                for (VehicleRecordCount item : list) {
                    counts.put(item.vehicleId, item.recordCount);
                }
            }
            adapter.refreshCounts();
        });
    }

    private void updateEmptyState(boolean empty) {
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvVehicles.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showEditDialog(Vehicle vehicle) {
        VehicleEditDialogFragment.show(getSupportFragmentManager(), vehicle);
    }

    private void confirmDelete(Vehicle vehicle) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_vehicle_title)
                .setMessage(getString(R.string.confirm_delete_vehicle_msg, vehicle.name))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> viewModel.delete(vehicle))
                .show();
    }
}
