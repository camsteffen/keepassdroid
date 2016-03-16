package com.keepassdroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.widget.Toast;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.exception.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;


class OpenDatabaseTask extends MyProgressTask<Void, Database> {

    WeakReference<MainActivity> activity;
    private final Uri dbUri;
    private final String password;
    private final Uri keyFileUri;
    private InputStream dbInputStream;
    private InputStream keyFileInputStream;
    private String errorMessage;
    public Exception exception;

    OpenDatabaseTask(MainActivity activity, Uri dbUri, String password, Uri keyFileUri) {
        super(activity, R.string.loading_database);
        this.activity = new WeakReference<>(activity);
        this.dbUri = dbUri;
        this.password = password;
        this.keyFileUri = keyFileUri;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Activity activity = this.activity.get();

        ContentResolver contentResolver = activity.getContentResolver();
        try {
            dbInputStream = contentResolver.openInputStream(dbUri);
        } catch (FileNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.database_file_not_found), Toast.LENGTH_LONG).show();
        }
        if (keyFileUri != null) {
            try {
                keyFileInputStream = contentResolver.openInputStream(keyFileUri);
            } catch (FileNotFoundException e) {
                Toast.makeText(activity, activity.getString(R.string.key_file_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected Database doInBackground(Void... params) {
        try {
            return new Database(dbInputStream, password, keyFileInputStream, this);
        } catch (Exception e) {
            exception = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(Database database) {
        super.onPostExecute(database);

        MainActivity activity = this.activity.get();
        if (activity == null)
            return;

        try {
            if (exception != null) {
                throw exception;
            }

            activity.db = database;
            App.getFileHistory().addFile(dbUri, keyFileUri);
            activity.openItem(database.pm.rootGroup);
            return;

        } catch (ArcFourException e) {
            errorMessage = activity.getString(R.string.error_arc4);
        } catch (InvalidPasswordException e) {
            errorMessage = activity.getString(R.string.InvalidPassword);
        } catch (ContentFileNotFoundException e) {
            errorMessage = activity.getString(R.string.file_not_found_content);
        } catch (FileNotFoundException e) {
            errorMessage = activity.getString(R.string.file_not_found);
        } catch (IOException e) {
            errorMessage = e.getMessage();
        } catch (KeyFileEmptyException e) {
            errorMessage = activity.getString(R.string.keyfile_is_empty);
        } catch (InvalidAlgorithmException e) {
            errorMessage = activity.getString(R.string.invalid_algorithm);
        } catch (InvalidKeyFileException e) {
            errorMessage = activity.getString(R.string.key_file_not_found);
        } catch (InvalidDBSignatureException e) {
            errorMessage = activity.getString(R.string.invalid_db_sig);
        } catch (InvalidDBVersionException e) {
            errorMessage = activity.getString(R.string.unsupported_db_version);
        } catch (InvalidDBException e) {
            errorMessage = activity.getString(R.string.error_invalid_db);
        } catch (OutOfMemoryError e) {
            errorMessage = activity.getString(R.string.error_out_of_memory);
        } catch (Exception e) {
            throw new IllegalStateException();
        }
        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
    }
}
