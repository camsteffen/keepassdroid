/*
 * Copyright 2009 Brian Pellin.
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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwItem;
import com.keepassdroid.fileselect.RecentFileHistory;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public Database db;

    private DrawerLayout drawerLayout;
    private LinearLayout drawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private ArrayAdapter<String> drawerAdapter;

    private static final int OPEN_DOCUMENT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri dbUri = intent.getData();
            openUri(dbUri);
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawerLayout != null;
        drawer = (LinearLayout) findViewById(R.id.left_drawer);
        assert drawer != null;
        final ListView recentList = (ListView) findViewById(R.id.recent_databases);
        assert recentList != null;

        drawerAdapter = new ArrayAdapter<>(this, R.layout.recent_database_list_item);
        recentList.setAdapter(drawerAdapter);
        onChangeRecentFiles();
        recentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new OpenRecentTask(MainActivity.this).execute(position);
                drawerLayout.closeDrawer(drawer);
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        setSupportActionBar(toolbar);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(mDrawerToggle);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    private void openUri(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(OpenDBFragment.KEY_DATABASE, uri);
        OpenDBFragment fragment = new OpenDBFragment();
        fragment.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
        setTitle(R.string.open_database);
    }

    void openUri(Uri dbUri, String password, Uri keyFileUri) {
        MyProgressTask<Void, Database> openDatabaseTask = new OpenDatabaseTask(this, dbUri, password, keyFileUri);
        openDatabaseTask.execute();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case OPEN_DOCUMENT:
                Uri uri = intent.getData();
                // TODO move take and remove persistable permissions
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    int takeFlags = intent.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    //noinspection ResourceType
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                }
                openUri(uri);
                break;
        }
    }

    private void onChangeRecentFiles() {
        List<String> recentFiles = App.getFileHistory().getRecentDBList();
        drawerAdapter.clear();
        //drawerAdapter.addAll(recentFiles); //API 11
        for (String recentFile : recentFiles) {
            drawerAdapter.add(recentFile);
        }
    }

    public void openDatabase(View view) {
        try {
            Intent intent;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            } else {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent = Intent.createChooser(intent, getString(R.string.open_database));
            }
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, OPEN_DOCUMENT);
            drawerLayout.closeDrawer(drawer);

        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.no_app_found)
                    .setMessage(R.string.no_app_found_msg)
                    .create()
                    .show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mDrawerToggle.onOptionsItemSelected(item))
            return true;
        if(super.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.menu_lock:
                finish();
                return true;
        }
        return false;
    }

    void openItem(PwItem item) {
        Fragment fragment = item.createFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private static class OpenRecentTask extends AsyncTask<Integer, Void, Void> {
        private final MainActivity activity;
        String fileName;
        String keyFile;

        OpenRecentTask(MainActivity activity) {
            this.activity = activity;
        }

        protected Void doInBackground(Integer... args) {
            int position = args[0];
            RecentFileHistory fileHistory = App.getFileHistory();
            fileName = fileHistory.getDatabaseAt(position);
            keyFile = fileHistory.getKeyfileAt(position);
            return null;
        }

        protected void onPostExecute(Void v) {
            Uri uri = Uri.parse(fileName);
            activity.openUri(uri);
        }
    }
}
