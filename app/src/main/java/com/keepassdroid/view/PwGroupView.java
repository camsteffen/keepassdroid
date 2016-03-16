/*
 * Copyright 2009-2012 Brian Pellin.
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
package com.keepassdroid.view;


import android.content.Context;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.android.keepass.R;
import com.keepassdroid.GroupFragment;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.settings.PrefsUtil;


public class PwGroupView extends ClickView {
	
	protected PwGroup mPw;
	protected TextView mTv;

	protected static final int MENU_OPEN = Menu.FIRST;
	
	public static PwGroupView getInstance(GroupFragment fragment, PwGroup pw) {
		if ( pw instanceof PwGroupV3 ) {
			return new PwGroupViewV3(fragment.getContext(), pw, fragment);
		} else {
			return new PwGroupView(fragment.getContext(), pw);
		}
	}
	
	PwGroupView(Context context, PwGroup pw) {
		super(context);
		
		View gv = View.inflate(context, R.layout.group_list_entry, null);
		
		mTv = (TextView) gv.findViewById(R.id.group_name);
		float size = PrefsUtil.getListTextSize(context);
		mTv.setTextSize(size);
		
		populateView(pw);
		
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		addView(gv, lp);
	}
	
	private void populateView(PwGroup pw) {
		mPw = pw;
		
		mTv.setText(pw.getName());
	}

	public void onClick() {
		launchGroup();
	}
	
	private void launchGroup() {
		//GroupFragment.Launch((Activity) getContext(), mPw, db);
        throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
		menu.add(0, MENU_OPEN, 0, R.string.open);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {

		case MENU_OPEN:
			launchGroup();
			return true;
		
		default:
			return false;
		}
	}

}