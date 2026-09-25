package com.carcheer.app.ui.mine;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.carcheer.app.R;
import com.carcheer.app.databinding.FragmentMineBinding;
import com.carcheer.app.ui.vehicle.VehicleManageActivity;
import com.carcheer.app.util.ThemeSettings;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MineFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentMineBinding binding = FragmentMineBinding.inflate(inflater, container, false);

        binding.cardVehicleManage.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), VehicleManageActivity.class)));

        binding.cardDataManage.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DataManageActivity.class)));

        binding.cardTheme.setOnClickListener(v -> showThemeDialog());

        binding.cardLanguage.setOnClickListener(v -> showLanguageDialog());

        binding.cardAbout.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.about_title)
                        .setMessage(getString(R.string.about_message, "1.0"))
                        .setPositiveButton(R.string.action_ok, null)
                        .show());

        return binding.getRoot();
    }

    /** 主题颜色选择：保存偏好后重建当前 Activity，所有页面按新主题重新加载 */
    private void showThemeDialog() {
        String[] names = getResources().getStringArray(R.array.theme_names);
        int current = ThemeSettings.getThemeIndex(requireContext());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_theme_title)
                .setSingleChoiceItems(names, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != current) {
                        ThemeSettings.setThemeIndex(requireContext(), which);
                        requireActivity().recreate();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** 语言切换：仅中文/英文，appcompat per-app locales 自动持久化并重建页面 */
    private void showLanguageDialog() {
        String[] names = {
                getString(R.string.settings_lang_zh),
                getString(R.string.settings_lang_en)
        };
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        String lang;
        if (!current.isEmpty() && current.get(0) != null) {
            lang = current.get(0).getLanguage();
        } else {
            // 跟随系统时按系统语言高亮，避免选中项被去重逻辑拦截
            lang = getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        int checked = "en".equals(lang) ? 1 : 0;
        final int currentIndex = checked;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_language_title)
                .setSingleChoiceItems(names, currentIndex, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == currentIndex) {
                        return;
                    }
                    if (which == 0) {
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags("zh"));
                    } else {
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags("en"));
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
