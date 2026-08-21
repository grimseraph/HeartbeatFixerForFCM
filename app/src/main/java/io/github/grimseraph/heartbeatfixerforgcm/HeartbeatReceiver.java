package io.github.grimseraph.heartbeatfixerforgcm;

import static io.github.grimseraph.heartbeatfixerforgcm.HeartbeatFixerUtils.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by shaobin on 3/8/15.
 * Modernized in 2026: since Android 8.0 implicit broadcasts no longer reach
 * manifest receivers, so the heartbeat intents are sent explicitly to the
 * Google Play services (FCM) and legacy GSF packages.
 */
public class HeartbeatReceiver extends BroadcastReceiver {
    private static final String ACTION_GTALK_HEARTBEAT = "com.google.android.intent.action.GTALK_HEARTBEAT";
    private static final String ACTION_MCS_HEARTBEAT = "com.google.android.intent.action.MCS_HEARTBEAT";

    // FCM's push connection lives in Google Play services; GSF handles it on very old devices.
    private static final String[] TARGET_PACKAGES = {
            "com.google.android.gms",
            "com.google.android.gsf",
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (HeartbeatFixerUtils.isNetworkConnected(context)) {
            for (String targetPackage : TARGET_PACKAGES) {
                context.sendBroadcast(new Intent(ACTION_GTALK_HEARTBEAT).setPackage(targetPackage));
                context.sendBroadcast(new Intent(ACTION_MCS_HEARTBEAT).setPackage(targetPackage));
            }
            Log.d(TAG, "HeartbeatReceiver sent heartbeat request");
        } else {
            Log.d(TAG, "HeartbeatReceiver skipped, no network");
        }
        HeartbeatFixerUtils.scheduleHeartbeatRequest(context);
    }
}
