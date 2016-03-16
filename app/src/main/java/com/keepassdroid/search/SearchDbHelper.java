/*
 * Copyright 2009-2013 Brian Pellin.
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.database.*;

import java.util.*;

public class SearchDbHelper {
	
	private static boolean omitBackup(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(context.getString(R.string.omitbackup_key), context.getResources().getBoolean(R.bool.omitbackup_default));
	}
	
	public static PwGroup search(Context context, Database db, String qStr) {
		PwDatabase pm = db.pm;

		PwGroup group;
		if ( pm instanceof PwDatabaseV3 ) {
			group = new PwGroupV3();
		} else if ( pm instanceof PwDatabaseV4 ) {
			group = new PwGroupV4();
		} else {
			Log.d("SearchDbHelper", "Tried to search with unknown db");
			return null;
		}
		group.name = context.getString(R.string.search_results);
		group.childEntries = new ArrayList<>();
		
		// Search all entries
		Locale loc = Locale.getDefault();
		qStr = qStr.toLowerCase(loc);
		boolean isOmitBackup = omitBackup(context);
		
		Queue<PwGroup> worklist = new LinkedList<>();
		if (pm.rootGroup != null) {
			worklist.add(pm.rootGroup);
		}
		
		while (worklist.size() != 0) {
			PwGroup top = worklist.remove();
			
			if (pm.isGroupSearchable(top, isOmitBackup)) {
				for (PwEntry entry : top.childEntries) {
					SearchDbHelper.processEntries(entry, group.childEntries, qStr, loc);
				}
				
				for (PwGroup childGroup : top.childGroups) {
					if (childGroup != null) {
						worklist.add(childGroup);
					}
				}
			}
		}
		
		return group;
	}
	
	private static void processEntries(PwEntry entry, List<PwEntry> results, String qStr, Locale loc) {
		// Search all strings in the entry
		Iterator<String> iter = entry.stringIterator();
		while (iter.hasNext()) {
			String str = iter.next();
			if (str != null && str.length() != 0) {
				String lower = str.toLowerCase(loc);
				if (lower.contains(qStr)) {
					results.add(entry);
					break;
				}
			}
		}
	}
	
}
