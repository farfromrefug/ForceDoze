package com.suyashsrijan.forcedoze;

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

    String TAG = "ForceDozeTileService";
    SharedPreferences settings;
    boolean serviceEnabled;

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.i(TAG, "QuickTile added");
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
        Log.i(TAG, "QuickTile removed");
    }

    @Override
    public void onStartListening() {
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
            Log.i(TAG, "Disabling ForceDoze");
            stopService(new Intent(this, ForceDozeService.class));
            updateTileState(false);
        } else {
            Log.i(TAG, "Enabling ForceDoze");
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
                if (active) {
                    settings.edit().putBoolean("serviceEnabled", true).apply();
                    sendBroadcastToApp(active);

                } else {
                    settings.edit().putBoolean("serviceEnabled", false).apply();
                    sendBroadcastToApp(active);
                }
            }
        }, 1500);
    }
}
