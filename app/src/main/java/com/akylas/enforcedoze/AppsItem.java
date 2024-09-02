package com.akylas.enforcedoze;

import java.io.Serializable;

public class AppsItem implements Serializable {

    private String appName;
    private String appPackageName;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }

}