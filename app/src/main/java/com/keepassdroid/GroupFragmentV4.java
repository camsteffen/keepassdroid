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
import com.keepassdroid.database.PwGroupIdV4;

import java.util.UUID;

public class GroupFragmentV4 extends GroupFragment {

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		UUID uuid = (UUID) args.getSerializable(KEY_ENTRY);
		id = new PwGroupIdV4(uuid);
	}

	@Override
	protected void setupButtons() {
		super.setupButtons();
		addEntryEnabled = !((MainActivity) getActivity()).db.readOnly;
	}
}
