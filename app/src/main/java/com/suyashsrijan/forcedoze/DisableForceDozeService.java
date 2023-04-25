package com.suyashsrijan.forcedoze;


import static com.suyashsrijan.forcedoze.Utils.logToLogcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

public class DisableForceDozeService extends BroadcastReceiver {
    public static String TAG = "ForceDoze";

    private static void log(String message) {
        logToLogcat(TAG, message);
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        log("com.suyashsrijan.forcedoze.DISABLE_FORCEDOZE broadcast intent received");
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("serviceEnabled", false).apply();
        if (Utils.isMyServiceRunning(ForceDozeService.class, context)) {
            context.stopService(new Intent(context, ForceDozeService.class));
        }
    }
}
