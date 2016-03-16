package com.keepassdroid.database;

import android.content.Context;
import android.graphics.drawable.Drawable;

public abstract class PwIcon {
	
	public boolean isMetaStreamIcon() {
		return false;
	}

	public abstract Drawable getDrawable(Context context);
}
