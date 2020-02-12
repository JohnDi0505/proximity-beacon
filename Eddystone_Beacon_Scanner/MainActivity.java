package com.example.eddystonebeaconscanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.BleSignal;
import com.google.android.gms.nearby.messages.Distance;
import com.google.android.gms.nearby.messages.EddystoneUid;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesClient;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    MessagesClient mMessagesClient;
    MessageListener mMessageListener;
    EddystoneUid eddystoneUid;

    Date current;
    String time;
    Date today = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("MM_dd_yyyy");
    String date = formatter.format(today);

    private static final String MY_EDDYSTONE_UID_NAMESPACE = "edd1ebeac04e5defa017";
    private static final String OUTPUT_DIR = "/sdcard"+ File.separator + "BLE_data";
    private static final int WRITE_PERMISSION_STATIC_CODE_IDENTIFIER = 1;
    private final String csvName = "BLE_data_" + date + ".csv";

    TextView statusTextview;
    Button mSubscribe;
    Button mUnsubscribe;
    Button mCapture;

    ListView listView;
    ArrayList<String> UIDs = new ArrayList<>();
    ArrayList<Integer> RSSIs = new ArrayList<>();
    ArrayList<String> BLEoutput = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;

    String UID;
    int RSSI;
    int n;
    int len;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextview = findViewById(R.id.mStatusText);
        mSubscribe = findViewById(R.id.mSubscribe);
        mUnsubscribe = findViewById(R.id.mUnsubscribe);
        mCapture = findViewById(R.id.mCapture);
        listView = findViewById(R.id.mListView);

        statusTextview.setText("");
        mUnsubscribe.setEnabled(false);
        mCapture.setEnabled(false);

        getLocationPermission();
        getDataWritingPermission();

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, BLEoutput);

        n = 0;

    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mMessagesClient = Nearby.getMessagesClient(this, new MessagesOptions.Builder()
                    .setPermissions(NearbyPermissions.BLE)
                    .build());
        }
    }

    private void getDataWritingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_PERMISSION_STATIC_CODE_IDENTIFIER);
        }
    }

    public void Subscribing(View view) {
        statusTextview.setText("Subscribing...");
        mSubscribe.setEnabled(false);
        mUnsubscribe.setEnabled(true);
        mCapture.setEnabled(true);

        Toast.makeText(this, "Subscribing to nearby beacon messages!", Toast.LENGTH_LONG).show();

        MessageFilter messageFilter = new MessageFilter.Builder()
                .includeEddystoneUids(MY_EDDYSTONE_UID_NAMESPACE, null)
                .build();

        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .setFilter(messageFilter)
                .build();

        mMessageListener = new MessageListener() {

            @Override
            public void onFound(final Message message) {
                if (Message.MESSAGE_NAMESPACE_RESERVED.equals(message.getNamespace())
                        && Message.MESSAGE_TYPE_EDDYSTONE_UID.equals(message.getType())) {
                    eddystoneUid = EddystoneUid.from(message);
                }
            }

            @Override
            public void onBleSignalChanged(final Message message, final BleSignal bleSignal) {
                UID = eddystoneUid.getInstance();
                RSSI = bleSignal.getRssi();
                if (!UIDs.contains(UID)) {
                    UIDs.add(UID);
                    RSSIs.add(RSSI);
                } else {
                    int idx = UIDs.indexOf(UID);
                    RSSIs.set(idx, RSSI);
                }

                BLEoutput.clear();

                if (UIDs != null) {
                    for (int i =0; i < UIDs.size(); i++) {
                        String entry = UIDs.get(i) + ": " + RSSIs.get(i);
                        BLEoutput.add(entry);
                    }
                }

                len = BLEoutput.size();
                listView.setAdapter(arrayAdapter);
            }

//            @Override
//            public void onDistanceChanged(final Message message, final Distance distance) {
//                Log.i(TAG, "" + eddystoneUid.getInstance() + ":" + distance);
//                BLEoutput.add(eddystoneUid.getInstance());
//                Log.i(TAG, "BLE_list size: " + BLEoutput.size());
//                Log.i(TAG, "adapter size: " + arrayAdapter.getCount());
//                arrayAdapter.notifyDataSetChanged();
//                listView.setAdapter(arrayAdapter);
//            }

            @Override
            public void onLost(Message message) {
                eddystoneUid = EddystoneUid.from(message);
                int idx = UIDs.indexOf(eddystoneUid.getInstance());
                try {
                    UIDs.remove(UIDs.get(idx));
                    RSSIs.remove(RSSIs.get(idx));

                    BLEoutput.clear();

                    if (UIDs != null) {
                        for (int i =0; i < UIDs.size(); i++) {
                            String entry = UIDs.get(i) + ": " + RSSIs.get(i);
                            BLEoutput.add(entry);
                        }
                    }
                    listView.setAdapter(arrayAdapter);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Nearby.getMessagesClient(this).subscribe(mMessageListener, options);

    }

    public void Unsubscribing(View view) {
        statusTextview.setText("Unsubscribed");
        mUnsubscribe.setEnabled(false);
        mSubscribe.setEnabled(true);
        mCapture.setEnabled(false);
        arrayAdapter.clear();
        listView.setAdapter(arrayAdapter);
        Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
        Toast.makeText(this, "Stop subscribing!", Toast.LENGTH_LONG).show();
    }

    public void Capturing(View view) {

        n++;

        current = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        time = formatter.format(current);

        File folder = new File(OUTPUT_DIR);

        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {

            File test = new File(folder, csvName);

            try {
                FileWriter writer = new FileWriter(test, true);

                for (String bt_item: BLEoutput) {
                    writer.append("" + n + "," + time + "," + bt_item + "\n");
                }
                writer.flush();
                writer.close();
                Toast.makeText(this,
                        "A data entry has been saved to " + OUTPUT_DIR + File.separator + csvName + "!",
                        Toast.LENGTH_LONG).show();


            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // Do something else on failure
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
