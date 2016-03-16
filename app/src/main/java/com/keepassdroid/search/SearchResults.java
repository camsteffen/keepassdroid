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
package com.keepassdroid.search;

import android.app.SearchManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.keepassdroid.GroupFragment;
import com.keepassdroid.MainActivity;
import com.keepassdroid.PwGroupListAdapter;
import com.keepassdroid.view.GroupEmptyView;
import com.keepassdroid.view.GroupViewOnlyView;

public class SearchResults extends GroupFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        String query = getArguments().getString(SearchManager.QUERY);
        assert query != null;
        query = query.trim();

        mGroup = ((MainActivity) getActivity()).db.Search(getContext(), query);

        View view;
        if ( mGroup == null || mGroup.childEntries.size() < 1 ) {
            view = new GroupEmptyView(getContext());
        } else {
            view = new GroupViewOnlyView(getContext());
        }

        //setGroupTitle(view); TODO

        setListAdapter(new PwGroupListAdapter(getContext(), mGroup));

        return view;
	}
	
}
