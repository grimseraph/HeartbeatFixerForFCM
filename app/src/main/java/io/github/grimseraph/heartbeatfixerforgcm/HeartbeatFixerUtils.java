package io.github.grimseraph.heartbeatfixerforgcm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.preference.PreferenceManager;

/**
 * Created by shaobin on 3/8/15.
 * Modernized in 2026: exact-alarm permission handling (Android 12+),
 * Doze-aware scheduling and NetworkCapabilities based connectivity check.
 */
public class HeartbeatFixerUtils {
    public static final String TAG = "HeartbeatFixer";

    private static final String PREF_KEY_FIXER_STATE = "fixer_state";

    private HeartbeatFixerUtils() {}

    public static void scheduleHeartbeatRequest(Context context) {
        Log.d(TAG, "scheduleHeartbeatRequest");
        if (!isHeartbeatFixerEnabled(context)) {
            cancelHeartbeatRequest(context);
            return;
        }
        int intervalMillis = isOnWifi(context)
                ? getHeartbeatIntervalMillisForWifi(context)
                : getHeartbeatIntervalMillisForMobile(context);
        setNextHeartbeatRequest(context, intervalMillis);
    }

    private static void setNextHeartbeatRequest(Context context, int intervalMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;
        Log.d(TAG, "setNextHeartbeatRequest at: " + DateFormat.format("yyyy-MM-dd HH:mm:ss", triggerAtMillis));
        PendingIntent broadcastPendingIntent = getBroadcastPendingIntent(context);
        if (canScheduleExactAlarms(context)) {
            // Fires on time even in Doze (subject to the system's once-per-9-min quota).
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, broadcastPendingIntent);
        } else {
            // No exact-alarm permission: fall back to an inexact while-idle alarm.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, broadcastPendingIntent);
        }
    }

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager.canScheduleExactAlarms();
    }

    public static void cancelHeartbeatRequest(Context context) {
        Log.d(TAG, "cancelHeartbeatRequest");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getBroadcastPendingIntent(context));
    }

    private static PendingIntent getBroadcastPendingIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, HeartbeatReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static boolean isOnWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static void setHeartbeatFixerEnabled(Context context, boolean enabled) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(PREF_KEY_FIXER_STATE, enabled).apply();
        if (enabled) {
            sendHeartbeatRequest(context);
        } else {
            cancelHeartbeatRequest(context);
        }
    }

    public static boolean isHeartbeatFixerEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY_FIXER_STATE, false);
    }

    public static int getHeartbeatIntervalMillisForWifi(Context context) {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_gcm_heartbeat_interval_wifi", "5")) * 60000;
    }

    public static int getHeartbeatIntervalMillisForMobile(Context context) {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_gcm_heartbeat_interval_mobile", "5")) * 60000;
    }

    public static void sendHeartbeatRequest(Context context) {
        context.sendBroadcast(new Intent(context, HeartbeatReceiver.class));
    }
}
