package com.suyashsrijan.forcedoze;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.CardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class DozeBatteryStatsActivity extends AppCompatActivity {

    Set<String> dozeUsageStats;
    ArrayList<String> sortedDozeUsageStats;
    MaterialDialog progressDialog = null;
    MaterialListView mListView;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    public static String TAG = "ForceDoze";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doze_battery_stats);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();
        dozeUsageStats = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("dozeUsageDataAdvanced", new LinkedHashSet<String>());
        mListView = (MaterialListView) findViewById(R.id.material_listview);
        if (!dozeUsageStats.isEmpty()) {
            sortedDozeUsageStats = new ArrayList<>(dozeUsageStats);
            Collections.sort(sortedDozeUsageStats);
            Collections.reverse(sortedDozeUsageStats);
            Log.i(TAG, "Size: " + sortedDozeUsageStats.size());

            if (sharedPreferences.contains("dozeUsageData")) {
                Log.i(TAG, "Found old stats data, deleting..");
                editor.remove("dozeUsageData").commit();
            } else if (sharedPreferences.contains("dozeUsageDataNew")) {
                Log.i(TAG, "Found old stats data, deleting..");
                editor.remove("dozeUsageDataNew").commit();
            }

            if (sortedDozeUsageStats.size() > 100) {
                Log.i(TAG, "Trimming stats data to most recent 100 entries...");
                int newSize = sortedDozeUsageStats.size() % 2 == 0 ? sortedDozeUsageStats.size() / 2 : (sortedDozeUsageStats.size() / 2) + 1;
                ArrayList<String> tempArrayList1 = new ArrayList<>(sortedDozeUsageStats.subList(0, newSize));
                ArrayList<String> tempArrayList2 = new ArrayList<>(sortedDozeUsageStats);
                tempArrayList2.removeAll(tempArrayList1);
                sortedDozeUsageStats.removeAll(tempArrayList2);
                tempArrayList1.clear();
                tempArrayList2.clear();
                editor.putStringSet("dozeUsageDataAdvanced", new LinkedHashSet<String>(sortedDozeUsageStats));
                editor.apply();
            }

            if ((sortedDozeUsageStats.size() & 1) == 0) {

                for (int i = 0; i < sortedDozeUsageStats.size(); ) {
                    String[] exit_data = sortedDozeUsageStats.get(i).split(",");
                    Log.i(TAG, "Exit data : [" + Arrays.toString(exit_data) + "]");
                    String[] enter_data = sortedDozeUsageStats.get(i + 1).split(",");
                    Log.i(TAG, "Enter data: [" + Arrays.toString(enter_data) + "]");

                    if (enter_data[2].equals("ENTER") && exit_data[2].equals("EXIT")) {
                        Card card = new Card.Builder(this)
                                .withProvider(new CardProvider())
                                .setLayout(R.layout.material_small_image_card)
                                .setTitle("Doze Session")
                                .setDrawable(returnDrawableBattery(Float.valueOf(enter_data[1]).intValue() - Float.valueOf(exit_data[1]).intValue()))
                                .setDescription("Start Time: " + Utils.getDateCurrentTimeZone(Long.valueOf(enter_data[0])) +
                                        "\nEnd Time: " + Utils.getDateCurrentTimeZone(Long.valueOf(exit_data[0])) +
                                        "\nTime spent: " + Utils.timeSpentString(Long.valueOf(enter_data[0]), Long.valueOf(exit_data[0])) +
                                        "\nBattery used: " + (Float.valueOf(enter_data[1]).intValue() - Float.valueOf(exit_data[1]).intValue() + "%"))
                                .endConfig()
                                .build();
                        mListView.getAdapter().add(card);
                        i = i + 2;
                    } else if (enter_data[2].equals("ENTER_MAINTENANCE") && exit_data[2].equals("EXIT_MAINTENANCE")) {
                        Card card = new Card.Builder(this)
                                .withProvider(new CardProvider())
                                .setLayout(R.layout.material_small_image_card)
                                .setTitle("Doze Session (Maintenance)")
                                .setDrawable(returnDrawableBattery(Float.valueOf(enter_data[1]).intValue() - Float.valueOf(exit_data[1]).intValue()))
                                .setDescription("Start Time: " + Utils.getDateCurrentTimeZone(Long.valueOf(enter_data[0])) +
                                        "\nEnd Time: " + Utils.getDateCurrentTimeZone(Long.valueOf(exit_data[0])) +
                                        "\nTime spent: " + Utils.timeSpentString(Long.valueOf(enter_data[0]), Long.valueOf(exit_data[0])) +
                                        "\nBattery used: " + (Integer.valueOf(enter_data[1]) - Integer.valueOf(exit_data[1]) + "%"))
                                .endConfig()
                                .build();
                        mListView.getAdapter().add(card);
                        i = i + 2;
                    } else {
                        i = i + 2;
                    }
                }
            } else {
                Log.i(TAG, "Missing log entries, redirecting users to old stats activity");
                startActivity(new Intent(this, DozeStatsActivity.class));
                finish();
            }
            mListView.scrollToPosition(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.doze_stats_menu_new, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_clear_stats:
                clearStats();
                break;
            case R.id.action_switch_stats_ui:
                startActivity(new Intent(this, DozeStatsActivity.class));
                break;
            case R.id.action_stats_more_info:
                showMoreInfoDialog();
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showMoreInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getString(R.string.doze_stats_battery_icon_meaning_dialog_title));
        builder.setMessage(getString(R.string.doze_stats_battery_icon_meaning_dialog_text));
        builder.setPositiveButton(getString(R.string.close_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    public Drawable returnDrawableBattery(int bUsage) {
        return (bUsage >= 3) ? ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_battery_alert_black_48dp) : ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_battery_charging_full_black_48dp);
    }

    public void clearStats() {
        progressDialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.please_wait_text))
                .cancelable(false)
                .autoDismiss(false)
                .content(getString(R.string.clearing_doze_stats_text))
                .progress(true, 0)
                .show();

        Tasks.executeInBackground(DozeBatteryStatsActivity.this, new BackgroundWork<Boolean>() {
            @Override
            public Boolean doInBackground() throws Exception {
                Log.i(TAG, "Clearing Doze stats");
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
                    Log.i(TAG, "Doze stats successfully cleared");
                    if (Utils.isMyServiceRunning(ForceDozeService.class, context)) {
                        Intent intent = new Intent("reload-settings");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                    mListView.getAdapter().clearAll();
                    mListView.getAdapter().notifyDataSetChanged();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
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
