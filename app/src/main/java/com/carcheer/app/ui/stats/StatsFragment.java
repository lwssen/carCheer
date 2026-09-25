package com.carcheer.app.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.carcheer.app.R;
import com.carcheer.app.data.entity.RefuelRecord;
import com.carcheer.app.databinding.FragmentStatsBinding;
import com.carcheer.app.databinding.ItemMonthStatBinding;
import com.carcheer.app.util.FuelCalculator;
import com.carcheer.app.util.HistoryRangeCalculator;
import com.carcheer.app.util.StatsCalculator;
import com.carcheer.app.viewmodel.RecordViewModel;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private static final int MAX_TREND_POINTS = 20;
    private static final SimpleDateFormat X_LABEL_FORMAT =
            new SimpleDateFormat("M/d", Locale.CHINA);

    /** 趋势图/月度汇总的时间范围下拉，与 HistoryRangeCalculator 类型对应 */
    private static final int[] RANGE_TYPES = {
            HistoryRangeCalculator.TYPE_THIS_MONTH,
            HistoryRangeCalculator.TYPE_LAST_3_MONTHS,
            HistoryRangeCalculator.TYPE_LAST_HALF_YEAR,
            HistoryRangeCalculator.TYPE_LAST_1_YEAR
    };
    private static final int DEFAULT_RANGE_POSITION = 1;

    private RecordViewModel viewModel;
    private FragmentStatsBinding binding;
    private List<RefuelRecord> latestAsc = new ArrayList<>();
    private FuelCalculator.Result latestFuel;
    private int rangePosition = DEFAULT_RANGE_POSITION;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(RecordViewModel.class);
        setupChart();
        setupRangeSpinner();

        viewModel.getVehicles().observe(getViewLifecycleOwner(), vehicles -> {
            boolean noVehicle = vehicles == null || vehicles.isEmpty();
            binding.tvEmptyStats.setVisibility(noVehicle ? View.VISIBLE : View.GONE);
            binding.cardOverview.setVisibility(noVehicle ? View.GONE : View.VISIBLE);
            binding.cardTrend.setVisibility(noVehicle ? View.GONE : View.VISIBLE);
            binding.cardMonthly.setVisibility(noVehicle ? View.GONE : View.VISIBLE);
        });

        viewModel.getRecords().observe(getViewLifecycleOwner(), records -> {
            List<RefuelRecord> asc = toAsc(records);
            boolean empty = asc.isEmpty();
            binding.tvEmptyStats.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) {
                binding.tvEmptyStats.setText(R.string.empty_records);
                return;
            }
            binding.tvEmptyStats.setVisibility(View.GONE);

            latestAsc = asc;
            latestFuel = FuelCalculator.calculate(asc);
            renderOverview(asc, latestFuel);
            refreshScopedViews();
        });
    }

    private void setupRangeSpinner() {
        String[] labels = {
                getString(R.string.time_type_month),
                getString(R.string.time_type_3months),
                getString(R.string.time_type_half_year),
                getString(R.string.time_type_1year)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRange.setAdapter(adapter);
        binding.spinnerRange.setSelection(DEFAULT_RANGE_POSITION, false);
        binding.spinnerRange.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v,
                                       int position, long id) {
                if (rangePosition == position) {
                    return;
                }
                rangePosition = position;
                refreshScopedViews();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    /** 按当前时间范围刷新趋势图与月度汇总（顶部概览卡片不受影响） */
    private void refreshScopedViews() {
        if (latestFuel == null) {
            return;
        }
        long rangeStart = HistoryRangeCalculator.range(
                RANGE_TYPES[rangePosition], System.currentTimeMillis())[0];
        renderTrend(latestAsc, latestFuel, rangeStart);
        renderMonthly(filterFrom(latestAsc, rangeStart));
    }

    private static List<RefuelRecord> filterFrom(List<RefuelRecord> asc, long rangeStart) {
        List<RefuelRecord> scoped = new ArrayList<>();
        for (RefuelRecord r : asc) {
            if (r.date >= rangeStart) {
                scoped.add(r);
            }
        }
        return scoped;
    }

    private void setupChart() {
        binding.lineChart.getDescription().setEnabled(false);
        binding.lineChart.getLegend().setEnabled(false);
        binding.lineChart.setScaleEnabled(false);
        binding.lineChart.setPinchZoom(false);
        binding.lineChart.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        binding.lineChart.getAxisRight().setEnabled(false);
        binding.lineChart.getAxisLeft().setAxisMinimum(0f);
        binding.lineChart.getAxisLeft().setDrawGridLines(true);
    }

    private void renderOverview(List<RefuelRecord> asc, FuelCalculator.Result fuelResult) {
        StatsCalculator.Overview ov = StatsCalculator.overview(asc, fuelResult);
        binding.tvTotalCount.setText(getString(R.string.count_fmt, ov.count));
        binding.tvTotalAmount.setText(
                String.format(Locale.CHINA, "¥%.2f", ov.totalAmount));
        binding.tvTotalVolume.setText(
                String.format(Locale.CHINA, "%.2f L", ov.totalVolume));
        binding.tvAvgConsumption.setText(ov.avgConsumption == null
                ? getString(R.string.stats_no_data)
                : String.format(Locale.CHINA, "%.2f", ov.avgConsumption));

        if (ov.latestPrice != null) {
            String text;
            if (ov.previousPrice != null) {
                double change = ov.latestPrice - ov.previousPrice;
                String changeText = String.format(Locale.CHINA, "%+.2f", change);
                text = getString(R.string.stats_price_change, ov.latestPrice, changeText);
            } else {
                text = getString(R.string.stats_price_latest_only, ov.latestPrice);
            }
            binding.tvPriceChange.setText(text);
            binding.tvPriceChange.setVisibility(View.VISIBLE);
        } else {
            binding.tvPriceChange.setVisibility(View.GONE);
        }
    }

    private void renderTrend(List<RefuelRecord> asc, FuelCalculator.Result fuelResult, long rangeStart) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (FuelCalculator.Segment segment : fuelResult.segments) {
            RefuelRecord record = findById(asc, segment.recordId);
            if (record == null || record.date < rangeStart) {
                continue;
            }
            Double consumption = fuelResult.consumptions.get(segment.recordId);
            if (consumption == null) {
                continue;
            }
            if (entries.size() >= MAX_TREND_POINTS) {
                break;
            }
            entries.add(new Entry(index, consumption.floatValue()));
            labels.add(X_LABEL_FORMAT.format(new Date(record.date)));
            index++;
        }

        if (entries.isEmpty()) {
            binding.lineChart.setVisibility(View.GONE);
            binding.tvTrendEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.lineChart.setVisibility(View.VISIBLE);
        binding.tvTrendEmpty.setVisibility(View.GONE);

        binding.lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(
                labels.toArray(new String[0])));
        binding.lineChart.getXAxis().setLabelCount(labels.size(), false);

        LineDataSet dataSet = new LineDataSet(entries, null);
        dataSet.setColor(0xFFB3261E);
        dataSet.setCircleColor(0xFFB3261E);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        binding.lineChart.setData(new LineData(dataSet));
        binding.lineChart.invalidate();
    }

    private void renderMonthly(List<RefuelRecord> asc) {
        binding.monthlyContainer.removeAllViews();

        List<StatsCalculator.MonthStat> monthly = StatsCalculator.monthly(asc);
        // 最新月份在前
        for (int i = monthly.size() - 1; i >= 0; i--) {
            StatsCalculator.MonthStat stat = monthly.get(i);
            ItemMonthStatBinding item =
                    ItemMonthStatBinding.inflate(getLayoutInflater(), binding.monthlyContainer, false);
            item.tvMonth.setText(stat.month);
            item.tvMonthAmount.setText(
                    String.format(Locale.CHINA, "¥%.2f", stat.amount));
            item.tvMonthVolume.setText(
                    String.format(Locale.CHINA, "%.2fL", stat.volume));
            item.tvMonthDistance.setText(
                    stat.distance > 0
                            ? String.format(Locale.CHINA, "%.0fkm", stat.distance)
                            : getString(R.string.stats_no_data));
            binding.monthlyContainer.addView(item.getRoot());
        }
    }

    private static RefuelRecord findById(List<RefuelRecord> records, long id) {
        for (RefuelRecord r : records) {
            if (r.id == id) {
                return r;
            }
        }
        return null;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
