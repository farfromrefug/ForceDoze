package com.akylas.enforcedoze;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class TaskerBroadcastsAdapter extends BaseAdapter {
    private ArrayList<TaskerBroadcastsItem> listData;
    private LayoutInflater layoutInflater;

    public TaskerBroadcastsAdapter(Context aContext, ArrayList<TaskerBroadcastsItem> listData) {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_row_layout_broadcasts, null);
            holder = new ViewHolder();
            holder.broadcastName = (TextView) convertView.findViewById(R.id.broadcastName);
            holder.broadcastValues = (TextView) convertView.findViewById(R.id.broadcastValues);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.broadcastName.setText(listData.get(position).getBroadcastName());
        holder.broadcastValues.setText(listData.get(position).getBroadcastValues());
        return convertView;
    }

    static class ViewHolder {
        TextView broadcastName;
        TextView broadcastValues;
    }
}