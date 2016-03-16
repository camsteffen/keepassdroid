/*
 * Copyright 2010 Brian Pellin.
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import com.android.keepass.R;
import com.keepassdroid.compat.BitmapDrawableCompat;

import java.util.UUID;

public class PwIconCustom extends PwIcon {
    private static final int SIZE = 24;

	public byte[] imageData;

    public PwIconCustom(byte[] data) {
        imageData = data;
    }

    @Deprecated
	public PwIconCustom(UUID u, byte[] data) {
		imageData = data;
	}

	@Override
	public Drawable getDrawable(Context context) {

		if (imageData == null) {
            return ContextCompat.getDrawable(context, R.drawable.ic99_blank);
        }
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
		// Could not understand custom icon
		if (bitmap == null) {
            return ContextCompat.getDrawable(context, R.drawable.ic99_blank);
        }
		bitmap = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true);

        return BitmapDrawableCompat.getBitmapDrawable(context.getResources(), bitmap);
	}
}
