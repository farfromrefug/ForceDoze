package com.suyashsrijan.forcedoze;

import static com.suyashsrijan.forcedoze.Utils.logToLogcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

public class ReenterDoze extends BroadcastReceiver {

    public static String TAG = "ForceDoze";
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
