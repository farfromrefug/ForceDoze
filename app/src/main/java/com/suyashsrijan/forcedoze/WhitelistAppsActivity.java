package com.suyashsrijan.forcedoze;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
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
import java.util.LinkedHashSet;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class WhitelistAppsActivity extends AppCompatActivity {
    ListView listView;
    SharedPreferences sharedPreferences;
    AppsAdapter whitelistAppsAdapter;
    ArrayList<String> whitelistedPackages;
    ArrayList<AppsItem> listData;
    public static String TAG = "ForceDoze";
    boolean showDozeWhitelistWarning = true;
    boolean isSuAvailable = false;
    MaterialDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist_apps);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = (ListView) findViewById(R.id.listView2);
        whitelistedPackages = new ArrayList<>();
        listData = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        whitelistAppsAdapter = new AppsAdapter(this, listData);
        listView.setAdapter(whitelistAppsAdapter);
        loadPackagesFromWhitelist();
        isSuAvailable = sharedPreferences.getBoolean("isSuAvailable", false);
        showDozeWhitelistWarning = sharedPreferences.getBoolean("showDozeWhitelistWarning", true);

        if (showDozeWhitelistWarning) {
            displayDialog(getString(R.string.whitelisting_text), getString(R.string.whitelisted_apps_restrictions_text));
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("showDozeWhitelistWarning", false);
            editor.apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.whitelist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add_whitelist:
                startActivityForResult(new Intent(WhitelistAppsActivity.this, PackageChooserActivity.class), 999);
                break;
            case R.id.action_remove_whitelist:
                startActivityForResult(new Intent(WhitelistAppsActivity.this, PackageChooserActivity.class), 998);
                break;
            case R.id.action_add_whitelist_package:
                showManuallyAddPackageDialog();
                break;
            case R.id.action_remove_whitelist_package:
                showManuallyRemovePackageDialog();
                break;
            case R.id.action_whitelist_more_info:
                displayDialog(getString(R.string.whitelisting_text), getString(R.string.whitelisted_apps_restrictions_text));
                break;
            case R.id.action_launch_system_whitelist:
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (requestCode == 999) {
                String pkg = data.getStringExtra("package_name");
                verifyAndAddPackage(pkg);
            } else if (requestCode == 998) {
                String pkg = data.getStringExtra("package_name");
                verifyAndRemovePackage(pkg);
            }
        }
    }

    public void loadPackagesFromWhitelist() {
        Log.i(TAG, "Loading whitelisted packages...");
        progressDialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.please_wait_text))
                .autoDismiss(false)
                .cancelable(false)
                .content(R.string.loading_whitelisted_packages)
                .progress(true, 0)
                .show();

        Tasks.executeInBackground(WhitelistAppsActivity.this, new BackgroundWork<List<String>>() {
            @Override
            public List<String> doInBackground() throws Exception {
                List<String> output;
                List<String> packages = new ArrayList<>();
                output = Shell.SH.run("dumpsys deviceidle whitelist");
                for (String s : output) {
                    packages.add(s.split(",")[1]);
                }

                return new ArrayList<>(new LinkedHashSet<>(packages));
            }
        }, new Completion<List<String>>() {
            @Override
            public void onSuccess(Context context, List<String> result) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                if (!result.isEmpty()) {
                    if (!listData.isEmpty() || !whitelistedPackages.isEmpty()) {
                        listData.clear();
                        whitelistedPackages.clear();
                    }
                    for (String r : result) {
                        AppsItem appItem = new AppsItem();
                        appItem.setAppPackageName(r);
                        whitelistedPackages.add(r);
                        try {
                            appItem.setAppName(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(r, PackageManager.GET_META_DATA)).toString());
                        } catch (PackageManager.NameNotFoundException e) {
                            appItem.setAppName("System package");
                        }
                        listData.add(appItem);
                    }
                    whitelistAppsAdapter.notifyDataSetChanged();
                }

                Log.i(TAG, "Whitelisted packages: " + listData.size() + " packages in total");
            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "Error loading packages: " + e.getMessage());

            }
        });
    }

    public void showManuallyAddPackageDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.whitelist_apps_setting_text))
                .content(R.string.manually_add_package_dialog_text)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("com.spotify.music", "", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        verifyAndAddPackage(input.toString());
                    }
                }).show();
    }

    public void showManuallyRemovePackageDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.whitelist_apps_setting_text))
                .content(R.string.manually_remove_package_dialog_text)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("com.spotify.music", "", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        verifyAndRemovePackage(input.toString());
                    }
                }).show();
    }


    public void verifyAndAddPackage(String packageName) {
        if (whitelistedPackages.contains(packageName)) {
            displayDialog(getString(R.string.info_text), getString(R.string.app_already_whitelisted_text));
        } else {
            modifyWhitelist(packageName, false);
            loadPackagesFromWhitelist();
        }
    }

    public void verifyAndRemovePackage(String packageName) {
        if (!whitelistedPackages.contains(packageName)) {
            displayDialog(getString(R.string.info_text), getString(R.string.app_not_whitelisted_text));
        } else {
            modifyWhitelist(packageName, true);
            loadPackagesFromWhitelist();
        }
    }

    public void modifyWhitelist(String packageName, boolean remove) {
        if (remove) {
            Log.i(TAG, "Removing app " + packageName + " from Doze whitelist");
            executeCommand("dumpsys deviceidle whitelist -" + packageName);
        } else {
            Log.i(TAG, "Adding app " + packageName + " to Doze whitelist");
            executeCommand("dumpsys deviceidle whitelist +" + packageName);
        }
    }

    public void displayDialog(String title, String message) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(getString(R.string.close_button_text), null);
        builder.show();
    }

    public void executeCommand(final String command) {
        if (Utils.isDeviceRunningOnN() && isSuAvailable) {
            Shell.SU.run(command);
        } else {
            Shell.SH.run(command);
        }
    }
}
