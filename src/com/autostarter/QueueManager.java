package com.autostarter;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class QueueManager {

    private static final String PREFS_NAME = "autostart_queue_prefs";
    private static final String KEY_QUEUE = "app_queue";
    private static final String KEY_ENABLED = "queue_enabled";
    private static final String KEY_TRIGGER = "trigger_mode"; // "boot", "bluetooth", "both"
    private static final String KEY_BT_DEVICE = "bt_device_address";

    private final SharedPreferences prefs;

    public QueueManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public String getTriggerMode() {
        return prefs.getString(KEY_TRIGGER, "boot");
    }

    public void setTriggerMode(String mode) {
        prefs.edit().putString(KEY_TRIGGER, mode).apply();
    }

    public String getBtDeviceAddress() {
        return prefs.getString(KEY_BT_DEVICE, "");
    }

    public void setBtDeviceAddress(String address) {
        prefs.edit().putString(KEY_BT_DEVICE, address).apply();
    }

    public List<QueueItem> getQueue() {
        List<QueueItem> queue = new ArrayList<QueueItem>();
        String json = prefs.getString(KEY_QUEUE, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                QueueItem item = new QueueItem();
                item.packageName = obj.getString("pkg");
                item.appName = obj.getString("name");
                item.closeAfterSeconds = obj.getInt("close");
                item.delayBeforeNextSeconds = obj.optInt("delay", 2);
                queue.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queue;
    }

    public void saveQueue(List<QueueItem> queue) {
        try {
            JSONArray arr = new JSONArray();
            for (QueueItem item : queue) {
                JSONObject obj = new JSONObject();
                obj.put("pkg", item.packageName);
                obj.put("name", item.appName);
                obj.put("close", item.closeAfterSeconds);
                obj.put("delay", item.delayBeforeNextSeconds);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_QUEUE, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addToQueue(QueueItem item) {
        List<QueueItem> queue = getQueue();
        queue.add(item);
        saveQueue(queue);
    }

    public void removeFromQueue(int position) {
        List<QueueItem> queue = getQueue();
        if (position >= 0 && position < queue.size()) {
            queue.remove(position);
            saveQueue(queue);
        }
    }

    public void moveItem(int from, int to) {
        List<QueueItem> queue = getQueue();
        if (from >= 0 && from < queue.size() && to >= 0 && to < queue.size()) {
            QueueItem item = queue.remove(from);
            queue.add(to, item);
            saveQueue(queue);
        }
    }

    public void updateCloseAfter(int position, int seconds) {
        List<QueueItem> queue = getQueue();
        if (position >= 0 && position < queue.size()) {
            queue.get(position).closeAfterSeconds = seconds;
            saveQueue(queue);
        }
    }

    public static class QueueItem {
        public String packageName;
        public String appName;
        public int closeAfterSeconds; // 0 = don't close, -1 = close immediately
        public int delayBeforeNextSeconds; // delay before launching next app

        public QueueItem() {
            closeAfterSeconds = 0;
            delayBeforeNextSeconds = 2;
        }

        public String getCloseLabel() {
            if (closeAfterSeconds == 0) return "Keep running";
            if (closeAfterSeconds == -1) return "Close immediately";
            return "Close after " + closeAfterSeconds + "s";
        }
    }
}
