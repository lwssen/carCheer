package com.carcheer.app.ui.history;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
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
import com.carcheer.app.databinding.FragmentHistoryBinding;
import com.carcheer.app.util.HistoryRangeCalculator;
import com.carcheer.app.util.StatsCalculator;
import com.carcheer.app.viewmodel.HistoryViewModel;
import com.carcheer.app.viewmodel.RecordViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private RecordViewModel recordViewModel;
    private HistoryAdapter adapter;
    private FragmentHistoryBinding binding;
    private boolean listMode = true;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);
        recordViewModel = new ViewModelProvider(requireActivity()).get(RecordViewModel.class);
        adapter = new HistoryAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);

        setupTimeTypeSpinner();
        setupDataTypeSpinner();
        setupDatePickers();
        setupModeToggle();

        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getExtremes().observe(getViewLifecycleOwner(), this::renderExtremes);

        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        // 车辆与记录页联动
        recordViewModel.getVehicleId().observe(getViewLifecycleOwner(), id -> {
            if (id != null) {
                viewModel.setVehicleId(id);
            }
        });

        // 日期文本跟随查询区间
        viewModel.getStartLive().observe(getViewLifecycleOwner(), this::renderDateTexts);
        viewModel.getEndLive().observe(getViewLifecycleOwner(), v -> renderDateTexts(null));

        viewModel.getPagedRecords().observe(getViewLifecycleOwner(), list -> {
            boolean highlight = viewModel.getDataType() != HistoryViewModel.DATA_TYPE_ALL;
            adapter.submitList(list, highlight);
            updateEmptyState();
        });
        viewModel.getEmptyHint().observe(getViewLifecycleOwner(), v -> updateEmptyState());
        viewModel.getTableVersion().observe(getViewLifecycleOwner(), v -> viewModel.resetPaging());

        if (viewModel.getStartLive().getValue() == null) {
            viewModel.setTimeType(HistoryRangeCalculator.TYPE_THIS_MONTH);
        }
    }

    private void setupTimeTypeSpinner() {
        String[] labels = {
                getString(R.string.time_type_month),
                getString(R.string.time_type_3months),
                getString(R.string.time_type_half_year),
                getString(R.string.time_type_custom)
        };
        ArrayAdapter<String> adapterTime = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTimeType.setAdapter(adapterTime);
        binding.spinnerTimeType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == HistoryRangeCalculator.TYPE_CUSTOM) {
                    return;
                }
                viewModel.setTimeType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupDataTypeSpinner() {
        String[] labels = {
                getString(R.string.data_type_all),
                getString(R.string.data_type_max_price),
                getString(R.string.data_type_max_amount)
        };
        ArrayAdapter<String> adapterData = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapterData.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDataType.setAdapter(adapterData);
        binding.spinnerDataType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                viewModel.setDataType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupDatePickers() {
        binding.tvStartDate.setOnClickListener(v -> pickDate(true));
        binding.tvEndDate.setOnClickListener(v -> pickDate(false));
    }

    private void pickDate(final boolean isStart) {
        Long current = isStart ? viewModel.getStartLive().getValue() : viewModel.getEndLive().getValue();
        Calendar cal = Calendar.getInstance();
        if (current != null) {
            cal.setTimeInMillis(current);
        }
        new DatePickerDialog(requireContext(),
                (picker, year, month, day) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.clear();
                    picked.set(year, month, day, 0, 0, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    if (isStart) {
                        viewModel.setCustomRange(picked.getTimeInMillis(), getEndOrDefault());
                    } else {
                        // 结束日期取当天 23:59:59.999，保证边界包含
                        picked.set(Calendar.HOUR_OF_DAY, 23);
                        picked.set(Calendar.MINUTE, 59);
                        picked.set(Calendar.SECOND, 59);
                        picked.set(Calendar.MILLISECOND, 999);
                        viewModel.setCustomRange(getStartOrDefault(), picked.getTimeInMillis());
                    }
                    // 手动改日期后同步切到"自定义"，保证再次选择预设类型能触发刷新
                    binding.spinnerTimeType.setSelection(HistoryRangeCalculator.TYPE_CUSTOM);
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private long getStartOrDefault() {
        Long v = viewModel.getStartLive().getValue();
        return v != null ? v : 0;
    }

    private long getEndOrDefault() {
        Long v = viewModel.getEndLive().getValue();
        if (v != null) {
            return v;
        }
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private void setupModeToggle() {
        binding.tvModeList.setOnClickListener(v -> setMode(true));
        binding.tvModeStats.setOnClickListener(v -> setMode(false));
        setMode(true);
    }

    private void setMode(boolean isListMode) {
        listMode = isListMode;
        int primary = attrColor(binding.tvModeList, androidx.appcompat.R.attr.colorPrimary);
        int variant = attrColor(binding.tvModeList,
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        binding.tvModeList.setTextColor(isListMode ? primary : variant);
        binding.tvModeList.setTypeface(null, isListMode ? Typeface.BOLD : Typeface.NORMAL);
        binding.tvModeStats.setTextColor(isListMode ? variant : primary);
        binding.tvModeStats.setTypeface(null, isListMode ? Typeface.NORMAL : Typeface.BOLD);
        binding.rvHistory.setVisibility(isListMode ? View.VISIBLE : View.GONE);
        binding.cardSummary.setVisibility(isListMode ? View.GONE : View.VISIBLE);
        updateEmptyState();
    }

    private void renderSummary(StatsCalculator.Overview ov) {
        if (ov == null) {
            return;
        }
        binding.tvSummaryCount.setText(getString(R.string.count_fmt, ov.count));
        binding.tvSummaryAmount.setText(String.format(Locale.CHINA, "¥%.2f", ov.totalAmount));
        binding.tvSummaryVolume.setText(String.format(Locale.CHINA, "%.2f L", ov.totalVolume));
        binding.tvSummaryAvg.setText(ov.avgConsumption == null
                ? getString(R.string.stats_no_data)
                : String.format(Locale.CHINA, "%.2f", ov.avgConsumption));
    }

    private void renderExtremes(StatsCalculator.RangeExtremes ex) {
        if (ex == null) {
            return;
        }
        binding.tvSummaryMaxPrice.setText(ex.maxPrice == null
                ? getString(R.string.stats_no_data)
                : String.format(Locale.CHINA, "¥%.2f/L", ex.maxPrice));
        binding.tvSummaryMaxAmount.setText(ex.maxAmount == null
                ? getString(R.string.stats_no_data)
                : String.format(Locale.CHINA, "¥%.2f", ex.maxAmount));
        binding.tvSummaryMaxPriceDate.setText(ex.maxPriceDate == null
                ? getString(R.string.stats_no_data)
                : dateFormat.format(new Date(ex.maxPriceDate)));
        binding.tvSummaryMaxAmountDate.setText(ex.maxAmountDate == null
                ? getString(R.string.stats_no_data)
                : dateFormat.format(new Date(ex.maxAmountDate)));
    }

    private static int attrColor(View v, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        v.getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void renderDateTexts(@Nullable Long ignored) {
        Long start = viewModel.getStartLive().getValue();
        Long end = viewModel.getEndLive().getValue();
        binding.tvStartDate.setText(start != null ? dateFormat.format(new Date(start)) : getString(R.string.history_start_date));
        binding.tvEndDate.setText(end != null ? dateFormat.format(new Date(end)) : getString(R.string.history_end_date));
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItems().isEmpty();
        boolean showEmpty = listMode && empty;
        binding.tvEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        if (showEmpty) {
            Integer hint = viewModel.getEmptyHint().getValue();
            binding.tvEmpty.setText(hint != null && hint == 2
                    ? R.string.history_empty_valid_data
                    : R.string.history_empty_range);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
