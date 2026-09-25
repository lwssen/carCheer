package com.carcheer.app.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.carcheer.app.R;
import com.carcheer.app.databinding.ActivityMainBinding;
import com.carcheer.app.ui.history.HistoryFragment;
import com.carcheer.app.ui.mine.MineFragment;
import com.carcheer.app.ui.record.RecordListFragment;
import com.carcheer.app.ui.stats.StatsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.carcheer.app.util.ThemeSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_records) {
                switchTitle(getString(R.string.nav_records));
                show(RecordListFragment.class);
                return true;
            } else if (id == R.id.nav_stats) {
                switchTitle(getString(R.string.nav_stats));
                show(StatsFragment.class);
                return true;
            } else if (id == R.id.nav_history) {
                switchTitle(getString(R.string.nav_history));
                show(HistoryFragment.class);
                return true;
            } else if (id == R.id.nav_mine) {
                switchTitle(getString(R.string.nav_mine));
                show(MineFragment.class);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_records);
        } else {
            // recreate（如主题/语言变更）后按当前选中项恢复标题
            syncTitle(binding.bottomNav.getSelectedItemId());
        }
    }

    private void syncTitle(int navId) {
        if (navId == R.id.nav_stats) {
            binding.toolbar.setTitle(R.string.nav_stats);
        } else if (navId == R.id.nav_history) {
            binding.toolbar.setTitle(R.string.nav_history);
        } else if (navId == R.id.nav_mine) {
            binding.toolbar.setTitle(R.string.nav_mine);
        } else {
            binding.toolbar.setTitle(R.string.nav_records);
        }
    }

    private void switchTitle(String title) {
        binding.toolbar.setTitle(title);
    }

    private void show(Class<? extends Fragment> clazz) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentById(R.id.fragment_container);
        if (current != null && clazz.isInstance(current)) {
            return;
        }
        FragmentTransaction tx = fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, clazz, null);
        tx.commit();
    }
}
