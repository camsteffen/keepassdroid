/*
 * Copyright 2010-2015 Brian Pellin.
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
package com.keepassdroid.database;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import com.keepassdroid.icons.Icons;

public class PwIconStandard extends PwIcon {
	public final int iconId;
	
	static PwIconStandard FIRST = new PwIconStandard(1);
	
	static final int TRASH_BIN = 43;
	static final int FOLDER = 48; // TODO make default
	
	public PwIconStandard(int iconId) {
		this.iconId = iconId;
	}

	@Override
	public Drawable getDrawable(Context context) {
		int resId = Icons.iconToResId(iconId);
		return ContextCompat.getDrawable(context, resId);
	}

	@Override
	public boolean isMetaStreamIcon() {
		return iconId == 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + iconId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PwIconStandard other = (PwIconStandard) obj;
		return iconId == other.iconId;
	}
}
