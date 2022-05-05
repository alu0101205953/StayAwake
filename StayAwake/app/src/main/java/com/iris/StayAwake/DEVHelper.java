package com.iris.StayAwake;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DEVHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "dev.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DeviceContract.DeviceEntry.TABLE_NAME + " (" +
                    DeviceContract.DeviceEntry.COLUMN_CODE + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DeviceContract.DeviceEntry.COLUMN_NAME + " TEXT NOT NULL)";

    public DEVHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DeviceContract.DeviceEntry.TABLE_NAME);
        onCreate(db);
    }

}
