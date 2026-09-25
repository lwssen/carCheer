package com.carcheer.app.ui.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.data.entity.Vehicle;
import com.carcheer.app.databinding.FragmentRecordListBinding;
import com.carcheer.app.util.FuelCalculator;
import com.carcheer.app.util.SelectedVehiclePrefs;
import com.carcheer.app.viewmodel.RecordViewModel;

import java.util.ArrayList;
import java.util.List;

public class RecordListFragment extends Fragment {

    private RecordViewModel viewModel;
    private RecordAdapter adapter;
    private FragmentRecordListBinding binding;

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<RefuelRecord> allRecords = new ArrayList<>();
    private final List<Integer> yearValues = new ArrayList<>();
    private boolean updatingVehicleSpinner;
    private boolean updatingYearSpinner;
    private int selectedYear;
    private int fullTankFilter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecordListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(RecordViewModel.class);
        // 筛选状态以 ViewModel 为准（首次为当前年/全部，视图重建后与查询保持一致）
        selectedYear = viewModel.getYearFilter();
        fullTankFilter = viewModel.getFullTankFilter();
        adapter = new RecordAdapter(this::openEdit);
        binding.rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecords.setAdapter(adapter);

        binding.spinnerVehicle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (updatingVehicleSpinner || position < 0 || position >= vehicles.size()) {
                    return;
                }
                long vehicleId = vehicles.get(position).id;
                saveSelectedVehicle(vehicleId);
                viewModel.setVehicleId(vehicleId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.fabAddRecord.setOnClickListener(v -> {
            Vehicle current = currentVehicle();
            if (current == null) {
                return;
            }
            startActivity(RecordEditActivity.newIntent(requireContext(), current.id, 0));
        });

        viewModel.getVehicles().observe(getViewLifecycleOwner(), list -> {
            vehicles.clear();
            if (list != null) {
                vehicles.addAll(list);
            }
            updateSpinner();
            updateEmptyState();
        });

        viewModel.getRecords().observe(getViewLifecycleOwner(), records -> {
            allRecords.clear();
            if (records != null) {
                allRecords.addAll(records);
            }
            // 油耗结算基于该车全量记录，筛选和分页只影响展示
            adapter.setCalculation(FuelCalculator.calculate(toAsc(allRecords)));
            updateYearSpinner();
            updateEmptyState();
        });

        // 分页列表：触底翻页后累加展示
        viewModel.getPagedRecords().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            updateEmptyState();
        });
        // 数据增删改后重置分页，从第一页重新查询
        viewModel.getTableVersion().observe(getViewLifecycleOwner(), v -> viewModel.resetPaging());

        binding.rvRecords.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) {
                    return;
                }
                if (!recyclerView.canScrollVertically(1)
                        && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 1) {
                    viewModel.loadNextPage();
                }
            }
        });

        setupYearSpinnerListener();
        setupFullTankSpinner();
    }

    private void setupYearSpinnerListener() {
        binding.spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (updatingYearSpinner || position < 0 || position >= yearValues.size()) {
                    return;
                }
                int year = yearValues.get(position);
                if (selectedYear == year) {
                    return;
                }
                selectedYear = year;
                viewModel.setYearFilter(year);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupFullTankSpinner() {
        String[] labels = {
                getString(R.string.filter_full_all),
                getString(R.string.filter_full_no),
                getString(R.string.record_full_tank)
        };
        ArrayAdapter<String> adapterFull = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapterFull.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFull.setAdapter(adapterFull);
        binding.spinnerFull.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (fullTankFilter == position) {
                    return;
                }
                fullTankFilter = position;
                viewModel.setFullTankFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /** 年份选项 = 全部 ∪ 当前车辆记录出现过的年份 ∪ 系统当前年，年份部分倒序 */
    private void updateYearSpinner() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        yearValues.clear();
        yearValues.add(0);
        for (RefuelRecord r : allRecords) {
            cal.setTimeInMillis(r.date);
            int year = cal.get(java.util.Calendar.YEAR);
            if (!yearValues.contains(year)) {
                yearValues.add(year);
            }
        }
        if (yearValues.size() > 1) {
            List<Integer> years = new ArrayList<>(yearValues.subList(1, yearValues.size()));
            java.util.Collections.sort(years, java.util.Collections.reverseOrder());
            yearValues.clear();
            yearValues.add(0);
            yearValues.addAll(years);
        }
        int currentYear = cal.get(java.util.Calendar.YEAR);
        if (!yearValues.contains(currentYear)) {
            yearValues.add(currentYear);
        }

        List<String> labels = new ArrayList<>();
        for (int year : yearValues) {
            labels.add(year == 0 ? getString(R.string.filter_full_all) : year + "年");
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerYear.setAdapter(yearAdapter);

        updatingYearSpinner = true;
        if (!yearValues.contains(selectedYear)) {
            selectedYear = currentYear;
        }
        for (int i = 0; i < yearValues.size(); i++) {
            if (yearValues.get(i) == selectedYear) {
                binding.spinnerYear.setSelection(i);
                break;
            }
        }
        updatingYearSpinner = false;
    }

    @Nullable
    private Vehicle currentVehicle() {
        long selected = getSelectedVehicle();
        for (Vehicle v : vehicles) {
            if (v.id == selected) {
                return v;
            }
        }
        return vehicles.isEmpty() ? null : vehicles.get(0);
    }

    private void updateSpinner() {
        updatingVehicleSpinner = true;
        List<String> names = new ArrayList<>();
        for (Vehicle v : vehicles) {
            names.add(v.name);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerVehicle.setAdapter(spinnerAdapter);

        if (!vehicles.isEmpty()) {
            binding.spinnerVehicle.setSelection(initialSelectionIndex());
            viewModel.setVehicleId(vehicles.get(initialSelectionIndex()).id);
        }
        binding.spinnerVehicle.setVisibility(vehicles.isEmpty() ? View.GONE : View.VISIBLE);
        updatingVehicleSpinner = false;
    }

    /** 初始选中：上次手动选择 > 默认车辆 > 第一辆 */
    private int initialSelectionIndex() {
        long saved = SelectedVehiclePrefs.get(requireContext());
        for (int i = 0; i < vehicles.size(); i++) {
            if (vehicles.get(i).id == saved) {
                return i;
            }
        }
        for (int i = 0; i < vehicles.size(); i++) {
            if (vehicles.get(i).isDefault) {
                return i;
            }
        }
        return 0;
    }

    private void updateEmptyState() {
        boolean noVehicle = vehicles.isEmpty();
        List<RefuelRecord> records = adapter.getItems();
        boolean noRecord = records.isEmpty();
        boolean show = noVehicle || noRecord;
        binding.tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvRecords.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.fabAddRecord.setVisibility(noVehicle ? View.GONE : View.VISIBLE);
        if (noVehicle) {
            binding.tvEmpty.setText(R.string.empty_records_no_vehicle);
        } else if (allRecords.isEmpty()) {
            binding.tvEmpty.setText(R.string.empty_records);
        } else {
            binding.tvEmpty.setText(R.string.filter_empty_result);
        }
    }

    private static List<RefuelRecord> toAsc(List<RefuelRecord> desc) {
        List<RefuelRecord> asc = new ArrayList<>(desc == null ? 0 : desc.size());
        if (desc != null) {
            for (int i = desc.size() - 1; i >= 0; i--) {
                asc.add(desc.get(i));
            }
        }
        return asc;
    }

    private long getSelectedVehicle() {
        return SelectedVehiclePrefs.get(requireContext());
    }

    private void saveSelectedVehicle(long id) {
        SelectedVehiclePrefs.set(requireContext(), id);
    }

    private void openEdit(RefuelRecord record) {
        Vehicle current = currentVehicle();
        startActivity(RecordEditActivity.newIntent(requireContext(),
                current == null ? 0 : current.id, record.id));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
