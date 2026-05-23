package com.autostarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "Boot completed received");

            QueueManager qm = new QueueManager(context);
            String trigger = qm.getTriggerMode();

            if (qm.isEnabled() && ("boot".equals(trigger) || "both".equals(trigger))) {
                Log.d(TAG, "Starting app launcher service");
                Intent serviceIntent = new Intent(context, AppLauncherService.class);
                context.startService(serviceIntent);
            } else {
                Log.d(TAG, "Queue disabled or trigger mode doesn't include boot");
            }
        }
    }
}
