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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.keepass.R;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PwGroupListAdapter extends BaseAdapter {

    private static final int GROUP = 0;
    private static final int ENTRY = 1;
    private final LayoutInflater mInflater;

    private Context context;
    private PwGroup mGroup;
    private List<PwGroup> groupsForViewing;
    private List<PwEntry> entriesForViewing;
    private Comparator<PwEntry> entryComp = new PwEntry.EntryNameComparator();
    private Comparator<PwGroup> groupComp = new PwGroup.GroupNameComparator();
    private SharedPreferences prefs;

    public PwGroupListAdapter(Context context, PwGroup group) {
        this.context = context;
        mGroup = group;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        filterAndSort();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        filterAndSort();
    }

    @Override
    public void notifyDataSetInvalidated() {
        super.notifyDataSetInvalidated();

        filterAndSort();
    }

    private void filterAndSort() {
        entriesForViewing = new ArrayList<>();
        for(PwEntry entry : mGroup.childEntries) {
            if(!entry.isMetaStream())
                entriesForViewing.add(entry);
        }

        boolean sortLists = prefs.getBoolean(context.getString(R.string.sort_key), context.getResources().getBoolean(R.bool.sort_default));
        if (sortLists) {
            groupsForViewing = new ArrayList<>(mGroup.childGroups);

            Collections.sort(entriesForViewing, entryComp);
            Collections.sort(groupsForViewing, groupComp);
        } else {
            groupsForViewing = mGroup.childGroups;
        }
    }

    @Override
    public int getCount() {
        return groupsForViewing.size() + entriesForViewing.size();
    }

    @Override
    public PwItem getItem(int position) {
        if (position < groupsForViewing.size()) {
            return groupsForViewing.get(position);
        } else {
            return entriesForViewing.get(position - groupsForViewing.size());
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView textView;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            switch (getItemViewType(position)) {
                case GROUP:
                    convertView = mInflater.inflate(R.layout.group_list_entry, parent, false);
                    holder.textView = (TextView) convertView.findViewById(R.id.group_name);
                    break;
                case ENTRY:
                    convertView = mInflater.inflate(R.layout.entry_list_entry, parent, false);
                    holder.textView = (TextView) convertView.findViewById(R.id.entry_name);
                    break;
                default:
                    throw new IllegalStateException();
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView.setText(getItem(position).getName());
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 2; // groups and entries
    }

    @Override
    public int getItemViewType(int position) {
        if (position < groupsForViewing.size()) {
            return GROUP;
        } else {
            return ENTRY;
        }
    }

}
