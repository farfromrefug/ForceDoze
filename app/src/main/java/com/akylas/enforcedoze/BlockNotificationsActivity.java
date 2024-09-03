package com.akylas.enforcedoze;

import static com.akylas.enforcedoze.Utils.logToLogcat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

public class BlockNotificationsActivity extends AppCompatActivity {
    ListView listView;
    SharedPreferences sharedPreferences;
    AppsAdapter blockNotificationApps;
    ArrayList<String> blockedPackages;
    ArrayList<AppsItem> listData;
    public static String TAG = "EnforceDoze";
    boolean isSuAvailable = false;
    MaterialDialog progressDialog = null;

    private static void log(String message) {
        logToLogcat(TAG, message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_blocklist_apps);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = (ListView) findViewById(R.id.listViewNotifBlockList);
        blockedPackages = new ArrayList<>();
        listData = new ArrayList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        blockNotificationApps = new AppsAdapter(this, listData);
        listView.setAdapter(blockNotificationApps);
        loadPackagesFromBlockList();
        isSuAvailable = sharedPreferences.getBoolean("isSuAvailable", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Utils.isMyServiceRunning(ForceDozeService.class, BlockNotificationsActivity.this)) {
            Intent intent = new Intent("reload-notification-blocklist");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.block_app_notifications_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_notification_blocklist) {
            startActivityForResult(new Intent(BlockNotificationsActivity.this, PackageChooserActivity.class), 503);
        } else if (id == R.id.action_remove_notification_blocklist) {
            startActivityForResult(new Intent(BlockNotificationsActivity.this, PackageChooserActivity.class), 504);
        } else if (id == R.id.action_add_notification_blocklist_package) {
            showManuallyAddPackageDialog();
        } else if (id == R.id.action_remove_notification_blocklist_package) {
            showManuallyRemovePackageDialog();
        } else if (id == R.id.action_notification_blacklist_more_info) {
            displayDialog(getString(R.string.notif_blocklist_dialog_title), getString(R.string.notif_blocklist_dialog_text));
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (requestCode == 503) {
                String pkg = data.getStringExtra("package_name");
                verifyAndAddPackage(pkg);
            } else if (requestCode == 504) {
                String pkg = data.getStringExtra("package_name");
                verifyAndRemovePackage(pkg);
            }
        }
    }

    public void loadPackagesFromBlockList() {
        log("Loading blocked packages...");
        progressDialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.please_wait_text))
                .autoDismiss(false)
                .cancelable(false)
                .content(getString(R.string.loading_blocked_packages))
                .progress(true, 0)
                .show();

        Tasks.executeInBackground(BlockNotificationsActivity.this, new BackgroundWork<List<String>>() {
            @Override
            public List<String> doInBackground() throws Exception {
                List<String> packages = new ArrayList<>(sharedPreferences.getStringSet("notificationBlockList", new LinkedHashSet<String>()));
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
                    blockNotificationApps.notifyDataSetChanged();
                }

                log("Blocked packages: " + listData.size() + " packages in total");
            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "Error loading blocklist packages: " + e.getMessage());

            }
        });
    }

    public void showManuallyAddPackageDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.block_app_notif_dialog_title))
                .content(R.string.name_of_package_notif_block)
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
                .title(getString(R.string.block_app_notif_dialog_title))
                .content(R.string.name_of_package_notif_block_remove)
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
            log("Removing app " + packageName + " to Notification blocklist");
            blockedPackages.remove(packageName);
            sharedPreferences.edit().putStringSet("notificationBlockList", new LinkedHashSet<>(blockedPackages)).apply();
        } else {
            blockedPackages.add(packageName);
            log("Adding app " + packageName + " to Notification blocklist");
            sharedPreferences.edit().putStringSet("notificationBlockList", new LinkedHashSet<>(blockedPackages)).apply();
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
