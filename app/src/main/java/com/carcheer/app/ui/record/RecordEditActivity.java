package com.carcheer.app.ui.record;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.data.entity.Vehicle;
import com.carcheer.app.databinding.ActivityRecordEditBinding;
import com.carcheer.app.util.DateUtils;
import com.carcheer.app.viewmodel.RecordViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class RecordEditActivity extends AppCompatActivity {

    private static final String EXTRA_VEHICLE_ID = "vehicle_id";
    private static final String EXTRA_RECORD_ID = "record_id";

    private RecordViewModel viewModel;
    private ActivityRecordEditBinding binding;

    private long vehicleId;
    private long recordId;
    private RefuelRecord editing;

    private Calendar date = Calendar.getInstance();
    private boolean updatingFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.carcheer.app.util.ThemeSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityRecordEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RecordViewModel.class);
        vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0);
        recordId = getIntent().getLongExtra(EXTRA_RECORD_ID, 0);

        if (recordId != 0) {
            editing = viewModel.getByIdSync(recordId);
        }
        if (editing != null) {
            vehicleId = editing.vehicleId;
            fillForm();
            binding.toolbar.setTitle(R.string.record_edit_title);
            binding.btnDelete.setVisibility(android.view.View.VISIBLE);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupFuelType();
        setupDatePicker();
        setupLinkage();
        prefillOdometer();

        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnSave.setOnClickListener(v -> save());
    }

    public static Intent newIntent(Context context, long vehicleId, long recordId) {
        Intent intent = new Intent(context, RecordEditActivity.class);
        intent.putExtra(EXTRA_VEHICLE_ID, vehicleId);
        intent.putExtra(EXTRA_RECORD_ID, recordId);
        return intent;
    }

    private void fillForm() {
        updatingFields = true;
        date.setTimeInMillis(editing.date);
        refreshDateText();
        binding.etAmount.setText(editing.amount == null ? "" : trimNumber(editing.amount));
        binding.etVolume.setText(editing.volume == null ? "" : trimNumber(editing.volume));
        binding.etPrice.setText(editing.price == null ? "" : trimNumber(editing.price));
        binding.etOdometer.setText(editing.odometer == 0 ? "" : trimNumber(editing.odometer));
        binding.etStation.setText(editing.station);
        binding.etNote.setText(editing.note);
        binding.switchFullTank.setChecked(editing.fullTank);
        updatingFields = false;
    }

    private void setupFuelType() {
        String[] types = getResources().getStringArray(R.array.fuel_types);
        binding.actFuelType.setSimpleItems(types);
        if (editing == null || TextUtils.isEmpty(editing.fuelType)) {
            binding.actFuelType.setText(types[0], false);
        } else {
            binding.actFuelType.setText(editing.fuelType, false);
        }
    }

    private void setupDatePicker() {
        refreshDateText();
        binding.btnDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this, (d, year, month, day) -> {
                date.set(Calendar.YEAR, year);
                date.set(Calendar.MONTH, month);
                date.set(Calendar.DAY_OF_MONTH, day);
                refreshDateText();
            }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
        binding.btnTime.setOnClickListener(v -> showTimePicker());
    }

    /** 时/分/秒三个滚轮的自定义时间选择对话框 */
    private void showTimePicker() {
        android.view.View content = getLayoutInflater()
                .inflate(com.carcheer.app.R.layout.dialog_time_pick, null);
        android.widget.NumberPicker hourPicker = content.findViewById(com.carcheer.app.R.id.picker_hour);
        android.widget.NumberPicker minutePicker = content.findViewById(com.carcheer.app.R.id.picker_minute);
        android.widget.NumberPicker secondPicker = content.findViewById(com.carcheer.app.R.id.picker_second);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        hourPicker.setValue(date.get(Calendar.HOUR_OF_DAY));
        minutePicker.setValue(date.get(Calendar.MINUTE));
        secondPicker.setValue(date.get(Calendar.SECOND));

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.time_pick_title)
                .setView(content)
                .setPositiveButton(R.string.action_ok, (d, which) -> {
                    date.set(Calendar.HOUR_OF_DAY, hourPicker.getValue());
                    date.set(Calendar.MINUTE, minutePicker.getValue());
                    date.set(Calendar.SECOND, secondPicker.getValue());
                    refreshDateText();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void refreshDateText() {
        binding.btnDate.setText(DateUtils.format(date.getTimeInMillis()));
        binding.btnTime.setText(String.format(java.util.Locale.CHINA, "%02d:%02d:%02d",
                date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), date.get(Calendar.SECOND)));
    }

    private static final int IDX_AMOUNT = 0;
    private static final int IDX_VOLUME = 1;
    private static final int IDX_PRICE = 2;

    /** 标记当前值是否为联动自动填充：重算时优先覆盖自动字段，避免覆盖用户手输内容 */
    private final boolean[] autoFilled = new boolean[3];

    /**
     * 金额/油量/单价联动。目标字段选取规则：
     * 1. 另外两个字段中有自动填充的 → 复算它（消除逐字符输入时中间值的反算污染）；
     * 2. 否则有空的 → 补算空的；
     * 3. 两个字段都是用户手输 → 不动。
     */
    private void setupLinkage() {
        addLinkageWatcher(binding.etAmount, IDX_AMOUNT);
        addLinkageWatcher(binding.etVolume, IDX_VOLUME);
        addLinkageWatcher(binding.etPrice, IDX_PRICE);
    }

    private void addLinkageWatcher(TextInputEditText editText, int idx) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingFields) {
                    return;
                }
                updatingFields = true;
                autoFilled[idx] = false;
                recalculateFrom(idx);
                updatingFields = false;
            }
        });
    }

    private void recalculateFrom(int editedIdx) {
        Double[] vals = {
                parse(binding.etAmount),
                parse(binding.etVolume),
                parse(binding.etPrice)
        };
        int a = (editedIdx + 1) % 3;
        int b = (editedIdx + 2) % 3;

        int target = pickTarget(a, b, vals);
        if (target < 0) {
            return;
        }
        int other = (target == a) ? b : a;

        if (vals[other] == null) {
            // 目标字段依赖的另一字段为空 → 反过来补算那个空字段
            Double targetVal = vals[target];
            if (targetVal == null) {
                return;
            }
            computeTarget(other, vals[editedIdx], targetVal, editedIdx, other);
            return;
        }
        if (vals[editedIdx] == null) {
            return;
        }
        computeTarget(target, vals[editedIdx], vals[other], editedIdx, other);
    }

    private int pickTarget(int a, int b, Double[] vals) {
        if (autoFilled[a] && !autoFilled[b]) {
            return a;
        }
        if (autoFilled[b] && !autoFilled[a]) {
            return b;
        }
        if (vals[a] != null && vals[b] == null) {
            return b;
        }
        if (vals[b] != null && vals[a] == null) {
            return a;
        }
        return -1;
    }

    /** 由另外两个字段推出目标字段：amount=volume×price，volume=amount/price，price=amount/volume */
    private void computeTarget(int target, double edited, double other, int editedIdx, int otherIdx) {
        switch (target) {
            case IDX_AMOUNT:
                setAmount(edited * other);
                break;
            case IDX_VOLUME:
                double numerator = editedIdx == IDX_AMOUNT ? edited : other;
                double denominator = otherIdx == IDX_AMOUNT ? edited : other;
                setVolume(denominator > 0 ? numerator / denominator : null);
                break;
            case IDX_PRICE:
                double num = editedIdx == IDX_AMOUNT ? edited : other;
                double den = otherIdx == IDX_VOLUME ? edited : other;
                setPrice(den > 0 ? num / den : null);
                break;
        }
    }

    private Double parse(TextInputEditText et) {
        CharSequence cs = et.getText();
        if (cs == null || cs.toString().trim().isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(cs.toString().trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setAmount(Double v) {
        binding.etAmount.setText(v == null ? "" : trimNumber(round(v, 2)));
        autoFilled[IDX_AMOUNT] = true;
    }

    private void setVolume(Double v) {
        binding.etVolume.setText(v == null ? "" : trimNumber(round(v, 2)));
        autoFilled[IDX_VOLUME] = true;
    }

    private void setPrice(Double v) {
        binding.etPrice.setText(v == null ? "" : trimNumber(round(v, 2)));
        autoFilled[IDX_PRICE] = true;
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    private static String trimNumber(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.valueOf(value);
    }

    private void prefillOdometer() {
        if (editing != null || vehicleId == 0) {
            return;
        }
        Vehicle vehicle = viewModel.getVehicleSync(vehicleId);
        double odometer = viewModel.getPrefillOdometerSync(vehicleId, vehicle);
        if (odometer > 0) {
            binding.etOdometer.setText(trimNumber(odometer));
        }
    }

    private void save() {
        if (vehicleId == 0) {
            Toast.makeText(this, R.string.error_no_vehicle, Toast.LENGTH_SHORT).show();
            return;
        }

        Double amount = parse(binding.etAmount);
        Double volume = parse(binding.etVolume);
        Double price = parse(binding.etPrice);

        if (amount == null && volume == null) {
            binding.tilAmount.setError(getString(R.string.error_amount_or_volume));
            return;
        }
        binding.tilAmount.setError(null);

        Double odometer = parseNumber(binding.etOdometer.getText());
        if (odometer == null) {
            binding.tilOdometer.setError(getString(R.string.error_odometer_required));
            return;
        }
        binding.tilOdometer.setError(null);

        // 单价缺失时由金额与油量推算
        if (price == null && amount != null && volume != null && volume > 0) {
            price = round(amount / volume, 2);
        }

        RefuelRecord record = editing != null ? editing : new RefuelRecord();
        record.vehicleId = vehicleId;
        record.date = date.getTimeInMillis();
        record.fuelType = binding.actFuelType.getText().toString();
        record.amount = amount;
        record.volume = volume;
        record.price = price;
        record.odometer = odometer;
        record.fullTank = binding.switchFullTank.isChecked();
        record.station = text(binding.etStation);
        record.note = text(binding.etNote);

        viewModel.save(record);
        finish();
    }

    private String text(TextInputEditText et) {
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private Double parseNumber(CharSequence cs) {
        if (cs == null || cs.toString().trim().isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(cs.toString().trim());
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_record_title)
                .setMessage(R.string.confirm_delete_record_msg)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    viewModel.delete(editing);
                    finish();
                })
                .show();
    }
}
