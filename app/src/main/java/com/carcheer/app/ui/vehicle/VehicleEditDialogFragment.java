package com.carcheer.app.ui.vehicle;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.Vehicle;
import com.carcheer.app.databinding.DialogVehicleEditBinding;
import com.carcheer.app.viewmodel.VehicleViewModel;

public class VehicleEditDialogFragment extends DialogFragment {

    private static final String ARG_VEHICLE_ID = "vehicle_id";

    private VehicleViewModel viewModel;
    private DialogVehicleEditBinding binding;
    private Vehicle editing;

    public static void show(FragmentManager fm, Vehicle vehicle) {
        VehicleEditDialogFragment fragment = new VehicleEditDialogFragment();
        Bundle args = new Bundle();
        if (vehicle != null) {
            args.putLong(ARG_VEHICLE_ID, vehicle.id);
        }
        fragment.setArguments(args);
        fragment.show(fm, "vehicle_edit");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(VehicleViewModel.class);
        binding = DialogVehicleEditBinding.inflate(getLayoutInflater());

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_VEHICLE_ID)) {
            editing = viewModel.getVehicleSync(args.getLong(ARG_VEHICLE_ID));
        }
        if (editing != null) {
            fillForm();
        }

        boolean isEdit = editing != null;
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? R.string.vehicle_edit_title : R.string.vehicle_add_title)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        // 校验失败时不关闭对话框，故拦截正按钮
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> save()));
        return dialog;
    }

    private void fillForm() {
        binding.etName.setText(editing.name);
        binding.etPlate.setText(editing.plateNo);
        if (editing.initialOdometer > 0) {
            binding.etInitialOdometer.setText(trimDouble(editing.initialOdometer));
        }
        if (editing.currentOdometer > 0) {
            binding.etCurrentOdometer.setText(trimDouble(editing.currentOdometer));
        }
        if (editing.lastOdometer > 0) {
            binding.etLastOdometer.setText(trimDouble(editing.lastOdometer));
        }
        if (editing.tankCapacity > 0) {
            binding.etTank.setText(trimDouble(editing.tankCapacity));
        }
        binding.switchDefault.setChecked(editing.isDefault);
    }

    private void save() {
        String name = getText(binding.etName);
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_vehicle_name_required));
            return;
        }

        Vehicle vehicle = editing != null ? editing : new Vehicle();
        vehicle.name = name;
        vehicle.plateNo = getText(binding.etPlate);
        vehicle.isDefault = binding.switchDefault.isChecked();

        Double odometer = parseNumber(getText(binding.etInitialOdometer));
        if (odometer == null) {
            toastInvalidNumber();
            return;
        }
        vehicle.initialOdometer = odometer;

        Double currentOdo = parseNumber(getText(binding.etCurrentOdometer));
        if (currentOdo == null) {
            toastInvalidNumber();
            return;
        }
        vehicle.currentOdometer = currentOdo;

        Double lastOdo = parseNumber(getText(binding.etLastOdometer));
        if (lastOdo == null) {
            toastInvalidNumber();
            return;
        }
        vehicle.lastOdometer = lastOdo;

        Double tank = parseNumber(getText(binding.etTank));
        if (tank == null) {
            toastInvalidNumber();
            return;
        }
        vehicle.tankCapacity = tank;

        viewModel.save(vehicle);
        dismiss();
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private Double parseNumber(String text) {
        if (TextUtils.isEmpty(text)) {
            return 0.0;
        }
        try {
            double value = Double.parseDouble(text);
            return value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void toastInvalidNumber() {
        Toast.makeText(requireContext(), R.string.error_invalid_number, Toast.LENGTH_SHORT).show();
    }

    private static String trimDouble(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.valueOf(value);
    }
}
