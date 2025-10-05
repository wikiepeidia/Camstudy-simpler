/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: UserDatabase.java
 * Last Modified: 5/10/2025 3:34
 */

package vn.edu.usth.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserDatabase extends SQLiteOpenHelper {
    private static final String TAG = "UserDatabase";
    private static final String DATABASE_NAME = "CamStudyUsers.db";
    private static final int DATABASE_VERSION = 2; // Increased version for database refresh

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
        try {
            // Create users table
            String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_EMAIL + " TEXT UNIQUE NOT NULL COLLATE NOCASE, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL, " +
                    COLUMN_CREATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')))";
            db.execSQL(createUsersTable);
            Log.d(TAG, "Users table created successfully");

            // Create session table
            String createSessionTable = "CREATE TABLE " + TABLE_SESSION + " (" +
                    COLUMN_SESSION_EMAIL + " TEXT PRIMARY KEY COLLATE NOCASE, " +
                    COLUMN_IS_LOGGED_IN + " INTEGER DEFAULT 0)";
            db.execSQL(createSessionTable);
            Log.d(TAG, "Session table created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating tables", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION);
            onCreate(db);
            Log.d(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database", e);
        }
    }

    // Check if email exists (case-insensitive)
    public boolean checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_USERS,
                    new String[]{COLUMN_EMAIL},
                    "LOWER(" + COLUMN_EMAIL + ") = LOWER(?)",
                    new String[]{email.trim()},
                    null, null, null);
            boolean exists = cursor.getCount() > 0;
            Log.d(TAG, "Email exists check for '" + email + "': " + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Error checking email exists", e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // Register new user
    public boolean registerUser(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            Log.e(TAG, "Invalid email or password");
            return false;
        }

        email = email.trim();

        if (checkEmailExists(email)) {
            Log.d(TAG, "Email already exists: " + email);
            return false;
        }

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_PASSWORD, password);

            long result = db.insert(TABLE_USERS, null, values);
            boolean success = result != -1;
            Log.d(TAG, "User registration for '" + email + "': " + (success ? "SUCCESS" : "FAILED"));
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error registering user", e);
            return false;
        }
    }

    // Validate login credentials (case-insensitive email)
    public boolean validateLogin(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null) {
            Log.d(TAG, "Invalid login credentials provided");
            return false;
        }

        email = email.trim();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_USERS,
                    new String[]{COLUMN_EMAIL, COLUMN_PASSWORD},
                    "LOWER(" + COLUMN_EMAIL + ") = LOWER(?) AND " + COLUMN_PASSWORD + " = ?",
                    new String[]{email, password},
                    null, null, null);
            boolean valid = cursor.getCount() > 0;
            Log.d(TAG, "Login validation for '" + email + "': " + (valid ? "SUCCESS" : "FAILED"));
            return valid;
        } catch (Exception e) {
            Log.e(TAG, "Error validating login", e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // Check if email exists but password is wrong (case-insensitive)
    public boolean isEmailRegistered(String email) {
        return checkEmailExists(email);
    }

    // Save login session
    public void saveLoginSession(String email, boolean isLoggedIn) {
        if (email == null || email.trim().isEmpty()) {
            Log.e(TAG, "Cannot save session with empty email");
            return;
        }

        email = email.trim();
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();

            // Clear previous sessions
            db.delete(TABLE_SESSION, null, null);

            // Insert new session
            ContentValues values = new ContentValues();
            values.put(COLUMN_SESSION_EMAIL, email);
            values.put(COLUMN_IS_LOGGED_IN, isLoggedIn ? 1 : 0);
            long result = db.insert(TABLE_SESSION, null, values);
            Log.d(TAG, "Session saved for '" + email + "': " + (result != -1 ? "SUCCESS" : "FAILED"));
        } catch (Exception e) {
            Log.e(TAG, "Error saving login session", e);
        }
    }

    // Get current session
    public String getLoggedInEmail() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_SESSION,
                    new String[]{COLUMN_SESSION_EMAIL},
                    COLUMN_IS_LOGGED_IN + " = ?",
                    new String[]{"1"},
                    null, null, null);

            if (cursor.moveToFirst()) {
                String email = cursor.getString(0);
                Log.d(TAG, "Logged in email: " + email);
                return email;
            }
            Log.d(TAG, "No logged in user found");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting logged in email", e);
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return getLoggedInEmail() != null;
    }

    // Logout user
    public void logout() {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            int deleted = db.delete(TABLE_SESSION, null, null);
            Log.d(TAG, "Logout: " + deleted + " session(s) cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
        }
    }

    // Get user count (for debugging)
    public int getUserCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                Log.d(TAG, "Total users in database: " + count);
                return count;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user count", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    // Clear all data (for testing)
    public void clearAllData() {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            db.delete(TABLE_USERS, null, null);
            db.delete(TABLE_SESSION, null, null);
            Log.d(TAG, "All data cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all data", e);
        }
    }
}
