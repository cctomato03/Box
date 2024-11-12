package com.github.tvbox.osc.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.databinding.ActivityMainBinding;
import com.github.tvbox.osc.ui.fragment.FilesFragment;
import com.github.tvbox.osc.ui.fragment.HomeFragment;
import com.github.tvbox.osc.ui.fragment.LiveFragment;
import com.github.tvbox.osc.ui.fragment.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {

    private final FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FragmentTransaction firstFragmentTransaction = fragmentManager.beginTransaction();
        firstFragmentTransaction.replace(R.id.mainContentView, new HomeFragment());
        firstFragmentTransaction.commit();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.mainContentView, new HomeFragment());
                fragmentTransaction.commit();
                return true;
            } else if (item.getItemId() == R.id.navigation_file) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.mainContentView, new FilesFragment());
                fragmentTransaction.commit();
                return true;
            } else if (item.getItemId() == R.id.navigation_live) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.mainContentView, new LiveFragment());
                fragmentTransaction.commit();
                return true;
            } else if (item.getItemId() == R.id.navigation_setting) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.mainContentView, new SettingsFragment());
                fragmentTransaction.commit();
                return true;
            } else {
                return false;
            }
        });
    }

}
