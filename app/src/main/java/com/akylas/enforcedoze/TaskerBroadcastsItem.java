package com.akylas.enforcedoze;

public class TaskerBroadcastsItem {

    private String broadcastName;
    private String broadcastValues;

    public TaskerBroadcastsItem(String broadcastName, String broadcastValues) {
        this.broadcastName = broadcastName;
        this.broadcastValues = broadcastValues;
    }

    public String getBroadcastName() {
        return broadcastName;
    }

    public void setBroadcastName(String broadcastName) {
        this.broadcastName = broadcastName;
    }

    public String getBroadcastValues() {
        return broadcastValues;
    }

    public void setBroadcastValues(String broadcastValues) {
        this.broadcastValues = broadcastValues;
    }


}