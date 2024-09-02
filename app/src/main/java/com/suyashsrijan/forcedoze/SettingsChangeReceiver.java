package com.suyashsrijan.forcedoze;

import static com.suyashsrijan.forcedoze.Utils.logToLogcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

public class SettingsChangeReceiver extends BroadcastReceiver {

    public static String TAG = "EnforceDoze";private static void log(String message) {
        logToLogcat(TAG, message);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        log("com.suyashsrijan.forcedoze.CHANGE_SETTING broadcast intent received");
        final String settingName = intent.getStringExtra("settingName");
        final String settingValue = intent.getStringExtra("settingValue");

        if (settingName != null && settingValue != null) {
            if (Utils.doesSettingExist(settingName)) {
                if (Utils.isSettingBool(settingName)) {
                    Utils.updateSettingBool(context, settingName, Boolean.valueOf(settingValue));
                    if (Utils.isMyServiceRunning(ForceDozeService.class, context)) {
                        Intent i = new Intent("reload-settings");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                    }
                } else {
                    Utils.updateSettingInt(context, settingName, Integer.valueOf(settingValue));
                    if (Utils.isMyServiceRunning(ForceDozeService.class, context)) {
                        Intent i = new Intent("reload-settings");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                    }
                }
            } else {
                log("Setting does not exist or not updatable");
            }
        } else {
            log("settingName and/or settingValue null");
        }
    }
}
