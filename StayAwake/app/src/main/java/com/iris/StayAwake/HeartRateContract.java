package com.iris.StayAwake;

import android.provider.BaseColumns;

public final class HeartRateContract {

    private HeartRateContract() {}

    public static class HeartRateEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";

        public static final String COLUMN_VALUE = "value";
        public static final String COLUMN_DAY = "day";
        public static final String COLUMN_MONTH = "month";
        public static final String COLUMN_YEAR = "year";
        public static final String COLUMN_HOUR = "hour";
        public static final String COLUMN_MINUTE = "minute";
        public static final String COLUMN_SECOND = "second";
        public static final String COLUMN_ADDRESS = "address";


    }

}
