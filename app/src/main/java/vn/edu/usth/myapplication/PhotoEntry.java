/*
 * Copyright (c) 2025 Pham The Minh
 * All rights reserved.
 * Project: My Application
 * File: PhotoEntry.java
 * Last Modified: 26/9/2025 9:38
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
