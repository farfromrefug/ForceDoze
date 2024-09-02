package com.akylas.enforcedoze;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class TaskerBroadcastsActivity extends AppCompatActivity {

    ArrayList<TaskerBroadcastsItem> items;
    ListView listView;
    TaskerBroadcastsAdapter taskerBroadcastsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasker_broadcasts);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = (ListView) findViewById(R.id.listViewBroadcasts);
        items = new ArrayList<>();
        items.add(new TaskerBroadcastsItem("com.akylas.enforcedoze.ENABLE_FORCEDOZE",
                "Broadcast values required: None"));
        items.add(new TaskerBroadcastsItem("com.akylas.enforcedoze.DISABLE_FORCEDOZE",
                "Broadcast values required: None"));
        items.add(new TaskerBroadcastsItem("com.akylas.enforcedoze.ADD_WHITELIST",
                "Broadcast values required:\npackageName\n\npackageName has to be the full " +
                        "package name of the app you want to add to the whitelist"));
        items.add(new TaskerBroadcastsItem("com.akylas.enforcedoze.REMOVE_WHITELIST",
                "Broadcast values required:\npackageName\n\npackageName has to be the full " +
                        "package name of the app you want to remove from the whitelist"));
        items.add(new TaskerBroadcastsItem("com.akylas.enforcedoze.CHANGE_SETTING",
                "Broadcast values required:\nsettingName\nsettingValue\n\nsettingName can be one of the following:" +
                        "\n1) turnOffDataInDoze\n2) turnOffWiFiInDoze\n3) ignoreLockscreenTimeout" +
                        "\n4) dozeEnterDelay\n5) useAutoRotateAndBrightnessFix\n6) enableSensors" +
                        "\n7) disableWhenCharging\n8) showPersistentNotif\n" +
                        "\n9) useNonRootSensorWorkaround\n\nsettingValue can be one of the " +
                        "following:\n1) true\n2) false\n3) an integer value (ONLY in case of dozeEnterDelay)"));
        taskerBroadcastsAdapter = new TaskerBroadcastsAdapter(this, items);
        listView.setAdapter(taskerBroadcastsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                ClipData broadcastData = ClipData.newPlainText("fd_broadcast", ((TextView)view.findViewById(R.id.broadcastName)).getText());
                clipboard.setPrimaryClip(broadcastData);
                Toast.makeText(getApplicationContext(), "Copied broadcast!", Toast.LENGTH_SHORT).show();
            }
        });
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
}