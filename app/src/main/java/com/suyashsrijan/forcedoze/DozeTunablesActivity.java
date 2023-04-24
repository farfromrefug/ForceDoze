package com.suyashsrijan.forcedoze;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class DozeTunablesActivity extends AppCompatActivity {

    public static String TAG = "ForceDoze";
    public String TUNABLE_STRING = "null";
    public static boolean suAvailable = false;

    private long LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = 5 * 60 * 1000L;
    private long LIGHT_PRE_IDLE_TIMEOUT = 10 * 60 * 1000L;
    private long LIGHT_IDLE_TIMEOUT = 5 * 60 * 1000L;
    private float LIGHT_IDLE_FACTOR = 2f;
    private long LIGHT_MAX_IDLE_TIMEOUT = 15 * 60 * 1000L;
    private long LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = 1 * 60 * 1000L;
    private long LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = 5 * 60 * 1000L;
    private long MIN_LIGHT_MAINTENANCE_TIME = 5 * 1000L;
    private long MIN_DEEP_MAINTENANCE_TIME = 30 * 1000L;
    private long INACTIVE_TIMEOUT = 30 * 60 * 1000L;
    private long SENSING_TIMEOUT = 4 * 60 * 1000L;
    private long LOCATING_TIMEOUT = 30 * 1000L;
    private float LOCATION_ACCURACY = 20;
    private long MOTION_INACTIVE_TIMEOUT = 10 * 60 * 1000L;
    private long IDLE_AFTER_INACTIVE_TIMEOUT = 30 * 60 * 1000L;
    private long IDLE_PENDING_TIMEOUT = 5 * 60 * 1000L;
    private long MAX_IDLE_PENDING_TIMEOUT = 10 * 60 * 1000L;
    private float IDLE_PENDING_FACTOR = 2;
    private long IDLE_TIMEOUT = 60 * 60 * 1000L;
    private long MAX_IDLE_TIMEOUT = 6 * 60 * 60 * 1000L;
    private long IDLE_FACTOR = 2;
    private long MIN_TIME_TO_ALARM = 60 * 60 * 1000L;
    private long MAX_TEMP_APP_WHITELIST_DURATION = 5 * 60 * 1000L;
    private long MMS_TEMP_APP_WHITELIST_DURATION = 60 * 1000L;
    private long SMS_TEMP_APP_WHITELIST_DURATION = 20 * 1000L;
    private long NOTIFICATION_WHITELIST_DURATION = 30 * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadTunables();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new DozeTunablesFragment())
                .commit();

        TUNABLE_STRING = getTunableString();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (!suAvailable) {
            menu.getItem(0).setVisible(false);
        }

        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.doze_tunables_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_apply_tunables:
                applyTunables();
                break;
            case R.id.action_copy_tunables:
                showCopyTunableDialog();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadTunables() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, "300000"));
        LIGHT_PRE_IDLE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_LIGHT_PRE_IDLE_TIMEOUT, "600000"));
        LIGHT_IDLE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_LIGHT_IDLE_TIMEOUT, "300000"));
        LIGHT_IDLE_FACTOR = Float.parseFloat(preferences.getString(DozeTunableConstants.KEY_LIGHT_IDLE_FACTOR, "2"));
        LIGHT_MAX_IDLE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_LIGHT_MAX_IDLE_TIMEOUT, "900000"));
        LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, "60000"));
        LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, "300000"));
        MIN_LIGHT_MAINTENANCE_TIME = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MIN_LIGHT_MAINTENANCE_TIME, "5000"));
        MIN_DEEP_MAINTENANCE_TIME = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MIN_DEEP_MAINTENANCE_TIME, "30000"));
        INACTIVE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_INACTIVE_TIMEOUT, "1800000"));
        SENSING_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_SENSING_TIMEOUT, "240000"));
        LOCATING_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_LOCATING_TIMEOUT, "30000"));
        LOCATION_ACCURACY = Float.parseFloat(preferences.getString(DozeTunableConstants.KEY_LOCATION_ACCURACY, "20"));
        MOTION_INACTIVE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MOTION_INACTIVE_TIMEOUT, "600000"));
        IDLE_AFTER_INACTIVE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_IDLE_AFTER_INACTIVE_TIMEOUT, "1800000"));
        IDLE_PENDING_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_IDLE_PENDING_TIMEOUT, "30000"));
        MAX_IDLE_PENDING_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MAX_IDLE_PENDING_TIMEOUT, "600000"));
        IDLE_PENDING_FACTOR = Float.parseFloat(preferences.getString(DozeTunableConstants.KEY_IDLE_PENDING_FACTOR, "2"));
        IDLE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_IDLE_TIMEOUT, "3600000"));
        MAX_IDLE_TIMEOUT = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MAX_IDLE_TIMEOUT, "21600000"));
        IDLE_FACTOR  = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_IDLE_FACTOR, "2"));
        MIN_TIME_TO_ALARM = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MIN_TIME_TO_ALARM, "3600000"));
        MAX_TEMP_APP_WHITELIST_DURATION = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MAX_TEMP_APP_WHITELIST_DURATION, "300000"));
        MMS_TEMP_APP_WHITELIST_DURATION = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_MMS_TEMP_APP_WHITELIST_DURATION, "60000"));
        SMS_TEMP_APP_WHITELIST_DURATION = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_SMS_TEMP_APP_WHITELIST_DURATION, "20000"));
        NOTIFICATION_WHITELIST_DURATION = Long.parseLong(preferences.getString(DozeTunableConstants.KEY_NOTIFICATION_WHITELIST_DURATION, "30000"));
    }

    public String getTunableString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DozeTunableConstants.KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT + "=" + LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_LIGHT_PRE_IDLE_TIMEOUT + "=" + LIGHT_PRE_IDLE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_LIGHT_IDLE_TIMEOUT + "=" + LIGHT_IDLE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_LIGHT_IDLE_FACTOR + "=" + LIGHT_IDLE_FACTOR + ",");
        sb.append(DozeTunableConstants.KEY_LIGHT_MAX_IDLE_TIMEOUT + "=" + LIGHT_MAX_IDLE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET + "=" + LIGHT_IDLE_MAINTENANCE_MIN_BUDGET + ",");
        sb.append(DozeTunableConstants.KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET + "=" + LIGHT_IDLE_MAINTENANCE_MAX_BUDGET + ",");
        sb.append(DozeTunableConstants.KEY_MIN_LIGHT_MAINTENANCE_TIME + "=" + MIN_LIGHT_MAINTENANCE_TIME + ",");
        sb.append(DozeTunableConstants.KEY_MIN_DEEP_MAINTENANCE_TIME + "=" + MIN_DEEP_MAINTENANCE_TIME + ",");
        sb.append(DozeTunableConstants.KEY_INACTIVE_TIMEOUT + "=" + INACTIVE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_SENSING_TIMEOUT + "=" + SENSING_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_LOCATING_TIMEOUT + "=" + LOCATING_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_LOCATION_ACCURACY + "=" + LOCATION_ACCURACY + ",");
        sb.append(DozeTunableConstants.KEY_MOTION_INACTIVE_TIMEOUT + "=" + MOTION_INACTIVE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_IDLE_AFTER_INACTIVE_TIMEOUT + "=" + IDLE_AFTER_INACTIVE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_IDLE_PENDING_TIMEOUT + "=" + IDLE_PENDING_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_MAX_IDLE_PENDING_TIMEOUT + "=" + MAX_IDLE_PENDING_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_IDLE_PENDING_FACTOR + "=" + IDLE_PENDING_FACTOR + ",");
        sb.append(DozeTunableConstants.KEY_IDLE_TIMEOUT + "=" + IDLE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_MAX_IDLE_TIMEOUT + "=" + MAX_IDLE_TIMEOUT + ",");
        sb.append(DozeTunableConstants.KEY_IDLE_FACTOR + "=" + IDLE_FACTOR + ",");
        sb.append(DozeTunableConstants.KEY_MIN_TIME_TO_ALARM + "=" + MIN_TIME_TO_ALARM + ",");
        sb.append(DozeTunableConstants.KEY_MAX_TEMP_APP_WHITELIST_DURATION + "=" + MAX_TEMP_APP_WHITELIST_DURATION + ",");
        sb.append(DozeTunableConstants.KEY_MMS_TEMP_APP_WHITELIST_DURATION + "=" + MMS_TEMP_APP_WHITELIST_DURATION + ",");
        sb.append(DozeTunableConstants.KEY_SMS_TEMP_APP_WHITELIST_DURATION + "=" + SMS_TEMP_APP_WHITELIST_DURATION + ",");
        sb.append(DozeTunableConstants.KEY_NOTIFICATION_WHITELIST_DURATION + "=" + NOTIFICATION_WHITELIST_DURATION);
        return sb.toString();
    }

    public void applyTunables() {
        loadTunables();
        TUNABLE_STRING = getTunableString();
        Log.i(TAG, "Setting device_idle_constants=" + TUNABLE_STRING);
        executeCommand("settings put global device_idle_constants " + TUNABLE_STRING);
        Toast.makeText(this, getString(R.string.applied_success_text), Toast.LENGTH_SHORT).show();
    }

    public void showCopyTunableDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.adb_command_text));
        builder.setMessage("You can apply the new values using ADB by running the following command:\n\nadb shell settings put global device_idle_constants " + TUNABLE_STRING);
        builder.setPositiveButton(getString(R.string.close_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.copy_to_clipboard_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Tunable k/v string", "adb shell settings put global device_idle_constants " + TUNABLE_STRING);
                clipboard.setPrimaryClip(clip);
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    public void executeCommand(final String command) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<String> output = Shell.SU.run(command);
                if (output != null) {
                    printShellOutput(output);
                } else {
                    Log.i(TAG, "Error occurred while executing command (" + command + ")");
                }
            }
        });
    }

    public void printShellOutput(List<String> output) {
        if (!output.isEmpty()) {
            for (String s : output) {
                Log.i(TAG, s);
            }
        }
    }

    public static class DozeTunablesFragment extends PreferenceFragment {

        MaterialDialog grantPermProgDialog;
        boolean isSuAvailable = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_doze_tunables);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("tunablesPreferenceScreen");
            PreferenceCategory lightDozeSettings = (PreferenceCategory) findPreference("lightDozeSettings");

            if (!Utils.isDeviceRunningOnN()) {
                preferenceScreen.removePreference(lightDozeSettings);
            }

            if (!preferences.getBoolean("isSuAvailable", false)) {
                grantPermProgDialog = new MaterialDialog.Builder(getActivity())
                        .title(getString(R.string.please_wait_text))
                        .cancelable(false)
                        .autoDismiss(false)
                        .content(getString(R.string.requesting_su_access_text))
                        .progress(true, 0)
                        .show();
                Log.i(TAG, "Check if SU is available, and request SU permission if it is");
                Tasks.executeInBackground(getActivity(), new BackgroundWork<Boolean>() {
                    @Override
                    public Boolean doInBackground() throws Exception {
                        return Shell.SU.available();
                    }
                }, new Completion<Boolean>() {
                    @Override
                    public void onSuccess(Context context, Boolean result) {
                        if (grantPermProgDialog != null) {
                            grantPermProgDialog.dismiss();
                        }
                        isSuAvailable = result;
                        suAvailable = isSuAvailable;
                        Log.i(TAG, "SU available: " + Boolean.toString(result));
                        if (isSuAvailable) {
                            Log.i(TAG, "Phone is rooted and SU permission granted");
                            if (!Utils.isSecureSettingsPermissionGranted(getActivity())) {
                                executeCommand("pm grant com.suyashsrijan.forcedoze android.permission.WRITE_SECURE_SETTINGS");
                            }
                        } else {
                            Log.i(TAG, "SU permission denied or not available");
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                            builder.setTitle(getString(R.string.error_text));
                            builder.setMessage(getString(R.string.tunables_su_not_available_error_text));
                            builder.setPositiveButton(getString(R.string.okay_button_text), new DialogInterface.OnClickListener() {
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
                        Log.e(TAG, "Error querying SU: " + e.getMessage());
                    }
                });
            } else {
                suAvailable = true;
            }
        }

        public void executeCommand(final String command) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    List<String> output = Shell.SU.run(command);
                    if (output != null) {
                        printShellOutput(output);
                    } else {
                        Log.i(TAG, "Error occurred while executing command (" + command + ")");
                    }
                }
            });
        }

        public void printShellOutput(List<String> output) {
            if (!output.isEmpty()) {
                for (String s : output) {
                    Log.i(TAG, s);
                }
            }
        }
    }
}
