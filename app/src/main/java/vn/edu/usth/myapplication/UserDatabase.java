/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: UserDatabase.java
 * Last Modified: 5/10/2025 3:3
 */

package vn.edu.usth.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CamStudyUsers.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_CREATED_AT = "created_at";

    // Session table
    private static final String TABLE_SESSION = "user_session";
    private static final String COLUMN_SESSION_EMAIL = "email";
    private static final String COLUMN_IS_LOGGED_IN = "is_logged_in";

    public UserDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                COLUMN_PASSWORD + " TEXT NOT NULL, " +
                COLUMN_CREATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')))";
        db.execSQL(createUsersTable);

        // Create session table
        String createSessionTable = "CREATE TABLE " + TABLE_SESSION + " (" +
                COLUMN_SESSION_EMAIL + " TEXT PRIMARY KEY, " +
                COLUMN_IS_LOGGED_IN + " INTEGER DEFAULT 0)";
        db.execSQL(createSessionTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION);
        onCreate(db);
    }

    // Check if email exists
    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_EMAIL},
                COLUMN_EMAIL + " = ?",
                new String[]{email},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Register new user
    public boolean registerUser(String email, String password) {
        if (checkEmailExists(email)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Validate login credentials
    public boolean validateLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_EMAIL},
                COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ?",
                new String[]{email, password},
                null, null, null);
        boolean valid = cursor.getCount() > 0;
        cursor.close();
        return valid;
    }

    // Check if email exists but password is wrong
    public boolean isEmailRegistered(String email) {
        return checkEmailExists(email);
    }

    // Save login session
    public void saveLoginSession(String email, boolean isLoggedIn) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Clear previous sessions
        db.delete(TABLE_SESSION, null, null);

        // Insert new session
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_EMAIL, email);
        values.put(COLUMN_IS_LOGGED_IN, isLoggedIn ? 1 : 0);
        db.insert(TABLE_SESSION, null, values);
    }

    // Get current session
    public String getLoggedInEmail() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SESSION,
                new String[]{COLUMN_SESSION_EMAIL},
                COLUMN_IS_LOGGED_IN + " = ?",
                new String[]{"1"},
                null, null, null);

        String email = null;
        if (cursor.moveToFirst()) {
            email = cursor.getString(0);
        }
        cursor.close();
        return email;
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return getLoggedInEmail() != null;
    }

    // Logout user
    public void logout() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SESSION, null, null);
    }

    // Clear all data (for testing)
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USERS, null, null);
        db.delete(TABLE_SESSION, null, null);
    }
}

