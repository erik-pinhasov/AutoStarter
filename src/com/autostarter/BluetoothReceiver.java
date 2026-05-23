package com.autostarter;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            QueueManager qm = new QueueManager(context);
            String trigger = qm.getTriggerMode();

            if (!qm.isEnabled()) {
                Log.d(TAG, "Queue disabled");
                return;
            }

            if (!"bluetooth".equals(trigger) && !"both".equals(trigger)) {
                Log.d(TAG, "Trigger mode doesn't include bluetooth");
                return;
            }

            // Check if specific BT device is configured
            String targetAddress = qm.getBtDeviceAddress();
            if (targetAddress != null && !targetAddress.isEmpty() && device != null) {
                if (!targetAddress.equals(device.getAddress())) {
                    Log.d(TAG, "Connected to different BT device, ignoring");
                    return;
                }
            }

            Log.d(TAG, "Bluetooth connected, starting launcher");
            Intent serviceIntent = new Intent(context, AppLauncherService.class);
            context.startService(serviceIntent);
        }
    }
}
