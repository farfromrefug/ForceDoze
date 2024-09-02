package com.akylas.enforcedoze;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class AboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        CustomTabs.with(getApplicationContext()).warm();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0f);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showLicences(View v) {
        final Notices notices = new Notices();
        notices.addNotice(new Notice("Material Dialogs", "https://github.com/afollestad/material-dialogs", "Aidan Michael Follestad", new MITLicense()));
        notices.addNotice(new Notice("libsuperuser", "https://github.com/Chainfire/libsuperuser", "Chainfire", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("Nanotasks", "https://github.com/fabiendevos/nanotasks", "Fabien Devos", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("ProcessPhoenix", "https://github.com/JakeWharton/ProcessPhoenix", "Jake Wharton", new ApacheSoftwareLicense20()));
//        notices.addNotice(new Notice("ckChangelog", "https://github.com/cketti/ckChangeLog", "cketti", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("SimpleCustomTabs", "https://github.com/eliseomartelli/SimpleCustomTabs", "Eliseo Martelli", new MITLicense()));
        notices.addNotice(new Notice("MaterialList", "https://github.com/dexafree/MaterialList", "Dexafree", new MITLicense()));
        new LicensesDialog.Builder(this).setNotices(notices).setIncludeOwnLicense(true).build().show();
    }

//    public void signUpBeta(View v) {
//        CustomTabs.with(getApplicationContext())
//                .setStyle(new CustomTabs.Style(getApplicationContext())
//                        .setShowTitle(true)
//                        .setExitAnimation(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
//                        .setToolbarColor(R.color.colorPrimary))
//                .openUrl("https://play.google.com/apps/testing/com.akylas.enforcedoze", this);
//    }

    public void showTranslationCreditsDialog(View v) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.translation_credits_dialog_title));
        builder.setMessage(getString(R.string.translation_credits_dialog_text));
        builder.setPositiveButton(getString(R.string.okay_button_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    public void showPrivacyPolicy(View v) {
        CustomTabs.with(getApplicationContext())
                .setStyle(new CustomTabs.Style(getApplicationContext())
                        .setShowTitle(true)
                        .setExitAnimation(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .setToolbarColor(R.color.colorPrimary))
                .openUrl("https://www.akylas.fr/privacy", this);
    }
}
