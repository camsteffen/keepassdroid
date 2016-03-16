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
package com.keepassdroid.fileselect;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.keepassdroid.compat.EditorCompat;

import java.io.File;
import java.io.FileFilter;

public class FileDbHelper {

    public static final String LAST_FILENAME = "lastFile";
    public static final String LAST_KEYFILE = "lastKey";

    public static final String DATABASE_NAME = "keepassdroid";
    private static final String FILE_TABLE = "files";
    private static final int DATABASE_VERSION = 1;

    public static final int MAX_FILES = 5;

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_FILENAME = "fileName";
    public static final String COLUMN_KEYFILE = "keyFile";
    public static final String COLUMN_UPDATED = "updated";

    private static final String DATABASE_CREATE =
            "create table " + FILE_TABLE + " ( " + COLUMN_ID + " integer primary key autoincrement, "
                    + COLUMN_FILENAME + " text not null, "
                    + COLUMN_KEYFILE + " text, "
                    + COLUMN_UPDATED + " integer not null);";

    private final Context mCtx;
    private SQLiteDatabase mDb;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final Context mCtx;

        DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
            mCtx = ctx;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);

            // Migrate preference to database if it is set.
            SharedPreferences settings = mCtx.getSharedPreferences("OpenDB", Context.MODE_PRIVATE);
            String lastFile = settings.getString(LAST_FILENAME, "");
            String lastKey = settings.getString(LAST_KEYFILE, "");

            if (lastFile.length() > 0) {
                ContentValues vals = new ContentValues();
                vals.put(COLUMN_FILENAME, lastFile);
                vals.put(COLUMN_UPDATED, System.currentTimeMillis());

                if (lastKey.length() > 0) {
                    vals.put(COLUMN_KEYFILE, lastKey);
                }

                db.insert(FILE_TABLE, null, vals);

                // Clear old preferences
                deletePrefs(settings);

            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Only one database version so far
        }

        private void deletePrefs(SharedPreferences prefs) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(LAST_FILENAME);
            editor.remove(LAST_KEYFILE);
            EditorCompat.apply(editor);
        }
    }

    public FileDbHelper(Context ctx) {
        mCtx = ctx;
    }

    public FileDbHelper open() throws SQLException {
        DatabaseHelper mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public boolean isOpen() {
        return mDb.isOpen();
    }

    public void close() {
        mDb.close();
    }


    public Cursor fetchAllFiles() {
        return mDb.query(
                FILE_TABLE,
                new String[] {
                        COLUMN_ID,
                        COLUMN_FILENAME,
                        COLUMN_KEYFILE
                },
                null,
                null,
                null,
                null,
                COLUMN_UPDATED + " DESC",
                Integer.toString(MAX_FILES));
    }

    /**
     * Deletes a database including its journal file and other auxiliary files
     * that may have been created by the database engine.
     *
     * @return True if the database was successfully deleted.
     */
    public static boolean deleteDatabase(Context ctx) {
        File file = ctx.getDatabasePath(DATABASE_NAME);
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }

        boolean deleted;
        deleted = file.delete();
        deleted |= new File(file.getPath() + "-journal").delete();
        deleted |= new File(file.getPath() + "-shm").delete();
        deleted |= new File(file.getPath() + "-wal").delete();

        File dir = file.getParentFile();
        if (dir != null) {
            final String prefix = file.getName() + "-mj";
            final FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(File candidate) {
                    return candidate.getName().startsWith(prefix);
                }
            };
            for (File masterJournal : dir.listFiles(filter)) {
                deleted |= masterJournal.delete();
            }
        }
        return deleted;
    }
}
