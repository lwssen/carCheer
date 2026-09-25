package com.carcheer.app.ui.mine;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.carcheer.app.R;
import com.carcheer.app.databinding.ActivityDataManageBinding;
import com.carcheer.app.viewmodel.RecordViewModel;
import com.carcheer.app.viewmodel.VehicleViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataManageActivity extends AppCompatActivity {

    private RecordViewModel recordViewModel;
    private VehicleViewModel vehicleViewModel;
    private ActivityDataManageBinding binding;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),
                    uri -> {
                        if (uri != null) {
                            writeCsv(uri);
                        }
                    });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            readCsv(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.carcheer.app.util.ThemeSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityDataManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        recordViewModel = new ViewModelProvider(this).get(RecordViewModel.class);
        vehicleViewModel = new ViewModelProvider(this).get(VehicleViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.cardExport.setOnClickListener(v -> {
            String name = "carcheer_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(new Date())
                    + ".csv";
            exportLauncher.launch(name);
        });

        binding.cardImport.setOnClickListener(v ->
                importLauncher.launch(new String[]{"text/csv", "text/comma-separated-values",
                        "text/plain", "application/csv", "application/octet-stream"}));

        binding.cardClear.setOnClickListener(v -> confirmClearAll());
    }

    private void writeCsv(Uri uri) {
        recordViewModel.exportCsv(csv -> {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    toast(R.string.export_failed);
                    return;
                }
                // UTF-8 BOM：保证 Excel/WPS 直接打开中文不乱码
                out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                out.write(csv.getBytes(StandardCharsets.UTF_8));
                toast(R.string.export_done);
            } catch (Exception e) {
                toast(R.string.export_failed);
            }
        });
    }

    private void readCsv(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) > 0) {
                sb.append(buffer, 0, len);
            }
            String csv = sb.toString();
            if (csv.startsWith("﻿")) {
                csv = csv.substring(1);
            }
            recordViewModel.importCsv(csv, (inserted, created) -> toastWithString(
                    getString(R.string.import_done, inserted, created)));
        } catch (Exception e) {
            toast(R.string.import_failed);
        }
    }

    private void confirmClearAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.data_clear)
                .setMessage(R.string.confirm_clear_all_msg)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) ->
                        vehicleViewModel.clearAll(() -> toast(R.string.clear_done)))
                .show();
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void toastWithString(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
