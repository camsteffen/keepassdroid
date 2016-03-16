/*
 * Copyright 2009-2015 Brian Pellin.
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
package com.keepassdroid;

import android.content.Context;
import android.net.Uri;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.database.save.PwDbOutput;
import com.keepassdroid.icons.DrawableFactory;
import com.keepassdroid.search.SearchDbHelper;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author bpellin
 */
public class Database {
    public Set<PwGroup> dirty = new HashSet<>();
    public PwDatabase pm;
    public Uri mUri;
    boolean readOnly = false;

    DrawableFactory drawFactory = new DrawableFactory();

    public Database(InputStream is, String password, InputStream kfIs) throws IOException, InvalidDBException {
        this(is, password, kfIs, null);
    }

    public Database(InputStream is, String password, InputStream kfIs, MyProgressTask status) throws IOException, InvalidDBException {
        BufferedInputStream bis = new BufferedInputStream(is);

        // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
        bis.mark(10);

        Importer importer = Importer.Factory.createImporter(bis);

        bis.reset();  // Return to the start

        pm = importer.openDatabase(bis, password, kfIs, status);

        if (pm != null) {
            PwGroup root = pm.rootGroup;

            pm.populateGlobals(root);

            if (pm != null) {
                if(!pm.validatePasswordEncoding(password)) {
                    throw new RuntimeException("validatePasswordEncoding failed");
                }
            }
        }
    }

    public PwGroup Search(Context context, String str) {
        return SearchDbHelper.search(context, this, str);
    }

    public void SaveData(Context ctx) throws IOException, PwDbOutputException {
        SaveData(ctx, mUri);
    }

    private void SaveData(Context ctx, Uri uri) throws IOException, PwDbOutputException {
        if (uri.getScheme().equals("file")) {
            String filename = uri.getPath();
            File tempFile = new File(filename + ".tmp");
            FileOutputStream fos = new FileOutputStream(tempFile);
            //BufferedOutputStream bos = new BufferedOutputStream(fos);

            //PwDbV3Output pmo = new PwDbV3Output(pm, bos, App.getCalendar());
            PwDbOutput pmo = PwDbOutput.getInstance(pm, fos);
            assert pmo != null;
            pmo.output();
            //bos.flush();
            //bos.close();
            fos.close();

            // Force data to disk before continuing
            try {
                fos.getFD().sync();
            } catch (SyncFailedException e) {
                // Ignore if fsync fails. We tried.
            }

            File orig = new File(filename);

            if (!tempFile.renameTo(orig)) {
                throw new IOException("Failed to store database.");
            }
        } else {
            OutputStream os;
            try {
                os = ctx.getContentResolver().openOutputStream(uri);
            } catch (Exception e) {
                throw new IOException("Failed to store database.");
            }

            PwDbOutput pmo = PwDbOutput.getInstance(pm, os);
            assert pmo != null;
            pmo.output();
            assert os != null;
            os.close();
        }

        mUri = uri;

    }

    void markAllGroupsAsDirty() {
        for (PwGroup group : pm.getGroups()) {
            dirty.add(group);
        }

        // TODO: This should probably be abstracted out
        // The root group in v3 is not an 'official' group
        if (pm instanceof PwDatabaseV3) {
            dirty.add(pm.rootGroup);
        }
    }

}
