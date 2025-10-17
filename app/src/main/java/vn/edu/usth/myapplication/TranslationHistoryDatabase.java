/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: TranslationHistoryDatabase.java
 * Last Modified: 17/10/2025 0:56
 */

package vn.edu.usth.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TranslationHistoryDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "translation_history.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "translations";

    private static final String COL_ID = "id";
    private static final String COL_SOURCE = "source_text";
    private static final String COL_TRANSLATED = "translated_text";
    private static final String COL_TIME = "created_at";

    public TranslationHistoryDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SOURCE + " TEXT, " +
                COL_TRANSLATED + " TEXT, " +
                COL_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP)"
                ;
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    public void addTranslation(String source, String translated) {
        SQLiteDatabase db = this.getWritableDatabase();


        ContentValues values = new ContentValues();
        values.put(COL_SOURCE, source);
        values.put(COL_TRANSLATED, translated);
        db.insert(TABLE_NAME, null, values);


        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            if (count > 20) {
                db.execSQL("DELETE FROM " + TABLE_NAME +
                        " WHERE " + COL_ID + " IN (SELECT " + COL_ID +
                        " FROM " + TABLE_NAME + " ORDER BY " + COL_TIME + " ASC LIMIT " + (count - 20) + ")");
            }
        }
        cursor.close();
        db.close();
    }

    public List<String[]> getAllTranslations() {
        List<String[]> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COL_SOURCE + ", " + COL_TRANSLATED +
                " FROM " + TABLE_NAME + " ORDER BY " + COL_TIME + " DESC", null);
        while (c.moveToNext()) {
            list.add(new String[]{c.getString(0), c.getString(1)});
        }
        c.close();
        db.close();
        return list;
    }
}