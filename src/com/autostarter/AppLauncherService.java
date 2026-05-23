package com.autostarter;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.util.List;

public class AppLauncherService extends Service {

    private static final String TAG = "AppLauncherService";
    private Handler handler;
    private List<QueueManager.QueueItem> queue;
    private int currentIndex = 0;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) {
            Log.d(TAG, "Already running, ignoring");
            return START_NOT_STICKY;
        }

        QueueManager qm = new QueueManager(this);
        if (!qm.isEnabled()) {
            Log.d(TAG, "Queue is disabled");
            stopSelf();
            return START_NOT_STICKY;
        }

        queue = qm.getQueue();
        if (queue.isEmpty()) {
            Log.d(TAG, "Queue is empty");
            stopSelf();
            return START_NOT_STICKY;
        }

        isRunning = true;
        currentIndex = 0;
        Log.d(TAG, "Starting queue with " + queue.size() + " apps");

        // Small initial delay to let the system settle
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                launchNext();
            }
        }, 500);

        return START_NOT_STICKY;
    }

    private void launchNext() {
        if (currentIndex >= queue.size()) {
            Log.d(TAG, "Queue complete");
            isRunning = false;
            stopSelf();
            return;
        }

        final QueueManager.QueueItem item = queue.get(currentIndex);
        Log.d(TAG, "Launching [" + (currentIndex + 1) + "/" + queue.size() + "]: " + item.appName);

        // Launch the app
        boolean launched = launchApp(item.packageName);

        if (!launched) {
            Log.w(TAG, "Failed to launch: " + item.packageName);
            currentIndex++;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    launchNext();
                }
            }, 1000);
            return;
        }

        // Handle close behavior
        if (item.closeAfterSeconds == -1) {
            // Close immediately - give it a moment to start, then kill and move on
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    killApp(item.packageName);
                    currentIndex++;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            launchNext();
                        }
                    }, item.delayBeforeNextSeconds * 1000);
                }
            }, 1500);
        } else if (item.closeAfterSeconds > 0) {
            // Close after N seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    killApp(item.packageName);
                    currentIndex++;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            launchNext();
                        }
                    }, item.delayBeforeNextSeconds * 1000);
                }
            }, item.closeAfterSeconds * 1000);
        } else {
            // closeAfterSeconds == 0: keep running, just move to next
            currentIndex++;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    launchNext();
                }
            }, item.delayBeforeNextSeconds * 1000);
        }
    }

    private boolean launchApp(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching " + packageName, e);
        }
        return false;
    }

    private void killApp(String packageName) {
        try {
            android.app.ActivityManager am =
                (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(packageName);
            }
            Log.d(TAG, "Killed: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error killing " + packageName, e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
