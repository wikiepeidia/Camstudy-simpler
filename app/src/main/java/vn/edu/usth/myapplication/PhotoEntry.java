/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: PhotoEntry.java
 * Last Modified: 1/10/2025 4:38
 */

package vn.edu.usth.myapplication;

import android.net.Uri;

public class PhotoEntry {
    private final String uriString;
    private final long dateTaken;

    public PhotoEntry(String uriString, long dateTaken) {
        this.uriString = uriString;
        this.dateTaken = dateTaken;
    }

    public Uri getUri() {
        return Uri.parse(uriString);
    }

    public String getUriString() {
        return uriString;
    }

    public long getDateTaken() {
        return dateTaken;
    }
}
