package com.iris.StayAwake;

import java.util.UUID;

public class BluetoothGatt {

    public static class MiBand {
        //Services
        public static UUID MiBand_Service_UUID = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb");
        public static UUID MiBand1_Service_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
        public static UUID HR_Service_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
        public static UUID Alert_Service_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");

        //Characteristics
        public static UUID Alert_Characteristic_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
        public static UUID Auth_Characteristic_UUID = UUID.fromString("00000009-0000-3512-2118-0009af100700");
        public static UUID Battery_Level_UUID = UUID.fromString("00000006-0000-3512-2118-0009af100700");
        public static UUID Measurement_Characteristic_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
        public static UUID Sensor_Characteristic_UUID = UUID.fromString("00000001-0000-3512-2118-0009af100700");
        public static UUID Sensor_Measure_Characteristic_UUID = UUID.fromString("00000002-0000-3512-2118-0009af100700");
        public static UUID Control_Characteristic_UUID = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");

        //Descriptor
        public static UUID Descriptor_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    }

}
