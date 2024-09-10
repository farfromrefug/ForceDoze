package com.akylas.enforcedoze;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import eu.chainfire.libsuperuser.Shell;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.akylas.enforcedoze.Utils.logToLogcat;

public class ForceDozeService extends Service {

    private static final String CHANNEL_STATS = "CHANNEL_STATS";
    private static final String CHANNEL_TIPS = "CHANNEL_TIPS";

    private static Shell.Interactive rootSession;
    private static Shell.Interactive nonRootSession;
    boolean isSuAvailable = false;
    boolean disableWhenCharging = true;
    boolean disableMotionSensors = true;
    boolean useAutoRotateAndBrightnessFix = false;
    boolean showPersistentNotif = true;
    boolean ignoreLockscreenTimeout = false;
    boolean useNonRootSensorWorkaround = false;
    boolean turnOffAllSensorsInDoze = false;
    boolean turnOffBiometricsInDoze = false;
    boolean turnOffWiFiInDoze = false;
    boolean ignoreIfHotspot = false;
    boolean turnOffDataInDoze = false;
    boolean whitelistMusicAppNetwork = false;
    boolean wasWiFiTurnedOn = false;
    boolean wasMobileDataTurnedOn = false;
    boolean wasHotSpotTurnedOn = false;
    boolean maintenance = false;
    boolean setPendingDozeEnterAlarm = false;
    boolean disableStats = false;
    boolean disableLogcat = false;
    int dozeEnterDelay = 0;
    Timer enterDozeTimer;
    Timer disableSensorsTimer;
    Timer enableSensorsTimer;
    DozeReceiver localDozeReceiver;
    ReloadSettingsReceiver reloadSettingsReceiver;
    PendingIntentDozeReceiver pendingIntentDozeReceiver;
    ReloadNotificationBlocklistReceiver reloadNotificationBlocklistReceiver;
    ReloadAppsBlocklistReceiver reloadAppsBlocklistReceiver;
    NotificationCompat.Builder mStatsBuilder;
    PendingIntent reenterDozePendingIntent;
    PowerManager pm;
    AlarmManager alarmManager;
    PowerManager.WakeLock tempWakeLock;
    Set<String> dozeUsageData;
    Set<String> dozeNotificationBlocklist;
    Set<String> dozeAppBlocklist;
    String sensorWhitelistPackage = "";
    String state = "";
    Long timeEnterDoze = 0L;
    Long timeExitDoze = 0L;
    String lastScreenOff = "Unknown";
    int lastDozeEnterBatteryLife = 0;
    int lastDozeExitBatteryLife = 0;
    String TAG = "ForceDozeService";
    String lastKnownState = "null";
    
    private void log(String message) {
       logToLogcat(TAG, message);
    }

    public ForceDozeService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        localDozeReceiver = new DozeReceiver();
        reloadSettingsReceiver = new ReloadSettingsReceiver();
        reloadNotificationBlocklistReceiver = new ReloadNotificationBlocklistReceiver();
        reloadAppsBlocklistReceiver = new ReloadAppsBlocklistReceiver();
        pendingIntentDozeReceiver = new PendingIntentDozeReceiver();
        enterDozeTimer = new Timer();
        enableSensorsTimer = new Timer();
        disableSensorsTimer = new Timer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence statsName = getString(R.string.notification_channel_stats_name);
            String statsDescription = getString(R.string.notification_channel_stats_description);
            int statsImportance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel statsChannel = new NotificationChannel(CHANNEL_STATS, statsName, statsImportance);
            statsChannel.setDescription(statsDescription);

            CharSequence tipsName = getString(R.string.notification_channel_tips_name);
            String tipsDescription = getString(R.string.notification_channel_tips_description);
            int tipsImportance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel tipsChannel = new NotificationChannel(CHANNEL_TIPS, tipsName, tipsImportance);
            tipsChannel.setDescription(tipsDescription);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(statsChannel);
            notificationManager.createNotificationChannel(tipsChannel);
        }

        mStatsBuilder = new NotificationCompat.Builder(this, CHANNEL_STATS);
        pm = (PowerManager) getSystemService(POWER_SERVICE);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        if (Utils.isDeviceRunningOnN()) {
            filter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(reloadSettingsReceiver, new IntentFilter("reload-settings"));
        LocalBroadcastManager.getInstance(this).registerReceiver(reloadNotificationBlocklistReceiver, new IntentFilter("reload-notification-blocklist"));
        LocalBroadcastManager.getInstance(this).registerReceiver(reloadAppsBlocklistReceiver, new IntentFilter("reload-app-blocklist"));
        LocalBroadcastManager.getInstance(this).registerReceiver(pendingIntentDozeReceiver, new IntentFilter("reenter-doze"));
        this.registerReceiver(localDozeReceiver, filter);
        turnOffDataInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffDataInDoze", false);
        ignoreIfHotspot = getDefaultSharedPreferences(getApplicationContext()).getBoolean("ignoreIfHotspot", true);
        turnOffWiFiInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffWiFiInDoze", false);
        turnOffAllSensorsInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffAllSensorsInDoze", false);
        turnOffBiometricsInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffBiometricsInDoze", false);
        whitelistMusicAppNetwork = getDefaultSharedPreferences(getApplicationContext()).getBoolean("whitelistMusicAppNetwork", false);
        ignoreLockscreenTimeout = getDefaultSharedPreferences(getApplicationContext()).getBoolean("ignoreLockscreenTimeout", true);
        useNonRootSensorWorkaround = getDefaultSharedPreferences(getApplicationContext()).getBoolean("useNonRootSensorWorkaround", false);
        dozeEnterDelay = getDefaultSharedPreferences(getApplicationContext()).getInt("dozeEnterDelay", 0);
        useAutoRotateAndBrightnessFix = getDefaultSharedPreferences(getApplicationContext()).getBoolean("autoRotateAndBrightnessFix", false);
        sensorWhitelistPackage = getDefaultSharedPreferences(getApplicationContext()).getString("sensorWhitelistPackage", "");
        disableMotionSensors = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableMotionSensors", true);
        disableStats = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableStats", false);
        disableLogcat = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableLogcat", false);
        disableWhenCharging = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableWhenCharging", true);
        isSuAvailable = getDefaultSharedPreferences(getApplicationContext()).getBoolean("isSuAvailable", false);
        showPersistentNotif = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("showPersistentNotif", true);
        dozeUsageData = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("dozeUsageDataAdvanced", new LinkedHashSet<String>());
        dozeNotificationBlocklist = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("notificationBlockList", new LinkedHashSet<String>());
        dozeAppBlocklist = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("dozeAppBlockList", new LinkedHashSet<String>());

        if (!Utils.isDumpPermissionGranted(getApplicationContext())) {
            if (isSuAvailable) {
                grantDumpPermission();
            }
        }

        if (Utils.isDeviceRunningOnN()) {
            if (!Utils.isSecureSettingsPermissionGranted(getApplicationContext())) {
                if (isSuAvailable) {
                    grantSecureSettingsPermission();
                }
            }
        }

        if (!Utils.isReadPhoneStatePermissionGranted(getApplicationContext())) {
            if (isSuAvailable) {
                grantReadPhoneStatePermission();
            }
        }

        // To initialize root shell/shell on service start
        if (isSuAvailable) {
            executeCommandWithRoot("whoami");
        } else {
            executeCommand("whoami");
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("Stopping service and enabling sensors");
        this.unregisterReceiver(localDozeReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(reloadSettingsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pendingIntentDozeReceiver);
        if (disableMotionSensors) {
            executeCommand("dumpsys sensorservice enable");
        }
        if (rootSession != null) {
            rootSession.close();
            rootSession = null;
        }
        if (nonRootSession != null) {
            nonRootSession.close();
            nonRootSession = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        log("Service has now started");
        if (showPersistentNotif) {
            showPersistentNotification();
        }
        addSelfToDozeWhitelist();
        lastKnownState = getDeviceIdleState();
        return START_STICKY;
    }

    public void reloadSettings() {
        log("EnforceDoze settings reloaded ----------------------------------");
        dozeUsageData = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("dozeUsageDataAdvanced", new LinkedHashSet<String>());
        log("dozeUsageData: " + "Total Entries -> " + dozeUsageData.size());
        turnOffDataInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffDataInDoze", false);
        log("turnOffDataInDoze: " + turnOffDataInDoze);
        ignoreIfHotspot = getDefaultSharedPreferences(getApplicationContext()).getBoolean("ignoreIfHotspot", true);
        log("ignoreIfHotspot: " + ignoreIfHotspot);
        turnOffWiFiInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffWiFiInDoze", false);
        log("turnOffWiFiInDoze: " + turnOffWiFiInDoze);
        turnOffAllSensorsInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffAllSensorsInDoze", false);
        log("turnOffAllSensorsInDoze: " + turnOffAllSensorsInDoze);
        turnOffBiometricsInDoze = getDefaultSharedPreferences(getApplicationContext()).getBoolean("turnOffBiometricsInDoze", false);
        log("turnOffBiometricsInDoze: " + turnOffBiometricsInDoze);
        whitelistMusicAppNetwork = getDefaultSharedPreferences(getApplicationContext()).getBoolean("whitelistMusicAppNetwork", false);
        log("whitelistMusicAppNetwork: " + whitelistMusicAppNetwork);
        ignoreLockscreenTimeout = getDefaultSharedPreferences(getApplicationContext()).getBoolean("ignoreLockscreenTimeout", false);
        log("ignoreLockscreenTimeout: " + ignoreLockscreenTimeout);
        dozeEnterDelay = getDefaultSharedPreferences(getApplicationContext()).getInt("dozeEnterDelay", 0);
        log("dozeEnterDelay: " + dozeEnterDelay);
        useAutoRotateAndBrightnessFix = getDefaultSharedPreferences(getApplicationContext()).getBoolean("autoRotateAndBrightnessFix", false);
        log("useAutoRotateAndBrightnessFix: " + useAutoRotateAndBrightnessFix);
        sensorWhitelistPackage = getDefaultSharedPreferences(getApplicationContext()).getString("sensorWhitelistPackage", "");
        log("sensorWhitelistPackage: " + sensorWhitelistPackage);
        disableMotionSensors = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableMotionSensors", true);
        log("disableMotionSensors: " + disableMotionSensors);
        disableStats = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableStats", false);
        log("disableStats: " + disableStats);
        disableLogcat = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableLogcat", false);
        log("disableLogcat: " + disableLogcat);
        disableWhenCharging = getDefaultSharedPreferences(getApplicationContext()).getBoolean("disableWhenCharging", true);
        log("disableWhenCharging: " + disableWhenCharging);
        showPersistentNotif = getDefaultSharedPreferences(getApplicationContext()).getBoolean("showPersistentNotif", false);
        log("showPersistentNotif: " + showPersistentNotif);
        useNonRootSensorWorkaround = getDefaultSharedPreferences(getApplicationContext()).getBoolean("useNonRootSensorWorkaround", false);
        log("useNonRootSensorWorkaround: " + useNonRootSensorWorkaround);
        log("EnforceDoze settings reloaded ----------------------------------");
    }

    public void reloadNotificationBlockList() {
        log("Notification blocklist reloaded ----------------------------------");
        dozeNotificationBlocklist = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("notificationBlockList", new LinkedHashSet<String>());
        log("notificationBlockList: " + dozeNotificationBlocklist.size() + " items");
        log("Notification blocklist reloaded ----------------------------------");
    }

    public void reloadAppsBlockList() {
        log("Apps blocklist reloaded ----------------------------------");
        dozeAppBlocklist = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet("dozeAppBlockList", new LinkedHashSet<String>());
        log("dozeAppBlockList: " + dozeAppBlocklist.size() + " items");
        log("Apps blocklist reloaded ----------------------------------");
    }

    public void grantDumpPermission() {
        log("Granting android.permission.DUMP to com.akylas.enforcedoze");
        executeCommandWithRoot("pm grant com.akylas.enforcedoze android.permission.DUMP");
    }

    public void grantSecureSettingsPermission() {
        log("Granting android.permission.WRITRE_SECURE_SETTINGS to com.akylas.enforcedoze");
        executeCommandWithRoot("pm grant com.akylas.enforcedoze android.permission.WRITE_SECURE_SETTINGS");
    }

    public void grantReadPhoneStatePermission() {
        log("Granting android.permission.READ_PHONE_STATE to com.akylas.enforcedoze");
        executeCommandWithRoot("pm grant com.akylas.enforcedoze android.permission.READ_PHONE_STATE");
    }
    public void grantSensorPrivacyPermission() {
        log("Granting android.permission.MANAGE_SENSOR_PRIVACY to com.akylas.enforcedoze");
        executeCommandWithRoot("pm grant com.akylas.enforcedoze android.permission.MANAGE_SENSOR_PRIVACY");
    }

    public void addSelfToDozeWhitelist() {
        log("Checking self-whitelist capability....");
        log("Nougat: " + Utils.isDeviceRunningOnN());
        log("SU available: " + isSuAvailable);
        String packageName = getPackageName();
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            if (!Utils.isDeviceRunningOnN()) {
                log("Adding service to Doze whitelist for stability");
                executeCommand("dumpsys deviceidle whitelist +com.akylas.enforcedoze");
            } else if (Utils.isDeviceRunningOnN() && isSuAvailable) {
                log("Adding service to Doze whitelist for stability");
                executeCommandWithRoot("dumpsys deviceidle whitelist +com.akylas.enforcedoze");
            } else {
                log("Service cannot be added to Doze whitelist because user is on Nougat. Showing notification...");
                Intent notificationIntent = new Intent();
                notificationIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                        notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                Notification n = new NotificationCompat.Builder(this, CHANNEL_TIPS)
                        .setContentTitle("EnforceDoze")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText("EnforceDoze needs to be added to the Doze whitelist in order to work reliably. Please click on this notification to open the battery optimisation view, click on 'EnforceDoze' and select 'Don't' Optimize'"))
                        .setSmallIcon(R.drawable.ic_battery_health)
                        .setPriority(1)
                        .setContentIntent(intent)
                        .setOngoing(false).build();
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(8765, n);
            }
        } else {
            log("Service already in Doze whitelist for stability");
        }
    }

    public void enterDoze(Context context) {
        if (!getDeviceIdleState().equals("IDLE") || !lastKnownState.equals("IDLE")) {
            if (!Utils.isScreenOn(context)) {
                lastKnownState = "IDLE";
                if (tempWakeLock != null) {
                    if (tempWakeLock.isHeld()) {
                        log("Releasing ForceDozeTempWakelock");
                        tempWakeLock.release();
                    }
                }

                if (dozeAppBlocklist.size() != 0) {
                    log("Disabling apps that are in the Doze app blocklist");
                    for (String pkg : dozeAppBlocklist) {
                        setPackageState(pkg, false);
                    }
                }

                if (dozeNotificationBlocklist.size() != 0) {
                    log("Disabling notifications for apps in the Notification blocklist");
                    for (String pkg : dozeNotificationBlocklist) {
                        if (!dozeAppBlocklist.contains(pkg)) {
                            setNotificationEnabledForPackage(pkg, false);
                        }
                    }
                }
                timeEnterDoze = System.currentTimeMillis();
                if (Utils.isConnectedToCharger(getApplicationContext())) {
                    lastDozeEnterBatteryLife = 0;
                } else {
                    lastDozeEnterBatteryLife = Utils.getBatteryLevel(getApplicationContext());
                }
                log("Entering Doze");
                if (Utils.isDeviceRunningOnN()) {
                    if (isSuAvailable) {
                        executeCommandWithRoot("dumpsys deviceidle force-idle deep");
                    } else {
                        log("Unrooted device, putting custom values in device_idle_constants...");
                        Settings.Global.putString(getContentResolver(), "device_idle_constants", "inactive_to=600000,light_after_inactive_to=300000,idle_after_inactive_to=5100,sensing_to=5100,locating_to=5100,location_accuracy=10000");
                    }
                } else {
                    executeCommand("dumpsys deviceidle force-idle");
                }
                lastScreenOff = Utils.getDateCurrentTimeZone(System.currentTimeMillis());
                
                if (!disableStats) {
                    dozeUsageData.add(Long.toString(System.currentTimeMillis()).concat(",").concat(Float.toString(Utils.isConnectedToCharger(getApplicationContext()) ? 0.0f : Utils.getBatteryLevel(getApplicationContext()))).concat(",").concat("ENTER"));
                    saveDozeDataStats();
                }

                if (disableMotionSensors) {
                    disableSensorsTimer = new Timer();
                    disableSensorsTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            log("Disabling motion sensors");
                            if (sensorWhitelistPackage.equals("")) {
                                executeCommand("dumpsys sensorservice restrict");
                            } else {
                                log("Package " + sensorWhitelistPackage + " is whitelisted from sensorservice");
                                log("Note: Packages that get whitelisted are supposed to request sensor access again, if the app doesn't work, email the dev of that app!");
                                executeCommand("dumpsys sensorservice restrict " + sensorWhitelistPackage);
                            }
                        }
                    }, 2000);
                } else {
                    log("Not disabling motion sensors because disableMotionSensors=false");
                }
                enterDozeHandleNetwork(context);

            } else {
                log("Screen is on, skip entering Doze");
            }
        } else {
            log("enterDoze() received but skipping because device is already Dozing");
        }
    }

    public void exitDoze() {
        timeExitDoze = System.currentTimeMillis();
        if (Utils.isConnectedToCharger(getApplicationContext())) {
            lastDozeExitBatteryLife = 0;
        } else {
            lastDozeExitBatteryLife = Utils.getBatteryLevel(getApplicationContext());
        }
        lastKnownState = "ACTIVE";
        if (Utils.isDeviceRunningOnN()) {
            if (isSuAvailable) {
                executeCommandWithRoot("dumpsys deviceidle unforce");
            } else {
                Settings.Global.putString(getContentResolver(), "device_idle_constants", null);
            }
        } else {
            executeCommand("dumpsys deviceidle step");
        }

        log("Current Doze state: " + getDeviceIdleState());

        if (!disableStats) {
            dozeUsageData.add(Long.toString(System.currentTimeMillis()).concat(",").concat(Float.toString(Utils.isConnectedToCharger(getApplicationContext()) ? 0.0f : Utils.getBatteryLevel(getApplicationContext()))).concat(",").concat("EXIT"));
            saveDozeDataStats();
        }

        if (dozeAppBlocklist.size() != 0) {
            log("Re-enabling apps that are in the Doze app blocklist");
            for (String pkg : dozeAppBlocklist) {
                setPackageState(pkg, true);
            }
        }

        if (dozeNotificationBlocklist.size() != 0) {
            log("Re-enabling notifications for apps in the Notification blocklist");
            for (String pkg : dozeNotificationBlocklist) {
                if (!dozeAppBlocklist.contains(pkg)) {
                    setNotificationEnabledForPackage(pkg, true);
                }
            }
        }

        if (disableMotionSensors) {
            enableSensorsTimer = new Timer();
            enableSensorsTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    log("Re-enabling motion sensors");
                    executeCommand("dumpsys sensorservice enable");
                    autoRotateBrightnessFix();
                }
            }, 2000);
        }

        if (useNonRootSensorWorkaround) {
            try {
                if (reenterDozePendingIntent != null) {
                    reenterDozePendingIntent.cancel();
                    alarmManager.cancel(reenterDozePendingIntent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Timer updateNotif = new Timer();
        updateNotif.schedule(new TimerTask() {
            @Override
            public void run() {
                if (showPersistentNotif) {
                    updatePersistentNotification(lastScreenOff, Utils.diffInMins(timeEnterDoze, timeExitDoze), (lastDozeEnterBatteryLife - lastDozeExitBatteryLife));
                }
            }
        }, 2000);

    }

    public void executeCommand(final String command ) {
        executeCommand(command, null);
    }
    public void executeCommand(final String command,Shell.OnCommandResultListener2 onResult ) {
        AsyncTask.execute(() -> {
            if (nonRootSession != null) {
                nonRootSession.addCommand(command, 0, (Shell.OnCommandResultListener2) (commandCode, exitCode, STDOUT, STDERR) -> {
                    if (onResult != null) {
                        onResult.onCommandResult(commandCode, exitCode, STDOUT, STDERR);
                    }
                    printShellOutput(STDOUT);
                    printShellOutput(STDERR);
                });
            } else {
                nonRootSession = new Shell.Builder().
                        useSH().
                        setWatchdogTimeout(5).
                        setMinimalLogging(true).
                        open((success, reason) -> {
                            if (reason != Shell.OnShellOpenResultListener.SHELL_RUNNING) {
                                log("Error opening shell: exitCode " + reason);
                            } else {
                                nonRootSession.addCommand(command, 0, (Shell.OnCommandResultListener2) (commandCode, exitCode, STDOUT, STDERR) -> {
                                    if (onResult != null) {
                                        onResult.onCommandResult(commandCode, exitCode, STDOUT, STDERR);
                                    }
                                    printShellOutput(STDOUT);
                                });
                            }
                        });
            }
        });
    }
    public void executeCommandWithRoot(final String command ) {
        executeCommandWithRoot(command, null);
    }
    public void executeCommandWithRoot(final String command,Shell.OnCommandResultListener2 onResult ) {
        AsyncTask.execute(() -> {
            if (rootSession != null) {
                rootSession.addCommand(command, 0, (Shell.OnCommandResultListener2) (commandCode, exitCode, STDOUT, STDERR) -> {
                    if (onResult != null) {
                        onResult.onCommandResult(commandCode, exitCode, STDOUT, STDERR);
                    }
                    printShellOutput(STDOUT);
                    printShellOutput(STDERR);
                });
            } else {
                rootSession = new Shell.Builder().
                        useSU().
                        setWatchdogTimeout(5).
                        setMinimalLogging(true).
                        open((success, reason) -> {
                            if (reason != Shell.OnShellOpenResultListener.SHELL_RUNNING) {
                                log("Error opening root shell: exitCode " + reason);
                            } else {
                                rootSession.addCommand(command, 0, (Shell.OnCommandResultListener2) (commandCode, exitCode, STDOUT, STDERR) -> {
                                    if (onResult != null) {
                                        onResult.onCommandResult(commandCode, exitCode, STDOUT, STDERR);
                                    }
                                    printShellOutput(STDOUT);
                                });
                            }
                        });
            }
        });
    }

    public void printShellOutput(List<String> output) {
        if (output != null && !output.isEmpty()) {
            for (String s : output) {
                log(s);
            }
        }
    }

    public void saveDozeDataStats() {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("dozeUsageDataAdvanced");
        editor.apply();
        editor.putStringSet("dozeUsageDataAdvanced", dozeUsageData);
        editor.apply();
    }

    public void autoRotateBrightnessFix() {
        if (useAutoRotateAndBrightnessFix && Utils.isWriteSettingsPermissionGranted(getApplicationContext())) {
            log("Executing auto-rotate fix by doing a toggle");
            log("Current value: " + (Utils.isAutoRotateEnabled(getApplicationContext())) + " to " + (!Utils.isAutoRotateEnabled(getApplicationContext())));
            Utils.setAutoRotateEnabled(getApplicationContext(), !Utils.isAutoRotateEnabled(getApplicationContext()));
            try {
                log("Sleeping for 100ms");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }
            log("Current value: " + (Utils.isAutoRotateEnabled(getApplicationContext())) + " to " + !Utils.isAutoRotateEnabled(getApplicationContext()));
            Utils.setAutoRotateEnabled(getApplicationContext(), !Utils.isAutoRotateEnabled(getApplicationContext()));
            try {
                log("Sleeping for 100ms");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }
            log("Executing auto-brightness fix by doing a toggle");
            log("Current value: " + (Utils.isAutoBrightnessEnabled(getApplicationContext())) + " to " + (!Utils.isAutoBrightnessEnabled(getApplicationContext())));
            Utils.setAutoBrightnessEnabled(getApplicationContext(), !Utils.isAutoBrightnessEnabled(getApplicationContext()));
            try {
                log("Sleeping for 100ms");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }
            log("Current value: " + (Utils.isAutoBrightnessEnabled(getApplicationContext())) + " to " + (!Utils.isAutoBrightnessEnabled(getApplicationContext())));
            Utils.setAutoBrightnessEnabled(getApplicationContext(), !Utils.isAutoBrightnessEnabled(getApplicationContext()));
        }
    }

    public void showPersistentNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification n = mStatsBuilder
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(getString(R.string.stats_no_data)))
                .setSmallIcon(R.drawable.ic_battery_health)
                .setPriority(-2)
                .setContentIntent(intent)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(1234, n);
        } else {
            startForeground(1234, n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        }
    }

    public void updatePersistentNotification(String lastScreenOff, int timeSpentDozing, int batteryUsage) {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification n = mStatsBuilder
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(getString(R.string.stats_long_text, lastScreenOff, timeSpentDozing, batteryUsage))
                                .setSummaryText(getString(R.string.stats_summary_text, batteryUsage)))
                .setShowWhen(false)
                .setSmallIcon(R.drawable.ic_battery_health)
                .setPriority(-2)
                .setContentIntent(intent)
                .setOngoing(true)
                .build();
        startForeground(1234, n);
    }

    public void setMobileNetwork(Context context, int targetState) {

        if (!Utils.isReadPhoneStatePermissionGranted(context)) {
            grantReadPhoneStatePermission();
        }

        String command;
        try {
            String transactionCode = getTransactionCode(context);
            SubscriptionManager mSubscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            for (int i = 0; i < mSubscriptionManager.getActiveSubscriptionInfoCountMax(); i++) {
                if (transactionCode != null && transactionCode.length() > 0) {
                    @SuppressLint("MissingPermission") int subscriptionId = mSubscriptionManager.getActiveSubscriptionInfoList().get(i).getSubscriptionId();
                    command = "service call phone " + transactionCode + " i32 " + subscriptionId + " i32 " + targetState;
                    List<String> output = new ArrayList<>();
                    List<String> err = new ArrayList<>();
                    Shell.Pool.SU.run(command, output, err, false);
                    if (err.isEmpty()) {
                        for (String s : output) {
                            log(s);
                        }
                    } else {
                        log("Error occurred while executing command (" + err + ")");
                    }
                }
            }
        } catch (Exception e) {
            log("Failed to toggle mobile data: " + e.getMessage());
        }
    }

    private static String getTransactionCode(Context context) {
        try {
            final TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final Class<?> mTelephonyClass = Class.forName(mTelephonyManager.getClass().getName());
            final Method mTelephonyMethod = mTelephonyClass.getDeclaredMethod("getITelephony");
            mTelephonyMethod.setAccessible(true);
            final Object mTelephonyStub = mTelephonyMethod.invoke(mTelephonyManager);
            final Class<?> mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
            final Class<?> mClass = mTelephonyStubClass.getDeclaringClass();
            final Field field = mClass.getDeclaredField("TRANSACTION_setDataEnabled");
            field.setAccessible(true);
            return String.valueOf(field.getInt(null));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setNotificationEnabledForPackage(String packageName, boolean enabled) {
        int command = 0;
        try {
            @SuppressLint("PrivateApi") Field field = Class.forName("android.app.INotificationManager").getDeclaredClasses()[0].getDeclaredField("TRANSACTION_setNotificationsEnabledForPackage");
            field.setAccessible(true);
            command = field.getInt(null);
        } catch (ClassNotFoundException e) {
            log(e.toString());
        } catch (NoSuchFieldException e2) {
            log(e2.toString());
        } catch (IllegalAccessException e3) {
            log(e3.toString());
        }

        ArrayList<PackageInfo> packageInfos = new ArrayList<>(getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA));

        for (PackageInfo p : packageInfos) {
            if (p.packageName.equals(packageName)) {
                log((enabled ? "Turning on " : "Turning off ") + "notifications for " + packageName);
                String exec = String.format(Locale.US, "service call notification %d s16 %s i32 %d i32 %d", command, packageName, p.applicationInfo.uid, enabled ? 1 : 0);
                executeCommandWithRoot(exec);
            }
        }
    }

    public void setPackageState(String packageName, boolean enabled) {
        log((enabled ? "Enabling " : "Disabling ") + packageName);
        executeCommandWithRoot("pm " + (enabled ? "enable " : "disable ") + packageName);
    }

    public String getDeviceIdleState() {
        log("Fetching Device Idle state...");
        if (Utils.isDeviceRunningOnN()) {
            if (isSuAvailable) {
                if (rootSession != null) {
                    rootSession.addCommand("dumpsys deviceidle", 0, (Shell.OnCommandResultListener2) (commandCode, exitCode, output, stderr) -> {
                        if (!output.isEmpty()) {
                            String outputString = TextUtils.join(", ", output);
                            if (outputString.contains("mState=ACTIVE")) {
                                state = "ACTIVE";
                            } else if (outputString.contains("mState=INACTIVE")) {
                                state = "INACTIVE";
                            } else if (outputString.contains("mState=IDLE_PENDING")) {
                                state = "IDLE_PENDING";
                            } else if (outputString.contains("mState=SENSING")) {
                                state = "SENSING";
                            } else if (outputString.contains("mState=LOCATING")) {
                                state = "LOCATING";
                            } else if (outputString.contains("mState=IDLE")) {
                                state = "IDLE";
                            } else if (outputString.contains("mState=IDLE_MAINTENANCE")) {
                                state = "IDLE_MAINTENANCE";
                            } else if (outputString.contains("mState=PRE_IDLE")) {
                                state = "PRE_IDLE";
                            } else if (outputString.contains("mState=WAITING_FOR_NETWORK")) {
                                state = "WAITING_FOR_NETWORK";
                            } else if (outputString.contains("mState=OVERRIDE")) {
                                state = "OVERRIDE";
                            }
                        } else {
                            if (pm.isDeviceIdleMode()) {
                                state = "IDLE";
                            } else {
                                state = "ACTIVE";
                            }
                        }
                    });
                }
            } else {
                if (pm.isDeviceIdleMode()) {
                    state = "IDLE";
                } else {
                    state = "ACTIVE";
                }
            }
        } else {
            List<String> output = new ArrayList<>();
            List<String> err = new ArrayList<>();
            try {
                Shell.Pool.SU.run("dumpsys deviceidle", output, err, false);
            } catch (Shell.ShellDiedException e) {
                e.printStackTrace();
            }
            String outputString = TextUtils.join(", ", output);
            if (outputString.contains("mState=ACTIVE")) {
                state = "ACTIVE";
            } else if (outputString.contains("mState=INACTIVE")) {
                state = "INACTIVE";
            } else if (outputString.contains("mState=IDLE_PENDING")) {
                state = "IDLE_PENDING";
            } else if (outputString.contains("mState=SENSING")) {
                state = "SENSING";
            } else if (outputString.contains("mState=LOCATING")) {
                state = "LOCATING";
            } else if (outputString.contains("mState=IDLE")) {
                state = "IDLE";
            } else if (outputString.contains("mState=IDLE_MAINTENANCE")) {
                state = "IDLE_MAINTENANCE";
            }
        }

        return state;
    }


    public void disableMobileData() {
        executeCommandWithRoot("svc data disable", (commandCode, exitCode, STDOUT, STDERR) -> {
            log("disableMobileData: " + Utils.isMobileDataEnabled(getApplicationContext()));
        });
    }

    public void enableMobileData() {
        executeCommandWithRoot("svc data enable", (commandCode, exitCode, STDOUT, STDERR) -> {
            log("enableMobileData: " + Utils.isMobileDataEnabled(getApplicationContext()));
        });
    }

    public void disableWiFi() {
        if (isSuAvailable) {
            executeCommandWithRoot("svc wifi disable", (commandCode, exitCode, STDOUT, STDERR) -> {
                log("disableWiFi: " + Utils.isWiFiEnabled(getApplicationContext()));
            });
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(false);
            log("disableWiFi: " + Utils.isWiFiEnabled(getApplicationContext()));
        }
    }
    public void setAllSensorsState(Context context, boolean enabled) {
        if (!isSuAvailable) {
            return;
        }
        if (!Utils.isSecureSensorPrivacyPermissionGranted(context)) {
            grantSensorPrivacyPermission();
        }

        String command;
        try {
            int transactionCode = 4;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                transactionCode = 9;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                transactionCode = 8;
            }
            command = "service call sensor_privacy " + transactionCode + " i32 " +(enabled? 0:1);
            List<String> output = new ArrayList<>();
            List<String> err = new ArrayList<>();
            Shell.Pool.SU.run(command, output, err, false);
            if (err.isEmpty()) {
                for (String s : output) {
                    log(s);
                }
            } else {
                log("Error occurred while executing command (" + err + ")");
            }
        } catch (Exception e) {
            log("Failed to toggle sensor off: " + e.getMessage());
        }
    }

    public void setBiometricsSensorState(Context context, boolean enabled) {
        if (!isSuAvailable) {
            return;
        }
        if (!Utils.isSecureSettingsPermissionGranted(context)) {
            grantSecureSettingsPermission();
        }
        executeCommandWithRoot("settings put secure biometric_keyguard_enabled " +(enabled? 1:0));
    }

    public void enableWiFi() {
        if (isSuAvailable) {
            executeCommandWithRoot("svc wifi enable", (commandCode, exitCode, STDOUT, STDERR) -> {
                log("enableWiFi: " + Utils.isWiFiEnabled(getApplicationContext()));
            });
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(true);
            log("enableWiFi: " + Utils.isWiFiEnabled(getApplicationContext()));
        }
    }

    class ReloadSettingsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("User changed a setting, loading new settings into service");
            reloadSettings();
        }
    }

    class ReloadNotificationBlocklistReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("User modified Notification blocklist, loading new packages into service");
            reloadNotificationBlockList();
        }
    }

    class ReloadAppsBlocklistReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("User modified Doze app blocklist, loading new packages into service");
            reloadAppsBlockList();
        }
    }

    class PendingIntentDozeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("Pending intent broadcast received");
            setPendingDozeEnterAlarm = false;
            if (Utils.isDeviceRunningOnN()) {
                if (isSuAvailable) {
                    executeCommandWithRoot("dumpsys deviceidle force-idle deep");
                } else {
                    Settings.Global.putString(getContentResolver(), "device_idle_constants", "inactive_to=600000,light_after_inactive_to=300000,idle_after_inactive_to=5100,sensing_to=5100,locating_to=5100,location_accuracy=10000");
                }
            } else {
                executeCommand("dumpsys deviceidle force-idle");
            }
        }
    }

    public void actualEnterDozeHandleNetwork(Context context, String packageName) {
        log("playingPackageName: " + packageName);
        wasWiFiTurnedOn = wasWiFiTurnedOn || Utils.isWiFiEnabled(context);
        wasMobileDataTurnedOn = wasMobileDataTurnedOn || Utils.isMobileDataEnabled(context) ;
        wasHotSpotTurnedOn = Utils.isHotspotEnabled(context);

        if (turnOffAllSensorsInDoze) {
            log("Disabling All sensors");
            setAllSensorsState(context, false);
        }
        if (turnOffBiometricsInDoze) {
            log("Disabling Biometrics");
            setBiometricsSensorState(context, false);
        }

        if (turnOffWiFiInDoze && (!ignoreIfHotspot || !wasHotSpotTurnedOn) && wasWiFiTurnedOn && packageName == null) {
            log("Disabling WiFi");
            disableWiFi();
        }

        if (turnOffDataInDoze && wasMobileDataTurnedOn && (!ignoreIfHotspot || !wasHotSpotTurnedOn) && (packageName == null || wasWiFiTurnedOn)) {
            log("Disabling mobile data");
            disableMobileData();
        }
    }
    public void enterDozeHandleNetwork(Context context) {
        if (whitelistMusicAppNetwork) {
            try {
                NotificationService.Companion.getInstance().getPlayingPackageName((String packageName)->{
                    actualEnterDozeHandleNetwork(context, packageName);
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
                actualEnterDozeHandleNetwork(context, null);
            }
        } else {
            actualEnterDozeHandleNetwork(context, null);
        }


    }

    public void leaveDozeHandleNetwork(Context context) {
        if (turnOffWiFiInDoze) {
            log("wasWiFiTurnedOn: " + wasWiFiTurnedOn);
            if (wasWiFiTurnedOn) {
                log("Enabling WiFi");
                enableWiFi();
            }

        }
        if (turnOffAllSensorsInDoze) {
            log("Enabling All sensors");
            setAllSensorsState(context, true);
        }
        if (turnOffBiometricsInDoze) {
            log("Enabling biometrics");
            setBiometricsSensorState(context, true);
        }

        if (turnOffDataInDoze) {
            log("wasDataTurnedOn: " + wasMobileDataTurnedOn);
            if (wasMobileDataTurnedOn) {
                log("Enabling mobile data");
                enableMobileData();
            }
        }
        wasWiFiTurnedOn = false;
        wasMobileDataTurnedOn = false;
    }

    class DozeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            int time = Settings.Secure.getInt(getContentResolver(), "lock_screen_lock_after_timeout", 5000);
            if (time == 0) {
                time = 1000;
            }
            int delay = dozeEnterDelay * 1000;
            time = time + delay;

            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                log("Screen ON received");
                log("Last known Doze state: " + lastKnownState);

                if (tempWakeLock != null) {
                    if (tempWakeLock.isHeld()) {
                        log("Releasing ForceDozeTempWakelock");
                        tempWakeLock.release();
                    }
                }

                leaveDozeHandleNetwork(context);

                if (!getDeviceIdleState().equals("ACTIVE") || !lastKnownState.equals("ACTIVE")) {
                    log("Exiting Doze");
                    exitDoze();
                } else {
                    if (ignoreLockscreenTimeout) {
                        log("Cancelling enterDoze() because user turned on screen and " + (delay) + "ms has not passed OR disableWhenCharging=true");
                    } else {
                        log("Cancelling enterDoze() because user turned on screen and " + (time) + "ms has not passed OR disableWhenCharging=true");
                    }
                    enterDozeTimer.cancel();
                }

            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                log("Screen OFF received");
                if (disableWhenCharging && Utils.isConnectedToCharger(getApplicationContext())) {
                    log("Connected to charger and disableWhenCharging=true, skip entering Doze");
                } else if (Utils.isUserInCommunicationCall(context)) {
                    log("User is in a VOIP call or an audio/video chat, skip entering Doze");
                } else if (Utils.isUserInCall(context)) {
                    log("User is in a phone call, skip entering Doze");
                } else {
                    log("Doze delay: " + delay + "ms");
                    if (ignoreLockscreenTimeout) {
                        if (dozeEnterDelay == 0) {
                            log("Ignoring lockscreen timeout value and entering Doze immediately");
                            enterDoze(context);
                        } else {
                            log("Waiting for " + (delay) + "ms and then entering Doze");
                            tempWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "forcedoze:tempWakelock");
                            log("Acquiring temporary wakelock (ForceDozeTempWakelock)");
                            tempWakeLock.acquire(10*60*1000L /*10 minutes*/);
                            enterDozeTimer = new Timer();
                            enterDozeTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    enterDoze(context);
                                }
                            }, delay);
                        }
                    } else {
                        log("Waiting for " + (time) + "ms and then entering Doze");
                        if (Utils.isLockscreenTimeoutValueTooHigh(getContentResolver())) {
                            tempWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "forcedoze:tempWakelock");
                            log("Acquiring temporary wakelock (ForceDozeTempWakelock)");
                            tempWakeLock.acquire(10*60*1000L /*10 minutes*/);
                        }
                        enterDozeTimer = new Timer();
                        enterDozeTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                enterDoze(context);
                            }
                        }, time);
                    }

                }
            } else if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                if ((getDeviceIdleState().equals("IDLE") || !Utils.isScreenOn(context)) && disableWhenCharging) {
                    log("Charger connected, exiting Doze mode");
                    enterDozeTimer.cancel();
                    exitDoze();
                }
            } else if (intent.getAction().equals(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)) {
                if (!Utils.isScreenOn(context)) {
                    log("ACTION_DEVICE_IDLE_MODE_CHANGED received");
                    lastKnownState = getDeviceIdleState();
                    log("Current (Deep) state: " + getDeviceIdleState());
                }

                if (!Utils.isScreenOn(context) && getDeviceIdleState().equals("IDLE_MAINTENANCE")) {
                    if (!maintenance) {
                        log("Device exited Doze for maintenance");
                        if (!disableStats) {
                            dozeUsageData.add(Long.toString(System.currentTimeMillis()).concat(",").concat(Float.toString(Utils.getBatteryLevel(getApplicationContext()))).concat(",").concat("EXIT_MAINTENANCE"));
                            saveDozeDataStats();
                        }

                        leaveDozeHandleNetwork(context);
                        maintenance = true;
                    }
                } else if (!Utils.isScreenOn(context) && getDeviceIdleState().equals("IDLE")) {
                    if (maintenance) {
                        log("Device entered Doze after maintenance");
                        if (!disableStats) {
                            dozeUsageData.add(Long.toString(System.currentTimeMillis()).concat(",").concat(Float.toString(Utils.getBatteryLevel(getApplicationContext()))).concat(",").concat("ENTER_MAINTENANCE"));
                            saveDozeDataStats();
                        }
                        enterDozeHandleNetwork(context);
                        maintenance = false;
                    }
                }

                if (useNonRootSensorWorkaround) {
                    if (!setPendingDozeEnterAlarm) {
                        if (!Utils.isScreenOn(context) && (!getDeviceIdleState().equals("IDLE") || !getDeviceIdleState().equals("IDLE_MAINTENANCE"))) {
                            log("Device gone out of Doze, scheduling pendingIntent for enterDoze in 15 mins");
                            reenterDozePendingIntent = PendingIntent.getBroadcast(context, 1, new Intent(context, ReenterDoze.class), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 900000, reenterDozePendingIntent);
                            setPendingDozeEnterAlarm = true;
                        }
                    }
                }
            } else if (intent.getAction().equals("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED")) {
                if (!Utils.isScreenOn(context)) {
                    log("LIGHT_DEVICE_IDLE_MODE_CHANGED received");
                    log("Current (Light) state: " + getDeviceIdleState());
                    lastKnownState = getDeviceIdleState();
                }
            }
        }
    }

}
