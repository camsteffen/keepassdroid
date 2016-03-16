/*
 * Copyright 2010-2014 Brian Pellin.
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

import android.os.Bundle;
import android.support.annotation.Nullable;
import com.keepassdroid.database.PwGroupIdV3;

public class GroupFragmentV3 extends GroupFragment {

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		int idInt = args.getInt(KEY_ENTRY);
		id = new PwGroupIdV3(idInt);
	}
	
	@Override
	protected void setupButtons() {
		super.setupButtons();
		Database db = ((MainActivity) getActivity()).db;
		addEntryEnabled = mGroup != db.pm.rootGroup && !db.readOnly;
	}
}
