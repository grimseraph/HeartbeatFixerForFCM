package io.github.grimseraph.heartbeatfixerforgcm;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private MaterialSwitch mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwitch = findViewById(R.id.fixer_switch);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync the switch without triggering the listener when returning
        // from system settings screens.
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch.setChecked(HeartbeatFixerUtils.isHeartbeatFixerEnabled(this));
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        HeartbeatFixerUtils.setHeartbeatFixerEnabled(this, isChecked);
        toastHeartbeatFixerState(isChecked);
        if (isChecked && !HeartbeatFixerUtils.canScheduleExactAlarms(this)) {
            requestExactAlarmPermission();
        }
    }

    private void toastHeartbeatFixerState(boolean enabled) {
        final int msgResId = enabled ? R.string.toast_heartbeat_fixer_on : R.string.toast_heartbeat_fixer_off;
        Snackbar.make(findViewById(R.id.container), msgResId, Snackbar.LENGTH_SHORT).show();
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Snackbar.make(findViewById(R.id.container), R.string.snackbar_exact_alarm_needed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_grant, v -> startActivity(
                            new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + getPackageName()))))
                    .show();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        public static final String PREF_GCM_HEARTBEAT_INTERVAL_WIFI = "pref_gcm_heartbeat_interval_wifi";
        public static final String PREF_GCM_HEARTBEAT_INTERVAL_MOBILE = "pref_gcm_heartbeat_interval_mobile";
        public static final String PREF_EXACT_ALARM = "pref_exact_alarm";
        public static final String PREF_BATTERY_OPTIMIZATION = "pref_battery_optimization";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            ListPreference intervalWifi = findPreference(PREF_GCM_HEARTBEAT_INTERVAL_WIFI);
            ListPreference intervalMobile = findPreference(PREF_GCM_HEARTBEAT_INTERVAL_MOBILE);
            intervalWifi.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            intervalMobile.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

            Preference exactAlarm = findPreference(PREF_EXACT_ALARM);
            exactAlarm.setOnPreferenceClickListener(preference -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + requireContext().getPackageName())));
                }
                return true;
            });

            Preference batteryOptimization = findPreference(PREF_BATTERY_OPTIMIZATION);
            batteryOptimization.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                return true;
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePermissionStates();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PREF_GCM_HEARTBEAT_INTERVAL_WIFI.equals(key)
                    || PREF_GCM_HEARTBEAT_INTERVAL_MOBILE.equals(key)) {
                sendHeartbeatRequestIfAllows();
            }
        }

        private void updatePermissionStates() {
            Context context = requireContext();

            Preference exactAlarm = findPreference(PREF_EXACT_ALARM);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                boolean granted = alarmManager.canScheduleExactAlarms();
                exactAlarm.setSummary(granted
                        ? R.string.pref_exact_alarm_summary_granted
                        : R.string.pref_exact_alarm_summary_denied);
                exactAlarm.setEnabled(!granted);
            } else {
                exactAlarm.setVisible(false);
            }

            Preference batteryOptimization = findPreference(PREF_BATTERY_OPTIMIZATION);
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean ignoring = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            batteryOptimization.setSummary(ignoring
                    ? R.string.pref_battery_optimization_summary_ignored
                    : R.string.pref_battery_optimization_summary_optimized);
        }

        private void sendHeartbeatRequestIfAllows() {
            Context context = requireContext();
            if (HeartbeatFixerUtils.isHeartbeatFixerEnabled(context)) {
                HeartbeatFixerUtils.sendHeartbeatRequest(context);
            }
        }
    }
}
