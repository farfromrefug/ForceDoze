package com.akylas.enforcedoze;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class BatteryConsumptionAdapter extends BaseAdapter {
    private ArrayList<BatteryConsumptionItem> listData;
    private LayoutInflater layoutInflater;

    public BatteryConsumptionAdapter(Context aContext, ArrayList<BatteryConsumptionItem> listData) {
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
            convertView = layoutInflater.inflate(R.layout.list_row_layout_stats, null);
            holder = new ViewHolder();
            holder.timestamp = (TextView) convertView.findViewById(R.id.dozeStateTimestamp);
            holder.batteryPerc = (TextView) convertView.findViewById(R.id.batteryLevel);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String[] data = listData.get(position).getTimestampPercCombo().split(",");
        if (data[2].equals("EXIT")) {
            holder.timestamp.setText("Exited Doze mode at ".concat(Utils.getDateCurrentTimeZone(Long.valueOf(data[0]))));
        } else if (data[2].equals("ENTER")) {
            holder.timestamp.setText("Entered Doze mode at ".concat(Utils.getDateCurrentTimeZone(Long.valueOf(data[0]))));
        } else if (data[2].equals("EXIT_MAINTENANCE")) {
            holder.timestamp.setText("Exited Doze mode for maintenance at ".concat(Utils.getDateCurrentTimeZone(Long.valueOf(data[0]))));
        } else if(data[2].equals("ENTER_MAINTENANCE")) {
            holder.timestamp.setText("Entered Doze mode after maintenance at ".concat(Utils.getDateCurrentTimeZone(Long.valueOf(data[0]))));
        }
        holder.batteryPerc.setText("Battery level: ".concat(data[1]).concat("%"));
        return convertView;
    }

    static class ViewHolder {
        TextView timestamp;
        TextView batteryPerc;
    }
}