package com.github.tvbox.osc.ui.activity;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.databinding.ActivityMainBinding;
import com.github.tvbox.osc.ui.fragment.DriversFragment;
import com.github.tvbox.osc.ui.fragment.HomeFragment;
import com.github.tvbox.osc.ui.fragment.LiveFragment;
import com.github.tvbox.osc.ui.fragment.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {

    private static Resources res;

    private static final HomeFragment homeFragment = new HomeFragment();
    private static final DriversFragment driversFragment = new DriversFragment();
    private static final LiveFragment liveFragment = new LiveFragment();
    private static final SettingsFragment settingsFragment = new SettingsFragment();

    public static Resources getRes() {
        return res;
    }

    private final FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        res = getResources();

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FragmentTransaction firstFragmentTransaction = fragmentManager.beginTransaction();
        firstFragmentTransaction.add(R.id.mainContentView, homeFragment);
        firstFragmentTransaction.add(R.id.mainContentView, driversFragment);
        firstFragmentTransaction.add(R.id.mainContentView, liveFragment);
        firstFragmentTransaction.add(R.id.mainContentView, settingsFragment);
        firstFragmentTransaction.show(homeFragment);
        firstFragmentTransaction.hide(driversFragment);
        firstFragmentTransaction.hide(liveFragment);
        firstFragmentTransaction.hide(settingsFragment);
        firstFragmentTransaction.commit();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.show(homeFragment);
                fragmentTransaction.hide(driversFragment);
                fragmentTransaction.hide(liveFragment);
                fragmentTransaction.hide(settingsFragment);
                fragmentTransaction.commit();
                return true;
            } else if (item.getItemId() == R.id.navigation_file) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.hide(homeFragment);
                fragmentTransaction.show(driversFragment);
                fragmentTransaction.hide(liveFragment);
                fragmentTransaction.hide(settingsFragment);
                fragmentTransaction.commit();
                return true;
            } else if (item.getItemId() == R.id.navigation_live) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.hide(homeFragment);
                fragmentTransaction.hide(driversFragment);
                fragmentTransaction.show(liveFragment);
                fragmentTransaction.hide(settingsFragment);
                fragmentTransaction.commit();
                return true;
            } else if (item.getItemId() == R.id.navigation_setting) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.hide(homeFragment);
                fragmentTransaction.hide(driversFragment);
                fragmentTransaction.hide(liveFragment);
                fragmentTransaction.show(settingsFragment);
                fragmentTransaction.commit();
                return true;
            } else {
                return false;
            }
        });
    }

}
