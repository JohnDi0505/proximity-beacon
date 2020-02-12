package com.example.bluetoothscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission_group.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    TextView statusTextview;
    TextView mTime;
    Button searchBotton;

    ArrayList<String> bluetoothDevices = new ArrayList<>();
    ArrayList<String> BLEoutput = new ArrayList<>();

    ArrayAdapter arrayAdapter;
    BluetoothAdapter bluetoothAdapter;

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9003;
    private boolean mLocationPermissionGranted = false;

    Date current;
    String time;
    Date today = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("MM_dd_yyyy");
    String date = formatter.format(today);

    private final String csvName = "BLE_scandata_" + date + ".csv";
    private int clickIndex;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            current = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
            time = formatter.format(current);

            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                statusTextview.setText("Finished");
                searchBotton.setEnabled(true);
                csvWriter();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                String rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));

                if (!bluetoothDevices.contains(name) && !bluetoothDevices.contains(address)) {

                    if (name == null || name.equals("")) {
                        bluetoothDevices.add(address);
                        BLEoutput.add(clickIndex + "," + date + "," + time + "," + address + ","+ rssi + " dBm");
                    } else {
                        bluetoothDevices.add(name);
                        BLEoutput.add(clickIndex + "," + date + "," + time + "," + name + ","+ rssi + " dBm");
                    }
                }
            }
        }
    };

    public void csvWriter(){

        FileOutputStream fos = null;

        try {

            fos = openFileOutput(csvName, Context.MODE_APPEND);

            for (String bt_item: BLEoutput) {
                fos.write((bt_item + "\n").getBytes());
            }

            fos.close();
            Toast.makeText(this, "Scanned data has been saved to " + csvName + "!", Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void searchClicked(View view){

        clickIndex += 1;

        bluetoothDevices.clear();
        BLEoutput.clear();

        mTime.setText(date + " " + time);
        statusTextview.setText("Searching...");
        searchBotton.setEnabled(false);
        bluetoothAdapter.startDiscovery();

    }

    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        clickIndex = 0;
        bluetoothDevices.clear();
        BLEoutput.clear();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getLocationPermission();

        listView = findViewById(R.id.listView);
        statusTextview = findViewById(R.id.statusTextview);
        searchBotton = findViewById(R.id.searchBotton);
        mTime = findViewById(R.id.mTime);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, BLEoutput);
        listView.setAdapter(arrayAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, intentFilter);

        bluetoothAdapter.startDiscovery();
    }
}

