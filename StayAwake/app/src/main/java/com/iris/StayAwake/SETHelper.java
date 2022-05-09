package com.iris.StayAwake;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SETHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "settings.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SettingsContract.SettingsEntry.TABLE_NAME + " (" +
                    SettingsContract.SettingsEntry.COLUMN_VARIABLE + " TEXT NOT NULL, " +
                    SettingsContract.SettingsEntry.COLUMN_VALUE + " TEXT NOT NULL, " +
                    SettingsContract.SettingsEntry.COLUMN_DESCRIPTION + " TEXT NOT NULL " + ")";

    public SETHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + SettingsContract.SettingsEntry.TABLE_NAME);
        onCreate(db);
    }

    public void clearDatabase(SQLiteDatabase db) {
        String clearDBQuery = "DELETE FROM " + SettingsContract.SettingsEntry.TABLE_NAME;
        db.execSQL(clearDBQuery);
    }
}
