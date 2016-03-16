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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.*;
import com.keepassdroid.database.edit.AddEntry;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;
import com.keepassdroid.database.edit.UpdateEntry;
import com.keepassdroid.icons.Icons;
import com.keepassdroid.utils.Types;
import com.keepassdroid.utils.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public abstract class EntryEditActivity extends LockCloseHideActivity {
    private static final String KEY_ENTRY = "entry";
    private static final String KEY_PARENT = "parent";

    static final int RESULT_OK_ICON_PICKER = 1000;
    static final int RESULT_OK_PASSWORD_GENERATOR = RESULT_OK_ICON_PICKER + 1;

    PwEntry mEntry;
    private boolean mShowPassword = false;
    boolean mIsNew;
    int mSelectedIconID = -1;

    Database db; // TODO

    static void Launch(Activity act, PwEntry pw) {
        Intent i;
        if (pw instanceof PwEntryV3) {
            i = new Intent(act, EntryEditActivityV3.class);
        } else if (pw instanceof PwEntryV4) {
            i = new Intent(act, EntryEditActivityV4.class);
        } else {
            throw new RuntimeException("Not yet implemented.");
        }

        i.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));

        act.startActivityForResult(i, 0);
    }

    static void Launch(Activity act, PwGroup pw) {
        Intent i;
        if (pw instanceof PwGroupV3) {
            i = new Intent(act, EntryEditActivityV3.class);
            EntryEditActivityV3.putParentId(i, KEY_PARENT, (PwGroupV3) pw);
        } else if (pw instanceof PwGroupV4) {
            i = new Intent(act, EntryEditActivityV4.class);
            EntryEditActivityV4.putParentId(i, KEY_PARENT, (PwGroupV4) pw);
        } else {
            throw new RuntimeException("Not yet implemented.");
        }

        act.startActivityForResult(i, 0);
    }

    protected abstract PwGroupId getParentGroupId(Intent i, String key);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mShowPassword = !prefs.getBoolean(getString(R.string.maskpass_key), getResources().getBoolean(R.bool.maskpass_default));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_edit);
        setResult(App.EXIT_NORMAL);

        Intent i = getIntent();
        byte[] uuidBytes = i.getByteArrayExtra(KEY_ENTRY);

        PwDatabase pm = db.pm;
        if (uuidBytes == null) {

            PwGroupId parentId = getParentGroupId(i, KEY_PARENT);
            PwGroup parent = pm.groups.get(parentId);
            mEntry = PwEntry.getInstance(parent);
            mIsNew = true;

        } else {
            UUID uuid = Types.bytestoUUID(uuidBytes);
            assert (uuid != null);

            mEntry = pm.entries.get(uuid);
            mIsNew = false;

            fillData();
        }

        View scrollView = findViewById(R.id.entry_scroll);
        assert scrollView != null;
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        ImageButton iconButton = (ImageButton) findViewById(R.id.icon_button);
        assert iconButton != null;
        iconButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                IconPickerActivity.Launch(EntryEditActivity.this);
            }
        });

        // Generate password button
        Button generatePassword = (Button) findViewById(R.id.generate_button);
        assert generatePassword != null;
        generatePassword.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                GeneratePasswordActivity.Launch(EntryEditActivity.this);
            }
        });

        // Save button
        Button save = (Button) findViewById(R.id.entry_save);
        assert save != null;
        save.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                EntryEditActivity act = EntryEditActivity.this;

                if (!validateBeforeSaving()) {
                    return;
                }

                PwEntry newEntry = populateNewEntry();

                if (newEntry.getTitle().equals(mEntry.getTitle())) {
                    setResult(App.EXIT_REFRESH);
                } else {
                    setResult(App.EXIT_REFRESH_TITLE);
                }

                RunnableOnFinish task;
                OnFinish onFinish = act.new AfterSave();

                if (mIsNew) {
                    task = AddEntry.getInstance(EntryEditActivity.this, db, newEntry, onFinish);
                } else {
                    task = new UpdateEntry(EntryEditActivity.this, db, mEntry, newEntry, onFinish);
                }
                ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
                pt.run();
            }

        });

        // Cancel button
        Button cancel = (Button) findViewById(R.id.entry_cancel);
        assert cancel != null;
        cancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                finish();

            }

        });

        // Respect mask password setting
        if (mShowPassword) {
            EditText pass = (EditText) findViewById(R.id.entry_password);
            EditText conf = (EditText) findViewById(R.id.entry_confpassword);

            assert pass != null;
            pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            assert conf != null;
            conf.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }

    }

    protected boolean validateBeforeSaving() {
        // Require title
        String title = Util.getEditText(this, R.id.entry_title);
        if (title.length() == 0) {
            Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password
        String pass = Util.getEditText(this, R.id.entry_password);
        String conf = Util.getEditText(this, R.id.entry_confpassword);
        if (!pass.equals(conf)) {
            Toast.makeText(this, R.string.error_pass_match, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    protected PwEntry populateNewEntry() {
        return populateNewEntry(null);
    }

    protected PwEntry populateNewEntry(PwEntry entry) {
        PwEntry newEntry;
        if (entry == null) {
            newEntry = mEntry.clone(true);
        } else {
            newEntry = entry;

        }

        Date now = Calendar.getInstance().getTime();
        newEntry.setLastAccessTime(now);
        newEntry.setLastModificationTime(now);

        PwDatabase pwDatabase = db.pm;
        newEntry.setTitle(Util.getEditText(this, R.id.entry_title), pwDatabase);
        newEntry.setUrl(Util.getEditText(this, R.id.entry_url), pwDatabase);
        newEntry.setUsername(Util.getEditText(this, R.id.entry_user_name), pwDatabase);
        newEntry.setNotes(Util.getEditText(this, R.id.entry_comment), pwDatabase);
        newEntry.setPassword(Util.getEditText(this, R.id.entry_password), pwDatabase);

        return newEntry;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK_ICON_PICKER:
                mSelectedIconID = data.getExtras().getInt(IconPickerActivity.KEY_ICON_ID);
                ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
                assert currIconButton != null;
                currIconButton.setImageResource(Icons.iconToResId(mSelectedIconID));
                break;

            case RESULT_OK_PASSWORD_GENERATOR:
                String generatedPassword = data.getStringExtra("com.keepassdroid.password.generated_password");
                EditText password = (EditText) findViewById(R.id.entry_password);
                EditText confPassword = (EditText) findViewById(R.id.entry_confpassword);

                assert password != null;
                password.setText(generatedPassword);
                assert confPassword != null;
                confPassword.setText(generatedPassword);

                break;
            case Activity.RESULT_CANCELED:
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.entry_edit, menu);
        inflater.inflate(R.menu.main, menu);


        MenuItem togglePassword = menu.findItem(R.id.menu_toggle_pass);
        if (mShowPassword) {
            togglePassword.setTitle(R.string.menu_hide_password);
        } else {
            togglePassword.setTitle(R.string.menu_showpass);
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_donate:
                try {
                    Util.gotoUrl(this, R.string.donate_url);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
                    return false;
                }

                return true;
            case R.id.menu_toggle_pass:
                if (mShowPassword) {
                    item.setTitle(R.string.menu_showpass);
                    mShowPassword = false;
                } else {
                    item.setTitle(R.string.menu_hide_password);
                    mShowPassword = true;
                }
                setPasswordStyle();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setPasswordStyle() {
        TextView password = (TextView) findViewById(R.id.entry_password);
        TextView confpassword = (TextView) findViewById(R.id.entry_confpassword);
        assert password != null;
        assert confpassword != null;

        if (mShowPassword) {
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            confpassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        } else {
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confpassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    protected void fillData() {
        ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
        db.drawFactory.assignDrawableTo(currIconButton, getResources(), mEntry.getIcon(db));

        populateText(R.id.entry_title, mEntry.getTitle());
        populateText(R.id.entry_user_name, mEntry.getUsername());
        populateText(R.id.entry_url, mEntry.getUrl());

        String password = mEntry.getPassword();
        populateText(R.id.entry_password, password);
        populateText(R.id.entry_confpassword, password);
        setPasswordStyle();

        populateText(R.id.entry_comment, mEntry.getNotes());
    }

    private void populateText(int viewId, String text) {
        TextView tv = (TextView) findViewById(viewId);
        assert tv != null;
        tv.setText(text);
    }

    private final class AfterSave extends OnFinish {

        @Override
        public void run() {
            if (mSuccess) {
                finish();
            } else {
                displayMessage(EntryEditActivity.this);
            }
        }

    }

}
