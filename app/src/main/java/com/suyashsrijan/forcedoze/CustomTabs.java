package com.suyashsrijan.forcedoze;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.AnimRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

/**
 * Created by eliseomartelli on 19/12/15.
 */
public class CustomTabs {

    static CustomTabsClient mCustomTabsClient;
    static CustomTabsSession mCustomTabsSession;

    static String packageNameToUse;

    /**
     * Method used to set the context
     * @param context Context of an activity
     */
    public static Operable with(Context context){
        return new Operable(context);
    }

    public static class Operable {
        Style style;
        Context context;
        Class <?> fallbackClass;

        private Operable(Context context){
            this.context = context;
        }

        /**
         * Method used to warm the custom tabs
         */
        public Warmer warm(){
            return new Warmer(context).warm();
        }

        /**
         * Method used to set the style of the custom tabs
         * @param style The style you want to set
         */
        public Operable setStyle(Style style){
            this.style = style;
            return this;
        }

        public Operable setFallBackActivity(Class <?> fallbackClass){
            this.fallbackClass = fallbackClass;
            return this;
        }

        /**
         * Method used to open an Uri
         * @param uri The Uri you want to open
         * @param activity The current activity
         */
        public Operable openUrl(Uri uri, Activity activity){
            Context context = activity.getApplicationContext();

            if (packageNameToUse != null) {

                if (style == null) style = new Style(context);

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(mCustomTabsSession);

                if (style.toolbarColor != 0) {
                    builder.setToolbarColor(style.toolbarColor);
                }

                builder.setShowTitle(style.showTitle);

                if (style.startEnterAnimation != 0 && style.startCloseAnimation != 0) {
                    builder.setStartAnimations
                            (context, style.startEnterAnimation, style.startCloseAnimation);
                }

                if (style.exitEnterAnimation != 0 && style.exitCloseAnimation != 0) {
                    builder.setExitAnimations
                            (context, style.exitEnterAnimation, style.exitCloseAnimation);
                }

                if (style.closeButton != null) {
                    builder.setCloseButtonIcon(style.closeButton);
                }

                if (style.actionButton != null) {
                    builder.setActionButton(style.actionButton.icon, style.actionButton.description,
                            style.actionButton.pendingIntent, style.actionButton.tint);
                }

                if (style.menuItemArrayList != null){
                    for (Style.MenuItem item: style.menuItemArrayList){
                        builder.addMenuItem(item.description, item.pendingIntent);
                    }
                }

                builder.build().launchUrl(activity, uri);
            } else {
                Intent intent;

                if (fallbackClass != null){
                    intent = new Intent(activity, fallbackClass);
                } else {
                    intent = new Intent(Intent.ACTION_VIEW);
                }

                ActivityCompat.startActivity(
                        activity,
                        intent
                                .setData(uri)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        ActivityOptionsCompat
                                .makeCustomAnimation(activity.getApplicationContext(),
                                        style.startEnterAnimation,
                                        style.startCloseAnimation).toBundle());
            }
            return this;
        }

        /**
         * Method used to open an Uri
         * @param url The Url you want to open
         * @param activity The current activity
         */
        public Operable openUrl(String url, Activity activity){
            return openUrl(Uri.parse(url), activity);
        }

    }

    /**
     * This class defines the Style you want to apply to the Custom Tab
     */
    public static class Style {
        Context context;

        private int toolbarColor;
        private int startEnterAnimation;
        private int exitEnterAnimation;
        private int startCloseAnimation;
        private int exitCloseAnimation;
        private boolean showTitle = false;
        Bitmap closeButton;
        ActionButton actionButton;

        ArrayList<MenuItem> menuItemArrayList;

        public Style(Context context){
            this.context = context;
            menuItemArrayList = new ArrayList<>();
        }

        /**
         * Method used to add a Menu Item.
         * @param description The description of the action
         * @param pendingIntent The pending intent it executes
         */
        public Style addMenuItem(String description, PendingIntent pendingIntent){
            menuItemArrayList.add(new MenuItem(description, pendingIntent));
            return this;
        }

        /**
         * Method used to set the Action Button
         * @param icon The icon you've to show
         * @param description The description of the action
         * @param pendingIntent The pending intent it executes
         * @param tint True if you want to tint the icon, false if not.
         */
        public Style setActionButton(Bitmap icon, String description, PendingIntent pendingIntent,
                                     boolean tint) {
            this.actionButton = new ActionButton(icon, description, pendingIntent, tint);
            return this;
        }

        /**
         * Method used to set the Action Button
         * @param icon The icon you've to show
         * @param description The description of the action
         * @param pendingIntent The pending intent it executes
         * @param tint True if you want to tint the icon, false if not.
         */
        public Style setActionButton(@DrawableRes int icon, String description, PendingIntent
                pendingIntent,
                                     boolean tint) {
            this.actionButton = new ActionButton
                    (BitmapFactory.decodeResource(context.getResources(), icon),
                            description,
                            pendingIntent,
                            tint);
            return this;
        }

        /**
         * Method used to set the close button icon
         * @param closeButton The close button icon you want to show
         */
        public Style setCloseButton(@DrawableRes int closeButton) {
            this.closeButton = BitmapFactory.decodeResource(context.getResources(), closeButton);
            return this;
        }

        /**
         * Method used to set the close button icon
         * @param closeButton The close button icon you want to show
         */
        public Style setCloseButton(Bitmap closeButton) {
            this.closeButton = closeButton;
            return this;
        }

        /**
         * Method used to show (or not) the title
         * @param showTitle True if you want to show, false if not
         */
        public Style setShowTitle(boolean showTitle) {
            this.showTitle = showTitle;
            return this;
        }

        /**
         * Method used to set the Toolbar color of the custom tab
         * @param toolbarColor Color res id you want to set
         */
        public Style setToolbarColor(@ColorRes int toolbarColor) {
            this.toolbarColor = ContextCompat.getColor(context, toolbarColor);
            return this;
        }


        /**
         * Method used to set the Toolbar color of the custom tab
         * @param toolbarColor Color you want to set
         */
        public Style setToolbarColorInt(int toolbarColor) {
            this.toolbarColor = toolbarColor;
            return this;
        }


        /**
         * Method used to set the Animations when the Custom Tab opens
         * @param startEnterAnimation The Enter Animation of the custom tab
         * @param startCloseAnimation The Close Animation of the new activity
         */
        public Style setStartAnimation(@AnimRes int startEnterAnimation, @AnimRes int
                startCloseAnimation){
            this.startEnterAnimation = startEnterAnimation;
            this.startCloseAnimation = startCloseAnimation;
            return this;
        }

        /**
         * Method used to set the Animations when the Custom Tab opens
         * @param exitEnterAnimation The Enter Animation of the new activity
         * @param exitCloseAnimation The Close Animation of the custom tab
         */
        public Style setExitAnimation(@AnimRes int exitEnterAnimation, @AnimRes int
                exitCloseAnimation){
            this.exitEnterAnimation = exitEnterAnimation;
            this.exitCloseAnimation = exitCloseAnimation;
            return this;
        }

        public class ActionButton {
            Bitmap icon;
            String description;
            PendingIntent pendingIntent;
            boolean tint;

            public ActionButton(Bitmap icon, String description, PendingIntent pendingIntent,
                                boolean tint){
                this.icon = icon;
                this.description = description;
                this.pendingIntent = pendingIntent;
                this.tint = tint;
            }
        }

        public class MenuItem {
            String description;
            PendingIntent pendingIntent;

            public MenuItem(String description, PendingIntent pendingIntent){
                this.description = description;
                this.pendingIntent = pendingIntent;
            }
        }

    }

    public static class Warmer {
        private Context context;
        private CustomTabsServiceConnection mCustomTabServiceConnection;

        public Warmer(Context context) {
            this.context = context;
        }

        private Warmer warm(){
            mCustomTabServiceConnection =
                    new CustomTabsServiceConnection() {
                        @Override
                        public void onCustomTabsServiceConnected(ComponentName componentName,
                                                                 CustomTabsClient customTabsClient) {
                            mCustomTabsClient = customTabsClient;
                            mCustomTabsClient.warmup(0);
                            mCustomTabsSession = mCustomTabsClient.newSession(null);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            mCustomTabsClient = null;
                        }
                    };

            packageNameToUse = ChromePackageHelper.getPackageNameToUse(context);

            if (packageNameToUse != null){
                CustomTabsClient.bindCustomTabsService(context, packageNameToUse,
                        mCustomTabServiceConnection);
            }
            return this;
        }

        public void unwarm() {
            if (mCustomTabServiceConnection == null || packageNameToUse == null) return;
            context.unbindService(mCustomTabServiceConnection);
            mCustomTabsClient = null;
            mCustomTabsSession = null;
        }
    }
}