/*
 * Copyright 2009-2016 Brian Pellin.
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.BackupManagerCompat;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.settings.AppSettingsActivity;

public class OpenDBFragment extends LockingFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_DEFAULT_FILENAME = "defaultFileName";
    public static final String KEY_FILENAME = "fileName";
    static final String KEY_DATABASE = "uri";
    public static final String KEY_KEY_FILE = "keyFile";

    // content loader IDs
    private static final int LOADER_DB = 0;
    private static final int LOADER_KEY_FILE = 1;

    // activity request codes
    private static final int GET_CONTENT = 0;
    private static final int OPEN_DOC = 1;

    private Uri dbUri = null;
    private Uri keyFileUri = null;

    private SharedPreferences preferences;

    private TextView dbUriTV;
    private EditText passwordTV;
    private Button addKeyFileButton;
    private ImageButton clearKeyFileButton;
    private TextView keyFileTV;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        dbUri = args.getParcelable(KEY_DATABASE);
        keyFileUri = App.getFileHistory().getFileByName(dbUri); // TODO: move to separate thread
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        View view = inflater.inflate(R.layout.password, container, false);

        // initialize view variables
        dbUriTV = (TextView) view.findViewById(R.id.db_uri);
        passwordTV = (EditText) view.findViewById(R.id.password);
        addKeyFileButton = (Button) view.findViewById(R.id.add_key_file);
        clearKeyFileButton = (ImageButton) view.findViewById(R.id.clear_key_file);
        keyFileTV = (TextView) view.findViewById(R.id.key_file_uri);

        getLoaderManager().initLoader(LOADER_DB, args, this);

        // default database checkbox
        CheckBox setDefaultDBCheckBox = (CheckBox) view.findViewById(R.id.default_database);
        String defaultFilename = preferences.getString(KEY_DEFAULT_FILENAME, null);
        if (defaultFilename != null) {
            Uri defaultUri = Uri.parse(defaultFilename);
            if(defaultUri.equals(dbUri)) {
                setDefaultDBCheckBox.setChecked(true);
            }
        }
        setDefaultDBCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String newDefaultFileName;

                if (isChecked) {
                    newDefaultFileName = dbUri.toString();
                } else {
                    newDefaultFileName = "";
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(KEY_DEFAULT_FILENAME, newDefaultFileName);
                EditorCompat.apply(editor);

                BackupManagerCompat backupManager = new BackupManagerCompat(getContext());
                backupManager.dataChanged();
            }
        });

        // show passwordTV checkbox
        CheckBox showPassword = (CheckBox) view.findViewById(R.id.show_password);
        showPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int inputType = isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                passwordTV.setInputType(inputType);
            }

        });

        // add key file button
        addKeyFileButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (StorageAF.useStorageFramework(getContext())) {
                    Intent i = new Intent(StorageAF.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, OPEN_DOC);
                } else {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");

                    try {
                        startActivityForResult(i, GET_CONTENT);
                    } catch (ActivityNotFoundException e) {
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.no_app_found)
                                .setMessage(R.string.no_app_found_msg)
                                .create()
                                .show();
                    }
                }
            }
        });

        // clear key file button
        clearKeyFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyFileUri = null;
                onChangeKeyFile();
            }
        });

        onChangeKeyFile();

        // open button
        Button openButton = (Button) view.findViewById(R.id.open);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (passwordTV.length() == 0 && (keyFileUri == null)) {
                    Toast.makeText(getContext(), R.string.error_nopass, Toast.LENGTH_LONG).show();
                    return;
                }
                String password = passwordTV.getText().toString();
                ((MainActivity) getActivity()).openUri(dbUri, password, keyFileUri);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK)
            return;

        switch (requestCode) {
            case GET_CONTENT:
            case OPEN_DOC: {
                if (data == null) break;

                keyFileUri = data.getData();
                getLoaderManager().restartLoader(LOADER_KEY_FILE, null, this);
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                AboutDialog dialog = new AboutDialog(getContext());
                dialog.show();
                return true;

            case R.id.menu_app_settings:
                AppSettingsActivity.Launch(getContext());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_DB:
                Uri uri = args.getParcelable(KEY_DATABASE);
                return new CursorLoader(
                        getContext(),
                        uri,
                        null,
                        null,
                        null,
                        null
                );
            case LOADER_KEY_FILE:
                return new CursorLoader(
                        getContext(),
                        keyFileUri,
                        null,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_DB: {
                data.moveToFirst();
                String displayName = data.getString(
                        data.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                dbUriTV.setText(displayName);
                break;
            }
            case LOADER_KEY_FILE: {
                data.moveToFirst();
                String displayName = data.getString(
                        data.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                keyFileTV.setText(displayName);
                onChangeKeyFile();
                break;
            }
        }
    }

    private void onChangeKeyFile() {
        if(keyFileUri == null) {
            addKeyFileButton.setVisibility(View.VISIBLE);
            clearKeyFileButton.setVisibility(View.GONE);
            keyFileTV.setVisibility(View.GONE);
        } else {
            addKeyFileButton.setVisibility(View.GONE);
            clearKeyFileButton.setVisibility(View.VISIBLE);
            keyFileTV.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
