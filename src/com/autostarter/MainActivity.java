package com.autostarter;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private QueueManager queueManager;
    private List<QueueManager.QueueItem> queue;
    private LinearLayout layoutQueue;
    private TextView tvEmpty;
    private Switch switchEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queueManager = new QueueManager(this);
        queue = queueManager.getQueue();

        // Enable switch
        switchEnabled = (Switch) findViewById(R.id.switchEnabled);
        switchEnabled.setChecked(queueManager.isEnabled());
        switchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                queueManager.setEnabled(isChecked);
                Toast.makeText(MainActivity.this,
                    isChecked ? "AutoStarter enabled" : "AutoStarter disabled",
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Trigger mode radio group
        RadioGroup rgTrigger = (RadioGroup) findViewById(R.id.rgTrigger);
        String triggerMode = queueManager.getTriggerMode();
        if ("boot".equals(triggerMode)) {
            ((RadioButton) findViewById(R.id.rbBoot)).setChecked(true);
        } else if ("bluetooth".equals(triggerMode)) {
            ((RadioButton) findViewById(R.id.rbBluetooth)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbBoth)).setChecked(true);
        }
        rgTrigger.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbBoot) {
                    queueManager.setTriggerMode("boot");
                } else if (checkedId == R.id.rbBluetooth) {
                    queueManager.setTriggerMode("bluetooth");
                } else {
                    queueManager.setTriggerMode("both");
                }
            }
        });

        // BT device picker
        Button btnPickBt = (Button) findViewById(R.id.btnPickBt);
        btnPickBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBtDevicePicker();
            }
        });
        updateBtDeviceLabel();

        // Queue container (LinearLayout instead of ListView)
        layoutQueue = (LinearLayout) findViewById(R.id.layoutQueue);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);

        // Add app button
        Button btnAdd = (Button) findViewById(R.id.btnAddApp);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppPicker();
            }
        });

        // Test button
        Button btnTest = (Button) findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (queue.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Queue is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Temporarily force enable for test
                boolean wasEnabled = queueManager.isEnabled();
                queueManager.setEnabled(true);
                Toast.makeText(MainActivity.this, "Running queue...", Toast.LENGTH_SHORT).show();
                Intent serviceIntent = new Intent(MainActivity.this, AppLauncherService.class);
                startService(serviceIntent);
                if (!wasEnabled) {
                    queueManager.setEnabled(false);
                }
            }
        });

        refreshQueue();
    }

    private void refreshQueue() {
        queue = queueManager.getQueue();
        layoutQueue.removeAllViews();

        if (queue.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            layoutQueue.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        layoutQueue.setVisibility(View.VISIBLE);

        final PackageManager pm = getPackageManager();

        for (int i = 0; i < queue.size(); i++) {
            final int position = i;
            QueueManager.QueueItem item = queue.get(i);

            View itemView = getLayoutInflater().inflate(R.layout.item_queue, layoutQueue, false);

            // Order number
            TextView tvOrder = (TextView) itemView.findViewById(R.id.tvOrder);
            tvOrder.setText(String.valueOf(position + 1));

            // App icon
            ImageView ivIcon = (ImageView) itemView.findViewById(R.id.ivAppIcon);
            try {
                Drawable icon = pm.getApplicationIcon(item.packageName);
                ivIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                ivIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            // App name
            TextView tvName = (TextView) itemView.findViewById(R.id.tvAppName);
            tvName.setText(item.appName);

            // Package name
            TextView tvPkg = (TextView) itemView.findViewById(R.id.tvPackage);
            tvPkg.setText(item.packageName);

            // Close behavior
            TextView tvClose = (TextView) itemView.findViewById(R.id.tvCloseAfter);
            tvClose.setText(item.getCloseLabel());
            tvClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCloseTimePicker(position);
                }
            });

            // Move up
            ImageButton btnUp = (ImageButton) itemView.findViewById(R.id.btnMoveUp);
            btnUp.setEnabled(position > 0);
            btnUp.setAlpha(position > 0 ? 1.0f : 0.3f);
            btnUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    queueManager.moveItem(position, position - 1);
                    refreshQueue();
                }
            });

            // Move down
            ImageButton btnDown = (ImageButton) itemView.findViewById(R.id.btnMoveDown);
            btnDown.setEnabled(position < queue.size() - 1);
            btnDown.setAlpha(position < queue.size() - 1 ? 1.0f : 0.3f);
            btnDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    queueManager.moveItem(position, position + 1);
                    refreshQueue();
                }
            });

            // Remove
            ImageButton btnRemove = (ImageButton) itemView.findViewById(R.id.btnRemove);
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Remove App")
                        .setMessage("Remove " + queue.get(position).appName + " from queue?")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                queueManager.removeFromQueue(position);
                                refreshQueue();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            });

            layoutQueue.addView(itemView);
        }
    }

    private void showAppPicker() {
        final PackageManager pm = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

        Set<String> queuedPkgs = new HashSet<String>();
        for (QueueManager.QueueItem qi : queue) {
            queuedPkgs.add(qi.packageName);
        }

        final List<ResolveInfo> available = new ArrayList<ResolveInfo>();
        for (ResolveInfo ri : resolveInfos) {
            String pkg = ri.activityInfo.packageName;
            if (!queuedPkgs.contains(pkg) && !pkg.equals(getPackageName())) {
                available.add(ri);
            }
        }

        Collections.sort(available, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                String nameA = a.loadLabel(pm).toString();
                String nameB = b.loadLabel(pm).toString();
                return nameA.compareToIgnoreCase(nameB);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select App to Add");

        final BaseAdapter appAdapter = new BaseAdapter() {
            @Override
            public int getCount() { return available.size(); }

            @Override
            public Object getItem(int pos) { return available.get(pos); }

            @Override
            public long getItemId(int pos) { return pos; }

            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_app_picker, parent, false);
                }
                ResolveInfo ri = available.get(pos);
                ImageView iv = (ImageView) convertView.findViewById(R.id.ivPickerIcon);
                TextView tvName = (TextView) convertView.findViewById(R.id.tvPickerName);
                TextView tvPkg = (TextView) convertView.findViewById(R.id.tvPickerPkg);

                iv.setImageDrawable(ri.loadIcon(pm));
                tvName.setText(ri.loadLabel(pm));
                tvPkg.setText(ri.activityInfo.packageName);
                return convertView;
            }
        };

        builder.setAdapter(appAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ResolveInfo ri = available.get(which);
                QueueManager.QueueItem item = new QueueManager.QueueItem();
                item.packageName = ri.activityInfo.packageName;
                item.appName = ri.loadLabel(pm).toString();
                item.closeAfterSeconds = 0;
                item.delayBeforeNextSeconds = 2;
                queueManager.addToQueue(item);
                refreshQueue();
                Toast.makeText(MainActivity.this,
                    item.appName + " added to queue", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCloseTimePicker(final int position) {
        final String[] options = {
            "Don't close (keep running)",
            "Close immediately",
            "Close after 3 seconds",
            "Close after 5 seconds",
            "Close after 10 seconds",
            "Close after 15 seconds",
            "Close after 30 seconds",
            "Close after 60 seconds"
        };
        final int[] values = { 0, -1, 3, 5, 10, 15, 30, 60 };

        int currentValue = queue.get(position).closeAfterSeconds;
        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentValue) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
            .setTitle("Close behavior: " + queue.get(position).appName)
            .setSingleChoiceItems(options, selectedIndex, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    queueManager.updateCloseAfter(position, values[which]);
                    refreshQueue();
                    dialog.dismiss();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showBtDevicePicker() {
        try {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter == null) {
                Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
                return;
            }

            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            if (pairedDevices.isEmpty()) {
                Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show();
                return;
            }

            final List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>(pairedDevices);
            List<String> names = new ArrayList<String>();
            names.add("Any Bluetooth device");
            for (BluetoothDevice dev : deviceList) {
                String name = dev.getName();
                if (name == null) name = "Unknown";
                names.add(name + "\n" + dev.getAddress());
            }

            String[] namesArr = names.toArray(new String[0]);

            new AlertDialog.Builder(this)
                .setTitle("Select Bluetooth Device")
                .setItems(namesArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            queueManager.setBtDeviceAddress("");
                        } else {
                            BluetoothDevice dev = deviceList.get(which - 1);
                            queueManager.setBtDeviceAddress(dev.getAddress());
                        }
                        updateBtDeviceLabel();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBtDeviceLabel() {
        TextView tvBt = (TextView) findViewById(R.id.tvBtDevice);
        String addr = queueManager.getBtDeviceAddress();
        if (addr == null || addr.isEmpty()) {
            tvBt.setText("Trigger on: Any BT device");
        } else {
            try {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                if (btAdapter != null) {
                    BluetoothDevice dev = btAdapter.getRemoteDevice(addr);
                    String name = dev.getName();
                    if (name != null) {
                        tvBt.setText("Trigger on: " + name);
                        return;
                    }
                }
            } catch (Exception e) { }
            tvBt.setText("Trigger on: " + addr);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshQueue();
    }
}
