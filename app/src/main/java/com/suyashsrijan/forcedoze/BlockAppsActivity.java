package com.suyashsrijan.forcedoze;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

public class BlockAppsActivity extends AppCompatActivity {
    ListView listView;
    SharedPreferences sharedPreferences;
    AppsAdapter blockDozeApps;
    ArrayList<String> blockedPackages;
    ArrayList<AppsItem> listData;
    public static String TAG = "ForceDoze";
    boolean isSuAvailable = false;
    MaterialDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doze_blocklist_apps);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = (ListView) findViewById(R.id.listViewAppBlockList);
        blockedPackages = new ArrayList<>();
        listData = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        blockDozeApps = new AppsAdapter(this, listData);
        listView.setAdapter(blockDozeApps);
        loadPackagesFromBlockList();
        isSuAvailable = sharedPreferences.getBoolean("isSuAvailable", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Utils.isMyServiceRunning(ForceDozeService.class, BlockAppsActivity.this)) {
            Intent intent = new Intent("reload-app-blocklist");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.block_app_doze_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add_app_doze_blocklist:
                startActivityForResult(new Intent(BlockAppsActivity.this, PackageChooserActivity.class), 505);
                break;
            case R.id.action_remove_app_doze_blocklist:
                startActivityForResult(new Intent(BlockAppsActivity.this, PackageChooserActivity.class), 506);
                break;
            case R.id.action_add_app_doze_blocklist_package:
                showManuallyAddPackageDialog();
                break;
            case R.id.action_remove_app_doze_blocklist_package:
                showManuallyRemovePackageDialog();
                break;
            case R.id.action_app_doze_blacklist_more_info:
                displayDialog(getString(R.string.app_blocklist_dialog_title), getString(R.string.app_blocklist_dialog_text));
                break;
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
            if (requestCode == 505) {
                String pkg = data.getStringExtra("package_name");
                verifyAndAddPackage(pkg);
            } else if (requestCode == 506) {
                String pkg = data.getStringExtra("package_name");
                verifyAndRemovePackage(pkg);
            }
        }
    }

    public void loadPackagesFromBlockList() {
        Log.i(TAG, "Loading blocked packages...");
        progressDialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.please_wait_text))
                .autoDismiss(false)
                .cancelable(false)
                .content(getString(R.string.loading_blocked_packages))
                .progress(true, 0)
                .show();

        Tasks.executeInBackground(BlockAppsActivity.this, new BackgroundWork<List<String>>() {
            @Override
            public List<String> doInBackground() throws Exception {
                List<String> packages = new ArrayList<>(sharedPreferences.getStringSet("dozeAppBlockList", new LinkedHashSet<String>()));
                return new ArrayList<>(new LinkedHashSet<>(packages));
            }
        }, new Completion<List<String>>() {
            @Override
            public void onSuccess(Context context, List<String> result) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                if (!result.isEmpty()) {
                    if (!listData.isEmpty() || !blockedPackages.isEmpty()) {
                        listData.clear();
                        blockedPackages.clear();
                    }
                    for (String r : result) {
                        AppsItem appItem = new AppsItem();
                        appItem.setAppPackageName(r);
                        blockedPackages.add(r);
                        try {
                            appItem.setAppName(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(r, PackageManager.GET_META_DATA)).toString());
                        } catch (PackageManager.NameNotFoundException e) {
                            appItem.setAppName("System package");
                        }
                        listData.add(appItem);
                    }
                    blockDozeApps.notifyDataSetChanged();
                }

                Log.i(TAG, "Blocked packages: " + listData.size() + " packages in total");
            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "Error loading blocklist packages: " + e.getMessage());

            }
        });
    }

    public void showManuallyAddPackageDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.block_app_dialog_title))
                .content(R.string.name_of_package_doze_block)
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
                .title(getString(R.string.block_app_dialog_title))
                .content(R.string.name_of_package_doze_block_remove)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("com.spotify.music", "", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        verifyAndRemovePackage(input.toString());
                    }
                }).show();
    }


    public void verifyAndAddPackage(String packageName) {
        if (blockedPackages.contains(packageName)) {
            displayDialog(getString(R.string.info_text), getString(R.string.app_already_in_blocklist));
        } else {
            modifyBlockList(packageName, false);
            loadPackagesFromBlockList();
        }
    }

    public void verifyAndRemovePackage(String packageName) {
        if (!blockedPackages.contains(packageName)) {
            displayDialog(getString(R.string.info_text), getString(R.string.app_doesnt_exist_in_blocklist));
        } else {
            modifyBlockList(packageName, true);
            loadPackagesFromBlockList();
        }
    }

    public void modifyBlockList(String packageName, boolean remove) {
        if (remove) {
            Log.i(TAG, "Removing app " + packageName + " to Doze app blocklist");
            blockedPackages.remove(packageName);
            sharedPreferences.edit().putStringSet("dozeAppBlockList", new LinkedHashSet<>(blockedPackages)).apply();
        } else {
            blockedPackages.add(packageName);
            Log.i(TAG, "Adding app " + packageName + " to Doze app blocklist");
            sharedPreferences.edit().putStringSet("dozeAppBlockList", new LinkedHashSet<>(blockedPackages)).apply();
        }
    }

    public void displayDialog(String title, String message) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(getString(R.string.close_button_text), null);
        builder.show();
    }
}
