package com.akylas.enforcedoze;

import static com.akylas.enforcedoze.Utils.logToLogcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ReenterDoze extends BroadcastReceiver {

    public static String TAG = "EnforceDoze";
    private static void log(String message) {
        logToLogcat(TAG, message);
    }

    public ReenterDoze() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        log("Re-enter broadcast received");
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("reenter-doze"));
    }
}
