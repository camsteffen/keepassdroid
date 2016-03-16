/*
 * Copyright 2013-2016 Brian Pellin.
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
package com.keepassdroid.fileselect;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.android.keepass.R;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.utils.UriUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecentFileHistory {

    private static String DB_KEY = "recent_databases";
    private static String KEYFILE_KEY = "recent_keyfiles";

    private List<String> recentDatabases = new ArrayList<>();
    private List<String> keyfiles = new ArrayList<>();
    private Context ctx;
    private SharedPreferences prefs;
    private OnSharedPreferenceChangeListener listener;
    private boolean enabled;
    private boolean init = false;

    public RecentFileHistory(Context c) {
        ctx = c.getApplicationContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(c);
        enabled = prefs.getBoolean(ctx.getString(R.string.recentfile_key), ctx.getResources().getBoolean(R.bool.recentfile_default));
        listener = new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                    String key) {
                if (key.equals(ctx.getString(R.string.recentfile_key))) {
                    enabled = sharedPreferences.getBoolean(ctx.getString(R.string.recentfile_key), ctx.getResources().getBoolean(R.bool.recentfile_default));
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    private synchronized void init() {
        if (!init) {
            if (!upgradeFromSQL()) {
                loadPrefs();
            }

            init = true;
        }
    }

    private boolean upgradeFromSQL() {

        try {
            // Check for a database to upgrade from
            if (!sqlDatabaseExists()) {
                return false;
            }

            recentDatabases.clear();
            keyfiles.clear();

            FileDbHelper helper = new FileDbHelper(ctx);
            helper.open();
            Cursor cursor = helper.fetchAllFiles();

            int dbIndex = cursor.getColumnIndex(FileDbHelper.COLUMN_FILENAME);
            int keyIndex = cursor.getColumnIndex(FileDbHelper.COLUMN_KEYFILE);

            if(cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    String filename = cursor.getString(dbIndex);
                    String keyfile = cursor.getString(keyIndex);

                    recentDatabases.add(filename);
                    keyfiles.add(keyfile);
                }
            }

            savePrefs();

            cursor.close();
            helper.close();

        } catch (Exception e) {
            // If upgrading fails, we'll just give up on it.
        }

        try {
            FileDbHelper.deleteDatabase(ctx);
        } catch (Exception e) {
            // If we fail to delete it, just move on
        }

        return true;
    }

    private boolean sqlDatabaseExists() {
        File db = ctx.getDatabasePath(FileDbHelper.DATABASE_NAME);
        return db.exists();
    }

    public void addFile(Uri uri, Uri keyUri) {
        if (!enabled || uri == null) return;

        init();

        // Remove any existing instance of the same filename
        deleteFile(uri, false);

        recentDatabases.add(0, uri.toString());

        String key = (keyUri == null) ? "" : keyUri.toString();
        keyfiles.add(0, key);

        trimLists();
        savePrefs();
    }

    public String getDatabaseAt(int i) {
        init();
        return recentDatabases.get(i);
    }

    public String getKeyfileAt(int i) {
        init();
        return keyfiles.get(i);
    }

    private void loadPrefs() {
        loadList(recentDatabases, DB_KEY);
        loadList(keyfiles, KEYFILE_KEY);
    }

    private void savePrefs() {
        saveList(DB_KEY, recentDatabases);
        saveList(KEYFILE_KEY, keyfiles);
    }

    private void loadList(List<String> list, String keyprefix) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int size = prefs.getInt(keyprefix, 0);

        list.clear();
        for (int i = 0; i < size; i++) {
            list.add(prefs.getString(keyprefix + "_" + i, ""));
        }
    }

    private void saveList(String keyprefix, List<String> list) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = prefs.edit();
        int size = list.size();
        edit.putInt(keyprefix, size);

        for (int i = 0; i < size; i++) {
            edit.putString(keyprefix + "_" + i, list.get(i));
        }
        EditorCompat.apply(edit);
    }

    public void deleteFile(Uri uri) {
        deleteFile(uri, true);
    }

    public void deleteFile(Uri uri, boolean save) {
        init();

        String uriName = uri.toString();
        String fileName = uri.getPath();

        for (int i = 0; i < recentDatabases.size(); i++) {
            String entry = recentDatabases.get(i);
            if (uriName.equals(entry) || fileName.equals(entry)) {
                recentDatabases.remove(i);
                keyfiles.remove(i);
                break;
            }
        }

        if (save) {
            savePrefs();
        }
    }

    public List<String> getRecentDBList() {
        init();
        return recentDatabases;
    }

    public Uri getFileByName(Uri database) {
        if (!enabled) return null;

        init();

        int size = recentDatabases.size();
        for (int i = 0; i < size; i++) {
            if (UriUtil.equalsDefaultfile(database, recentDatabases.get(i))) {
                return UriUtil.parseDefaultFile(keyfiles.get(i));
            }
        }

        return null;
    }

    public void deleteAll() {
        init();

        recentDatabases.clear();
        keyfiles.clear();

        savePrefs();
    }

    public void deleteAllKeys() {
        init();

        keyfiles.clear();

        int size = recentDatabases.size();
        for (int i = 0; i < size; i++) {
            keyfiles.add("");
        }

        savePrefs();
    }

    private void trimLists() {
        int size = recentDatabases.size();
        for (int i = FileDbHelper.MAX_FILES; i < size; i++) {
            recentDatabases.remove(i);
            keyfiles.remove(i);
        }
    }
}
