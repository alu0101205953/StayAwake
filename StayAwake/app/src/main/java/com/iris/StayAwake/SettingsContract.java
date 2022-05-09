package com.iris.StayAwake;

import android.provider.BaseColumns;

public final class SettingsContract {

    private SettingsContract() {}

    public static class SettingsEntry implements BaseColumns {
        public static final String TABLE_NAME = "set_entry";

        public static final String COLUMN_VARIABLE = "variable";
        public static final String COLUMN_VALUE = "value";
        public static final String COLUMN_DESCRIPTION = "description";
    }
}
