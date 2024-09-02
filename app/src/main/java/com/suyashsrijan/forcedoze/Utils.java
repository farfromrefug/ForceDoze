package com.suyashsrijan.forcedoze;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.Context.BATTERY_SERVICE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Utils {

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWriteSettingsPermissionGranted(Context context) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
            return true;
        else return false;
    }

    public static boolean isDumpPermissionGranted(Context context) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED)
            return true;
        else return false;
    }

    public static boolean isReadPhoneStatePermissionGranted(Context context) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
            return true;
        else return false;
    }

    public static boolean isReadLogsPermissionGranted(Context context) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED)
            return true;
        else return false;
    }

    public static boolean isSecureSettingsPermissionGranted(Context context) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
            return true;
        else return false;
    }
    public static boolean isSecureSensorPrivacyPermissionGranted(Context context) {
        if (context.checkCallingOrSelfPermission("android.permission.MANAGE_SENSOR_PRIVACY") == PackageManager.PERMISSION_GRANTED)
            return true;
        else return false;
    }

    public static boolean isConnectedToCharger(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent != null) {
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        } else return false;
    }

    public static String getDateCurrentTimeZone(long timestamp) {
        //return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.UK).format(new Date(timestamp));
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");
        return dateFormat.format(cal.getTime());
    }

    public static int getBatteryLevel(Context context) {
        BatteryManager bm = (BatteryManager)context.getSystemService(BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static boolean checkForAutoPowerModesFlag() {
        return Resources.getSystem().getBoolean(Resources.getSystem().getIdentifier("config_enableAutoPowerModes", "bool", "android"));
    }

    public static boolean isDeviceRunningOnN() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static int diffInMins(long start, long end) {
        return (int) ((end - start) / 1000) / 60;
    }

    public static String timeSpentString(long start, long end) {
        long diff = end - start;

        if (diff < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        diff -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        diff -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        diff -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);

        return String.valueOf(days) +
                " days, " +
                hours +
                " hours, " +
                minutes +
                " minutes, " +
                seconds +
                " seconds";
    }

    public static void setAutoRotateEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
    }

    public static boolean isAutoRotateEnabled(Context context) {
        return android.provider.Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    public static boolean isAutoBrightnessEnabled(Context context) {
        try {
            return android.provider.Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    public static void setAutoBrightnessEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, enabled ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    public static boolean isUserInCommunicationCall(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return manager.getMode() == AudioManager.MODE_IN_CALL || manager.getMode() == AudioManager.MODE_IN_COMMUNICATION;
    }

    public static boolean isUserInCall(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK || manager.getCallState() == TelephonyManager.CALL_STATE_RINGING;
    }

    public static boolean isMobileDataEnabled(Context context) {
        //reading "mobile_data" does not work on all devices
        // return Settings.Secure.getInt(context.getContentResolver(), "mobile_data", 1) == 1;
        boolean mobileYN = false;

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
            int dataState = tm.getDataState();
            if(dataState != TelephonyManager.DATA_DISCONNECTED){
                mobileYN = true;
            }

        }

        return mobileYN;
    }

    public static boolean isWiFiEnabled(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }
    public static boolean isHotspotEnabled(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method method = null;
        try {
            ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            method = wifi.getClass().getDeclaredMethod("getWifiApState");
            method.setAccessible(true);
            int actualState = (Integer) method.invoke(wifi, (Object[]) null);
            boolean isActiveNetworkMetered = conMgr.isActiveNetworkMetered();
            return actualState == 12 || actualState == 13;
//            return conMgr.isActiveNetworkMetered() ||  actualState == 12 || actualState == 13;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isLockscreenTimeoutValueTooHigh(ContentResolver contentResolver) {
        return Settings.Secure.getInt(contentResolver, "lock_screen_lock_after_timeout", 5000) >= 5000;
    }

    public static float getLockscreenTimeoutValue(ContentResolver contentResolver) {
        return ((Settings.Secure.getInt(contentResolver, "lock_screen_lock_after_timeout", 5000) / 1000f) / 60f);
    }

    public static boolean doesSettingExist(String settingName) {
        String[] updatableSettings = {"ignoreIfHotspot", "turnOffDataInDoze", "turnOffWiFiInDoze", "ignoreLockscreenTimeout",
                "dozeEnterDelay", "useAutoRotateAndBrightnessFix", "enableSensors", "disableWhenCharging",
                "showPersistentNotif", "useNonRootSensorWorkaround"};
        return Arrays.asList(updatableSettings).contains(settingName);
    }

    public static void updateSettingBool(Context context, String settingName, boolean settingValue) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(settingName, settingValue).apply();
    }

    public static void updateSettingInt(Context context, String settingName, int settingValue) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(settingName, settingValue).apply();
    }

    public static boolean isSettingBool(String settingName) {
        // Since all the settings loaded dynamically by the service except dozeEnterDelay are bools,
        // return true only if settingName != dozeEnterDelay
        if (settingName.equals("dozeEnterDelay")) {
            return false;
        } else return true;
    }

    public static boolean isScreenOn(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() == Display.STATE_ON
                    || display.getState() == Display.STATE_UNKNOWN) {
                return true;
            }
        }
        return false;
    }
    static class ReloadSettingsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadSettings();
        }
    }
    static ReloadSettingsReceiver reloadSettingsReceiver;
    static boolean disableLogcat = false;
    static Context applicationContext;
    static {
        init();
    }
    private static void init() {
        applicationContext = MyApplication.getAppContext();
        reloadSettingsReceiver = new ReloadSettingsReceiver();
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(reloadSettingsReceiver, new IntentFilter("reload-settings"));
        disableLogcat = getDefaultSharedPreferences(applicationContext).getBoolean("disableLogcat", false);
    }

    public static void reloadSettings() {
        disableLogcat = getDefaultSharedPreferences(applicationContext).getBoolean("disableLogcat", false);
    }

    public static void logToLogcat(String TAG, String message) {
        if (!disableLogcat) {
            Log.i(TAG, message);
        }
    }
}
