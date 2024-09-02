package com.akylas.enforcedoze;

import static com.akylas.enforcedoze.Utils.logToLogcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class AddWhiteListReceiver extends BroadcastReceiver {
    public static String TAG = "EnforceDoze";
    private static void log(String message) {
        logToLogcat(TAG, message);
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        log("com.akylas.enforcedoze.ADD_WHITELIST broadcast intent received");
        final String packageName = intent.getStringExtra("packageName");
        log("Package name received: " + packageName);
        if (packageName != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    List<String> output = Shell.SH.run("dumpsys deviceidle whitelist +" + packageName);
                    if (output != null) {
                        for (String s : output) {
                            log(s);
                        }
                    } else {
                        log("Error occurred while executing command (" + "dumpsys deviceidle whitelist +packagename" + ")");
                    }
                }
            });
        } else {
            log("Package name null or empty");
        }
    }
}
