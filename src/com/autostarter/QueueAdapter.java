package com.autostarter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class QueueAdapter extends BaseAdapter {

    public interface OnQueueActionListener {
        void onMoveUp(int position);
        void onMoveDown(int position);
        void onRemove(int position);
        void onChangeCloseTime(int position);
    }

    private final Context context;
    private List<QueueManager.QueueItem> items;
    private final OnQueueActionListener listener;
    private final PackageManager pm;

    public QueueAdapter(Context context, List<QueueManager.QueueItem> items,
                        OnQueueActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.pm = context.getPackageManager();
    }

    public void setItems(List<QueueManager.QueueItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public QueueManager.QueueItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_queue, parent, false);
        }

        QueueManager.QueueItem item = items.get(position);

        // Order number
        TextView tvOrder = (TextView) convertView.findViewById(R.id.tvOrder);
        tvOrder.setText(String.valueOf(position + 1));

        // App icon
        ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivAppIcon);
        try {
            Drawable icon = pm.getApplicationIcon(item.packageName);
            ivIcon.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            ivIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // App name
        TextView tvName = (TextView) convertView.findViewById(R.id.tvAppName);
        tvName.setText(item.appName);

        // Package name
        TextView tvPkg = (TextView) convertView.findViewById(R.id.tvPackage);
        tvPkg.setText(item.packageName);

        // Close behavior
        TextView tvClose = (TextView) convertView.findViewById(R.id.tvCloseAfter);
        tvClose.setText(item.getCloseLabel());
        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onChangeCloseTime(position);
            }
        });

        // Move up button
        ImageButton btnUp = (ImageButton) convertView.findViewById(R.id.btnMoveUp);
        btnUp.setEnabled(position > 0);
        btnUp.setAlpha(position > 0 ? 1.0f : 0.3f);
        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onMoveUp(position);
            }
        });

        // Move down button
        ImageButton btnDown = (ImageButton) convertView.findViewById(R.id.btnMoveDown);
        btnDown.setEnabled(position < items.size() - 1);
        btnDown.setAlpha(position < items.size() - 1 ? 1.0f : 0.3f);
        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onMoveDown(position);
            }
        });

        // Remove button
        ImageButton btnRemove = (ImageButton) convertView.findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRemove(position);
            }
        });

        return convertView;
    }
}
