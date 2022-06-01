package com.iris.StayAwake;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names

    // GUI Components
    private TextView mBluetoothStatus, mHeader, mHrValue, mBatteryValue;
    private EditText mAuthInput;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private Button mDisconnectBtn;
    private Button mSubmitBtn;
    private ListView mDevicesListView;

    private LinearLayout mLayout1, mLayout2, mLayout3, mLayout4, mLayout5, mLayout6, mLayout7, mLayout8, mLayout9, mLayout10;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private BluetoothGatt mBluetoothGatt;
    private ArrayList<Integer> heartRateValues;
    private ArrayList<Integer> currentValues;
    private ArrayList<Integer> means;
    private Timer ping;
    private HRDBHelper dbHelper;
    private DEVHelper devHelper;
    private SETHelper setHelper;
    private SQLiteDatabase db;
    private SQLiteDatabase db1;
    private SQLiteDatabase db2;
    private SQLiteDatabase db3;
    private Handler mHandler;

    byte[] mi_band_3_key = new byte[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45};
    byte[] secret_key;
    byte[] mi_band_key;

    String name;
    String address;
    int ignored = 0, stored = 0, group = 0, zeroCounter = 0;
    //This should be placed on a settings database
    int maxIgnored = 15;
    boolean firstPeriod = true;
    double delay = 0;
    int referenceMean = 0;
    int periodMean = 0;
    static final long ONE_MINUTE_IN_MILLIS = 60000;//milliseconds
    Calendar referenceDate;
    long t;
    int lastX = 0;

    private void displayNoAuth() {
        mHandler.post(() -> {
            mLayout1.setVisibility(View.GONE);
            mLayout2.setVisibility(View.GONE);
            mLayout3.setVisibility(View.GONE);
            mDevicesListView.setVisibility(View.GONE);
            mBTArrayAdapter.clear();

            zeroCounter = 0;
            mLayout4.setVisibility(View.VISIBLE);
            mLayout5.setVisibility(View.VISIBLE);
            mLayout6.setVisibility(View.VISIBLE);
            mLayout7.setVisibility(View.VISIBLE);
            mLayout8.setVisibility(View.VISIBLE);
            mHrValue.setVisibility(View.VISIBLE);
            mHeader.setText(name);
        });
    }

    private void displayAfterAuth() {
        mHandler.post(() -> {
            mLayout9.setVisibility(View.GONE);
            mLayout10.setVisibility(View.GONE);

            ignored = 0; stored = 0; group = 0; zeroCounter = 0;
            mLayout4.setVisibility(View.VISIBLE);
            mLayout5.setVisibility(View.VISIBLE);
            mLayout6.setVisibility(View.VISIBLE);
            mLayout7.setVisibility(View.VISIBLE);
            mLayout8.setVisibility(View.VISIBLE);
            mHrValue.setVisibility(View.VISIBLE);
            mHeader.setText(name);
        });
    }

    private void authRequest() {
        mHandler.post(() -> {
            mLayout1.setVisibility(View.GONE);
            mLayout2.setVisibility(View.GONE);
            mLayout3.setVisibility(View.GONE);
            mDevicesListView.setVisibility(View.GONE);
            mBTArrayAdapter.clear();

            mLayout9.setVisibility(View.VISIBLE);
            mLayout10.setVisibility(View.VISIBLE);
        });
    }

    private void back() {
        mHandler.post(() -> {
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
        });
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
        devHelper.close();
        setHelper.close();
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
        mSubmitBtn = (Button)findViewById(R.id.submit);

        mLayout1 = (LinearLayout)findViewById(R.id.Layout1);
        mLayout2 = (LinearLayout)findViewById(R.id.Layout2);
        mLayout3 = (LinearLayout)findViewById(R.id.Layout3);
        mLayout4 = (LinearLayout)findViewById(R.id.Layout4);
        mLayout5 = (LinearLayout)findViewById(R.id.Layout5);
        mLayout6 = (LinearLayout)findViewById(R.id.Layout6);
        mLayout7 = (LinearLayout)findViewById(R.id.Layout7);
        mLayout8 = (LinearLayout)findViewById(R.id.Layout8);
        mLayout9 = (LinearLayout)findViewById(R.id.Layout9);
        mLayout10 = (LinearLayout)findViewById(R.id.Layout10);
        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView)findViewById(R.id.devices_list_view);
        assert mDevicesListView != null;
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);
        dbHelper = new HRDBHelper(this);
        db = dbHelper.getWritableDatabase();
        devHelper = new DEVHelper(this);
        db1 = devHelper.getWritableDatabase();
        setHelper = new SETHelper(this);
        db2 = setHelper.getWritableDatabase();
        db3 = setHelper.getReadableDatabase();

        heartRateValues = new ArrayList<>();
        currentValues = new ArrayList<>();
        means = new ArrayList<>();

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

            mScanBtn.setOnClickListener(v -> bluetoothOn());

            mOffBtn.setOnClickListener(v -> bluetoothOff());

            mListPairedDevicesBtn.setOnClickListener(v -> listPairedDevices());

            mDiscoverBtn.setOnClickListener(v -> discover());

            mSubmitBtn.setOnClickListener(v -> {
                mAuthInput = (EditText) findViewById(R.id.input);
                assert mAuthInput != null;
                String textReceived = mAuthInput.getText().toString();
                mi_band_key = hexStringToByteArray(textReceived);
                enableNotifications(mBluetoothGatt, mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Auth_Characteristic_UUID));
            });

            mDisconnectBtn.setOnClickListener(v -> {
                mBluetoothGatt.disconnect();
                mHandler.post(() -> mBluetoothStatus.setText("Disconnected"));
                ping.stop();
                stopVibrate();
                stopHrMeasure();

                /* dbHelper.clearDatabase(db);
                devHelper.clearDatabase(db1);
                setHelper.clearDatabase(db2); */


                try {
                    BackupDatabase();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                close();
                back();
            });

            Cursor mCursor = db3.rawQuery("SELECT * FROM " + SettingsContract.SettingsEntry.TABLE_NAME, null);
            if(mCursor != null && mCursor.getCount() > 0) {
                /*maxIgnored = Integer.parseInt(mCursor.getString(mCursor.getColumnIndexOrThrow((SettingsContract.SettingsEntry.COLUMN_VALUE))));
                mCursor.moveToNext();
                maxGroups = Integer.parseInt(mCursor.getString(mCursor.getColumnIndexOrThrow((SettingsContract.SettingsEntry.COLUMN_VALUE))));
                mCursor.moveToNext();
                maxStored = Integer.parseInt(mCursor.getString(mCursor.getColumnIndexOrThrow((SettingsContract.SettingsEntry.COLUMN_VALUE))));*/
            } else {
                maxIgnored = 15;

                ContentValues cv = new ContentValues();
                cv.put(SettingsContract.SettingsEntry.COLUMN_VARIABLE, "maxIgnored");
                cv.put(SettingsContract.SettingsEntry.COLUMN_VALUE, maxIgnored);
                cv.put(SettingsContract.SettingsEntry.COLUMN_DESCRIPTION, "Number of measurements ignored in order to let the sensor stabilize");

                db2.insertOrThrow(SettingsContract.SettingsEntry.TABLE_NAME, null, cv);
            }
            assert mCursor != null;
            mCursor.close();
        }
    }

    private void BackupDatabase() throws IOException {
        File src = new File(getString(R.string.database));
        File dst = new File(Environment.getExternalStorageDirectory() + File.separator + "DB_Backup" + File.separator);
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String outFileName = "hr_backup" + timeStamp + ".db";

        if (!dst.exists()) {
            if (!dst.mkdir()) {
                Toast.makeText(getApplicationContext(),"Can't create directory",Toast.LENGTH_SHORT).show();
            }
        }

        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst.getPath() + File.separator + outFileName);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
        inChannel.close();
        outChannel.close();

        Toast.makeText(getApplicationContext(),"Backup file created", Toast.LENGTH_SHORT).show();
    }

    private void bluetoothOn() {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
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

    private void bluetoothOff() {
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your location seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void discover() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else {
            if (mBTAdapter.isEnabled() && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mHandler.post(() -> mBluetoothStatus.setText("Location off, can't discover"));
            } else if (mBTAdapter.isEnabled() && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                mHandler.post(() -> mBluetoothStatus.setText("Discovering..."));
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else {
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
                    //mBTArrayAdapter.add(device.getAddress());
                } else {
                    if (device.getName().contains("Band"))
                        mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices() {
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

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //bluetooth is connected so discover services
                mHandler.post(() -> mBluetoothStatus.setText("Authenticating..."));

                ContentValues cv = new ContentValues();
                cv.put(DeviceContract.DeviceEntry.COLUMN_ADDRESS, address);
                cv.put(DeviceContract.DeviceEntry.COLUMN_NAME, name);

                db1.replaceOrThrow(DeviceContract.DeviceEntry.TABLE_NAME, null, cv);
                //Request permissions to write on external storage
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.d("Services", "Found Service " + service.getUuid().toString());

                    for (BluetoothGattCharacteristic mCharacteristic : service.getCharacteristics()) {
                        Log.d("Services", "Found Characteristic " + mCharacteristic.getUuid().toString());
                    }
                }

                if (name.contains("Mi Band 3") || name.contains("Mi Band 2")) {
                    enableNotifications(gatt, gatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Auth_Characteristic_UUID));
                } else if (name.contains("Mi Smart Band 5") || name.contains("Mi Band 4")) {
                    authRequest();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (characteristic.getUuid().toString()) {
                    case "00000006-0000-3512-2118-0009af100700":
                        byte[] data = characteristic.getValue();
                        final int value = new BigInteger(String.valueOf(data[1])).intValue();

                        mHandler.post(() -> mBatteryValue.setText(value + " %"));
                        startHrMeasure();
                        //startPositionMeasure();
                        break;
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BluetoothGattCharacteristic ch = gatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Auth_Characteristic_UUID);
            Log.d("BLE", "Received characteristics changed event : " + characteristic.getUuid());
            byte[] charValue = Arrays.copyOfRange(characteristic.getValue(), 0, 3);
            switch (characteristic.getUuid().toString()) {
                case "00002a37-0000-1000-8000-00805f9b34fb":
                    byte currentHrValue = characteristic.getValue()[1];
                    HRCallback(currentHrValue);
                    break;
                case "00000009-0000-3512-2118-0009af100700": {
                    switch (Arrays.toString(charValue)) {
                        case "[16, 1, 1]": {
                            Log.d("Case 1", "1");
                            ch.setValue(new byte[]{0x02, 0x08});
                            gatt.writeCharacteristic(ch);
                            break;
                        }
                        case "[16, 2, 1]": {
                            Log.d("Case 2", "2");
                            executeAuthorisationSequence(gatt, characteristic);
                            break;
                        }
                        case "[16, 3, 1]": {
                            mHandler.post(() -> mBluetoothStatus.setText("Connected"));
                            if (name.contains("Mi Band 3") || name.contains("Mi Band 2")) {
                                displayNoAuth();
                            } else if (name.contains("Mi Smart Band 5") || name.contains("Mi Band 4")) {
                                displayAfterAuth();
                            }
                            getBattery();
                            break;
                        }
                        default:
                            mHandler.post(() -> mBluetoothStatus.setText("Authentication failed"));
                            Log.d("SAD", "Not found " + Arrays.toString(charValue));
                            break;
                    }
                    break;
                }
                /*case "00000002-0000-3512-2118-0009af100700":
                    System.out.println(Arrays.toString(characteristic.getValue()));
                    if(characteristic.getValue()[0] == (byte) 0x01) {

                    }
                    break;*/
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BluetoothGattCharacteristic sensorChar = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand1_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Sensor_Characteristic_UUID);
            BluetoothGattCharacteristic sensorMeasureChar = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand1_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Sensor_Measure_Characteristic_UUID);

            super.onCharacteristicWrite(gatt, characteristic, status);
            if ("00000001-0000-3512-2118-0009af100700".equals(characteristic.getUuid().toString())) {
                switch (Arrays.toString(characteristic.getValue())) {
                    case "[1, 3, 25]": {
                        enableNotifications(gatt, gatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.HR_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Measurement_Characteristic_UUID));
                        break;
                    }
                    /* case "[1, 1, 25]":
                        sensorChar.setValue(new byte[]{0x02});
                        if (mBluetoothGatt.writeCharacteristic(sensorChar))

                            break;
                    case "[2]":
                        enableNotifications(mBluetoothGatt, sensorMeasureChar);
                        ping = new Timer(60000, () -> { //ping every sixty seconds
                            sensorChar.setValue(new byte[]{0x01, 0x01, 0x19});
                            mBluetoothGatt.writeCharacteristic(sensorChar);

                            sensorChar.setValue(new byte[]{0x02});
                            mBluetoothGatt.writeCharacteristic(sensorChar);
                        });
                        ping.start();
                        break; */

                    default:
                        Log.d("TAG", Arrays.toString(characteristic.getValue()));
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("DESC", "Desc write");
            BluetoothGattCharacteristic ch = gatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Auth_Characteristic_UUID);
            switch (descriptor.getCharacteristic().getUuid().toString()) {
                case "00000009-0000-3512-2118-0009af100700":
                    if (name.contains("Mi Band 3") || name.contains("Mi Band 2")) {
                        byte[] rq = Arrays.copyOf(new byte[]{0x01, 0x08}, 2 + mi_band_3_key.length);
                        System.arraycopy(mi_band_3_key, 0, rq, 2, mi_band_3_key.length);
                        ch.setValue(rq);
                        gatt.writeCharacteristic(ch);
                    } else if (name.contains("Mi Smart Band 5") || name.contains("Mi Band 4") || name.contains("Mi Band 6")) {
                        ch.setValue(new byte[]{0x02, 0x00});
                        gatt.writeCharacteristic(ch);
                    }
                    break;

                case "00002a37-0000-1000-8000-00805f9b34fb":
                    if ("[1, 0]".equals(Arrays.toString(descriptor.getValue()))) {
                        BluetoothGattCharacteristic hmc = gatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.HR_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Control_Characteristic_UUID);
                        hmc.setValue(new byte[]{0x15, 0x01, 0x01});
                        Log.d("INFO", "HMC " + gatt.writeCharacteristic(hmc));
                        Toast.makeText(getBaseContext(), "Reading...", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("INFO", "onDescriptorWrite UUID: " + descriptor.getUuid().toString() + " value: " + Arrays.toString(descriptor.getValue()));
                    }
            }
        }
    };

    private final AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            } else {
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
        }
    };

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        unregisterReceiver(blReceiver);
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Test sandbox
     */

    private void getBattery() {
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand1_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Battery_Level_UUID);
        if (characteristic != null) {
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
        gatt.setCharacteristicNotification(ch, true);
        BluetoothGattDescriptor descriptor = ch.getDescriptor(com.iris.StayAwake.BluetoothGatt.MiBand.Descriptor_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    private void executeAuthorisationSequence(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value[0] == 16 && value[1] == 2 && value[2] == 1) {
            try {
                if (name.contains("Mi Band 2") || name.contains("Mi Band 3")) {
                    secret_key = Arrays.copyOfRange(value, 3, 19);
                    @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
                    String CIPHER_NAME = "AES";
                    SecretKeySpec key = new SecretKeySpec(mi_band_3_key, CIPHER_NAME);
                    cipher.init(Cipher.ENCRYPT_MODE, key);
                    byte[] bytes = cipher.doFinal(secret_key);
                    byte[] rq = Arrays.copyOf(new byte[]{0x03, 0x08}, 2 + bytes.length);
                    System.arraycopy(bytes, 0, rq, 2, bytes.length);

                    characteristic.setValue(rq);
                    gatt.writeCharacteristic(characteristic);
                } else if (name.contains("Mi Band 4") || name.contains("Mi Smart Band 5")) {
                    byte[] mValue = Arrays.copyOfRange(value, 3, 19);
                    @SuppressLint("GetInstance") Cipher eCipher = Cipher.getInstance("AES/ECB/NoPadding");
                    SecretKeySpec newKey = new SecretKeySpec(mi_band_key, "AES");
                    eCipher.init(Cipher.ENCRYPT_MODE, newKey);
                    byte[] bytes = eCipher.doFinal(mValue);
                    byte[] rq = Arrays.copyOf(new byte[]{0x03, 0x00}, 2 + bytes.length);
                    System.arraycopy(bytes, 0, rq, 2, bytes.length);

                    characteristic.setValue(rq);
                    gatt.writeCharacteristic(characteristic);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startHrMeasure() {
        ignored = 0;
        referenceDate = Calendar.getInstance();
        t = referenceDate.getTimeInMillis();
        delay = 30;
        BluetoothGattCharacteristic hmc = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.HR_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Control_Characteristic_UUID);
        BluetoothGattCharacteristic sensorChar = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand1_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Sensor_Characteristic_UUID);
        sensorChar.setValue(new byte[] {0x01, 0x03, 0x19});
        mBluetoothGatt.writeCharacteristic(sensorChar);

        ping = new Timer(12000, () -> { //ping every twelve seconds
            hmc.setValue(new byte[] {0x16});
            mBluetoothGatt.writeCharacteristic(hmc);
        });
        ping.start();
    }

    /* private void startPositionMeasure() {
        BluetoothGattCharacteristic sensorChar = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand1_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Sensor_Characteristic_UUID);

        sensorChar.setValue(new byte[] {0x01, 0x01, 0x19});
        if(mBluetoothGatt.writeCharacteristic(sensorChar)) {

        } else {
            Log.d("TAG", "Failed to write on step 1");
        }
    } */

    private void stopPositionMeasure() {
        BluetoothGattCharacteristic sensorChar = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.MiBand1_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Sensor_Characteristic_UUID);
        sensorChar.setValue(new byte[] {0x03});
        mBluetoothGatt.writeCharacteristic(sensorChar);
    }

    private void stopHrMeasure() {
        BluetoothGattCharacteristic hmc = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.HR_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Control_Characteristic_UUID);
        hmc.setValue(new byte[] {0x15, 0x01, 0x00});
        mBluetoothGatt.writeCharacteristic(hmc);
    }

    private void startVibrate() {
        BluetoothGattCharacteristic chr = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.Alert_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Alert_Characteristic_UUID);
        if (name.contains("Mi Band 3") || name.contains("Mi Band 2")) {
            chr.setValue(new byte[] {0x02, 0x01});
        } else if (name.contains("Mi Smart Band 5") || name.contains("Mi Band 4")) {
            chr.setValue(new byte[] {0x03, 0x01});
        }
        mBluetoothGatt.writeCharacteristic(chr);
    }

    private void stopVibrate() {
        BluetoothGattCharacteristic chr = mBluetoothGatt.getService(com.iris.StayAwake.BluetoothGatt.MiBand.Alert_Service_UUID).getCharacteristic(com.iris.StayAwake.BluetoothGatt.MiBand.Alert_Characteristic_UUID);
        chr.setValue(new byte[] {0x00});
        mBluetoothGatt.writeCharacteristic(chr);
    }

    private void saveData(String value, Calendar cal) {
        ContentValues cv = new ContentValues();
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_VALUE, value);
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_DAY, cal.get(Calendar.DAY_OF_MONTH));
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_MONTH, cal.get(Calendar.MONTH) + 1);
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_YEAR, cal.get(Calendar.YEAR));
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_HOUR, cal.get(Calendar.HOUR_OF_DAY));
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_MINUTE, cal.get(Calendar.MINUTE));
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_SECOND, cal.get(Calendar.SECOND));
        cv.put(HeartRateContract.HeartRateEntry.COLUMN_ADDRESS, address);

        db.insert(HeartRateContract.HeartRateEntry.TABLE_NAME, null, cv);
    }

    private void HRCallback (byte currentHrValue) {
        Calendar measureTime = Calendar.getInstance();
        if (currentHrValue > 35) { //Check if sensor is working properly
            final String heartRateValue = String.valueOf(currentHrValue);
            Log.d("HR", "Received: " + heartRateValue);

            // Ignore first measurements in order to stabilize sensor
            if (ignored < maxIgnored) {
                ignored++;
                mHandler.post(() -> mHrValue.setText(heartRateValue));
                Log.d("CALLBACK", "Ignored");
            } else if (firstPeriod) {
                if ((measureTime.getTimeInMillis() - t) < (delay * ONE_MINUTE_IN_MILLIS)) {
                    heartRateValues.add((int) currentHrValue);
                    saveData(heartRateValue, measureTime);
                    mHandler.post(() -> mHrValue.setText(heartRateValue));
                } else {
                    referenceMean = meanOfGroup();
                    double std = standardDeviation(referenceMean);
                    if (std < 10) {
                        firstPeriod = false;
                        delay = 10;
                        heartRateValues.clear();
                        referenceDate = Calendar.getInstance();
                        t = referenceDate.getTimeInMillis();
                    } else {
                        delay += 5;
                    }
                }
            } else if (!firstPeriod) {
                if ((measureTime.getTimeInMillis() - t) < (delay * ONE_MINUTE_IN_MILLIS)) {
                    heartRateValues.add((int) currentHrValue);
                    saveData(heartRateValue, measureTime);
                    mHandler.post(() -> mHrValue.setText(heartRateValue));
                } else {
                    periodMean = meanOfGroup();
                    Log.d("CALLBACK", "New Period");
                    double std = standardDeviation(periodMean);
                    if (std < 10) {
                        double diffPercentage = meanDifference(referenceMean, periodMean);
                        Pair<Double, Double> trend = calculateTrend();
                        Log.d("CALLBACK", "Calculated trend: " + trend);
                        if ((trend.first < 0) && (diffPercentage < -6)) {
                            startVibrate();
                            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 200);
                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 3000);
                        }
                        heartRateValues.clear();
                        referenceDate = Calendar.getInstance();
                        t = referenceDate.getTimeInMillis();
                        delay = 10;
                    } else {
                        delay += 5;
                    }
                }
            }

            /*else if (stored <= maxStored){ //Learning process
                heartRateValues.add((int) currentHrValue);
                stored++;
                Log.d("CALLBACK", "Stored");
                Calendar cal = Calendar.getInstance();
                saveData(heartRateValue, cal);

                mHandler.post(() -> mHrValue.setText(heartRateValue));
            } else if (group < maxGroups){
                stored = 0;
                means.add(meanOfGroup());
                group++;
            } else if (group == maxGroups) {
                stored = 0;
                group = 0;
                Pair<Double, Double> trend = calculateTrend();
                Log.d("CALLBACK", "Calculated trend: " + trend);
                // if (trend.first < 0) startVibrate(); // for example??? Have to adjust it after testing
            }*/

        } else {
            if (zeroCounter > 4) {
                wristAlert();
                zeroCounter = 0;
            } else {
                zeroCounter++;
            }
        }
    }

    private void wristAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Band");
        builder.setMessage("Please, make sure you have your band correctly adjusted on your wrist.");
        builder.setPositiveButton("Ok", null);

        MainActivity.this.runOnUiThread(() -> {
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private double meanDifference(int referenceMean, int periodMean) {
        //noinspection IntegerDivisionInFloatingPointContext
        return (periodMean * 100 / referenceMean) - 100;
    }
    private int meanOfGroup() {
        //Remove the maximum and minimum 25% of the values to prevent anomalies
        /*for (int i = 0; i < ((25 * maxStored) / 100); i++) {
            heartRateValues.remove(Collections.max(heartRateValues));
        }
        for (int i = 0; i < ((25 * maxStored) / 100); i++) {
            heartRateValues.remove(Collections.min(heartRateValues));
        }*/

        int acc = 0;
        for (int x: heartRateValues) acc += x;
        int mean = acc / heartRateValues.size();
        // heartRateValues.clear();
        Log.d("CALLBACK", "Calculated mean: " + mean);
        return mean;
    }

    private double standardDeviation(int mean) {
        double standardDeviation = 0.0;
        for (int i = 0; i < heartRateValues.size(); i++) {
            standardDeviation += Math.pow(heartRateValues.get(i) - mean, 2);
        }
        return Math.sqrt(standardDeviation / heartRateValues.size());
    }

    private Pair<Double, Double> calculateTrend() { //Using the two means method
        //Split observations into two groups
        ArrayList<Integer> firstGroup = new ArrayList<>(heartRateValues.subList(0, (heartRateValues.size()) / 2));
        ArrayList<Integer> secondGroup = new ArrayList<>(heartRateValues.subList(heartRateValues.size() / 2, heartRateValues.size()));

        Log.d("TREND", String.valueOf(firstGroup));
        Log.d("TREND", String.valueOf(secondGroup));

        int t1, t2, y1, y2;
        int acc = 0;
        int aux = 1;

        for (int i = 0; i < firstGroup.size(); i++) {
            acc += aux;
            aux++;
        }
        t1 = acc / firstGroup.size();

        Log.d("TREND", String.valueOf(t1));

        acc = 0;
        for (int x: firstGroup) acc += x;
        y1 = acc / firstGroup.size();
        Log.d("TREND", String.valueOf(y1));

        acc = 0;
        for (int i = 0; i < secondGroup.size(); i++) {
            acc += aux;
            aux++;
        }
        t2 = acc / secondGroup.size();
        Log.d("TREND", String.valueOf(t2));

        acc = 0;
        for (int x: secondGroup) acc += x;
        y2 = acc / secondGroup.size();
        Log.d("TREND", String.valueOf(y2));

        double m = (double)(y2 - y1) / (t2 - t1);
        double n = (m * t1) + y1;

        return new Pair<>(m, n);
    }

    /*
      Test sandbox
     */

}