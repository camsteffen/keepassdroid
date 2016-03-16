/*
 * Copyright 2009-2013 Brian Pellin.
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
package com.keepassdroid.app;

import android.app.Application;
import com.keepassdroid.compat.PRNGFixes;
import com.keepassdroid.fileselect.RecentFileHistory;

import java.util.Calendar;

public class App extends Application {

    public static final int EXIT_NORMAL = 0;
    public static final int EXIT_LOCK = 1;
    public static final int EXIT_REFRESH = 2;
    public static final int EXIT_REFRESH_TITLE = 3;

    private static Calendar calendar = null;
    private static RecentFileHistory fileHistory;

    public static RecentFileHistory getFileHistory() {
        return fileHistory;
    }

    public static Calendar getCalendar() {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }

        return calendar;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fileHistory = new RecentFileHistory(this);

        PRNGFixes.apply();
    }
}
