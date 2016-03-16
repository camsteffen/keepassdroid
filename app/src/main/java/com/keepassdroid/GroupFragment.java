/*
 * Copyright 2009-2014 Brian Pellin.
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

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.android.keepass.R;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwItem;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.dialog.ReadOnlyDialog;
import com.keepassdroid.view.ClickView;

public abstract class GroupFragment extends ListFragment {

    public static final String KEY_ENTRY = "entry";
    private boolean addGroupEnabled = false;
    boolean addEntryEnabled = false;
    PwGroupId id;
    protected PwGroup mGroup;

    private static final String TAG = "Group Activity:";

    protected void setupButtons() {
        addGroupEnabled = !((MainActivity) getActivity()).db.readOnly;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.w(TAG, "Creating group view");

        Database db = ((MainActivity) getActivity()).db;
        PwGroup root = db.pm.rootGroup;
        if (id == null) {
            mGroup = root;
        } else {
            mGroup = db.pm.groups.get(id);
        }

        Log.w(TAG, "Retrieved group");

        setupButtons();

        View view = inflater.inflate(R.layout.group_add_entry, container, false);

        // Add Group button
        Button addGroup = (Button) view.findViewById(R.id.add_group);
        if (addGroupEnabled) {
            addGroup.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    GroupEditActivity.Launch(getActivity());
                }
            });
        } else {
            addGroup.setVisibility(View.GONE);
        }

        // Add Entry button
        Button addEntry = (Button) view.findViewById(R.id.add_entry);
        if (addEntryEnabled) {
            addEntry.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    EntryEditActivity.Launch(getActivity(), mGroup);
                }
            });
        } else {
            addEntry.setVisibility(View.GONE);
        }

        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(mGroup.getName());

        setListAdapter(new PwGroupListAdapter(getActivity(), mGroup));
        registerForContextMenu(view.findViewById(android.R.id.list));

        if (mGroup == db.pm.rootGroup) {
            showWarnings();
        }

        return view;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        PwGroupListAdapter adapter = (PwGroupListAdapter) getListAdapter();
        PwItem item = adapter.getItem(position);
        MainActivity activity = (MainActivity) getActivity();
        activity.openItem(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {

        AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
        ClickView cv = (ClickView) acmi.targetView;
        cv.onCreateMenu(menu, menuInfo);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
        ClickView cv = (ClickView) acmi.targetView;

        return cv.onContextItemSelected(item);
    }

    /* TODO
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                String GroupName = data.getExtras().getString(GroupEditActivity.KEY_NAME);
                int GroupIconID = data.getExtras().getInt(GroupEditActivity.KEY_ICON_ID);
                AddGroup task = AddGroup.getInstance(getContext(), ((MainActivity) getActivity()).db, GroupName, GroupIconID, mGroup, new RefreshTask(), false);
                ProgressTask pt = new ProgressTask(getContext(), task, R.string.saving_database);
                pt.run();
                break;

            case Activity.RESULT_CANCELED:
            default:
                break;
        }
    }
    */

    private void showWarnings() {
        if (((MainActivity) getActivity()).db.readOnly) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

            if (prefs.getBoolean(getString(R.string.show_read_only_warning), true)) {
                Dialog dialog = new ReadOnlyDialog(getContext());
                dialog.show();
            }
        }
    }

    private void refreshIfDirty() {
        Database db = ((MainActivity) getActivity()).db;
        if ( db.dirty.contains(mGroup) ) {
            db.dirty.remove(mGroup);
            BaseAdapter adapter = (BaseAdapter) getListAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    public class AfterDeleteGroup extends OnFinish {

        @Override
        public void run() {
            if ( mSuccess) {
                refreshIfDirty();
            } else {
                mHandler.post(new UIToastTask(getContext(), "Unrecoverable error: " + mMessage));
                getActivity().finish();
            }
        }

    }
}
