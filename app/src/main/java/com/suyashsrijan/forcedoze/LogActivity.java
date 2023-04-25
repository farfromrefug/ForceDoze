package com.suyashsrijan.forcedoze;

import static com.suyashsrijan.forcedoze.Utils.logToLogcat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nanotasks.BackgroundWork;
import com.nanotasks.Completion;
import com.nanotasks.Tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class LogActivity extends AppCompatActivity {

    public static String TAG = "ForceDoze";
    public List<String> log;
    boolean isSuAvailable;
    MaterialDialog progressDialog = null;

    private static void log(String message) {
        logToLogcat(TAG, message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        grantLogsPermissionAndPrintLog();
        progressDialog = new MaterialDialog.Builder(this)
                .title("Please wait")
                .cancelable(false)
                .autoDismiss(false)
                .content("Requesting SU access and fetching log")
                .progress(true, 0)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.debug_logs_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_share_log:
                saveAndShareLog();
                break;
            case R.id.action_share_fulllog:
                progressDialog = new MaterialDialog.Builder(this)
                        .title("Please wait")
                        .content("Requesting SU access and fetching log...")
                        .progress(true, 0)
                        .show();
                getFullLogcat();
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getAndPrintLogcat() {
        Tasks.executeInBackground(LogActivity.this, new BackgroundWork<List<String>>() {
            @Override
            public List<String> doInBackground() throws Exception {
                List<String> output = Shell.SH.run("logcat -d -s \"ForceDozeService\",\"ForceDoze\"");
                return output;
            }
        }, new Completion<List<String>>() {
            @Override
            public void onSuccess(Context context, List<String> result) {
                if (progressDialog != null) {
                    progressDialog.cancel();
                }
                EditText logcatEd = (EditText) findViewById(R.id.editText);
                if (result != null) {
                    log = result;
                    logcatEd.setLongClickable(false);
                    logcatEd.setFocusable(false);
                    logcatEd.setClickable(true);
                    logcatEd.setText(result.toString());
                } else {
                    log = null;
                    logcatEd.setLongClickable(false);
                    logcatEd.setFocusable(false);
                    logcatEd.setClickable(true);
                    logcatEd.setText("Unable to get logcat");
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "Error getting logcat: " + e.getMessage());
            }
        });
    }

    public void getFullLogcat() {
        Tasks.executeInBackground(LogActivity.this, new BackgroundWork<List<String>>() {
            @Override
            public List<String> doInBackground() throws Exception {
                List<String> output = Shell.SH.run("logcat -d");
                return output;
            }
        }, new Completion<List<String>>() {
            @Override
            public void onSuccess(Context context, List<String> result) {
                if (progressDialog != null) {
                    progressDialog.cancel();
                }

                if (result != null) {
                    saveAndShareFullLog(result);
                } else {
                    log("Unable to get full logcat");
                }
            }

            @Override
            public void onError(Context context, Exception e) {
                Log.e(TAG, "Error getting logcat: " + e.getMessage());
            }
        });
    }

    public void grantLogsPermissionAndPrintLog() {
        if (!Utils.isReadLogsPermissionGranted(getApplicationContext())) {
            Tasks.executeInBackground(LogActivity.this, new BackgroundWork<Boolean>() {
                @Override
                public Boolean doInBackground() throws Exception {
                    return Shell.SU.available();
                }
            }, new Completion<Boolean>() {
                @Override
                public void onSuccess(Context context, Boolean result) {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    isSuAvailable = result;
                    if (isSuAvailable) {
                        if (!Utils.isReadLogsPermissionGranted(context)) {
                            executeCommand("pm grant com.suyashsrijan.forcedoze android.permission.READ_LOGS");
                        }
                        getAndPrintLogcat();
                    } else {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(LogActivity.this);
                        builder.setTitle(getString(R.string.error_text));
                        builder.setMessage(getString(R.string.read_logcat_su_not_avail_or_denied_error_text));
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
            getAndPrintLogcat();
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
                    log("Error occurred while executing command (" + command + ")");
                }
            }
        });
    }

    public void printShellOutput(List<String> output) {
        if (!output.isEmpty()) {
            for (String s : output) {
                log(s);
            }
        }
    }

    public void saveAndShareLog() {
        File file = new File(getExternalFilesDir(null), "app_log_forcedoze.txt");
        try {
            OutputStream os = new FileOutputStream(file);
            os.write(log.toString().getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        shareLogFile("app_log_forcedoze.txt");
    }

    public void saveAndShareFullLog(List<String> logcat) {
        File file = new File(getExternalFilesDir(null), "full_logcat_forcedoze.txt");
        try {
            OutputStream os = new FileOutputStream(file);
            os.write(logcat.toString().getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        shareLogFile("full_logcat_forcedoze.txt");
    }

    public void shareLogFile(String filename) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(getExternalFilesDir(null), filename)));
        startActivity(Intent.createChooser(intent, ""));
    }
}
