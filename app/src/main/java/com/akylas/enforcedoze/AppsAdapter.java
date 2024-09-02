package com.akylas.enforcedoze;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class AppsAdapter extends BaseAdapter {
    private ArrayList<AppsItem> listData;
    private LayoutInflater layoutInflater;

    public AppsAdapter(Context aContext, ArrayList<AppsItem> listData) {
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
            convertView = layoutInflater.inflate(R.layout.list_row_layout, null);
            holder = new ViewHolder();
            holder.appName = (TextView) convertView.findViewById(R.id.appName);
            holder.appPackageName = (TextView) convertView.findViewById(R.id.appPackageName);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.appName.setText(listData.get(position).getAppName());
        holder.appPackageName.setText(listData.get(position).getAppPackageName());
        return convertView;
    }

    static class ViewHolder {
        TextView appName;
        TextView appPackageName;
    }
}