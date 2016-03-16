/*
 * Copyright 2012 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.timeout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.keepass.R;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.timers.Timeout;

public class TimeoutHelper {

    private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;  // 5 minutes

    public static void pause(Context context) {
        // Record timeout time in case timeout service is killed
        long time = System.currentTimeMillis();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(context.getString(R.string.timeout_key), time);

        EditorCompat.apply(edit);

        //if (App.getDB().Loaded()) { // TODO
            Timeout.start(context);
        //}
    }

    public static void resume(Context context) {
        //if (App.getDB().Loaded()) { TODO
            Timeout.cancel(context);
        //}

        // Check whether the timeout has expired
        long cur_time = System.currentTimeMillis();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long timeout_start = prefs.getLong(context.getString(R.string.timeout_key), -1);
        // The timeout never started
        if (timeout_start == -1) {
            return;
        }


        String sTimeout = prefs.getString(context.getString(R.string.app_timeout_key), context.getString(R.string.clipboard_timeout_default));
        long timeout;
        try {
            timeout = Long.parseLong(sTimeout);
        } catch (NumberFormatException e) {
            timeout = DEFAULT_TIMEOUT;
        }

        // We are set to never timeout
        if (timeout == -1) {
            return;
        }

        long diff = cur_time - timeout_start;
        if (diff >= timeout) {
            // We have timed out
            // TODO
            // App.setShutdown();
        }
    }
}
