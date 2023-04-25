package com.suyashsrijan.forcedoze;

import android.content.Context;

public class MyApplication extends android.app.Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}
