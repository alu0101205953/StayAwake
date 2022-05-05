package com.iris.StayAwake;

import android.provider.BaseColumns;

public final class DeviceContract {

    private DeviceContract() {}

    public static class DeviceEntry implements BaseColumns {
        public static final String TABLE_NAME = "dev_entry";

        public static final String COLUMN_CODE = "code";
        public static final String COLUMN_NAME = "name";
    }

}