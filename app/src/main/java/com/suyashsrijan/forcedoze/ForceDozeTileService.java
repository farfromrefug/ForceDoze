package com.suyashsrijan.forcedoze;

import static com.suyashsrijan.forcedoze.Utils.logToLogcat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;


@RequiresApi(api = Build.VERSION_CODES.N)
public class ForceDozeTileService extends TileService {

    static String TAG = "ForceDozeTileService";
    SharedPreferences settings;
    boolean serviceEnabled;

    private static void log(String message) {
        logToLogcat(TAG, message);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        log("QuickTile added");
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serviceEnabled = settings.getBoolean("serviceEnabled", false);
        if (serviceEnabled) {
            updateTileState(true);
        } else {
            updateTileState(false);
        }
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        log("QuickTile removed");
    }

    @Override
    public void onStartListening() {
        log("QuickTile onStartListening");
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serviceEnabled = settings.getBoolean("serviceEnabled", false);
        if (serviceEnabled) {
            updateTileState(true);
        } else {
            updateTileState(false);
        }
    }


    @Override
    public void onClick() {
        super.onClick();
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serviceEnabled = settings.getBoolean("serviceEnabled", false);
        if (serviceEnabled) {
            log("Disabling ForceDoze");
            stopService(new Intent(this, ForceDozeService.class));
            updateTileState(false);
        } else {
            log("Enabling ForceDoze");
            startService(new Intent(this, ForceDozeService.class));
            updateTileState(true);
        }
    }

    public void sendBroadcastToApp(boolean active) {
        Intent intent = new Intent("update-state-from-tile");
        intent.putExtra("isActive", active);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void updateTileState(final boolean active) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Tile tile = getQsTile();
                if (tile != null) {
                    tile.setLabel(active ? "ForceDoze on" : "ForceDoze off");
                    tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
                    tile.updateTile();
                }
                boolean currentValue = settings.getBoolean("serviceEnabled", false);
                if (currentValue != active) {
                    settings.edit().putBoolean("serviceEnabled", active).apply();
                    sendBroadcastToApp(active);
                }
            }
        }, 150);
    }
}
