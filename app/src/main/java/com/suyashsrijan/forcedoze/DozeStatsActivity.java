package com.suyashsrijan.forcedoze;

import static com.suyashsrijan.forcedoze.Utils.logToLogcat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class DozeStatsActivity extends AppCompatActivity {

    /* Old Doze stats activity left to show a compact view of Doze stats and to handle some edge cases */

    ArrayList<BatteryConsumptionItem> batteryConsumptionItems;
    Set<String> dozeUsageStats;
    ListView listView;
    BatteryConsumptionAdapter batteryConsumptionAdapter;
    MaterialDialog progressDialog = null;
    public static String TAG = "ForceDoze";

    private static void log(String message) {
        logToLogcat(TAG, message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doze_battery_consumption);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = (ListView) findViewById(R.id.listView);
        batteryConsumptionItems = new ArrayList<>();
        dozeUsageStats = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("dozeUsageDataAdvanced", new LinkedHashSet<String>());
        if (!dozeUsageStats.isEmpty()) {
            ArrayList<String> sortedList = new ArrayList<String>(dozeUsageStats);
            Collections.sort(sortedList);
            Collections.reverse(sortedList);
            for (int i = 0; i < dozeUsageStats.size(); i++) {
                BatteryConsumptionItem item = new BatteryConsumptionItem();
                item.setTimestampPercCombo(sortedList.get(i));
                batteryConsumptionItems.add(item);
            }
        }
        batteryConsumptionAdapter = new BatteryConsumptionAdapter(this, batteryConsumptionItems);
        listView.setAdapter(batteryConsumptionAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.doze_stats_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_clear_stats:
                clearStats();
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clearStats() {
        progressDialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.please_wait_text))
                .cancelable(false)
                .autoDismiss(false)
                .content(getString(R.string.clearing_doze_stats_text))
                .progress(true, 0)
                .show();

        Tasks.executeInBackground(DozeStatsActivity.this, new BackgroundWork<Boolean>() {
            @Override
            public Boolean doInBackground() throws Exception {
                log("Clearing Doze stats");
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("dozeUsageDataAdvanced");
                return editor.commit();
            }
        }, new Completion<Boolean>() {
            @Override
            public void onSuccess(Context context, Boolean result) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                if (result) {
                    log("Doze stats successfully cleared");
                    if (Utils.isMyServiceRunning(ForceDozeService.class, context)) {
                        Intent intent = new Intent("reload-settings");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                    batteryConsumptionItems.clear();
                    batteryConsumptionAdapter.notifyDataSetChanged();
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle(getString(R.string.cleared_text));
                    builder.setMessage(getString(R.string.doze_battery_stats_clear_msg));
                    builder.setPositiveButton(getString(R.string.close_button_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.show();
                }

            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "Error clearing Doze stats: " + e.getMessage());

            }
        });
    }
}
