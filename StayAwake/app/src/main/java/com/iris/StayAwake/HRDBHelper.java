package com.iris.StayAwake;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HRDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "pym.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + HeartRateContract.HeartRateEntry.TABLE_NAME + " (" +
            HeartRateContract.HeartRateEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            HeartRateContract.HeartRateEntry.COLUMN_VALUE + " TEXT NOT NULL, " +
            HeartRateContract.HeartRateEntry.COLUMN_DAY + " TEXT NOT NULL, " +
            HeartRateContract.HeartRateEntry.COLUMN_MONTH + " TEXT NOT NULL, " +
            HeartRateContract.HeartRateEntry.COLUMN_YEAR + " TEXT NOT NULL, " +
            HeartRateContract.HeartRateEntry.COLUMN_HOUR + " TEXT NOT NULL, " +
            HeartRateContract.HeartRateEntry.COLUMN_MINUTE + " TEXT NOT NULL, " +
            HeartRateContract.HeartRateEntry.COLUMN_SECOND + " TEXT NOT NULL, " +
            HeartRateContract.HeartRateEntry.COLUMN_ADDRESS + " TEXT NOT NULL, " +
            " FOREIGN KEY (" + HeartRateContract.HeartRateEntry.COLUMN_ADDRESS + ") REFERENCES " + DeviceContract.DeviceEntry.TABLE_NAME + "(" + DeviceContract.DeviceEntry.COLUMN_ADDRESS + "))";

    public HRDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + HeartRateContract.HeartRateEntry.TABLE_NAME);
        onCreate(db);
    }

}
