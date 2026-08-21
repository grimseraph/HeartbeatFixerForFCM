package io.github.grimseraph.heartbeatfixerforgcm;

import static io.github.grimseraph.heartbeatfixerforgcm.HeartbeatFixerUtils.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Restores the heartbeat schedule after a reboot or app update.
 * Replaces the old NetworkStateReceiver: CONNECTIVITY_CHANGE is no longer
 * delivered to manifest receivers since Android 7.0, so the alarm chain
 * simply reschedules itself and checks connectivity at fire time.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Log.d(TAG, "BootReceiver, intent: " + intent);
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            HeartbeatFixerUtils.scheduleHeartbeatRequest(context);
        }
    }
}
