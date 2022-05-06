package com.iris.StayAwake;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

// My Smart Band 6 key: 0x2cc639cb653f9d796fe1ad151ba10c3a

public class MainActivity extends AppCompatActivity {

    public static UUID MiBand_Service_UUID = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb");
    public static UUID Auth_Characteristic_UUID = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    public static UUID Descriptor_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static UUID MiBand1_Service_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    public static UUID service = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static UUID measurementCharacteristic = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static UUID Sensor = UUID.fromString("00000001-0000-3512-2118-0009af100700");
    public static UUID controlCharacteristic = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");

    public static UUID Alert_Service_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static UUID Alert_Characteristic_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    private static final UUID Battery_Service_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Level_UUID = UUID.fromString("00000006-0000-3512-2118-0009af100700");

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names

    // GUI Components
    private TextView mBluetoothStatus, mHeader, mHrValue, mBatteryValue;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private Button mDisconnectBtn;
    private ListView mDevicesListView;

    private LinearLayout mLayout1, mLayout2, mLayout3, mLayout4, mLayout5, mLayout6, mLayout7, mLayout8;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private BluetoothGatt mBluetoothGatt;
    private ArrayList<Integer> heartRateValues;
    private Timer ping;
    private HRDBHelper dbHelper;
    private DEVHelper devHelper;
    private SQLiteDatabase db;
    private SQLiteDatabase db1;

    private Handler mHandler;

    //byte[] auth_char_key = new byte[]{0x2c, (byte) 0xc6, 0x39, (byte) 0xcb, 0x65, 0x3f, (byte) 0x9d, 0x79, 0x6f, (byte) 0xe1, (byte) 0xad, 0x15, 0x1b, (byte) 0xa1, 0x0c, 0x3a};
    byte[] auth_char_key = new byte[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45};
    byte[] secret_key;

    String name;
    String address;


    private void display() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLayout1.setVisibility(View.GONE);
                mLayout2.setVisibility(View.GONE);
                mLayout3.setVisibility(View.GONE);
                mDevicesListView.setVisibility(View.GONE);
                mBTArrayAdapter.clear();


                mLayout4.setVisibility(View.VISIBLE);
                mLayout5.setVisibility(View.VISIBLE);
                mLayout6.setVisibility(View.VISIBLE);
                mLayout7.setVisibility(View.VISIBLE);
                mLayout8.setVisibility(View.VISIBLE);
                mHrValue.setVisibility(View.VISIBLE);
                mHeader.setText(name);
            }
        });
    }

    private void back() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLayout1.setVisibility(View.VISIBLE);
                mLayout2.setVisibility(View.VISIBLE);
                mLayout3.setVisibility(View.VISIBLE);
                mDevicesListView.setVisibility(View.VISIBLE);

                mLayout4.setVisibility(View.GONE);
                mLayout5.setVisibility(View.GONE);
                mLayout6.setVisibility(View.GONE);
                mLayout7.setVisibility(View.GONE);
                mLayout8.setVisibility(View.GONE);
                mHrValue.setVisibility(View.GONE);
                mHeader.setText("Stay Awake");
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
        unregisterReceiver(blReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHeader = (TextView)findViewById(R.id.header_text);
        mHrValue = (TextView)findViewById(R.id.Hr_txt);
        mBatteryValue = (TextView)findViewById(R.id.battery_txt);
        mBluetoothStatus = (TextView)findViewById(R.id.bluetooth_status);
        mScanBtn = (Button)findViewById(R.id.on);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.paired);
        mDisconnectBtn = (Button)findViewById(R.id.disconnect);

        mLayout1 = (LinearLayout)findViewById(R.id.Layout1);
        mLayout2 = (LinearLayout)findViewById(R.id.Layout2);
        mLayout3 = (LinearLayout)findViewById(R.id.Layout3);
        mLayout4 = (LinearLayout)findViewById(R.id.Layout4);
        mLayout5 = (LinearLayout)findViewById(R.id.Layout5);
        mLayout6 = (LinearLayout)findViewById(R.id.Layout6);
        mLayout7 = (LinearLayout)findViewById(R.id.Layout7);
        mLayout8 = (LinearLayout)findViewById(R.id.Layout8);

        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView)findViewById(R.id.devices_list_view);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);
        dbHelper = new HRDBHelper(this);
        db = dbHelper.getWritableDatabase();
        devHelper = new DEVHelper(this);
        db1 = devHelper.getWritableDatabase();

        heartRateValues = new ArrayList<>();
        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mHandler = new Handler(Looper.getMainLooper());

        if (mBTAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn();
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff();

                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices();
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover();
                }
            });

            mDisconnectBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    mBluetoothGatt.disconnect();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothStatus.setText("Disconnected");
                        }
                    });
                    ping.stop();
                    stopHrMeasure();
                    Cursor cursor = db.query(
                            HeartRateContract.HeartRateEntry.TABLE_NAME,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);

                    while(cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(HeartRateContract.HeartRateEntry.COLUMN_ADDRESS));
                        Log.d("DB", "Id: " + String.valueOf(id));
                    }
                    //db1.execSQL("DELETE FROM " + DeviceContract.DeviceEntry.TABLE_NAME);
                    //db.execSQL("DELETE FROM " + HeartRateContract.HeartRateEntry.TABLE_NAME);
                    close();
                    back();
                }
            });
        }
    }

    private void bluetoothOn(){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mBluetoothStatus.setText("Enabled");
            }
            else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }


    private void discover(){
        // Check if the device is already discovering
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location");
        builder.setMessage("Please, make sure you have location enabled before discovering.");
        builder.setPositiveButton("Ok", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothStatus.setText("Discovering...");
                    }
                });
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                if (TextUtils.isEmpty(device.getName())) {
                    mBTArrayAdapter.add(device.getAddress());
                } else {
                    mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };


    private void listPairedDevices(){
        mBTArrayAdapter.clear();
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }


    /**
     * Test playground
     */

    private void getBattery() {
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(Battery_Service_UUID).getCharacteristic(Battery_Level_UUID);
        if (characteristic != null) {
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic chrt) {
        gatt.setCharacteristicNotification(chrt, true);
        BluetoothGattDescriptor descriptor = chrt.getDescriptor(Descriptor_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    private void executeAuthorisationSequence(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value[0] == 16 && value[1] == 2 && value[2] == 1) {
            try {
                secret_key = Arrays.copyOfRange(value, 3, 19);
                String CIPHER_TYPE = "AES/ECB/NoPadding";
                Cipher cipher = Cipher.getInstance(CIPHER_TYPE);

                String CIPHER_NAME = "AES";
                SecretKeySpec key = new SecretKeySpec(auth_char_key, CIPHER_NAME);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] bytes = cipher.doFinal(secret_key);
                byte[] rq = Arrays.copyOf(new byte[]{0x03, 0x08}, 2 + bytes.length);
                System.arraycopy(bytes, 0, rq, 2, bytes.length);

                characteristic.setValue(rq);
                gatt.writeCharacteristic(characteristic);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startHrMeasure() {
        BluetoothGattCharacteristic hmc = mBluetoothGatt.getService(service).getCharacteristic(controlCharacteristic);
        BluetoothGattCharacteristic sensorChar = mBluetoothGatt.getService(MiBand1_Service_UUID).getCharacteristic(Sensor);
        sensorChar.setValue(new byte[] {0x01, 0x03, 0x19});
        mBluetoothGatt.writeCharacteristic(sensorChar);


        ping = new Timer(12000, () -> { //ping every twelve seconds
            hmc.setValue(new byte[]{0x16});
            mBluetoothGatt.writeCharacteristic(hmc);
        });
        ping.start();
    }

    private void stopHrMeasure() {
        BluetoothGattCharacteristic hmc = mBluetoothGatt.getService(service).getCharacteristic(controlCharacteristic);
        hmc.setValue(new byte[] {0x15, 0x01, 0x00});
        mBluetoothGatt.writeCharacteristic(hmc);
    }

    private void startVibrate() {
        BluetoothGattCharacteristic chr = mBluetoothGatt.getService(Alert_Service_UUID).getCharacteristic(Alert_Characteristic_UUID);
        chr.setValue(new byte[] {2});
        mBluetoothGatt.writeCharacteristic(chr);
    }

    private void stopVibrate() {
        BluetoothGattCharacteristic chr = mBluetoothGatt.getService(Alert_Service_UUID).getCharacteristic(Alert_Characteristic_UUID);
        chr.setValue(new byte[] {0});
        mBluetoothGatt.writeCharacteristic(chr);
    }

    /**
     * Test playground
     */


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //bluetooth is connected so discover services
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothStatus.setText("Connected");
                    }
                });

                ContentValues cv = new ContentValues();
                cv.put(DeviceContract.DeviceEntry.COLUMN_ADDRESS, address);
                cv.put(DeviceContract.DeviceEntry.COLUMN_NAME, name);

                db1.replaceOrThrow(DeviceContract.DeviceEntry.TABLE_NAME, null, cv);

                Cursor cursor = db1.query(
                        DeviceContract.DeviceEntry.TABLE_NAME,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

                while(cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(DeviceContract.DeviceEntry.COLUMN_NAME));
                    Log.d("DB1", "Id: " + id);
                }

                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.d("Services", "Found Service " + service.getUuid().toString());
                    if (service.getUuid().equals(Battery_Service_UUID)) {
                        Log.d("Bat-Srv", String.valueOf(service.getUuid()));

                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Battery_Level_UUID);

                        if (characteristic != null) {
                            //gatt.readCharacteristic(characteristic);
                        }
                    }

                    for(BluetoothGattCharacteristic mCharacteristic: service.getCharacteristics())
                    {
                        Log.d("Services", "Found Characteristic " + mCharacteristic.getUuid().toString());
                    }
                }
                enableNotifications(gatt, mBluetoothGatt.getService(MiBand_Service_UUID).getCharacteristic(Auth_Characteristic_UUID));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (characteristic.getUuid().toString()) {
                    case "00000006-0000-3512-2118-0009af100700":
                        byte[] data = characteristic.getValue();
                        final int value = new BigInteger(String.valueOf(data[1])).intValue();

                        Log.d("Bat", "battery level: " + value + "%");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mBatteryValue.setText(value + " %");
                            }
                        });
                        startHrMeasure();
                        break;
                }


            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BluetoothGattCharacteristic chrt = gatt.getService(MiBand_Service_UUID).getCharacteristic(Auth_Characteristic_UUID);
            Log.d("BLE", "Received characteristics changed event : " + characteristic.getUuid());
            byte[] charValue = Arrays.copyOfRange(characteristic.getValue(), 0, 3);
            switch (characteristic.getUuid().toString()) {
                case "00000009-0000-3512-2118-0009af100700": {
                    switch (Arrays.toString(charValue)) {
                        case "[16, 1, 1]": {
                            Log.d("Case 1", "1");
                            chrt.setValue(new byte[]{0x02, 0x08});
                            gatt.writeCharacteristic(chrt);
                            break;
                        }
                        case "[16, 2, 1]": {
                            Log.d("Case 2", "2");
                            executeAuthorisationSequence(gatt, characteristic);
                            break;
                        }
                        case "[16, 3, 1]": {
                            Log.d("AUTH", "Success");
                            display();
                            getBattery();

                            //startVibrate();
                            break;
                        }
                        default:
                            Log.d("SAD", "Not found " + Arrays.toString(charValue));
                            break;
                    }
                    break;
                }
                case "00002a37-0000-1000-8000-00805f9b34fb":
                    byte currentHrValue = characteristic.getValue()[1];
                    if (currentHrValue != 0) {
                        final String heartRateValue = String.valueOf(currentHrValue);
                        heartRateValues.add(Integer.valueOf(currentHrValue));
                        Log.d("HR", "Received: " + heartRateValue);
                        Calendar cal = Calendar.getInstance();


                        ContentValues cv = new ContentValues();
                        cv.put(HeartRateContract.HeartRateEntry.COLUMN_VALUE, heartRateValue);
                        cv.put(HeartRateContract.HeartRateEntry.COLUMN_DAY, cal.get(Calendar.DAY_OF_MONTH));
                        cv.put(HeartRateContract.HeartRateEntry.COLUMN_MONTH, cal.get(Calendar.MONTH) + 1);
                        cv.put(HeartRateContract.HeartRateEntry.COLUMN_YEAR, cal.get(Calendar.YEAR));
                        cv.put(HeartRateContract.HeartRateEntry.COLUMN_HOUR, cal.get(Calendar.HOUR_OF_DAY));
                        cv.put(HeartRateContract.HeartRateEntry.COLUMN_MINUTE, cal.get(Calendar.MINUTE));
                        cv.put(HeartRateContract.HeartRateEntry.COLUMN_ADDRESS, address);

                        db.insert(HeartRateContract.HeartRateEntry.TABLE_NAME, null, cv);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mHrValue.setText(heartRateValue);
                            }
                        });
                    } else {
                        startVibrate();
                    }
                    break;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            switch (characteristic.getUuid().toString()) {
                case "00000001-0000-3512-2118-0009af100700": {
                    switch (Arrays.toString(characteristic.getValue())) {
                        case "[1, 3, 25]": {
                            enableNotifications(gatt, gatt.getService(service).getCharacteristic(measurementCharacteristic));
                            break;
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("DESC", "Desc write");
            BluetoothGattCharacteristic chrt = gatt.getService(MiBand_Service_UUID).getCharacteristic(Auth_Characteristic_UUID);
            switch (descriptor.getCharacteristic().getUuid().toString()) {
                case "00000009-0000-3512-2118-0009af100700":
                    String s = new String(auth_char_key, StandardCharsets.UTF_8);
                    Log.d("DESC", s + " " + Integer.toString(auth_char_key.length));
                    byte[] rq = Arrays.copyOf(new byte[]{0x01, 0x08}, 2 + auth_char_key.length);
                    System.arraycopy(auth_char_key, 0, rq, 2, auth_char_key.length);
                    chrt.setValue(rq);
                    gatt.writeCharacteristic(chrt);
                    break;

                case "00002a37-0000-1000-8000-00805f9b34fb":
                    switch (Arrays.toString(descriptor.getValue())) {
                        case "[1, 0]":
                            BluetoothGattCharacteristic hmc = gatt.getService(service).getCharacteristic(controlCharacteristic);
                            hmc.setValue(new byte[] {0x15, 0x01, 0x01});
                            Log.d("INFO", "HMC " + gatt.writeCharacteristic(hmc));
                            break;
                        default:
                            Log.d("INFO", "onDescriptorWrite UUID: " + descriptor.getUuid().toString() + " value: " + Arrays.toString(descriptor.getValue()));
                    }
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBTAdapter.cancelDiscovery();
            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            address = info.substring(info.length() - 17);
            name = info.substring(0, info.length() - 17);

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                Log.d("Bond", "Trying to pair with " + name);
                try {
                    final BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    // connect to the GATT server on the device
                    mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
                } catch (IllegalArgumentException exception) {
                    Log.w("Dev", "Device not found with provided address. Unable to connect.");
                }

            }
        }
    };

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}

class Timer {

    private int mili;
    private TimerTask task;
    private java.util.Timer timer;
    private Behavior behavior;

    public Timer(int time, Behavior b){
        mili = time;
        behavior = b;
        timer = new java.util.Timer();
    }

    public void start(){
        task = new TimerTask() {
            @Override
            public void run() {
                behavior.run();
            }
        };
        timer.scheduleAtFixedRate(task,0,mili);
    }

    public void stop(){
        timer.cancel();
        task.cancel();
    }
}

interface Behavior {
    void run();
}